package com.danielcioban.routeplanner.ui.layout

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPanesTest {
    @Test
    fun isWide_matchesHomeRailBreakpoint() {
        assertFalse(AppPanes.isWide(699.dp))
        assertTrue(AppPanes.isWide(700.dp))
        assertTrue(AppPanes.isWide(1024.dp))
    }

    @Test
    fun titleChrome_isNarrowerThanReadableContent() {
        assertTrue(AppPanes.TitleMaxWidth < AppPanes.ReadableMaxWidth)
    }
}
