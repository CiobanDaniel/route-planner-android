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

    /** Delivery and break stops only — GPS origin is stored on the route, not as a list row. */
    val deliveryStops: List<StopEntity>
        get() = orderedStops.filter { !it.isOrigin }

    val completedCount: Int
        get() = deliveryStops.count { it.isCompleted }

    val remainingDeliveryStops: List<StopEntity>
        get() = deliveryStops.filter { !it.isCompleted }

    val nextIncompleteStop: StopEntity?
        get() = remainingDeliveryStops.firstOrNull()

    fun isTodayWork(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (route.archived) return false
        if (remainingDeliveryStops.isEmpty()) return false
        val dated = remainingDeliveryStops.mapNotNull { it.arriveByEpochMs }
        if (dated.isEmpty()) return true
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMs }
        val year = cal.get(java.util.Calendar.YEAR)
        val day = cal.get(java.util.Calendar.DAY_OF_YEAR)
        return dated.any { epoch ->
            cal.timeInMillis = epoch
            cal.get(java.util.Calendar.YEAR) == year &&
                cal.get(java.util.Calendar.DAY_OF_YEAR) == day
        }
    }

    /** Straight-line chain between consecutive remaining pinned stops. */
    val approxDistanceMeters: Double
        get() = remainingDistanceMeters

    val remainingDistanceMeters: Double
        get() {
            val pinned = deliveryStops.filter {
                !it.isCompleted && it.latitude != null && it.longitude != null
            }
            if (pinned.isEmpty()) return 0.0
            var sum = 0.0
            val originLat = route.originLatitude
            val originLng = route.originLongitude
            if (originLat != null && originLng != null) {
                sum += GeoUtils.distanceMeters(
                    originLat,
                    originLng,
                    pinned.first().latitude!!,
                    pinned.first().longitude!!,
                )
            }
            sum += pinned.zipWithNext().sumOf { (from, to) ->
                GeoUtils.distanceMeters(
                    from.latitude!!,
                    from.longitude!!,
                    to.latitude!!,
                    to.longitude!!,
                )
            }
            if (route.roundTrip) {
                val first = deliveryStops.firstOrNull {
                    it.latitude != null && it.longitude != null
                } ?: orderedStops.firstOrNull { it.latitude != null && it.longitude != null }
                val last = pinned.lastOrNull()
                if (first != null && last != null && first.id != last.id) {
                    sum += GeoUtils.distanceMeters(
                        last.latitude!!,
                        last.longitude!!,
                        first.latitude!!,
                        first.longitude!!,
                    )
                }
            }
            return sum
        }

    fun matchesQuery(query: String): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return true
        if (route.name.contains(needle, ignoreCase = true)) return true
        if (route.notes.contains(needle, ignoreCase = true)) return true
        if (route.vanName.contains(needle, ignoreCase = true)) return true
        if (route.shiftName.contains(needle, ignoreCase = true)) return true
        return orderedStops.any { stop ->
            stop.name.contains(needle, ignoreCase = true) ||
                stop.addressHint.contains(needle, ignoreCase = true)
        }
    }
}
