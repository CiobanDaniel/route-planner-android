package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRouteScreen(
    viewModel: EditRouteViewModel,
    isNew: Boolean,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedRouteId) {
        state.savedRouteId?.let(onSaved)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New route" else "Edit route") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = state.routeName,
                    onValueChange = viewModel::updateRouteName,
                    label = { Text("Route name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = state.routeNotes,
                    onValueChange = viewModel::updateRouteNotes,
                    label = { Text("Route notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
            item {
                Text("Stops", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Add latitude/longitude for shops on roads missing from maps. Leave blank if unknown.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            itemsIndexed(state.stops, key = { _, stop -> stop.localId }) { index, stop ->
                StopEditorCard(
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
            item {
                TextButton(onClick = viewModel::addStop) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add stop", modifier = Modifier.padding(start = 8.dp))
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
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(if (isNew) "Create route" else "Save changes")
                }
            }
        }
    }
}

@Composable
private fun StopEditorCard(
    index: Int,
    stop: EditableStop,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onChange: ((EditableStop) -> EditableStop) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Stop $index", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove stop")
            }
        }
        OutlinedTextField(
            value = stop.name,
            onValueChange = { value -> onChange { it.copy(name = value) } },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = stop.addressHint,
            onValueChange = { value -> onChange { it.copy(addressHint = value) } },
            label = { Text("Address / landmark (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = stop.latitudeText,
                onValueChange = { value -> onChange { it.copy(latitudeText = value) } },
                label = { Text("Latitude") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("45.7489") },
            )
            OutlinedTextField(
                value = stop.longitudeText,
                onValueChange = { value -> onChange { it.copy(longitudeText = value) } },
                label = { Text("Longitude") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("21.2257") },
            )
        }
        OutlinedTextField(
            value = stop.notes,
            onValueChange = { value -> onChange { it.copy(notes = value) } },
            label = { Text("Stop notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
    }
}
