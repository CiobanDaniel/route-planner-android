package com.danielcioban.routeplanner.ui.routes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.data.settings.DistanceUnit
import com.danielcioban.routeplanner.ui.components.CollapsibleBottomIsland
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.components.IslandListItem
import com.danielcioban.routeplanner.ui.components.MapAttributionChip
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.location.rememberDeviceBearing
import com.danielcioban.routeplanner.ui.location.rememberMergedUserFix
import com.danielcioban.routeplanner.ui.location.rememberUserLocation
import com.danielcioban.routeplanner.ui.map.MapLayersButton
import com.danielcioban.routeplanner.ui.map.MapLayersMenuHost
import com.danielcioban.routeplanner.ui.map.MapViewMode
import com.danielcioban.routeplanner.ui.map.RouteMapBackdrop
import com.danielcioban.routeplanner.ui.map.rememberMapChromeState
import com.danielcioban.routeplanner.ui.menu.AppMenuPanel
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.GeoUtils
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
    preferredMapStyle: MapViewMode = MapViewMode.MAP,
    onPreferredMapStyleChange: (MapViewMode) -> Unit = {},
    distanceUnit: DistanceUnit = DistanceUnit.METRIC,
) {
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    val deliverySession by viewModel.deliverySession.collectAsStateWithLifecycle()
    val accountSession by viewModel.accountSession.collectAsStateWithLifecycle()
    val chrome = rememberMapChromeState(
        preferredBrowseMode = preferredMapStyle,
        onPreferredModeChange = onPreferredMapStyleChange,
    )
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.share_route_chooser)
    val copySuffix = stringResource(R.string.route_copy_suffix)
    var routeQuery by remember { mutableStateOf("") }
    val filteredRoutes = remember(routes, routeQuery) {
        routes.filter { it.matchesQuery(routeQuery) }
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

    BackHandler(enabled = menuOpen) { menuOpen = false }

    Box(modifier = Modifier.fillMaxSize()) {
        RouteMapBackdrop(
            stops = emptyList(),
            fitStops = false,
            userLocation = userFix,
            recenterToken = chrome.recenterToken,
            mapViewMode = chrome.mapViewMode,
            driveFollow = chrome.driveFollow,
        )

        // Absolute chrome: menus/overlays must not live in a height-wrapping Box
        // or they grow that box and shove the routes list / other islands down.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FloatingIsland(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = 16.dp,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = IslandColors.onSurface,
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
                        menuOpen = !menuOpen
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
                    onClick = {
                        menuOpen = false
                        chrome.openLayersMenu()
                    },
                )
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
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            if (activeDeliveryRoute != null) {
                FloatingIsland(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = 14.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.home_resume_delivery),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(
                                    R.string.home_resume_delivery_body,
                                    activeDeliveryRoute.route.name,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = IslandColors.onSurfaceMuted,
                            )
                        }
                        Button(
                            onClick = { onOpenRoute(activeDeliveryRoute.route.id) },
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_start))
                        }
                    }
                }
            }

            CollapsibleBottomIsland(
                modifier = Modifier.fillMaxWidth(),
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
                            items(filteredRoutes, key = { it.route.id }) { route ->
                                RouteListRow(
                                    route = route,
                                    isActiveDelivery = route.route.id == deliverySession.activeRouteId,
                                    distanceUnit = distanceUnit,
                                    onClick = { onOpenRoute(route.route.id) },
                                    onShare = {
                                        ShareRoute.share(context, route, shareChooserTitle)
                                    },
                                    onDuplicate = {
                                        viewModel.duplicateRoute(route.route.id, copySuffix) { newId ->
                                            onOpenRoute(newId)
                                        }
                                    },
                                    onDelete = { viewModel.deleteRoute(route.route.id) },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (menuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(IslandColors.scrim.copy(alpha = 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { menuOpen = false },
                    ),
            ) {
                AppMenuPanel(
                    accountSession = accountSession,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 72.dp, end = 16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                    onSettings = {
                        menuOpen = false
                        onOpenSettings()
                    },
                    onAbout = {
                        menuOpen = false
                        onOpenAbout()
                    },
                    onStopLibrary = {
                        menuOpen = false
                        onOpenStopLibrary()
                    },
                    onAccount = {
                        menuOpen = false
                        onOpenAccount()
                    },
                    onProfile = {
                        menuOpen = false
                        onOpenAccount()
                    },
                    onLogin = {
                        menuOpen = false
                        onOpenAccount()
                    },
                    onLogout = {
                        menuOpen = false
                        viewModel.signOut()
                    },
                )
            }
        }

        MapLayersMenuHost(chrome = chrome)
    }
}

@Composable
private fun RouteListRow(
    route: RouteWithStops,
    isActiveDelivery: Boolean,
    distanceUnit: DistanceUnit,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val progress = if (route.stops.isEmpty()) 0f else route.completedCount.toFloat() / route.stops.size
    IslandListItem(onClick = onClick) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                route.route.name,
                style = MaterialTheme.typography.titleMedium,
                color = IslandColors.onSurface,
            )
            Text(
                text = if (isActiveDelivery) {
                    stringResource(R.string.home_resume_delivery)
                } else {
                    buildString {
                        append(
                            pluralStringResource(
                                R.plurals.home_stops_summary,
                                route.stops.size,
                                route.stops.size,
                                route.completedCount,
                            ),
                        )
                        if (route.approxDistanceMeters > 0) {
                            append(" · ")
                            append(GeoUtils.formatDistance(route.approxDistanceMeters, distanceUnit))
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
            if (route.stops.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = IslandColors.fieldBorder.copy(alpha = 0.35f),
                )
            }
        }
        IconButton(onClick = onDuplicate) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.cd_duplicate_route),
                tint = IslandColors.onSurfaceMuted,
            )
        }
        IconButton(onClick = onShare) {
            Icon(
                Icons.Default.Share,
                contentDescription = stringResource(R.string.cd_share_route_row),
                tint = IslandColors.onSurfaceMuted,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.cd_delete_route),
                tint = IslandColors.onSurfaceMuted,
            )
        }
    }
}
