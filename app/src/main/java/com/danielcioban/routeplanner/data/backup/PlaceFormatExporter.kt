package com.danielcioban.routeplanner.data.backup

import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import com.danielcioban.routeplanner.util.OpenLocationCode

object PlaceFormatExporter {
    fun routeCsv(route: RouteWithStops): String {
        val sb = StringBuilder()
        sb.appendLine("name,latitude,longitude,address,notes,plus_code")
        route.deliveryStops.forEach { stop ->
            val lat = stop.latitude
            val lng = stop.longitude
            val plus = if (lat != null && lng != null) OpenLocationCode.encode(lat, lng) else ""
            sb.append(CsvStopExporter.escape(stop.name)).append(',')
            sb.append(lat ?: "").append(',')
            sb.append(lng ?: "").append(',')
            sb.append(CsvStopExporter.escape(stop.addressHint)).append(',')
            sb.append(CsvStopExporter.escape(stop.notes)).append(',')
            sb.append(CsvStopExporter.escape(plus)).append('\n')
        }
        return sb.toString()
    }

    fun libraryGpx(stops: List<StopLibraryEntity>): String = gpx(
        name = "Library",
        points = stops.filter { it.deletedAtEpochMs == null }.map {
            NamedPoint(it.name, it.latitude, it.longitude, it.addressHint)
        },
    )

    fun routeGpx(route: RouteWithStops): String = gpx(
        name = route.route.name,
        points = route.deliveryStops.mapNotNull { stop ->
            val lat = stop.latitude ?: return@mapNotNull null
            val lng = stop.longitude ?: return@mapNotNull null
            NamedPoint(stop.name, lat, lng, stop.addressHint)
        },
    )

    fun libraryKml(stops: List<StopLibraryEntity>): String = kml(
        name = "Library",
        points = stops.filter { it.deletedAtEpochMs == null }.map {
            NamedPoint(it.name, it.latitude, it.longitude, it.addressHint)
        },
    )

    fun routeKml(route: RouteWithStops): String = kml(
        name = route.route.name,
        points = route.deliveryStops.mapNotNull { stop ->
            val lat = stop.latitude ?: return@mapNotNull null
            val lng = stop.longitude ?: return@mapNotNull null
            NamedPoint(stop.name, lat, lng, stop.addressHint)
        },
    )

    fun libraryGeoJson(stops: List<StopLibraryEntity>): String = geoJson(
        stops.filter { it.deletedAtEpochMs == null }.map {
            NamedPoint(it.name, it.latitude, it.longitude, it.addressHint)
        },
    )

    fun routeGeoJson(route: RouteWithStops): String = geoJson(
        route.deliveryStops.mapNotNull { stop ->
            val lat = stop.latitude ?: return@mapNotNull null
            val lng = stop.longitude ?: return@mapNotNull null
            NamedPoint(stop.name, lat, lng, stop.addressHint)
        },
    )

    private data class NamedPoint(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val address: String,
    )

    private fun escapeXml(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun gpx(name: String, points: List<NamedPoint>): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<gpx version="1.1" creator="Route Planner" xmlns="http://www.topografix.com/GPX/1/1">""")
        appendLine("  <metadata><name>${escapeXml(name)}</name></metadata>")
        points.forEach { point ->
            appendLine(
                """  <wpt lat="${point.latitude}" lon="${point.longitude}"><name>${escapeXml(point.name)}</name><desc>${escapeXml(point.address)}</desc></wpt>""",
            )
        }
        appendLine("</gpx>")
    }

    private fun kml(name: String, points: List<NamedPoint>): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<kml xmlns="http://www.opengis.net/kml/2.2"><Document>""")
        appendLine("  <name>${escapeXml(name)}</name>")
        points.forEach { point ->
            appendLine("  <Placemark>")
            appendLine("    <name>${escapeXml(point.name)}</name>")
            appendLine("    <description>${escapeXml(point.address)}</description>")
            appendLine("    <Point><coordinates>${point.longitude},${point.latitude},0</coordinates></Point>")
            appendLine("  </Placemark>")
        }
        appendLine("</Document></kml>")
    }

    private fun escapeJson(value: String): String = buildString(value.length) {
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }

    private fun geoJson(points: List<NamedPoint>): String = buildString {
        appendLine("{")
        appendLine("  \"type\": \"FeatureCollection\",")
        appendLine("  \"features\": [")
        points.forEachIndexed { index, point ->
            val plus = OpenLocationCode.encode(point.latitude, point.longitude)
            appendLine("    {")
            appendLine("      \"type\": \"Feature\",")
            appendLine("      \"geometry\": {")
            appendLine("        \"type\": \"Point\",")
            appendLine("        \"coordinates\": [${point.longitude}, ${point.latitude}]")
            appendLine("      },")
            appendLine("      \"properties\": {")
            appendLine("        \"name\": \"${escapeJson(point.name)}\",")
            appendLine("        \"address\": \"${escapeJson(point.address)}\",")
            appendLine("        \"plusCode\": \"${escapeJson(plus)}\"")
            appendLine("      }")
            append("    }")
            if (index != points.lastIndex) append(',')
            appendLine()
        }
        appendLine("  ]")
        append("}")
    }
}
