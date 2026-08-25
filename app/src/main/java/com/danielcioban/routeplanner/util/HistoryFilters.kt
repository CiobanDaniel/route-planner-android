package com.danielcioban.routeplanner.util

import com.danielcioban.routeplanner.data.local.TripHistoryEntity
import com.danielcioban.routeplanner.data.local.TripStatus

enum class HistoryFilter {
    ALL,
    WEEK,
    CANCELLED,
}

object HistoryFilters {
    fun apply(
        trips: List<TripHistoryEntity>,
        filter: HistoryFilter,
        routeQuery: String = "",
        nowMs: Long = System.currentTimeMillis(),
    ): List<TripHistoryEntity> {
        val weekStart = nowMs - 7L * 24L * 60L * 60L * 1000L
        val q = routeQuery.trim()
        return trips.filter { trip ->
            val rangeOk = when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.WEEK -> trip.startedAtEpochMs >= weekStart
                HistoryFilter.CANCELLED -> trip.status == TripStatus.CANCELLED
            }
            val queryOk = q.isEmpty() ||
                trip.title.contains(q, ignoreCase = true) ||
                trip.destName.contains(q, ignoreCase = true)
            rangeOk && queryOk
        }
    }
}
