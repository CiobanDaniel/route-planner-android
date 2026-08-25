package com.danielcioban.routeplanner.util

import com.danielcioban.routeplanner.data.local.StopEntity
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Straight-line remaining-leg ETAs for planning (not live OSRM). */
object RouteEta {
    /** ~30 km/h urban courier average. */
    const val DEFAULT_SPEED_MPS = 8.33

    data class StopArrival(
        val stopId: Long,
        val epochMs: Long,
        val travelMetersFromPrevious: Double,
        val late: Boolean,
    )

    fun estimateRemaining(
        remaining: List<StopEntity>,
        fromLatitude: Double?,
        fromLongitude: Double?,
        nowMs: Long = System.currentTimeMillis(),
        speedMps: Double = DEFAULT_SPEED_MPS,
    ): List<StopArrival> {
        val pinned = remaining.filter { it.latitude != null && it.longitude != null }
        if (pinned.isEmpty()) return emptyList()
        var cursorMs = nowMs
        var prevLat = fromLatitude
        var prevLng = fromLongitude
        val speed = speedMps.coerceAtLeast(1.0)
        return pinned.map { stop ->
            val lat = stop.latitude!!
            val lng = stop.longitude!!
            val fromLat = prevLat
            val fromLng = prevLng
            val meters = if (fromLat != null && fromLng != null) {
                GeoUtils.distanceMeters(fromLat, fromLng, lat, lng)
            } else {
                0.0
            }
            val travelMs = ((meters / speed) * 1000.0).toLong()
            cursorMs += travelMs
            val late = isArrivalLate(cursorMs, stop.arriveByEpochMs, stop.arriveByMinutes, nowMs)
            val arrival = StopArrival(
                stopId = stop.id,
                epochMs = cursorMs,
                travelMetersFromPrevious = meters,
                late = late,
            )
            cursorMs += TimeUnit.MINUTES.toMillis(stop.serviceMinutes.coerceAtLeast(0).toLong())
            prevLat = lat
            prevLng = lng
            arrival
        }
    }

    /** Minutes from [nowMs] until leaving the last remaining stop (travel + dwell). */
    fun remainingWorkMinutes(
        remaining: List<StopEntity>,
        fromLatitude: Double?,
        fromLongitude: Double?,
        nowMs: Long = System.currentTimeMillis(),
        speedMps: Double = DEFAULT_SPEED_MPS,
    ): Int {
        val arrivals = estimateRemaining(remaining, fromLatitude, fromLongitude, nowMs, speedMps)
        if (arrivals.isEmpty()) return 0
        val last = arrivals.last()
        val lastStop = remaining.first { it.id == last.stopId }
        val doneMs = last.epochMs +
            TimeUnit.MINUTES.toMillis(lastStop.serviceMinutes.coerceAtLeast(0).toLong())
        return TimeUnit.MILLISECONDS.toMinutes((doneMs - nowMs).coerceAtLeast(0)).toInt()
    }

    fun minutesFromMidnight(epochMs: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun clampMinutes(value: Int): Int = value.coerceIn(0, 23 * 60 + 59)

    /** Absolute promised arrival, or today's clock time from [arriveByMinutes]. */
    fun promisedDeadlineMs(
        arriveByEpochMs: Long?,
        arriveByMinutes: Int?,
        nowMs: Long = System.currentTimeMillis(),
    ): Long? {
        arriveByEpochMs?.let { return it }
        val minutes = arriveByMinutes ?: return null
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        cal.set(Calendar.HOUR_OF_DAY, minutes / 60)
        cal.set(Calendar.MINUTE, minutes % 60)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun isArrivalLate(
        arrivalMs: Long,
        arriveByEpochMs: Long?,
        arriveByMinutes: Int?,
        nowMs: Long = System.currentTimeMillis(),
    ): Boolean {
        val deadline = promisedDeadlineMs(arriveByEpochMs, arriveByMinutes, nowMs) ?: return false
        return arrivalMs > deadline
    }

    fun epochFromLocalDateTime(
        year: Int,
        month: Int,
        day: Int,
        minutesFromMidnight: Int,
    ): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, minutesFromMidnight / 60)
        cal.set(Calendar.MINUTE, minutesFromMidnight % 60)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
