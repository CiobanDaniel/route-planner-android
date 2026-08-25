package com.danielcioban.routeplanner.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class GeoUtilsTest {
    @Test
    fun distanceBetweenSamePoint_isZero() {
        assertEquals(0.0, GeoUtils.distanceMeters(45.75, 21.23, 45.75, 21.23), 0.01)
    }

    @Test
    fun distanceInCity_isPlausible() {
        val meters = GeoUtils.distanceMeters(45.7489, 21.2257, 45.7550, 21.2300)
        assertTrue(meters in 500.0..2000.0)
    }

    @Test
    fun formatClockMinutes_24h_isZeroPadded() {
        assertEquals("09:05", GeoUtils.formatClockMinutes(9 * 60 + 5, use24Hour = true, locale = Locale.US))
        assertEquals("14:30", GeoUtils.formatClockMinutes(14 * 60 + 30, use24Hour = true, locale = Locale.US))
    }

    @Test
    fun formatClockMinutes_12h_usesAmPm() {
        val morning = GeoUtils.formatClockMinutes(9 * 60 + 5, use24Hour = false, locale = Locale.US)
        val afternoon = GeoUtils.formatClockMinutes(14 * 60 + 30, use24Hour = false, locale = Locale.US)
        assertTrue(morning.contains("9:05"))
        assertTrue(morning.uppercase(Locale.US).contains("AM"))
        assertTrue(afternoon.contains("2:30"))
        assertTrue(afternoon.uppercase(Locale.US).contains("PM"))
    }
}
