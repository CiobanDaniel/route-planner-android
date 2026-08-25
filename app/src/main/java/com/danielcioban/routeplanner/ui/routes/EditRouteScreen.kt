package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.local.ShiftSlot
import com.danielcioban.routeplanner.ui.components.AddressSearchDialog
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.components.IslandListItem
import com.danielcioban.routeplanner.ui.components.IslandListItemColumn
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.components.StopReorderControls
import com.danielcioban.routeplanner.ui.library.StopLibraryPickerDialog
import com.danielcioban.routeplanner.ui.layout.AdaptiveSheetSlot
import com.danielcioban.routeplanner.ui.layout.AppPanes
import com.danielcioban.routeplanner.ui.location.rememberUserLocation
import com.danielcioban.routeplanner.ui.map.LatLng
import com.danielcioban.routeplanner.ui.map.RouteMapBackdrop
import com.danielcioban.routeplanner.ui.menu.ScreenMenuButton
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.ui.theme.RouteColors
import com.danielcioban.routeplanner.data.settings.RouteGeofenceMode

@Composable
fun EditRouteScreen(
    viewModel: EditRouteViewModel,
    isNew: Boolean,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onOpenStopLibrary: () -> Unit = {},
    onOpenMenu: () -> Unit = {},
    savedCamera: com.danielcioban.routeplanner.data.settings.SavedMapCamera? = null,
    onCameraMoved: (com.danielcioban.routeplanner.data.settings.SavedMapCamera) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val libraryStops by viewModel.stopLibrary.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val userLocation = rememberUserLocation(autoRequest = true)
    var recenterToken by remember { mutableIntStateOf(0) }
    var showLibraryPicker by remember { mutableStateOf(false) }
    var showAddressSearch by remember { mutableStateOf(false) }
    var pendingPin by remember { mutableStateOf<LatLng?>(null) }
    var pinName by remember { mutableStateOf("") }
    var pinNotes by remember { mutableStateOf("") }
    val showStopsEditor = true

    val previewStops = remember(state.stops) {
        state.stops.mapIndexedNotNull { index, stop ->
            val lat = stop.latitudeText.toDoubleOrNull()
            val lon = stop.longitudeText.toDoubleOrNull()
            if (lat == null || lon == null) return@mapIndexedNotNull null
            com.danielcioban.routeplanner.data.local.StopEntity(
                id = stop.localId,
                routeId = 0,
                position = index,
                name = stop.name.ifBlank {
                    context.getString(R.string.edit_stop_default_name, index + 1)
                },
                latitude = lat,
                longitude = lon,
                libraryStopId = stop.libraryStopId,
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
            savedCamera = savedCamera,
            onCameraMoved = onCameraMoved,
            onMapLongClick = { pin ->
                pendingPin = pin
                pinName = ""
                pinNotes = ""
            },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FloatingCircleButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                ScreenMenuButton(onClick = onOpenMenu, embedded = false)
                Spacer(modifier = Modifier.weight(1f))
                FloatingCircleButton(onClick = { showAddressSearch = true }) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.cd_search_place),
                        tint = IslandColors.onSurface,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                FloatingCircleButton(onClick = { showLibraryPicker = true }) {
                    Icon(
                        Icons.Default.BookmarkBorder,
                        contentDescription = stringResource(R.string.cd_add_from_library),
                        tint = IslandColors.onSurface,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
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
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = stringResource(R.string.cd_my_location),
                    )
                }
            }

            AdaptiveSheetSlot(
                wide = wide,
                maxExpandedHeight = 520.dp,
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
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 4.dp),
                    ) {
                        item {
                            Text(
                                text = stringResource(
                                    if (isNew) R.string.edit_new_route else R.string.edit_edit_route,
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = IslandColors.onSurface,
                            )
                            Text(
                                text = stringResource(
                                    if (isNew) R.string.edit_hint_new else R.string.edit_hint_existing,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = IslandColors.onSurfaceMuted,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        item {
                            SoftField(
                                value = state.routeName,
                                onValueChange = viewModel::updateRouteName,
                                label = stringResource(R.string.edit_route_name),
                            )
                        }
                        item {
                            SoftField(
                                value = state.routeNotes,
                                onValueChange = viewModel::updateRouteNotes,
                                label = stringResource(R.string.edit_notes_optional),
                                singleLine = false,
                            )
                        }
                        item {
                            SoftField(
                                value = state.vanName,
                                onValueChange = viewModel::updateVanName,
                                label = stringResource(R.string.route_van_name),
                            )
                        }
                        item {
                            SoftField(
                                value = state.shiftName,
                                onValueChange = viewModel::updateShiftName,
                                label = stringResource(R.string.route_shift_name),
                            )
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.route_shift_slot),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IslandColors.onSurface,
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    FilterChip(
                                        selected = state.shiftSlot.isEmpty(),
                                        onClick = { viewModel.updateShiftSlot("") },
                                        label = { Text(stringResource(R.string.home_shift_unset)) },
                                    )
                                    FilterChip(
                                        selected = state.shiftSlot == ShiftSlot.MORNING,
                                        onClick = { viewModel.updateShiftSlot(ShiftSlot.MORNING) },
                                        label = { Text(stringResource(R.string.home_filter_morning)) },
                                    )
                                    FilterChip(
                                        selected = state.shiftSlot == ShiftSlot.AFTERNOON,
                                        onClick = { viewModel.updateShiftSlot(ShiftSlot.AFTERNOON) },
                                        label = { Text(stringResource(R.string.home_filter_afternoon)) },
                                    )
                                }
                            }
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.route_color),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IslandColors.onSurface,
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    val selected = state.colorHex.ifBlank { RouteColors.DEFAULT_HEX }
                                    RouteColors.palette.forEach { hex ->
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(RouteColors.parse(hex))
                                                .border(
                                                    width = if (hex.equals(selected, ignoreCase = true)) {
                                                        3.dp
                                                    } else {
                                                        0.dp
                                                    },
                                                    color = IslandColors.onSurface,
                                                    shape = CircleShape,
                                                )
                                                .clickable { viewModel.updateColorHex(hex) },
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.edit_round_trip),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = IslandColors.onSurface,
                                    )
                                    Text(
                                        text = stringResource(R.string.edit_round_trip_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = IslandColors.onSurfaceMuted,
                                    )
                                }
                                Switch(
                                    checked = state.roundTrip,
                                    onCheckedChange = viewModel::updateRoundTrip,
                                )
                            }
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.edit_geofence_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IslandColors.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.edit_geofence_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IslandColors.onSurfaceMuted,
                                )
                                RouteGeofenceMode.entries.forEach { mode ->
                                    IslandListItem(
                                        onClick = { viewModel.updateGeofenceMode(mode) },
                                        selected = state.geofenceMode == mode,
                                    ) {
                                        Text(
                                            text = geofenceModeLabel(mode),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = IslandColors.onSurface,
                                        )
                                    }
                                }
                                GeofenceRadiusField(
                                    radiusMeters = state.geofenceRadiusMeters,
                                    onChange = viewModel::updateGeofenceRadius,
                                    resetKey = "route",
                                )
                            }
                        }
                        item {
                            Column {
                                Text(
                                    text = stringResource(R.string.edit_stops),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                FlowRow(modifier = Modifier.fillMaxWidth()) {
                                    if (state.stops.size >= 2) {
                                        TextButton(onClick = viewModel::optimizeStopOrder) {
                                            Text(stringResource(R.string.action_optimize_order))
                                        }
                                        TextButton(onClick = viewModel::reverseStopOrder) {
                                            Text(stringResource(R.string.action_reverse_order))
                                        }
                                    }
                                    if (state.stops.any { !it.isCompleted && it.arriveByMinutes != null }) {
                                        TextButton(onClick = viewModel::sortStopsByArriveBy) {
                                            Text(stringResource(R.string.action_sort_arrive_by))
                                        }
                                    }
                                    TextButton(onClick = { showLibraryPicker = true }) {
                                        Text(stringResource(R.string.library_pick_title))
                                    }
                                }
                            }
                        }
                        if (!showStopsEditor || state.stops.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.edit_no_stops),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (showStopsEditor) {
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
                                    onMoveToTop = { viewModel.moveStopToTop(stop.localId) },
                                    onMoveToBottom = { viewModel.moveStopToBottom(stop.localId) },
                                )
                            }
                        }
                        if (state.errorRes != null) {
                            item {
                                Text(
                                    text = state.errorArg?.let { arg ->
                                        stringResource(state.errorRes!!, arg)
                                    } ?: stringResource(state.errorRes!!),
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
                                Text(
                                    stringResource(
                                        if (isNew) R.string.edit_create else R.string.edit_save_changes,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
            }
        }

        if (showLibraryPicker) {
            StopLibraryPickerDialog(
                stops = libraryStops,
                onDismiss = { showLibraryPicker = false },
                onPick = { stop -> viewModel.addStopFromLibrary(stop.id) },
                onManageLibrary = onOpenStopLibrary,
                alreadyOnRouteLibraryIds = state.stops.mapNotNull { it.libraryStopId }.toSet(),
            )
        }

        if (showAddressSearch) {
            AddressSearchDialog(
                onDismissRequest = { showAddressSearch = false },
                onPlaceSelected = { place ->
                    showAddressSearch = false
                    viewModel.addPinnedStop(
                        name = place.shortName,
                        latitude = place.latitude,
                        longitude = place.longitude,
                        addressHint = place.displayName,
                        phone = place.phone,
                    )
                },
                near = userLocation.coordinate,
            )
        }

        pendingPin?.let { pin ->
            AddStopFromPinDialog(
                pin = PendingStopPin(
                    latitude = pin.latitude,
                    longitude = pin.longitude,
                ),
                name = pinName,
                onNameChange = { pinName = it },
                notes = pinNotes,
                onNotesChange = { pinNotes = it },
                onDismiss = { pendingPin = null },
                onConfirm = {
                    viewModel.addPinnedStop(
                        name = pinName.ifBlank {
                            context.getString(R.string.gps_stop_default_name)
                        },
                        latitude = pin.latitude,
                        longitude = pin.longitude,
                    )
                    pendingPin = null
                },
                libraryHint = false,
            )
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
    onMoveToTop: () -> Unit,
    onMoveToBottom: () -> Unit,
) {
    val hasPin = stop.latitudeText.isNotBlank() && stop.longitudeText.isNotBlank()
    IslandListItemColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SoftField(
            value = stop.name,
            onValueChange = { value -> onChange { it.copy(name = value) } },
            label = stringResource(R.string.edit_stop_label, index),
        )
        if (stop.libraryStopId != null) {
            Text(
                text = stringResource(R.string.library_linked_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        SoftOutlinedTextField(
            value = stop.notes,
            onValueChange = { value -> onChange { it.copy(notes = value) } },
            label = stringResource(R.string.dialog_delivery_notes),
            placeholder = stringResource(R.string.dialog_delivery_notes_hint),
            singleLine = false,
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        ArriveByControls(
            arriveByMinutes = stop.arriveByMinutes,
            serviceMinutes = stop.serviceMinutes,
            enabled = true,
            onArriveByChange = { minutes -> onChange { it.copy(arriveByMinutes = minutes) } },
            onServiceMinutesChange = { minutes -> onChange { it.copy(serviceMinutes = minutes) } },
            resetKey = stop.localId,
            arriveByEpochMs = stop.arriveByEpochMs,
            onArriveByEpochChange = { epoch -> onChange { it.copy(arriveByEpochMs = epoch) } },
        )
        SoftOutlinedTextField(
            value = stop.phone,
            onValueChange = { value -> onChange { it.copy(phone = value) } },
            label = stringResource(R.string.stop_phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SoftOutlinedTextField(
            value = stop.doorCode,
            onValueChange = { value -> onChange { it.copy(doorCode = value) } },
            label = stringResource(R.string.stop_door_code),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SoftOutlinedTextField(
            value = stop.codAmountText,
            onValueChange = { value -> onChange { it.copy(codAmountText = value) } },
            label = stringResource(R.string.cod_amount),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SoftOutlinedTextField(
            value = stop.barcode,
            onValueChange = { value -> onChange { it.copy(barcode = value) } },
            label = stringResource(R.string.stop_barcode),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = stop.isFixedOrder,
                onCheckedChange = { checked -> onChange { it.copy(isFixedOrder = checked) } },
            )
            Text(stringResource(R.string.stop_fixed_order))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = stop.isBreak,
                onCheckedChange = { checked -> onChange { it.copy(isBreak = checked) } },
            )
            Text(stringResource(R.string.stop_break))
        }
        GeofenceRadiusField(
            radiusMeters = stop.geofenceRadiusMeters,
            onChange = { meters -> onChange { it.copy(geofenceRadiusMeters = meters) } },
            resetKey = stop.localId,
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
                text = stringResource(
                    if (hasPin) R.string.edit_pinned else R.string.edit_no_pin,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.onSurfaceMuted,
                modifier = Modifier.weight(1f),
            )
            StopReorderControls(
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onMoveToTop = onMoveToTop,
                onMoveToBottom = onMoveToBottom,
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_remove))
            }
        }
    }
}

@Composable
private fun geofenceModeLabel(mode: RouteGeofenceMode): String = when (mode) {
    RouteGeofenceMode.INHERIT -> stringResource(R.string.geofence_mode_inherit)
    RouteGeofenceMode.OFF -> stringResource(R.string.geofence_mode_off)
    RouteGeofenceMode.VISITED -> stringResource(R.string.geofence_mode_visited)
    RouteGeofenceMode.COMPLETE -> stringResource(R.string.geofence_mode_complete)
}
