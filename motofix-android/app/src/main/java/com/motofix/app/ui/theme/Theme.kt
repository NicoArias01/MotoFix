package com.motofix.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MotoFixColors = darkColorScheme(
    primary = Orange,
    onPrimary = OnOrange,
    secondary = Cyan,
    onSecondary = Background,
    tertiary = Green,
    background = Background,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceRaised,
    outline = Border,
    error = ErrorRed,
)

/** La app siempre usa la interfaz oscura (RNF03). */
@Composable
fun MotoFixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MotoFixColors,
        typography = MotoFixTypography,
        content = content,
    )
}
