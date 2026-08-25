package com.danielcioban.routeplanner.ui.dev

import android.location.Location
import com.danielcioban.routeplanner.ui.map.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Release stub — developer GPS sim must not ship. */
object DevLocationSim {
    const val PROVIDER_ID = "developer"

    val enabled: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    val fix: StateFlow<LatLng?> = MutableStateFlow(null).asStateFlow()
    val speed: StateFlow<DevSimSpeed> = MutableStateFlow(DevSimSpeed.CITY).asStateFlow()

    fun isActive(): Boolean = false
    fun ensureStarted() = Unit
    fun disable() = Unit
    fun setStick(x: Float, y: Float) = Unit
    fun setSpeed(preset: DevSimSpeed) = Unit
    fun teleport(latitude: Double, longitude: Double, bearingDegrees: Float? = null) = Unit
    fun addListener(onLocation: (Location) -> Unit): () -> Unit = {}
    fun snapshotLocation(): Location? = null
    fun tick(dtSeconds: Float) = Unit
}
