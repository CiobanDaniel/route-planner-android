package com.danielcioban.routeplanner.ui.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.ui.theme.IslandColors

/** Drag handle plus up/down and top/bottom. Up/down stay as buttons. */
@Composable
fun StopReorderControls(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveToTop: () -> Unit,
    onMoveToBottom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        StopReorderHandle(
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onMoveToTop,
                enabled = canMoveUp,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Default.KeyboardDoubleArrowUp,
                    contentDescription = stringResource(R.string.cd_move_top),
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.cd_move_up),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onMoveToBottom,
                enabled = canMoveDown,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Default.KeyboardDoubleArrowDown,
                    contentDescription = stringResource(R.string.cd_move_bottom),
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.cd_move_down),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun StopReorderHandle(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val moveUp by rememberUpdatedState(onMoveUp)
    val moveDown by rememberUpdatedState(onMoveDown)
    val canUp by rememberUpdatedState(canMoveUp)
    val canDown by rememberUpdatedState(canMoveDown)
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }
    var accumulated by remember { mutableFloatStateOf(0f) }

    Icon(
        imageVector = Icons.Default.DragHandle,
        contentDescription = stringResource(R.string.cd_reorder_drag),
        tint = IslandColors.onSurfaceMuted,
        modifier = Modifier
            .size(width = 36.dp, height = 48.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { accumulated = 0f },
                    onDragCancel = { accumulated = 0f },
                ) { _, dragAmount ->
                    accumulated += dragAmount
                    while (accumulated <= -threshold && canUp) {
                        moveUp()
                        accumulated += threshold
                    }
                    while (accumulated >= threshold && canDown) {
                        moveDown()
                        accumulated -= threshold
                    }
                    if (!canUp && accumulated < 0f) accumulated = 0f
                    if (!canDown && accumulated > 0f) accumulated = 0f
                }
            },
    )
}
