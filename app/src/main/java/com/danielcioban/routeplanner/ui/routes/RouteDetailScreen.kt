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
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.danielcioban.routeplanner.ui.components.AddressSearchDialog
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.components.MapAttributionChip
import com.danielcioban.routeplanner.ui.library.StopLibraryPickerDialog
import com.danielcioban.routeplanner.ui.location.rememberDeviceBearing
import com.danielcioban.routeplanner.ui.location.rememberMergedUserFix
import com.danielcioban.routeplanner.ui.location.rememberUserLocation
import com.danielcioban.routeplanner.ui.map.LatLng
import com.danielcioban.routeplanner.ui.map.MapLayersButton
import com.danielcioban.routeplanner.ui.map.MapLayersMenuHost
import com.danielcioban.routeplanner.ui.map.RouteMapBackdrop
import com.danielcioban.routeplanner.ui.map.rememberMapChromeState
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.ExternalNavigation
import com.danielcioban.routeplanner.util.GeoUtils
import com.danielcioban.routeplanner.util.ShareRoute

@Composable
fun RouteDetailScreen(
    viewModel: RouteDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenStopLibrary: () -> Unit = {},
    settings: AppSettings = AppSettings(),
) {
    val routeWithStops by viewModel.route.collectAsStateWithLifecycle()
    val deliveryActive by viewModel.deliveryActive.collectAsStateWithLifecycle()
    val pendingPin by viewModel.pendingPin.collectAsStateWithLifecycle()
    val selectedStopId by viewModel.selectedStopId.collectAsStateWithLifecycle()
    val navigation by viewModel.navigation.collectAsStateWithLifecycle()
    val libraryStops by viewModel.stopLibrary.collectAsStateWithLifecycle()
    val noticeMessageRes by viewModel.noticeMessageRes.collectAsStateWithLifecycle()
    val chrome = rememberMapChromeState()
    var focusToken by remember { mutableIntStateOf(0) }
    var focusTarget by remember { mutableStateOf<LatLng?>(null) }
    var followPaused by remember { mutableStateOf(false) }
    var showAddressSearch by remember { mutableStateOf(false) }
    var showEndDeliveryDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showStopQueue by remember { mutableStateOf(false) }
    var showLibraryPicker by remember { mutableStateOf(false) }
    var saveNewStopToLibrary by remember { mutableStateOf(false) }
    val shareChooserTitle = stringResource(R.string.share_route_chooser)
    val inAppNavigating = navigation.phase == NavigationPhase.Navigating ||
        navigation.phase == NavigationPhase.LoadingRoute ||
        navigation.phase == NavigationPhase.Arrived
    val navigating = chrome.driveFollow || deliveryActive || inAppNavigating
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
    val userFix = rememberMergedUserFix(
        coordinate = userLocation.coordinate,
        compassBearing = compassBearing,
        active = navigating,
    )

    var newStopName by remember { mutableStateOf("") }
    var newStopNotes by remember { mutableStateOf("") }
    var locationMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(noticeMessageRes) {
        val res = noticeMessageRes ?: return@LaunchedEffect
        locationMessage = context.getString(res)
        viewModel.consumeNotice()
    }

    LaunchedEffect(userLocation.message) {
        val gpsMessage = userLocation.message
        if (gpsMessage != null) {
            locationMessage = gpsMessage
        }
    }

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
    var didCenterEmptyRoute by remember(routeWithStops?.route?.id) { mutableStateOf(false) }
    LaunchedEffect(userFix, hasPinnedStops, navigating) {
        if (!hasPinnedStops && userFix != null && !navigating && !didCenterEmptyRoute) {
            didCenterEmptyRoute = true
            chrome.bumpRecenter()
        }
    }
    val distanceFromYou = remember(userFix, progress.nextStop, settings.distanceUnit) {
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
            context.getString(
                R.string.distance_away,
                GeoUtils.formatDistance(meters, settings.distanceUnit),
                GeoUtils.formatBearing(bearing),
            )
        } else {
            null
        }
    }

    fun openExternalMaps(stop: StopEntity) {
        val lat = stop.latitude
        val lng = stop.longitude
        if (lat == null || lng == null) {
            locationMessage = context.getString(R.string.msg_no_map_pin)
            return
        }
        val opened = ExternalNavigation.openDrivingDirections(
            context = context,
            latitude = lat,
            longitude = lng,
            label = stop.name,
        )
        if (!opened) {
            locationMessage = context.getString(R.string.msg_maps_unavailable)
        }
    }

    fun beginInAppNavigation() {
        val fix = userFix
        if (!userLocation.hasPermission) {
            userLocation.requestPermission()
            locationMessage = context.getString(R.string.msg_allow_location_nav)
            return
        }
        if (fix == null) {
            userLocation.refresh()
            locationMessage = context.getString(R.string.msg_waiting_gps)
            return
        }
        locationMessage = null
        followPaused = false
        chrome.enterDrivingFollow()
        viewModel.startNavigationToNext(fix)
    }

    fun recenterOnUser() {
        followPaused = false
        chrome.driveFollow = true
        chrome.bumpRecenter()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RouteMapBackdrop(
            stops = stops,
            fitStops = hasPinnedStops && !navigating,
            userLocation = userFix,
            recenterToken = chrome.recenterToken,
            mapViewMode = chrome.mapViewMode,
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
            focusTarget = focusTarget,
            focusToken = focusToken,
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
                                contentDescription = stringResource(R.string.cd_back),
                                tint = IslandColors.onSurface,
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = data?.route?.name ?: stringResource(R.string.route_fallback_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = IslandColors.onSurface,
                            )
                            Text(
                                text = when {
                                    followPaused && navigating -> stringResource(R.string.detail_map_free)
                                    deliveryActive -> stringResource(
                                        R.string.detail_delivery_active,
                                        progress.remaining,
                                    )
                                    stops.isEmpty() -> stringResource(R.string.detail_empty_subtitle)
                                    else -> stringResource(R.string.detail_long_press_hint)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = IslandColors.onSurfaceMuted,
                            )
                        }
                        FloatingCircleButton(
                            onClick = {
                                data?.let {
                                    ShareRoute.share(context, it, shareChooserTitle)
                                }
                            },
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = stringResource(R.string.cd_share_route),
                                tint = IslandColors.onSurface,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingCircleButton(
                            onClick = { data?.route?.id?.let(onEdit) },
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.cd_edit),
                                tint = IslandColors.onSurface,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    MapLayersButton(onClick = chrome::openLayersMenu)
                    Spacer(modifier = Modifier.height(10.dp))
                    if (!deliveryActive) {
                        FloatingCircleButton(onClick = { showAddressSearch = true }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.cd_search_place),
                                tint = IslandColors.onSurface,
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        FloatingCircleButton(onClick = { showLibraryPicker = true }) {
                            Icon(
                                Icons.Default.BookmarkBorder,
                                contentDescription = stringResource(R.string.cd_add_from_library),
                                tint = IslandColors.onSurface,
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    FloatingCircleButton(
                        onClick = {
                            if (navigating) {
                                recenterOnUser()
                                return@FloatingCircleButton
                            }
                            if (!userLocation.hasPermission) {
                                userLocation.requestPermission()
                                locationMessage = context.getString(R.string.msg_allow_location_gps)
                            } else {
                                val loc = userFix
                                if (loc != null) {
                                    viewModel.addStopAtCurrentLocation(
                                        context.getString(R.string.gps_stop_default_name),
                                        loc.latitude,
                                        loc.longitude,
                                    )
                                    locationMessage = null
                                    chrome.bumpRecenter()
                                } else {
                                    userLocation.refresh()
                                    locationMessage = context.getString(R.string.msg_waiting_gps)
                                }
                            }
                        },
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = stringResource(
                                if (navigating) R.string.cd_recenter else R.string.cd_add_my_location,
                            ),
                            tint = if (followPaused) MaterialTheme.colorScheme.primary else IslandColors.onSurface,
                        )
                    }
                }
            }

            // Keep chrome height stable — messages float over the map, they don't
            // insert into the column and shove the stop list / HUD.
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
                    onEndDelivery = { showEndDeliveryDialog = true },
                    onRetryRoute = { beginInAppNavigation() },
                    onOpenExternalMaps = { openExternalMaps(it) },
                    onOpenQueue = { showStopQueue = true },
                )
            } else if (stops.isEmpty()) {
                FloatingIsland(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = 18.dp,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.detail_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = IslandColors.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.detail_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = IslandColors.onSurfaceMuted,
                        )
                        Button(
                            onClick = { showAddressSearch = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.detail_empty_search))
                        }
                        OutlinedButton(
                            onClick = { showLibraryPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.library_pick_title))
                        }
                        Text(
                            text = stringResource(R.string.detail_empty_long_press),
                            style = MaterialTheme.typography.bodySmall,
                            color = IslandColors.onSurfaceMuted,
                        )
                    }
                }
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
                                text = stringResource(
                                    R.string.detail_completed_count,
                                    data?.completedCount ?: 0,
                                    stops.size,
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            if ((data?.completedCount ?: 0) > 0) {
                                TextButton(onClick = { showResetDialog = true }) {
                                    Text(stringResource(R.string.action_reset_progress))
                                }
                            }
                            Button(
                                onClick = {
                                    val unpinned = stops.count { !it.isCompleted && (it.latitude == null || it.longitude == null) }
                                    if (unpinned > 0) {
                                        locationMessage = context.getString(
                                            R.string.msg_stops_missing_pins,
                                            unpinned,
                                        )
                                    }
                                    // Only beginInAppNavigation activates delivery (after GPS is ready).
                                    beginInAppNavigation()
                                },
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.action_start))
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
                                    distanceUnit = settings.distanceUnit,
                                    canMoveUp = index > 0,
                                    canMoveDown = index < stops.lastIndex,
                                    onClick = { viewModel.selectStop(stop.id) },
                                    onCompletedChange = { viewModel.setStopCompleted(stop.id, it) },
                                    onMoveUp = { viewModel.moveStopUp(stop.id) },
                                    onMoveDown = { viewModel.moveStopDown(stop.id) },
                                )
                            }
                        }
                    }
                }
            }
        }

        MapAttributionChip(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 88.dp),
        )

        if (locationMessage != null) {
            FloatingIsland(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 120.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = 12.dp,
            ) {
                Text(locationMessage.orEmpty(), style = MaterialTheme.typography.bodySmall)
            }
        }

        MapLayersMenuHost(chrome = chrome, followChecked = navigating)

        if (showStopQueue && deliveryActive) {
            val remaining = stops.filter { !it.isCompleted }
            val currentId = progress.nextStop?.id ?: navigation.targetStopId
            DeliveryStopQueueSheet(
                remainingStops = remaining,
                currentStopId = currentId,
                onDismiss = { showStopQueue = false },
                onJumpTo = { stop ->
                    viewModel.jumpToStop(stop.id, userFix)
                    chrome.enterDrivingFollow()
                },
                onSkipCurrent = {
                    viewModel.completeNextStop(userFix)
                },
            )
        }

        if (showEndDeliveryDialog) {
            EndDeliveryDialog(
                onDismiss = { showEndDeliveryDialog = false },
                onEndKeepProgress = {
                    showEndDeliveryDialog = false
                    viewModel.endDelivery(resetCompletions = false)
                    chrome.exitDrivingFollow()
                },
                onEndAndReset = {
                    showEndDeliveryDialog = false
                    viewModel.endDelivery(resetCompletions = true)
                    chrome.exitDrivingFollow()
                },
            )
        }

        if (showResetDialog) {
            ResetProgressDialog(
                onDismiss = { showResetDialog = false },
                onConfirm = {
                    showResetDialog = false
                    viewModel.resetCompletions()
                },
            )
        }

        if (showLibraryPicker && !deliveryActive) {
            StopLibraryPickerDialog(
                stops = libraryStops,
                onDismiss = { showLibraryPicker = false },
                onPick = { stop -> viewModel.addStopFromLibrary(stop.id) },
                onManageLibrary = onOpenStopLibrary,
                alreadyOnRouteLibraryIds = stops.mapNotNull { it.libraryStopId }.toSet(),
            )
        }

        if (showAddressSearch && !deliveryActive) {
            AddressSearchDialog(
                onDismissRequest = { showAddressSearch = false },
                near = userFix,
                onPlaceSelected = { place ->
                    showAddressSearch = false
                    focusTarget = place.coordinate
                    focusToken++
                    newStopName = place.shortName
                    newStopNotes = ""
                    viewModel.beginAddStopAt(
                        latitude = place.latitude,
                        longitude = place.longitude,
                        suggestedName = place.shortName,
                        addressHint = place.displayName,
                    )
                },
            )
        }

        pendingPin?.let { pin ->
            AddStopFromPinDialog(
                pin = pin,
                name = newStopName,
                onNameChange = { newStopName = it },
                notes = newStopNotes,
                onNotesChange = { newStopNotes = it },
                alsoSaveToLibrary = saveNewStopToLibrary,
                onAlsoSaveToLibraryChange = { saveNewStopToLibrary = it },
                onDismiss = {
                    viewModel.cancelPendingPin()
                    newStopName = ""
                    newStopNotes = ""
                },
                onConfirm = {
                    viewModel.confirmPendingStop(
                        name = newStopName,
                        notes = newStopNotes,
                        alsoSaveToLibrary = saveNewStopToLibrary,
                    )
                    newStopName = ""
                    newStopNotes = ""
                    saveNewStopToLibrary = false
                },
            )
        }

        if (selectedStop != null && !deliveryActive) {
            SelectedStopDialogs(
                stop = selectedStop,
                onDismiss = { viewModel.selectStop(null) },
                onSave = { name, notes ->
                    viewModel.updateStopDetails(
                        stopId = selectedStop.id,
                        name = name,
                        notes = notes,
                        addressHint = selectedStop.addressHint,
                    )
                    viewModel.selectStop(null)
                },
                onDelete = { viewModel.deleteStop(selectedStop.id) },
                onSaveToLibrary = { viewModel.saveSelectedStopToLibrary(selectedStop.id) },
                onRefreshFromLibrary = {
                    viewModel.refreshStopFromLibrary(selectedStop.id)
                },
                onNavigate = {
                    val stop = selectedStop
                    viewModel.selectStop(null)
                    val fix = userFix
                    if (!userLocation.hasPermission) {
                        userLocation.requestPermission()
                        locationMessage = context.getString(R.string.msg_allow_location_nav)
                    } else if (fix == null) {
                        userLocation.refresh()
                        locationMessage = context.getString(R.string.msg_waiting_gps)
                    } else {
                        locationMessage = null
                        followPaused = false
                        chrome.enterDrivingFollow()
                        viewModel.startNavigationToStop(stop, fix)
                    }
                },
                onOpenExternalMaps = {
                    openExternalMaps(selectedStop)
                    viewModel.selectStop(null)
                },
            )
        }
    }
}

@Composable
private fun StopRow(
    index: Int,
    stop: StopEntity,
    previous: StopEntity?,
    selected: Boolean,
    distanceUnit: com.danielcioban.routeplanner.data.settings.DistanceUnit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val hasPin = stop.latitude != null && stop.longitude != null
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
            if (stop.libraryStopId != null) {
                Text(
                    text = stringResource(R.string.library_linked_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (stop.addressHint.isNotBlank()) {
                Text(
                    text = stop.addressHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = IslandColors.onSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (stop.notes.isNotBlank()) {
                Text(
                    text = stop.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = IslandColors.onSurfaceMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when {
                hasPin && previous?.latitude != null && previous.longitude != null -> {
                    val meters = GeoUtils.distanceMeters(
                        previous.latitude!!,
                        previous.longitude!!,
                        stop.latitude!!,
                        stop.longitude!!,
                    )
                    Text(
                        text = stringResource(
                            R.string.detail_from_previous,
                            GeoUtils.formatDistance(meters, distanceUnit),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                hasPin -> {
                    Text(
                        text = stringResource(R.string.edit_pinned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                else -> {
                    Text(
                        text = stringResource(R.string.detail_no_coordinates),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        Column {
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.cd_move_up),
                )
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.cd_move_down),
                )
            }
        }
    }
}
