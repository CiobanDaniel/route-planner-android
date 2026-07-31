package com.danielcioban.routeplanner.ui.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

fun Context.hasLocationPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
suspend fun Context.currentLocationOrNull(): Location? {
    if (!hasLocationPermission()) return null
    val client = LocationServices.getFusedLocationProviderClient(this)
    return suspendCancellableCoroutine { cont ->
        val cts = CancellationTokenSource()
        cont.invokeOnCancellation { cts.cancel() }
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    cont.resume(location)
                } else {
                    client.lastLocation
                        .addOnSuccessListener { last -> cont.resume(last) }
                        .addOnFailureListener { cont.resume(null) }
                }
            }
            .addOnFailureListener { cont.resume(null) }
    }
}

/** @deprecated Prefer [currentLocationOrNull] for bearing. */
@SuppressLint("MissingPermission")
suspend fun Context.currentLatLngOrNull(): Pair<Double, Double>? {
    val loc = currentLocationOrNull() ?: return null
    return loc.latitude to loc.longitude
}

fun Location.toBearingOrNull(): Float? {
    if (!hasBearing() || bearing < 0f) return null
    // Ignore tiny/noisy bearings when nearly stationary.
    if (hasSpeed() && speed < 0.8f) return null
    return bearing
}

@SuppressLint("MissingPermission")
fun Context.requestLocationUpdates(
    highFrequency: Boolean,
    onLocation: (Location) -> Unit,
): (() -> Unit)? {
    if (!hasLocationPermission()) return null
    val client = LocationServices.getFusedLocationProviderClient(this)
    val intervalMs = if (highFrequency) 1_500L else 8_000L
    val request = LocationRequest.Builder(
        if (highFrequency) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
        intervalMs,
    )
        .setMinUpdateIntervalMillis(if (highFrequency) 800L else 4_000L)
        .setMinUpdateDistanceMeters(if (highFrequency) 3f else 12f)
        .build()

    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(onLocation)
        }
    }
    client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    return { client.removeLocationUpdates(callback) }
}
