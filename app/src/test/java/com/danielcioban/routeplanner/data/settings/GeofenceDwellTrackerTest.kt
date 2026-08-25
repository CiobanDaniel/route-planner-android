package com.danielcioban.routeplanner.data.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceDwellTrackerTest {
    @Test
    fun zeroDwell_firesOnFirstInsideTick() {
        val tracker = GeofenceDwellTracker()
        assertTrue(tracker.ready(1L, inside = true, nowMs = 1_000L, dwellMs = 0L))
    }

    @Test
    fun dwell_waitsUntilElapsed() {
        val tracker = GeofenceDwellTracker()
        assertFalse(tracker.ready(1L, inside = true, nowMs = 1_000L, dwellMs = 5_000L))
        assertFalse(tracker.ready(1L, inside = true, nowMs = 5_999L, dwellMs = 5_000L))
        assertTrue(tracker.ready(1L, inside = true, nowMs = 6_000L, dwellMs = 5_000L))
    }

    @Test
    fun leaving_resetsDwell() {
        val tracker = GeofenceDwellTracker()
        assertFalse(tracker.ready(1L, inside = true, nowMs = 1_000L, dwellMs = 5_000L))
        assertFalse(tracker.ready(1L, inside = false, nowMs = 4_000L, dwellMs = 5_000L))
        assertFalse(tracker.ready(1L, inside = true, nowMs = 4_001L, dwellMs = 5_000L))
        assertTrue(tracker.ready(1L, inside = true, nowMs = 9_001L, dwellMs = 5_000L))
    }

    @Test
    fun separateStops_areIndependent() {
        val tracker = GeofenceDwellTracker()
        assertFalse(tracker.ready(1L, inside = true, nowMs = 0L, dwellMs = 3_000L))
        assertTrue(tracker.ready(2L, inside = true, nowMs = 0L, dwellMs = 0L))
        assertFalse(tracker.ready(1L, inside = true, nowMs = 2_999L, dwellMs = 3_000L))
        assertTrue(tracker.ready(1L, inside = true, nowMs = 3_000L, dwellMs = 3_000L))
    }
}
