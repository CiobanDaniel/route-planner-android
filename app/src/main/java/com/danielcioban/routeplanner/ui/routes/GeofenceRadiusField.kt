package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.settings.StopGeofence
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField

@Composable
fun GeofenceRadiusField(
    radiusMeters: Int?,
    onChange: (Int?) -> Unit,
    enabled: Boolean = true,
    resetKey: Any = Unit,
    optional: Boolean = true,
) {
    var text by remember(resetKey) {
        mutableStateOf(radiusMeters?.toString().orEmpty())
    }
    SoftOutlinedTextField(
        value = text,
        onValueChange = { value ->
            val digits = value.filter { it.isDigit() }.take(3)
            text = digits
            onChange(StopGeofence.parseRadius(digits))
        },
        label = stringResource(
            if (optional) R.string.geofence_radius_optional else R.string.geofence_radius,
        ),
        placeholder = stringResource(
            R.string.geofence_radius_range,
            StopGeofence.MIN_RADIUS_METERS,
            StopGeofence.MAX_RADIUS_METERS,
        ),
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
}
