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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.location.rememberUserLocation
import com.danielcioban.routeplanner.ui.map.MapLayersButton
import com.danielcioban.routeplanner.ui.map.MapViewMode
import com.danielcioban.routeplanner.ui.map.RouteMapBackdrop
import com.danielcioban.routeplanner.ui.menu.AppMenuPanel
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.ShareRoute

@Composable
fun RouteListScreen(
    viewModel: RouteListViewModel,
    onCreateRoute: () -> Unit,
    onOpenRoute: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    val deliverySession by viewModel.deliverySession.collectAsStateWithLifecycle()
    var recenterToken by remember { mutableIntStateOf(0) }
    var mapViewMode by remember { mutableStateOf(MapViewMode.MAP) }
    var driveFollow by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val shareChooserTitle = stringResource(R.string.share_route_chooser)
    val userLocation = rememberUserLocation(
        autoRequest = true,
        highFrequency = driveFollow,
    )
    val activeDeliveryRoute = remember(routes, deliverySession.activeRouteId) {
        val id = deliverySession.activeRouteId ?: return@remember null
        routes.firstOrNull { it.route.id == id }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RouteMapBackdrop(
            stops = emptyList(),
            fitStops = false,
            userLocation = userLocation.coordinate,
            recenterToken = recenterToken,
            mapViewMode = mapViewMode,
            driveFollow = driveFollow,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
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
                    FloatingCircleButton(onClick = { menuOpen = !menuOpen }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = stringResource(R.string.menu_open),
                            tint = IslandColors.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
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
                        driveFollow = driveFollow,
                        onDriveFollowChange = { enabled ->
                            driveFollow = enabled
                            if (enabled) {
                                mapViewMode = MapViewMode.DRIVING
                                recenterToken++
                            }
                        },
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    FloatingCircleButton(
                        onClick = {
                            if (!userLocation.hasPermission) {
                                userLocation.requestPermission()
                            } else {
                                userLocation.refresh()
                                recenterToken++
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

                if (menuOpen) {
                    AppMenuPanel(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 64.dp, end = 56.dp),
                        onSettings = {
                            menuOpen = false
                            onOpenSettings()
                        },
                        onAccount = { menuOpen = false },
                        onProfile = { menuOpen = false },
                        onLogin = { menuOpen = false },
                        onLogout = { menuOpen = false },
                    )
                }
            }

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

            FloatingIsland(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 340.dp),
                shape = RoundedCornerShape(28.dp),
                contentPadding = 12.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                        FloatingCircleButton(onClick = onCreateRoute) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.cd_create_route),
                            )
                        }
                    }

                    if (routes.isEmpty()) {
                        Text(
                            text = stringResource(R.string.home_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = IslandColors.onSurfaceMuted,
                            modifier = Modifier.padding(12.dp),
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(routes, key = { it.route.id }) { route ->
                                RouteListRow(
                                    route = route,
                                    isActiveDelivery = route.route.id == deliverySession.activeRouteId,
                                    onClick = { onOpenRoute(route.route.id) },
                                    onShare = {
                                        ShareRoute.share(context, route, shareChooserTitle)
                                    },
                                    onDelete = { viewModel.deleteRoute(route.route.id) },
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteListRow(
    route: RouteWithStops,
    isActiveDelivery: Boolean,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                route.route.name,
                style = MaterialTheme.typography.titleMedium,
                color = IslandColors.onSurface,
            )
            Text(
                text = if (isActiveDelivery) {
                    stringResource(R.string.home_resume_delivery)
                } else {
                    stringResource(
                        R.string.home_stops_summary,
                        route.stops.size,
                        route.completedCount,
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActiveDelivery) {
                    MaterialTheme.colorScheme.primary
                } else {
                    IslandColors.onSurfaceMuted
                },
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
