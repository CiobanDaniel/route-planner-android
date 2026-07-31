package com.danielcioban.routeplanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.danielcioban.routeplanner.data.settings.ThemeMode

/** Brand accent — warm orange used for buttons, highlights, and focus. */
object BrandColors {
    val orange = Color(0xFFE86A17)
    val orangeDark = Color(0xFFB84E0E)
    val orangeSoft = Color(0xFFF0A04B)
    val orangeMuted = Color(0xFFC45A12)
    val orangeDeep = Color(0xFF8F3A0A)
}

data class IslandPalette(
    val surface: Color,
    val surfaceElevated: Color,
    val darkShadow: Color,
    val lightShadow: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val fieldBorder: Color,
    val fieldBorderFocused: Color,
    val scrim: Color,
)

val LightIslandPalette = IslandPalette(
    surface = Color(0xFFF7F8FA),
    surfaceElevated = Color(0xFFFFFFFF),
    darkShadow = Color(0xFF7A8699),
    lightShadow = Color(0xFFFFFFFF),
    onSurface = Color(0xFF152033),
    onSurfaceMuted = Color(0xFF3A4658),
    fieldBorder = Color(0xFF9AA5B5),
    fieldBorderFocused = BrandColors.orange,
    scrim = Color(0x99000000),
)

val DarkIslandPalette = IslandPalette(
    surface = Color(0xFF243041),
    surfaceElevated = Color(0xFF2C3A4D),
    darkShadow = Color(0xFF000000),
    lightShadow = Color(0x33FFFFFF),
    onSurface = Color(0xFFE8EEF5),
    onSurfaceMuted = Color(0xFFB0BBC8),
    fieldBorder = Color(0xFF6B7788),
    fieldBorderFocused = BrandColors.orangeSoft,
    scrim = Color(0xCC000000),
)

val LocalIslandColors = staticCompositionLocalOf { LightIslandPalette }

/** Theme-aware island colors — read only from Compose. */
object IslandColors {
    val surface: Color
        @Composable @ReadOnlyComposable get() = LocalIslandColors.current.surface
    val surfaceElevated: Color
        @Composable @ReadOnlyComposable get() = LocalIslandColors.current.surfaceElevated
    val darkShadow: Color
        @Composable @ReadOnlyComposable get() = LocalIslandColors.current.darkShadow
    val lightShadow: Color
        @Composable @ReadOnlyComposable get() = LocalIslandColors.current.lightShadow
    val onSurface: Color
        @Composable @ReadOnlyComposable get() = LocalIslandColors.current.onSurface
    val onSurfaceMuted: Color
        @Composable @ReadOnlyComposable get() = LocalIslandColors.current.onSurfaceMuted
    val fieldBorder: Color
        @Composable @ReadOnlyComposable get() = LocalIslandColors.current.fieldBorder
    val fieldBorderFocused: Color
        @Composable @ReadOnlyComposable get() = LocalIslandColors.current.fieldBorderFocused
    val scrim: Color
        @Composable @ReadOnlyComposable get() = LocalIslandColors.current.scrim
}

private val LightColorScheme = lightColorScheme(
    primary = BrandColors.orange,
    onPrimary = Color.White,
    secondary = BrandColors.orangeMuted,
    onSecondary = Color.White,
    tertiary = BrandColors.orangeSoft,
    onTertiary = BrandColors.orangeDeep,
    background = Color(0xFFE8EEF5),
    onBackground = LightIslandPalette.onSurface,
    surface = LightIslandPalette.surfaceElevated,
    onSurface = LightIslandPalette.onSurface,
    surfaceVariant = Color(0xFFE8EDF4),
    onSurfaceVariant = LightIslandPalette.onSurfaceMuted,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandColors.orangeSoft,
    onPrimary = BrandColors.orangeDeep,
    secondary = BrandColors.orange,
    onSecondary = Color.White,
    tertiary = BrandColors.orangeMuted,
    onTertiary = Color.White,
    background = Color(0xFF121820),
    onBackground = DarkIslandPalette.onSurface,
    surface = DarkIslandPalette.surfaceElevated,
    onSurface = DarkIslandPalette.onSurface,
    surfaceVariant = Color(0xFF1E2836),
    onSurfaceVariant = DarkIslandPalette.onSurfaceMuted,
)

@Composable
fun RoutePlannerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val island = if (darkTheme) DarkIslandPalette else LightIslandPalette
    CompositionLocalProvider(LocalIslandColors provides island) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            content = content,
        )
    }
}
