package com.danielcioban.routeplanner.ui.nav

import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.routing.DrivingRoute
import com.danielcioban.routeplanner.ui.map.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutingBannerTest {
    private val cached = DrivingRoute(
        coordinates = listOf(LatLng(45.75, 21.22), LatLng(45.76, 21.23)),
        distanceMeters = 100.0,
        durationSeconds = 20.0,
        steps = emptyList(),
        isCached = true,
    )

    @Test
    fun cached_offline_usesNetworkDownCopy() {
        assertEquals(
            R.string.nav_cached_route,
            routingBannerRes(cached, failureMessage = null, online = false),
        )
    }

    @Test
    fun cached_online_doesNotClaimNetworkDown() {
        assertEquals(
            R.string.nav_cached_route_online,
            routingBannerRes(cached, failureMessage = null, online = true),
        )
    }

    @Test
    fun liveRoadPath_hasNoBanner() {
        val live = cached.copy(isCached = false)
        assertNull(routingBannerRes(live, failureMessage = null, online = true))
    }
}
