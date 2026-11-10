package io.margy.aichat.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.margy.aichat.data.AppSettings
import io.margy.aichat.data.Attachment
import io.margy.aichat.data.Chat
import io.margy.aichat.data.ChatJson
import io.margy.aichat.data.ChatStore
import io.margy.aichat.data.Message
import io.margy.aichat.data.PROVIDER_LOCAL
import io.margy.aichat.data.PROVIDER_POLLINATIONS
import io.margy.aichat.data.Role
import io.margy.aichat.data.SettingsStore
import io.margy.aichat.net.ApiException
import io.margy.aichat.net.ChatApi
import io.margy.aichat.net.WebSearch
import io.margy.aichat.util.Media
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsStore = SettingsStore(app)
    private val chatStore = ChatStore(app)

    var settings by mutableStateOf(settingsStore.load())
        private set

    var chats by mutableStateOf<List<Chat>>(emptyList())
        private set

    var currentId by mutableStateOf("")
        private set

    var draft by mutableStateOf("")
    var pending by mutableStateOf<List<Attachment>>(emptyList())
        private set

    var busy by mutableStateOf(false)
        private set

    var thinking by mutableStateOf(false)
    var searchEnabled by mutableStateOf(false)

    var checkingKey by mutableStateOf(false)
        private set

    var models by mutableStateOf<List<String>>(emptyList())
        private set

    var toastMessage by mutableStateOf<String?>(null)
        private set

    var showModelPicker by mutableStateOf(false)
        private set

    var screen by mutableStateOf("chat")
        set

    var devLogs by mutableStateOf<List<String>>(emptyList())
        private set

    var showLogin by mutableStateOf(false)
        private set

    private var sendJob: Job? = null

    init {
        val loaded = chatStore.load(settings.accountUser)
        chats = if (loaded.isEmpty()) listOf(Chat()) else loaded.sortedByDescending { it.updated }
        currentId = chats.first().id

        if (settings.accountUser.isBlank() && settings.firstRun) {
            screen = "login"
        }
    }

    val currentChat: Chat?
        get() = chats.firstOrNull { it.id == currentId }

    val devMode: Boolean
        get() = settings.devMode

    val apiReady: Boolean
        get() = settings.apiKey.isNotBlank() || settings.provider == PROVIDER_POLLINATIONS || settings.provider == PROVIDER_LOCAL

    val isLoggedIn: Boolean
        get() = settings.accountLoggedIn && settings.accountUser.isNotBlank()

    val appVersion: String
        get() = "1.0"

    fun openSettings() {
        screen = "settings"
    }

    fun closeSettings() {
        screen = "chat"
    }

    fun openLogin() {
        showLogin = true
    }

    fun closeLogin() {
        showLogin = false
    }

    fun toChatScreen() {
        screen = "chat"
    }

    fun updateSettings(s: AppSettings) {
        settings = s
        settingsStore.save(s)
    }

    fun showToast(text: String) {
        toastMessage = text
    }

    fun consumeToast() {
        toastMessage = null
    }

    fun newChat() {
        val chat = Chat()
        chats = listOf(chat) + chats
        currentId = chat.id
        screen = "chat"
        persistNow()
    }

    fun selectChat(id: String) {
        currentId = id
        screen = "chat"
    }

    fun deleteChat(id: String) {
        val rest = chats.filterNot { it.id == id }
        chats = if (rest.isEmpty()) listOf(Chat()) else rest
        if (currentId == id) currentId = chats.first().id
        persistNow()
    }

    fun clearAll() {
        val chat = Chat()
        chats = listOf(chat)
        currentId = chat.id
        persistNow()
    }

    fun deleteMessage(msgId: String) {
        val id = currentId
        updateChat(id) { c -> c.copy(messages = c.messages.filterNot { it.id == msgId }) }
        persistNow()
    }

    fun addAttachments(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val app = getApplication<Application>()
            val list = withContext(Dispatchers.IO) {
                uris.mapNotNull { Media.fromUri(app, it) }
            }
            if (list.isEmpty()) {
                toastMessage = "Не удалось прочитать файл"
            } else {
                pending = pending + list
            }
        }
    }

    fun removeAttachment(att: Attachment) {
        pending = pending.filterNot { it.name == att.name && it.size == att.size && it.path == att.path }
    }

    fun clearPending() {
        pending = emptyList()
    }

    fun send() {
        if (busy) {
            stop()
            return
        }
        val text = draft.trim()
        val atts = pending
        if (text.isEmpty() && atts.isEmpty()) return
        if (!apiReady) {
            toastMessage = "API-ключ не настроен. Перейдите в Настройки."
            screen = "settings"
            return
        }

        draft = ""
        pending = emptyList()
        val chatId = ensureChat()
        val userMsg = Message(role = Role.USER, text = text, attachments = atts)
        val aiMsg = Message(role = Role.ASSISTANT, streaming = true)
        updateChat(chatId) { c ->
            val firstUser = c.messages.none { it.role == Role.USER }
            c.copy(
                title = if (firstUser) titleFrom(text, atts) else c.title,
                messages = c.messages + userMsg + aiMsg
            )
        }
        addDevLog("ПОЛЬЗОВАТЕЛЬ: $text")
        launchReply(chatId, aiMsg.id, atts)
    }

    fun stop() {
        sendJob?.cancel()
        sendJob = null
        busy = false
    }

    fun regenerate() {
        if (busy) {
            stop()
            return
        }
        val chat = currentChat ?: return
        val lastAi = chat.messages.lastOrNull { it.role == Role.ASSISTANT } ?: return
        val lastUser = chat.messages.lastOrNull { it.role == Role.USER } ?: return
        updateChat(chat.id) { c -> c.copy(messages = c.messages.filterNot { it.id == lastAi.id }) }
        val aiMsg = Message(role = Role.ASSISTANT, streaming = true)
        updateChat(chat.id) { c -> c.copy(messages = c.messages + aiMsg) }
        launchReply(chat.id, aiMsg.id, lastUser.attachments)
    }

    fun retry() {
        val chat = currentChat ?: return
        val lastAi = chat.messages.lastOrNull { it.role == Role.ASSISTANT } ?: return
        if (busy) return
        val lastUser = chat.messages.lastOrNull { it.role == Role.USER } ?: return
        updateChat(chat.id) { c -> c.copy(messages = c.messages.filterNot { it.id == lastAi.id }) }
        val aiMsg = Message(role = Role.ASSISTANT, streaming = true)
        updateChat(chat.id) { c -> c.copy(messages = c.messages + aiMsg) }
        launchReply(chat.id, aiMsg.id, lastUser.attachments)
    }

    fun exportJson(): String = ChatJson.encode(chats)

    fun checkKey() {
        if (checkingKey) return
        viewModelScope.launch {
            checkingKey = true
            val res = ChatApi.listModels(settings)
            checkingKey = false
            res.onSuccess { list ->
                toastMessage = "Ключ работает. Моделей: ${list.size}"
            }.onFailure {
                toastMessage = it.message ?: "Не удалось проверить ключ"
            }
        }
    }

    fun checkConnection() {
        if (checkingKey) return
        viewModelScope.launch {
            checkingKey = true
            toastMessage = "Проверяю связь…"
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val ep = ChatApi.endpoint(settings)
                    val body = org.json.JSONObject().apply {
                        put("model", settings.model.ifBlank { "openai" })
                        put("messages", org.json.JSONArray().apply {
                            put(org.json.JSONObject().put("role", "user").put("content", "ping"))
                        })
                        put("stream", false)
                        put("max_tokens", 8)
                    }
                    val req = okhttp3.Request.Builder()
                        .url(ep.url)
                        .header("Content-Type", "application/json")
                        .apply { if (ep.key.isNotBlank()) header("Authorization", "Bearer ${ep.key}") }
                        .post(body.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    val resp = ChatApi.http.newCall(req).execute()
                    resp.use {
                        val txt = it.body?.string().orEmpty()
                        if (!it.isSuccessful) {
                            "Ошибка ${it.code}: " + txt.take(120)
                        } else {
                            "Связь работает! Ответ: " +
                                runCatching {
                                    org.json.JSONObject(txt)
                                        .optJSONArray("choices")?.optJSONObject(0)
                                        ?.optJSONObject("message")?.optString("content")
                                        ?.take(60)
                                }.getOrNull().orEmpty()
                        }
                    }
                }.getOrElse { "Нет связи: ${it.message}" }
            }
            checkingKey = false
            toastMessage = result
        }
    }

    fun fetchModels() {
        if (checkingKey) return
        viewModelScope.launch {
            checkingKey = true
            val res = ChatApi.listModels(settings)
            checkingKey = false
            res.onSuccess { list ->
                models = list
                if (list.isEmpty()) toastMessage = "Сервер не вернул модели"
                else showModelPicker = true
            }.onFailure {
                toastMessage = it.message ?: "Не удалось получить список моделей"
            }
        }
    }

    fun closeModelPicker() {
        showModelPicker = false
    }

    fun openModelPicker() {
        showModelPicker = true
    }

    fun chooseModel(id: String) {
        updateSettings(settings.copy(model = id))
        showModelPicker = false
    }

    fun tryDevLogin(password: String): Boolean {
        if (password == "Loti_Dev_2026!@#") {
            updateSettings(settings.copy(devMode = true))
            return true
        }
        return false
    }

    fun exitDevMode() {
        updateSettings(settings.copy(devMode = false))
    }

    fun registerAccount(user: String, pass: String): Boolean {
        if (user.isBlank() || pass.isBlank()) {
            toastMessage = "Введите логин и пароль"
            return false
        }
        updateSettings(settings.copy(
            accountUser = user,
            accountPass = pass,
            accountLoggedIn = true,
            firstRun = false
        ))
        val loaded = chatStore.load(user)
        chats = if (loaded.isEmpty()) listOf(Chat()) else loaded.sortedByDescending { it.updated }
        currentId = chats.first().id
        screen = "chat"
        toastMessage = "Аккаунт создан: $user"
        return true
    }

    fun loginAccount(user: String, pass: String): Boolean {
        if (user.isBlank() || pass.isBlank()) {
            toastMessage = "Введите логин и пароль"
            return false
        }
        if (user == settings.accountUser && pass == settings.accountPass) {
            updateSettings(settings.copy(accountLoggedIn = true, firstRun = false))
            val loaded = chatStore.load(user)
            chats = if (loaded.isEmpty()) listOf(Chat()) else loaded.sortedByDescending { it.updated }
            currentId = chats.first().id
            screen = "chat"
            toastMessage = "Добро пожаловать, $user"
            return true
        } else if (settings.accountUser.isNotBlank() && user != settings.accountUser) {
            toastMessage = "Аккаунт не найден. Зарегистрируйтесь."
            return false
        } else {
            toastMessage = "Неверный пароль"
            return false
        }
    }

    fun logoutAccount() {
        updateSettings(settings.copy(accountLoggedIn = false, accountUser = "", accountPass = ""))
        chats = listOf(Chat())
        currentId = chats.first().id
        screen = "chat"
        toastMessage = "Вышли из аккаунта"
    }

    private fun addDevLog(text: String) {
        val entry = "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $text"
        devLogs = devLogs + entry
    }

    private fun ensureChat(): String {
        val id = currentId
        if (id.isNotBlank() && chats.any { it.id == id }) return id
        val chat = Chat()
        chats = listOf(chat) + chats
        currentId = chat.id
        return chat.id
    }

    private fun titleFrom(text: String, atts: List<Attachment>): String {
        val base = text.ifBlank { if (atts.any { it.isImage }) "Изображение" else "Файл" }
        return base.replace('\n', ' ').take(40).ifBlank { "Новый чат" }
    }

    private fun userQuery(m: Message): String = buildString {
        append(m.text)
        m.attachments.filter { !it.isImage && it.text != null }.forEach {
            append(" ").append(it.text!!.take(500))
        }
        m.attachments.filter { it.isImage }.forEach { append(" ").append(it.name) }
    }.take(400)

    private fun updateChat(id: String, transform: (Chat) -> Chat) {
        chats = chats.map { c ->
            if (c.id == id) transform(c).copy(updated = System.currentTimeMillis()) else c
        }.sortedByDescending { it.updated }
    }

    private fun patchMessage(chatId: String, aiId: String, transform: (Message) -> Message) {
        chats = chats.map { c ->
            if (c.id != chatId) c
            else c.copy(messages = c.messages.map { m -> if (m.id == aiId) transform(m) else m })
        }
    }

    private fun setStatus(chatId: String, aiId: String, status: String?) {
        patchMessage(chatId, aiId) { it.copy(status = status) }
    }

    private fun persistNow() {
        val snapshot = chats
        viewModelScope.launch(Dispatchers.IO) { chatStore.save(snapshot, settings.accountUser) }
    }

    private fun launchReply(chatId: String, aiId: String, atts: List<Attachment>) {
        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            busy = true
            try {
                val chatNow = chats.firstOrNull { it.id == chatId } ?: return@launch
                val history = chatNow.messages.filter { it.id != aiId && !it.error }

                val model = settings.model

                if (settings.provider == PROVIDER_LOCAL) {
                    var delta = StringBuilder()
                    val response = localDemo(chatNow.messages.filter { it.role == Role.USER }.lastOrNull()?.text ?: "")
                    for (ch in response) {
                        delta.append(ch)
                        patchMessage(chatId, aiId) { it.copy(text = delta.toString(), streaming = true) }
                        withContext(Dispatchers.Main) {
                            kotlinx.coroutines.delay(30)
                        }
                    }
                    addDevLog("ЛОКАЛЬНАЯ МОДЕЛЬ: $response")
                    patchMessage(chatId, aiId) { it.copy(streaming = false, status = null) }
                    return@launch
                }

                var searchContext = ""
                if (searchEnabled) {
                    setStatus(chatId, aiId, "Ищу в интернете…")
                    val q = history.lastOrNull { it.role == Role.USER }?.let { userQuery(it) }.orEmpty()
                    searchContext = runCatching { WebSearch.search(q) }.getOrDefault("")
                    setStatus(chatId, aiId, if (searchContext.isBlank()) "Ничего не нашлось, отвечаю из знаний" else "Изучаю найденное…")
                }

                var delta = StringBuilder()
                var reason = StringBuilder()

                suspend fun attempt(images: Boolean) {
                    delta = StringBuilder()
                    reason = StringBuilder()
                    var lastFlush = 0L

                    fun flush(force: Boolean) {
                        val now = System.currentTimeMillis()
                        if (!force && now - lastFlush < 80) return
                        lastFlush = now
                        val d = delta.toString()
                        val r = reason.toString()
                        if (d.isEmpty() && r.isEmpty()) return
                        patchMessage(chatId, aiId) { it.copy(text = d, reasoning = r, status = null) }
                    }

                    ChatApi.stream(
                        settings = settings,
                        model = model,
                        history = history,
                        searchContext = searchContext,
                        thinking = thinking,
                        withImages = images,
                        onDelta = { s ->
                            delta.append(s)
                            flush(false)
                        },
                        onReasoning = { s ->
                            reason.append(s)
                            flush(false)
                        }
                    )
                    flush(true)
                }

                try {
                    attempt(true)
                } catch (e: ApiException) {
                    val errText = e.message?.take(300) ?: "Ошибка сети"
                    addDevLog("API error: $errText")
                    patchMessage(chatId, aiId) {
                        it.copy(
                            text = "",
                            streaming = false,
                            status = null,
                            error = true,
                            reasoning = ""
                        )
                    }
                    patchMessage(chatId, aiId) { m ->
                        m.copy(text = "⚠️ $errText")
                    }
                }

                patchMessage(chatId, aiId) { it.copy(streaming = false, status = null) }
            } catch (c: CancellationException) {
                patchMessage(chatId, aiId) { it.copy(streaming = false, status = null) }
            } catch (e: Exception) {
                val msg = e.message?.take(400) ?: "Ошибка сети"
                patchMessage(chatId, aiId) {
                    val base = it.text
                    it.copy(
                        streaming = false,
                        status = null,
                        error = base.isBlank(),
                        text = if (base.isBlank()) "Ошибка: $msg" else base + "\n\nОшибка: $msg"
                    )
                }
            } finally {
                busy = false
                sendJob = null
                persistNow()
            }
        }
    }

    private fun localDemo(text: String): String {
        val t = text.trim().lowercase()
        return when {
            t.contains("привет") || t.contains("здравств") || t.contains("хай") || t.contains("добрый") -> {
                val greetings = listOf(
                    "Привет! Я Loki, ваш личный ассистент. Чем могу помочь?",
                    "Здравствуйте! Готов ответить на вопросы.",
                    "Приветствую! Я Loki — бесплатный ИИ-ассистент."
                )
                greetings[(text.hashCode() and 0x7FFFFFFF) % greetings.size]
            }
            t.contains("код") || t.contains("программ") -> {
                "Я помогу с программированием. Напишите на каком языке нужен код."
            }
            t.contains("как") && t.contains("работает") -> {
                "Объясню подробно. Напишите, что именно вас интересует."
            }
            t.contains("напиши") || t.contains("составь") -> {
                "Я могу написать текст на любую тему. Укажите тему."
            }
            t.contains("план") || t.contains("совет") -> {
                "Я помогу составить план. Расскажите подробнее."
            }
            t.contains("кто ты") || t.contains("ты такой") -> {
                "Я Loki — бесплатный ИИ-ассистент."
            }
            t.contains("спасибо") || t.contains("благодар") -> {
                "Пожалуйста! Обращайтесь ещё."
            }
            t.contains("пока") || t.contains("до свидан") -> {
                "До встречи!"
            }
            else -> {
                "Понял ваш вопрос: «$text». Я Loki — бесплатный ИИ-ассистент."
            }
        }
    }

    private fun titleFrom(text: String, atts: List<Attachment>): String {
        val base = text.ifBlank { if (atts.any { it.isImage }) "Изображение" else "Файл" }
        return base.replace('\n', ' ').take(40).ifBlank { "Новый чат" }
    }

    private fun userQuery(m: Message): String = buildString {
        append(m.text)
        m.attachments.filter { !it.isImage && it.text != null }.forEach {
            append(" ").append(it.text!!.take(500))
        }
        m.attachments.filter { it.isImage }.forEach { append(" ").append(it.name) }
    }.take(400)
}
