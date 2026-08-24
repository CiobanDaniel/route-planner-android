package com.danielcioban.routeplanner.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun LibraryEditScopeDialog(
    stopName: String,
    routeNames: List<String>,
    showThisRoute: Boolean,
    onEverywhere: () -> Unit,
    onThisRoute: () -> Unit,
    onSaveAsCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.library_edit_scope_title),
        confirmLabel = null,
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(
            text = stringResource(
                R.string.library_edit_scope_body,
                stopName,
                formatRouteNames(routeNames),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        Column(
            modifier = Modifier.align(Alignment.End),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = onEverywhere) {
                Text(stringResource(R.string.library_edit_everywhere))
            }
            if (showThisRoute) {
                TextButton(onClick = onThisRoute) {
                    Text(stringResource(R.string.library_edit_this_route))
                }
            } else {
                TextButton(onClick = onSaveAsCopy) {
                    Text(stringResource(R.string.library_edit_as_copy))
                }
            }
        }
    }
}

@Composable
fun LibraryDeleteScopeDialog(
    stopName: String,
    routeNames: List<String>,
    showThisRoute: Boolean,
    onEverywhere: () -> Unit,
    onThisRoute: () -> Unit,
    onDismiss: () -> Unit,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.library_delete_scope_title),
        confirmLabel = null,
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(
            text = if (routeNames.isEmpty()) {
                stringResource(R.string.library_delete_unused_body, stopName)
            } else {
                stringResource(
                    R.string.library_delete_everywhere_body,
                    stopName,
                    formatRouteNames(routeNames),
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        Column(
            modifier = Modifier.align(Alignment.End),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (showThisRoute) {
                TextButton(onClick = onThisRoute) {
                    Text(stringResource(R.string.library_delete_this_route))
                }
            }
            TextButton(onClick = onEverywhere) {
                Text(
                    stringResource(R.string.library_delete_everywhere),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

fun formatRouteNames(names: List<String>): String {
    if (names.isEmpty()) return ""
    return names.joinToString(", ")
}
