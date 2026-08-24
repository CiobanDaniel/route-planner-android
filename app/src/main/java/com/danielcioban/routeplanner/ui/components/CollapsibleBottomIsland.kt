package com.danielcioban.routeplanner.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.ui.theme.IslandColors
import kotlinx.coroutines.launch

/**
 * Bottom map island with a grab bar. Drag down (or tap the bar) to peek more map;
 * drag up to restore. Height change is absorbed by the screen's weighted spacer,
 * so top chrome does not reflow. Drag lives on the handle only so lists still scroll.
 */
@Composable
fun CollapsibleBottomIsland(
    modifier: Modifier = Modifier,
    maxExpandedHeight: Dp,
    collapsedHeight: Dp = 80.dp,
    contentPadding: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val collapsedPx = with(density) { collapsedHeight.toPx() }
    val expandedPx = with(density) { maxExpandedHeight.toPx() }
    var expanded by rememberSaveable { mutableStateOf(true) }
    val heightPx = remember { Animatable(if (expanded) expandedPx else collapsedPx) }
    val scope = rememberCoroutineScope()
    var ignoreHandleClick by remember { mutableStateOf(false) }

    fun settle(expand: Boolean) {
        expanded = expand
        scope.launch {
            heightPx.animateTo(
                targetValue = if (expand) expandedPx else collapsedPx,
                animationSpec = tween(220),
            )
        }
    }

    val handleDescription = stringResource(
        if (expanded) R.string.cd_bottom_sheet_collapse else R.string.cd_bottom_sheet_expand,
    )
    val dragState = rememberDraggableState { delta ->
        val next = (heightPx.value - delta).coerceIn(collapsedPx, expandedPx)
        scope.launch { heightPx.snapTo(next) }
    }

    FloatingIsland(
        modifier = modifier
            .fillMaxWidth()
            .height(with(density) { heightPx.value.toDp() }),
        shape = RoundedCornerShape(28.dp),
        contentPadding = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = handleDescription
                        role = Role.Button
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (ignoreHandleClick) {
                                ignoreHandleClick = false
                                return@clickable
                            }
                            settle(!expanded)
                        },
                    )
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        onDragStopped = {
                            ignoreHandleClick = true
                            val midpoint = (collapsedPx + expandedPx) / 2f
                            settle(heightPx.value >= midpoint)
                        },
                    )
                    .padding(top = 10.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(IslandColors.fieldBorder.copy(alpha = 0.85f)),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = contentPadding, end = contentPadding, bottom = contentPadding),
                content = content,
            )
        }
    }
}
