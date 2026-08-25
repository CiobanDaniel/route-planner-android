package com.danielcioban.routeplanner.ui.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun FgsLocationExplainerDialog(
    onContinue: () -> Unit,
) {
    IslandDialog(
        onDismissRequest = onContinue,
        title = stringResource(R.string.fgs_location_title),
        confirmLabel = stringResource(R.string.fgs_location_confirm),
        onConfirm = onContinue,
    ) {
        Text(
            text = stringResource(R.string.fgs_location_body),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
    }
}
