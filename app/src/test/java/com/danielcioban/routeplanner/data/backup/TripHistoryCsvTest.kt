package com.danielcioban.routeplanner.data.backup

import com.danielcioban.routeplanner.data.local.TripHistoryEntity
import com.danielcioban.routeplanner.data.local.TripKind
import com.danielcioban.routeplanner.data.local.TripStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class TripHistoryCsvTest {
    @Test
    fun export_includesHeaderAndQuotedTitle() {
        val csv = TripHistoryCsv.export(
            listOf(
                TripHistoryEntity(
                    kind = TripKind.QUICK,
                    status = TripStatus.COMPLETED,
                    title = "Cafe, downtown",
                    startedAtEpochMs = 1_700_000_000_000L,
                    endedAtEpochMs = 1_700_000_360_000L,
                    destName = "Cafe",
                    stopsCompleted = 1,
                    stopsTotal = 1,
                    distanceMeters = 1200.0,
                    lateStops = 0,
                ),
            ),
        )
        assertTrue(csv.startsWith("started,ended,kind,status,title,"))
        assertTrue(csv.contains("\"Cafe, downtown\""))
        assertTrue(csv.contains("QUICK"))
        assertTrue(csv.contains("COMPLETED"))
    }
}
