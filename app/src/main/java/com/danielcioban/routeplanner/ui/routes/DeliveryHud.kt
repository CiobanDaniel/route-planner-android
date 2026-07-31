package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    distanceUnit: DistanceUnit = DistanceUnit.METRIC,
    modifier: Modifier = Modifier,
) {
    val next = progress.nextStop
    val completed = (totalStops - progress.remaining).coerceAtLeast(0)
    val approximate = navigation.route?.isApproximate == true

    FloatingIsland(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        contentPadding = 18.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        next == null -> stringResource(R.string.nav_route_finished)
                        navigation.phase == NavigationPhase.Navigating ||
                            navigation.phase == NavigationPhase.LoadingRoute ||
                            navigation.phase == NavigationPhase.Arrived ->
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
            }

            when {
                next == null -> {
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

                navigation.phase == NavigationPhase.LoadingRoute -> {
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
                                text = next.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = IslandColors.onSurfaceMuted,
                            )
                        }
                    }
                }

                navigation.phase == NavigationPhase.Error -> {
                    Text(
                        text = next.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = IslandColors.onSurface,
                    )
                    Text(
                        text = stringResource(navigation.errorMessageRes ?: R.string.nav_error_generic),
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
                    ExternalMapsRow(onClick = { onOpenExternalMaps(next) })
                    ActionRow(onMarkDone = onMarkDone, onEndDelivery = onEndDelivery)
                }

                navigation.phase == NavigationPhase.Arrived && progress.remaining == 0 -> {
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

                navigation.phase == NavigationPhase.Arrived -> {
                    Text(
                        text = stringResource(R.string.nav_arrived),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = next.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = IslandColors.onSurface,
                    )
                    StopNotesBanner(
                        notes = next.notes,
                        addressHint = next.addressHint,
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
                    ExternalMapsRow(onClick = { onOpenExternalMaps(next) })
                    TextButton(onClick = onEndDelivery, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(R.string.nav_end_delivery))
                    }
                }

                navigation.phase == NavigationPhase.Navigating && navigation.guidance != null -> {
                    val guidance = navigation.guidance
                    val step = guidance.currentStep
                    if (approximate) {
                        Text(
                            text = stringResource(R.string.nav_approx_banner),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = GeoUtils.formatDistance(guidance.distanceToManeuverMeters, distanceUnit),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = IslandColors.onSurface,
                    )
                    Text(
                        text = when {
                            approximate -> stringResource(R.string.nav_approx_head, next.name)
                            step != null -> step.instruction
                            else -> stringResource(R.string.nav_continue_to, next.name)
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
                                text = stringResource(R.string.nav_then, then.instruction),
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
                            append(next.name)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = IslandColors.onSurfaceMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    StopNotesBanner(
                        notes = next.notes,
                        addressHint = next.addressHint,
                        compact = true,
                    )
                    ActionRow(onMarkDone = onMarkDone, onEndDelivery = onEndDelivery)
                    ExternalMapsRow(onClick = { onOpenExternalMaps(next) })
                }

                else -> {
                    Text(
                        text = next.name,
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
                        notes = next.notes,
                        addressHint = next.addressHint,
                    )
                    val canNavigate = next.latitude != null && next.longitude != null
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
                        ExternalMapsRow(onClick = { onOpenExternalMaps(next) })
                    }
                    ActionRow(onMarkDone = onMarkDone, onEndDelivery = onEndDelivery)
                }
            }
        }
    }
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
