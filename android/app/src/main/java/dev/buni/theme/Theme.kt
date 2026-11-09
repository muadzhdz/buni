package dev.buni.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class BuniPalette(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val borderLight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val isDark: Boolean
)

val DarkPalette = BuniPalette(
    background = Black,
    surface = Zinc950,
    surfaceVariant = Zinc900,
    border = Zinc800,
    borderLight = Zinc700,
    textPrimary = White,
    textSecondary = Zinc400,
    textMuted = Zinc600,
    accent = Emerald500,
    isDark = true
)

val LightPalette = BuniPalette(
    background = Zinc50,
    surface = White,
    surfaceVariant = Zinc100,
    border = Zinc300,
    borderLight = Zinc200,
    textPrimary = Zinc950,
    textSecondary = Zinc600,
    textMuted = Zinc400,
    accent = Emerald500,
    isDark = false
)

val LocalBuniColors = staticCompositionLocalOf { DarkPalette }

val buniColors: BuniPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalBuniColors.current

private val BuniDarkColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    background = Black,
    onBackground = White,
    surface = Zinc950,
    onSurface = White,
    surfaceVariant = Zinc900,
    onSurfaceVariant = Zinc400,
    outline = Zinc700
)

private val BuniLightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    background = Zinc50,
    onBackground = Zinc950,
    surface = White,
    onSurface = Zinc950,
    surfaceVariant = Zinc100,
    onSurfaceVariant = Zinc600,
    outline = Zinc300
)

@Composable
fun BuniTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkPalette else LightPalette
    val colorScheme = if (darkTheme) BuniDarkColorScheme else BuniLightColorScheme

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalBuniColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
