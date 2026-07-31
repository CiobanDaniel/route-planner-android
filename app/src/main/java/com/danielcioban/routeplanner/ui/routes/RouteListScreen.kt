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

@Composable
fun RouteListScreen(
    viewModel: RouteListViewModel,
    onCreateRoute: () -> Unit,
    onOpenRoute: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    var recenterToken by remember { mutableIntStateOf(0) }
    var mapViewMode by remember { mutableStateOf(MapViewMode.MAP) }
    var driveFollow by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val userLocation = rememberUserLocation(
        autoRequest = true,
        highFrequency = driveFollow,
    )

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
                            contentDescription = "My location",
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
                            Icon(Icons.Default.Add, contentDescription = "Create route")
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
                                    onClick = { onOpenRoute(route.route.id) },
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
    onClick: () -> Unit,
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
                text = "${route.stops.size} stops · ${route.completedCount} done",
                style = MaterialTheme.typography.bodyMedium,
                color = IslandColors.onSurfaceMuted,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete route",
                tint = IslandColors.onSurfaceMuted,
            )
        }
    }
}
