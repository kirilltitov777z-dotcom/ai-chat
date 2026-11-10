# Compose + OkHttp + kotlinx.coroutines
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Сохраняем имена наших data-классов на всякий случай
-keep class io.margy.aichat.data.** { *; }
-keep class io.margy.aichat.net.** { *; }

# Kotlin metadata
-keep class kotlin.Metadata { *; }
