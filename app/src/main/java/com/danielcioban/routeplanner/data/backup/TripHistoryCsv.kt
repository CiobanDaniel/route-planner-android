package com.danielcioban.routeplanner.data.backup

import com.danielcioban.routeplanner.data.local.TripHistoryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TripHistoryCsv {
    fun export(trips: List<TripHistoryEntity>): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        return buildString {
            appendLine("started,ended,kind,status,title,stops_completed,stops_total,distance_m,late_stops")
            trips.forEach { trip ->
                append(csv(fmt.format(Date(trip.startedAtEpochMs)))).append(',')
                append(
                    csv(trip.endedAtEpochMs?.let { fmt.format(Date(it)) }.orEmpty()),
                ).append(',')
                append(csv(trip.kind)).append(',')
                append(csv(trip.status)).append(',')
                append(csv(trip.title.ifBlank { trip.destName })).append(',')
                append(trip.stopsCompleted).append(',')
                append(trip.stopsTotal).append(',')
                append("%.0f".format(Locale.US, trip.distanceMeters)).append(',')
                append(trip.lateStops)
                appendLine()
            }
        }
    }

    private fun csv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '"' || it == '\n' }) "\"$escaped\"" else escaped
    }
}
