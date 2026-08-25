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

/** Brand accent — warm orange for primary actions and selection, not every label. */
object BrandColors {
    val orange = Color(0xFFE86A17)
    val orangeDark = Color(0xFFB84E0E)
    val orangeSoft = Color(0xFFE8A05A)
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
    val rowSurface: Color,
    val scrim: Color,
    /** Metadata chips (library, van, origin) — not the orange CTA. */
    val badge: Color,
    val success: Color,
    val noteFill: Color,
    val progress: Color,
    /** Neumorphic light highlight — looks like a white halo in dark mode; keep off there. */
    val useHighlightShadow: Boolean = true,
)

val LightIslandPalette = IslandPalette(
    surface = Color(0xFFF4F6F8),
    surfaceElevated = Color(0xFFFAFBFC),
    darkShadow = Color(0xFF6B7380),
    lightShadow = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A2332),
    onSurfaceMuted = Color(0xFF5C6776),
    fieldBorder = Color(0xFFC5CDD6),
    fieldBorderFocused = BrandColors.orange,
    rowSurface = Color(0xFFF0F3F6),
    scrim = Color(0x8A000000),
    badge = Color(0xFF3D5C73),
    success = Color(0xFF2E7D4F),
    noteFill = Color(0xFFEEF2F6),
    progress = Color(0xFF3D6B8A),
    useHighlightShadow = true,
)

val DarkIslandPalette = IslandPalette(
    surface = Color(0xFF1E2836),
    surfaceElevated = Color(0xFF273243),
    darkShadow = Color(0xFF000000),
    lightShadow = Color(0x00000000),
    onSurface = Color(0xFFF0F3F7),
    onSurfaceMuted = Color(0xFFA8B4C2),
    fieldBorder = Color(0xFF4A5868),
    fieldBorderFocused = BrandColors.orangeSoft,
    rowSurface = Color(0xFF1C2633),
    scrim = Color(0xB3000000),
    badge = Color(0xFF9BB4C8),
    success = Color(0xFF7BC49A),
    noteFill = Color(0xFF1A232E),
    progress = Color(0xFF7EA3C0),
    useHighlightShadow = false,
)

val LocalIslandColors = staticCompositionLocalOf { LightIslandPalette }

/** Skip or shorten UI motion when the user asked for it in Settings. */
val LocalReduceMotion = staticCompositionLocalOf { false }

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
    val rowSurface: Color
        @Composable @ReadOnlyComposable get() = LocalIslandColors.current.rowSurface
    val scrim: Color
        @Composable @ReadOnlyComposable get() = LocalIslandColors.current.scrim
    val badge: Color
        @Composable @ReadOnlyComposable get() = LocalIslandColors.current.badge
    val success: Color
        @Composable @ReadOnlyComposable get() = LocalIslandColors.current.success
    val noteFill: Color
        @Composable @ReadOnlyComposable get() = LocalIslandColors.current.noteFill
    val progress: Color
        @Composable @ReadOnlyComposable get() = LocalIslandColors.current.progress
    val useHighlightShadow: Boolean
        @Composable @ReadOnlyComposable get() = LocalIslandColors.current.useHighlightShadow
}

private val LightColorScheme = lightColorScheme(
    primary = BrandColors.orange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6E4D4),
    onPrimaryContainer = BrandColors.orangeDeep,
    secondary = Color(0xFF4A5C6E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEF1F5),
    onSecondaryContainer = Color(0xFF1A2332),
    tertiary = BrandColors.orangeMuted,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8EEF4),
    onTertiaryContainer = Color(0xFF1A2332),
    background = Color(0xFFE6ECF2),
    onBackground = LightIslandPalette.onSurface,
    surface = LightIslandPalette.surfaceElevated,
    onSurface = LightIslandPalette.onSurface,
    surfaceVariant = Color(0xFFE8EDF2),
    onSurfaceVariant = LightIslandPalette.onSurfaceMuted,
    error = Color(0xFFC24732),
    onError = Color.White,
    errorContainer = Color(0xFFF8E0DA),
    onErrorContainer = Color(0xFF6B2418),
    outline = LightIslandPalette.fieldBorder,
    outlineVariant = Color(0xFFD8DEE6),
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandColors.orange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D2A1C),
    onPrimaryContainer = Color(0xFFF3D2B4),
    secondary = Color(0xFF9BB0C2),
    onSecondary = Color(0xFF15202C),
    secondaryContainer = Color(0xFF323E4E),
    onSecondaryContainer = Color(0xFFF0F3F7),
    tertiary = BrandColors.orangeSoft,
    onTertiary = Color(0xFF2A1A0C),
    tertiaryContainer = Color(0xFF3A2818),
    onTertiaryContainer = Color(0xFFF3D2B4),
    background = Color(0xFF121820),
    onBackground = DarkIslandPalette.onSurface,
    surface = DarkIslandPalette.surfaceElevated,
    onSurface = DarkIslandPalette.onSurface,
    surfaceVariant = Color(0xFF1E2836),
    onSurfaceVariant = DarkIslandPalette.onSurfaceMuted,
    error = Color(0xFFE8A090),
    onError = Color(0xFF4A1810),
    errorContainer = Color(0xFF5A2A22),
    onErrorContainer = Color(0xFFF5D0C8),
    outline = DarkIslandPalette.fieldBorder,
    outlineVariant = Color(0xFF3A4656),
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
