package com.danielcioban.routeplanner.util

import com.danielcioban.routeplanner.data.local.TripHistoryEntity
import com.danielcioban.routeplanner.data.local.TripKind
import com.danielcioban.routeplanner.data.local.TripStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryFiltersTest {
    private val now = 1_700_000_000_000L
    private val week = 7L * 24L * 60L * 60L * 1000L

    @Test
    fun week_dropsOlderTrips() {
        val trips = listOf(
            trip(started = now - week + 1_000, title = "New"),
            trip(started = now - week - 1_000, title = "Old"),
        )
        val filtered = HistoryFilters.apply(trips, HistoryFilter.WEEK, nowMs = now)
        assertEquals(listOf("New"), filtered.map { it.title })
    }

    @Test
    fun cancelled_andNameQuery() {
        val trips = listOf(
            trip(status = TripStatus.CANCELLED, title = "Morning van"),
            trip(status = TripStatus.COMPLETED, title = "Morning van"),
            trip(status = TripStatus.CANCELLED, title = "Night", dest = "Cafe"),
        )
        val filtered = HistoryFilters.apply(
            trips,
            HistoryFilter.CANCELLED,
            routeQuery = "cafe",
            nowMs = now,
        )
        assertEquals(1, filtered.size)
        assertEquals("Night", filtered.first().title)
    }

    private fun trip(
        status: String = TripStatus.COMPLETED,
        started: Long = now,
        title: String,
        dest: String = "",
    ) = TripHistoryEntity(
        kind = TripKind.ROUTE,
        status = status,
        title = title,
        startedAtEpochMs = started,
        destName = dest,
    )
}
