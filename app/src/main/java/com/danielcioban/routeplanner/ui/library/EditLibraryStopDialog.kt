package com.danielcioban.routeplanner.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun EditLibraryStopDialog(
    stop: StopLibraryEntity,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        addressHint: String,
        notes: String,
        latitude: Double,
        longitude: Double,
    ) -> Unit,
) {
    var name by remember(stop.id) { mutableStateOf(stop.name) }
    var addressHint by remember(stop.id) { mutableStateOf(stop.addressHint) }
    var notes by remember(stop.id) { mutableStateOf(stop.notes) }
    var latitudeText by remember(stop.id) { mutableStateOf(formatCoord(stop.latitude)) }
    var longitudeText by remember(stop.id) { mutableStateOf(formatCoord(stop.longitude)) }
    var errorMessage by remember(stop.id) { mutableStateOf<String?>(null) }
    val nameRequired = stringResource(R.string.library_edit_name_required)
    val coordsInvalid = stringResource(R.string.library_edit_coords_invalid)

    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.library_edit_title),
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = confirm@{
            val trimmed = name.trim()
            if (trimmed.isEmpty()) {
                errorMessage = nameRequired
                return@confirm
            }
            val lat = parseCoord(latitudeText)
            val lng = parseCoord(longitudeText)
            if (lat == null || lng == null ||
                lat !in -90.0..90.0 ||
                lng !in -180.0..180.0
            ) {
                errorMessage = coordsInvalid
                return@confirm
            }
            onSave(trimmed, addressHint.trim(), notes.trim(), lat, lng)
            onDismiss()
        },
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        SoftOutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                errorMessage = null
            },
            label = stringResource(R.string.dialog_name),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SoftOutlinedTextField(
            value = addressHint,
            onValueChange = { addressHint = it },
            label = stringResource(R.string.library_edit_address),
            singleLine = false,
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        SoftOutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = stringResource(R.string.dialog_delivery_notes),
            placeholder = stringResource(R.string.dialog_delivery_notes_hint),
            singleLine = false,
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SoftOutlinedTextField(
                value = latitudeText,
                onValueChange = {
                    latitudeText = it
                    errorMessage = null
                },
                label = stringResource(R.string.library_edit_latitude),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            SoftOutlinedTextField(
                value = longitudeText,
                onValueChange = {
                    longitudeText = it
                    errorMessage = null
                },
                label = stringResource(R.string.library_edit_longitude),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            Text(
                text = stringResource(R.string.library_edit_hint),
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.onSurfaceMuted,
            )
        }
    }
}

@Composable
fun DeleteLibraryStopDialog(
    stopName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.library_delete_title),
        confirmLabel = stringResource(R.string.action_delete),
        onConfirm = {
            onConfirm()
            onDismiss()
        },
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(
            text = stringResource(R.string.library_delete_body, stopName),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
    }
}

private fun formatCoord(value: Double): String = "%.5f".format(value)

private fun parseCoord(text: String): Double? {
    val trimmed = text.trim().replace(',', '.')
    if (trimmed.isEmpty()) return null
    return trimmed.toDoubleOrNull()
}
