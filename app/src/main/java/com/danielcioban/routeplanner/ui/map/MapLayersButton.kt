package com.danielcioban.routeplanner.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun MapLayersButton(
    selected: MapViewMode,
    onSelected: (MapViewMode) -> Unit,
    driveFollow: Boolean = false,
    onDriveFollowChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        if (expanded) {
            FloatingIsland(
                modifier = Modifier
                    .width(240.dp)
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(22.dp),
                contentPadding = 10.dp,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Map type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = IslandColors.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                    MapViewMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelected(mode)
                                    if (mode == MapViewMode.DRIVING) {
                                        onDriveFollowChange?.invoke(true)
                                    } else {
                                        onDriveFollowChange?.invoke(false)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = IslandColors.onSurface,
                                )
                                Text(
                                    text = mode.description,
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
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Navigation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Follow me",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = IslandColors.onSurface,
                                )
                                Text(
                                    text = "Heading-up camera while you move",
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
                }
            }
        }

        FloatingCircleButton(onClick = { expanded = !expanded }) {
            Icon(
                Icons.Default.Layers,
                contentDescription = "Map layers",
                tint = IslandColors.onSurface,
            )
        }
    }
}
