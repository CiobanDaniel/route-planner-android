package com.danielcioban.routeplanner.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.ui.components.CollapsibleBottomIsland
import com.danielcioban.routeplanner.ui.components.FloatingIsland

/**
 * Overlay-safe tablet / landscape breakpoints.
 * Side panes live in a [Box] sibling (fillMaxSize + align), never inside
 * height-wrapping chrome that also holds FABs (see docs/DEV_STATUS.md).
 */
object AppPanes {
    /** Home rail and map sheets switch at this width (phone landscape / tablet). */
    val WideMinWidth = 700.dp
    val SideWidth = 400.dp
    val HomeRailWidth = 360.dp
    val HudMaxWidth = 440.dp
    val ReadableMaxWidth = 720.dp
    /** Top chrome islands; extra width is leftover map, not a stretched title bar. */
    val TitleMaxWidth = 560.dp

    fun isWide(width: Dp): Boolean = width >= WideMinWidth

    /** HUD (thumb reach) plus remaining-stops rail without overlap. */
    fun showDrivingQueueRail(width: Dp): Boolean =
        isWide(width) && width >= HudMaxWidth + SideWidth + 32.dp
}

fun Modifier.readableWidth(): Modifier =
    fillMaxWidth().widthIn(max = AppPanes.ReadableMaxWidth)

fun Modifier.chromeIslandWidth(): Modifier =
    fillMaxWidth().widthIn(max = AppPanes.TitleMaxWidth)

/**
 * Phone: collapsible bottom island. Wide: full-height side overlay on the end edge.
 * Parent must be a [Box] that fills the map gap (typically [ColumnScope] + [weight] 1).
 */
@Composable
fun BoxScope.AdaptiveMapSheet(
    wide: Boolean,
    maxExpandedHeight: Dp,
    modifier: Modifier = Modifier,
    collapsedHeight: Dp = 80.dp,
    contentPadding: Dp = 12.dp,
    sideWidth: Dp = AppPanes.SideWidth,
    fillHeight: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (wide) {
        FloatingIsland(
            modifier = modifier
                .align(if (fillHeight) Alignment.CenterEnd else Alignment.TopEnd)
                .width(sideWidth)
                .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier),
            shape = RoundedCornerShape(24.dp),
            contentPadding = contentPadding,
        ) {
            Column(
                modifier = if (fillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
                content = content,
            )
        }
    } else {
        CollapsibleBottomIsland(
            modifier = modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            maxExpandedHeight = maxExpandedHeight,
            collapsedHeight = collapsedHeight,
            contentPadding = contentPadding,
            content = content,
        )
    }
}

/** Weighted map-gap slot so the sheet is a Box overlay, not extra Column height. */
@Composable
fun ColumnScope.AdaptiveSheetSlot(
    wide: Boolean,
    maxExpandedHeight: Dp,
    modifier: Modifier = Modifier,
    collapsedHeight: Dp = 80.dp,
    contentPadding: Dp = 12.dp,
    sideWidth: Dp = AppPanes.SideWidth,
    fillHeight: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
    ) {
        AdaptiveMapSheet(
            wide = wide,
            maxExpandedHeight = maxExpandedHeight,
            modifier = modifier,
            collapsedHeight = collapsedHeight,
            contentPadding = contentPadding,
            sideWidth = sideWidth,
            fillHeight = fillHeight,
            content = content,
        )
    }
}

/**
 * Home (and similar): fill leftover rail height on wide layouts; bottom sheet on phones.
 * Lives in the already-aligned rail [Column], not inside height-wrapping FAB chrome.
 */
@Composable
fun ColumnScope.AdaptiveRailIsland(
    wide: Boolean,
    maxExpandedHeight: Dp,
    contentPadding: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (wide) {
        FloatingIsland(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            contentPadding = contentPadding,
        ) {
            Column(modifier = Modifier.fillMaxSize(), content = content)
        }
    } else {
        CollapsibleBottomIsland(
            modifier = Modifier.fillMaxWidth(),
            maxExpandedHeight = maxExpandedHeight,
            contentPadding = contentPadding,
            content = content,
        )
    }
}
