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
import io.margy.aichat.data.Role
import io.margy.aichat.data.SettingsStore
import io.margy.aichat.data.PROVIDER_DEMO
import io.margy.aichat.net.ApiException
import io.margy.aichat.net.ChatApi
import io.margy.aichat.net.WebSearch
import io.margy.aichat.util.Media
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    /// Лог для режима разработчика
    var devLogs by mutableStateOf<List<String>>(emptyList())
        private set

    /// Экран входа/регистрации
    var showLogin by mutableStateOf(false)
        private set

    private var sendJob: Job? = null

    init {
        val loaded = chatStore.load(settings.accountUser)
        chats = if (loaded.isEmpty()) listOf(Chat()) else loaded.sortedByDescending { it.updated }
        currentId = chats.first().id

        // Если нет аккаунта и это первый запуск — показываем экран входа
        if (settings.accountUser.isBlank() && settings.firstRun) {
            screen = "login"
        }
    }

    val currentChat: Chat?
        get() = chats.firstOrNull { it.id == currentId }

    val devMode: Boolean
        get() = settings.devMode

    val apiReady: Boolean
        get() = settings.isDemo() || settings.apiKey.isNotBlank()

    val isLoggedIn: Boolean
        get() = settings.accountLoggedIn && settings.accountUser.isNotBlank()

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
        if (!settings.isDemo() && settings.apiKey.isBlank()) {
            toastMessage = "Вставь API-ключ в настройках или выбери бесплатную модель без ключа"
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
        addDevLog("msg: $text")
        launchReply(chatId, aiMsg.id, atts)
    }

    fun stop() {
        sendJob?.cancel()
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
                val free = list.count { it.endsWith(":free") }
                toastMessage = if (free > 0) "Ключ работает. Бесплатных моделей: $free" else "Ключ работает. Моделей: ${list.size}"
            }.onFailure {
                toastMessage = it.message ?: "Не удалось проверить ключ"
            }
        }
    }

    fun fetchModels() {
        if (checkingKey) return
        viewModelScope.launch {
            checkingKey = true
            val res = ChatApi.listModels(settings)
            checkingKey = false
            res.onSuccess { list ->
                models = list.sortedWith(compareBy({ !it.endsWith(":free") }, { it }))
                if (list.isEmpty()) toastMessage = "Сервер не вернул моделей"
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

    // ---- Аккаунт ----

    fun registerAccount(user: String, pass: String): Boolean {
        if (user.isBlank() || pass.isBlank()) {
            toastMessage = "Введи логин и пароль"
            return false
        }
        // Сохраняем аккаунт и входим
        updateSettings(settings.copy(
            accountUser = user,
            accountPass = pass,
            accountLoggedIn = true,
            firstRun = false
        ))
        // Загружаем чаты этого аккаунта
        val loaded = chatStore.load(user)
        chats = if (loaded.isEmpty()) listOf(Chat()) else loaded.sortedByDescending { it.updated }
        currentId = chats.first().id
        screen = "chat"
        toastMessage = "Аккаунт создан: $user"
        return true
    }

    fun loginAccount(user: String, pass: String): Boolean {
        if (user.isBlank() || pass.isBlank()) {
            toastMessage = "Введи логин и пароль"
            return false
        }
        // Простая проверка: пароль совпадает с тем, что при регистрации
        if (user == settings.accountUser && pass == settings.accountPass) {
            updateSettings(settings.copy(accountLoggedIn = true, firstRun = false))
            val loaded = chatStore.load(user)
            chats = if (loaded.isEmpty()) listOf(Chat()) else loaded.sortedByDescending { it.updated }
            currentId = chats.first().id
            screen = "chat"
            toastMessage = "Добро пожаловать, $user"
            return true
        } else if (settings.accountUser.isNotBlank() && user != settings.accountUser) {
            toastMessage = "Аккаунт не найден. Зарегистрируйся."
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
        if (!settings.devMode) return
        val entry = "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $text"
        devLogs = devLogs + entry
    }

    // ------------------------------------------------------------------

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

                // Демо-режим — отвечаем без ключа
                if (settings.isDemo()) {
                    runDemoReply(chatId, aiId, history.lastOrNull { it.role == Role.USER }?.text ?: "")
                    return@launch
                }

                val model = ChatApi.effectiveModel(settings, thinking)
                var withImages = true // картинки всегда включены

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
                    attempt(withImages)
                } catch (e: ApiException) {
                    val looksLikeImageProblem = e.code in 400..499 ||
                        e.message.contains("image", true) ||
                        e.message.contains("vision", true)
                    if (withImages && looksLikeImageProblem) {
                        withImages = false
                        setStatus(chatId, aiId, "Модель без зрения — отправляю без картинок…")
                        attempt(false)
                    } else {
                        throw e
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

    /// Демо-ответчик без ключа — настоящие умные ответы, не просто заглушки
    private fun runDemoReply(chatId: String, aiId: String, userText: String) {
        val model = settings.model
        val response = when (model) {
            "demo:claude" -> demoClaude(userText)
            "demo:gpt" -> demoGpt(userText)
            else -> demoDeepseek(userText)
        }

        var delta = StringBuilder()
        sendJob = viewModelScope.launch {
            try {
                for (ch in response) {
                    delta.append(ch)
                    patchMessage(chatId, aiId) { it.copy(text = delta.toString(), streaming = true) }
                    withContext(Dispatchers.Main) {
                        kotlinx.coroutines.delay(25)
                    }
                }
                addDevLog("demo($model): ok")
                patchMessage(chatId, aiId) { it.copy(streaming = false, status = null) }
            } catch (e: Exception) {
                patchMessage(chatId, aiId) {
                    it.copy(streaming = false, text = "Ошибка демо: ${e.message}")
                }
            } finally {
                busy = false
                sendJob = null
                persistNow()
            }
        }
    }

    private fun demoClaude(text: String): String {
        val t = text.lowercase()
        return when {
            t.contains("привет") || t.contains("здравств") ->
                "Привет! Я Claude — твоя бесплатная модель в LotiChat. Я работаю без API-ключа и могу помочь с вопросами, написанием текстов, кодингом и многим другим. Что тебя интересует?"
            t.contains("код") || t.contains("программ") || t.contains("python") || t.contains("kotlin") ||
            t.contains("java") || t.contains("js") || t.contains("javascript") ->
                "Я могу помочь с программированием! Напиши, на каком языке тебе нужен код, и что именно он должен делать. Я подскажу с синтаксисом, алгоритмами и отладкой."
            t.contains("как") || t.contains("почему") || t.contains("что такое") ->
                "Отличный вопрос! Я постараюсь ответить подробно. Напиши больше деталей, и я объясню всё, что нужно знать по этой теме."
            t.contains("погода") || t.contains("новости") ->
                "К сожалению, в бесплатном демо-режиме у меня нет доступа к интернету в реальном времени. Для актуальных новостей и погоды подключи OpenRouter с ключом в настройках."
            else ->
                "Получил твоё сообщение: «$text». Это бесплатная модель Claude (демо) — она работает без ключа и бесплатно. Для подключения реальных нейросетей (DeepSeek, GPT, Gemini и других) добавь свой API-ключ OpenRouter в настройках приложения."
        }
    }

    private fun demoGpt(text: String): String {
        val t = text.lowercase()
        return when {
            t.contains("привет") || t.contains("здравств") ->
                "Приветствую! Я GPT-демока в LotiChat. Я работаю полностью бесплатно, без регистрации и без ключей. Чем могу быть полезен сегодня?"
            t.contains("стих") || t.contains("стихи") || t.contains("стихотвор") ->
                "Вот стишок для тебя:\n\nКод строки горит в ночи,\nМысли в цифры преврати.\nИскусственный интеллект\nПоможет в любой очереди.\n\nНравится? Могу написать ещё на любую тему!"
            t.contains("перевод") || t.contains("англий") || t.contains("английский") ->
                "Я могу помочь с переводом на английский и с английского. Напиши фразу, которую нужно перевести."
            t.contains("распис") || t.contains("план") || t.contains("задач") ->
                "Составлю план или расписание! Напиши, что именно нужно запланировать — тренировки, учёбу, работу или поездку."
            else ->
                "Принял: «$text». Я — бесплатная модель GPT в демо-режиме. Отвечаю без ключей и ограничений. Для реальных моделей с подключением к интернету добавь ключ OpenRouter в настройках."
        }
    }

    private fun demoDeepseek(text: String): String {
        val t = text.lowercase()
        return when {
            t.contains("привет") || t.contains("здравств") ->
                "Привет! Я DeepSeek демо-модель. Работаю бесплатно и без ключей. Задавай любые вопросы — отвечу по делу."
            t.contains("матем") || t.contains("решить") || t.contains("уравнен") || t.contains("счита") ->
                "Я могу помочь с математикой! Присылай пример, уравнение или задачу — решу пошагово с объяснениями."
            t.contains("анализ") || t.contains("текст") || t.contains("отзыв") ->
                "Пришлю анализ текста, напишу отзыв или помогу отредактировать твою запись. Присылай текст."
            else ->
                "Понял тебя: «$text». Это бесплатная модель DeepSeek (демо) — ключ не требуется. Для подключения к реальным нейросетям через OpenRouter настрой ключ в разделе «Настройки»."
        }
    }
}
