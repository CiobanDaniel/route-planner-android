package com.danielcioban.routeplanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RouteBlue = Color(0xFF0F4C81)
private val RouteBlueDark = Color(0xFF082F4F)
private val AccentTeal = Color(0xFF1B7F7A)
private val SurfaceLight = Color(0xFFF5F7FA)
private val SurfaceDark = Color(0xFF121820)

private val LightColorScheme = lightColorScheme(
    primary = RouteBlue,
    onPrimary = Color.White,
    secondary = AccentTeal,
    onSecondary = Color.White,
    background = SurfaceLight,
    onBackground = Color(0xFF1A2330),
    surface = Color.White,
    onSurface = Color(0xFF1A2330),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6BA3D6),
    onPrimary = RouteBlueDark,
    secondary = Color(0xFF5CBDB8),
    onSecondary = Color(0xFF003735),
    background = SurfaceDark,
    onBackground = Color(0xFFE8EEF5),
    surface = Color(0xFF1A2330),
    onSurface = Color(0xFFE8EEF5),
)

@Composable
fun RoutePlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content,
    )
}
