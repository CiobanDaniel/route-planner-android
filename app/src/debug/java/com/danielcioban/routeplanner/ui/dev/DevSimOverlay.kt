package com.danielcioban.routeplanner.ui.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.theme.BrandColors
import com.danielcioban.routeplanner.ui.theme.IslandColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Debug-only overlay: analog stick + speed + teleport. Overlay on the app root Box. */
@Composable
fun DevSimOverlay(
    onJumpToMapCenter: () -> Unit,
    onJumpToNextStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val speed by DevLocationSim.speed.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        while (true) {
            DevLocationSim.tick(0.05f)
            delay(50)
        }
    }
    FloatingIsland(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        contentPadding = 10.dp,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.dev_sim_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = BrandColors.orange,
            )
            DevJoystick()
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                DevSimSpeed.entries.forEach { preset ->
                    TextButton(onClick = { DevLocationSim.setSpeed(preset) }) {
                        Text(
                            text = stringResource(preset.labelRes),
                            color = if (speed == preset) BrandColors.orange else IslandColors.onSurfaceMuted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (speed == preset) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextButton(onClick = onJumpToMapCenter) {
                    Text(stringResource(R.string.dev_sim_jump_map), style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onJumpToNextStop) {
                    Text(stringResource(R.string.dev_sim_jump_stop), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private val DevSimSpeed.labelRes: Int
    get() = when (this) {
        DevSimSpeed.WALK -> R.string.dev_sim_walk
        DevSimSpeed.BIKE -> R.string.dev_sim_bike
        DevSimSpeed.CITY -> R.string.dev_sim_city
        DevSimSpeed.HIGHWAY -> R.string.dev_sim_hwy
    }

@Composable
private fun DevJoystick() {
    val radius = 52.dp
    val knobSize = 36.dp
    val maxR = with(LocalDensity.current) { (radius - knobSize / 2).toPx() }
    var knob by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .size(radius * 2)
            .clip(CircleShape)
            .background(IslandColors.rowSurface)
            .border(1.dp, IslandColors.fieldBorder.copy(alpha = 0.55f), CircleShape)
            .pointerInput(maxR) {
                detectDragGestures(
                    onDragEnd = {
                        knob = Offset.Zero
                        DevLocationSim.setStick(0f, 0f)
                    },
                    onDragCancel = {
                        knob = Offset.Zero
                        DevLocationSim.setStick(0f, 0f)
                    },
                    onDragStart = { start ->
                        applyStick(start, maxR, size.width.toFloat(), size.height.toFloat()) { knob = it }
                    },
                ) { change, _ ->
                    change.consume()
                    applyStick(change.position, maxR, size.width.toFloat(), size.height.toFloat()) { knob = it }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(knob.x.roundToInt(), knob.y.roundToInt()) }
                .size(knobSize)
                .clip(CircleShape)
                .background(BrandColors.orange),
        )
    }
}

private fun applyStick(
    pos: Offset,
    maxR: Float,
    width: Float,
    height: Float,
    setKnob: (Offset) -> Unit,
) {
    val center = Offset(width / 2f, height / 2f)
    val delta = pos - center
    val dist = delta.getDistance().coerceAtLeast(0.001f)
    val clamped = if (dist > maxR) delta * (maxR / dist) else delta
    setKnob(clamped)
    val nx = (clamped.x / maxR).coerceIn(-1f, 1f)
    val ny = (-clamped.y / maxR).coerceIn(-1f, 1f)
    DevLocationSim.setStick(nx, ny)
}
