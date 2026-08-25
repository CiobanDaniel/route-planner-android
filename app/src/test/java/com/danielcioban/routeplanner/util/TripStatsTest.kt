package com.danielcioban.routeplanner.util

import com.danielcioban.routeplanner.data.local.RouteEntity
import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.local.TripHistoryEntity
import com.danielcioban.routeplanner.data.local.TripKind
import com.danielcioban.routeplanner.data.local.TripStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripStatsTest {
    @Test
    fun summarizeIgnoresCancelledAndCountsCompleted() {
        val now = 1_700_000_000_000L
        val done = TripHistoryEntity(
            kind = TripKind.ROUTE,
            status = TripStatus.COMPLETED,
            title = "A",
            startedAtEpochMs = now - 3_600_000L,
            endedAtEpochMs = now,
            stopsCompleted = 6,
            distanceMeters = 1200.0,
            lateStops = 2,
        )
        val cancelled = done.copy(
            status = TripStatus.CANCELLED,
            stopsCompleted = 99,
            distanceMeters = 9_000.0,
        )
        val summary = TripStats.summarize(listOf(done, cancelled), nowMs = now, codCollected = 12.5)
        assertEquals(1, summary.completedTrips)
        assertEquals(6, summary.stopsCompleted)
        assertEquals(1200.0, summary.distanceMeters, 0.01)
        assertEquals(2, summary.lateStops)
        assertEquals(12.5, summary.codCollected, 0.01)
        assertTrue(summary.stopsPerHour > 5.0)
    }

    @Test
    fun completedPathIncludesOrigin() {
        val route = RouteWithStops(
            route = RouteEntity(
                id = 1,
                name = "R",
                originLatitude = 45.0,
                originLongitude = 21.0,
            ),
            stops = listOf(
                StopEntity(
                    id = 1,
                    routeId = 1,
                    position = 0,
                    name = "A",
                    latitude = 45.01,
                    longitude = 21.0,
                    isCompleted = true,
                ),
                StopEntity(
                    id = 2,
                    routeId = 1,
                    position = 1,
                    name = "B",
                    latitude = 45.02,
                    longitude = 21.0,
                    isCompleted = true,
                ),
            ),
        )
        val meters = TripStats.completedPathMeters(route)
        assertTrue(meters > 1_000.0)
    }

    @Test
    fun lateStopUsesArriveByEpoch() {
        val stop = StopEntity(
            id = 1,
            routeId = 1,
            position = 0,
            name = "Late",
            arriveByEpochMs = 1_000L,
            visitedAtEpochMs = 2_000L,
        )
        assertEquals(1, TripStats.lateStopCount(listOf(stop)))
    }
}
