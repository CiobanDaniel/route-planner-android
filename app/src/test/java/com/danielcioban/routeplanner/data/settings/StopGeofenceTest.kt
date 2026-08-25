package com.danielcioban.routeplanner.data.settings

import com.danielcioban.routeplanner.util.GeoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StopGeofenceTest {
    @Test
    fun clampRadius_enforcesMinAndMax() {
        assertEquals(StopGeofence.MIN_RADIUS_METERS, StopGeofence.clampRadius(1))
        assertEquals(StopGeofence.MAX_RADIUS_METERS, StopGeofence.clampRadius(9_999))
        assertEquals(80, StopGeofence.clampRadius(80))
    }

    @Test
    fun parseRadius_emptyOrInvalid_isNull() {
        assertNull(StopGeofence.parseRadius(""))
        assertNull(StopGeofence.parseRadius("  "))
        assertNull(StopGeofence.parseRadius("nope"))
        assertEquals(50, StopGeofence.parseRadius(" 50 "))
    }

    @Test
    fun effectiveAction_routeOverridesInherit() {
        assertEquals(
            GeofenceAction.VISITED,
            StopGeofence.effectiveAction(RouteGeofenceMode.INHERIT, GeofenceAction.VISITED),
        )
        assertEquals(
            GeofenceAction.OFF,
            StopGeofence.effectiveAction(RouteGeofenceMode.OFF, GeofenceAction.COMPLETE),
        )
        assertEquals(
            GeofenceAction.COMPLETE,
            StopGeofence.effectiveAction(RouteGeofenceMode.COMPLETE, GeofenceAction.OFF),
        )
    }

    @Test
    fun effectiveRadius_stopThenRouteThenGlobal() {
        assertEquals(80, StopGeofence.effectiveRadiusMeters(80, 40, 50))
        assertEquals(40, StopGeofence.effectiveRadiusMeters(null, 40, 50))
        assertEquals(50, StopGeofence.effectiveRadiusMeters(null, null, 50))
    }

    @Test
    fun isInside_falseWhenAccuracyWorseThanRadius() {
        val lat = 45.75
        val lng = 21.23
        assertFalse(
            StopGeofence.isInside(
                userLatitude = lat,
                userLongitude = lng,
                stopLatitude = lat,
                stopLongitude = lng,
                radiusMeters = 50,
                accuracyMeters = 80f,
            ),
        )
        assertTrue(
            StopGeofence.isInside(
                userLatitude = lat,
                userLongitude = lng,
                stopLatitude = lat,
                stopLongitude = lng,
                radiusMeters = 50,
                accuracyMeters = 10f,
            ),
        )
    }

    @Test
    fun isInside_usesHaversineDistance() {
        val stopLat = 45.75
        val stopLng = 21.23
        val radius = 50
        val north40 = stopLat + 40.0 / 111_320.0
        val north80 = stopLat + 80.0 / 111_320.0
        assertTrue(
            StopGeofence.isInside(north40, stopLng, stopLat, stopLng, radius, accuracyMeters = null),
        )
        assertFalse(
            StopGeofence.isInside(north80, stopLng, stopLat, stopLng, radius, accuracyMeters = null),
        )
        assertEquals(
            40.0,
            GeoUtils.distanceMeters(north40, stopLng, stopLat, stopLng),
            2.0,
        )
    }

    @Test
    fun dwellSeconds_snapsToAllowedOptions() {
        assertEquals(0, GeofenceDwell.clampSeconds(0))
        assertEquals(3, GeofenceDwell.clampSeconds(4))
        assertEquals(5, GeofenceDwell.clampSeconds(6))
        assertEquals(10, GeofenceDwell.clampSeconds(12))
    }
}
