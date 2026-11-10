package io.margy.aichat.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Бесплатный поиск без ключей: Wikipedia (API) + DuckDuckGo Instant Answer.
 * Возвращает готовый текстовый блок для системного промпта.
 */
object WebSearch {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    suspend fun search(query: String): String = withContext(Dispatchers.IO) {
        val q = query.trim().replace("\n", " ").take(300)
        if (q.isBlank()) return@withContext ""
        val parts = ArrayList<String>()

        runCatching { wikipedia(q, "ru") }.getOrNull()?.let { if (it.isNotBlank()) parts.add(it) }
        runCatching { duckduckgo(q) }.getOrNull()?.let { if (it.isNotBlank()) parts.add(it) }
        if (parts.isEmpty()) {
            runCatching { wikipedia(q, "en") }.getOrNull()?.let { if (it.isNotBlank()) parts.add(it) }
        }
        parts.joinToString("\n\n").take(5000)
    }

    private fun get(url: String): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "LotiChat/2.0 (Android; github.com/kirilltitov777z-dotcom/ai-chat)")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return ""
            return resp.body?.string().orEmpty()
        }
    }

    private fun wikipedia(query: String, lang: String): String {
        val url = "https://$lang.wikipedia.org/w/api.php?action=query&generator=search" +
            "&gsrsearch=${enc(query)}&gsrlimit=3&prop=extracts&exintro=1&explaintext=1" +
            "&format=json&utf8=1&origin=*"
        val root = JSONObject(get(url))
        val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: return ""
        val sb = StringBuilder()
        val keys = pages.keys()
        var count = 0
        while (keys.hasNext() && count < 3) {
            val page = pages.optJSONObject(keys.next()) ?: continue
            val title = page.optString("title").orEmpty()
            val extract = page.optString("extract").orEmpty().replace("\n", " ").trim()
            if (title.isBlank() || extract.isBlank()) continue
            sb.append("Википедия (${lang.uppercase()}): ").append(title).append(" — ")
                .append(extract.take(700)).append('\n')
            count++
        }
        return sb.toString().trim()
    }

    private fun duckduckgo(query: String): String {
        val url = "https://api.duckduckgo.com/?q=${enc(query)}&format=json&no_html=1&skip_disambig=1"
        val root = JSONObject(get(url))
        val sb = StringBuilder()
        val abstract = root.optString("AbstractText").orEmpty()
        if (abstract.isNotBlank()) {
            sb.append("DuckDuckGo: ").append(abstract.take(700)).append('\n')
        }
        val topics = root.optJSONArray("RelatedTopics")
        if (topics != null) {
            var count = 0
            for (i in 0 until topics.length()) {
                if (count >= 4) break
                val obj = topics.optJSONObject(i) ?: continue
                val text = obj.optString("Text").orEmpty()
                val firstUrl = obj.optString("FirstURL").orEmpty()
                if (text.isBlank()) continue
                sb.append("- ").append(text.take(220))
                if (firstUrl.isNotBlank()) sb.append(" (").append(firstUrl).append(')')
                sb.append('\n')
                count++
            }
        }
        return sb.toString().trim()
    }
}
