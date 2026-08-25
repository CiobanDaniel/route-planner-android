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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.ui.dev.DevLocationSim
import com.danielcioban.routeplanner.ui.map.LastKnownMapCenter
import com.danielcioban.routeplanner.ui.map.LatLng
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch

/** Accuracy worse than this (meters) triggers a soft poor-GPS hint. */
const val PoorGpsAccuracyMeters = 75f

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

    val simOn by DevLocationSim.enabled.collectAsStateWithLifecycle()
    val simFix by DevLocationSim.fix.collectAsStateWithLifecycle()

    var coordinate by remember {
        mutableStateOf(LastKnownMapCenter.coordinate)
    }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun applyLocation(location: android.location.Location) {
        val previousBearing = coordinate?.bearingDegrees
        val freshCourse = if (DevLocationSim.isActive() && location.hasBearing()) {
            location.bearing
        } else {
            location.toBearingOrNull()
        }
        val accuracy = location.accuracy.takeIf { location.hasAccuracy() }
        val next = LatLng(
            latitude = location.latitude,
            longitude = location.longitude,
            bearingDegrees = freshCourse ?: previousBearing,
            speedMps = location.speed.takeIf { location.hasSpeed() && it >= 0f },
            accuracyMeters = accuracy,
        )
        coordinate = next
        LastKnownMapCenter.update(next)
        message = when {
            DevLocationSim.isActive() -> null
            accuracy != null && accuracy > PoorGpsAccuracyMeters ->
                context.getString(R.string.msg_poor_gps)
            else -> null
        }
    }

    fun refresh() {
        scope.launch {
            if (DevLocationSim.isActive()) {
                DevLocationSim.fix.value?.let { coordinate = it }
                isLoading = false
                message = null
                return@launch
            }
            if (!permission.status.isGranted && !context.hasLocationPermission()) {
                message = context.getString(R.string.msg_location_permission_needed)
                return@launch
            }
            isLoading = true
            // Cached fix first so the map can leave the default world view immediately.
            context.lastKnownLocationOrNull()?.let(::applyLocation)
            val loc = context.currentLocationOrNull()
            isLoading = false
            if (loc != null) {
                applyLocation(loc)
            } else if (coordinate == null) {
                message = context.getString(R.string.msg_waiting_gps_hint)
            }
        }
    }

    LaunchedEffect(simOn, simFix) {
        if (simOn && simFix != null) {
            coordinate = simFix
            message = null
            isLoading = false
        }
    }

    LaunchedEffect(permission.status.isGranted, simOn) {
        if (simOn || permission.status.isGranted || context.hasLocationPermission()) {
            refresh()
        } else if (autoRequest && !permission.status.shouldShowRationale) {
            permission.launchPermissionRequest()
        }
    }

    DisposableEffect(permission.status.isGranted, highFrequency, simOn) {
        if (!simOn && !permission.status.isGranted && !context.hasLocationPermission()) {
            return@DisposableEffect onDispose { }
        }
        val stop = context.requestLocationUpdates(highFrequency) { applyLocation(it) }
        onDispose { stop?.invoke() }
    }

    return UserLocationUi(
        coordinate = if (simOn) simFix ?: coordinate else coordinate,
        hasPermission = permission.status.isGranted || context.hasLocationPermission() || simOn,
        isLoading = isLoading,
        message = message,
        requestPermission = { permission.launchPermissionRequest() },
        refresh = { refresh() },
    )
}
