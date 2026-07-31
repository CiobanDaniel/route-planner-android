package com.danielcioban.routeplanner.data.routing

import com.danielcioban.routeplanner.ui.map.LatLng

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
) {
    fun toLineGeoJson(): String {
        val coords = coordinates.joinToString(",") { "[${it.longitude},${it.latitude}]" }
        return """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[$coords]}}]}"""
    }
}
