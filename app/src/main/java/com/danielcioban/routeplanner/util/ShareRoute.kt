package com.danielcioban.routeplanner.util

import android.content.Context
import android.content.Intent
import com.danielcioban.routeplanner.data.local.RouteWithStops

object ShareRoute {
    fun buildShareText(routeWithStops: RouteWithStops): String {
        val route = routeWithStops.route
        val stops = routeWithStops.orderedStops
        return buildString {
            appendLine(route.name.trim().ifEmpty { "Route" })
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
                val lat = stop.latitude
                val lng = stop.longitude
                if (lat != null && lng != null) {
                    appendLine("   ${"%.5f".format(lat)}, ${"%.5f".format(lng)}")
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
        val text = buildShareText(routeWithStops)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, routeWithStops.route.name)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, chooserTitle))
    }
}
