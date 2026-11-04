package dev.buni.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BuniColorScheme = darkColorScheme(
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

@Composable
fun BuniTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BuniColorScheme,
        content = content
    )
}
