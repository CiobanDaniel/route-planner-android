package com.danielcioban.routeplanner.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.components.IslandListDivider
import com.danielcioban.routeplanner.ui.components.IslandListItem
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun MapLayersButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingCircleButton(onClick = onClick, modifier = modifier) {
        Icon(
            Icons.Default.Layers,
            contentDescription = stringResource(R.string.cd_map_layers),
            tint = IslandColors.onSurface,
        )
    }
}

/**
 * Full-window map-type / follow-me menu. Call from a [fillMaxSize] root [Box]
 * (or anywhere that is not a height-wrapping parent) so it cannot reflow siblings.
 */
@Composable
fun MapLayersMenuDialog(
    selected: MapViewMode,
    onSelected: (MapViewMode) -> Unit,
    driveFollow: Boolean = false,
    onDriveFollowChange: ((Boolean) -> Unit)? = null,
    northUp: Boolean = false,
    onNorthUpChange: ((Boolean) -> Unit)? = null,
    dataSaver: Boolean = false,
    onReloadMap: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("map_layers_menu")
                .background(IslandColors.scrim.copy(alpha = 0.22f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        ) {
            FloatingIsland(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(260.dp)
                    .heightIn(max = 560.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
                shape = RoundedCornerShape(22.dp),
                contentPadding = 10.dp,
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.map_type_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = IslandColors.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                    MapViewMode.entries
                        .filter { !(dataSaver && it == MapViewMode.SATELLITE) }
                        .forEach { mode ->
                        IslandListItem(
                            selected = mode == selected,
                            onClick = {
                                onSelected(mode)
                                if (mode == MapViewMode.DRIVING) {
                                    onDriveFollowChange?.invoke(true)
                                } else {
                                    onDriveFollowChange?.invoke(false)
                                }
                                onDismiss()
                            },
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(mode.labelRes),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = IslandColors.onSurface,
                                )
                                Text(
                                    text = stringResource(mode.descriptionRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IslandColors.onSurfaceMuted,
                                )
                            }
                            if (mode == selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    if (onDriveFollowChange != null) {
                        IslandListDivider()
                        IslandListItem {
                            Icon(
                                Icons.Default.Navigation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.map_follow_me),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = IslandColors.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.map_follow_me_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IslandColors.onSurfaceMuted,
                                )
                            }
                            Switch(
                                checked = driveFollow,
                                onCheckedChange = {
                                    onDriveFollowChange(it)
                                    if (it) onSelected(MapViewMode.DRIVING)
                                },
                            )
                        }
                    }
                    if (onNorthUpChange != null) {
                        IslandListItem {
                            Icon(
                                Icons.Default.Explore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.map_north_up),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = IslandColors.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.map_north_up_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IslandColors.onSurfaceMuted,
                                )
                            }
                            Switch(
                                checked = northUp,
                                onCheckedChange = onNorthUpChange,
                            )
                        }
                    }
                    if (onReloadMap != null) {
                        IslandListDivider()
                        IslandListItem(
                            onClick = {
                                onReloadMap()
                                onDismiss()
                            },
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.map_reload_assets),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = IslandColors.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.map_reload_assets_desc),
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
}
