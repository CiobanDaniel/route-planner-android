package com.danielcioban.routeplanner.data.routing

import com.danielcioban.routeplanner.ui.map.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutePolylineCacheTest {
    @Test
    fun putGet_marksCachedAndRoundsKey() {
        val cache = RoutePolylineCache(maxEntries = 2)
        val from = LatLng(45.7501, 21.2266)
        val to = LatLng(45.7602, 21.2301)
        val route = DrivingRoute(
            coordinates = listOf(from, to),
            distanceMeters = 100.0,
            durationSeconds = 20.0,
            steps = emptyList(),
        )
        cache.put(RoutePolylineCache.key(from, to, "driving", null), route)
        val nearbyFrom = LatLng(45.7504, 21.2267)
        val hit = cache.get(RoutePolylineCache.key(nearbyFrom, to, "driving", null))
        assertNotNull(hit)
        assertTrue(hit!!.isCached)
        assertFalse(hit.isApproximate)
    }

    @Test
    fun skipsApproximate_andEvicts() {
        val cache = RoutePolylineCache(maxEntries = 1)
        val a = LatLng(1.0, 1.0)
        val b = LatLng(2.0, 2.0)
        cache.put(
            RoutePolylineCache.key(a, b, "driving", null),
            DrivingRoute.straightLine(a, b, "", ""),
        )
        assertNull(cache.get(RoutePolylineCache.key(a, b, "driving", null)))

        val road = DrivingRoute(
            coordinates = listOf(a, b),
            distanceMeters = 10.0,
            durationSeconds = 5.0,
            steps = emptyList(),
        )
        cache.put(RoutePolylineCache.key(a, b, "driving", null), road)
        cache.put(
            RoutePolylineCache.key(LatLng(3.0, 3.0), LatLng(4.0, 4.0), "driving", null),
            road.copy(coordinates = listOf(LatLng(3.0, 3.0), LatLng(4.0, 4.0))),
        )
        assertNull(cache.get(RoutePolylineCache.key(a, b, "driving", null)))
    }
}

class DrivingRouteRecoverTest {
    @Test
    fun recoverAfterFailure_keepsPreviousRoad_notStraightLine() {
        val previous = DrivingRoute(
            coordinates = listOf(LatLng(45.75, 21.22), LatLng(45.76, 21.23)),
            distanceMeters = 50.0,
            durationSeconds = 10.0,
            steps = emptyList(),
        )
        val recovered = DrivingRoute.recoverAfterFailure(
            previous,
            LatLng(45.751, 21.221),
            LatLng(45.76, 21.23),
        )
        assertTrue(recovered.isCached)
        assertEquals(2, recovered.coordinates.size)
        val miss = DrivingRoute.recoverAfterFailure(null, LatLng(1.0, 1.0), LatLng(2.0, 2.0))
        assertTrue(miss.isApproximate)
    }
}
