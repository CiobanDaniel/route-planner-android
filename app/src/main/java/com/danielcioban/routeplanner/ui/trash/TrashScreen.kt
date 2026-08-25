package com.danielcioban.routeplanner.ui.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.components.IslandListItem
import com.danielcioban.routeplanner.ui.layout.AdaptiveSheetSlot
import com.danielcioban.routeplanner.ui.layout.AppPanes
import com.danielcioban.routeplanner.ui.layout.chromeIslandWidth
import com.danielcioban.routeplanner.ui.map.MapViewMode
import com.danielcioban.routeplanner.ui.map.RouteMapBackdrop
import com.danielcioban.routeplanner.ui.menu.ScreenMenuButton
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun TrashScreen(
    viewModel: TrashViewModel,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    val library by viewModel.library.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        RouteMapBackdrop(
            stops = emptyList(),
            mapViewMode = MapViewMode.MAP,
            showStraightStopLinks = false,
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
            FloatingIsland(
                modifier = Modifier.chromeIslandWidth(),
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
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.trash_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = IslandColors.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.trash_blurb),
                            style = MaterialTheme.typography.bodySmall,
                            color = IslandColors.onSurfaceMuted,
                        )
                    }
                    ScreenMenuButton(onClick = onOpenMenu)
                }
            }
            AdaptiveSheetSlot(
                wide = wide,
                maxExpandedHeight = 480.dp,
                fillHeight = routes.isNotEmpty() || library.isNotEmpty(),
            ) {
                if (routes.isEmpty() && library.isEmpty()) {
                    Text(
                        text = stringResource(R.string.trash_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = IslandColors.onSurface,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (routes.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.trash_routes),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IslandColors.onSurfaceMuted,
                                )
                            }
                            items(routes, key = { "r-${it.route.id}" }) { route ->
                                TrashRow(
                                    name = route.route.name,
                                    onRestore = { viewModel.restoreRoute(route.route.id) },
                                    onPurge = { viewModel.purgeRoute(route.route.id) },
                                )
                            }
                        }
                        if (library.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.trash_library),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IslandColors.onSurfaceMuted,
                                )
                            }
                            items(library, key = { "l-${it.id}" }) { stop ->
                                TrashRow(
                                    name = stop.name,
                                    onRestore = { viewModel.restoreLibrary(stop.id) },
                                    onPurge = { viewModel.purgeLibrary(stop.id) },
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

@Composable
private fun TrashRow(
    name: String,
    onRestore: () -> Unit,
    onPurge: () -> Unit,
) {
    IslandListItem {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = IslandColors.onSurface,
            )
        }
        TextButton(onClick = onRestore) {
            Text(stringResource(R.string.trash_restore))
        }
        TextButton(onClick = onPurge) {
            Text(
                stringResource(R.string.trash_purge),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}