package com.danielcioban.routeplanner.ui.location

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.danielcioban.routeplanner.ui.map.LatLng
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch

data class UserLocationUi(
    val coordinate: LatLng? = null,
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null,
    val requestPermission: () -> Unit = {},
    val refresh: () -> Unit = {},
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberUserLocation(
    autoRequest: Boolean = true,
    /** Faster GPS + bearing updates — use while navigating / Follow me. */
    highFrequency: Boolean = false,
): UserLocationUi {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    var coordinate by remember { mutableStateOf<LatLng?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun applyLocation(location: android.location.Location) {
        val previousBearing = coordinate?.bearingDegrees
        coordinate = LatLng(
            latitude = location.latitude,
            longitude = location.longitude,
            bearingDegrees = location.toBearingOrNull() ?: previousBearing,
        )
        message = null
    }

    fun refresh() {
        scope.launch {
            if (!permission.status.isGranted && !context.hasLocationPermission()) {
                message = "Location permission needed to center on you"
                return@launch
            }
            isLoading = true
            val loc = context.currentLocationOrNull()
            isLoading = false
            if (loc != null) {
                applyLocation(loc)
            } else {
                message = "Waiting for GPS… try outdoors or check location is on"
            }
        }
    }

    LaunchedEffect(permission.status.isGranted) {
        if (permission.status.isGranted || context.hasLocationPermission()) {
            refresh()
        } else if (autoRequest && !permission.status.shouldShowRationale) {
            permission.launchPermissionRequest()
        }
    }

    DisposableEffect(permission.status.isGranted, highFrequency) {
        if (!permission.status.isGranted && !context.hasLocationPermission()) {
            return@DisposableEffect onDispose { }
        }
        val stop = context.requestLocationUpdates(highFrequency) { applyLocation(it) }
        onDispose { stop?.invoke() }
    }

    return UserLocationUi(
        coordinate = coordinate,
        hasPermission = permission.status.isGranted || context.hasLocationPermission(),
        isLoading = isLoading,
        message = message,
        requestPermission = { permission.launchPermissionRequest() },
        refresh = { refresh() },
    )
}
