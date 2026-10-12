package com.laboratoriofisico.app.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PetrolDark,
    onPrimary = Color.White,
    primaryContainer = PetrolLight,
    onPrimaryContainer = Color.White,
    secondary = PetrolMedium,
    onSecondary = Color.White,
    tertiary = TealSuccess,
    onTertiary = Color.White,
    background = SmokeWhite,
    onBackground = Graphite,
    surface = Color.White,
    onSurface = Graphite,
    onSurfaceVariant = GraphiteMedium,
    error = DarkRedError,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = PetrolLight,
    onPrimary = Color.White,
    primaryContainer = PetrolDark,
    onPrimaryContainer = Color.White,
    secondary = PetrolMedium,
    onSecondary = Color.White,
    tertiary = TealSuccess,
    onTertiary = Color.White,
    background = SurfaceDark,
    onBackground = SmokeWhite,
    surface = SurfaceDarkElevated,
    onSurface = SmokeWhite,
    onSurfaceVariant = GraphiteMedium,
    error = DarkRedError,
    onError = Color.White
)

@Composable
fun LaboratorioFisicoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
