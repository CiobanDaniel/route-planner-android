package com.danielcioban.routeplanner.data.backup

import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvStopImporterTest {
    @Test
    fun headerComma_readsNamedStops() {
        val csv = """
            name,latitude,longitude,address,notes
            Bakery,45.75,21.23,Main St,Leave at door
            Depot,45.76,21.24,,
        """.trimIndent()
        val result = CsvStopImporter.parse(csv)
        assertEquals(2, result.stops.size)
        assertEquals(0, result.skipped)
        assertEquals("Bakery", result.stops[0].name)
        assertEquals(45.75, result.stops[0].latitude, 0.0001)
        assertEquals("Main St", result.stops[0].addressHint)
        assertEquals("Leave at door", result.stops[0].notes)
    }

    @Test
    fun semicolonAndDecimalComma() {
        val csv = "nume;lat;lng\nMagazin;45,75;21,23"
        val result = CsvStopImporter.parse(csv)
        // header aliases need lat/lng — "nume" is not a name alias, but lat/lng match
        assertEquals(1, result.stops.size)
        assertEquals(45.75, result.stops[0].latitude, 0.0001)
        assertEquals(21.23, result.stops[0].longitude, 0.0001)
    }

    @Test
    fun quotedNameWithComma() {
        val csv = """
            name,lat,lng
            "Cafe, Center",45.75,21.23
        """.trimIndent()
        val result = CsvStopImporter.parse(csv)
        assertEquals("Cafe, Center", result.stops.single().name)
    }

    @Test
    fun noHeaderNameLatLng() {
        val csv = "Warehouse,45.1,21.2"
        val result = CsvStopImporter.parse(csv)
        assertEquals("Warehouse", result.stops.single().name)
        assertEquals(45.1, result.stops.single().latitude, 0.0001)
    }

    @Test
    fun skipsRowsWithoutCoords() {
        val csv = """
            name,lat,lng
            Good,45.75,21.23
            Bad,,
        """.trimIndent()
        val result = CsvStopImporter.parse(csv)
        assertEquals(1, result.stops.size)
        assertEquals(1, result.skipped)
        assertEquals("Good", result.stops.single().name)
    }

    @Test
    fun bomAndAliases() {
        val csv = "\uFEFFtitle,y,x\nStop A,45.0,21.0"
        val result = CsvStopImporter.parse(csv)
        assertEquals("Stop A", result.stops.single().name)
        assertTrue(result.stops.single().longitude in 20.9..21.1)
    }

    @Test
    fun exportLibrary_roundTripsThroughImporter() {
        val csv = CsvStopExporter.libraryCsv(
            listOf(
                StopLibraryEntity(
                    name = "Cafe, Center",
                    addressHint = "Main St",
                    notes = "Leave at door",
                    latitude = 45.75,
                    longitude = 21.23,
                ),
            ),
        )
        val parsed = CsvStopImporter.parse(csv)
        assertEquals(1, parsed.stops.size)
        assertEquals("Cafe, Center", parsed.stops.single().name)
        assertEquals(45.75, parsed.stops.single().latitude, 0.0001)
        assertEquals("Main St", parsed.stops.single().addressHint)
        assertEquals("Leave at door", parsed.stops.single().notes)
    }

    @Test
    fun tagsAndPlusCodeColumns() {
        val csv = """
            name,latitude,longitude,tags,plus_code
            Cafe,45.75,21.23,"north, mall",
        """.trimIndent()
        val result = CsvStopImporter.parse(csv)
        assertEquals("north, mall", result.stops.single().tags)
    }
}
