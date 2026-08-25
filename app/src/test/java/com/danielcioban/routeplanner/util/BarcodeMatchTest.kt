package com.danielcioban.routeplanner.util

import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import com.danielcioban.routeplanner.data.local.StopTaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeMatchTest {
    @Test
    fun matchesStopBarcode() {
        val stop = stop(id = 3, barcode = "PKG-99")
        val result = BarcodeMatch.match("pkg-99", listOf(stop), emptyMap())
        assertEquals(BarcodeMatch.Result.Stop(3), result)
    }

    @Test
    fun matchesOpenTaskTitle() {
        val stop = stop(id = 8)
        val task = StopTaskEntity(id = 12, stopId = 8, title = "ID check", isCompleted = false)
        val result = BarcodeMatch.match(
            "ID check",
            listOf(stop),
            mapOf(8L to listOf(task)),
        )
        assertEquals(BarcodeMatch.Result.Task(8, 12), result)
    }

    @Test
    fun parsesGeoUriAsNearbyStop() {
        val stop = stop(id = 4, latitude = 45.75, longitude = 21.23)
        val result = BarcodeMatch.match("geo:45.75001,21.23001", listOf(stop), emptyMap())
        assertEquals(BarcodeMatch.Result.Stop(4), result)
    }

    @Test
    fun libraryUriReturnsRemoteIdWhenNotOnRoute() {
        val result = BarcodeMatch.match(
            "https://routeplanner.local/l/lib-abc",
            emptyList(),
            emptyMap(),
        )
        assertEquals(BarcodeMatch.Result.Library("lib-abc"), result)
    }

    @Test
    fun libraryUriMatchesStopOnRoute() {
        val library = StopLibraryEntity(
            id = 2,
            remoteId = "lib-abc",
            name = "Shop",
            latitude = 45.0,
            longitude = 21.0,
        )
        val stop = stop(id = 9, libraryStopId = 2)
        val result = BarcodeMatch.match(
            "https://routeplanner.local/l/lib-abc",
            listOf(stop),
            emptyMap(),
            mapOf(2L to library),
        )
        assertEquals(BarcodeMatch.Result.Stop(9), result)
    }

    @Test
    fun emptyPayloadIsNone() {
        assertTrue(BarcodeMatch.match("  ", emptyList(), emptyMap()) is BarcodeMatch.Result.None)
    }

    private fun stop(
        id: Long,
        barcode: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        libraryStopId: Long? = null,
    ) = StopEntity(
        id = id,
        routeId = 1,
        position = 0,
        name = "Stop $id",
        barcode = barcode,
        latitude = latitude,
        longitude = longitude,
        libraryStopId = libraryStopId,
    )
}
