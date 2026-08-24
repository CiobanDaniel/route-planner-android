package com.danielcioban.routeplanner.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.components.IslandListItem
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun StopLibraryPickerDialog(
    stops: List<StopLibraryEntity>,
    onDismiss: () -> Unit,
    onPick: (StopLibraryEntity) -> Unit,
    onManageLibrary: (() -> Unit)? = null,
    alreadyOnRouteLibraryIds: Set<Long> = emptySet(),
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.library_pick_title),
        confirmLabel = stringResource(R.string.action_close),
        onConfirm = onDismiss,
        dismissLabel = null,
    ) {
        Text(
            text = stringResource(R.string.library_pick_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        if (stops.isEmpty()) {
            Text(
                text = stringResource(R.string.library_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = IslandColors.onSurface,
            )
        } else {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                stops.forEach { stop ->
                    val alreadyOnRoute = stop.id in alreadyOnRouteLibraryIds
                    IslandListItem(onClick = {
                        onPick(stop)
                        onDismiss()
                    }) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stop.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (stop.addressHint.isNotBlank()) {
                                Text(
                                    text = stop.addressHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IslandColors.onSurfaceMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (alreadyOnRoute) {
                                Text(
                                    text = stringResource(R.string.library_already_linked_short),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        TextButton(onClick = {
                            onPick(stop)
                            onDismiss()
                        }) {
                            Text(
                                stringResource(
                                    if (alreadyOnRoute) {
                                        R.string.library_open_on_route
                                    } else {
                                        R.string.library_add_to_route
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        }
        if (onManageLibrary != null) {
            TextButton(
                onClick = {
                    onDismiss()
                    onManageLibrary()
                },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.library_manage))
            }
        }
    }
}
