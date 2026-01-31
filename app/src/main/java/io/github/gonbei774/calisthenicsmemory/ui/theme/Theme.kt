package io.github.gonbei774.calisthenicsmemory.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Blue600,
    secondary = Green600,
    tertiary = Purple600,
    background = Slate900,
    surface = Slate800,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    error = Red600,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Blue600,
    secondary = Green600,
    tertiary = Purple600,
    background = Color.White,
    surface = Slate50,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Slate800,
    onSurface = Slate800,
    error = Red600,
    onError = Color.White
)

@Immutable
data class AppColors(
    val background: Color,
    val backgroundGradientStart: Color,
    val backgroundGradientEnd: Color,
    val cardBackground: Color,
    val cardBackgroundSelected: Color,
    val cardBackgroundSecondary: Color,
    val cardBackgroundDisabled: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val border: Color,
    val borderFocused: Color,
    val divider: Color,
    val switchTrack: Color,
    val switchThumb: Color,
    val isDark: Boolean
)

private val DarkAppColors = AppColors(
    background = Slate900,
    backgroundGradientStart = Slate900,
    backgroundGradientEnd = Slate800,
    cardBackground = Slate800,
    cardBackgroundSelected = Slate750,
    cardBackgroundSecondary = Slate700,
    cardBackgroundDisabled = Slate700,
    textPrimary = Color.White,
    textSecondary = Slate400,
    textTertiary = Slate300,
    textDisabled = Slate500,
    border = Slate600,
    borderFocused = Blue600,
    divider = Slate700,
    switchTrack = Slate500,
    switchThumb = Color.White,
    isDark = true
)

private val LightAppColors = AppColors(
    background = Color.White,
    backgroundGradientStart = Color.White,
    backgroundGradientEnd = Slate50,
    cardBackground = Slate50,
    cardBackgroundSelected = Slate100,
    cardBackgroundSecondary = Slate100,
    cardBackgroundDisabled = Slate200,
    textPrimary = Slate800,
    textSecondary = Slate500,
    textTertiary = Slate600,
    textDisabled = Slate400,
    border = Slate300,
    borderFocused = Blue600,
    divider = Slate200,
    switchTrack = Slate300,
    switchThumb = Color.White,
    isDark = false
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

@Composable
fun CalisthenicsMemoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}