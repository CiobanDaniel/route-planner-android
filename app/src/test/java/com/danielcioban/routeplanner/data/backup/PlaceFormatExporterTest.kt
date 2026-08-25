package com.danielcioban.routeplanner.data.backup

import com.danielcioban.routeplanner.data.local.RouteEntity
import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.data.local.StopEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceFormatExporterTest {
    @Test
    fun routeGpxContainsWaypoint() {
        val gpx = PlaceFormatExporter.routeGpx(sampleRoute())
        assertTrue(gpx.contains("<wpt lat=\"45.75\" lon=\"21.23\">"))
        assertTrue(gpx.contains("Bakery"))
    }

    @Test
    fun routeGeoJsonIsFeatureCollection() {
        val json = PlaceFormatExporter.routeGeoJson(sampleRoute())
        assertTrue(json.contains("\"FeatureCollection\""))
        assertTrue(json.contains("Bakery"))
        assertTrue(json.contains("plusCode"))
    }

    private fun sampleRoute(): RouteWithStops {
        val route = RouteEntity(id = 1, name = "Morning", notes = "")
        val stop = StopEntity(
            id = 1,
            routeId = 1,
            position = 0,
            name = "Bakery",
            addressHint = "Main St",
            latitude = 45.75,
            longitude = 21.23,
        )
        return RouteWithStops(route = route, stops = listOf(stop))
    }
}
