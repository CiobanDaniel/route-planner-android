package com.danielcioban.routeplanner.data.routing

import com.danielcioban.routeplanner.ui.map.LatLng
import java.util.Locale

/** In-memory LRU of last successful OSRM geometries. Used when the network dies. */
class RoutePolylineCache(private val maxEntries: Int = 16) {
    private val lock = Any()
    private val map = object : LinkedHashMap<String, DrivingRoute>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DrivingRoute>?): Boolean {
            return size > maxEntries
        }
    }

    fun put(key: String, route: DrivingRoute) {
        if (route.isApproximate || route.coordinates.size < 2) return
        synchronized(lock) {
            map[key] = route.copy(isCached = false)
        }
    }

    fun get(key: String): DrivingRoute? = synchronized(lock) {
        map[key]?.copy(isCached = true)
    }

    fun clear() {
        synchronized(lock) { map.clear() }
    }

    companion object {
        val Shared = RoutePolylineCache()

        fun key(from: LatLng, to: LatLng, profile: String, exclude: String?): String {
            fun round(value: Double) = "%.3f".format(Locale.US, value)
            return buildString {
                append(round(from.latitude)).append(',').append(round(from.longitude))
                append('>')
                append(round(to.latitude)).append(',').append(round(to.longitude))
                append('|').append(profile.ifBlank { "driving" })
                append('|').append(exclude.orEmpty())
            }
        }
    }
}
