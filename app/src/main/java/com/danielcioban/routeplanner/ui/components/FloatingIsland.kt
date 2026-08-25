package com.danielcioban.routeplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.ui.theme.IslandPalette
import com.danielcioban.routeplanner.ui.theme.LocalIslandColors

/**
 * Floating panel above the map.
 *
 * Light comes from the upper-left **on the face**. The drop shadow is cast
 * lower-right onto whatever is behind the island — not a glow on the panel.
 * Dark theme: no surrounding white halo (that reads as fog on the map).
 */
@Composable
fun FloatingIsland(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    containerColor: Color = IslandColors.surfaceElevated,
    contentPadding: Dp = 16.dp,
    elevation: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalIslandColors.current
    val lightTheme = palette.useHighlightShadow
    Box(
        modifier = modifier
            .graphicsLayer { clip = false }
            .islandCastShadow(shape = shape, elevation = elevation, palette = palette)
            .clip(shape)
            .background(containerColor)
            .islandFaceLight(shape = shape, palette = palette)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (lightTheme) {
                        listOf(
                            Color.White.copy(alpha = 0.58f),
                            palette.fieldBorder.copy(alpha = 0.42f),
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.12f),
                            palette.fieldBorder.copy(alpha = 0.7f),
                        )
                    },
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
                shape = shape,
            )
            .blockMapPassThrough()
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun FloatingCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
    prominent: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    if (embedded) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(10.dp),
            contentAlignment = Alignment.Center,
            content = content,
        )
        return
    }
    FloatingIsland(
        modifier = modifier,
        shape = RoundedCornerShape(if (prominent) 22.dp else 18.dp),
        contentPadding = 0.dp,
        elevation = if (prominent) 14.dp else 10.dp,
    ) {
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(if (prominent) 18.dp else 12.dp),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/**
 * Makes this node a hit target so an [android.webkit.WebView] map behind it does not
 * receive the same pan/zoom gestures. Do not consume: nested scroll (lists) needs the
 * slop window, and children (buttons, fields) still win hit-testing first.
 */
fun Modifier.blockMapPassThrough(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial)
        }
    }
}

/** Soft drop shadow on the map, offset lower-right. Not drawn as a rim on the face. */
private fun Modifier.islandCastShadow(
    shape: RoundedCornerShape,
    elevation: Dp,
    palette: IslandPalette,
): Modifier = drawBehind {
    val corner = shape.topStart.toPx(size, this)
    val elev = elevation.toPx().coerceAtLeast(1f)
    val blur = elev * 1.35f
    val dx = elev * 0.42f
    val dy = elev * 0.52f
    val shadowAlpha = if (palette.useHighlightShadow) 0.18f else 0.38f
    drawIntoCanvas { canvas ->
        val paint = Paint()
        paint.color = Color.Black.copy(alpha = 0.02f)
        val fp = paint.asFrameworkPaint()
        fp.isAntiAlias = true
        fp.setShadowLayer(
            blur,
            dx,
            dy,
            palette.darkShadow.copy(alpha = shadowAlpha).toArgb(),
        )
        canvas.drawRoundRect(0f, 0f, size.width, size.height, corner, corner, paint)
    }
}

/**
 * Lighting on the panel itself (under labels/icons): bright from upper-left,
 * a hint of shade toward lower-right. Clipped to the island so it never
 * becomes a halo on the map.
 */
private fun Modifier.islandFaceLight(
    shape: RoundedCornerShape,
    palette: IslandPalette,
): Modifier = drawWithContent {
    val corner = CornerRadius(shape.topStart.toPx(size, this))
    if (palette.useHighlightShadow) {
        drawRoundRect(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0f to Color.White.copy(alpha = 0.20f),
                    0.42f to Color.White.copy(alpha = 0.05f),
                    1f to Color.Transparent,
                ),
                start = Offset.Zero,
                end = Offset(size.width * 0.7f, size.height * 0.7f),
            ),
            cornerRadius = corner,
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.045f),
                ),
                start = Offset(size.width * 0.4f, size.height * 0.4f),
                end = Offset(size.width, size.height),
            ),
            cornerRadius = corner,
        )
    } else {
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.06f),
                    Color.Transparent,
                ),
                start = Offset.Zero,
                end = Offset(size.width * 0.5f, size.height * 0.5f),
            ),
            cornerRadius = corner,
        )
    }
    drawContent()
}
