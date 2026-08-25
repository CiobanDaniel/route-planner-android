package com.danielcioban.routeplanner.data.settings

import com.danielcioban.routeplanner.util.GeoUtils

enum class GeofenceAction {
    OFF,
    VISITED,
    COMPLETE,
    ;

    companion object {
        fun fromStored(value: String?): GeofenceAction =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: VISITED
    }
}

enum class RouteGeofenceMode {
    INHERIT,
    OFF,
    VISITED,
    COMPLETE,
    ;

    companion object {
        fun fromStored(value: String?): RouteGeofenceMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: INHERIT
    }
}

object StopGeofence {
    const val DEFAULT_RADIUS_METERS = 50
    const val MIN_RADIUS_METERS = 20
    const val MAX_RADIUS_METERS = 500

    fun clampRadius(meters: Int): Int = meters.coerceIn(MIN_RADIUS_METERS, MAX_RADIUS_METERS)

    fun parseRadius(text: String): Int? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.toIntOrNull()?.let(::clampRadius)
    }

    fun effectiveAction(routeMode: RouteGeofenceMode, global: GeofenceAction): GeofenceAction =
        when (routeMode) {
            RouteGeofenceMode.INHERIT -> global
            RouteGeofenceMode.OFF -> GeofenceAction.OFF
            RouteGeofenceMode.VISITED -> GeofenceAction.VISITED
            RouteGeofenceMode.COMPLETE -> GeofenceAction.COMPLETE
        }

    fun effectiveRadiusMeters(
        stopRadiusMeters: Int?,
        routeRadiusMeters: Int?,
        globalRadiusMeters: Int,
    ): Int = clampRadius(stopRadiusMeters ?: routeRadiusMeters ?: globalRadiusMeters)

    fun isInside(
        userLatitude: Double,
        userLongitude: Double,
        stopLatitude: Double,
        stopLongitude: Double,
        radiusMeters: Int,
        accuracyMeters: Float?,
    ): Boolean {
        if (accuracyMeters != null && accuracyMeters > radiusMeters) return false
        return GeoUtils.distanceMeters(
            userLatitude,
            userLongitude,
            stopLatitude,
            stopLongitude,
        ) <= radiusMeters
    }
}

object GeofenceDwell {
    val OPTIONS_SECONDS = listOf(0, 3, 5, 10)

    fun clampSeconds(seconds: Int): Int =
        OPTIONS_SECONDS.minByOrNull { kotlin.math.abs(it - seconds) } ?: 0
}

/** Requires [dwellMs] inside the radius before [ready] returns true. Zero dwell fires on the first inside tick. */
class GeofenceDwellTracker {
    private val enteredAtMs = mutableMapOf<Long, Long>()

    fun reset() {
        enteredAtMs.clear()
    }

    fun ready(id: Long, inside: Boolean, nowMs: Long, dwellMs: Long): Boolean {
        if (!inside) {
            enteredAtMs.remove(id)
            return false
        }
        val start = enteredAtMs.getOrPut(id) { nowMs }
        return nowMs - start >= dwellMs.coerceAtLeast(0L)
    }
}
