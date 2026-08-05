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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.local.StopEntity
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
    alsoSaveToLibrary: Boolean,
    onAlsoSaveToLibraryChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(
                checked = alsoSaveToLibrary,
                onCheckedChange = onAlsoSaveToLibraryChange,
            )
            Text(
                text = stringResource(R.string.library_also_save),
                style = MaterialTheme.typography.bodyMedium,
                color = IslandColors.onSurface,
            )
        }
    }
}

@Composable
fun SelectedStopDialogs(
    stop: StopEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, notes: String) -> Unit,
    onDelete: () -> Unit,
    onSaveToLibrary: () -> Unit,
    onRefreshFromLibrary: (() -> Unit)? = null,
    onNavigate: () -> Unit,
    onOpenExternalMaps: () -> Unit,
) {
    var editName by remember(stop.id) { mutableStateOf(stop.name) }
    var editNotes by remember(stop.id) { mutableStateOf(stop.notes) }
    var confirmingDelete by remember(stop.id) { mutableStateOf(false) }
    val linkedToLibrary = stop.libraryStopId != null

    if (confirmingDelete) {
        IslandDialog(
            onDismissRequest = { confirmingDelete = false },
            title = stringResource(R.string.dialog_delete_stop_title),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                onDelete()
                confirmingDelete = false
            },
            dismissLabel = stringResource(R.string.action_cancel),
            onDismissButton = { confirmingDelete = false },
        ) {
            Text(
                text = stringResource(
                    if (linkedToLibrary) {
                        R.string.dialog_delete_stop_body_linked
                    } else {
                        R.string.dialog_delete_stop_body
                    },
                    stop.name,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = IslandColors.onSurfaceMuted,
            )
        }
    } else {
        IslandDialog(
            onDismissRequest = onDismiss,
            title = stringResource(R.string.dialog_stop_details),
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = { onSave(editName, editNotes) },
            dismissLabel = stringResource(R.string.action_close),
        ) {
            if (linkedToLibrary) {
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
                modifier = Modifier.fillMaxWidth(),
            )
            SoftOutlinedTextField(
                value = editNotes,
                onValueChange = { editNotes = it },
                label = stringResource(R.string.dialog_delivery_notes),
                placeholder = stringResource(R.string.dialog_delivery_notes_hint),
                singleLine = false,
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
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
                    TextButton(onClick = onSaveToLibrary) {
                        Text(
                            stringResource(
                                if (linkedToLibrary) {
                                    R.string.library_update_entry
                                } else {
                                    R.string.library_save_to
                                },
                            ),
                        )
                    }
                    if (onRefreshFromLibrary != null && linkedToLibrary) {
                        TextButton(onClick = onRefreshFromLibrary) {
                            Text(stringResource(R.string.library_refresh_from))
                        }
                    }
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
                if (linkedToLibrary) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onSaveToLibrary) {
                            Text(stringResource(R.string.library_update_entry))
                        }
                        if (onRefreshFromLibrary != null) {
                            TextButton(onClick = onRefreshFromLibrary) {
                                Text(stringResource(R.string.library_refresh_from))
                            }
                        }
                    }
                }
            }
            TextButton(
                onClick = { confirmingDelete = true },
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
