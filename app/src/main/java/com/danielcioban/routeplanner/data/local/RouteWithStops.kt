package com.danielcioban.routeplanner.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.danielcioban.routeplanner.util.GeoUtils

data class RouteWithStops(
    @Embedded val route: RouteEntity,
    @Relation(parentColumn = "id", entityColumn = "routeId")
    val stops: List<StopEntity>,
) {
    val orderedStops: List<StopEntity>
        get() = stops.sortedBy { it.position }

    val completedCount: Int
        get() = stops.count { it.isCompleted }

    /** Straight-line chain between consecutive pinned stops. */
    val approxDistanceMeters: Double
        get() {
            val pinned = orderedStops.filter { it.latitude != null && it.longitude != null }
            if (pinned.size < 2) return 0.0
            return pinned.zipWithNext().sumOf { (from, to) ->
                GeoUtils.distanceMeters(
                    from.latitude!!,
                    from.longitude!!,
                    to.latitude!!,
                    to.longitude!!,
                )
            }
        }

    fun matchesQuery(query: String): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return true
        if (route.name.contains(needle, ignoreCase = true)) return true
        if (route.notes.contains(needle, ignoreCase = true)) return true
        return orderedStops.any { stop ->
            stop.name.contains(needle, ignoreCase = true) ||
                stop.addressHint.contains(needle, ignoreCase = true)
        }
    }
}
