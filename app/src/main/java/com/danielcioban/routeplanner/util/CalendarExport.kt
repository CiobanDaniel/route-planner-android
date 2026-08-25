package com.danielcioban.routeplanner.util

import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.data.local.StopEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Build a minimal iCalendar for promised stop times. */
object CalendarExport {
    fun icsForRoute(route: RouteWithStops, nowMs: Long = System.currentTimeMillis()): String {
        val stamp = formatUtc(nowMs)
        val events = route.deliveryStops.mapNotNull { stop ->
            val start = eventStartMs(stop) ?: return@mapNotNull null
            event(stop, start, stamp)
        }
        return buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//Route Planner//Courier//EN")
            appendLine("CALSCALE:GREGORIAN")
            events.forEach { append(it) }
            appendLine("END:VCALENDAR")
        }
    }

    fun eventStartMs(stop: StopEntity, nowMs: Long = System.currentTimeMillis()): Long? {
        stop.arriveByEpochMs?.let { return it }
        val minutes = stop.arriveByMinutes ?: return null
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMs }
        cal.set(java.util.Calendar.HOUR_OF_DAY, minutes / 60)
        cal.set(java.util.Calendar.MINUTE, minutes % 60)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun event(stop: StopEntity, startMs: Long, stamp: String): String {
        val endMs = startMs + stop.serviceMinutes.coerceAtLeast(15) * 60_000L
        val summary = escape(stop.name.ifBlank { "Stop" })
        val desc = escape(
            listOf(stop.addressHint, stop.phone, stop.notes)
                .filter { it.isNotBlank() }
                .joinToString("\\n"),
        )
        return buildString {
            appendLine("BEGIN:VEVENT")
            appendLine("UID:${stop.remoteId}@routeplanner.local")
            appendLine("DTSTAMP:$stamp")
            appendLine("DTSTART:${formatUtc(startMs)}")
            appendLine("DTEND:${formatUtc(endMs)}")
            appendLine("SUMMARY:$summary")
            if (desc.isNotBlank()) appendLine("DESCRIPTION:$desc")
            val lat = stop.latitude
            val lng = stop.longitude
            if (lat != null && lng != null) {
                appendLine("GEO:$lat;$lng")
            }
            appendLine("END:VEVENT")
        }
    }

    private fun formatUtc(epochMs: Long): String {
        val fmt = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(epochMs))
    }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")
}
