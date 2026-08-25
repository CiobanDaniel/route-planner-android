package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.routing.ManeuverFormatter
import com.danielcioban.routeplanner.data.routing.NavGuidance
import com.danielcioban.routeplanner.data.routing.NavigationProgress
import com.danielcioban.routeplanner.data.settings.DistanceUnit
import com.danielcioban.routeplanner.ui.components.CollapsibleBottomIsland
import com.danielcioban.routeplanner.ui.components.StopNotesBanner
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.GeoUtils

class DeliveryHudOps(
    val onProof: (() -> Unit)? = null,
    val onScan: (() -> Unit)? = null,
    val onShareEta: (() -> Unit)? = null,
)

@Composable
fun DeliveryHud(
    progress: DeliveryProgress,
    totalStops: Int,
    navigation: NavigationUiState,
    distanceFromYou: String?,
    onStartInAppNav: () -> Unit,
    onMarkDone: () -> Unit,
    onEndDelivery: () -> Unit,
    onRetryRoute: () -> Unit,
    onOpenExternalMaps: (StopEntity) -> Unit,
    onOpenQueue: () -> Unit,
    distanceUnit: DistanceUnit = DistanceUnit.METRIC,
    modifier: Modifier = Modifier,
    roundTrip: Boolean = false,
    onReturnToStart: (() -> Unit)? = null,
    etaLabel: String? = null,
    late: Boolean = false,
    onPreviewPath: (() -> Unit)? = null,
    onRunAgain: (() -> Unit)? = null,
    markDoneEnabled: Boolean = true,
    onFail: (() -> Unit)? = null,
    onReschedule: (() -> Unit)? = null,
    onPause: (() -> Unit)? = null,
    paused: Boolean = false,
    enlargedActions: Boolean = false,
    onShareSms: (() -> Unit)? = null,
    onOpenNextThree: (() -> Unit)? = null,
    onCall: (() -> Unit)? = null,
    onDeferTasks: (() -> Unit)? = null,
    onAddStop: (() -> Unit)? = null,
    onSkipLater: (() -> Unit)? = null,
    markDoneLabelRes: Int = R.string.nav_mark_done,
    ops: DeliveryHudOps = DeliveryHudOps(),
) {
    val resources = LocalContext.current.resources
    val next = progress.nextStop
    val failAction = if (next?.isBreak == true) null else onFail
    val doneLabelRes = if (next?.isBreak == true) R.string.nav_end_break else markDoneLabelRes
    val completed = (totalStops - progress.remaining).coerceAtLeast(0)
    val approximate = navigation.route?.isApproximate == true

    CollapsibleBottomIsland(
        modifier = modifier,
        maxExpandedHeight = 460.dp,
        collapsedHeight = 112.dp,
        contentPadding = 12.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DeliveryHudHeader(
                progress = progress,
                totalStops = totalStops,
                completed = completed,
                phase = navigation.phase,
                onOpenQueue = onOpenQueue,
            )

            when {
                next == null -> DeliveryHudAllDone(
                    onEndDelivery = onEndDelivery,
                    roundTrip = roundTrip,
                    onReturnToStart = onReturnToStart,
                    onRunAgain = onRunAgain,
                )
                navigation.phase == NavigationPhase.LoadingRoute ->
                    DeliveryHudLoading(stopName = next.name)
                navigation.phase == NavigationPhase.Error ->
                    DeliveryHudError(
                        stop = next,
                        errorMessageRes = navigation.errorMessageRes,
                        onRetryRoute = onRetryRoute,
                        onMarkDone = onMarkDone,
                        onEndDelivery = onEndDelivery,
                        onOpenExternalMaps = onOpenExternalMaps,
                        markDoneEnabled = markDoneEnabled,
                        onFail = failAction,
                        onReschedule = onReschedule,
                        onPause = onPause,
                        paused = paused,
                        enlargedActions = enlargedActions,
                        onShareSms = onShareSms,
                        onOpenNextThree = onOpenNextThree,
                        onCall = onCall,
                        onDeferTasks = onDeferTasks,
                        onAddStop = onAddStop,
                        onSkipLater = onSkipLater,
                        markDoneLabelRes = doneLabelRes,
                        ops = ops,
                    )
                navigation.phase == NavigationPhase.Arrived && progress.remaining == 0 ->
                    DeliveryHudRouteFinished(
                        onEndDelivery = onEndDelivery,
                        onRunAgain = onRunAgain,
                    )
                navigation.phase == NavigationPhase.Arrived ->
                    DeliveryHudArrived(
                        stop = next,
                        onMarkDone = onMarkDone,
                        onEndDelivery = onEndDelivery,
                        onOpenExternalMaps = onOpenExternalMaps,
                        markDoneEnabled = markDoneEnabled,
                        onFail = failAction,
                        onReschedule = onReschedule,
                        onPause = onPause,
                        paused = paused,
                        enlargedActions = enlargedActions,
                        onShareSms = onShareSms,
                        onOpenNextThree = onOpenNextThree,
                        onCall = onCall,
                        onDeferTasks = onDeferTasks,
                        onAddStop = onAddStop,
                        onSkipLater = onSkipLater,
                        markDoneLabelRes = doneLabelRes,
                        ops = ops,
                    )
                navigation.phase == NavigationPhase.Navigating && navigation.guidance != null -> {
                    val guidance = navigation.guidance
                    DeliveryHudNavigating(
                        stop = next,
                        guidance = guidance,
                        approximate = approximate,
                        approxMessageRes = navigation.errorMessageRes,
                        distanceUnit = distanceUnit,
                        resources = resources,
                        onRetryRoute = onRetryRoute,
                        onMarkDone = onMarkDone,
                        onEndDelivery = onEndDelivery,
                        onOpenExternalMaps = onOpenExternalMaps,
                        markDoneEnabled = markDoneEnabled,
                        onFail = failAction,
                        onReschedule = onReschedule,
                        onPause = onPause,
                        paused = paused,
                        enlargedActions = enlargedActions,
                        onShareSms = onShareSms,
                        onOpenNextThree = onOpenNextThree,
                        onCall = onCall,
                        onDeferTasks = onDeferTasks,
                        onAddStop = onAddStop,
                        onSkipLater = onSkipLater,
                        markDoneLabelRes = doneLabelRes,
                        ops = ops,
                    )
                }
                else ->
                    DeliveryHudIdleNext(
                        progress = progress,
                        stop = next,
                        distanceFromYou = distanceFromYou,
                        etaLabel = etaLabel,
                        late = late,
                        onStartInAppNav = onStartInAppNav,
                        onMarkDone = onMarkDone,
                        onEndDelivery = onEndDelivery,
                        onOpenExternalMaps = onOpenExternalMaps,
                        onPreviewPath = onPreviewPath,
                        markDoneEnabled = markDoneEnabled,
                        onFail = failAction,
                        onReschedule = onReschedule,
                        onPause = onPause,
                        paused = paused,
                        enlargedActions = enlargedActions,
                        onShareSms = onShareSms,
                        onOpenNextThree = onOpenNextThree,
                        onCall = onCall,
                        onDeferTasks = onDeferTasks,
                        onAddStop = onAddStop,
                        onSkipLater = onSkipLater,
                        markDoneLabelRes = doneLabelRes,
                        ops = ops,
                    )
            }
        }
    }
}

@Composable
private fun DeliveryHudHeader(
    progress: DeliveryProgress,
    totalStops: Int,
    completed: Int,
    phase: NavigationPhase,
    onOpenQueue: () -> Unit,
) {
    val next = progress.nextStop
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = when {
                next == null -> stringResource(R.string.nav_route_finished)
                phase == NavigationPhase.Navigating ||
                    phase == NavigationPhase.LoadingRoute ||
                    phase == NavigationPhase.Arrived ->
                    stringResource(R.string.nav_navigating_progress, completed + 1, totalStops)
                else -> stringResource(R.string.nav_next_stop_progress, completed + 1, totalStops)
            },
            style = MaterialTheme.typography.labelLarge,
            color = IslandColors.onSurfaceMuted,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (progress.remaining == 0) {
                stringResource(R.string.nav_done)
            } else {
                pluralStringResource(R.plurals.nav_left, progress.remaining, progress.remaining)
            },
            style = MaterialTheme.typography.labelLarge,
            color = IslandColors.onSurfaceMuted,
        )
        if (progress.remaining > 0) {
            IconButton(onClick = onOpenQueue) {
                Icon(
                    Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = stringResource(R.string.cd_open_stop_queue),
                    tint = IslandColors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.DeliveryHudAllDone(
    onEndDelivery: () -> Unit,
    roundTrip: Boolean,
    onReturnToStart: (() -> Unit)?,
    onRunAgain: (() -> Unit)?,
) {
    Text(
        text = stringResource(R.string.nav_all_completed),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = IslandColors.onSurface,
    )
    Text(
        text = stringResource(R.string.nav_end_when_ready),
        style = MaterialTheme.typography.bodyMedium,
        color = IslandColors.onSurfaceMuted,
    )
    if (roundTrip && onReturnToStart != null) {
        Button(
            onClick = onReturnToStart,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.Navigation, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.nav_return_to_start))
        }
    }
    if (onRunAgain != null) {
        Button(
            onClick = onRunAgain,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_run_again))
        }
    }
    OutlinedButton(
        onClick = onEndDelivery,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(Icons.Default.Stop, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.nav_end_delivery))
    }
}

@Composable
private fun ColumnScope.DeliveryHudLoading(stopName: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        Column {
            Text(
                text = stringResource(R.string.nav_calculating),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = IslandColors.onSurface,
            )
            Text(
                text = stopName,
                style = MaterialTheme.typography.bodyMedium,
                color = IslandColors.onSurfaceMuted,
            )
        }
    }
}

@Composable
private fun ColumnScope.DeliveryHudError(
    stop: StopEntity,
    errorMessageRes: Int?,
    onRetryRoute: () -> Unit,
    onMarkDone: () -> Unit,
    onEndDelivery: () -> Unit,
    onOpenExternalMaps: (StopEntity) -> Unit,
    markDoneEnabled: Boolean,
    onFail: (() -> Unit)?,
    onReschedule: (() -> Unit)?,
    onPause: (() -> Unit)? = null,
    paused: Boolean = false,
    enlargedActions: Boolean = false,
    onShareSms: (() -> Unit)? = null,
    onOpenNextThree: (() -> Unit)? = null,
    onCall: (() -> Unit)? = null,
    onDeferTasks: (() -> Unit)? = null,
    onAddStop: (() -> Unit)? = null,
    onSkipLater: (() -> Unit)? = null,
    markDoneLabelRes: Int = R.string.nav_mark_done,
    ops: DeliveryHudOps = DeliveryHudOps(),
) {
    Text(
        text = stop.name,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = IslandColors.onSurface,
    )
    Text(
        text = stringResource(errorMessageRes ?: R.string.nav_error_generic),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
    Button(
        onClick = onRetryRoute,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(Icons.Default.Refresh, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.nav_retry_route))
    }
    ActionRow(
        onMarkDone = onMarkDone,
        onEndDelivery = onEndDelivery,
        markDoneEnabled = markDoneEnabled,
        onFail = onFail,
        onReschedule = onReschedule,
        onPause = onPause,
        paused = paused,
        enlargedActions = enlargedActions,
        onShareSms = onShareSms,
        onOpenNextThree = onOpenNextThree,
        onCall = onCall,
        onDeferTasks = onDeferTasks,
        onAddStop = onAddStop,
        onSkipLater = onSkipLater,
        markDoneLabelRes = markDoneLabelRes,
        ops = ops,
        onOpenExternal = { onOpenExternalMaps(stop) },
    )
}

@Composable
private fun ColumnScope.DeliveryHudRouteFinished(
    onEndDelivery: () -> Unit,
    onRunAgain: (() -> Unit)?,
) {
    Text(
        text = stringResource(R.string.nav_finished_route),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = IslandColors.onSurface,
    )
    if (onRunAgain != null) {
        Button(
            onClick = onRunAgain,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_run_again))
        }
    }
    OutlinedButton(
        onClick = onEndDelivery,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(Icons.Default.Stop, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.nav_end_delivery))
    }
}

@Composable
private fun ColumnScope.DeliveryHudArrived(
    stop: StopEntity,
    onMarkDone: () -> Unit,
    onEndDelivery: () -> Unit,
    onOpenExternalMaps: (StopEntity) -> Unit,
    markDoneEnabled: Boolean,
    onFail: (() -> Unit)?,
    onReschedule: (() -> Unit)?,
    onPause: (() -> Unit)? = null,
    paused: Boolean = false,
    enlargedActions: Boolean = false,
    onShareSms: (() -> Unit)? = null,
    onOpenNextThree: (() -> Unit)? = null,
    onCall: (() -> Unit)? = null,
    onDeferTasks: (() -> Unit)? = null,
    onAddStop: (() -> Unit)? = null,
    onSkipLater: (() -> Unit)? = null,
    markDoneLabelRes: Int = R.string.nav_mark_done,
    ops: DeliveryHudOps = DeliveryHudOps(),
) {
    Text(
        text = stringResource(R.string.nav_arrived),
        style = MaterialTheme.typography.labelLarge,
        color = IslandColors.onSurfaceMuted,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = stop.name,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = IslandColors.onSurface,
    )
    StopNotesBanner(
        notes = stop.notes,
        addressHint = stop.addressHint,
        compact = true,
        phone = stop.phone,
        doorCode = stop.doorCode,
        extraLine = if (stop.codAmount > 0.0) {
            stringResource(
                if (stop.codCollected) R.string.cod_banner_collected else R.string.cod_banner,
                "%.2f".format(stop.codAmount),
            )
        } else {
            ""
        },
    )
    Button(
        onClick = onMarkDone,
        enabled = markDoneEnabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(markDoneLabelRes))
    }
    OutcomeLinks(
        onFail = if (stop.isBreak) null else onFail,
        onReschedule = onReschedule,
        onPause = onPause,
        paused = paused,
        onShareSms = onShareSms,
        onOpenNextThree = onOpenNextThree,
        onCall = onCall,
        onDeferTasks = onDeferTasks,
        onAddStop = onAddStop,
        onSkipLater = onSkipLater,
        ops = ops,
        onOpenExternal = { onOpenExternalMaps(stop) },
        onEndDelivery = onEndDelivery,
    )
}

@Composable
private fun ColumnScope.DeliveryHudNavigating(
    stop: StopEntity,
    guidance: NavGuidance,
    approximate: Boolean,
    approxMessageRes: Int?,
    distanceUnit: DistanceUnit,
    resources: android.content.res.Resources,
    onRetryRoute: () -> Unit,
    onMarkDone: () -> Unit,
    onEndDelivery: () -> Unit,
    onOpenExternalMaps: (StopEntity) -> Unit,
    markDoneEnabled: Boolean,
    onFail: (() -> Unit)?,
    onReschedule: (() -> Unit)?,
    onPause: (() -> Unit)? = null,
    paused: Boolean = false,
    enlargedActions: Boolean = false,
    onShareSms: (() -> Unit)? = null,
    onOpenNextThree: (() -> Unit)? = null,
    onCall: (() -> Unit)? = null,
    onDeferTasks: (() -> Unit)? = null,
    onAddStop: (() -> Unit)? = null,
    onSkipLater: (() -> Unit)? = null,
    markDoneLabelRes: Int = R.string.nav_mark_done,
    ops: DeliveryHudOps = DeliveryHudOps(),
) {
    val step = guidance.currentStep
    if (approximate) {
        Text(
            text = stringResource(approxMessageRes ?: R.string.nav_approx_roads_unavailable),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedButton(
            onClick = onRetryRoute,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.nav_retry_route))
        }
    }
    Text(
        text = GeoUtils.formatDistance(guidance.distanceToManeuverMeters, distanceUnit),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = IslandColors.onSurface,
    )
    Text(
        text = when {
            approximate -> stringResource(R.string.nav_approx_head, stop.name)
            step != null -> ManeuverFormatter.formatStep(resources, step)
            else -> stringResource(R.string.nav_continue_to, stop.name)
        },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = IslandColors.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
    if (!approximate) {
        guidance.thenStep?.let { then ->
            Text(
                text = stringResource(
                    R.string.nav_then,
                    ManeuverFormatter.formatStep(resources, then),
                ),
                style = if (enlargedActions) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                fontWeight = if (enlargedActions) FontWeight.SemiBold else FontWeight.Normal,
                color = if (enlargedActions) IslandColors.onSurface else IslandColors.onSurfaceMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    Text(
        text = buildString {
            append(GeoUtils.formatDistance(guidance.remainingDistanceMeters, distanceUnit))
            append(" · ")
            append(NavigationProgress.formatEta(guidance.remainingDurationSeconds))
            append(" · ")
            append(stop.name)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = IslandColors.onSurfaceMuted,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    StopNotesBanner(
        notes = stop.notes,
        addressHint = stop.addressHint,
        compact = true,
        phone = stop.phone,
        doorCode = stop.doorCode,
        extraLine = if (stop.codAmount > 0.0) {
            stringResource(
                if (stop.codCollected) R.string.cod_banner_collected else R.string.cod_banner,
                "%.2f".format(stop.codAmount),
            )
        } else {
            ""
        },
    )
    ActionRow(
        onMarkDone = onMarkDone,
        onEndDelivery = onEndDelivery,
        markDoneEnabled = markDoneEnabled,
        onFail = onFail,
        onReschedule = onReschedule,
        onPause = onPause,
        paused = paused,
        enlargedActions = enlargedActions,
        onShareSms = onShareSms,
        onOpenNextThree = onOpenNextThree,
        onCall = onCall,
        onDeferTasks = onDeferTasks,
        onAddStop = onAddStop,
        onSkipLater = onSkipLater,
        markDoneLabelRes = markDoneLabelRes,
        ops = ops,
        onOpenExternal = { onOpenExternalMaps(stop) },
    )
}

@Composable
private fun ColumnScope.DeliveryHudIdleNext(
    progress: DeliveryProgress,
    stop: StopEntity,
    distanceFromYou: String?,
    etaLabel: String?,
    late: Boolean,
    onStartInAppNav: () -> Unit,
    onMarkDone: () -> Unit,
    onEndDelivery: () -> Unit,
    onOpenExternalMaps: (StopEntity) -> Unit,
    onPreviewPath: (() -> Unit)?,
    markDoneEnabled: Boolean,
    onFail: (() -> Unit)?,
    onReschedule: (() -> Unit)?,
    onPause: (() -> Unit)? = null,
    paused: Boolean = false,
    enlargedActions: Boolean = false,
    onShareSms: (() -> Unit)? = null,
    onOpenNextThree: (() -> Unit)? = null,
    onCall: (() -> Unit)? = null,
    onDeferTasks: (() -> Unit)? = null,
    onAddStop: (() -> Unit)? = null,
    onSkipLater: (() -> Unit)? = null,
    markDoneLabelRes: Int = R.string.nav_mark_done,
    ops: DeliveryHudOps = DeliveryHudOps(),
) {
    Text(
        text = stop.name,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = IslandColors.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    val prevLabel = progress.approxFromPrevious?.let {
        stringResource(R.string.nav_prev_approx, it)
    }
    val meta = listOfNotNull(distanceFromYou, prevLabel, etaLabel).joinToString(" · ")
    if (meta.isNotBlank()) {
        Text(
            text = meta,
            style = MaterialTheme.typography.bodyMedium,
            color = if (late) MaterialTheme.colorScheme.error else IslandColors.onSurfaceMuted,
        )
    }
    StopNotesBanner(
        notes = stop.notes,
        addressHint = stop.addressHint,
        compact = true,
        phone = stop.phone,
        doorCode = stop.doorCode,
        extraLine = if (stop.codAmount > 0.0) {
            stringResource(
                if (stop.codCollected) R.string.cod_banner_collected else R.string.cod_banner,
                "%.2f".format(stop.codAmount),
            )
        } else {
            ""
        },
    )
    val canNavigate = stop.latitude != null && stop.longitude != null
    Button(
        onClick = onStartInAppNav,
        enabled = canNavigate,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(Icons.Default.Navigation, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            if (canNavigate) {
                stringResource(R.string.nav_start)
            } else {
                stringResource(R.string.nav_no_pin)
            },
        )
    }
    ActionRow(
        onMarkDone = onMarkDone,
        onEndDelivery = onEndDelivery,
        markDoneEnabled = markDoneEnabled,
        onFail = onFail,
        onReschedule = onReschedule,
        onPause = onPause,
        paused = paused,
        enlargedActions = enlargedActions,
        onShareSms = onShareSms,
        onOpenNextThree = onOpenNextThree,
        onCall = onCall,
        onDeferTasks = onDeferTasks,
        onAddStop = onAddStop,
        onSkipLater = onSkipLater,
        markDoneLabelRes = markDoneLabelRes,
        ops = ops,
        onOpenExternal = if (canNavigate) {
            { onOpenExternalMaps(stop) }
        } else {
            null
        },
        onPreviewPath = if (canNavigate) onPreviewPath else null,
    )
}

@Composable
private fun ActionRow(
    onMarkDone: () -> Unit,
    onEndDelivery: () -> Unit,
    markDoneEnabled: Boolean = true,
    onFail: (() -> Unit)? = null,
    onReschedule: (() -> Unit)? = null,
    onPause: (() -> Unit)? = null,
    paused: Boolean = false,
    enlargedActions: Boolean = false,
    markDoneLabelRes: Int = R.string.nav_mark_done,
    onShareSms: (() -> Unit)? = null,
    onOpenNextThree: (() -> Unit)? = null,
    onCall: (() -> Unit)? = null,
    onDeferTasks: (() -> Unit)? = null,
    onAddStop: (() -> Unit)? = null,
    onSkipLater: (() -> Unit)? = null,
    ops: DeliveryHudOps = DeliveryHudOps(),
    onOpenExternal: (() -> Unit)? = null,
    onPreviewPath: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledTonalButton(
            onClick = onMarkDone,
            enabled = markDoneEnabled,
            modifier = Modifier
                .weight(1.2f)
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(if (enlargedActions) 20.dp else 16.dp),
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(markDoneLabelRes))
        }
        OutlinedButton(
            onClick = onEndDelivery,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = IslandColors.onSurface,
            ),
            border = BorderStroke(1.dp, IslandColors.fieldBorder),
        ) {
            Icon(Icons.Default.Stop, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.nav_end))
        }
    }
    OutcomeLinks(
        onFail = onFail,
        onReschedule = onReschedule,
        onPause = onPause,
        paused = paused,
        onShareSms = onShareSms,
        onOpenNextThree = onOpenNextThree,
        onCall = onCall,
        onDeferTasks = onDeferTasks,
        onAddStop = onAddStop,
        onSkipLater = onSkipLater,
        ops = ops,
        onOpenExternal = onOpenExternal,
        onPreviewPath = onPreviewPath,
    )
}

@Composable
private fun OutcomeLinks(
    onFail: (() -> Unit)?,
    onReschedule: (() -> Unit)?,
    onPause: (() -> Unit)? = null,
    paused: Boolean = false,
    onShareSms: (() -> Unit)? = null,
    onOpenNextThree: (() -> Unit)? = null,
    onCall: (() -> Unit)? = null,
    onDeferTasks: (() -> Unit)? = null,
    onAddStop: (() -> Unit)? = null,
    onSkipLater: (() -> Unit)? = null,
    ops: DeliveryHudOps = DeliveryHudOps(),
    onOpenExternal: (() -> Unit)? = null,
    onPreviewPath: (() -> Unit)? = null,
    onEndDelivery: (() -> Unit)? = null,
) {
    val hasLinks = onFail != null || onReschedule != null || onPause != null ||
        onShareSms != null || onOpenNextThree != null || onCall != null ||
        onDeferTasks != null || onAddStop != null || onSkipLater != null ||
        ops.onProof != null || ops.onScan != null || ops.onShareEta != null ||
        onOpenExternal != null || onPreviewPath != null || onEndDelivery != null
    if (!hasLinks) return

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (onSkipLater != null) {
            HudQuietLink(stringResource(R.string.hud_skip_later), onSkipLater)
        }
        if (onCall != null) {
            HudQuietLink(stringResource(R.string.nav_call_stop), onCall)
        }
        if (onAddStop != null) {
            HudQuietLink(stringResource(R.string.hud_add_stop), onAddStop)
        }
        if (ops.onScan != null) {
            HudQuietLink(stringResource(R.string.hud_scan), ops.onScan)
        }
        if (ops.onProof != null) {
            HudQuietLink(stringResource(R.string.hud_proof), ops.onProof)
        }
        if (ops.onShareEta != null) {
            HudQuietLink(stringResource(R.string.hud_share_eta), ops.onShareEta)
        }
        if (onShareSms != null) {
            HudQuietLink(stringResource(R.string.nav_share_sms), onShareSms)
        }
        if (onOpenNextThree != null) {
            HudQuietLink(stringResource(R.string.nav_open_next_three), onOpenNextThree)
        }
        if (onPause != null) {
            HudQuietLink(
                stringResource(if (paused) R.string.nav_resume else R.string.nav_pause),
                onPause,
            )
        }
        if (onReschedule != null) {
            HudQuietLink(stringResource(R.string.nav_reschedule_stop), onReschedule)
        }
        if (onFail != null) {
            HudQuietLink(stringResource(R.string.nav_fail_stop), onFail)
        }
        if (onDeferTasks != null) {
            HudQuietLink(stringResource(R.string.nav_defer_tasks), onDeferTasks)
        }
        if (onPreviewPath != null) {
            HudQuietLink(stringResource(R.string.action_preview_path), onPreviewPath)
        }
        if (onOpenExternal != null) {
            HudQuietLink(stringResource(R.string.nav_open_external), onOpenExternal)
        }
        if (onEndDelivery != null) {
            HudQuietLink(stringResource(R.string.nav_end_delivery), onEndDelivery)
        }
    }
}

@Composable
private fun HudQuietLink(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = IslandColors.onSurfaceMuted),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
