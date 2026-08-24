package com.danielcioban.routeplanner.util

/**
 * Greedy nearest-neighbor reorder for courier stop lists.
 * Unpinned items keep their relative order at the end.
 */
object RouteOrderOptimizer {
    fun <T> nearestNeighborOrder(
        items: List<T>,
        latitude: (T) -> Double?,
        longitude: (T) -> Double?,
        startLatitude: Double? = null,
        startLongitude: Double? = null,
    ): List<T> {
        if (items.size <= 1) return items
        val pinned = mutableListOf<T>()
        val unpinned = mutableListOf<T>()
        for (item in items) {
            val lat = latitude(item)
            val lng = longitude(item)
            if (lat != null && lng != null) pinned += item else unpinned += item
        }
        if (pinned.size <= 1) return items

        val remaining = pinned.toMutableList()
        val ordered = ArrayList<T>(items.size)
        var currentLat: Double
        var currentLng: Double
        if (startLatitude != null && startLongitude != null) {
            currentLat = startLatitude
            currentLng = startLongitude
        } else {
            val first = remaining.removeAt(0)
            ordered += first
            currentLat = latitude(first)!!
            currentLng = longitude(first)!!
        }
        while (remaining.isNotEmpty()) {
            var bestIndex = 0
            var bestDistance = Double.POSITIVE_INFINITY
            remaining.forEachIndexed { index, item ->
                val distance = GeoUtils.distanceMeters(
                    currentLat,
                    currentLng,
                    latitude(item)!!,
                    longitude(item)!!,
                )
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestIndex = index
                }
            }
            val next = remaining.removeAt(bestIndex)
            ordered += next
            currentLat = latitude(next)!!
            currentLng = longitude(next)!!
        }
        ordered += unpinned
        return ordered
    }

    /** Keep completed stops in place, then nearest-neighbor the rest from [start] or last completed pin. */
    fun <T> optimizeRemaining(
        items: List<T>,
        isCompleted: (T) -> Boolean,
        latitude: (T) -> Double?,
        longitude: (T) -> Double?,
        startLatitude: Double? = null,
        startLongitude: Double? = null,
    ): List<T> {
        val completed = items.filter(isCompleted)
        val remaining = items.filterNot(isCompleted)
        val originLat: Double?
        val originLng: Double?
        if (startLatitude != null && startLongitude != null) {
            originLat = startLatitude
            originLng = startLongitude
        } else {
            val lastPin = completed.lastOrNull { latitude(it) != null && longitude(it) != null }
            originLat = lastPin?.let(latitude)
            originLng = lastPin?.let(longitude)
        }
        return completed + nearestNeighborOrder(
            remaining,
            latitude,
            longitude,
            originLat,
            originLng,
        )
    }
}
