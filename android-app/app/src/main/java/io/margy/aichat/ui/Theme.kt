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
    Color(0xFF6750A4), // фиолетовый (Material You)
    Color(0xFF00696E), // бирюза
    Color(0xFF8B5000), // янтарь
    Color(0xFF9C4146), // роза
    Color(0xFF00658F), // небо
    Color(0xFF3B6939)  // зелёный
)

val ACCENT_NAMES = listOf("Фиолетовый", "Бирюза", "Янтарь", "Роза", "Небо", "Зелёный")

private fun lightScheme(seed: Color) = lightColorScheme(
    primary = seed,
    onPrimary = Color.White,
    primaryContainer = lerp(Color.White, seed, 0.24f),
    onPrimaryContainer = lerp(seed, Color.Black, 0.55f),
    secondary = lerp(seed, Color.Black, 0.18f),
    onSecondary = Color.White,
    secondaryContainer = lerp(Color.White, seed, 0.16f),
    onSecondaryContainer = lerp(seed, Color.Black, 0.55f),
    tertiary = lerp(seed, Color(0xFFFFB74D), 0.65f),
    surface = Color(0xFFFFFBFE),
    background = Color(0xFFFFFBFE),
    surfaceVariant = lerp(Color.White, seed, 0.10f),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = lerp(Color(0xFFFFFBFE), seed, 0.03f),
    surfaceContainer = lerp(Color(0xFFFFFBFE), seed, 0.06f),
    surfaceContainerHigh = lerp(Color(0xFFFFFBFE), seed, 0.09f),
    surfaceContainerHighest = lerp(Color(0xFFFFFBFE), seed, 0.12f)
)

private fun darkScheme(seed: Color) = darkColorScheme(
    primary = lerp(seed, Color.White, 0.55f),
    onPrimary = lerp(seed, Color.Black, 0.72f),
    primaryContainer = lerp(seed, Color.Black, 0.35f),
    onPrimaryContainer = lerp(seed, Color.White, 0.85f),
    secondary = lerp(seed, Color.White, 0.62f),
    onSecondary = lerp(seed, Color.Black, 0.7f),
    secondaryContainer = lerp(seed, Color.Black, 0.5f),
    onSecondaryContainer = lerp(seed, Color.White, 0.82f),
    tertiary = lerp(seed, Color(0xFFFFCC80), 0.6f),
    surface = Color(0xFF141218),
    background = Color(0xFF141218),
    surfaceVariant = lerp(Color(0xFF141218), seed, 0.18f),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = lerp(Color(0xFF141218), seed, 0.05f),
    surfaceContainer = lerp(Color(0xFF141218), seed, 0.08f),
    surfaceContainerHigh = lerp(Color(0xFF141218), seed, 0.13f),
    surfaceContainerHighest = lerp(Color(0xFF141218), seed, 0.18f)
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
