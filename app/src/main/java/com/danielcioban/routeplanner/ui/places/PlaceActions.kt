package com.danielcioban.routeplanner.ui.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.geocoding.NominatimGeocodingClient
import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.components.IslandListItem
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.drive.DriveHereTarget
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.PlaceShare
import kotlinx.coroutines.delay

@Composable
fun PlaceActionDialog(
    target: DriveHereTarget,
    onDismiss: () -> Unit,
    onDriveHere: (name: String, saveToLibrary: Boolean) -> Unit,
    onSaveToLibrary: ((name: String) -> Unit)? = null,
    onAddToRoute: ((name: String) -> Unit)? = null,
    onNewRoute: ((name: String) -> Unit)? = null,
) {
    val fallbackName = stringResource(R.string.drive_here_unnamed)
    val context = LocalContext.current
    val geocoder = remember { NominatimGeocodingClient() }
    val languageTag = LocalConfiguration.current.locales[0]?.toLanguageTag()
    var name by remember(target.latitude, target.longitude, target.name) {
        mutableStateOf(target.name.ifBlank { fallbackName })
    }
    var resolvedAddress by remember(target.latitude, target.longitude) {
        mutableStateOf(target.addressHint)
    }
    LaunchedEffect(target.latitude, target.longitude, target.name, target.addressHint) {
        if (target.name.isNotBlank() || target.addressHint.isNotBlank()) return@LaunchedEffect
        delay(650)
        geocoder.reverse(target.latitude, target.longitude, languageTag).getOrNull()?.let { place ->
            resolvedAddress = place.displayName
            if (name == fallbackName || name.isBlank()) {
                name = place.shortName
            }
        }
    }
    val trimmed = { name.trim().ifBlank { fallbackName } }
    val fromLibrary = target.libraryStopId != null

    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.place_action_title),
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(
            text = stringResource(R.string.place_action_body),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        if (resolvedAddress.isNotBlank()) {
            Text(
                text = resolvedAddress,
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.onSurfaceMuted,
            )
        }
        TextButton(
            onClick = { PlaceShare.copyCoordinates(context, target.latitude, target.longitude) },
        ) {
            Text(stringResource(R.string.place_copy_coords))
        }
        SoftOutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.dialog_stop_name),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onDriveHere(trimmed(), !fromLibrary) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.drive_here_confirm))
        }
        if (!fromLibrary && onSaveToLibrary != null) {
            OutlinedButton(
                onClick = { onSaveToLibrary(trimmed()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.place_save_library))
            }
        }
        if (onAddToRoute != null) {
            OutlinedButton(
                onClick = { onAddToRoute(trimmed()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.place_add_to_route))
            }
        }
        if (onNewRoute != null) {
            OutlinedButton(
                onClick = { onNewRoute(trimmed()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.place_new_route))
            }
        }
    }
}

@Composable
fun RoutePickerDialog(
    routes: List<RouteWithStops>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
    emptyHint: String? = null,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.route_picker_title),
        confirmLabel = stringResource(R.string.action_close),
        onConfirm = onDismiss,
        dismissLabel = null,
    ) {
        Text(
            text = stringResource(R.string.route_picker_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        if (routes.isEmpty()) {
            Text(
                text = emptyHint ?: stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.bodyLarge,
                color = IslandColors.onSurface,
            )
        } else {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                routes.forEach { item ->
                    IslandListItem(onClick = { onPick(item.route.id) }) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.route.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = IslandColors.onSurface,
                            )
                            Text(
                                text = stringResource(
                                    R.string.route_picker_stops,
                                    item.stops.size,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = IslandColors.onSurfaceMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReplaceDriveDialog(
    currentLabel: String,
    onDismiss: () -> Unit,
    onKeepCurrent: () -> Unit,
    onReplace: () -> Unit,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.replace_drive_title),
        confirmLabel = stringResource(R.string.replace_drive_replace),
        onConfirm = onReplace,
        dismissLabel = stringResource(R.string.replace_drive_keep),
        onDismissButton = onKeepCurrent,
    ) {
        Text(
            text = stringResource(R.string.replace_drive_body, currentLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
    }
}

@Composable
fun ConfirmDeleteRouteDialog(
    routeName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.confirm_delete_route_title),
        confirmLabel = stringResource(R.string.action_delete),
        onConfirm = onConfirm,
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(
            text = stringResource(R.string.confirm_delete_route_body, routeName),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
    }
}

@Composable
fun JumpAheadDialog(
    stopName: String,
    onDismiss: () -> Unit,
    onGoOnly: () -> Unit,
    onMarkPreviousDone: () -> Unit,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.jump_ahead_title),
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(
            text = stringResource(R.string.jump_ahead_body, stopName),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        Button(
            onClick = onGoOnly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.jump_ahead_go_only))
        }
        OutlinedButton(
            onClick = onMarkPreviousDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.jump_ahead_mark_done))
        }
    }
}

@Composable
fun ExportRouteDialog(
    onDismiss: () -> Unit,
    onShareText: () -> Unit,
    onCsv: () -> Unit,
    onGpx: () -> Unit,
    onKml: () -> Unit,
    onGeoJson: () -> Unit,
    onIcs: () -> Unit = {},
    onPdf: () -> Unit = {},
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.export_places_title),
        dismissLabel = stringResource(R.string.action_close),
    ) {
        OutlinedButton(onClick = onShareText, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cd_share_route))
        }
        OutlinedButton(onClick = onCsv, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.export_format_csv))
        }
        OutlinedButton(onClick = onGpx, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.export_format_gpx))
        }
        OutlinedButton(onClick = onKml, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.export_format_kml))
        }
        OutlinedButton(onClick = onGeoJson, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.export_format_geojson))
        }
        OutlinedButton(onClick = onIcs, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.export_format_ics))
        }
        OutlinedButton(onClick = onPdf, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.export_format_pdf))
        }
    }
}

@Composable
fun CsvImportTargetDialog(
    routes: List<RouteWithStops>,
    onDismiss: () -> Unit,
    onNewRoute: () -> Unit,
    onExistingRoute: (Long) -> Unit,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.csv_import_target_title),
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(
            text = stringResource(R.string.csv_import_target_body),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        Button(
            onClick = onNewRoute,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.csv_import_new_route))
        }
        if (routes.isEmpty()) {
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.bodyLarge,
                color = IslandColors.onSurface,
            )
        } else {
            Column(
                modifier = Modifier
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                routes.forEach { item ->
                    IslandListItem(onClick = { onExistingRoute(item.route.id) }) {
                        Text(
                            text = item.route.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = IslandColors.onSurface,
                        )
                    }
                }
            }
        }
    }
}
