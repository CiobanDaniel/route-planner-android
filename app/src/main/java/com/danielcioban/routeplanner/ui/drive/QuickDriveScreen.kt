package com.danielcioban.routeplanner.ui.drive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.danielcioban.routeplanner.data.routing.ManeuverFormatter
import com.danielcioban.routeplanner.data.routing.NavGuidance
import com.danielcioban.routeplanner.data.routing.NavigationProgress
import com.danielcioban.routeplanner.data.settings.AppSettings
import com.danielcioban.routeplanner.ui.components.CollapsibleBottomIsland
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.components.MapAttributionChip
import com.danielcioban.routeplanner.ui.layout.AppPanes
import com.danielcioban.routeplanner.ui.location.CompassCalibrationHint
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
import com.danielcioban.routeplanner.ui.routes.NavigationPhase
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.ExternalNavigation
import com.danielcioban.routeplanner.util.GeoUtils

@Composable
fun QuickDriveScreen(
    viewModel: QuickDriveViewModel,
    settings: AppSettings,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    onOpenMenu: () -> Unit = {},
    onSavedAsRoute: (Long) -> Unit = {},
    onPreferredMapStyleChange: (MapViewMode) -> Unit = {},
    savedCamera: com.danielcioban.routeplanner.data.settings.SavedMapCamera? = null,
    onCameraMoved: (com.danielcioban.routeplanner.data.settings.SavedMapCamera) -> Unit = {},
) {
    val sessionReady by viewModel.sessionReady.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val navigation by viewModel.navigation.collectAsStateWithLifecycle()
    val noticeMessageRes by viewModel.noticeMessageRes.collectAsStateWithLifecycle()
    val chrome = rememberMapChromeState(
        preferredBrowseMode = settings.effectiveBrowseStyle(),
        onPreferredModeChange = onPreferredMapStyleChange,
    )
    val dest = remember(session) {
        val lat = session.quickLatitude
        val lng = session.quickLongitude
        if (lat == null || lng == null) {
            null
        } else {
            StopEntity(
                id = session.quickLibraryStopId ?: -1L,
                routeId = 0,
                position = 0,
                name = session.quickName.orEmpty(),
                latitude = lat,
                longitude = lng,
                libraryStopId = session.quickLibraryStopId,
            )
        }
    }
    val inAppNavigating = navigation.phase == NavigationPhase.Navigating ||
        navigation.phase == NavigationPhase.LoadingRoute ||
        navigation.phase == NavigationPhase.Arrived
    val tripActive = sessionReady == true && inAppNavigating
    val navigating = chrome.driveFollow || tripActive
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
        sessionReady,
        userFix?.speedMps,
    ) {
        view.keepScreenOn = settings.keepScreenAwake(sessionReady == true, userFix?.speedMps)
        onDispose { view.keepScreenOn = false }
    }
    ManeuverSpeechEffect(
        enabled = settings.speakManeuvers,
        navigation = navigation,
        languageTag = settings.language.tag,
        verbosity = settings.ttsVerbosity,
        muteDuringCalls = settings.muteTtsDuringCalls,
    )
    var followPaused by remember { mutableStateOf(false) }
    var locationMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(sessionReady) {
        if (sessionReady == false) onFinished()
        if (sessionReady == true) {
            chrome.enterDrivingFollow(settings.defaultFollowMe)
        }
    }
    LaunchedEffect(noticeMessageRes) {
        val res = noticeMessageRes ?: return@LaunchedEffect
        locationMessage = context.getString(res)
        viewModel.consumeNotice()
    }
    LaunchedEffect(userLocation.message) {
        val gpsMessage = userLocation.message
        if (gpsMessage != null) locationMessage = gpsMessage
    }
    LaunchedEffect(userFix, session.kind) {
        val fix = userFix ?: return@LaunchedEffect
        if (sessionReady != true) return@LaunchedEffect
        viewModel.startIfNeeded(fix)
        viewModel.onUserLocationUpdated(fix, settings)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RouteMapBackdrop(
            stops = listOfNotNull(dest),
            fitStops = dest != null && !tripActive,
            userLocation = userFix,
            recenterToken = chrome.recenterToken,
            mapViewMode = chrome.mapViewMode,
            driveFollow = chrome.driveFollow,
            navRouteLineJson = navigation.navLineJson,
            navRouteFitToken = navigation.navFitToken,
            showStraightStopLinks = false,
            onFollowPaused = { paused -> followPaused = paused },
            savedCamera = savedCamera,
            onCameraMoved = onCameraMoved,
            navDoor = dest?.let { stop ->
                val lat = stop.latitude ?: return@let null
                val lng = stop.longitude ?: return@let null
                LatLng(lat, lng)
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
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                FloatingIsland(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = AppPanes.TitleMaxWidth),
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
                                text = dest?.name?.ifBlank { stringResource(R.string.drive_here_unnamed) }
                                    ?: stringResource(R.string.drive_here_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = IslandColors.onSurface,
                            )
                            Text(
                                text = when {
                                    locationMessage != null -> locationMessage.orEmpty()
                                    followPaused && navigating -> stringResource(R.string.detail_map_free)
                                    else -> stringResource(R.string.quick_drive_subtitle)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = IslandColors.onSurfaceMuted,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        ScreenMenuButton(onClick = onOpenMenu)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    MapLayersButton(onClick = chrome::openLayersMenu)
                    Spacer(modifier = Modifier.height(10.dp))
                    FloatingCircleButton(
                        onClick = {
                            if (!userLocation.hasPermission) {
                                userLocation.requestPermission()
                                locationMessage = context.getString(R.string.msg_allow_location_nav)
                            } else {
                                userLocation.refresh()
                                followPaused = false
                                chrome.driveFollow = true
                                chrome.bumpRecenter()
                            }
                        },
                        prominent = navigating,
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = stringResource(R.string.cd_my_location),
                            modifier = Modifier.size(if (navigating) 28.dp else 24.dp),
                            tint = if (followPaused) MaterialTheme.colorScheme.primary else IslandColors.onSurface,
                        )
                    }
                }
            }

            MapAttributionChip(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
            if (navigating && compassLow && !compassHintDismissed) {
                CompassCalibrationHint(
                    onDismiss = { compassHintDismissed = true },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
            QuickDriveHud(
                modifier = Modifier
                    .align(if (wide) Alignment.BottomStart else Alignment.BottomCenter)
                    .then(
                        if (wide) {
                            Modifier
                                .widthIn(max = AppPanes.HudMaxWidth)
                                .fillMaxWidth()
                        } else {
                            Modifier.fillMaxWidth()
                        },
                    ),
                dest = dest,
                navigation = navigation,
                distanceUnit = settings.distanceUnit,
                waitingForGps = userFix == null,
                onRetry = {
                    val fix = userFix
                    if (fix == null) {
                        locationMessage = context.getString(R.string.msg_waiting_gps)
                    } else {
                        viewModel.retry(fix)
                    }
                },
                onArrived = { viewModel.confirmArrived(onFinished) },
                onCancel = { viewModel.cancel(onFinished) },
                onOpenExternalMaps = { stop ->
                    val lat = stop.latitude
                    val lng = stop.longitude
                    if (lat == null || lng == null) {
                        locationMessage = context.getString(R.string.msg_no_map_pin)
                    } else {
                        ExternalNavigation.openDrivingDirections(context, lat, lng, stop.name)
                    }
                },
                onSaveAsRoute = {
                    viewModel.saveAsRoute(
                        roundTrip = settings.defaultRoundTrip,
                        onCreated = onSavedAsRoute,
                    )
                },
            )
            }
            }
        }

        MapLayersMenuHost(chrome = chrome)
    }
}

@Composable
private fun QuickDriveHud(
    dest: StopEntity?,
    navigation: com.danielcioban.routeplanner.ui.routes.NavigationUiState,
    distanceUnit: com.danielcioban.routeplanner.data.settings.DistanceUnit,
    waitingForGps: Boolean,
    onRetry: () -> Unit,
    onArrived: () -> Unit,
    onCancel: () -> Unit,
    onOpenExternalMaps: (StopEntity) -> Unit,
    onSaveAsRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    val approximate = navigation.route?.isApproximate == true
    CollapsibleBottomIsland(
        modifier = modifier,
        maxExpandedHeight = 380.dp,
        collapsedHeight = 88.dp,
        contentPadding = 16.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.quick_drive_hud_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            when {
                dest == null || waitingForGps && navigation.phase == NavigationPhase.Idle -> {
                    Text(
                        text = stringResource(R.string.msg_waiting_gps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = IslandColors.onSurfaceMuted,
                    )
                    CancelRow(onCancel = onCancel)
                }
                navigation.phase == NavigationPhase.LoadingRoute -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                        Column {
                            Text(
                                text = stringResource(R.string.nav_calculating),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = IslandColors.onSurface,
                            )
                            Text(
                                text = dest.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = IslandColors.onSurfaceMuted,
                            )
                        }
                    }
                }
                navigation.phase == NavigationPhase.Error -> {
                    Text(
                        text = dest.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = IslandColors.onSurface,
                    )
                    Text(
                        text = stringResource(navigation.errorMessageRes ?: R.string.nav_error_generic),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.nav_retry_route))
                    }
                    ExternalMapsButton { onOpenExternalMaps(dest) }
                    CancelRow(onCancel = onCancel)
                }
                navigation.phase == NavigationPhase.Arrived -> {
                    Text(
                        text = stringResource(R.string.nav_arrived),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = dest.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = IslandColors.onSurface,
                    )
                    Button(
                        onClick = onArrived,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.quick_drive_im_here))
                    }
                    ExternalMapsButton { onOpenExternalMaps(dest) }
                    CancelRow(onCancel = onCancel)
                }
                navigation.phase == NavigationPhase.Navigating && navigation.guidance != null -> {
                    NavigatingBlock(
                        dest = dest,
                        guidance = navigation.guidance,
                        approximate = approximate,
                        approxMessageRes = navigation.errorMessageRes,
                        distanceUnit = distanceUnit,
                        resources = resources,
                        onRetry = onRetry,
                        onArrived = onArrived,
                        onCancel = onCancel,
                        onOpenExternalMaps = onOpenExternalMaps,
                    )
                }
                else -> {
                    Text(
                        text = dest.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = IslandColors.onSurface,
                    )
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.nav_start))
                    }
                    CancelRow(onCancel = onCancel)
                }
            }
            OutlinedButton(
                onClick = onSaveAsRoute,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.quick_drive_save_route))
            }
        }
    }
}

@Composable
private fun ColumnScope.NavigatingBlock(
    dest: StopEntity,
    guidance: NavGuidance,
    approximate: Boolean,
    approxMessageRes: Int?,
    distanceUnit: com.danielcioban.routeplanner.data.settings.DistanceUnit,
    resources: android.content.res.Resources,
    onRetry: () -> Unit,
    onArrived: () -> Unit,
    onCancel: () -> Unit,
    onOpenExternalMaps: (StopEntity) -> Unit,
) {
    val step = guidance.currentStep
    if (approximate) {
        Text(
            text = stringResource(approxMessageRes ?: R.string.nav_approx_roads_unavailable),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.nav_retry_route))
        }
    }
    Text(
        text = GeoUtils.formatDistance(guidance.distanceToManeuverMeters, distanceUnit),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = IslandColors.onSurface,
    )
    Text(
        text = when {
            approximate -> stringResource(R.string.nav_approx_head, dest.name)
            step != null -> ManeuverFormatter.formatStep(resources, step)
            else -> stringResource(R.string.nav_continue_to, dest.name)
        },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = IslandColors.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
    if (!approximate) {
        guidance.thenStep?.let { then ->
            Text(
                text = stringResource(
                    R.string.nav_then,
                    ManeuverFormatter.formatStep(resources, then),
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = IslandColors.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    Text(
        text = buildString {
            append(GeoUtils.formatDistance(guidance.remainingDistanceMeters, distanceUnit))
            append(" · ")
            append(NavigationProgress.formatEta(guidance.remainingDurationSeconds))
            append(" · ")
            append(dest.name)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = IslandColors.onSurfaceMuted,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onArrived,
            modifier = Modifier.weight(1.2f),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.quick_drive_im_here))
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.Stop, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.action_cancel))
        }
    }
    ExternalMapsButton { onOpenExternalMaps(dest) }
}

@Composable
private fun ExternalMapsButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.nav_open_external))
    }
}

@Composable
private fun CancelRow(onCancel: () -> Unit) {
    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.quick_drive_cancel))
    }
}
