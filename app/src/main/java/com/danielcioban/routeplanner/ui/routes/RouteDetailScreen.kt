package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.settings.AppSettings
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.location.rememberDeviceBearing
import com.danielcioban.routeplanner.ui.location.rememberUserLocation
import com.danielcioban.routeplanner.ui.map.MapLayersButton
import com.danielcioban.routeplanner.ui.map.MapViewMode
import com.danielcioban.routeplanner.ui.map.RouteMapBackdrop
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.ExternalNavigation
import com.danielcioban.routeplanner.util.GeoUtils

@Composable
fun RouteDetailScreen(
    viewModel: RouteDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    settings: AppSettings = AppSettings(),
) {
    val routeWithStops by viewModel.route.collectAsStateWithLifecycle()
    val deliveryActive by viewModel.deliveryActive.collectAsStateWithLifecycle()
    val pendingPin by viewModel.pendingPin.collectAsStateWithLifecycle()
    val selectedStopId by viewModel.selectedStopId.collectAsStateWithLifecycle()
    val navigation by viewModel.navigation.collectAsStateWithLifecycle()
    var recenterToken by remember { mutableIntStateOf(0) }
    var mapViewMode by remember { mutableStateOf(MapViewMode.MAP) }
    var driveFollow by remember { mutableStateOf(false) }
    var followPaused by remember { mutableStateOf(false) }
    val inAppNavigating = navigation.phase == NavigationPhase.Navigating ||
        navigation.phase == NavigationPhase.LoadingRoute ||
        navigation.phase == NavigationPhase.Arrived
    val navigating = driveFollow || deliveryActive || inAppNavigating
    val view = LocalView.current
    DisposableEffect(settings.keepScreenOnDuringNav, navigating) {
        val keep = settings.keepScreenOnDuringNav && navigating
        view.keepScreenOn = keep
        onDispose { view.keepScreenOn = false }
    }
    val userLocation = rememberUserLocation(
        autoRequest = true,
        highFrequency = navigating,
    )
    val compassBearing = rememberDeviceBearing(enabled = navigating)
    val userFix = remember(userLocation.coordinate, compassBearing) {
        val base = userLocation.coordinate ?: return@remember null
        // Rotation-vector azimuth is mirrored vs map bearing for heading-up.
        val compassForMap = compassBearing?.let { ((360f - it) % 360f) }
        base.copy(bearingDegrees = base.bearingDegrees ?: compassForMap)
    }

    var newStopName by remember { mutableStateOf("") }
    var newStopNotes by remember { mutableStateOf("") }
    var locationMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(userFix, navigation.phase, deliveryActive) {
        val fix = userFix ?: return@LaunchedEffect
        if (deliveryActive || inAppNavigating) {
            viewModel.onUserLocationUpdated(fix)
        }
    }

    val data = routeWithStops
    val stops = data?.orderedStops.orEmpty()
    val selectedStop = stops.firstOrNull { it.id == selectedStopId }
    val progress = viewModel.deliveryProgress()
    val hasPinnedStops = stops.any { it.latitude != null && it.longitude != null }
    val distanceFromYou = remember(userFix, progress.nextStop) {
        val user = userFix
        val next = progress.nextStop
        if (user != null && next?.latitude != null && next.longitude != null) {
            val meters = GeoUtils.distanceMeters(
                user.latitude,
                user.longitude,
                next.latitude!!,
                next.longitude!!,
            )
            val bearing = GeoUtils.bearingDegrees(
                user.latitude,
                user.longitude,
                next.latitude!!,
                next.longitude!!,
            )
            "${GeoUtils.formatDistance(meters, settings.distanceUnit)} away · ${GeoUtils.formatBearing(bearing)}"
        } else {
            null
        }
    }

    fun openExternalMaps(stop: StopEntity) {
        val lat = stop.latitude
        val lng = stop.longitude
        if (lat == null || lng == null) {
            locationMessage = "This stop has no map pin yet"
            return
        }
        val opened = ExternalNavigation.openDrivingDirections(
            context = context,
            latitude = lat,
            longitude = lng,
            label = stop.name,
        )
        if (!opened) {
            locationMessage = "Couldn’t open Maps or Waze"
        }
    }

    fun beginInAppNavigation() {
        val fix = userFix
        if (!userLocation.hasPermission) {
            userLocation.requestPermission()
            locationMessage = "Allow location access to navigate"
            return
        }
        if (fix == null) {
            userLocation.refresh()
            locationMessage = "Waiting for GPS fix…"
            return
        }
        locationMessage = null
        driveFollow = true
        followPaused = false
        mapViewMode = MapViewMode.DRIVING
        recenterToken++
        viewModel.startNavigationToNext(fix)
    }

    fun recenterOnUser() {
        followPaused = false
        driveFollow = true
        recenterToken++
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RouteMapBackdrop(
            stops = stops,
            fitStops = hasPinnedStops && !navigating,
            userLocation = userFix,
            recenterToken = recenterToken,
            mapViewMode = mapViewMode,
            driveFollow = navigating,
            navRouteLineJson = navigation.navLineJson,
            navRouteFitToken = navigation.navFitToken,
            showStraightStopLinks = !inAppNavigating,
            onMapLongClick = { latLng ->
                viewModel.beginAddStopAt(latLng.latitude, latLng.longitude)
                newStopName = ""
                newStopNotes = ""
            },
            onStopClick = { id -> viewModel.selectStop(id) },
            onFollowPaused = { paused -> followPaused = paused },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                FloatingIsland(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = 12.dp,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FloatingCircleButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = IslandColors.onSurface,
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = data?.route?.name ?: "Route",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = IslandColors.onSurface,
                            )
                            Text(
                                text = when {
                                    followPaused && navigating -> "Map free · tap location to resume"
                                    deliveryActive -> "Delivery active · ${progress.remaining} left"
                                    else -> "Long-press map to add a stop"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = IslandColors.onSurfaceMuted,
                            )
                        }
                        FloatingCircleButton(
                            onClick = { data?.route?.id?.let(onEdit) },
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = IslandColors.onSurface,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    MapLayersButton(
                        selected = mapViewMode,
                        onSelected = { mode ->
                            mapViewMode = mode
                            if (mode == MapViewMode.DRIVING) {
                                driveFollow = true
                                recenterToken++
                            } else {
                                driveFollow = false
                            }
                        },
                        driveFollow = navigating,
                        onDriveFollowChange = { enabled ->
                            driveFollow = enabled
                            if (enabled) {
                                mapViewMode = MapViewMode.DRIVING
                                recenterToken++
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FloatingCircleButton(
                        onClick = {
                            if (navigating) {
                                recenterOnUser()
                                return@FloatingCircleButton
                            }
                            if (!userLocation.hasPermission) {
                                userLocation.requestPermission()
                                locationMessage = "Allow location access to use GPS"
                            } else {
                                val loc = userFix
                                if (loc != null) {
                                    viewModel.addStopAtCurrentLocation(
                                        "GPS stop",
                                        loc.latitude,
                                        loc.longitude,
                                    )
                                    locationMessage = null
                                    recenterToken++
                                } else {
                                    userLocation.refresh()
                                    locationMessage = "Waiting for GPS fix…"
                                }
                            }
                        },
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = if (navigating) "Recenter" else "Add / go to my location",
                            tint = if (followPaused) MaterialTheme.colorScheme.primary else IslandColors.onSurface,
                        )
                    }
                }
            }

            if (locationMessage != null) {
                Spacer(modifier = Modifier.heightIn(min = 8.dp))
                FloatingIsland(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = 12.dp,
                ) {
                    Text(locationMessage.orEmpty(), style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (deliveryActive) {
                DeliveryHud(
                    progress = progress,
                    totalStops = stops.size,
                    navigation = navigation,
                    distanceFromYou = distanceFromYou,
                    distanceUnit = settings.distanceUnit,
                    onStartInAppNav = { beginInAppNavigation() },
                    onMarkDone = { viewModel.completeNextStop(userFix) },
                    onEndDelivery = {
                        viewModel.setDeliveryActive(false)
                        driveFollow = false
                        mapViewMode = MapViewMode.MAP
                    },
                    onRetryRoute = { beginInAppNavigation() },
                    onOpenExternalMaps = { openExternalMaps(it) },
                )
            } else {
                FloatingIsland(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = 12.dp,
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "${data?.completedCount ?: 0}/${stops.size} completed",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = { beginInAppNavigation() },
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start")
                            }
                        }
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            itemsIndexed(stops, key = { _, stop -> stop.id }) { index, stop ->
                                val previous = stops.getOrNull(index - 1)
                                StopRow(
                                    index = index + 1,
                                    stop = stop,
                                    previous = previous,
                                    selected = stop.id == selectedStopId,
                                    onClick = { viewModel.selectStop(stop.id) },
                                    onCompletedChange = { viewModel.setStopCompleted(stop.id, it) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingPin != null) {
        IslandDialog(
            onDismissRequest = {
                viewModel.cancelPendingPin()
                newStopName = ""
                newStopNotes = ""
            },
            title = stringResource(R.string.dialog_add_stop_title),
            confirmLabel = stringResource(R.string.action_add),
            onConfirm = {
                viewModel.confirmPendingStop(newStopName, newStopNotes)
                newStopName = ""
                newStopNotes = ""
            },
            dismissLabel = stringResource(R.string.action_cancel),
        ) {
            Text(
                text = "${"%.5f".format(pendingPin!!.latitude)}, ${"%.5f".format(pendingPin!!.longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.onSurfaceMuted,
            )
            SoftOutlinedTextField(
                value = newStopName,
                onValueChange = { newStopName = it },
                label = stringResource(R.string.dialog_stop_name),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SoftOutlinedTextField(
                value = newStopNotes,
                onValueChange = { newStopNotes = it },
                label = stringResource(R.string.dialog_delivery_notes),
                placeholder = stringResource(R.string.dialog_delivery_notes_hint),
                singleLine = false,
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (selectedStop != null && !deliveryActive) {
        var editName by remember(selectedStop.id) { mutableStateOf(selectedStop.name) }
        var editNotes by remember(selectedStop.id) { mutableStateOf(selectedStop.notes) }
        IslandDialog(
            onDismissRequest = { viewModel.selectStop(null) },
            title = stringResource(R.string.dialog_stop_details),
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = {
                viewModel.updateStopDetails(
                    stopId = selectedStop.id,
                    name = editName,
                    notes = editNotes,
                    addressHint = selectedStop.addressHint,
                )
                viewModel.selectStop(null)
            },
            dismissLabel = stringResource(R.string.action_close),
        ) {
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
            if (selectedStop.latitude != null && selectedStop.longitude != null) {
                Text(
                    "Pinned ${"%.5f".format(selectedStop.latitude)}, ${"%.5f".format(selectedStop.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = IslandColors.onSurfaceMuted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            val stop = selectedStop
                            viewModel.selectStop(null)
                            val fix = userFix
                            if (!userLocation.hasPermission) {
                                userLocation.requestPermission()
                                locationMessage = "Allow location access to navigate"
                            } else if (fix == null) {
                                userLocation.refresh()
                                locationMessage = "Waiting for GPS fix…"
                            } else {
                                locationMessage = null
                                driveFollow = true
                                followPaused = false
                                mapViewMode = MapViewMode.DRIVING
                                recenterToken++
                                viewModel.startNavigationToStop(stop, fix)
                            }
                        },
                    ) {
                        Text(stringResource(R.string.action_navigate))
                    }
                    androidx.compose.material3.TextButton(
                        onClick = {
                            openExternalMaps(selectedStop)
                            viewModel.selectStop(null)
                        },
                    ) {
                        Text(stringResource(R.string.dialog_maps))
                    }
                }
            } else {
                Text(
                    "No map pin yet — long-press the map or edit the route.",
                    style = MaterialTheme.typography.bodySmall,
                    color = IslandColors.onSurfaceMuted,
                )
            }
        }
    }
}

@Composable
private fun StopRow(
    index: Int,
    stop: StopEntity,
    previous: StopEntity?,
    selected: Boolean,
    onClick: () -> Unit,
    onCompletedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(checked = stop.isCompleted, onCheckedChange = onCompletedChange)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "$index. ${stop.name}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
            if (stop.notes.isNotBlank()) {
                Text(
                    text = stop.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = IslandColors.onSurfaceMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (stop.latitude != null && stop.longitude != null &&
                previous?.latitude != null && previous.longitude != null
            ) {
                val meters = GeoUtils.distanceMeters(
                    previous.latitude!!,
                    previous.longitude!!,
                    stop.latitude!!,
                    stop.longitude!!,
                )
                Text(
                    text = GeoUtils.formatDistance(meters) + " from previous",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (stop.latitude == null) {
                Text(
                    text = "No coordinates",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
