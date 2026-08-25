package com.danielcioban.routeplanner.util

import android.content.Context
import android.content.Intent
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.RoutePlannerApplication
import com.danielcioban.routeplanner.data.local.RouteWithStops

object ShareRoute {
    fun buildShareText(context: Context, routeWithStops: RouteWithStops): String {
        val use24Hour = (context.applicationContext as? RoutePlannerApplication)
            ?.latestSettings?.clockFormat?.is24Hour(context) ?: true
        val route = routeWithStops.route
        val stops = routeWithStops.orderedStops
        return buildString {
            appendLine(route.name.trim().ifEmpty { context.getString(R.string.route_fallback_name) })
            if (route.roundTrip) {
                appendLine(context.getString(R.string.route_round_trip_badge))
            }
            if (route.notes.isNotBlank()) {
                appendLine(route.notes.trim())
            }
            appendLine()
            stops.forEachIndexed { index, stop ->
                append("${index + 1}. ${stop.name.trim()}")
                if (stop.isCompleted) append(" ✓")
                appendLine()
                if (stop.addressHint.isNotBlank()) {
                    appendLine("   ${stop.addressHint.trim()}")
                }
                stop.arriveByMinutes?.let { minutes ->
                    appendLine(
                        "   ${context.getString(R.string.stop_arrive_by_set, GeoUtils.formatClockMinutes(minutes, use24Hour))}",
                    )
                }
                if (stop.serviceMinutes > 0) {
                    appendLine(
                        "   ${context.getString(R.string.stop_service_minutes)}: ${stop.serviceMinutes}",
                    )
                }
                val lat = stop.latitude
                val lng = stop.longitude
                if (lat != null && lng != null) {
                    appendLine("   ${"%.5f".format(lat)}, ${"%.5f".format(lng)}")
                    appendLine("   ${OpenLocationCode.encode(lat, lng)}")
                    appendLine("   https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                }
                if (stop.notes.isNotBlank()) {
                    appendLine("   ${stop.notes.trim()}")
                }
                if (index != stops.lastIndex) appendLine()
            }
        }.trimEnd()
    }

    fun share(context: Context, routeWithStops: RouteWithStops, chooserTitle: String) {
        val text = buildShareText(context, routeWithStops)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, routeWithStops.route.name)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, chooserTitle))
    }
}
