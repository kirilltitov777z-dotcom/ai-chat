package io.margy.aichat.data

import android.content.Context

const val DEFAULT_SYSTEM = "Ты — Loki, дружелюбный и умный ассистент. Отвечай по делу, на русском языке, если не попросили иначе. Оформляй ответ Markdown-ом: списки, **жирный**, `код`."

const val PROVIDER_POLLINATIONS = "pollinations"
const val PROVIDER_GROQ = "groq"
const val PROVIDER_OPENROUTER = "openrouter"
const val PROVIDER_LOCAL = "local"
const val PROVIDER_CUSTOM = "custom"

const val DEFAULT_API_KEY = "sk-24bb60f46a5c4a108fcfad60aacd4270"
const val DEFAULT_MODEL = "openai"

val POLLINATIONS_PRESETS = listOf(
    "openai",
    "openai-fast",
    "openai-large",
    "gpt-oss-20b",
    "gpt-oss-120b",
    "mistral",
    "llama-roblox",
    "micronet",
    "qwen-coder"
)

val GROQ_PRESETS = listOf(
    "llama-3.1-8b-instant",
    "gemma2-9b-it",
    "llama-3.2-3b-preview",
    "llama-3.2-1b-preview",
    "mixtral-8x7b-32768"
)

val LOCAL_PRESETS = listOf("local-mini")

val MODEL_DESCRIPTIONS = mapOf(
    "openai" to "Универсальная · бесплатно",
    "openai-fast" to "Самый быстрый ответ",
    "openai-large" to "Больше знаний, медленнее",
    "gpt-oss-20b" to "Открытая модель OpenAI",
    "gpt-oss-120b" to "Самая умная, но тяжёлая",
    "mistral" to "Сильная математика",
    "llama-roblox" to "Лёгкая и быстрая",
    "micronet" to "Мини-размер, быстро",
    "qwen-coder" to "Специалист по коду",
    "llama-3.1-8b-instant" to "Мгновенный ответ",
    "gemma2-9b-it" to "От Google, быстрая",
    "llama-3.2-3b-preview" to "Лёгкая, шустрая",
    "llama-3.2-1b-preview" to "Самая лёгкая",
    "mixtral-8x7b-32768" to "Длинная память",
    "deepseek/deepseek-chat-v3-0324:free" to "Китайский DeepSeek",
    "deepseek/deepseek-r1:free" to "С рассуждениями",
    "google/gemini-2.0-flash-exp:free" to "От Google, быстрая",
    "google/gemini-2.5-flash-preview:free" to "Новая от Google",
    "meta-llama/llama-3.3-70b-instruct:free" to "Универсальная, умная",
    "qwen/qwen-2.5-72b-instruct:free" to "Мощная, с кодом",
    "qwen/qwen-2.5-vl-72b-instruct:free" to "Понимает картинки",
    "nvidia/nemotron-70b-instruct:free" to "От NVIDIA, точная",
    "mistralai/mistral-7b-instruct:free" to "Компактная, шустрая",
    "meta-llama/llama-3.1-8b-instruct:free" to "Лёгкая, быстрая",
    "local-mini" to "Без интернета · офлайн"
)

fun modelDescription(id: String): String = MODEL_DESCRIPTIONS[id] ?: ""

const val DEV_MODE_PASSWORD = "Loti_Dev_2026!@#"

data class AppSettings(
    val provider: String = PROVIDER_POLLINATIONS,
    val apiKey: String = "",
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
        PROVIDER_LOCAL -> LOCAL_PRESETS.first()
        else -> DEFAULT_MODEL
    }

    fun isDemo(): Boolean = provider == "demo"
    fun isLocal(): Boolean = provider == PROVIDER_LOCAL
    fun isPollinations(): Boolean = provider == PROVIDER_POLLINATIONS
}

class SettingsStore(context: Context) {
    private val sp = context.getSharedPreferences("lotichat_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        provider = sp.getString("provider", PROVIDER_POLLINATIONS) ?: PROVIDER_POLLINATIONS,
        apiKey = sp.getString("apiKey", "").orEmpty(),
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
