package com.danielcioban.routeplanner.ui.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.local.TripHistoryEntity
import com.danielcioban.routeplanner.data.local.TripKind
import com.danielcioban.routeplanner.data.local.TripStatus
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.components.IslandListItem
import com.danielcioban.routeplanner.ui.components.MapAttributionChip
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.layout.AdaptiveSheetSlot
import com.danielcioban.routeplanner.ui.layout.AppPanes
import com.danielcioban.routeplanner.ui.layout.chromeIslandWidth
import com.danielcioban.routeplanner.ui.map.MapViewMode
import com.danielcioban.routeplanner.ui.map.RouteMapBackdrop
import com.danielcioban.routeplanner.ui.menu.ScreenMenuButton
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.HistoryFilter
import com.danielcioban.routeplanner.util.ShareFile
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@Composable
fun TripHistoryScreen(
    viewModel: TripHistoryViewModel,
    onBack: () -> Unit,
    onOpenRoute: (Long) -> Unit,
    onOpenQuickDrive: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val routeQuery by viewModel.routeQuery.collectAsStateWithLifecycle()
    val dateFormat = remember {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportChooser = stringResource(R.string.history_export_chooser)
    val exportSubject = stringResource(R.string.history_export_subject)

    Box(modifier = Modifier.fillMaxSize()) {
        RouteMapBackdrop(
            stops = emptyList(),
            mapViewMode = MapViewMode.MAP,
            showStraightStopLinks = false,
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            val wide = AppPanes.isWide(maxWidth)
            Column(modifier = Modifier.fillMaxSize()) {
            FloatingIsland(
                modifier = Modifier.chromeIslandWidth(),
                shape = RoundedCornerShape(22.dp),
                contentPadding = 12.dp,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FloatingCircleButton(onClick = onBack, embedded = true) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = IslandColors.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.history_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = IslandColors.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.history_blurb),
                            style = MaterialTheme.typography.bodySmall,
                            color = IslandColors.onSurfaceMuted,
                        )
                    }
                    ScreenMenuButton(onClick = onOpenMenu)
                }
            }

            MapAttributionChip(modifier = Modifier.padding(vertical = 8.dp))
            AdaptiveSheetSlot(
                wide = wide,
                maxExpandedHeight = 520.dp,
                fillHeight = trips.isNotEmpty(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedFilter == HistoryFilter.ALL,
                        onClick = { viewModel.setFilter(HistoryFilter.ALL) },
                        label = { Text(stringResource(R.string.history_filter_all)) },
                    )
                    FilterChip(
                        selected = selectedFilter == HistoryFilter.WEEK,
                        onClick = { viewModel.setFilter(HistoryFilter.WEEK) },
                        label = { Text(stringResource(R.string.history_filter_week)) },
                    )
                    FilterChip(
                        selected = selectedFilter == HistoryFilter.CANCELLED,
                        onClick = { viewModel.setFilter(HistoryFilter.CANCELLED) },
                        label = { Text(stringResource(R.string.history_filter_cancelled)) },
                    )
                }
                SoftOutlinedTextField(
                    value = routeQuery,
                    onValueChange = viewModel::setQuery,
                    label = stringResource(R.string.history_filter_route),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val csv = viewModel.exportFilteredCsv()
                            val file = ShareFile.writeCache(context, "trip-history.csv", csv)
                            ShareFile.share(context, file, "text/csv", exportChooser, exportSubject)
                        }
                    },
                    enabled = trips.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    Text(stringResource(R.string.history_export_csv))
                }
                if (trips.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(
                                if (selectedFilter == HistoryFilter.ALL && routeQuery.isBlank()) {
                                    R.string.history_empty
                                } else {
                                    R.string.history_filter_empty
                                },
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = IslandColors.onSurface,
                        )
                        if (selectedFilter == HistoryFilter.ALL && routeQuery.isBlank()) {
                            Text(
                                text = stringResource(R.string.history_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = IslandColors.onSurfaceMuted,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(trips, key = { it.id }) { trip ->
                            TripHistoryRow(
                                trip = trip,
                                whenLabel = dateFormat.format(Date(trip.startedAtEpochMs)),
                                onOpen = {
                                    viewModel.resumeOrOpen(trip) { action ->
                                        when (action) {
                                            is TripHistoryAction.OpenRoute -> onOpenRoute(action.routeId)
                                            TripHistoryAction.OpenQuickDrive -> onOpenQuickDrive()
                                        }
                                    }
                                },
                                onDelete = { viewModel.delete(trip.id) },
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun TripHistoryRow(
    trip: TripHistoryEntity,
    whenLabel: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val kindLabel = if (trip.kind == TripKind.QUICK) {
        stringResource(R.string.history_kind_quick)
    } else {
        stringResource(R.string.history_kind_route)
    }
    val statusLabel = when (trip.status) {
        TripStatus.IN_PROGRESS -> stringResource(R.string.history_status_in_progress)
        TripStatus.COMPLETED -> stringResource(R.string.history_status_completed)
        else -> stringResource(R.string.history_status_cancelled)
    }
    val canOpen = trip.kind == TripKind.ROUTE && trip.routeId != null && trip.routeId > 0 ||
        trip.kind == TripKind.QUICK && trip.destLatitude != null && trip.destLongitude != null
    IslandListItem(onClick = if (canOpen) onOpen else null) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = trip.title.ifBlank { trip.destName }.ifBlank {
                    stringResource(R.string.drive_here_unnamed)
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = IslandColors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$kindLabel · $statusLabel",
                style = MaterialTheme.typography.bodySmall,
                color = if (trip.status == TripStatus.IN_PROGRESS) {
                    MaterialTheme.colorScheme.primary
                } else {
                    IslandColors.onSurfaceMuted
                },
            )
            Text(
                text = whenLabel,
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.onSurfaceMuted,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.history_stops_reached,
                    trip.stopsCompleted,
                    trip.stopsCompleted,
                    trip.stopsTotal,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = IslandColors.onSurfaceMuted,
            )
        }
        if (canOpen) {
            IconButton(onClick = onOpen) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = stringResource(
                        if (trip.status == TripStatus.IN_PROGRESS) {
                            R.string.history_resume
                        } else {
                            R.string.history_open
                        },
                    ),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.action_delete),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
