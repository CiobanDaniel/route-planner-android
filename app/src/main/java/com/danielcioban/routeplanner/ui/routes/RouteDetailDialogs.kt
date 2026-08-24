package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.local.StopTaskEntity
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.theme.IslandColors

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
    LaunchedEffect(pin.latitude, pin.longitude, pin.suggestedName) {
        if (name.isBlank() && pin.suggestedName.isNotBlank()) {
            onNameChange(pin.suggestedName)
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
            text = "${"%.5f".format(pin.latitude)}, ${"%.5f".format(pin.longitude)}",
            style = MaterialTheme.typography.bodySmall,
            color = IslandColors.onSurfaceMuted,
        )
        if (pin.addressHint.isNotBlank()) {
            Text(
                text = pin.addressHint,
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
    onDismiss: () -> Unit,
    onSave: (name: String, notes: String) -> Unit,
    onDelete: () -> Unit,
    onAddTask: (title: String, required: Boolean) -> Unit,
    onTaskCompletedChange: (taskId: Long, completed: Boolean, note: String) -> Unit,
    onCompletionNoteChange: (taskId: Long, note: String) -> Unit,
    onUpdateTask: (taskId: Long, title: String, required: Boolean) -> Unit,
    onDeleteTask: (taskId: Long) -> Unit,
    onNavigate: () -> Unit,
    onOpenExternalMaps: () -> Unit,
) {
    var editName by remember(stop.id) { mutableStateOf(stop.name) }
    var editNotes by remember(stop.id) { mutableStateOf(stop.notes) }

    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.dialog_stop_details),
        confirmLabel = if (placeReadOnly) null else stringResource(R.string.action_save),
        onConfirm = if (placeReadOnly) null else ({ onSave(editName, editNotes) }),
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
        StopTaskChecklist(
            stopId = stop.id,
            tasks = tasks,
            readOnly = false,
            onAddTask = onAddTask,
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
