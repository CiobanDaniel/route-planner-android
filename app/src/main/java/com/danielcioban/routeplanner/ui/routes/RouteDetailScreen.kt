package com.danielcioban.routeplanner.ui.routes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.pluralStringResource
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
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.components.IslandListItem
import com.danielcioban.routeplanner.ui.components.MapAttributionChip
import com.danielcioban.routeplanner.ui.library.StopLibraryPickerDialog
import com.danielcioban.routeplanner.ui.layout.AdaptiveMapSheet
import com.danielcioban.routeplanner.ui.layout.AppPanes
import com.danielcioban.routeplanner.ui.location.CompassCalibrationHint
import com.danielcioban.routeplanner.ui.location.HeadingGpsSpeedThresholdMps
import com.danielcioban.routeplanner.ui.location.rememberCompassAccuracyLow
import com.danielcioban.routeplanner.ui.location.rememberDeviceBearing
import com.danielcioban.routeplanner.ui.location.rememberMergedUserFix
import com.danielcioban.routeplanner.ui.location.rememberUserLocation
import com.danielcioban.routeplanner.ui.map.LatLng
import com.danielcioban.routeplanner.ui.map.MapLayersButton
import com.danielcioban.routeplanner.ui.map.MapLayersMenuHost
import com.danielcioban.routeplanner.ui.map.MapViewMode
import com.danielcioban.routeplanner.ui.map.RouteMapBackdrop
import com.danielcioban.routeplanner.ui.map.rememberMapChromeState
import com.danielcioban.routeplanner.ui.menu.ScreenMenuButton
import com.danielcioban.routeplanner.ui.nav.ManeuverSpeechEffect
import com.danielcioban.routeplanner.data.backup.PlaceFormatExporter
import com.danielcioban.routeplanner.ui.places.ExportRouteDialog
import com.danielcioban.routeplanner.ui.places.JumpAheadDialog
import com.danielcioban.routeplanner.util.PlaceShare
import com.danielcioban.routeplanner.util.ShareRoute
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.CalendarExport
import com.danielcioban.routeplanner.util.CourierHaptics
import com.danielcioban.routeplanner.util.ExternalNavigation
import com.danielcioban.routeplanner.util.FailureEvidence
import com.danielcioban.routeplanner.util.GeoUtils
import com.danielcioban.routeplanner.util.RouteEta
import com.danielcioban.routeplanner.util.ShareFile
import com.danielcioban.routeplanner.util.ShareNextStop
import com.danielcioban.routeplanner.util.StopListPdf

@Composable
fun RouteDetailScreen(
    viewModel: RouteDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenStopLibrary: () -> Unit = {},
    onOpenMenu: () -> Unit = {},
    autostart: Boolean = false,
    settings: AppSettings = AppSettings(),
    onPreferredMapStyleChange: (MapViewMode) -> Unit = {},
    savedCamera: com.danielcioban.routeplanner.data.settings.SavedMapCamera? = null,
    onCameraMoved: (com.danielcioban.routeplanner.data.settings.SavedMapCamera) -> Unit = {},
) {
    val routeWithStops by viewModel.route.collectAsStateWithLifecycle()
    val deliveryActive by viewModel.deliveryActive.collectAsStateWithLifecycle()
    val pendingPin by viewModel.pendingPin.collectAsStateWithLifecycle()
    val selectedStopId by viewModel.selectedStopId.collectAsStateWithLifecycle()
    val selectedStopTasks by viewModel.selectedStopTasks.collectAsStateWithLifecycle()
    val navigation by viewModel.navigation.collectAsStateWithLifecycle()
    val libraryStops by viewModel.stopLibrary.collectAsStateWithLifecycle()
    val noticeMessageRes by viewModel.noticeMessageRes.collectAsStateWithLifecycle()
    val taskProgressByStopId by viewModel.taskProgressByStopId.collectAsStateWithLifecycle()
    val taskTemplates by viewModel.taskTemplates.collectAsStateWithLifecycle()
    val otherRoutes by viewModel.otherRoutes.collectAsStateWithLifecycle()
    val chrome = rememberMapChromeState(
        preferredBrowseMode = settings.effectiveBrowseStyle(),
        onPreferredModeChange = onPreferredMapStyleChange,
    )
    var focusToken by remember { mutableIntStateOf(0) }
    var focusTarget by remember { mutableStateOf<LatLng?>(null) }
    var followPaused by remember { mutableStateOf(false) }
    var showAddressSearch by remember { mutableStateOf(false) }
    var showAddStopMenu by remember { mutableStateOf(false) }
    var showEndDeliveryDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showStopQueue by remember { mutableStateOf(false) }
    val deliveryPaused by viewModel.deliveryPaused.collectAsStateWithLifecycle()
    val undoMarkDone by viewModel.undoMarkDone.collectAsStateWithLifecycle()
    var showFailDialog by remember { mutableStateOf(false) }
    var pendingFailReason by remember { mutableStateOf<String?>(null) }
    var failPhotoPath by remember { mutableStateOf<String?>(null) }
    var failSignaturePath by remember { mutableStateOf<String?>(null) }
    var showSignaturePad by remember { mutableStateOf(false) }
    var showDeferDialog by remember { mutableStateOf(false) }
    var showBulkDialog by remember { mutableStateOf(false) }
    var failPhotoFile by remember { mutableStateOf<java.io.File?>(null) }
    var showProofDialog by remember { mutableStateOf(false) }
    var proofCompletes by remember { mutableStateOf(false) }
    var proofPhotoPath by remember { mutableStateOf<String?>(null) }
    var proofSignaturePath by remember { mutableStateOf<String?>(null) }
    var proofCollectCod by remember { mutableStateOf(false) }
    var proofPhotoFile by remember { mutableStateOf<java.io.File?>(null) }
    var signatureForProof by remember { mutableStateOf(false) }
    var showScanDialog by remember { mutableStateOf(false) }
    var showRescheduleDialog by remember { mutableStateOf(false) }
    var showLibraryPicker by remember { mutableStateOf(false) }
    var pendingEdit by remember { mutableStateOf<PendingStopPlaceEdit?>(null) }
    var pendingDelete by remember { mutableStateOf<StopEntity?>(null) }
    var pendingJump by remember { mutableStateOf<StopEntity?>(null) }
    var copySourceStopId by remember { mutableStateOf<Long?>(null) }
    var selectedUsageNames by remember { mutableStateOf<List<String>>(emptyList()) }
    val shareChooserTitle = stringResource(R.string.share_route_chooser)
    val inAppNavigating = navigation.phase == NavigationPhase.Navigating ||
        navigation.phase == NavigationPhase.LoadingRoute ||
        navigation.phase == NavigationPhase.Arrived
    val tripActive = (deliveryActive || inAppNavigating) && !deliveryPaused
    val navigating = chrome.driveFollow || tripActive
    val previewLineJson by viewModel.previewLineJson.collectAsStateWithLifecycle()
    val previewFitToken by viewModel.previewFitToken.collectAsStateWithLifecycle()
    val view = LocalView.current
    val userLocation = rememberUserLocation(
        autoRequest = true,
        highFrequency = navigating,
    )
    val compassBearing = rememberDeviceBearing(enabled = navigating)
    val compassLow = rememberCompassAccuracyLow(enabled = navigating)
    var compassHintDismissed by remember { mutableStateOf(false) }
    val userFix = rememberMergedUserFix(
        coordinate = userLocation.coordinate,
        compassBearing = compassBearing,
        active = navigating,
    )
    DisposableEffect(
        settings.keepScreenOnDuringNav,
        settings.keepScreenOnOnlyWhileMoving,
        tripActive,
        deliveryPaused,
        userFix?.speedMps,
    ) {
        view.keepScreenOn = settings.keepScreenAwake(tripActive, userFix?.speedMps)
        onDispose { view.keepScreenOn = false }
    }
    ManeuverSpeechEffect(
        enabled = settings.speakManeuvers,
        navigation = navigation,
        languageTag = settings.language.tag,
        verbosity = settings.ttsVerbosity,
        muteDuringCalls = settings.muteTtsDuringCalls,
    )

    var newStopName by remember { mutableStateOf("") }
    var newStopNotes by remember { mutableStateOf("") }
    var locationMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val takeFailPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            failPhotoPath = failPhotoFile?.absolutePath
        }
    }
    val takeProofPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            proofPhotoPath = proofPhotoFile?.absolutePath
        }
    }
    val moving = deliveryActive &&
        (userFix?.speedMps ?: 0f) >= HeadingGpsSpeedThresholdMps
    val planningChrome = !deliveryActive

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
            viewModel.onUserLocationUpdated(fix, settings)
        }
    }

    val data = routeWithStops
    val stops = data?.deliveryStops.orEmpty()
    val mapStops = remember(data) {
        val originLat = data?.route?.originLatitude
        val originLng = data?.route?.originLongitude
        val origin = if (originLat != null && originLng != null) {
            listOf(
                StopEntity(
                    id = -1L,
                    routeId = data.route.id,
                    position = -1,
                    name = "",
                    latitude = originLat,
                    longitude = originLng,
                    isOrigin = true,
                    isCompleted = true,
                ),
            )
        } else {
            emptyList()
        }
        origin + stops
    }
    val selectedStop = stops.firstOrNull { it.id == selectedStopId }

    LaunchedEffect(selectedStop?.libraryStopId) {
        val libraryId = selectedStop?.libraryStopId
        if (libraryId == null) {
            selectedUsageNames = emptyList()
        } else {
            viewModel.loadLibraryUsage(libraryId) { selectedUsageNames = it.routeNames }
        }
    }
    val progress = viewModel.deliveryProgress()
    val incompleteUnpinnedCount = remember(stops) {
        stops.count { !it.isCompleted && (it.latitude == null || it.longitude == null) }
    }
    val incompleteUnpinnedMessage = pluralStringResource(
        R.plurals.msg_stops_missing_pins,
        incompleteUnpinnedCount,
        incompleteUnpinnedCount,
    )
    val hasPinnedStops = mapStops.any { it.latitude != null && it.longitude != null }
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
    val use24Hour = settings.clockFormat.is24Hour(context)
    val remainingArrivals = remember(userFix, stops) {
        RouteEta.estimateRemaining(
            stops.filter { !it.isCompleted },
            userFix?.latitude,
            userFix?.longitude,
        ).associateBy { it.stopId }
    }
    val nextArrival = progress.nextStop?.id?.let { remainingArrivals[it] }
    val etaLabel = nextArrival?.let { arrival ->
        val clock = GeoUtils.formatClockFromEpoch(arrival.epochMs, use24Hour)
        val promised = progress.nextStop?.arriveByMinutes
        if (arrival.late && promised != null) {
            stringResource(R.string.stop_late, GeoUtils.formatClockMinutes(promised, use24Hour))
        } else {
            stringResource(R.string.stop_eta, clock)
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

    fun openRemainingInMaps() {
        val remainingPins = stops.filter { !it.isCompleted && it.latitude != null && it.longitude != null }
        if (remainingPins.isEmpty()) {
            locationMessage = context.getString(R.string.msg_no_map_pin)
            return
        }
        val dests = remainingPins.map { it.latitude!! to it.longitude!! }.toMutableList()
        if (routeWithStops?.route?.roundTrip == true) {
            val first = stops.firstOrNull {
                !it.isOrigin && it.latitude != null && it.longitude != null
            } ?: stops.firstOrNull { it.latitude != null && it.longitude != null }
            if (first != null) {
                val end = dests.last()
                if (end.first != first.latitude || end.second != first.longitude) {
                    dests += first.latitude!! to first.longitude!!
                }
            }
        }
        val truncated = dests.size > ExternalNavigation.MAX_MAPS_STOPS
        val origin = userFix?.let { it.latitude to it.longitude }
        val opened = ExternalNavigation.openMultiStopDriving(context, dests, origin)
        locationMessage = when {
            !opened -> context.getString(R.string.msg_maps_unavailable)
            truncated -> context.getString(R.string.maps_stops_truncated, ExternalNavigation.MAX_MAPS_STOPS)
            else -> null
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
        chrome.enterDrivingFollow(settings.defaultFollowMe)
        viewModel.startNavigationToNext(fix)
    }

    var autostartConsumed by remember { mutableStateOf(false) }
    LaunchedEffect(autostart, userFix, routeWithStops) {
        if (!autostart || autostartConsumed) return@LaunchedEffect
        val fix = userFix ?: return@LaunchedEffect
        val currentStops = routeWithStops?.deliveryStops.orEmpty()
        if (currentStops.isEmpty()) return@LaunchedEffect
        autostartConsumed = true
        if (currentStops.all { it.isCompleted }) {
            chrome.enterDrivingFollow(settings.defaultFollowMe)
            viewModel.runAgain(fix)
        } else {
            beginInAppNavigation()
        }
    }

    fun recenterOnUser() {
        followPaused = false
        chrome.driveFollow = true
        chrome.bumpRecenter()
    }

    val navDoor = progress.nextStop?.let { stop ->
        val lat = stop.latitude ?: return@let null
        val lng = stop.longitude ?: return@let null
        LatLng(lat, lng)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RouteMapBackdrop(
            stops = mapStops,
            fitStops = hasPinnedStops && !tripActive,
            userLocation = userFix,
            recenterToken = chrome.recenterToken,
            mapViewMode = chrome.mapViewMode,
            driveFollow = chrome.driveFollow,
            navRouteLineJson = if (inAppNavigating) navigation.navLineJson else previewLineJson,
            navRouteFitToken = if (inAppNavigating) navigation.navFitToken else previewFitToken,
            showStraightStopLinks = !inAppNavigating,
            onMapLongClick = { latLng ->
                if (moving) return@RouteMapBackdrop
                CourierHaptics.tick(context)
                viewModel.beginAddStopAt(latLng.latitude, latLng.longitude)
                newStopName = ""
                newStopNotes = ""
            },
            onStopClick = { id -> if (id > 0L) viewModel.selectStop(id) },
            onFollowPaused = { paused -> followPaused = paused },
            focusTarget = focusTarget,
            focusToken = focusToken,
            savedCamera = savedCamera,
            onCameraMoved = onCameraMoved,
            navDoor = navDoor,
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            val wide = AppPanes.isWide(maxWidth)
            val queueRail = AppPanes.showDrivingQueueRail(maxWidth)
            Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = AppPanes.TitleMaxWidth),
                ) {
                    FloatingIsland(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = 12.dp,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                        FloatingCircleButton(onClick = onBack, embedded = true) {
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
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = when {
                                    locationMessage != null -> locationMessage.orEmpty()
                                    followPaused && navigating -> stringResource(R.string.detail_map_free)
                                    deliveryActive -> pluralStringResource(
                                        R.plurals.detail_delivery_active,
                                        progress.remaining,
                                        progress.remaining,
                                    )
                                    stops.isEmpty() -> stringResource(R.string.detail_empty_subtitle)
                                    data?.route?.originLatitude != null ->
                                        stringResource(R.string.origin_set_badge)
                                    else -> stringResource(R.string.detail_long_press_hint)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = IslandColors.onSurfaceMuted,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        FloatingCircleButton(
                            onClick = { showExportDialog = true },
                            embedded = true,
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = stringResource(R.string.cd_share_route),
                                tint = IslandColors.onSurface,
                            )
                        }
                        if (planningChrome) {
                            Spacer(modifier = Modifier.width(8.dp))
                            FloatingCircleButton(
                                onClick = { data?.route?.id?.let(onEdit) },
                                embedded = true,
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.cd_edit),
                                    tint = IslandColors.onSurface,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        ScreenMenuButton(onClick = onOpenMenu)
                        }
                    }
                    MapAttributionChip(
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    if (navigating && compassLow && !compassHintDismissed) {
                        CompassCalibrationHint(
                            onDismiss = { compassHintDismissed = true },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    MapLayersButton(onClick = chrome::openLayersMenu)
                    if (planningChrome) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FloatingCircleButton(onClick = { showAddStopMenu = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.cd_add_stop),
                                tint = IslandColors.onSurface,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FloatingCircleButton(
                        onClick = {
                            if (!planningChrome) {
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
                        prominent = navigating,
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = stringResource(
                                if (navigating) R.string.cd_recenter else R.string.cd_add_my_location,
                            ),
                            modifier = Modifier.size(if (navigating) 28.dp else 24.dp),
                            tint = if (followPaused) MaterialTheme.colorScheme.primary else IslandColors.onSurface,
                        )
                    }
                }
            }

            // Map-gap Box: side pane / HUD overlay the map. Do not grow wrapping chrome.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
            if (deliveryActive) {
                DeliveryHud(
                    modifier = Modifier
                        .align(
                            if (queueRail) Alignment.BottomStart else Alignment.BottomCenter,
                        )
                        .then(
                            if (queueRail) {
                                Modifier
                                    .widthIn(max = AppPanes.HudMaxWidth)
                                    .fillMaxWidth()
                            } else {
                                Modifier.fillMaxWidth()
                            },
                        ),
                    progress = progress,
                    totalStops = stops.size,
                    navigation = navigation,
                    distanceFromYou = distanceFromYou,
                    distanceUnit = settings.distanceUnit,
                    onStartInAppNav = { beginInAppNavigation() },
                    onMarkDone = {
                        val next = progress.nextStop
                        val needsHandoff = settings.askProofOnDone ||
                            (next != null && next.codAmount > 0.0 && !next.codCollected)
                        if (needsHandoff && next != null && !next.isBreak) {
                            proofCompletes = true
                            proofPhotoPath = next.podPhotoPath
                            proofSignaturePath = next.podSignaturePath
                            proofCollectCod = next.codCollected
                            showProofDialog = true
                        } else {
                            viewModel.completeNextStop(userFix)
                        }
                    },
                    onEndDelivery = { showEndDeliveryDialog = true },
                    onRetryRoute = {
                        val fix = userFix
                        if (fix == null) {
                            userLocation.refresh()
                            locationMessage = context.getString(R.string.msg_waiting_gps)
                        } else {
                            viewModel.retryNavigation(fix)
                        }
                    },
                    onOpenExternalMaps = { openExternalMaps(it) },
                    onOpenQueue = { showStopQueue = true },
                    roundTrip = routeWithStops?.route?.roundTrip == true,
                    etaLabel = etaLabel,
                    late = nextArrival?.late == true,
                    markDoneEnabled = run {
                        val next = progress.nextStop
                        next == null ||
                            next.isBreak ||
                            (taskProgressByStopId[next.id]?.requiredRemaining ?: 0) == 0
                    },
                    onFail = { showFailDialog = true },
                    onReschedule = { showRescheduleDialog = true },
                    onPreviewPath = {
                        val fix = userFix
                        if (fix != null) viewModel.previewPathToNext(fix)
                    },
                    onRunAgain = {
                        val fix = userFix
                        if (fix == null) {
                            locationMessage = context.getString(R.string.msg_waiting_gps)
                        } else {
                            chrome.enterDrivingFollow(settings.defaultFollowMe)
                            viewModel.runAgain(fix)
                        }
                    },
                    onReturnToStart = {
                        val fix = userFix
                        if (fix != null) {
                            chrome.enterDrivingFollow()
                            viewModel.startReturnToStart(fix)
                        }
                    },
                    paused = deliveryPaused,
                    enlargedActions = moving,
                    onPause = { viewModel.setTripPaused(!deliveryPaused) },
                    onShareSms = {
                        progress.nextStop?.let { ShareNextStop.sms(context, it, etaLabel) }
                    },
                    onOpenNextThree = {
                        val dests = stops.filter {
                            !it.isCompleted && it.latitude != null && it.longitude != null
                        }.map { it.latitude!! to it.longitude!! }
                        ExternalNavigation.openNextStops(
                            context,
                            dests,
                            userFix?.let { it.latitude to it.longitude },
                        )
                    },
                    onCall = progress.nextStop?.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                        {
                            ShareNextStop.dial(context, phone)
                            Unit
                        }
                    },
                    onDeferTasks = { showDeferDialog = true },
                    onAddStop = { showAddStopMenu = true },
                    onSkipLater = if (progress.remaining > 1) {
                        {
                            progress.nextStop?.id?.let { viewModel.moveStopToBottom(it) }
                        }
                    } else {
                        null
                    },
                    ops = DeliveryHudOps(
                        onProof = progress.nextStop?.takeIf { !it.isBreak }?.let {
                            {
                                proofCompletes = false
                                proofPhotoPath = it.podPhotoPath
                                proofSignaturePath = it.podSignaturePath
                                proofCollectCod = it.codCollected
                                showProofDialog = true
                            }
                        },
                        onScan = { showScanDialog = true },
                        onShareEta = {
                            progress.nextStop?.let { stop ->
                                ShareNextStop.shareEta(
                                    context,
                                    stop,
                                    etaLabel,
                                    context.getString(R.string.share_eta_chooser),
                                )
                            }
                        },
                    ),
                )
                if (queueRail) {
                    val remaining = stops.filter { !it.isCompleted }
                    if (remaining.isNotEmpty()) {
                        AdaptiveMapSheet(
                            wide = true,
                            maxExpandedHeight = 360.dp,
                        ) {
                            DeliveryQueuePane(
                                remainingStops = remaining,
                                currentStopId = progress.nextStop?.id
                                    ?: navigation.targetStopId,
                                onJumpTo = { stop -> pendingJump = stop },
                                onSkipCurrent = {
                                    progress.nextStop?.id?.let { viewModel.moveStopToBottom(it) }
                                },
                            )
                        }
                    }
                }
            } else if (stops.isEmpty()) {
                AdaptiveMapSheet(
                    wide = wide,
                    maxExpandedHeight = 320.dp,
                    contentPadding = 18.dp,
                    fillHeight = false,
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
                        if (data?.route?.originLatitude != null) {
                            Text(
                                text = stringResource(R.string.origin_set_badge),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
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
                        OutlinedButton(
                            onClick = {
                                val loc = userFix
                                if (loc == null) {
                                    locationMessage = context.getString(R.string.msg_waiting_gps)
                                } else {
                                    viewModel.setGpsOrigin(
                                        context.getString(R.string.origin_stop_default_name),
                                        loc.latitude,
                                        loc.longitude,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(stringResource(R.string.action_im_here))
                        }
                    }
                }
            } else {
                AdaptiveMapSheet(
                    wide = wide,
                    maxExpandedHeight = 360.dp,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.detail_completed_count,
                                    data?.completedCount ?: 0,
                                    data?.completedCount ?: 0,
                                    stops.size,
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = {
                                    if (incompleteUnpinnedCount > 0) {
                                        locationMessage = incompleteUnpinnedMessage
                                    }
                                    val allDone = stops.isNotEmpty() && stops.all { it.isCompleted }
                                    if (allDone) {
                                        val fix = userFix
                                        if (fix == null) {
                                            locationMessage = context.getString(R.string.msg_waiting_gps)
                                        } else {
                                            chrome.enterDrivingFollow(settings.defaultFollowMe)
                                            viewModel.runAgain(fix)
                                        }
                                    } else {
                                        beginInAppNavigation()
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(
                                        if (stops.isNotEmpty() && stops.all { it.isCompleted }) {
                                            R.string.action_run_again
                                        } else {
                                            R.string.action_start
                                        },
                                    ),
                                )
                            }
                        }
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                        ) {
                            if (!deliveryActive) {
                                TextButton(
                                    onClick = {
                                        val loc = userFix
                                        if (loc == null) {
                                            locationMessage = context.getString(R.string.msg_waiting_gps)
                                        } else {
                                            viewModel.setGpsOrigin(
                                                context.getString(R.string.origin_stop_default_name),
                                                loc.latitude,
                                                loc.longitude,
                                            )
                                        }
                                    },
                                ) {
                                    Text(stringResource(R.string.action_im_here))
                                }
                            }
                            if (data?.route?.originLatitude != null) {
                                Text(
                                    text = stringResource(R.string.origin_set_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                                )
                            }
                            if (!deliveryActive && stops.any { !it.isCompleted }) {
                                TextButton(
                                    onClick = {
                                        val fix = userFix
                                        if (fix == null) {
                                            locationMessage = context.getString(R.string.msg_waiting_gps)
                                        } else {
                                            viewModel.previewPathToNext(fix)
                                        }
                                    },
                                ) {
                                    Text(stringResource(R.string.action_preview_path))
                                }
                            }
                            if (!deliveryActive && stops.size >= 2) {
                                TextButton(onClick = { viewModel.optimizeStopOrder(userFix) }) {
                                    Text(stringResource(R.string.action_optimize_order))
                                }
                                TextButton(onClick = { viewModel.reverseRemainingStops() }) {
                                    Text(stringResource(R.string.action_reverse_order))
                                }
                            }
                            if (!deliveryActive && stops.any { !it.isCompleted && it.arriveByMinutes != null }) {
                                TextButton(onClick = { viewModel.sortRemainingByArriveBy() }) {
                                    Text(stringResource(R.string.action_sort_arrive_by))
                                }
                            }
                            if (!deliveryActive && stops.any { !it.isCompleted }) {
                                TextButton(onClick = { showBulkDialog = true }) {
                                    Text(stringResource(R.string.nav_bulk_complete))
                                }
                            }
                            if (stops.any { !it.isCompleted && it.latitude != null && it.longitude != null }) {
                                TextButton(onClick = { openRemainingInMaps() }) {
                                    Text(stringResource(R.string.action_open_route_in_maps))
                                }
                            }
                            if ((data?.completedCount ?: 0) > 0) {
                                TextButton(onClick = { showResetDialog = true }) {
                                    Text(stringResource(R.string.action_reset_progress))
                                }
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            itemsIndexed(stops, key = { _, stop -> stop.id }) { index, stop ->
                                val previous = stops.getOrNull(index - 1)
                                StopRow(
                                    index = index + 1,
                                    stop = stop,
                                    previous = previous,
                                    selected = stop.id == selectedStopId,
                                    taskProgress = taskProgressByStopId[stop.id],
                                    distanceUnit = settings.distanceUnit,
                                    use24Hour = use24Hour,
                                    arrival = remainingArrivals[stop.id],
                                    canMoveUp = index > 0,
                                    canMoveDown = index < stops.lastIndex,
                                    onClick = { viewModel.selectStop(stop.id) },
                                    onCompletedChange = { viewModel.setStopCompleted(stop.id, it) },
                                    onMoveUp = { viewModel.moveStopUp(stop.id) },
                                    onMoveDown = { viewModel.moveStopDown(stop.id) },
                                    onMoveToTop = { viewModel.moveStopToTop(stop.id) },
                                    onMoveToBottom = { viewModel.moveStopToBottom(stop.id) },
                                )
                            }
                        }
                    }
                }
            }
            }
            }
        }

        MapLayersMenuHost(chrome = chrome, followChecked = chrome.driveFollow)

        if (showStopQueue && deliveryActive) {
            val remaining = stops.filter { !it.isCompleted }
            val currentId = progress.nextStop?.id ?: navigation.targetStopId
            DeliveryStopQueueSheet(
                remainingStops = remaining,
                currentStopId = currentId,
                onDismiss = { showStopQueue = false },
                onJumpTo = { stop ->
                    showStopQueue = false
                    pendingJump = stop
                },
                onSkipCurrent = {
                    showStopQueue = false
                    progress.nextStop?.id?.let { viewModel.moveStopToBottom(it) }
                },
            )
        }

        pendingJump?.let { stop ->
            JumpAheadDialog(
                stopName = stop.name,
                onDismiss = { pendingJump = null },
                onGoOnly = {
                    pendingJump = null
                    chrome.enterDrivingFollow()
                    viewModel.jumpToStop(stop.id, userFix, markPreviousDone = false)
                },
                onMarkPreviousDone = {
                    pendingJump = null
                    chrome.enterDrivingFollow()
                    viewModel.jumpToStop(stop.id, userFix, markPreviousDone = true)
                },
            )
        }

        if (showFailDialog) {
            val failName = progress.nextStop?.name.orEmpty()
            FailStopDialog(
                stopName = failName,
                onDismiss = { showFailDialog = false },
                onConfirm = { reason ->
                    showFailDialog = false
                    pendingFailReason = reason
                    failPhotoPath = null
                    failSignaturePath = null
                },
            )
        }

        pendingFailReason?.let { reason ->
            FailEvidenceDialog(
                photoAttached = failPhotoPath != null,
                signed = failSignaturePath != null,
                onDismiss = { pendingFailReason = null },
                onTakePhoto = {
                    val file = FailureEvidence.newPhotoFile(context)
                    failPhotoFile = file
                    takeFailPhoto.launch(FailureEvidence.uriFor(context, file))
                },
                onSign = {
                    signatureForProof = false
                    showSignaturePad = true
                },
                onSkip = {
                    viewModel.failCurrentStop(reason, userFix)
                    pendingFailReason = null
                },
                onSave = {
                    viewModel.failCurrentStop(
                        reason,
                        userFix,
                        photoPath = failPhotoPath,
                        signaturePath = failSignaturePath,
                    )
                    pendingFailReason = null
                },
            )
        }

        if (showSignaturePad) {
            SignaturePadDialog(
                onDismiss = { showSignaturePad = false },
                onSave = { bitmap ->
                    val path = FailureEvidence.saveSignature(context, bitmap)
                    if (signatureForProof) {
                        proofSignaturePath = path
                    } else {
                        failSignaturePath = path
                    }
                    showSignaturePad = false
                },
            )
        }

        if (showProofDialog) {
            val next = progress.nextStop
            ProofOfDeliveryDialog(
                photoAttached = proofPhotoPath != null,
                signed = proofSignaturePath != null,
                codAmount = next?.codAmount ?: 0.0,
                collectCod = proofCollectCod,
                onCollectCodChange = { proofCollectCod = it },
                onDismiss = { showProofDialog = false },
                onTakePhoto = {
                    val file = FailureEvidence.newPhotoFile(context)
                    proofPhotoFile = file
                    takeProofPhoto.launch(FailureEvidence.uriFor(context, file))
                },
                onSign = {
                    signatureForProof = true
                    showSignaturePad = true
                },
                onSkip = {
                    showProofDialog = false
                    if (proofCompletes) viewModel.completeNextStop(userFix)
                },
                onSave = {
                    viewModel.saveProofOfDelivery(proofPhotoPath, proofSignaturePath)
                    viewModel.setCodCollected(proofCollectCod)
                    showProofDialog = false
                    if (proofCompletes) viewModel.completeNextStop(userFix)
                },
            )
        }

        if (showScanDialog) {
            ScanCodeDialog(
                onDismiss = { showScanDialog = false },
                onSubmit = { raw ->
                    showScanDialog = false
                    viewModel.applyScan(raw)
                },
                onScanCamera = {
                    PlayBarcodeScan.start(context) { raw ->
                        if (!raw.isNullOrBlank()) {
                            showScanDialog = false
                            viewModel.applyScan(raw)
                        }
                    }
                },
            )
        }

        if (showDeferDialog) {
            DeferTasksDialog(
                stopName = progress.nextStop?.name.orEmpty(),
                onDismiss = { showDeferDialog = false },
                onConfirm = { note ->
                    showDeferDialog = false
                    viewModel.completeNextStopDeferred(note, userFix)
                },
            )
        }

        if (showBulkDialog) {
            BulkCompleteDialog(
                onDismiss = { showBulkDialog = false },
                onConfirm = {
                    showBulkDialog = false
                    viewModel.completeRemainingStops()
                },
            )
        }

        if (showRescheduleDialog) {
            val current = progress.nextStop
            RescheduleStopDialog(
                stopName = current?.name.orEmpty(),
                currentArriveByMinutes = current?.arriveByMinutes,
                onDismiss = { showRescheduleDialog = false },
                onEndOfRoute = {
                    showRescheduleDialog = false
                    viewModel.rescheduleCurrentStop()
                },
                onPlusOneHour = {
                    showRescheduleDialog = false
                    viewModel.rescheduleCurrentStop(addMinutes = 60)
                },
                onPickArriveBy = { minutes ->
                    showRescheduleDialog = false
                    viewModel.rescheduleCurrentStop(arriveByMinutes = minutes)
                },
                onPickDateTime = { epoch ->
                    showRescheduleDialog = false
                    viewModel.rescheduleCurrentStop(arriveByEpochMs = epoch)
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

        if (showExportDialog) {
            ExportRouteDialog(
                onDismiss = { showExportDialog = false },
                onShareText = {
                    data?.let { ShareRoute.share(context, it, shareChooserTitle) }
                    showExportDialog = false
                },
                onCsv = {
                    data?.let {
                        PlaceShare.shareText(
                            context = context,
                            subject = it.route.name,
                            body = PlaceFormatExporter.routeCsv(it),
                            mimeType = "text/csv",
                            chooserTitle = context.getString(R.string.csv_export_chooser),
                        )
                    }
                    showExportDialog = false
                },
                onGpx = {
                    data?.let {
                        PlaceShare.shareText(
                            context = context,
                            subject = it.route.name,
                            body = PlaceFormatExporter.routeGpx(it),
                            mimeType = "application/gpx+xml",
                            chooserTitle = context.getString(R.string.export_places_chooser),
                        )
                    }
                    showExportDialog = false
                },
                onKml = {
                    data?.let {
                        PlaceShare.shareText(
                            context = context,
                            subject = it.route.name,
                            body = PlaceFormatExporter.routeKml(it),
                            mimeType = "application/vnd.google-earth.kml+xml",
                            chooserTitle = context.getString(R.string.export_places_chooser),
                        )
                    }
                    showExportDialog = false
                },
                onGeoJson = {
                    data?.let {
                        PlaceShare.shareText(
                            context = context,
                            subject = it.route.name,
                            body = PlaceFormatExporter.routeGeoJson(it),
                            mimeType = "application/geo+json",
                            chooserTitle = context.getString(R.string.export_places_chooser),
                        )
                    }
                    showExportDialog = false
                },
                onIcs = {
                    data?.let { route ->
                        val file = ShareFile.writeCache(
                            context,
                            "route-${route.route.id}.ics",
                            CalendarExport.icsForRoute(route),
                        )
                        ShareFile.share(
                            context,
                            file,
                            "text/calendar",
                            context.getString(R.string.export_ics_chooser),
                            route.route.name,
                        )
                    }
                    showExportDialog = false
                },
                onPdf = {
                    data?.let { route ->
                        val file = StopListPdf.write(context, route)
                        ShareFile.share(
                            context,
                            file,
                            "application/pdf",
                            context.getString(R.string.export_pdf_chooser),
                            route.route.name,
                        )
                    }
                    showExportDialog = false
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

        if (showAddStopMenu) {
            IslandDialog(
                onDismissRequest = { showAddStopMenu = false },
                title = stringResource(R.string.add_stop_title),
                dismissLabel = stringResource(R.string.action_cancel),
            ) {
                IslandListItem(
                    onClick = {
                        showAddStopMenu = false
                        showAddressSearch = true
                    },
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = IslandColors.onSurface,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.add_stop_search),
                        style = MaterialTheme.typography.bodyLarge,
                        color = IslandColors.onSurface,
                    )
                }
                IslandListItem(
                    onClick = {
                        showAddStopMenu = false
                        showLibraryPicker = true
                    },
                ) {
                    Icon(
                        Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = IslandColors.onSurface,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.add_stop_library),
                        style = MaterialTheme.typography.bodyLarge,
                        color = IslandColors.onSurface,
                    )
                }
            }
        }

        if (showLibraryPicker) {
            StopLibraryPickerDialog(
                stops = libraryStops,
                onDismiss = { showLibraryPicker = false },
                onPick = { stop -> viewModel.addStopFromLibrary(stop.id) },
                onManageLibrary = onOpenStopLibrary,
                alreadyOnRouteLibraryIds = stops.mapNotNull { it.libraryStopId }.toSet(),
            )
        }

        if (showAddressSearch) {
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
                        phone = place.phone,
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
                onDismiss = {
                    viewModel.cancelPendingPin()
                    newStopName = ""
                    newStopNotes = ""
                },
                onConfirm = {
                    CourierHaptics.tick(context)
                    viewModel.confirmPendingStop(
                        name = newStopName,
                        notes = newStopNotes,
                    )
                    newStopName = ""
                    newStopNotes = ""
                },
            )
        }

        if (selectedStop != null) {
            SelectedStopDialogs(
                stop = selectedStop,
                tasks = selectedStopTasks,
                usageRouteNames = selectedUsageNames,
                placeReadOnly = deliveryActive,
                onDismiss = { viewModel.selectStop(null) },
                onSave = { name, notes, arriveByMinutes, serviceMinutes, geofenceRadiusMeters,
                    arriveByEpochMs, phone, doorCode, isFixedOrder, isBreak, codAmount, barcode, ->
                    val stop = selectedStop
                    val libraryId = stop.libraryStopId
                    if (libraryId == null) {
                        viewModel.updateStopDetails(
                            stopId = stop.id,
                            name = name,
                            notes = notes,
                            addressHint = stop.addressHint,
                            arriveByMinutes = arriveByMinutes,
                            serviceMinutes = serviceMinutes,
                            geofenceRadiusMeters = geofenceRadiusMeters,
                            arriveByEpochMs = arriveByEpochMs,
                            phone = phone,
                            doorCode = doorCode,
                            isFixedOrder = isFixedOrder,
                            isBreak = isBreak,
                            codAmount = codAmount,
                            barcode = barcode,
                        )
                        viewModel.selectStop(null)
                    } else {
                        viewModel.loadLibraryUsage(libraryId) { usage ->
                            if (usage.count > 1) {
                                pendingEdit = PendingStopPlaceEdit(
                                    stopId = stop.id,
                                    name = name,
                                    notes = notes,
                                    addressHint = stop.addressHint,
                                    usage = usage,
                                    arriveByMinutes = arriveByMinutes,
                                    serviceMinutes = serviceMinutes,
                                    geofenceRadiusMeters = geofenceRadiusMeters,
                                    arriveByEpochMs = arriveByEpochMs,
                                    phone = phone,
                                    doorCode = doorCode,
                                    isFixedOrder = isFixedOrder,
                                    isBreak = isBreak,
                                    codAmount = codAmount,
                                    barcode = barcode,
                                )
                            } else {
                                viewModel.updateStopDetails(
                                    stopId = stop.id,
                                    name = name,
                                    notes = notes,
                                    addressHint = stop.addressHint,
                                    arriveByMinutes = arriveByMinutes,
                                    serviceMinutes = serviceMinutes,
                                    geofenceRadiusMeters = geofenceRadiusMeters,
                                    arriveByEpochMs = arriveByEpochMs,
                                    phone = phone,
                                    doorCode = doorCode,
                                    isFixedOrder = isFixedOrder,
                                    isBreak = isBreak,
                                    codAmount = codAmount,
                                    barcode = barcode,
                                )
                                viewModel.selectStop(null)
                            }
                        }
                    }
                },
                onDelete = {
                    if (selectedStop.libraryStopId == null) {
                        viewModel.deleteStop(selectedStop.id)
                    } else {
                        pendingDelete = selectedStop
                    }
                },
                onAddTask = viewModel::addSelectedStopTask,
                onApplyTemplate = viewModel::applyTemplateToSelected,
                onApplyTemplateToRemaining = viewModel::applyTemplateToRemainingFromSelected,
                templates = taskTemplates,
                onTaskCompletedChange = viewModel::setStopTaskCompleted,
                onCompletionNoteChange = viewModel::updateStopTaskCompletionNote,
                onUpdateTask = viewModel::updateStopTask,
                onDeleteTask = viewModel::deleteStopTask,
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
                onCall = {
                    ShareNextStop.dial(context, selectedStop.phone)
                },
                onCopyToRoute = {
                    copySourceStopId = selectedStop.id
                    viewModel.selectStop(null)
                },
            )
        }

        copySourceStopId?.let { stopId ->
            CopyStopTargetDialog(
                routes = otherRoutes.filter {
                    it.route.id != routeWithStops?.route?.id && !it.route.archived
                },
                onDismiss = { copySourceStopId = null },
                onPick = { targetId ->
                    viewModel.duplicateStopToRoute(stopId, targetId)
                    copySourceStopId = null
                },
            )
        }

        pendingEdit?.let { edit ->
            com.danielcioban.routeplanner.ui.library.LibraryEditScopeDialog(
                stopName = edit.name,
                routeNames = edit.usage.routeNames,
                showThisRoute = true,
                onEverywhere = {
                    viewModel.updateStopDetails(
                        stopId = edit.stopId,
                        name = edit.name,
                        notes = edit.notes,
                        addressHint = edit.addressHint,
                        arriveByMinutes = edit.arriveByMinutes,
                        serviceMinutes = edit.serviceMinutes,
                        geofenceRadiusMeters = edit.geofenceRadiusMeters,
                        arriveByEpochMs = edit.arriveByEpochMs,
                        phone = edit.phone,
                        doorCode = edit.doorCode,
                        isFixedOrder = edit.isFixedOrder,
                        isBreak = edit.isBreak,
                        codAmount = edit.codAmount,
                        barcode = edit.barcode,
                        scope = com.danielcioban.routeplanner.data.LibraryEditScope.Global,
                    )
                    pendingEdit = null
                    viewModel.selectStop(null)
                },
                onThisRoute = {
                    viewModel.updateStopDetails(
                        stopId = edit.stopId,
                        name = edit.name,
                        notes = edit.notes,
                        addressHint = edit.addressHint,
                        arriveByMinutes = edit.arriveByMinutes,
                        serviceMinutes = edit.serviceMinutes,
                        geofenceRadiusMeters = edit.geofenceRadiusMeters,
                        arriveByEpochMs = edit.arriveByEpochMs,
                        phone = edit.phone,
                        doorCode = edit.doorCode,
                        isFixedOrder = edit.isFixedOrder,
                        isBreak = edit.isBreak,
                        codAmount = edit.codAmount,
                        barcode = edit.barcode,
                        scope = com.danielcioban.routeplanner.data.LibraryEditScope.ThisRoute,
                    )
                    pendingEdit = null
                    viewModel.selectStop(null)
                },
                onSaveAsCopy = { pendingEdit = null },
                onDismiss = { pendingEdit = null },
            )
        }

        pendingDelete?.let { stop ->
            com.danielcioban.routeplanner.ui.library.LibraryDeleteScopeDialog(
                stopName = stop.name,
                routeNames = selectedUsageNames,
                showThisRoute = true,
                onEverywhere = {
                    viewModel.deleteStop(
                        stop.id,
                        com.danielcioban.routeplanner.data.LibraryDeleteScope.Everywhere,
                    )
                    pendingDelete = null
                    viewModel.selectStop(null)
                },
                onThisRoute = {
                    viewModel.deleteStop(
                        stop.id,
                        com.danielcioban.routeplanner.data.LibraryDeleteScope.ThisRouteOnly,
                    )
                    pendingDelete = null
                    viewModel.selectStop(null)
                },
                onDismiss = { pendingDelete = null },
            )
        }

        undoMarkDone?.let {
            FloatingIsland(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 108.dp),
                shape = RoundedCornerShape(22.dp),
                contentPadding = 12.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.nav_undo_done),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                        color = IslandColors.onSurface,
                    )
                    TextButton(onClick = { viewModel.undoLastCompletion() }) {
                        Text(stringResource(R.string.nav_undo_done))
                    }
                }
            }
        }
    }
}
