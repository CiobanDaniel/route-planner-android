package com.danielcioban.routeplanner.util

import com.danielcioban.routeplanner.data.settings.DistanceUnit
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Straight-line (haversine) helpers for off-road approximation when mapped roads end. */
object GeoUtils {
    private const val earthRadiusMeters = 6_371_000.0

    fun distanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusMeters * c
    }

    /** Compass bearing in degrees: 0 = north, 90 = east. */
    fun bearingDegrees(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
        val bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360) % 360
    }

    fun formatDistance(
        meters: Double,
        unit: DistanceUnit = DistanceUnit.METRIC,
    ): String {
        return when (unit) {
            DistanceUnit.METRIC -> if (meters < 1000) {
                "${meters.roundToInt()} m"
            } else {
                String.format(Locale.getDefault(), "%.1f km", meters / 1000.0)
            }
            DistanceUnit.IMPERIAL -> {
                val feet = meters * 3.28084
                if (feet < 528) {
                    "${feet.roundToInt()} ft"
                } else {
                    String.format(Locale.getDefault(), "%.1f mi", meters / 1609.344)
                }
            }
        }
    }

    fun formatBearing(degrees: Double): String {
        val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((degrees + 22.5) / 45.0).toInt() % 8
        return "${dirs[index]} (${degrees.roundToInt()}°)"
    }

    fun formatClockMinutes(
        minutesFromMidnight: Int,
        use24Hour: Boolean = true,
        locale: Locale = Locale.getDefault(),
    ): String {
        val clamped = minutesFromMidnight.coerceIn(0, 23 * 60 + 59)
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, clamped / 60)
            set(java.util.Calendar.MINUTE, clamped % 60)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val pattern = if (use24Hour) "HH:mm" else "h:mm a"
        return java.text.SimpleDateFormat(pattern, locale).format(cal.time)
    }

    fun formatDurationMinutes(totalMinutes: Int): String {
        val minutes = totalMinutes.coerceAtLeast(0)
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            hours <= 0 -> "${rest} min"
            rest == 0 -> "${hours} h"
            else -> "${hours} h ${rest} min"
        }
    }

    fun formatClockFromEpoch(
        epochMs: Long,
        use24Hour: Boolean = true,
        locale: Locale = Locale.getDefault(),
    ): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochMs }
        return formatClockMinutes(
            cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE),
            use24Hour,
            locale,
        )
    }

    fun formatDateTime(
        epochMs: Long,
        use24Hour: Boolean = true,
        locale: Locale = Locale.getDefault(),
    ): String {
        val date = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, locale)
            .format(java.util.Date(epochMs))
        return "$date, ${formatClockFromEpoch(epochMs, use24Hour, locale)}"
    }
}
