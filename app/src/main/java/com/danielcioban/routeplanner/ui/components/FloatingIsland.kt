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
import androidx.compose.ui.geometry.CornerRadius
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
 * Light theme: soft dual shadows. Dark theme: single drop shadow (no white halo).
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
    val borderColor = palette.fieldBorder.copy(alpha = if (palette.useHighlightShadow) 0.72f else 0.7f)
    Box(
        modifier = modifier
            .graphicsLayer { clip = false }
            .islandElevation(shape = shape, elevation = elevation, palette = palette)
            .clip(shape)
            .background(containerColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
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
        shape = RoundedCornerShape(18.dp),
        contentPadding = 0.dp,
        elevation = 10.dp,
    ) {
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(14.dp),
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

private fun Modifier.islandElevation(
    shape: RoundedCornerShape,
    elevation: Dp,
    palette: IslandPalette,
): Modifier = drawBehind {
    val corner = shape.topStart.toPx(size, this)
    val elev = elevation.toPx()
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val fp = paint.asFrameworkPaint()
        fp.isAntiAlias = true

        if (palette.useHighlightShadow) {
            fp.setShadowLayer(
                elev * 0.9f,
                elev * 0.35f,
                elev * 0.45f,
                palette.darkShadow.copy(alpha = 0.40f).toArgb(),
            )
            canvas.drawRoundRect(0f, 0f, size.width, size.height, corner, corner, paint)

            fp.setShadowLayer(
                elev * 0.7f,
                -elev * 0.28f,
                -elev * 0.32f,
                palette.lightShadow.copy(alpha = 0.95f).toArgb(),
            )
            canvas.drawRoundRect(0f, 0f, size.width, size.height, corner, corner, paint)
        } else {
            // Dark mode: soft drop shadow only — light “sculpt” reads as a foggy halo.
            fp.setShadowLayer(
                elev * 1.1f,
                0f,
                elev * 0.35f,
                palette.darkShadow.copy(alpha = 0.55f).toArgb(),
            )
            canvas.drawRoundRect(0f, 0f, size.width, size.height, corner, corner, paint)
        }
    }

    if (palette.useHighlightShadow) {
        drawRoundRect(
            color = palette.lightShadow.copy(alpha = 0.55f),
            cornerRadius = CornerRadius(corner, corner),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f),
        )
    }
}
