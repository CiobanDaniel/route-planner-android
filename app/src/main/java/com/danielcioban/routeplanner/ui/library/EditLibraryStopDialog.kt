package com.danielcioban.routeplanner.ui.library

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.local.LibraryDefaultTaskEntity
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.routes.GeofenceRadiusField
import com.danielcioban.routeplanner.ui.routes.NfcWriteDialog
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.NfcPlaceTag
import com.danielcioban.routeplanner.util.OpenLocationCode
import com.danielcioban.routeplanner.util.PlaceShare

@Composable
fun EditLibraryStopDialog(
    stop: StopLibraryEntity,
    defaultTasks: List<LibraryDefaultTaskEntity> = emptyList(),
    onDismiss: () -> Unit,
    onDriveHere: (() -> Unit)? = null,
    onToggleFavorite: ((Boolean) -> Unit)? = null,
    onAddDefaultTask: (title: String, required: Boolean) -> Unit = { _, _ -> },
    onDeleteDefaultTask: (Long) -> Unit = {},
    onSave: (
        name: String,
        addressHint: String,
        notes: String,
        latitude: Double,
        longitude: Double,
        tags: String,
        what3words: String,
        plusCode: String,
        geofenceRadiusMeters: Int?,
    ) -> Unit,
) {
    val context = LocalContext.current
    var name by remember(stop.id) { mutableStateOf(stop.name) }
    var addressHint by remember(stop.id) { mutableStateOf(stop.addressHint) }
    var notes by remember(stop.id) { mutableStateOf(stop.notes) }
    var tags by remember(stop.id) { mutableStateOf(stop.tags) }
    var what3words by remember(stop.id) { mutableStateOf(stop.what3words) }
    var plusCode by remember(stop.id) {
        mutableStateOf(stop.plusCode.ifBlank { OpenLocationCode.encode(stop.latitude, stop.longitude) })
    }
    var latitudeText by remember(stop.id) { mutableStateOf(formatCoord(stop.latitude)) }
    var longitudeText by remember(stop.id) { mutableStateOf(formatCoord(stop.longitude)) }
    var geofenceRadius by remember(stop.id) { mutableStateOf(stop.defaultGeofenceRadiusMeters) }
    var errorMessage by remember(stop.id) { mutableStateOf<String?>(null) }
    var newTaskTitle by remember(stop.id) { mutableStateOf("") }
    var newTaskRequired by remember(stop.id) { mutableStateOf(false) }
    val nameRequired = stringResource(R.string.library_edit_name_required)
    val coordsInvalid = stringResource(R.string.library_edit_coords_invalid)
    var writingNfc by remember { mutableStateOf(false) }
    var nfcMessageRes by remember { mutableStateOf<Int?>(null) }
    val activity = context.findActivity()

    DisposableEffect(writingNfc, stop.remoteId) {
        val host = activity
        if (writingNfc && host != null) {
            NfcPlaceTag.beginWrite(host, NfcPlaceTag.uriForLibrary(stop.remoteId)) { ok ->
                host.runOnUiThread {
                    writingNfc = false
                    nfcMessageRes = if (ok) R.string.nfc_write_done else R.string.nfc_write_failed
                }
            }
        }
        onDispose {
            host?.let(NfcPlaceTag::cancelWrite)
        }
    }

    IslandDialog(
        onDismissRequest = {
            activity?.let(NfcPlaceTag::cancelWrite)
            onDismiss()
        },
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
            val plus = plusCode.trim().ifBlank { OpenLocationCode.encode(lat, lng) }
            onSave(
                trimmed,
                addressHint.trim(),
                notes.trim(),
                lat,
                lng,
                tags.trim(),
                what3words.trim(),
                plus,
                geofenceRadius,
            )
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
        SoftOutlinedTextField(
            value = tags,
            onValueChange = { tags = it },
            label = stringResource(R.string.library_tags),
            placeholder = stringResource(R.string.library_tags_hint),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SoftOutlinedTextField(
            value = plusCode,
            onValueChange = { plusCode = it },
            label = stringResource(R.string.library_plus_code),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SoftOutlinedTextField(
            value = what3words,
            onValueChange = { what3words = it },
            label = stringResource(R.string.library_what3words),
            placeholder = stringResource(R.string.library_what3words_hint),
            singleLine = true,
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
        TextButton(
            onClick = {
                val lat = parseCoord(latitudeText)
                val lng = parseCoord(longitudeText)
                if (lat != null && lng != null) {
                    PlaceShare.copyCoordinates(context, lat, lng)
                }
            },
        ) {
            Text(stringResource(R.string.place_copy_coords))
        }
        if (onToggleFavorite != null) {
            FilterChip(
                selected = stop.isFavorite,
                onClick = { onToggleFavorite(!stop.isFavorite) },
                label = { Text(stringResource(R.string.library_favorite)) },
            )
        }
        GeofenceRadiusField(
            radiusMeters = geofenceRadius,
            onChange = { geofenceRadius = it },
            resetKey = stop.id,
        )
        OutlinedButton(
            onClick = {
                nfcMessageRes = null
                writingNfc = true
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.nfc_write_title))
        }
        nfcMessageRes?.let { res ->
            Text(
                text = stringResource(res),
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.library_default_tasks),
            style = MaterialTheme.typography.labelLarge,
            color = IslandColors.onSurface,
        )
        defaultTasks.forEach { task ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.bodyMedium)
                    if (task.isRequired) {
                        Text(
                            text = stringResource(R.string.tasks_required),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                TextButton(onClick = { onDeleteDefaultTask(task.id) }) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
        SoftOutlinedTextField(
            value = newTaskTitle,
            onValueChange = { newTaskTitle = it },
            label = stringResource(R.string.task_templates_title_label),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = newTaskRequired,
                onClick = { newTaskRequired = !newTaskRequired },
                label = { Text(stringResource(R.string.tasks_required)) },
            )
            TextButton(
                onClick = {
                    onAddDefaultTask(newTaskTitle, newTaskRequired)
                    newTaskTitle = ""
                    newTaskRequired = false
                },
                enabled = newTaskTitle.isNotBlank(),
            ) {
                Text(stringResource(R.string.task_templates_add))
            }
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
        if (onDriveHere != null) {
            Button(
                onClick = onDriveHere,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.drive_here_confirm))
            }
        }
    }
    if (writingNfc) {
        NfcWriteDialog(
            nfcReady = activity != null && NfcPlaceTag.available(activity),
            onDismiss = {
                activity?.let(NfcPlaceTag::cancelWrite)
                writingNfc = false
            },
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
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
