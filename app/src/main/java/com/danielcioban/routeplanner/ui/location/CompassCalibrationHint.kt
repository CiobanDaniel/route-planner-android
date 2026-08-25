package com.danielcioban.routeplanner.ui.location

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun CompassCalibrationHint(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingIsland(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        contentPadding = 4.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.compass_calibrate_hint),
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, top = 8.dp, bottom = 8.dp),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = IslandColors.onSurfaceMuted,
                )
            }
        }
    }
}
