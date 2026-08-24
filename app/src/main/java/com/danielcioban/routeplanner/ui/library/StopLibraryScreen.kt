package com.danielcioban.routeplanner.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.LibraryDeleteScope
import com.danielcioban.routeplanner.data.LibraryEditScope
import com.danielcioban.routeplanner.data.LibraryUsage
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import com.danielcioban.routeplanner.ui.components.CollapsibleBottomIsland
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.components.IslandListItem
import com.danielcioban.routeplanner.ui.components.MapAttributionChip
import com.danielcioban.routeplanner.ui.map.LatLng
import com.danielcioban.routeplanner.ui.map.MapViewMode
import com.danielcioban.routeplanner.ui.map.RouteMapBackdrop
import com.danielcioban.routeplanner.ui.map.libraryStopsToMapStops
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun StopLibraryScreen(
    viewModel: StopLibraryViewModel,
    onBack: () -> Unit,
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val stops = items.map { it.stop }
    var editingStop by remember { mutableStateOf<StopLibraryEntity?>(null) }
    var pendingLibraryEdit by remember { mutableStateOf<PendingLibraryEdit?>(null) }
    var deletingItem by remember { mutableStateOf<LibraryStopItem?>(null) }
    var pendingPin by remember { mutableStateOf<LatLng?>(null) }
    var newStopName by remember { mutableStateOf("") }
    var newStopNotes by remember { mutableStateOf("") }
    var focusTarget by remember { mutableStateOf<LatLng?>(null) }
    var focusToken by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        RouteMapBackdrop(
            stops = libraryStopsToMapStops(stops),
            fitStops = stops.isNotEmpty(),
            mapViewMode = MapViewMode.MAP,
            showStraightStopLinks = false,
            focusTarget = focusTarget,
            focusToken = focusToken,
            onMapLongClick = { pin ->
                pendingPin = pin
                newStopName = ""
                newStopNotes = ""
            },
            onStopClick = { libraryId ->
                items.firstOrNull { it.stop.id == libraryId }?.let { item ->
                    focusTarget = LatLng(item.stop.latitude, item.stop.longitude)
                    focusToken++
                    editingStop = item.stop
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            FloatingIsland(
                modifier = Modifier.fillMaxWidth(),
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
                            text = stringResource(R.string.library_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = IslandColors.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.library_blurb),
                            style = MaterialTheme.typography.bodySmall,
                            color = IslandColors.onSurfaceMuted,
                        )
                    }
                }
            }

            MapAttributionChip(modifier = Modifier.padding(vertical = 8.dp))

            Spacer(modifier = Modifier.weight(1f))

            CollapsibleBottomIsland(
                modifier = Modifier.fillMaxWidth(),
                maxExpandedHeight = 420.dp,
            ) {
                if (items.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.library_empty),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = IslandColors.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.library_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = IslandColors.onSurfaceMuted,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items, key = { it.stop.id }) { item ->
                            LibraryStopRow(
                                item = item,
                                onEdit = {
                                    focusTarget = LatLng(item.stop.latitude, item.stop.longitude)
                                    focusToken++
                                    editingStop = item.stop
                                },
                                onDelete = { deletingItem = item },
                            )
                        }
                    }
                }
            }
        }

        pendingPin?.let { pin ->
            com.danielcioban.routeplanner.ui.routes.AddStopFromPinDialog(
                pin = com.danielcioban.routeplanner.ui.routes.PendingStopPin(
                    latitude = pin.latitude,
                    longitude = pin.longitude,
                ),
                name = newStopName,
                onNameChange = { newStopName = it },
                notes = newStopNotes,
                onNotesChange = { newStopNotes = it },
                onDismiss = { pendingPin = null },
                onConfirm = {
                    viewModel.create(
                        name = newStopName,
                        addressHint = "",
                        notes = newStopNotes,
                        latitude = pin.latitude,
                        longitude = pin.longitude,
                    )
                    pendingPin = null
                },
            )
        }

        editingStop?.let { stop ->
            EditLibraryStopDialog(
                stop = stop,
                onDismiss = { editingStop = null },
                onSave = { name, addressHint, notes, latitude, longitude ->
                    val item = items.firstOrNull { it.stop.id == stop.id }
                    if (item != null && item.usedOn.size > 1) {
                        pendingLibraryEdit = PendingLibraryEdit(
                            stopId = stop.id,
                            name = name,
                            addressHint = addressHint,
                            notes = notes,
                            latitude = latitude,
                            longitude = longitude,
                            usage = LibraryUsage(
                                libraryStopId = stop.id,
                                routes = emptyList(),
                            ),
                            routeNames = item.usedOn,
                        )
                    } else {
                        viewModel.applyEdit(
                            id = stop.id,
                            name = name,
                            addressHint = addressHint,
                            notes = notes,
                            latitude = latitude,
                            longitude = longitude,
                            scope = LibraryEditScope.Global,
                        )
                    }
                    editingStop = null
                },
            )
        }

        pendingLibraryEdit?.let { edit ->
            LibraryEditScopeDialog(
                stopName = edit.name,
                routeNames = edit.routeNames,
                showThisRoute = false,
                onEverywhere = {
                    viewModel.applyEdit(
                        id = edit.stopId,
                        name = edit.name,
                        addressHint = edit.addressHint,
                        notes = edit.notes,
                        latitude = edit.latitude,
                        longitude = edit.longitude,
                        scope = LibraryEditScope.Global,
                    )
                    pendingLibraryEdit = null
                },
                onThisRoute = { pendingLibraryEdit = null },
                onSaveAsCopy = {
                    viewModel.applyEdit(
                        id = edit.stopId,
                        name = edit.name,
                        addressHint = edit.addressHint,
                        notes = edit.notes,
                        latitude = edit.latitude,
                        longitude = edit.longitude,
                        scope = LibraryEditScope.SaveAsCopy,
                    )
                    pendingLibraryEdit = null
                },
                onDismiss = { pendingLibraryEdit = null },
            )
        }

        deletingItem?.let { item ->
            LibraryDeleteScopeDialog(
                stopName = item.stop.name,
                routeNames = item.usedOn,
                showThisRoute = false,
                onEverywhere = {
                    viewModel.delete(item.stop.id, LibraryDeleteScope.Everywhere)
                    deletingItem = null
                },
                onThisRoute = { deletingItem = null },
                onDismiss = { deletingItem = null },
            )
        }
    }
}

private data class PendingLibraryEdit(
    val stopId: Long,
    val name: String,
    val addressHint: String,
    val notes: String,
    val latitude: Double,
    val longitude: Double,
    val usage: LibraryUsage,
    val routeNames: List<String>,
)

@Composable
private fun LibraryStopRow(
    item: LibraryStopItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val stop = item.stop
    IslandListItem(onClick = onEdit) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stop.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = IslandColors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (stop.addressHint.isNotBlank()) {
                Text(
                    text = stop.addressHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = IslandColors.onSurfaceMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (stop.notes.isNotBlank()) {
                Text(
                    text = stop.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = IslandColors.onSurfaceMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = if (item.usedOn.isEmpty()) {
                    stringResource(R.string.library_usage_none)
                } else {
                    pluralStringResource(
                        R.plurals.library_usage_count,
                        item.usedOn.size,
                        item.usedOn.size,
                    ) + " · " + item.usedOn.joinToString(", ")
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (item.usedOn.isEmpty()) {
                    IslandColors.onSurfaceMuted
                } else {
                    MaterialTheme.colorScheme.primary
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.cd_delete_library_stop),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
