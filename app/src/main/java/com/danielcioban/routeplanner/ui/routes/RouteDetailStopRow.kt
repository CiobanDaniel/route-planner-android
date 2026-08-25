package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.LibraryUsage
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.local.StopFailureReason
import com.danielcioban.routeplanner.data.local.StopTaskProgress
import com.danielcioban.routeplanner.data.settings.DistanceUnit
import com.danielcioban.routeplanner.ui.components.IslandListItem
import com.danielcioban.routeplanner.ui.components.StopReorderControls
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.GeoUtils
import com.danielcioban.routeplanner.util.RouteEta

internal data class PendingStopPlaceEdit(
    val stopId: Long,
    val name: String,
    val notes: String,
    val addressHint: String,
    val usage: LibraryUsage,
    val arriveByMinutes: Int?,
    val serviceMinutes: Int,
    val geofenceRadiusMeters: Int?,
    val arriveByEpochMs: Long?,
    val phone: String,
    val doorCode: String,
    val isFixedOrder: Boolean,
    val isBreak: Boolean,
    val codAmount: Double,
    val barcode: String,
)

@Composable
internal fun StopRow(
    index: Int,
    stop: StopEntity,
    previous: StopEntity?,
    selected: Boolean,
    taskProgress: StopTaskProgress?,
    distanceUnit: DistanceUnit,
    use24Hour: Boolean,
    arrival: RouteEta.StopArrival?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveToTop: () -> Unit,
    onMoveToBottom: () -> Unit,
) {
    val hasPin = stop.latitude != null && stop.longitude != null
    IslandListItem(onClick = onClick, selected = selected) {
        Checkbox(checked = stop.isCompleted, onCheckedChange = onCompletedChange)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "$index. ${stop.name}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
            if (stop.isOrigin) {
                Text(
                    text = stringResource(R.string.origin_stop_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = IslandColors.badge,
                )
            } else if (stop.libraryStopId != null) {
                Text(
                    text = stringResource(R.string.library_linked_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = IslandColors.badge,
                )
            }
            if (stop.isBreak) {
                Text(
                    text = stringResource(R.string.stop_break_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = IslandColors.badge,
                )
            }
            if (stop.isFixedOrder) {
                Text(
                    text = stringResource(R.string.stop_fixed_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = IslandColors.badge,
                )
            }
            if (stop.tasksDeferred) {
                Text(
                    text = stringResource(R.string.stop_deferred_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = IslandColors.onSurfaceMuted,
                )
            }
            stop.failureReason?.let { reason ->
                Text(
                    text = stringResource(failureReasonLabelRes(reason)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (stop.isVisited && !stop.isCompleted) {
                Text(
                    text = stringResource(R.string.stop_visited_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = IslandColors.success,
                )
            }
            if (taskProgress != null && taskProgress.total > 0) {
                Text(
                    text = stringResource(
                        R.string.tasks_progress,
                        taskProgress.completed,
                        taskProgress.total,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (taskProgress.requiredRemaining > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        IslandColors.success
                    },
                )
            }
            if (stop.addressHint.isNotBlank()) {
                Text(
                    text = stop.addressHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = IslandColors.onSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
            stop.arriveByEpochMs?.let { epoch ->
                Text(
                    text = stringResource(
                        R.string.stop_arrive_by_date,
                        GeoUtils.formatDateTime(epoch, use24Hour),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = IslandColors.onSurfaceMuted,
                )
            } ?: stop.arriveByMinutes?.let { minutes ->
                Text(
                    text = stringResource(
                        R.string.stop_arrive_by_set,
                        GeoUtils.formatClockMinutes(minutes, use24Hour),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = IslandColors.onSurfaceMuted,
                )
            }
            arrival?.let { estimate ->
                val latePromised = stop.arriveByMinutes
                Text(
                    text = if (estimate.late && latePromised != null) {
                        stringResource(R.string.stop_late, GeoUtils.formatClockMinutes(latePromised, use24Hour))
                    } else {
                        stringResource(R.string.stop_eta, GeoUtils.formatClockFromEpoch(estimate.epochMs, use24Hour))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (estimate.late) {
                        MaterialTheme.colorScheme.error
                    } else {
                        IslandColors.onSurfaceMuted
                    },
                )
            }
            when {
                hasPin && previous?.latitude != null && previous.longitude != null -> {
                    val meters = GeoUtils.distanceMeters(
                        previous.latitude!!,
                        previous.longitude!!,
                        stop.latitude!!,
                        stop.longitude!!,
                    )
                    Text(
                        text = stringResource(
                            R.string.detail_from_previous,
                            GeoUtils.formatDistance(meters, distanceUnit),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                hasPin -> {
                    Text(
                        text = stringResource(R.string.edit_pinned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                else -> {
                    Text(
                        text = stringResource(R.string.detail_no_coordinates),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        StopReorderControls(
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onMoveToTop = onMoveToTop,
            onMoveToBottom = onMoveToBottom,
        )
    }
}

internal fun failureReasonLabelRes(reason: String): Int = when (reason) {
    StopFailureReason.NOT_HOME -> R.string.fail_reason_not_home
    StopFailureReason.REFUSED -> R.string.fail_reason_refused
    StopFailureReason.CLOSED -> R.string.fail_reason_closed
    else -> R.string.fail_reason_other
}
