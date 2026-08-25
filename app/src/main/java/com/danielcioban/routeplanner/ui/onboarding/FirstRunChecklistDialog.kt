package com.danielcioban.routeplanner.ui.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun FirstRunChecklistDialog(
    locationGranted: Boolean,
    hasRoute: Boolean,
    onDismiss: () -> Unit,
    onAllowLocation: () -> Unit,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.onboarding_title),
        confirmLabel = stringResource(R.string.onboarding_dismiss),
        onConfirm = onDismiss,
        dismissLabel = if (!locationGranted) {
            stringResource(R.string.home_allow_location)
        } else {
            null
        },
        onDismissButton = if (!locationGranted) onAllowLocation else null,
    ) {
        Text(
            text = stringResource(R.string.onboarding_body),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        Text(
            text = if (locationGranted) {
                "1. ${stringResource(R.string.onboarding_location_done)}"
            } else {
                "1. ${stringResource(R.string.onboarding_location)}"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = IslandColors.onSurface,
        )
        Text(
            text = if (hasRoute) {
                "2. ${stringResource(R.string.onboarding_route_done)}"
            } else {
                "2. ${stringResource(R.string.onboarding_stop)}"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = IslandColors.onSurface,
        )
        Text(
            text = "3. ${stringResource(R.string.onboarding_start)}",
            style = MaterialTheme.typography.bodyLarge,
            color = IslandColors.onSurface,
        )
    }
}
