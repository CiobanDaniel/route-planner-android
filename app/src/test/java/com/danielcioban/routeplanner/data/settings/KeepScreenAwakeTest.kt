package com.danielcioban.routeplanner.data.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepScreenAwakeTest {
    @Test
    fun off_neverKeepsAwake() {
        val settings = AppSettings(keepScreenOnDuringNav = false)
        assertFalse(settings.keepScreenAwake(tripActive = true, speedMps = 20f))
    }

    @Test
    fun wholeTrip_keepsAwakeWhenParked() {
        val settings = AppSettings(
            keepScreenOnDuringNav = true,
            keepScreenOnOnlyWhileMoving = false,
        )
        assertTrue(settings.keepScreenAwake(tripActive = true, speedMps = 0f))
        assertFalse(settings.keepScreenAwake(tripActive = false, speedMps = 20f))
    }

    @Test
    fun onlyWhileMoving_requiresWalkingSpeed() {
        val settings = AppSettings(
            keepScreenOnDuringNav = true,
            keepScreenOnOnlyWhileMoving = true,
        )
        assertFalse(settings.keepScreenAwake(tripActive = true, speedMps = 0.5f))
        assertTrue(settings.keepScreenAwake(tripActive = true, speedMps = 1.4f))
        assertFalse(settings.keepScreenAwake(tripActive = true, speedMps = null))
    }
}
