package io.margy.aichat.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val ACCENTS = listOf(
    Color(0xFF7B61FF), // неоновый фиолетовый
    Color(0xFF00D9C0), // мятный неон
    Color(0xFFFF6B6B), // коралловый
    Color(0xFF4ECDC4), // бирюзовый неон
    Color(0xFFFFA726), // оранжевый неон
    Color(0xFF8E44AD)  // глубокий фиолетовый
)

val ACCENT_NAMES = listOf("Неон", "Мятный", "Коралл", "Бирюза", "Оранж", "Фиолет")

private fun lightScheme(seed: Color) = lightColorScheme(
    primary = seed,
    onPrimary = Color.White,
    primaryContainer = lerp(Color.White, seed, 0.15f),
    onPrimaryContainer = lerp(seed, Color.Black, 0.60f),
    secondary = lerp(seed, Color.Black, 0.12f),
    onSecondary = Color.White,
    secondaryContainer = lerp(Color.White, seed, 0.10f),
    onSecondaryContainer = lerp(seed, Color.Black, 0.60f),
    tertiary = lerp(seed, Color(0xFFFFB74D), 0.55f),
    surface = Color(0xFFF8F9FF),
    background = Color(0xFFF8F9FF),
    surfaceVariant = lerp(Color.White, seed, 0.08f),
    onSurfaceVariant = Color(0xFF4A4A5A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = lerp(Color(0xFFF8F9FF), seed, 0.02f),
    surfaceContainer = lerp(Color(0xFFF8F9FF), seed, 0.05f),
    surfaceContainerHigh = lerp(Color(0xFFF8F9FF), seed, 0.08f),
    surfaceContainerHighest = lerp(Color(0xFFF8F9FF), seed, 0.11f)
)

private fun darkScheme(seed: Color) = darkColorScheme(
    primary = lerp(seed, Color.White, 0.45f),
    onPrimary = Color.Black,
    primaryContainer = lerp(seed, Color.Black, 0.25f),
    onPrimaryContainer = lerp(seed, Color.White, 0.90f),
    secondary = lerp(seed, Color.White, 0.50f),
    onSecondary = Color.Black,
    secondaryContainer = lerp(seed, Color.Black, 0.30f),
    onSecondaryContainer = lerp(seed, Color.White, 0.88f),
    tertiary = lerp(seed, Color(0xFFFFCC80), 0.50f),
    surface = Color(0xFF0A0A0F),
    background = Color(0xFF0A0A0F),
    surfaceVariant = lerp(Color(0xFF0A0A0F), seed, 0.15f),
    onSurfaceVariant = Color(0xFFB8B8C8),
    surfaceContainerLowest = Color(0xFF050508),
    surfaceContainerLow = lerp(Color(0xFF0A0A0F), seed, 0.04f),
    surfaceContainer = lerp(Color(0xFF0A0A0F), seed, 0.07f),
    surfaceContainerHigh = lerp(Color(0xFF0A0A0F), seed, 0.11f),
    surfaceContainerHighest = lerp(Color(0xFF0A0A0F), seed, 0.15f)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun LotiChatTheme(
    darkTheme: Boolean,
    dynamic: Boolean,
    accent: Int,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val seed = ACCENTS[accent.coerceIn(0, ACCENTS.lastIndex)]
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> darkScheme(seed)
        else -> lightScheme(seed)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        shapes = AppShapes,
        content = content
    )
}
