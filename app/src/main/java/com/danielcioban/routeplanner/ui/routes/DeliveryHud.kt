package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.routing.ManeuverFormatter
import com.danielcioban.routeplanner.data.routing.NavGuidance
import com.danielcioban.routeplanner.data.routing.NavigationProgress
import com.danielcioban.routeplanner.data.settings.DistanceUnit
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.components.StopNotesBanner
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.GeoUtils

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
) {
    val resources = LocalContext.current.resources
    val next = progress.nextStop
    val completed = (totalStops - progress.remaining).coerceAtLeast(0)
    val approximate = navigation.route?.isApproximate == true

    FloatingIsland(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        contentPadding = 18.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DeliveryHudHeader(
                progress = progress,
                totalStops = totalStops,
                completed = completed,
                phase = navigation.phase,
                onOpenQueue = onOpenQueue,
            )

            when {
                next == null -> DeliveryHudAllDone(onEndDelivery = onEndDelivery)
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
                    )
                navigation.phase == NavigationPhase.Arrived && progress.remaining == 0 ->
                    DeliveryHudRouteFinished(onEndDelivery = onEndDelivery)
                navigation.phase == NavigationPhase.Arrived ->
                    DeliveryHudArrived(
                        stop = next,
                        onMarkDone = onMarkDone,
                        onEndDelivery = onEndDelivery,
                        onOpenExternalMaps = onOpenExternalMaps,
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
                    )
                }
                else ->
                    DeliveryHudIdleNext(
                        progress = progress,
                        stop = next,
                        distanceFromYou = distanceFromYou,
                        onStartInAppNav = onStartInAppNav,
                        onMarkDone = onMarkDone,
                        onEndDelivery = onEndDelivery,
                        onOpenExternalMaps = onOpenExternalMaps,
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
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (progress.remaining == 0) {
                stringResource(R.string.nav_done)
            } else {
                stringResource(R.string.nav_left, progress.remaining)
            },
            style = MaterialTheme.typography.labelLarge,
            color = IslandColors.onSurfaceMuted,
        )
        if (progress.remaining > 0) {
            IconButton(onClick = onOpenQueue) {
                Icon(
                    Icons.Default.FormatListBulleted,
                    contentDescription = stringResource(R.string.cd_open_stop_queue),
                    tint = IslandColors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.DeliveryHudAllDone(onEndDelivery: () -> Unit) {
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
    ExternalMapsRow(onClick = { onOpenExternalMaps(stop) })
    ActionRow(onMarkDone = onMarkDone, onEndDelivery = onEndDelivery)
}

@Composable
private fun ColumnScope.DeliveryHudRouteFinished(onEndDelivery: () -> Unit) {
    Text(
        text = stringResource(R.string.nav_finished_route),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = IslandColors.onSurface,
    )
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
) {
    Text(
        text = stringResource(R.string.nav_arrived),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
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
    )
    Button(
        onClick = onMarkDone,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.nav_mark_done_next))
    }
    ExternalMapsRow(onClick = { onOpenExternalMaps(stop) })
    TextButton(onClick = onEndDelivery, modifier = Modifier.align(Alignment.End)) {
        Text(stringResource(R.string.nav_end_delivery))
    }
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
                style = MaterialTheme.typography.bodyLarge,
                color = IslandColors.onSurfaceMuted,
                maxLines = 2,
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
    )
    ActionRow(onMarkDone = onMarkDone, onEndDelivery = onEndDelivery)
    ExternalMapsRow(onClick = { onOpenExternalMaps(stop) })
}

@Composable
private fun ColumnScope.DeliveryHudIdleNext(
    progress: DeliveryProgress,
    stop: StopEntity,
    distanceFromYou: String?,
    onStartInAppNav: () -> Unit,
    onMarkDone: () -> Unit,
    onEndDelivery: () -> Unit,
    onOpenExternalMaps: (StopEntity) -> Unit,
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
    val meta = listOfNotNull(distanceFromYou, prevLabel).joinToString(" · ")
    if (meta.isNotBlank()) {
        Text(
            text = meta,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Medium,
        )
    }
    StopNotesBanner(
        notes = stop.notes,
        addressHint = stop.addressHint,
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
    if (canNavigate) {
        ExternalMapsRow(onClick = { onOpenExternalMaps(stop) })
    }
    ActionRow(onMarkDone = onMarkDone, onEndDelivery = onEndDelivery)
}

@Composable
private fun ActionRow(
    onMarkDone: () -> Unit,
    onEndDelivery: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilledTonalButton(
            onClick = onMarkDone,
            modifier = Modifier.weight(1.2f),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.nav_mark_done))
        }
        OutlinedButton(
            onClick = onEndDelivery,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.Stop, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.nav_end))
        }
    }
}

@Composable
private fun ExternalMapsRow(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.nav_open_external))
    }
}
