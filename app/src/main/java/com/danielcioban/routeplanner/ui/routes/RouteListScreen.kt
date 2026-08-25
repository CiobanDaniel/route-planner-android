package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.delivery.DriveKind
import com.danielcioban.routeplanner.data.local.DayPlan
import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.data.local.ShiftSlot
import com.danielcioban.routeplanner.data.settings.DistanceUnit
import com.danielcioban.routeplanner.ui.components.AddressSearchDialog
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.components.IslandListItem
import com.danielcioban.routeplanner.ui.components.MapAttributionChip
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.drive.DriveHereTarget
import com.danielcioban.routeplanner.ui.layout.AdaptiveRailIsland
import com.danielcioban.routeplanner.ui.layout.AppPanes
import com.danielcioban.routeplanner.ui.places.ConfirmDeleteRouteDialog
import com.danielcioban.routeplanner.ui.places.PlaceActionDialog
import com.danielcioban.routeplanner.ui.places.RoutePickerDialog
import com.danielcioban.routeplanner.ui.location.rememberDeviceBearing
import com.danielcioban.routeplanner.ui.location.rememberMergedUserFix
import com.danielcioban.routeplanner.ui.location.rememberUserLocation
import com.danielcioban.routeplanner.ui.map.MapLayersButton
import com.danielcioban.routeplanner.ui.map.MapLayersMenuHost
import com.danielcioban.routeplanner.ui.map.MapViewMode
import com.danielcioban.routeplanner.ui.map.RouteMapBackdrop
import com.danielcioban.routeplanner.ui.map.rememberMapChromeState
import com.danielcioban.routeplanner.ui.onboarding.FirstRunChecklistDialog
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.ui.theme.RouteColors
import com.danielcioban.routeplanner.ui.trip.TripGuidanceService
import com.danielcioban.routeplanner.ui.trip.TripHudStore
import com.danielcioban.routeplanner.util.ExternalNavigation
import com.danielcioban.routeplanner.util.GeoUtils
import com.danielcioban.routeplanner.util.RouteEta
import com.danielcioban.routeplanner.util.ShareRoute

@Composable
fun RouteListScreen(
    viewModel: RouteListViewModel,
    onCreateRoute: () -> Unit,
    onOpenRoute: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenStopLibrary: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenTripHistory: () -> Unit = {},
    onOpenMenu: () -> Unit = {},
    onOpenQuickDrive: () -> Unit = {},
    onDriveHere: (DriveHereTarget, Boolean) -> Unit = { _, _ -> },
    onStartRoute: (Long) -> Unit = {},
    preferredMapStyle: MapViewMode = MapViewMode.MAP,
    onPreferredMapStyleChange: (MapViewMode) -> Unit = {},
    distanceUnit: DistanceUnit = DistanceUnit.METRIC,
    savedCamera: com.danielcioban.routeplanner.data.settings.SavedMapCamera? = null,
    onCameraMoved: (com.danielcioban.routeplanner.data.settings.SavedMapCamera) -> Unit = {},
    onboardingDismissed: Boolean = true,
    onDismissOnboarding: () -> Unit = {},
    homeName: String = "",
    homeLatitude: Double? = null,
    homeLongitude: Double? = null,
    dispatcherVanName: String = "",
    showBackupReminder: Boolean = false,
    onDriveHome: () -> Unit = {},
    defaultRoundTrip: Boolean = false,
) {
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    val deliverySession by viewModel.deliverySession.collectAsStateWithLifecycle()
    val chrome = rememberMapChromeState(
        preferredBrowseMode = preferredMapStyle,
        onPreferredModeChange = onPreferredMapStyleChange,
    )
    var showAddressSearch by remember { mutableStateOf(false) }
    var pendingPlace by remember { mutableStateOf<DriveHereTarget?>(null) }
    var pickingRouteFor by remember { mutableStateOf<DriveHereTarget?>(null) }
    var pendingDelete by remember { mutableStateOf<RouteWithStops?>(null) }
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.share_route_chooser)
    val copySuffix = stringResource(R.string.route_copy_suffix)
    var routeQuery by remember { mutableStateOf("") }
    var listFilter by remember { mutableStateOf(HomeRouteFilter.ALL) }
    var shiftFilter by remember { mutableStateOf("") }
    var myVanOnly by remember { mutableStateOf(false) }
    val filteredRoutes = remember(routes, routeQuery, listFilter, shiftFilter, myVanOnly, dispatcherVanName) {
        routes.filter { route ->
            val inBucket = when (listFilter) {
                HomeRouteFilter.ALL -> !route.route.archived
                HomeRouteFilter.TODAY -> route.isTodayWork()
                HomeRouteFilter.ARCHIVED -> route.route.archived
            }
            inBucket &&
                route.matchesQuery(routeQuery) &&
                DayPlan.matchesVan(route, dispatcherVanName, myVanOnly) &&
                DayPlan.matchesShift(route, shiftFilter)
        }
    }
    val daySections = remember(filteredRoutes, listFilter) {
        DayPlan.sections(filteredRoutes, groupBySlot = listFilter == HomeRouteFilter.TODAY)
    }
    val userLocation = rememberUserLocation(
        autoRequest = true,
        highFrequency = chrome.driveFollow,
    )
    val compassBearing = rememberDeviceBearing(enabled = chrome.driveFollow)
    val userFix = rememberMergedUserFix(
        coordinate = userLocation.coordinate,
        compassBearing = compassBearing,
        active = chrome.driveFollow,
    )
    val activeDeliveryRoute = remember(routes, deliverySession.activeRouteId) {
        val id = deliverySession.activeRouteId ?: return@remember null
        routes.firstOrNull { it.route.id == id }
    }
    val activeQuickName = deliverySession.takeIf { it.kind == DriveKind.QUICK }?.quickName
    val hud by TripHudStore.snapshot.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        RouteMapBackdrop(
            stops = emptyList(),
            fitStops = false,
            userLocation = userFix,
            recenterToken = chrome.recenterToken,
            mapViewMode = chrome.mapViewMode,
            driveFollow = chrome.driveFollow,
            savedCamera = savedCamera,
            onCameraMoved = onCameraMoved,
            onMapLongClick = { pin ->
                pendingPlace = DriveHereTarget(
                    name = "",
                    latitude = pin.latitude,
                    longitude = pin.longitude,
                )
            },
        )

        // Absolute chrome: menus/overlays must not live in a height-wrapping Box
        // or they grow that box and shove the routes list / other islands down.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            val twoPane = AppPanes.isWide(maxWidth)
            Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FloatingIsland(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = AppPanes.TitleMaxWidth),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = 16.dp,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = IslandColors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = when {
                                userLocation.message != null -> userLocation.message.orEmpty()
                                userLocation.coordinate != null -> stringResource(R.string.home_centered)
                                userLocation.hasPermission -> stringResource(R.string.home_getting_gps)
                                else -> stringResource(R.string.home_allow_location)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = IslandColors.onSurfaceMuted,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                FloatingCircleButton(
                    onClick = {
                        chrome.dismissLayersMenu()
                        onOpenMenu()
                    },
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = stringResource(R.string.menu_open),
                        tint = IslandColors.onSurface,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                MapLayersButton(
                    onClick = chrome::openLayersMenu,
                )
                Spacer(modifier = Modifier.width(10.dp))
                FloatingCircleButton(
                    onClick = {
                        showAddressSearch = true
                    },
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.cd_search_place),
                        tint = IslandColors.onSurface,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                FloatingCircleButton(
                    onClick = {
                        if (!userLocation.hasPermission) {
                            userLocation.requestPermission()
                        } else {
                            userLocation.refresh()
                            chrome.bumpRecenter()
                        }
                    },
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = stringResource(R.string.cd_my_location),
                        tint = IslandColors.onSurface,
                    )
                }
            }

            MapAttributionChip(
                modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
            )
            if (showBackupReminder) {
                TextButton(onClick = onOpenSettings) {
                    Text(
                        text = stringResource(R.string.backup_reminder),
                        style = MaterialTheme.typography.bodySmall,
                        color = IslandColors.onSurfaceMuted,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .align(if (twoPane) Alignment.CenterEnd else Alignment.BottomCenter)
                        .then(
                            if (twoPane) {
                                Modifier
                                    .width(AppPanes.HomeRailWidth)
                                    .fillMaxHeight()
                            } else {
                                Modifier.fillMaxWidth()
                            },
                        ),
                ) {
            if (activeDeliveryRoute != null || activeQuickName != null) {
                val resumeName = activeDeliveryRoute?.route?.name
                    ?: activeQuickName.orEmpty()
                val resumeClick = {
                    val routeId = activeDeliveryRoute?.route?.id
                    if (routeId != null) onOpenRoute(routeId) else onOpenQuickDrive()
                }
                val nextTitle = when {
                    hud.title.isNotBlank() -> hud.title
                    else -> activeDeliveryRoute?.nextIncompleteStop?.name ?: resumeName
                }
                val nextMeta = listOfNotNull(
                    hud.distance.takeIf { it.isNotBlank() },
                    hud.progressLabel.takeIf { it.isNotBlank() },
                ).joinToString(" · ").ifBlank {
                    val next = activeDeliveryRoute?.nextIncompleteStop
                    val user = userFix
                    if (user != null && next?.latitude != null && next.longitude != null) {
                        GeoUtils.formatDistance(
                            GeoUtils.distanceMeters(
                                user.latitude,
                                user.longitude,
                                next.latitude!!,
                                next.longitude!!,
                            ),
                            distanceUnit,
                        )
                    } else {
                        ""
                    }
                }
                val tripPaused = deliverySession.paused
                FloatingIsland(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = 14.dp,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.home_park_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = IslandColors.onSurfaceMuted,
                        )
                        Text(
                            text = nextTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = IslandColors.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (nextMeta.isNotBlank()) {
                            Text(
                                text = nextMeta,
                                style = MaterialTheme.typography.bodyMedium,
                                color = IslandColors.onSurfaceMuted,
                            )
                        }
                        Text(
                            text = stringResource(R.string.home_resume_delivery_body, resumeName),
                            style = MaterialTheme.typography.bodySmall,
                            color = IslandColors.onSurfaceMuted,
                        )
                        if (tripPaused) {
                            Text(
                                text = stringResource(R.string.home_trip_paused),
                                style = MaterialTheme.typography.bodySmall,
                                color = IslandColors.onSurfaceMuted,
                            )
                        }
                        if (hud.tasksBlocked && !tripPaused) {
                            Text(
                                text = stringResource(R.string.trip_notification_tasks),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = {
                                    if (tripPaused) viewModel.resumeTrip()
                                    resumeClick()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(
                                        if (tripPaused) R.string.nav_resume else R.string.home_continue,
                                    ),
                                )
                            }
                            if (!tripPaused && hud.canMarkDone && !hud.allDone) {
                                FilledTonalButton(
                                    onClick = {
                                        TripGuidanceService.sendAction(
                                            context,
                                            TripGuidanceService.ACTION_MARK_DONE,
                                        )
                                    },
                                    enabled = !hud.tasksBlocked,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.nav_mark_done))
                                }
                            }
                        }
                    }
                }
            }

            AdaptiveRailIsland(
                wide = twoPane,
                maxExpandedHeight = 380.dp,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.home_routes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = IslandColors.onSurface,
                            )
                            Text(
                                text = if (routes.isEmpty()) {
                                    stringResource(R.string.home_empty_title)
                                } else {
                                    "${routes.size}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = IslandColors.onSurfaceMuted,
                            )
                        }
                        FloatingCircleButton(onClick = onCreateRoute, embedded = true) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.cd_create_route),
                            )
                        }
                    }

                    if (routes.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = listFilter == HomeRouteFilter.ALL,
                                onClick = { listFilter = HomeRouteFilter.ALL },
                                label = { Text(stringResource(R.string.home_filter_all)) },
                            )
                            FilterChip(
                                selected = listFilter == HomeRouteFilter.TODAY,
                                onClick = { listFilter = HomeRouteFilter.TODAY },
                                label = { Text(stringResource(R.string.home_filter_today)) },
                            )
                            FilterChip(
                                selected = listFilter == HomeRouteFilter.ARCHIVED,
                                onClick = { listFilter = HomeRouteFilter.ARCHIVED },
                                label = { Text(stringResource(R.string.home_filter_archived)) },
                            )
                            if (listFilter == HomeRouteFilter.TODAY) {
                                FilterChip(
                                    selected = shiftFilter == ShiftSlot.MORNING,
                                    onClick = {
                                        shiftFilter = if (shiftFilter == ShiftSlot.MORNING) {
                                            ShiftSlot.UNSET
                                        } else {
                                            ShiftSlot.MORNING
                                        }
                                    },
                                    label = { Text(stringResource(R.string.home_filter_morning)) },
                                )
                                FilterChip(
                                    selected = shiftFilter == ShiftSlot.AFTERNOON,
                                    onClick = {
                                        shiftFilter = if (shiftFilter == ShiftSlot.AFTERNOON) {
                                            ShiftSlot.UNSET
                                        } else {
                                            ShiftSlot.AFTERNOON
                                        }
                                    },
                                    label = { Text(stringResource(R.string.home_filter_afternoon)) },
                                )
                                if (dispatcherVanName.isNotBlank()) {
                                    FilterChip(
                                        selected = myVanOnly,
                                        onClick = { myVanOnly = !myVanOnly },
                                        label = { Text(stringResource(R.string.home_filter_my_van)) },
                                    )
                                }
                            }
                            if (homeLatitude != null && homeLongitude != null) {
                                FilterChip(
                                    selected = false,
                                    onClick = onDriveHome,
                                    label = {
                                        Text(
                                            homeName.ifBlank { stringResource(R.string.drive_home) },
                                        )
                                    },
                                )
                            }
                        }
                        SoftOutlinedTextField(
                            value = routeQuery,
                            onValueChange = { routeQuery = it },
                            label = stringResource(R.string.home_filter_routes),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    }

                    if (routes.isEmpty()) {
                        Text(
                            text = stringResource(R.string.home_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = IslandColors.onSurfaceMuted,
                            modifier = Modifier.padding(12.dp),
                        )
                    } else if (filteredRoutes.isEmpty()) {
                        Text(
                            text = stringResource(R.string.home_filter_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = IslandColors.onSurfaceMuted,
                            modifier = Modifier.padding(12.dp),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            daySections.forEach { section ->
                                if (listFilter == HomeRouteFilter.TODAY && daySections.size > 1) {
                                    item(key = "shift-${section.slot}") {
                                        Text(
                                            text = stringResource(
                                                when (section.slot) {
                                                    ShiftSlot.MORNING -> R.string.home_filter_morning
                                                    ShiftSlot.AFTERNOON -> R.string.home_filter_afternoon
                                                    else -> R.string.home_shift_unset
                                                },
                                            ),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = IslandColors.onSurfaceMuted,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                                        )
                                    }
                                }
                                items(section.routes, key = { it.route.id }) { route ->
                                RouteListRow(
                                    route = route,
                                    isActiveDelivery = route.route.id == deliverySession.activeRouteId,
                                    distanceUnit = distanceUnit,
                                    fromLatitude = userFix?.latitude,
                                    fromLongitude = userFix?.longitude,
                                    onClick = { onOpenRoute(route.route.id) },
                                    onShare = {
                                        ShareRoute.share(context, route, shareChooserTitle)
                                    },
                                    onDuplicate = {
                                        viewModel.duplicateRoute(route.route.id, copySuffix) { newId ->
                                            onOpenRoute(newId)
                                        }
                                    },
                                    onDelete = { pendingDelete = route },
                                    onStart = { onStartRoute(route.route.id) },
                                    onArchive = {
                                        viewModel.archiveRoute(
                                            route.route.id,
                                            archived = !route.route.archived,
                                        )
                                    },
                                    onOpenMaps = {
                                        val pins = route.orderedStops.filter {
                                            !it.isCompleted && it.latitude != null && it.longitude != null
                                        }
                                        if (pins.isNotEmpty()) {
                                            ExternalNavigation.openMultiStopDriving(
                                                context,
                                                pins.map { it.latitude!! to it.longitude!! },
                                                userFix?.let { it.latitude to it.longitude },
                                            )
                                        }
                                    },
                                )
                            }
                            }
                        }
                    }
                }
            }
            }
        }
        }

        MapLayersMenuHost(chrome = chrome)

        if (!onboardingDismissed) {
            FirstRunChecklistDialog(
                locationGranted = userLocation.hasPermission,
                hasRoute = routes.isNotEmpty(),
                onDismiss = onDismissOnboarding,
                onAllowLocation = { userLocation.requestPermission() },
            )
        }

        if (showAddressSearch) {
            AddressSearchDialog(
                onDismissRequest = { showAddressSearch = false },
                onPlaceSelected = { place ->
                    showAddressSearch = false
                    pendingPlace = DriveHereTarget(
                        name = place.shortName,
                        latitude = place.latitude,
                        longitude = place.longitude,
                        addressHint = place.displayName,
                    )
                },
                near = userFix,
            )
        }

        pendingPlace?.let { target ->
            PlaceActionDialog(
                target = target,
                onDismiss = { pendingPlace = null },
                onDriveHere = { name, saveToLibrary ->
                    pendingPlace = null
                    onDriveHere(target.copy(name = name), saveToLibrary)
                },
                onSaveToLibrary = { name ->
                    pendingPlace = null
                    viewModel.savePlaceToLibrary(
                        name = name,
                        latitude = target.latitude,
                        longitude = target.longitude,
                        addressHint = target.addressHint,
                    )
                },
                onAddToRoute = { name ->
                    pendingPlace = null
                    pickingRouteFor = target.copy(name = name)
                },
                onNewRoute = { name ->
                    pendingPlace = null
                    viewModel.createRouteFromPlace(
                        name = name,
                        latitude = target.latitude,
                        longitude = target.longitude,
                        addressHint = target.addressHint,
                        libraryStopId = target.libraryStopId,
                        roundTrip = defaultRoundTrip,
                    ) { routeId ->
                        onOpenRoute(routeId)
                    }
                },
            )
        }

        pickingRouteFor?.let { target ->
            RoutePickerDialog(
                routes = routes,
                onDismiss = { pickingRouteFor = null },
                onPick = { routeId ->
                    pickingRouteFor = null
                    viewModel.addPlaceToRoute(
                        routeId = routeId,
                        name = target.name,
                        latitude = target.latitude,
                        longitude = target.longitude,
                        addressHint = target.addressHint,
                        libraryStopId = target.libraryStopId,
                    ) { id ->
                        onOpenRoute(id)
                    }
                },
            )
        }

        pendingDelete?.let { route ->
            ConfirmDeleteRouteDialog(
                routeName = route.route.name,
                onDismiss = { pendingDelete = null },
                onConfirm = {
                    viewModel.deleteRoute(route.route.id)
                    pendingDelete = null
                },
            )
        }
    }
}
}

@Composable
private fun RouteListRow(
    route: RouteWithStops,
    isActiveDelivery: Boolean,
    distanceUnit: DistanceUnit,
    fromLatitude: Double?,
    fromLongitude: Double?,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit,
    onArchive: () -> Unit,
    onOpenMaps: () -> Unit,
) {
    val deliveryCount = route.deliveryStops.size
    val progress = if (deliveryCount == 0) 0f else route.completedCount.toFloat() / deliveryCount
    IslandListItem(onClick = onClick) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(48.dp)
                .background(
                    RouteColors.parse(
                        route.route.colorHex.ifBlank { RouteColors.DEFAULT_HEX },
                    ),
                    RoundedCornerShape(4.dp),
                ),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                route.route.name,
                style = MaterialTheme.typography.titleMedium,
                color = IslandColors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val vanShift = listOf(route.route.vanName, route.route.shiftName)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            if (vanShift.isNotBlank()) {
                Text(
                    text = vanShift,
                    style = MaterialTheme.typography.labelSmall,
                    color = IslandColors.badge,
                )
            }
            val remaining = route.remainingDeliveryStops
            val arrivals = RouteEta.estimateRemaining(
                remaining,
                fromLatitude,
                fromLongitude,
            )
            val etaMin = RouteEta.remainingWorkMinutes(
                remaining,
                fromLatitude,
                fromLongitude,
            )
            val lateCount = arrivals.count { it.late }
            Text(
                text = if (isActiveDelivery) {
                    stringResource(R.string.home_resume_delivery)
                } else {
                    buildString {
                        append(
                            pluralStringResource(
                                R.plurals.home_stops_summary,
                                route.deliveryStops.size,
                                route.deliveryStops.size,
                                route.completedCount,
                            ),
                        )
                        if (route.approxDistanceMeters > 0) {
                            append(" · ")
                            append(GeoUtils.formatDistance(route.approxDistanceMeters, distanceUnit))
                        }
                        if (etaMin > 0) {
                            append(" · ")
                            append(
                                stringResource(
                                    R.string.home_remaining_eta,
                                    GeoUtils.formatDurationMinutes(etaMin),
                                ),
                            )
                        }
                        if (route.route.roundTrip) {
                            append(" · ")
                            append(stringResource(R.string.route_round_trip_badge))
                        }
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActiveDelivery) {
                    MaterialTheme.colorScheme.primary
                } else {
                    IslandColors.onSurfaceMuted
                },
            )
            if (!isActiveDelivery && lateCount > 0) {
                Text(
                    text = pluralStringResource(R.plurals.home_late_stops, lateCount, lateCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (deliveryCount > 0) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = IslandColors.progress,
                    trackColor = IslandColors.fieldBorder.copy(alpha = 0.45f),
                )
            }
        }
        IconButton(onClick = onStart) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.cd_start_route),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        var moreOpen by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { moreOpen = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.cd_more_actions),
                    tint = IslandColors.onSurfaceMuted,
                )
            }
            DropdownMenu(
                expanded = moreOpen,
                onDismissRequest = { moreOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.cd_open_route_maps)) },
                    onClick = {
                        moreOpen = false
                        onOpenMaps()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Map, contentDescription = null)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.cd_duplicate_route)) },
                    onClick = {
                        moreOpen = false
                        onDuplicate()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.cd_share_route_row)) },
                    onClick = {
                        moreOpen = false
                        onShare()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Share, contentDescription = null)
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (route.route.archived) R.string.route_unarchive else R.string.route_archive,
                            ),
                        )
                    },
                    onClick = {
                        moreOpen = false
                        onArchive()
                    },
                    leadingIcon = {
                        Icon(
                            if (route.route.archived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = null,
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.cd_delete_route)) },
                    onClick = {
                        moreOpen = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    },
                )
            }
        }
    }
}

private enum class HomeRouteFilter { ALL, TODAY, ARCHIVED }

