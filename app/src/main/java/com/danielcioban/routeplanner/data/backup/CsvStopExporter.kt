package com.danielcioban.routeplanner.data.backup

import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import com.danielcioban.routeplanner.util.OpenLocationCode

/** CSV of library stops; columns match [CsvStopImporter] header aliases. */
object CsvStopExporter {
    fun libraryCsv(stops: List<StopLibraryEntity>): String {
        val active = stops.filter { it.deletedAtEpochMs == null }
        val sb = StringBuilder()
        sb.appendLine("name,latitude,longitude,address,notes,tags,plus_code")
        for (stop in active) {
            val plus = stop.plusCode.ifBlank {
                OpenLocationCode.encode(stop.latitude, stop.longitude)
            }
            sb.append(escape(stop.name)).append(',')
            sb.append(stop.latitude).append(',')
            sb.append(stop.longitude).append(',')
            sb.append(escape(stop.addressHint)).append(',')
            sb.append(escape(stop.notes)).append(',')
            sb.append(escape(stop.tags)).append(',')
            sb.append(escape(plus)).append('\n')
        }
        return sb.toString()
    }

    fun escape(value: String): String {
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }
}
