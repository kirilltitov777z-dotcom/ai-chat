package io.margy.aichat.net

import io.margy.aichat.data.AppSettings
import io.margy.aichat.data.Message
import io.margy.aichat.data.PROVIDER_CUSTOM
import io.margy.aichat.data.PROVIDER_GROQ
import io.margy.aichat.data.PROVIDER_LOCAL
import io.margy.aichat.data.PROVIDER_OPENROUTER
import io.margy.aichat.data.PROVIDER_POLLINATIONS
import io.margy.aichat.data.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class ApiException(val code: Int, override val message: String) : Exception(message)

object ChatApi {

    private val JSON = "application/json; charset=utf-8".toMediaType()

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(0, TimeUnit.MILLISECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .dns(io.margy.aichat.util.Net.dns)
        .build()

    data class Endpoint(val url: String, val key: String, val headers: Map<String, String>)

    fun endpoint(s: AppSettings): Endpoint = when (s.provider) {
        PROVIDER_POLLINATIONS -> Endpoint(
            "https://text.pollinations.ai/openai/chat/completions",
            "",
            emptyMap()
        )
        PROVIDER_GROQ -> Endpoint(
            "https://api.groq.com/openai/v1/chat/completions",
            s.apiKey,
            emptyMap()
        )
        PROVIDER_OPENROUTER -> Endpoint(
            "https://openrouter.ai/api/v1/chat/completions",
            s.apiKey,
            mapOf("X-Title" to "LotiChat")
        )
        PROVIDER_LOCAL -> Endpoint("local://local", "", emptyMap())
        PROVIDER_CUSTOM -> {
            val base = s.baseUrl.trim().trimEnd('/')
            val url = when {
                base.isBlank() -> ""
                base.endsWith("/chat/completions") -> base
                else -> "$base/chat/completions"
            }
            Endpoint(url, s.apiKey, emptyMap())
        }
        else -> Endpoint(
            "https://openrouter.ai/api/v1/chat/completions",
            s.apiKey,
            mapOf("X-Title" to "LotiChat")
        )
    }

    fun effectiveModel(s: AppSettings, thinking: Boolean): String {
        val current = s.model.ifBlank { s.defaultModel() }
        if (s.provider == "demo" || s.provider == "local") return current
        if (!thinking) return current
        return when (s.provider) {
            PROVIDER_GROQ -> current
            PROVIDER_CUSTOM -> current
            else -> current
        }.let { model ->
            if (s.provider != "openrouter") return@let model
            if (Regex("r1|reasoner|thinking|qwq|o1|o3", RegexOption.IGNORE_CASE).containsMatchIn(model)) model
            else "deepseek/deepseek-r1:free"
        }
    }

    fun systemPrompt(s: AppSettings, searchContext: String): String {
        val base = s.system.ifBlank { "Ты — полезный ассистент." }
        if (searchContext.isBlank()) return base
        return buildString {
            append(base)
            append("\n\nНиже — свежие данные из интернета, найденные поиском. ")
            append("Опирайся на них, если они относятся к вопросу, и коротко укажи источник.\n\n")
            append(searchContext)
        }
    }

    private fun buildUserText(m: Message, withImages: Boolean): String = buildString {
        append(m.text)
        m.attachments.forEach { a ->
            if (a.isImage) {
                if (!withImages) append("\n\n[Прикреплено изображение: ${a.name}]")
            } else {
                val body = a.text
                append("\n\n[Файл: ${a.name}]\n")
                if (body != null) append("```\n${body.take(20000)}\n```")
                else append("(бинарный файл, ${a.size} байт)")
            }
        }
    }

    private fun messagesJson(history: List<Message>, system: String, withImages: Boolean): JSONArray {
        val arr = JSONArray()
        if (system.isNotBlank()) {
            arr.put(JSONObject().put("role", "system").put("content", system))
        }
        val trimmed = history.takeLast(40)
        trimmed.forEach { m ->
            if (m.error) return@forEach
            if (m.role == Role.ASSISTANT) {
                if (m.text.isBlank()) return@forEach
                arr.put(JSONObject().put("role", "assistant").put("content", m.text))
            } else {
                val text = buildUserText(m, withImages)
                val images = if (withImages) m.attachments.filter { it.isImage && !it.dataUrl.isNullOrBlank() } else emptyList()
                if (images.isEmpty()) {
                    if (text.isNotBlank()) arr.put(JSONObject().put("role", "user").put("content", text))
                } else {
                    val content = JSONArray()
                    content.put(JSONObject().put("type", "text").put("text", text.ifBlank { "Что на изображении?" }))
                    images.forEach { img ->
                        content.put(
                            JSONObject()
                                .put("type", "image_url")
                                .put("image_url", JSONObject().put("url", img.dataUrl))
                        )
                    }
                    arr.put(JSONObject().put("role", "user").put("content", content))
                }
            }
        }
        return arr
    }

    private fun requestBody(
        s: AppSettings,
        model: String,
        history: List<Message>,
        searchContext: String,
        thinking: Boolean,
        withImages: Boolean
    ): JSONObject = JSONObject().apply {
        put("model", model)
        put("messages", messagesJson(history, systemPrompt(s, searchContext), withImages))
        put("stream", true)
        put("temperature", s.temperature.toDouble())
        if (thinking && s.provider == "openrouter") {
            put("reasoning", JSONObject().put("enabled", true))
        }
    }

    suspend fun stream(
        settings: AppSettings,
        model: String,
        history: List<Message>,
        searchContext: String,
        thinking: Boolean,
        withImages: Boolean,
        onDelta: (String) -> Unit,
        onReasoning: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val ep = endpoint(settings)
        if (ep.url.isBlank()) throw ApiException(0, "Не указан адрес сервера в настройках")
        if (ep.key.isBlank() && settings.provider != PROVIDER_POLLINATIONS && settings.provider != PROVIDER_LOCAL) {
            throw ApiException(0, "Не указан API-ключ — открой настройки")
        }

        val body = requestBody(settings, model, history, searchContext, thinking, withImages).toString()
        val req = Request.Builder()
            .url(ep.url)
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .apply {
                if (ep.key.isNotBlank()) header("Authorization", "Bearer ${ep.key}")
                ep.headers.forEach { (k, v) -> header(k, v) }
            }
            .post(body.toRequestBody(JSON))
            .build()

        val call = http.newCall(req)
        val handle = kotlinx.coroutines.currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }
        try {
            call.execute().use { resp: Response ->
                if (!resp.isSuccessful) {
                    val raw = resp.body?.string().orEmpty()
                    throw ApiException(resp.code, parseError(raw, resp.code))
                }
                val ct = resp.header("Content-Type").orEmpty()
                val b = resp.body ?: throw ApiException(0, "Пустой ответ сервера")
                if (ct.contains("event-stream", true) || ct.contains("stream", true)) {
                    val source = b.source()
                    while (true) {
                        val line = source.readUtf8Line() ?: break
                        if (line.isBlank()) continue
                        if (!line.startsWith("data:")) continue
                        val data = line.removePrefix("data:").trim()
                        if (data == "[DONE]") break
                        parseChunk(data, onDelta, onReasoning)
                    }
                } else {
                    parseWhole(b.string(), onDelta, onReasoning)
                }
            }
        } catch (e: java.net.UnknownHostException) {
            throw ApiException(0, "Не удалось найти сервер. Проверь интернет или отключи Private DNS в настройках телефона.")
        } finally {
            handle?.dispose()
        }
    }

    private fun parseChunk(data: String, onDelta: (String) -> Unit, onReasoning: (String) -> Unit) {
        val obj = runCatching { JSONObject(data) }.getOrNull() ?: return
        obj.optJSONObject("error")?.let { err ->
            throw ApiException(0, err.optString("message", "Ошибка API"))
        }
        val choices = obj.optJSONArray("choices") ?: return
        val choice = choices.optJSONObject(0) ?: return
        val delta = choice.optJSONObject("delta") ?: choice.optJSONObject("message") ?: return
        val content = delta.optString("content", "")
        if (content.isNotEmpty() && content != "null") onDelta(content)
        val reasoning = delta.optString("reasoning", "").ifBlank { delta.optString("reasoning_content", "") }
        if (reasoning.isNotEmpty() && reasoning != "null") onReasoning(reasoning)
    }

    private fun parseWhole(raw: String, onDelta: (String) -> Unit, onReasoning: (String) -> Unit) {
        val obj = runCatching { JSONObject(raw) }.getOrNull()
            ?: throw ApiException(0, "Непонятный ответ сервера")
        obj.optJSONObject("error")?.let { err ->
            throw ApiException(0, err.optString("message", "Ошибка API"))
        }
        val choice = obj.optJSONArray("choices")?.optJSONObject(0)
            ?: throw ApiException(0, "Пустой ответ модели")
        val msg = choice.optJSONObject("message")
        val content = msg?.optString("content").orEmpty()
        val reasoning = msg?.optString("reasoning").orEmpty().ifBlank { msg?.optString("reasoning_content").orEmpty() }
        if (reasoning.isNotBlank()) onReasoning(reasoning)
        if (content.isNotBlank()) onDelta(content)
    }

    private fun parseError(raw: String, code: Int): String {
        if (raw.contains("FREE_LIMIT", true)) {
            val wait = Regex("Retry after (\\d+)s").find(raw)?.groupValues?.get(1)?.toIntOrNull()
            return if (wait != null) {
                "Бесплатный лимит: 1 ответ в 45 сек. Подождите $wait сек и повторите."
            } else {
                "Бесплатный лимит: 1 ответ в 45 секунд. Подождите немного и повторите."
            }
        }
        val msg = runCatching {
            val o = JSONObject(raw)
            o.optJSONObject("error")?.optString("message")
                ?: o.optString("message")
        }.getOrNull()
        val base = when {
            !msg.isNullOrBlank() -> msg
            raw.isNotBlank() -> raw.take(300)
            code == 401 -> "Ключ не подошёл (401)"
            code == 402 -> "Закончились кредиты (402)"
            code == 404 -> "Модель не найдена (404)"
            code == 429 -> "Слишком много запросов (429), подожди немного"
            else -> "Ошибка сервера ($code)"
        }
        return base
    }

    suspend fun listModels(s: AppSettings): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching<List<String>> {
            val ep = endpoint(s)
            val modelsUrl = when {
                s.provider == PROVIDER_CUSTOM -> {
                    val base = s.baseUrl.trim().trimEnd('/')
                    if (base.isBlank()) "" else "$base/models"
                }
                s.provider == PROVIDER_GROQ -> "https://api.groq.com/openai/v1/models"
                else -> "https://openrouter.ai/api/v1/models"
            }
            if (modelsUrl.isBlank()) throw ApiException(0, "Не указан адрес сервера")
            if (ep.key.isBlank()) throw ApiException(0, "Не указан API-ключ")
            val req = Request.Builder()
                .url(modelsUrl)
                .header("Authorization", "Bearer ${ep.key}")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw ApiException(resp.code, parseError(raw, resp.code))
                val arr = JSONObject(raw).optJSONArray("data") ?: JSONArray()
                val ids = ArrayList<String>(arr.length())
                for (i in 0 until arr.length()) {
                    val id = arr.optJSONObject(i)?.optString("id").orEmpty()
                    if (id.isNotBlank()) ids.add(id)
                }
                ids
            }
        }
    }
}
