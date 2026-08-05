package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun DeliveryStopQueueSheet(
    remainingStops: List<StopEntity>,
    currentStopId: Long?,
    onDismiss: () -> Unit,
    onJumpTo: (StopEntity) -> Unit,
    onSkipCurrent: () -> Unit,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.queue_title),
        confirmLabel = stringResource(R.string.action_close),
        onConfirm = onDismiss,
        dismissLabel = null,
    ) {
        Text(
            text = stringResource(R.string.queue_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        if (currentStopId != null && remainingStops.any { it.id == currentStopId }) {
            FilledTonalButton(
                onClick = {
                    onSkipCurrent()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.queue_skip_current))
            }
        }
        if (remainingStops.isEmpty()) {
            Text(
                text = stringResource(R.string.nav_all_completed),
                style = MaterialTheme.typography.bodyLarge,
                color = IslandColors.onSurface,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                remainingStops.forEachIndexed { index, stop ->
                    val isCurrent = stop.id == currentStopId
                    QueueStopRow(
                        index = index + 1,
                        stop = stop,
                        isCurrent = isCurrent,
                        onClick = {
                            if (!isCurrent) {
                                onJumpTo(stop)
                                onDismiss()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueStopRow(
    index: Int,
    stop: StopEntity,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val hasPin = stop.latitude != null && stop.longitude != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCurrent, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$index. ${stop.name}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isCurrent) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.queue_now),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (stop.notes.isNotBlank()) {
                Text(
                    text = stop.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = IslandColors.onSurfaceMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp),
                    tint = if (hasPin) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                Text(
                    text = stringResource(
                        if (hasPin) R.string.edit_pinned else R.string.detail_no_coordinates,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = IslandColors.onSurfaceMuted,
                )
            }
        }
        if (!isCurrent) {
            TextButton(onClick = onClick) {
                Text(stringResource(R.string.queue_jump))
            }
        }
    }
}
