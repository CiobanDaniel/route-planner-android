package com.danielcioban.routeplanner.util

import com.danielcioban.routeplanner.data.local.RouteEntity
import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.data.local.StopEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarExportTest {
    @Test
    fun icsContainsPromisedStops() {
        val route = RouteWithStops(
            route = RouteEntity(id = 1, name = "Morning", remoteId = "route-1"),
            stops = listOf(
                StopEntity(
                    id = 1,
                    remoteId = "stop-1",
                    routeId = 1,
                    position = 0,
                    name = "Shop",
                    arriveByEpochMs = 1_700_000_000_000L,
                    serviceMinutes = 20,
                    latitude = 45.75,
                    longitude = 21.23,
                ),
                StopEntity(
                    id = 2,
                    remoteId = "stop-2",
                    routeId = 1,
                    position = 1,
                    name = "No window",
                ),
            ),
        )
        val ics = CalendarExport.icsForRoute(route, nowMs = 1_700_000_000_000L)
        assertTrue(ics.contains("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("SUMMARY:Shop"))
        assertTrue(ics.contains("GEO:45.75;21.23"))
        assertFalse(ics.contains("SUMMARY:No window"))
    }
}
