package com.danielcioban.routeplanner.ui.dev

import android.location.Location
import android.os.SystemClock
import com.danielcioban.routeplanner.ui.map.LastKnownMapCenter
import com.danielcioban.routeplanner.ui.map.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot

/**
 * Debug-only GPS simulation. Release source set is a no-op so this never ships.
 */
object DevLocationSim {
    const val PROVIDER_ID = "developer"

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _fix = MutableStateFlow<LatLng?>(null)
    val fix: StateFlow<LatLng?> = _fix.asStateFlow()

    private val _speed = MutableStateFlow(DevSimSpeed.CITY)
    val speed: StateFlow<DevSimSpeed> = _speed.asStateFlow()

    @Volatile
    private var stickX = 0f

    @Volatile
    private var stickY = 0f

    private val listeners = CopyOnWriteArrayList<(Location) -> Unit>()

    fun isActive(): Boolean = _enabled.value

    fun ensureStarted() {
        if (_fix.value == null) {
            val seed = LastKnownMapCenter.coordinate
            if (seed != null) {
                _fix.value = seed.copy(accuracyMeters = 4f)
            }
        }
        _enabled.value = true
        publish()
    }

    fun disable() {
        _enabled.value = false
        stickX = 0f
        stickY = 0f
    }

    fun setStick(x: Float, y: Float) {
        stickX = x.coerceIn(-1f, 1f)
        stickY = y.coerceIn(-1f, 1f)
    }

    fun setSpeed(preset: DevSimSpeed) {
        _speed.value = preset
    }

    fun teleport(latitude: Double, longitude: Double, bearingDegrees: Float? = _fix.value?.bearingDegrees) {
        val next = LatLng(
            latitude = latitude.coerceIn(-85.0, 85.0),
            longitude = wrapLng(longitude),
            bearingDegrees = bearingDegrees,
            speedMps = 0f,
            accuracyMeters = 4f,
        )
        _fix.value = next
        LastKnownMapCenter.update(next)
        publish()
    }

    fun addListener(onLocation: (Location) -> Unit): () -> Unit {
        listeners.add(onLocation)
        currentAndroidLocation()?.let(onLocation)
        return { listeners.remove(onLocation) }
    }

    fun snapshotLocation(): Location? = currentAndroidLocation()

    fun tick(dtSeconds: Float) {
        if (!_enabled.value) return
        val mag = hypot(stickX.toDouble(), stickY.toDouble()).toFloat()
        if (mag < 0.08f) return
        val from = _fix.value ?: return
        val nx = stickX / mag
        val ny = stickY / mag
        val meters = _speed.value.metersPerSecond * dtSeconds * mag
        val east = nx * meters
        val north = ny * meters
        val (lat, lng) = offsetMeters(from.latitude, from.longitude, north.toDouble(), east.toDouble())
        val heading = Math.toDegrees(atan2(east.toDouble(), north.toDouble())).toFloat().let {
            ((it % 360f) + 360f) % 360f
        }
        val next = LatLng(
            latitude = lat,
            longitude = lng,
            bearingDegrees = heading,
            speedMps = _speed.value.metersPerSecond * mag,
            accuracyMeters = 4f,
        )
        _fix.value = next
        LastKnownMapCenter.update(next)
        publish()
    }

    private fun publish() {
        val loc = currentAndroidLocation() ?: return
        listeners.forEach { it(loc) }
    }

    private fun currentAndroidLocation(): Location? {
        val fix = _fix.value ?: return null
        return Location("dev-sim").apply {
            latitude = fix.latitude
            longitude = fix.longitude
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            accuracy = fix.accuracyMeters ?: 4f
            fix.bearingDegrees?.let {
                bearing = it
            }
            fix.speedMps?.let {
                speed = it
            }
        }
    }

    private fun offsetMeters(lat: Double, lng: Double, north: Double, east: Double): Pair<Double, Double> {
        val dLat = north / 111_320.0
        val cosLat = cos(Math.toRadians(lat)).coerceAtLeast(0.2)
        val dLng = east / (111_320.0 * cosLat)
        return (lat + dLat).coerceIn(-85.0, 85.0) to wrapLng(lng + dLng)
    }

    private fun wrapLng(lng: Double): Double {
        var x = lng
        while (x > 180.0) x -= 360.0
        while (x < -180.0) x += 360.0
        return x
    }
}
