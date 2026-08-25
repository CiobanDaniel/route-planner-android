package com.danielcioban.routeplanner.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StringResourceParityTest {
    @Test
    fun shippedLocalesHaveTheSameKeysAsEnglish() {
        val res = File("src/main/res")
        val english = parseKeys(File(res, "values/strings.xml"))
        val locales = listOf(
            "values-ro",
            "values-fr",
            "values-de",
            "values-it",
            "values-es",
            "values-pt",
        )
        locales.forEach { folder ->
            val file = File(res, "$folder/strings.xml")
            assertTrue("missing $folder/strings.xml", file.isFile)
            val keys = parseKeys(file)
            assertEquals(
                "keys differ in $folder (missing=${english - keys} extra=${keys - english})",
                english,
                keys,
            )
        }
    }

    private fun parseKeys(file: File): Set<String> {
        val names = Regex("""<(?:string|plurals)\s+name="([^"]+)"""")
        return names.findAll(file.readText()).map { it.groupValues[1] }.toSet()
    }
}
