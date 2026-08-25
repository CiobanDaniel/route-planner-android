package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.geocoding.NominatimGeocodingClient
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.local.StopTaskEntity
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.OpenLocationCode
import com.danielcioban.routeplanner.util.PlaceShare
import kotlinx.coroutines.delay

@Composable
fun EndDeliveryDialog(
    onDismiss: () -> Unit,
    onEndKeepProgress: () -> Unit,
    onEndAndReset: () -> Unit,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.dialog_end_delivery_title),
        confirmLabel = stringResource(R.string.nav_end_delivery),
        onConfirm = onEndKeepProgress,
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(
            text = stringResource(R.string.dialog_end_delivery_body),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        TextButton(
            onClick = onEndAndReset,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(
                stringResource(R.string.dialog_end_and_reset),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
fun ResetProgressDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.dialog_reset_progress_title),
        confirmLabel = stringResource(R.string.action_reset_progress),
        onConfirm = onConfirm,
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(
            text = stringResource(R.string.dialog_reset_progress_body),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
    }
}

@Composable
fun AddStopFromPinDialog(
    pin: PendingStopPin,
    name: String,
    onNameChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    libraryHint: Boolean = true,
) {
    val geocoder = remember { NominatimGeocodingClient() }
    val languageTag = LocalConfiguration.current.locales[0]?.toLanguageTag()
    val context = LocalContext.current
    var resolvedAddress by remember(pin.latitude, pin.longitude) { mutableStateOf(pin.addressHint) }
    LaunchedEffect(pin.latitude, pin.longitude, pin.suggestedName) {
        if (name.isBlank() && pin.suggestedName.isNotBlank()) {
            onNameChange(pin.suggestedName)
        }
        if (pin.suggestedName.isNotBlank() || pin.addressHint.isNotBlank()) return@LaunchedEffect
        delay(650)
        geocoder.reverse(pin.latitude, pin.longitude, languageTag).getOrNull()?.let { place ->
            resolvedAddress = place.displayName
            if (name.isBlank()) onNameChange(place.shortName)
        }
    }
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.dialog_add_stop_title),
        confirmLabel = stringResource(R.string.action_add),
        onConfirm = onConfirm,
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(
            text = "${"%.5f".format(pin.latitude)}, ${"%.5f".format(pin.longitude)}  ${OpenLocationCode.encode(pin.latitude, pin.longitude)}",
            style = MaterialTheme.typography.bodySmall,
            color = IslandColors.onSurfaceMuted,
        )
        TextButton(
            onClick = { PlaceShare.copyCoordinates(context, pin.latitude, pin.longitude) },
        ) {
            Text(stringResource(R.string.place_copy_coords))
        }
        val hint = resolvedAddress.ifBlank { pin.addressHint }
        if (hint.isNotBlank()) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.onSurfaceMuted,
            )
        }
        SoftOutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = stringResource(R.string.dialog_stop_name),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SoftOutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = stringResource(R.string.dialog_delivery_notes),
            placeholder = stringResource(R.string.dialog_delivery_notes_hint),
            singleLine = false,
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        if (libraryHint) {
            Text(
                text = stringResource(R.string.library_add_saves_to_library),
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.onSurfaceMuted,
            )
        }
    }
}

@Composable
fun SelectedStopDialogs(
    stop: StopEntity,
    tasks: List<StopTaskEntity>,
    usageRouteNames: List<String> = emptyList(),
    placeReadOnly: Boolean = false,
    templates: List<com.danielcioban.routeplanner.data.local.TaskTemplateEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        notes: String,
        arriveByMinutes: Int?,
        serviceMinutes: Int,
        geofenceRadiusMeters: Int?,
        arriveByEpochMs: Long?,
        phone: String,
        doorCode: String,
        isFixedOrder: Boolean,
        isBreak: Boolean,
        codAmount: Double,
        barcode: String,
    ) -> Unit,
    onDelete: () -> Unit,
    onAddTask: (title: String, required: Boolean) -> Unit,
    onApplyTemplate: (templateId: Long) -> Unit = {},
    onApplyTemplateToRemaining: (templateId: Long) -> Unit = {},
    onTaskCompletedChange: (taskId: Long, completed: Boolean, note: String) -> Unit,
    onCompletionNoteChange: (taskId: Long, note: String) -> Unit,
    onUpdateTask: (taskId: Long, title: String, required: Boolean) -> Unit,
    onDeleteTask: (taskId: Long) -> Unit,
    onNavigate: () -> Unit,
    onOpenExternalMaps: () -> Unit,
    onCall: () -> Unit = {},
    onCopyToRoute: () -> Unit = {},
) {
    var editName by remember(stop.id) { mutableStateOf(stop.name) }
    var editNotes by remember(stop.id) { mutableStateOf(stop.notes) }
    var arriveByMinutes by remember(stop.id) { mutableStateOf(stop.arriveByMinutes) }
    var arriveByEpochMs by remember(stop.id) { mutableStateOf(stop.arriveByEpochMs) }
    var serviceMinutes by remember(stop.id) { mutableStateOf(stop.serviceMinutes) }
    var geofenceRadiusMeters by remember(stop.id) { mutableStateOf(stop.geofenceRadiusMeters) }
    var phone by remember(stop.id) { mutableStateOf(stop.phone) }
    var doorCode by remember(stop.id) { mutableStateOf(stop.doorCode) }
    var codAmountText by remember(stop.id) {
        mutableStateOf(if (stop.codAmount > 0.0) stop.codAmount.toString() else "")
    }
    var barcode by remember(stop.id) { mutableStateOf(stop.barcode) }
    var isFixedOrder by remember(stop.id) { mutableStateOf(stop.isFixedOrder) }
    var isBreak by remember(stop.id) { mutableStateOf(stop.isBreak) }
    val context = LocalContext.current

    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.dialog_stop_details),
        confirmLabel = if (placeReadOnly) null else stringResource(R.string.action_save),
        onConfirm = if (placeReadOnly) {
            null
        } else {
            {
                onSave(
                    editName,
                    editNotes,
                    arriveByMinutes,
                    serviceMinutes,
                    geofenceRadiusMeters,
                    arriveByEpochMs,
                    phone,
                    doorCode,
                    isFixedOrder,
                    isBreak,
                    codAmountText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                    barcode,
                )
            }
        },
        dismissLabel = stringResource(R.string.action_close),
    ) {
        if (usageRouteNames.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.library_used_on_list,
                    usageRouteNames.joinToString(", "),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        } else if (stop.libraryStopId != null) {
            Text(
                text = stringResource(R.string.library_linked_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        val lat = stop.latitude
        val lng = stop.longitude
        if (lat != null && lng != null) {
            Text(
                text = PlaceShare.coordinatesText(lat, lng),
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.onSurfaceMuted,
            )
            TextButton(onClick = { PlaceShare.copyCoordinates(context, lat, lng) }) {
                Text(stringResource(R.string.place_copy_coords))
            }
        }
        SoftOutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = stringResource(R.string.dialog_name),
            singleLine = true,
            enabled = !placeReadOnly,
            modifier = Modifier.fillMaxWidth(),
        )
        SoftOutlinedTextField(
            value = editNotes,
            onValueChange = { editNotes = it },
            label = stringResource(R.string.dialog_delivery_notes),
            placeholder = stringResource(R.string.dialog_delivery_notes_hint),
            singleLine = false,
            minLines = 3,
            enabled = !placeReadOnly,
            modifier = Modifier.fillMaxWidth(),
        )
        ArriveByControls(
            arriveByMinutes = arriveByMinutes,
            serviceMinutes = serviceMinutes,
            enabled = !placeReadOnly,
            onArriveByChange = { arriveByMinutes = it },
            onServiceMinutesChange = { serviceMinutes = it },
            resetKey = stop.id,
            arriveByEpochMs = arriveByEpochMs,
            onArriveByEpochChange = { arriveByEpochMs = it },
        )
        SoftOutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = stringResource(R.string.stop_phone),
            singleLine = true,
            enabled = !placeReadOnly,
            modifier = Modifier.fillMaxWidth(),
        )
        SoftOutlinedTextField(
            value = doorCode,
            onValueChange = { doorCode = it },
            label = stringResource(R.string.stop_door_code),
            singleLine = true,
            enabled = !placeReadOnly,
            modifier = Modifier.fillMaxWidth(),
        )
        SoftOutlinedTextField(
            value = codAmountText,
            onValueChange = { codAmountText = it },
            label = stringResource(R.string.cod_amount),
            singleLine = true,
            enabled = !placeReadOnly,
            modifier = Modifier.fillMaxWidth(),
        )
        SoftOutlinedTextField(
            value = barcode,
            onValueChange = { barcode = it },
            label = stringResource(R.string.stop_barcode),
            singleLine = true,
            enabled = !placeReadOnly,
            modifier = Modifier.fillMaxWidth(),
        )
        if (phone.isNotBlank()) {
            TextButton(onClick = onCall) {
                Text(stringResource(R.string.nav_call_stop))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isFixedOrder,
                onCheckedChange = { isFixedOrder = it },
                enabled = !placeReadOnly,
            )
            Text(stringResource(R.string.stop_fixed_order))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isBreak,
                onCheckedChange = { isBreak = it },
                enabled = !placeReadOnly,
            )
            Text(stringResource(R.string.stop_break))
        }
        GeofenceRadiusField(
            radiusMeters = geofenceRadiusMeters,
            onChange = { geofenceRadiusMeters = it },
            enabled = !placeReadOnly,
            resetKey = stop.id,
        )
        StopTaskChecklist(
            stopId = stop.id,
            tasks = tasks,
            templates = templates,
            readOnly = false,
            onAddTask = onAddTask,
            onApplyTemplate = onApplyTemplate,
            onApplyTemplateToRemaining = onApplyTemplateToRemaining,
            onTaskCompletedChange = onTaskCompletedChange,
            onCompletionNoteChange = onCompletionNoteChange,
            onUpdateTask = onUpdateTask,
            onDeleteTask = onDeleteTask,
        )
        if (stop.latitude != null && stop.longitude != null) {
            Text(
                stringResource(
                    R.string.dialog_pinned_coords,
                    stop.latitude!!,
                    stop.longitude!!,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.onSurfaceMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onNavigate) {
                    Text(stringResource(R.string.action_navigate))
                }
                TextButton(onClick = onOpenExternalMaps) {
                    Text(stringResource(R.string.dialog_maps))
                }
            }
        } else {
            Text(
                stringResource(R.string.dialog_no_map_pin),
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.onSurfaceMuted,
            )
        }
        if (!placeReadOnly) {
            TextButton(onClick = onCopyToRoute) {
                Text(stringResource(R.string.stop_copy_to_route))
            }
            TextButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
fun CopyStopTargetDialog(
    routes: List<com.danielcioban.routeplanner.data.local.RouteWithStops>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.stop_copy_pick_title),
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        if (routes.isEmpty()) {
            Text(
                text = stringResource(R.string.stop_copy_no_routes),
                style = MaterialTheme.typography.bodyLarge,
                color = IslandColors.onSurface,
            )
        } else {
            routes.forEach { item ->
                TextButton(
                    onClick = { onPick(item.route.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(item.route.name)
                }
            }
        }
    }
}
