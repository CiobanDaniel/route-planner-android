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
    /** True when this geometry came from the last successful OSRM cache. */
    val isCached: Boolean = false,
) {
    fun toLineGeoJson(): String {
        val coords = coordinates.joinToString(",") { "[${it.longitude},${it.latitude}]" }
        return """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[$coords]}}]}"""
    }

    /** Append a walking leg from the street-snapped end to the building pin. */
    fun appendFootLeg(foot: DrivingRoute): DrivingRoute {
        if (foot.coordinates.size < 2) return this
        val overlap = coordinates.lastOrNull()
        val footStart = foot.coordinates.first()
        val skipFirst = overlap != null &&
            GeoUtils.distanceMeters(
                overlap.latitude,
                overlap.longitude,
                footStart.latitude,
                footStart.longitude,
            ) < 8.0
        val extra = if (skipFirst) foot.coordinates.drop(1) else foot.coordinates
        if (extra.isEmpty()) return this
        val vehicleSteps = steps.filterNot { it.type == "arrive" }
        return copy(
            coordinates = coordinates + extra,
            distanceMeters = distanceMeters + foot.distanceMeters,
            durationSeconds = durationSeconds + foot.durationSeconds,
            steps = vehicleSteps + foot.steps,
            isApproximate = isApproximate && foot.isApproximate,
        )
    }

    companion object {
        fun recoverAfterFailure(
            previous: DrivingRoute?,
            from: LatLng,
            to: LatLng,
        ): DrivingRoute {
            previous?.takeIf { !it.isApproximate && it.coordinates.size >= 2 }?.let {
                return it.copy(isCached = true)
            }
            return straightLine(from, to, "", "")
        }

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
