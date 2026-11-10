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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val ACCENTS = listOf(
    Color(0xFF7C3AED), // фиолетовый
    Color(0xFF2563EB), // синий
    Color(0xFF0891B2), // бирюзовый
    Color(0xFF059669), // зелёный
    Color(0xFFD97706), // янтарный
    Color(0xFFDB2777)  // розовый
)

val ACCENT_NAMES = listOf("Фиолетовый", "Синий", "Бирюзовый", "Зелёный", "Янтарный", "Розовый")

private fun lightScheme(seed: Color) = lightColorScheme(
    primary = seed,
    onPrimary = Color.White,
    primaryContainer = lerp(Color(0xFFFFFFFF), seed, 0.14f),
    onPrimaryContainer = lerp(seed, Color(0xFF000000), 0.72f),
    secondary = lerp(seed, Color(0xFF000000), 0.22f),
    onSecondary = Color.White,
    secondaryContainer = lerp(Color(0xFFFFFFFF), seed, 0.08f),
    onSecondaryContainer = lerp(seed, Color(0xFF000000), 0.72f),
    tertiary = lerp(seed, Color(0xFFF59E0B), 0.55f),
    onTertiary = Color.White,
    tertiaryContainer = lerp(Color(0xFFFFFFFF), Color(0xFFF59E0B), 0.22f),
    onTertiaryContainer = Color(0xFF3B2600),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF1C1B20),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = lerp(Color(0xFFFFFFFF), seed, 0.07f),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceTint = seed,
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF7),
    inversePrimary = lerp(seed, Color.White, 0.75f),
    outline = Color(0xFF79747E),
    outlineVariant = lerp(Color(0xFFFFFFFF), seed, 0.14f),
    scrim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = lerp(Color(0xFFFCF8FF), seed, 0.02f),
    surfaceContainer = lerp(Color(0xFFFCF8FF), seed, 0.045f),
    surfaceContainerHigh = lerp(Color(0xFFFCF8FF), seed, 0.07f),
    surfaceContainerHighest = lerp(Color(0xFFFCF8FF), seed, 0.10f)
)

private fun darkScheme(seed: Color) = darkColorScheme(
    primary = lerp(seed, Color.White, 0.45f),
    onPrimary = lerp(seed, Color.Black, 0.80f),
    primaryContainer = lerp(seed, Color.Black, 0.28f),
    onPrimaryContainer = lerp(seed, Color.White, 0.92f),
    secondary = lerp(seed, Color.White, 0.50f),
    onSecondary = Color.Black,
    secondaryContainer = lerp(seed, Color.Black, 0.32f),
    onSecondaryContainer = lerp(seed, Color.White, 0.90f),
    tertiary = lerp(seed, Color(0xFFFFD08A), 0.55f),
    onTertiary = lerp(seed, Color.Black, 0.80f),
    tertiaryContainer = lerp(seed, Color.Black, 0.30f),
    onTertiaryContainer = lerp(seed, Color(0xFFFFD08A), 0.95f),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121016),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF121016),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = lerp(Color(0xFF121016), seed, 0.13f),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceTint = lerp(seed, Color.White, 0.45f),
    inverseSurface = Color(0xFFE6E1E9),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = seed,
    outline = Color(0xFF938F99),
    outlineVariant = lerp(Color(0xFF121016), seed, 0.20f),
    scrim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF0C0A10),
    surfaceContainerLow = Color(0xFF1A171F),
    surfaceContainer = Color(0xFF1E1B24),
    surfaceContainerHigh = Color(0xFF282530),
    surfaceContainerHighest = Color(0xFF33303B)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 10.sp, lineHeight = 14.sp)
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
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
