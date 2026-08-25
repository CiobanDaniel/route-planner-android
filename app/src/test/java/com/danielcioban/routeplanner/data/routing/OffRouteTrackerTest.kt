package com.danielcioban.routeplanner.data.routing

import com.danielcioban.routeplanner.data.settings.RerouteAggressiveness
import com.danielcioban.routeplanner.ui.map.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OffRouteTrackerTest {
    @Test
    fun normalRecalcAfterThresholdAndInterval() {
        val tracker = OffRouteTracker()
        assertFalse(tracker.shouldRecalc(10.0, RerouteAggressiveness.NORMAL, nowMs = 1_000L))
        assertTrue(tracker.shouldRecalc(60.0, RerouteAggressiveness.NORMAL, nowMs = 10_000L))
        assertFalse(tracker.shouldRecalc(60.0, RerouteAggressiveness.NORMAL, nowMs = 12_000L))
    }

    @Test
    fun calmNeedsTwoHits() {
        val tracker = OffRouteTracker()
        assertFalse(tracker.shouldRecalc(100.0, RerouteAggressiveness.CALM, nowMs = 20_000L))
        assertTrue(tracker.shouldRecalc(100.0, RerouteAggressiveness.CALM, nowMs = 21_000L))
    }
}

class DrivingRouteLastMileTest {
    @Test
    fun appendFootLegDropsArriveAndAddsWalk() {
        val vehicle = DrivingRoute(
            coordinates = listOf(LatLng(45.75, 21.22), LatLng(45.751, 21.221)),
            distanceMeters = 120.0,
            durationSeconds = 20.0,
            steps = listOf(
                ManeuverStep("", "depart", null, "", 120.0, 20.0, LatLng(45.75, 21.22)),
                ManeuverStep("", "arrive", null, "", 0.0, 0.0, LatLng(45.751, 21.221)),
            ),
        )
        val foot = DrivingRoute(
            coordinates = listOf(LatLng(45.751, 21.221), LatLng(45.7512, 21.2213)),
            distanceMeters = 30.0,
            durationSeconds = 25.0,
            steps = listOf(
                ManeuverStep("", "depart", null, "", 30.0, 25.0, LatLng(45.751, 21.221)),
                ManeuverStep("", "arrive", null, "", 0.0, 0.0, LatLng(45.7512, 21.2213)),
            ),
        )
        val merged = vehicle.appendFootLeg(foot)
        assertEquals(3, merged.coordinates.size)
        assertEquals(150.0, merged.distanceMeters, 0.01)
        assertEquals(45.0, merged.durationSeconds, 0.01)
        assertEquals(3, merged.steps.size)
        assertFalse(merged.steps.dropLast(1).any { it.type == "arrive" })
        assertEquals("arrive", merged.steps.last().type)
    }
}
