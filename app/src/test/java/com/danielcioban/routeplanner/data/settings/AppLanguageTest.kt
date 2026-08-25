package com.danielcioban.routeplanner.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun fromTag_matchesPrimarySubtag() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag(""))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-US"))
        assertEquals(AppLanguage.ROMANIAN, AppLanguage.fromTag("ro"))
        assertEquals(AppLanguage.ROMANIAN, AppLanguage.fromTag("RO-RO"))
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromTag("fr-FR"))
        assertEquals(AppLanguage.GERMAN, AppLanguage.fromTag("de_DE"))
        assertEquals(AppLanguage.ITALIAN, AppLanguage.fromTag("it"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromTag("es-MX"))
        assertEquals(AppLanguage.PORTUGUESE, AppLanguage.fromTag("pt-BR"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("zh-CN"))
    }
}
