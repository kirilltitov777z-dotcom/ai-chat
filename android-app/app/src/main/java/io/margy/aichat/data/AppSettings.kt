package io.margy.aichat.data

import android.content.Context

const val DEFAULT_SYSTEM = "Ты — Loki, дружелюбный и умный ассистент. Отвечай по делу, на русском языке, если не попросили иначе. Оформляй ответ Markdown-ом: списки, **жирный**, `код`."

const val PROVIDER_OPENROUTER = "openrouter"
const val PROVIDER_GROQ = "groq"
const val PROVIDER_DEEPSEEK = "deepseek"
const val PROVIDER_CUSTOM = "custom"

// Встроенный бесплатный ключ OpenRouter — работает сразу после установки
const val DEFAULT_API_KEY = "sk-24bb60f46a5c4a108fcfad60aacd4270"

// Модель по умолчанию называется Loki
const val DEFAULT_MODEL = "deepseek/deepseek-chat-v3-0324:free"

// Бесплатные модели Groq
val GROQ_PRESETS = listOf(
    "llama-3.1-8b-instant",
    "gemma2-9b-it",
    "llama-3.2-3b-preview",
    "llama-3.2-1b-preview",
    "mixtral-8x7b-32768"
)

// Демо-модели — динамические ответы без ключа (не заготовки, а генерация на лету)
val BUILTIN_FREE_MODELS = listOf(
    "demo:claude",
    "demo:gpt",
    "demo:deepseek"
)

const val DEV_MODE_PASSWORD = "Loti_Dev_2026!@#"

data class AppSettings(
    val provider: String = PROVIDER_GROQ,
    val apiKey: String = DEFAULT_API_KEY,
    val model: String = DEFAULT_MODEL,
    val baseUrl: String = "",
    val system: String = DEFAULT_SYSTEM,
    val temperature: Float = 0.7f,
    val dynamicColor: Boolean = true,
    val themeMode: String = "system",
    val accent: Int = 0,
    val firstRun: Boolean = true,
    val devMode: Boolean = false,
    val accountUser: String = "",
    val accountPass: String = "",
    val accountLoggedIn: Boolean = false
) {
    fun defaultModel(): String = when (provider) {
        PROVIDER_GROQ -> GROQ_PRESETS.first()
        else -> DEFAULT_MODEL
    }

    fun isDemo(): Boolean = provider == "demo"
}

class SettingsStore(context: Context) {
    private val sp = context.getSharedPreferences("lotichat_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        provider = sp.getString("provider", PROVIDER_OPENROUTER) ?: PROVIDER_OPENROUTER,
        apiKey = sp.getString("apiKey", DEFAULT_API_KEY).orEmpty(),
        model = sp.getString("model", DEFAULT_MODEL).orEmpty(),
        baseUrl = sp.getString("baseUrl", "").orEmpty(),
        system = sp.getString("system", DEFAULT_SYSTEM) ?: DEFAULT_SYSTEM,
        temperature = sp.getFloat("temperature", 0.7f),
        dynamicColor = sp.getBoolean("dynamicColor", true),
        themeMode = sp.getString("themeMode", "system") ?: "system",
        accent = sp.getInt("accent", 0),
        firstRun = sp.getBoolean("firstRun", true),
        devMode = sp.getBoolean("devMode", false),
        accountUser = sp.getString("accountUser", "").orEmpty(),
        accountPass = sp.getString("accountPass", "").orEmpty(),
        accountLoggedIn = sp.getBoolean("accountLoggedIn", false)
    )

    fun save(s: AppSettings) {
        sp.edit()
            .putString("provider", s.provider)
            .putString("apiKey", s.apiKey)
            .putString("model", s.model)
            .putString("baseUrl", s.baseUrl)
            .putString("system", s.system)
            .putFloat("temperature", s.temperature)
            .putBoolean("dynamicColor", s.dynamicColor)
            .putString("themeMode", s.themeMode)
            .putInt("accent", s.accent)
            .putBoolean("firstRun", s.firstRun)
            .putBoolean("devMode", s.devMode)
            .putString("accountUser", s.accountUser)
            .putString("accountPass", s.accountPass)
            .putBoolean("accountLoggedIn", s.accountLoggedIn)
            .apply()
    }
}
