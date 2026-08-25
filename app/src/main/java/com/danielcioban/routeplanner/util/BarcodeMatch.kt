package com.danielcioban.routeplanner.util

import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import com.danielcioban.routeplanner.data.local.StopTaskEntity
import java.util.Locale

/** Parse a camera / typed barcode and match it to a stop, task, or map pin. */
object BarcodeMatch {
    private val GEO_REGEX = Regex(
        """^geo:(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)(?:\?(.*))?$""",
        RegexOption.IGNORE_CASE,
    )
    private val MAPS_QUERY = Regex(
        """[?&]q=(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)""",
        RegexOption.IGNORE_CASE,
    )
    private val LIBRARY_URI = Regex(
        """https?://routeplanner\.local/l/([A-Za-z0-9\-]+)""",
        RegexOption.IGNORE_CASE,
    )

    sealed class Result {
        data class Stop(val stopId: Long) : Result()
        data class Task(val stopId: Long, val taskId: Long) : Result()
        data class Place(val latitude: Double, val longitude: Double, val name: String) : Result()
        data class Library(val libraryRemoteId: String) : Result()
        data object None : Result()
    }

    fun libraryRemoteId(raw: String): String? =
        LIBRARY_URI.find(raw.trim())?.groupValues?.getOrNull(1)

    fun match(
        raw: String,
        stops: List<StopEntity>,
        tasksByStopId: Map<Long, List<StopTaskEntity>>,
        libraryById: Map<Long, StopLibraryEntity> = emptyMap(),
    ): Result {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return Result.None

        libraryRemoteId(trimmed)?.let { remote ->
            val stop = stops.firstOrNull { stop ->
                stop.libraryStopId?.let { libraryById[it]?.remoteId } == remote
            }
            if (stop != null) return Result.Stop(stop.id)
            return Result.Library(remote)
        }

        parseGeo(trimmed)?.let { (lat, lng, label) ->
            val near = stops.firstOrNull { stop ->
                val slat = stop.latitude ?: return@firstOrNull false
                val slng = stop.longitude ?: return@firstOrNull false
                GeoUtils.distanceMeters(lat, lng, slat, slng) <= 40.0
            }
            if (near != null) return Result.Stop(near.id)
            return Result.Place(lat, lng, label.ifBlank { "Pin" })
        }

        OpenLocationCode.decode(trimmed)?.let { decoded ->
            val near = stops.firstOrNull { stop ->
                val slat = stop.latitude ?: return@firstOrNull false
                val slng = stop.longitude ?: return@firstOrNull false
                GeoUtils.distanceMeters(decoded.latitude, decoded.longitude, slat, slng) <= 40.0
            }
            if (near != null) return Result.Stop(near.id)
            return Result.Place(decoded.latitude, decoded.longitude, trimmed.uppercase(Locale.US))
        }

        val needle = trimmed.lowercase(Locale.US)
        stops.forEach { stop ->
            if (fieldEquals(stop.barcode, trimmed) ||
                fieldEquals(stop.doorCode, trimmed) ||
                fieldEquals(stop.phone, trimmed) ||
                fieldEquals(stop.remoteId, trimmed) ||
                stop.name.equals(trimmed, ignoreCase = true)
            ) {
                return Result.Stop(stop.id)
            }
        }
        tasksByStopId.forEach { (stopId, tasks) ->
            val task = tasks.firstOrNull { task ->
                !task.isCompleted && (
                    task.title.equals(trimmed, ignoreCase = true) ||
                        task.title.lowercase(Locale.US).contains(needle)
                    )
            }
            if (task != null) return Result.Task(stopId, task.id)
        }
        val contains = stops.firstOrNull { stop ->
            stop.notes.contains(trimmed, ignoreCase = true) ||
                stop.addressHint.contains(trimmed, ignoreCase = true)
        }
        if (contains != null) return Result.Stop(contains.id)
        return Result.None
    }

    fun parseGeo(raw: String): Triple<Double, Double, String>? {
        GEO_REGEX.matchEntire(raw.trim())?.let { match ->
            val lat = match.groupValues[1].toDoubleOrNull() ?: return null
            val lng = match.groupValues[2].toDoubleOrNull() ?: return null
            val q = match.groupValues.getOrNull(3).orEmpty()
            val label = q.substringAfter("q=", "")
                .substringBefore('&')
                .replace('+', ' ')
            return Triple(lat, lng, label)
        }
        MAPS_QUERY.find(raw)?.let { match ->
            val lat = match.groupValues[1].toDoubleOrNull() ?: return null
            val lng = match.groupValues[2].toDoubleOrNull() ?: return null
            return Triple(lat, lng, "")
        }
        return null
    }

    private fun fieldEquals(value: String, raw: String): Boolean {
        val trimmed = value.trim()
        return trimmed.isNotEmpty() && trimmed.equals(raw, ignoreCase = true)
    }
}
