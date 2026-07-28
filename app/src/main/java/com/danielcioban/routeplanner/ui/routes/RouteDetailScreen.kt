package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.util.GeoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    viewModel: RouteDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val routeWithStops by viewModel.route.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(routeWithStops?.route?.name ?: "Route") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val id = routeWithStops?.route?.id
                    if (id != null) {
                        IconButton(onClick = { onEdit(id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit route")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->
        val data = routeWithStops
        if (data == null) {
            Text(
                text = "Loading…",
                modifier = Modifier.padding(innerPadding).padding(24.dp),
            )
            return@Scaffold
        }

        val stops = data.orderedStops
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (data.route.notes.isNotBlank()) {
                item {
                    Text(data.route.notes, style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                Text(
                    text = "${data.completedCount} of ${stops.size} stops completed",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            itemsIndexed(stops, key = { _, stop -> stop.id }) { index, stop ->
                val previous = stops.getOrNull(index - 1)
                StopDetailRow(
                    index = index + 1,
                    stop = stop,
                    previous = previous,
                    onCompletedChange = { viewModel.setStopCompleted(stop.id, it) },
                )
            }
        }
    }
}

@Composable
private fun StopDetailRow(
    index: Int,
    stop: StopEntity,
    previous: StopEntity?,
    onCompletedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = stop.isCompleted,
            onCheckedChange = onCompletedChange,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("$index. ${stop.name}", style = MaterialTheme.typography.titleMedium)
            if (stop.addressHint.isNotBlank()) {
                Text(stop.addressHint, style = MaterialTheme.typography.bodyMedium)
            }
            if (stop.notes.isNotBlank()) {
                Text(
                    stop.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                )
            }
            if (stop.latitude != null && stop.longitude != null) {
                Text(
                    text = "Coords: ${"%.5f".format(stop.latitude)}, ${"%.5f".format(stop.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (previous?.latitude != null && previous.longitude != null) {
                    val meters = GeoUtils.distanceMeters(
                        previous.latitude,
                        previous.longitude,
                        stop.latitude,
                        stop.longitude,
                    )
                    val bearing = GeoUtils.bearingDegrees(
                        previous.latitude,
                        previous.longitude,
                        stop.latitude,
                        stop.longitude,
                    )
                    Text(
                        text = "Approx. from previous: ${GeoUtils.formatDistance(meters)} · ${GeoUtils.formatBearing(bearing)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            } else {
                Text(
                    text = "No coordinates yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                )
            }
        }
    }
}
