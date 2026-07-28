package com.danielcioban.routeplanner.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoUtilsTest {
    @Test
    fun distanceBetweenSamePoint_isZero() {
        assertEquals(0.0, GeoUtils.distanceMeters(45.75, 21.23, 45.75, 21.23), 0.01)
    }

    @Test
    fun distanceInCity_isPlausible() {
        // Roughly ~1.1 km apart in Timisoara area
        val meters = GeoUtils.distanceMeters(45.7489, 21.2257, 45.7550, 21.2300)
        assertTrue(meters in 500.0..2000.0)
    }
}
