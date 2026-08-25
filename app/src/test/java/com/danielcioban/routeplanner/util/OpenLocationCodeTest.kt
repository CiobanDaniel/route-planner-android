package com.danielcioban.routeplanner.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class OpenLocationCodeTest {
    @Test
    fun encode_knownZurichSample() {
        val code = OpenLocationCode.encode(47.365590, 8.524997)
        assertTrue(OpenLocationCode.isFullCode(code))
        assertEquals(11, code.length)
        assertTrue(code.startsWith("8FVC9G8F+"))
    }

    @Test
    fun decode_roundTripsWithinCell() {
        val encoded = OpenLocationCode.encode(45.75, 21.23)
        val decoded = OpenLocationCode.decode(encoded)
        assertNotNull(decoded)
        assertTrue(abs(decoded!!.latitude - 45.75) < 0.001)
        assertTrue(abs(decoded.longitude - 21.23) < 0.001)
    }

    @Test
    fun formatLatLng() {
        assertEquals("45.750000, 21.230000", OpenLocationCode.formatLatLng(45.75, 21.23))
    }
}
