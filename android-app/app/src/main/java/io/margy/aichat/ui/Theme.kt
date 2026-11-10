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
    Color(0xFF15803D), // зелёный (основной)
    Color(0xFF059669), // изумруд
    Color(0xFFDC2626), // красный
    Color(0xFF0891B2), // синий
    Color(0xFFD97706), // оранжевый
    Color(0xFF7C3AED)  // фиолетовый
)

val ACCENT_NAMES = listOf("Зелёный", "Изумруд", "Красный", "Синий", "Оранж", "Фиолет")

private fun lightScheme(seed: Color) = lightColorScheme(
    primary = seed,
    onPrimary = Color.White,
    primaryContainer = lerp(Color.White, seed, 0.10f),
    onPrimaryContainer = lerp(seed, Color.Black, 0.70f),
    secondary = lerp(seed, Color.Black, 0.08f),
    onSecondary = Color.White,
    secondaryContainer = lerp(Color.White, seed, 0.06f),
    onSecondaryContainer = lerp(seed, Color.Black, 0.70f),
    tertiary = lerp(seed, Color(0xFFF59E0B), 0.45f),
    surface = Color(0xFFF0FDF4),
    background = Color(0xFFF0FDF4),
    surfaceVariant = lerp(Color.White, seed, 0.05f),
    onSurfaceVariant = Color(0xFF1F2937),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = lerp(Color(0xFFF0FDF4), seed, 0.01f),
    surfaceContainer = lerp(Color(0xFFF0FDF4), seed, 0.03f),
    surfaceContainerHigh = lerp(Color(0xFFF0FDF4), seed, 0.05f),
    surfaceContainerHighest = lerp(Color(0xFFF0FDF4), seed, 0.08f)
)

private fun darkScheme(seed: Color) = darkColorScheme(
    primary = lerp(seed, Color.White, 0.35f),
    onPrimary = Color.Black,
    primaryContainer = lerp(seed, Color.Black, 0.15f),
    onPrimaryContainer = lerp(seed, Color.White, 0.95f),
    secondary = lerp(seed, Color.White, 0.40f),
    onSecondary = Color.Black,
    secondaryContainer = lerp(seed, Color.Black, 0.20f),
    onSecondaryContainer = lerp(seed, Color.White, 0.93f),
    tertiary = lerp(seed, Color(0xFFFCD34D), 0.40f),
    surface = Color(0xFF064E3B),
    background = Color(0xFF064E3B),
    surfaceVariant = lerp(Color(0xFF064E3B), seed, 0.10f),
    onSurfaceVariant = Color(0xFFBBF7D0),
    surfaceContainerLowest = Color(0xFF022C22),
    surfaceContainerLow = lerp(Color(0xFF064E3B), seed, 0.02f),
    surfaceContainer = lerp(Color(0xFF064E3B), seed, 0.04f),
    surfaceContainerHigh = lerp(Color(0xFF064E3B), seed, 0.06f),
    surfaceContainerHighest = lerp(Color(0xFF064E3B), seed, 0.09f)
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
