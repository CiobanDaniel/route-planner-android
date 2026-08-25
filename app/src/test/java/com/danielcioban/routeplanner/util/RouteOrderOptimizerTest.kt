package com.danielcioban.routeplanner.util

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteOrderOptimizerTest {
    data class Stop(
        val name: String,
        val lat: Double? = null,
        val lng: Double? = null,
        val done: Boolean = false,
    )

    @Test
    fun nearestNeighbor_fromOrigin_picksClosestFirst() {
        val stops = listOf(
            Stop("Far", lat = 46.0, lng = 21.0),
            Stop("Near", lat = 45.76, lng = 21.23),
            Stop("Mid", lat = 45.80, lng = 21.25),
        )
        val ordered = RouteOrderOptimizer.nearestNeighborOrder(
            stops,
            { it.lat },
            { it.lng },
            startLatitude = 45.75,
            startLongitude = 21.23,
        )
        assertEquals(listOf("Near", "Mid", "Far"), ordered.map { it.name })
    }

    @Test
    fun unpinnedStops_stayAtEndInOriginalOrder() {
        val stops = listOf(
            Stop("B", lat = 46.0, lng = 21.0),
            Stop("No pin A"),
            Stop("A", lat = 45.75, lng = 21.23),
            Stop("No pin B"),
        )
        val ordered = RouteOrderOptimizer.nearestNeighborOrder(
            stops,
            { it.lat },
            { it.lng },
            startLatitude = 45.75,
            startLongitude = 21.23,
        )
        assertEquals(listOf("A", "B", "No pin A", "No pin B"), ordered.map { it.name })
    }

    @Test
    fun optimizeRemaining_keepsCompletedPrefix() {
        val stops = listOf(
            Stop("Done", lat = 45.75, lng = 21.23, done = true),
            Stop("Far", lat = 46.0, lng = 21.0),
            Stop("Near", lat = 45.76, lng = 21.23),
        )
        val ordered = RouteOrderOptimizer.optimizeRemaining(
            stops,
            { it.done },
            { it.lat },
            { it.lng },
            startLatitude = 45.75,
            startLongitude = 21.23,
        )
        assertEquals(listOf("Done", "Near", "Far"), ordered.map { it.name })
    }

    @Test
    fun twoOptWithDurationMatrix_avoidsExpensiveHaversineEdge() {
        data class Stop(val name: String)
        val a = Stop("A")
        val b = Stop("B")
        val c = Stop("C")
        val d = Stop("D")
        val matrix = Array(5) { row ->
            DoubleArray(5) { col -> if (row == col) 0.0 else 100.0 }
        }
        // Origin (0) is cheap to every stop. A→D→C→B is the cheap path.
        for (i in 1..4) matrix[0][i] = 1.0
        matrix[1][4] = 1.0
        matrix[4][3] = 1.0
        matrix[3][2] = 1.0
        matrix[2][3] = 1.0
        matrix[3][4] = 1.0
        matrix[4][2] = 1.0
        val ordered = RouteOrderOptimizer.twoOptWithDurationMatrix(
            listOf(a, b, c, d),
            matrix,
            hasOrigin = true,
            roundTrip = false,
        )
        assertEquals(listOf("A", "D", "C", "B"), ordered.map { it.name })
    }

    @Test
    fun optimizeRemaining_keepsFixedOrderAnchorFirst() {
        data class Stop(
            val name: String,
            val lat: Double,
            val lng: Double,
            val fixed: Boolean = false,
        )
        val ordered = RouteOrderOptimizer.optimizeRemaining(
            listOf(
                Stop("Must first", lat = 46.0, lng = 21.0, fixed = true),
                Stop("Near", lat = 45.76, lng = 21.23),
            ),
            isCompleted = { false },
            latitude = { it.lat },
            longitude = { it.lng },
            startLatitude = 45.75,
            startLongitude = 21.23,
            isFixedOrder = { it.fixed },
        )
        assertEquals("Must first", ordered.first().name)
        assertEquals(listOf("Must first", "Near"), ordered.map { it.name })
    }

    @Test
    fun optimizeRemaining_repairsLateTimeWindow() {
        data class Stop(
            val name: String,
            val lat: Double,
            val lng: Double,
            val deadlineMs: Long,
        )
        val now = 1_700_000_000_000L
        val near = Stop("Near", 45.76, 21.23, now + 3 * 60 * 60 * 1000L)
        val far = Stop("Far", 46.0, 21.0, now + 30 * 60 * 1000L)
        val ordered = RouteOrderOptimizer.optimizeRemaining(
            listOf(near, far),
            isCompleted = { false },
            latitude = { it.lat },
            longitude = { it.lng },
            startLatitude = 45.75,
            startLongitude = 21.23,
            arriveByEpochMs = { it.deadlineMs },
            nowEpochMs = now,
        )
        assertEquals("Far", ordered.first().name)
    }
}
