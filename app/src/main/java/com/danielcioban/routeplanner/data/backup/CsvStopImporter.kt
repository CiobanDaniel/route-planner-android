package com.danielcioban.routeplanner.data.backup

import com.danielcioban.routeplanner.util.OpenLocationCode

data class ParsedCsvStop(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val addressHint: String = "",
    val notes: String = "",
    val tags: String = "",
)

data class CsvParseResult(
    val stops: List<ParsedCsvStop>,
    val skipped: Int,
)

data class CsvImportResult(
    val routeId: Long,
    val routeName: String,
    val imported: Int,
    val skipped: Int,
)

/** Flexible CSV/TSV stop list: header aliases, comma or semicolon, quoted fields. */
object CsvStopImporter {
    fun parse(text: String): CsvParseResult {
        val raw = text.removePrefix("\uFEFF").trim()
        if (raw.isBlank()) return CsvParseResult(emptyList(), 0)
        val lines = raw.split(Regex("\\r\\n|\\n|\\r")).filter { it.isNotBlank() }
        if (lines.isEmpty()) return CsvParseResult(emptyList(), 0)
        val delimiter = detectDelimiter(lines.first())
        val rows = lines.map { parseCsvLine(it, delimiter) }
            .filter { row -> row.any { cell -> cell.isNotBlank() } }
        if (rows.isEmpty()) return CsvParseResult(emptyList(), 0)

        val header = rows.first().map { it.trim().lowercase() }
        val headerCols = columnsFromHeader(header)
        val hasHeader = (headerCols.lat >= 0 && headerCols.lng >= 0) || headerCols.plusCode >= 0
        val cols = if (hasHeader) headerCols else inferColumns(rows.first())
        if ((cols.lat < 0 || cols.lng < 0) && cols.plusCode < 0) {
            return CsvParseResult(emptyList(), rows.size)
        }
        val dataRows = if (hasHeader) rows.drop(1) else rows
        val stops = mutableListOf<ParsedCsvStop>()
        var skipped = 0
        dataRows.forEachIndexed { index, row ->
            var lat = cols.lat.takeIf { it >= 0 }?.let { parseCoord(row.getOrElse(it) { "" }) }
            var lng = cols.lng.takeIf { it >= 0 }?.let { parseCoord(row.getOrElse(it) { "" }) }
            if (lat == null || lng == null) {
                val plus = cols.plusCode.takeIf { it >= 0 }?.let { row.getOrElse(it) { "" }.trim() }.orEmpty()
                OpenLocationCode.decode(plus)?.let { decoded ->
                    lat = decoded.latitude
                    lng = decoded.longitude
                }
            }
            val resolvedLat = lat
            val resolvedLng = lng
            if (resolvedLat == null || resolvedLng == null ||
                resolvedLat !in -90.0..90.0 ||
                resolvedLng !in -180.0..180.0
            ) {
                skipped += 1
                return@forEachIndexed
            }
            val address = cols.address.takeIf { it >= 0 }?.let { row.getOrElse(it) { "" }.trim() }.orEmpty()
            val notes = cols.notes.takeIf { it >= 0 }?.let { row.getOrElse(it) { "" }.trim() }.orEmpty()
            val tags = cols.tags.takeIf { it >= 0 }?.let { row.getOrElse(it) { "" }.trim() }.orEmpty()
            val rawName = cols.name.takeIf { it >= 0 }?.let { row.getOrElse(it) { "" }.trim() }.orEmpty()
            val name = rawName.ifBlank { address.ifBlank { "Stop ${index + 1}" } }
            stops += ParsedCsvStop(
                name = name,
                latitude = resolvedLat,
                longitude = resolvedLng,
                addressHint = address,
                notes = notes,
                tags = tags,
            )
        }
        return CsvParseResult(stops = stops, skipped = skipped)
    }

    internal fun detectDelimiter(firstLine: String): Char {
        var commas = 0
        var semis = 0
        var tabs = 0
        var inQuotes = false
        firstLine.forEach { c ->
            when {
                c == '"' -> inQuotes = !inQuotes
                inQuotes -> Unit
                c == ',' -> commas++
                c == ';' -> semis++
                c == '\t' -> tabs++
            }
        }
        return when {
            tabs > 0 && tabs >= commas && tabs >= semis -> '\t'
            semis > commas -> ';'
            else -> ','
        }
    }

    internal fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>()
        val buf = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        buf.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    out += buf.toString()
                    buf.clear()
                }
                else -> buf.append(c)
            }
            i++
        }
        out += buf.toString()
        return out
    }

    internal fun parseCoord(raw: String): Double? {
        val trimmed = raw.trim().replace(" ", "")
        if (trimmed.isEmpty()) return null
        val normalized = if (trimmed.count { it == ',' } == 1 && !trimmed.contains('.')) {
            trimmed.replace(',', '.')
        } else {
            trimmed
        }
        return normalized.toDoubleOrNull()
    }

    private data class Columns(
        val name: Int,
        val lat: Int,
        val lng: Int,
        val address: Int,
        val notes: Int,
        val tags: Int,
        val plusCode: Int,
    )

    private fun columnsFromHeader(header: List<String>): Columns {
        fun index(vararg aliases: String): Int =
            header.indexOfFirst { cell -> aliases.any { alias -> cell == alias || cell.replace('_', ' ') == alias } }

        return Columns(
            name = index("name", "title", "stop", "stop name", "label", "nume"),
            lat = index("lat", "latitude", "y"),
            lng = index("lon", "lng", "long", "longitude", "x"),
            address = index("address", "addresshint", "address hint", "street"),
            notes = index("notes", "note", "description", "desc"),
            tags = index("tags", "tag", "zone", "zones"),
            plusCode = index("plus_code", "plus code", "pluscode", "olc"),
        )
    }

    private fun inferColumns(first: List<String>): Columns {
        val nums = first.map { parseCoord(it) != null }
        return when {
            first.size >= 3 && !nums.getOrElse(0) { false } &&
                nums.getOrElse(1) { false } && nums.getOrElse(2) { false } ->
                Columns(name = 0, lat = 1, lng = 2, address = -1, notes = -1, tags = -1, plusCode = -1)
            first.size >= 3 && nums.getOrElse(0) { false } && nums.getOrElse(1) { false } ->
                Columns(name = 2, lat = 0, lng = 1, address = -1, notes = -1, tags = -1, plusCode = -1)
            first.size >= 2 && nums.getOrElse(0) { false } && nums.getOrElse(1) { false } ->
                Columns(name = -1, lat = 0, lng = 1, address = -1, notes = -1, tags = -1, plusCode = -1)
            else -> Columns(name = -1, lat = -1, lng = -1, address = -1, notes = -1, tags = -1, plusCode = -1)
        }
    }
}
