package com.danielcioban.routeplanner.ui.map

import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.local.StopLibraryEntity

object MapDefaults {
    // Fallback only — live maps start world-zoom then fly to LastKnownMapCenter / GPS.
    const val defaultLat = 20.0
    const val defaultLon = 0.0
    const val defaultZoom = 2.0
}

fun stopsToPointsGeoJson(stops: List<StopEntity>): String {
    val features = stops.mapNotNull { stop ->
        val lat = stop.latitude ?: return@mapNotNull null
        val lon = stop.longitude ?: return@mapNotNull null
        val name = escapeJson(stop.name)
        """
        {
          "type":"Feature",
          "geometry":{"type":"Point","coordinates":[$lon,$lat]},
          "properties":{
            "id":${stop.id},
            "name":"$name",
            "position":${stop.position},
            "completed":${stop.isCompleted}
          }
        }
        """.trimIndent()
    }
    return """{"type":"FeatureCollection","features":[${features.joinToString(",")}]}"""
}

fun stopsToLineGeoJson(stops: List<StopEntity>): String {
    val coords = stops
        .sortedBy { it.position }
        .mapNotNull { stop ->
            val lat = stop.latitude ?: return@mapNotNull null
            val lon = stop.longitude ?: return@mapNotNull null
            "[$lon,$lat]"
        }
    if (coords.size < 2) {
        return """{"type":"FeatureCollection","features":[]}"""
    }
    return """
    {
      "type":"FeatureCollection",
      "features":[{
        "type":"Feature",
        "geometry":{"type":"LineString","coordinates":[${coords.joinToString(",")}]},
        "properties":{}
      }]
    }
    """.trimIndent()
}

private fun escapeJson(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

fun libraryStopsToMapStops(stops: List<StopLibraryEntity>): List<StopEntity> =
    stops.mapIndexed { index, stop ->
        StopEntity(
            id = stop.id,
            routeId = 0L,
            position = index,
            name = stop.name,
            addressHint = stop.addressHint,
            notes = stop.notes,
            latitude = stop.latitude,
            longitude = stop.longitude,
        )
    }
