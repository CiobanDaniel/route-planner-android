package com.danielcioban.routeplanner.data.routing

import com.danielcioban.routeplanner.ui.map.LatLng
import com.danielcioban.routeplanner.util.GeoUtils

data class ManeuverStep(
    val instruction: String,
    val type: String,
    val modifier: String?,
    val name: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val location: LatLng,
)

data class DrivingRoute(
    val coordinates: List<LatLng>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val steps: List<ManeuverStep>,
    /** True when this is a haversine fallback (no road geometry from OSRM). */
    val isApproximate: Boolean = false,
) {
    fun toLineGeoJson(): String {
        val coords = coordinates.joinToString(",") { "[${it.longitude},${it.latitude}]" }
        return """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[$coords]}}]}"""
    }

    companion object {
        /** Straight-line fallback when road routing is unavailable. */
        fun straightLine(
            from: LatLng,
            to: LatLng,
            headInstruction: String,
            arriveInstruction: String,
        ): DrivingRoute {
            val distance = GeoUtils.distanceMeters(
                from.latitude,
                from.longitude,
                to.latitude,
                to.longitude,
            )
            // ~40 km/h average for ETA when roads are unknown.
            val duration = distance / 11.0
            return DrivingRoute(
                coordinates = listOf(from, to),
                distanceMeters = distance,
                durationSeconds = duration,
                isApproximate = true,
                steps = listOf(
                    ManeuverStep(
                        instruction = headInstruction,
                        type = "depart",
                        modifier = null,
                        name = "",
                        distanceMeters = distance,
                        durationSeconds = duration,
                        location = from,
                    ),
                    ManeuverStep(
                        instruction = arriveInstruction,
                        type = "arrive",
                        modifier = null,
                        name = "",
                        distanceMeters = 0.0,
                        durationSeconds = 0.0,
                        location = to,
                    ),
                ),
            )
        }
    }
}
