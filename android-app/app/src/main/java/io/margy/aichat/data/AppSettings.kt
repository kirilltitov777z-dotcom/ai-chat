package io.margy.aichat.data

import android.content.Context

const val DEFAULT_SYSTEM = "Ты — дружелюбный и умный ассистент LotiChat. Отвечай по делу, на русском языке, если не попросили иначе. Оформляй ответ Markdown-ом: списки, **жирный**, `код`."

const val PROVIDER_OPENROUTER = "openrouter"
const val PROVIDER_DEEPSEEK = "deepseek"
const val PROVIDER_CUSTOM = "custom"

const val DEFAULT_OPENROUTER_MODEL = "deepseek/deepseek-chat-v3-0324:free"
const val DEFAULT_DEEPSEEK_MODEL = "deepseek-chat"

val MODEL_PRESETS = listOf(
    "deepseek/deepseek-chat-v3-0324:free",
    "deepseek/deepseek-r1:free",
    "google/gemini-2.0-flash-exp:free",
    "meta-llama/llama-3.3-70b-instruct:free",
    "qwen/qwen-2.5-72b-instruct:free"
)

val DEEPSEEK_PRESETS = listOf("deepseek-chat", "deepseek-reasoner")

/// 3 модели, которые доступны сразу после установки — пользователю не нужно ничего настраивать
val BUILTIN_FREE_MODELS = listOf(
    "deepseek/deepseek-chat-v3-0324:free",
    "deepseek/deepseek-r1:free",
    "google/gemini-2.0-flash-exp:free"
)

data class AppSettings(
    val provider: String = PROVIDER_OPENROUTER,
    val apiKey: String = "",
    val model: String = DEFAULT_OPENROUTER_MODEL,
    val baseUrl: String = "",
    val system: String = DEFAULT_SYSTEM,
    val temperature: Float = 0.7f,
    val dynamicColor: Boolean = true,
    val themeMode: String = "system",
    val accent: Int = 0,
    val sendImages: Boolean = true,
    val firstRun: Boolean = true
) {
    fun defaultModel(): String = when (provider) {
        PROVIDER_DEEPSEEK -> DEFAULT_DEEPSEEK_MODEL
        PROVIDER_CUSTOM -> ""
        else -> DEFAULT_OPENROUTER_MODEL
    }
}

class SettingsStore(context: Context) {
    private val sp = context.getSharedPreferences("lotichat_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        provider = sp.getString("provider", PROVIDER_OPENROUTER) ?: PROVIDER_OPENROUTER,
        apiKey = sp.getString("apiKey", "").orEmpty(),
        model = sp.getString("model", DEFAULT_OPENROUTER_MODEL).orEmpty(),
        baseUrl = sp.getString("baseUrl", "").orEmpty(),
        system = sp.getString("system", DEFAULT_SYSTEM) ?: DEFAULT_SYSTEM,
        temperature = sp.getFloat("temperature", 0.7f),
        dynamicColor = sp.getBoolean("dynamicColor", true),
        themeMode = sp.getString("themeMode", "system") ?: "system",
        accent = sp.getInt("accent", 0),
        sendImages = sp.getBoolean("sendImages", true),
        firstRun = sp.getBoolean("firstRun", true)
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
            .putBoolean("sendImages", s.sendImages)
            .putBoolean("firstRun", s.firstRun)
            .apply()
    }
}
