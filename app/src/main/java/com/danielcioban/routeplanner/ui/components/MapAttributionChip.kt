package com.danielcioban.routeplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.ui.theme.IslandColors

/** Compact OSM / tile credit for map screens (Compose overlay). */
@Composable
fun MapAttributionChip(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.map_attribution),
        style = MaterialTheme.typography.labelSmall,
        color = IslandColors.onSurfaceMuted,
        modifier = modifier
            .background(
                color = IslandColors.surface.copy(alpha = 0.88f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
