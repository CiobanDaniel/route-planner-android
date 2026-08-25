package com.danielcioban.routeplanner.util

import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.local.TripHistoryEntity
import com.danielcioban.routeplanner.data.local.TripStatus
import java.util.Calendar

data class TripStatsSummary(
    val completedTrips: Int,
    val stopsCompleted: Int,
    val distanceMeters: Double,
    val lateStops: Int,
    val durationMs: Long,
    val stopsPerHour: Double,
    val todayStops: Int,
    val todayDistanceMeters: Double,
    val todayLate: Int,
    val codCollected: Double,
)

object TripStats {
    fun summarize(
        trips: List<TripHistoryEntity>,
        nowMs: Long = System.currentTimeMillis(),
        codCollected: Double = 0.0,
    ): TripStatsSummary {
        val done = trips.filter { it.status == TripStatus.COMPLETED }
        val stops = done.sumOf { it.stopsCompleted.coerceAtLeast(0) }
        val distance = done.sumOf { it.distanceMeters.coerceAtLeast(0.0) }
        val late = done.sumOf { it.lateStops.coerceAtLeast(0) }
        val duration = done.sumOf { trip ->
            val end = trip.endedAtEpochMs ?: return@sumOf 0L
            (end - trip.startedAtEpochMs).coerceAtLeast(0L)
        }
        val hours = duration / 3_600_000.0
        val perHour = if (hours > 0.05) stops / hours else 0.0
        val today = done.filter { sameLocalDay(it.startedAtEpochMs, nowMs) }
        return TripStatsSummary(
            completedTrips = done.size,
            stopsCompleted = stops,
            distanceMeters = distance,
            lateStops = late,
            durationMs = duration,
            stopsPerHour = perHour,
            todayStops = today.sumOf { it.stopsCompleted.coerceAtLeast(0) },
            todayDistanceMeters = today.sumOf { it.distanceMeters.coerceAtLeast(0.0) },
            todayLate = today.sumOf { it.lateStops.coerceAtLeast(0) },
            codCollected = codCollected,
        )
    }

    fun completedPathMeters(route: RouteWithStops): Double {
        val pinned = route.deliveryStops.filter {
            it.isCompleted && it.latitude != null && it.longitude != null
        }
        if (pinned.isEmpty()) return 0.0
        var sum = 0.0
        val originLat = route.route.originLatitude
        val originLng = route.route.originLongitude
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
        return sum
    }

    fun lateStopCount(stops: List<StopEntity>): Int = stops.count { isLate(it) }

    fun isLate(stop: StopEntity): Boolean {
        val visited = stop.visitedAtEpochMs ?: return false
        val deadlineEpoch = stop.arriveByEpochMs
        if (deadlineEpoch != null) return visited > deadlineEpoch
        val minutes = stop.arriveByMinutes ?: return false
        val cal = Calendar.getInstance().apply { timeInMillis = visited }
        val visitedMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return visitedMinutes > minutes
    }

    fun sameLocalDay(aMs: Long, bMs: Long): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = aMs
        val year = cal.get(Calendar.YEAR)
        val day = cal.get(Calendar.DAY_OF_YEAR)
        cal.timeInMillis = bMs
        return cal.get(Calendar.YEAR) == year && cal.get(Calendar.DAY_OF_YEAR) == day
    }
}
