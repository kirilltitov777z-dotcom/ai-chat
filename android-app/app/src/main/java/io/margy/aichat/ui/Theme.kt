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
    Color(0xFF1E3A8A), // темно-синий
    Color(0xFF7C3AED), // фиолетовый
    Color(0xFFBE185D), // розовый
    Color(0xFF0891B2), // cyan
    Color(0xFF15803D), // зелёный
    Color(0xFFD97706)  // оранжевый
)

val ACCENT_NAMES = listOf("Синий", "Фиолет", "Роза", "Cyan", "Зелёный", "Оранж")

private fun lightScheme(seed: Color) = lightColorScheme(
    primary = seed,
    onPrimary = Color.White,
    primaryContainer = lerp(Color.White, seed, 0.12f),
    onPrimaryContainer = lerp(seed, Color.Black, 0.65f),
    secondary = lerp(seed, Color.Black, 0.10f),
    onSecondary = Color.White,
    secondaryContainer = lerp(Color.White, seed, 0.08f),
    onSecondaryContainer = lerp(seed, Color.Black, 0.65f),
    tertiary = lerp(seed, Color(0xFFF59E0B), 0.5f),
    surface = Color(0xFFFAFAFA),
    background = Color(0xFFFAFAFA),
    surfaceVariant = lerp(Color.White, seed, 0.06f),
    onSurfaceVariant = Color(0xFF374151),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = lerp(Color(0xFFFAFAFA), seed, 0.015f),
    surfaceContainer = lerp(Color(0xFFFAFAFA), seed, 0.04f),
    surfaceContainerHigh = lerp(Color(0xFFFAFAFA), seed, 0.07f),
    surfaceContainerHighest = lerp(Color(0xFFFAFAFA), seed, 0.10f)
)

private fun darkScheme(seed: Color) = darkColorScheme(
    primary = lerp(seed, Color.White, 0.4f),
    onPrimary = Color.Black,
    primaryContainer = lerp(seed, Color.Black, 0.2f),
    onPrimaryContainer = lerp(seed, Color.White, 0.92f),
    secondary = lerp(seed, Color.White, 0.45f),
    onSecondary = Color.Black,
    secondaryContainer = lerp(seed, Color.Black, 0.25f),
    onSecondaryContainer = lerp(seed, Color.White, 0.90f),
    tertiary = lerp(seed, Color(0xFFFCD34D), 0.45f),
    surface = Color(0xFF0F172A),
    background = Color(0xFF0F172A),
    surfaceVariant = lerp(Color(0xFF0F172A), seed, 0.12f),
    onSurfaceVariant = Color(0xFFCBD5E1),
    surfaceContainerLowest = Color(0xFF020617),
    surfaceContainerLow = lerp(Color(0xFF0F172A), seed, 0.03f),
    surfaceContainer = lerp(Color(0xFF0F172A), seed, 0.06f),
    surfaceContainerHigh = lerp(Color(0xFF0F172A), seed, 0.09f),
    surfaceContainerHighest = lerp(Color(0xFF0F172A), seed, 0.12f)
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
