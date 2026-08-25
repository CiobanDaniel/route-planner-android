package com.danielcioban.routeplanner.util

import com.danielcioban.routeplanner.data.routing.LastRoutingErrorStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProblemReportTest {
    @Test
    fun body_includesVersionAndNoneWhenNoError() {
        val text = ProblemReport.body(
            device = ProblemReport.DeviceInfo(
                versionName = "0.5.0",
                versionCode = 6,
                sdk = 36,
                release = "16",
                manufacturer = "Google",
                model = "Pixel",
            ),
            error = null,
            noneLabel = "(none)",
        )
        assertTrue(text.contains("Route Planner 0.5.0 (6)"))
        assertTrue(text.contains("Android 16 (SDK 36)"))
        assertTrue(text.contains("Google Pixel"))
        assertTrue(text.contains("(none)"))
    }

    @Test
    fun body_includesLastRoutingErrorAndHost() {
        val text = ProblemReport.body(
            device = ProblemReport.DeviceInfo(
                versionName = "0.5.0",
                versionCode = 6,
                sdk = 34,
                release = "14",
                manufacturer = "Samsung",
                model = "SM-A",
            ),
            error = LastRoutingErrorStore.Snapshot(
                summary = "routing_timeout",
                host = "https://router.example",
                atEpochMs = 1_724_572_800_000L,
            ),
            noneLabel = "(none)",
        )
        assertTrue(text.contains("routing_timeout"))
        assertTrue(text.contains("https://router.example"))
        assertTrue(text.contains("UTC"))
        assertFalse(text.contains("(none)"))
    }
}
