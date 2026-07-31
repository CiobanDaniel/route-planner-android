package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.location.rememberUserLocation
import com.danielcioban.routeplanner.ui.map.RouteMapBackdrop
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun EditRouteScreen(
    viewModel: EditRouteViewModel,
    isNew: Boolean,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val userLocation = rememberUserLocation(autoRequest = true)
    var recenterToken by remember { mutableIntStateOf(0) }

    val previewStops = remember(state.stops) {
        state.stops.mapIndexedNotNull { index, stop ->
            val lat = stop.latitudeText.toDoubleOrNull()
            val lon = stop.longitudeText.toDoubleOrNull()
            if (lat == null || lon == null) return@mapIndexedNotNull null
            com.danielcioban.routeplanner.data.local.StopEntity(
                id = stop.localId,
                routeId = 0,
                position = index,
                name = stop.name.ifBlank { "Stop ${index + 1}" },
                latitude = lat,
                longitude = lon,
            )
        }
    }

    LaunchedEffect(state.savedRouteId) {
        state.savedRouteId?.let(onSaved)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RouteMapBackdrop(
            stops = previewStops,
            fitStops = previewStops.size >= 2,
            userLocation = userLocation.coordinate,
            recenterToken = recenterToken,
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.weight(1f))
                FloatingCircleButton(
                    onClick = {
                        if (!userLocation.hasPermission) {
                            userLocation.requestPermission()
                        } else {
                            userLocation.refresh()
                            recenterToken++
                        }
                    },
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My location")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            FloatingIsland(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                shape = RoundedCornerShape(28.dp),
                contentPadding = 16.dp,
            ) {
                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 4.dp),
                    ) {
                        item {
                            Text(
                                text = if (isNew) "New route" else "Edit route",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = IslandColors.onSurface,
                            )
                            Text(
                                text = if (isNew) {
                                    "Name it, save, then drop stops on the map."
                                } else {
                                    "Rename, reorder, or remove stops. Pins stay on the map."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = IslandColors.onSurfaceMuted,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        item {
                            SoftField(
                                value = state.routeName,
                                onValueChange = viewModel::updateRouteName,
                                label = "Route name",
                            )
                        }
                        item {
                            SoftField(
                                value = state.routeNotes,
                                onValueChange = viewModel::updateRouteNotes,
                                label = "Notes (optional)",
                                singleLine = false,
                            )
                        }
                        if (!isNew) {
                            item {
                                Text(
                                    text = "Stops",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            if (state.stops.isEmpty()) {
                                item {
                                    Text(
                                        text = "No stops yet — go back and long-press the map to add some.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            itemsIndexed(state.stops, key = { _, stop -> stop.localId }) { index, stop ->
                                CompactStopRow(
                                    index = index + 1,
                                    stop = stop,
                                    canMoveUp = index > 0,
                                    canMoveDown = index < state.stops.lastIndex,
                                    onChange = { viewModel.updateStop(stop.localId, it) },
                                    onRemove = { viewModel.removeStop(stop.localId) },
                                    onMoveUp = { viewModel.moveStopUp(stop.localId) },
                                    onMoveDown = { viewModel.moveStopDown(stop.localId) },
                                )
                            }
                        }
                        if (state.errorMessage != null) {
                            item {
                                Text(
                                    text = state.errorMessage.orEmpty(),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        item {
                            Button(
                                onClick = viewModel::save,
                                enabled = !state.isSaving,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .padding(end = 8.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                                Text(if (isNew) "Create route" else "Save changes")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoftField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    SoftOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = 2,
    )
}

@Composable
private fun CompactStopRow(
    index: Int,
    stop: EditableStop,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onChange: ((EditableStop) -> EditableStop) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val hasPin = stop.latitudeText.isNotBlank() && stop.longitudeText.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(IslandColors.surface, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SoftField(
            value = stop.name,
            onValueChange = { value -> onChange { it.copy(name = value) } },
            label = "Stop $index",
        )
        SoftOutlinedTextField(
            value = stop.notes,
            onValueChange = { value -> onChange { it.copy(notes = value) } },
            label = "Delivery notes",
            placeholder = "Gate code, phone, leave at door…",
            singleLine = false,
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = if (hasPin) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (hasPin) "Pinned on map" else "No map pin",
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.onSurfaceMuted,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        }
    }
}
