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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
                        next == null -> "Route finished"
                        navigation.phase == NavigationPhase.Navigating ||
                            navigation.phase == NavigationPhase.LoadingRoute ||
                            navigation.phase == NavigationPhase.Arrived ->
                            "Navigating · ${completed + 1} of $totalStops"
                        else -> "Next stop · ${completed + 1} of $totalStops"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (progress.remaining == 0) "Done" else "${progress.remaining} left",
                    style = MaterialTheme.typography.labelLarge,
                    color = IslandColors.onSurfaceMuted,
                )
            }

            when {
                next == null -> {
                    Text(
                        text = "All stops completed",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = IslandColors.onSurface,
                    )
                    Text(
                        text = "End delivery when you’re ready.",
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
                        Text("End delivery")
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
                                text = "Calculating road route…",
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
                        text = navigation.errorMessage ?: "Couldn’t calculate a route",
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
                        Text("Retry route")
                    }
                    ExternalMapsRow(onClick = { onOpenExternalMaps(next) })
                    ActionRow(onMarkDone = onMarkDone, onEndDelivery = onEndDelivery)
                }

                navigation.phase == NavigationPhase.Arrived && progress.remaining == 0 -> {
                    Text(
                        text = "You’ve finished the route",
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
                        Text("End delivery")
                    }
                }

                navigation.phase == NavigationPhase.Arrived -> {
                    Text(
                        text = "You’ve arrived",
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
                        Text("Mark done · next stop")
                    }
                    ExternalMapsRow(onClick = { onOpenExternalMaps(next) })
                    TextButton(onClick = onEndDelivery, modifier = Modifier.align(Alignment.End)) {
                        Text("End delivery")
                    }
                }

                navigation.phase == NavigationPhase.Navigating && navigation.guidance != null -> {
                    val guidance = navigation.guidance
                    val step = guidance.currentStep
                    Text(
                        text = GeoUtils.formatDistance(guidance.distanceToManeuverMeters, distanceUnit),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = IslandColors.onSurface,
                    )
                    Text(
                        text = step?.instruction ?: "Continue to ${next.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = IslandColors.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    guidance.thenStep?.let { then ->
                        Text(
                            text = "Then: ${then.instruction}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = IslandColors.onSurfaceMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
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
                    // Delivery active but in-app nav not started / idle
                    Text(
                        text = next.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = IslandColors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = buildList {
                        distanceFromYou?.let { add(it) }
                        progress.approxFromPrevious?.let { add("prev $it") }
                    }.joinToString(" · ")
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
                        Text(if (canNavigate) "Start navigation" else "No map pin")
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
            Text("Mark done")
        }
        OutlinedButton(
            onClick = onEndDelivery,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.Stop, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("End")
        }
    }
}

@Composable
private fun ExternalMapsRow(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Open in Google Maps / Waze")
    }
}
