package com.danielcioban.routeplanner.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.settings.DistanceUnit
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.layout.AppPanes
import com.danielcioban.routeplanner.ui.layout.readableWidth
import com.danielcioban.routeplanner.ui.map.MapViewMode
import com.danielcioban.routeplanner.ui.map.RouteMapBackdrop
import com.danielcioban.routeplanner.ui.menu.ScreenMenuButton
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.GeoUtils
import java.util.Locale

@Composable
fun TripStatsScreen(
    viewModel: TripStatsViewModel,
    distanceUnit: DistanceUnit,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val fuel by viewModel.fuel.collectAsStateWithLifecycle()
    val liters = fuel.mapNotNull { it.liters }.sum()
    val km = fuel.mapNotNull { it.odometerKm }.let { readings ->
        if (readings.size < 2) 0.0 else readings.maxOrNull()!! - readings.minOrNull()!!
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RouteMapBackdrop(
            stops = emptyList(),
            mapViewMode = MapViewMode.MAP,
            showStraightStopLinks = false,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FloatingCircleButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = IslandColors.onSurface,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                ScreenMenuButton(onClick = onOpenMenu, embedded = false)
                Spacer(modifier = Modifier.width(12.dp))
                FloatingIsland(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = AppPanes.TitleMaxWidth),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = 16.dp,
                ) {
                    Text(
                        text = stringResource(R.string.stats_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = IslandColors.onSurface,
                    )
                }
            }
            Spacer(modifier = Modifier.padding(top = 16.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
            FloatingIsland(
                modifier = Modifier.readableWidth(),
                shape = RoundedCornerShape(28.dp),
                contentPadding = 16.dp,
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatRow(
                        stringResource(R.string.stats_today_stops),
                        summary.todayStops.toString(),
                    )
                    StatRow(
                        stringResource(R.string.stats_today_km),
                        GeoUtils.formatDistance(summary.todayDistanceMeters, distanceUnit),
                    )
                    StatRow(
                        stringResource(R.string.stats_today_late),
                        summary.todayLate.toString(),
                    )
                    StatRow(
                        stringResource(R.string.stats_trips),
                        summary.completedTrips.toString(),
                    )
                    StatRow(
                        stringResource(R.string.stats_stops),
                        summary.stopsCompleted.toString(),
                    )
                    StatRow(
                        stringResource(R.string.stats_distance),
                        GeoUtils.formatDistance(summary.distanceMeters, distanceUnit),
                    )
                    StatRow(
                        stringResource(R.string.stats_late),
                        summary.lateStops.toString(),
                    )
                    StatRow(
                        stringResource(R.string.stats_per_hour),
                        String.format(Locale.getDefault(), "%.1f", summary.stopsPerHour),
                    )
                    StatRow(
                        stringResource(R.string.stats_cod),
                        String.format(Locale.getDefault(), "%.2f", summary.codCollected),
                    )
                    StatRow(
                        stringResource(R.string.stats_fuel_liters),
                        String.format(Locale.getDefault(), "%.1f", liters),
                    )
                    if (km > 0) {
                        StatRow(
                            stringResource(R.string.stats_fuel_span),
                            GeoUtils.formatDistance(km * 1000.0, distanceUnit),
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = IslandColors.onSurface,
        )
    }
}
