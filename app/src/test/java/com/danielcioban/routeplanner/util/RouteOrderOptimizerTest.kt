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
}
