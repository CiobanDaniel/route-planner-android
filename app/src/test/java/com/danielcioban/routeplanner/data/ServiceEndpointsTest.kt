package com.danielcioban.routeplanner.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceEndpointsTest {
    @Test
    fun candidates_dropsBlankAndDuplicates() {
        assertEquals(
            listOf("https://osrm.example"),
            ServiceEndpoints.candidates("https://osrm.example/", "https://osrm.example"),
        )
        assertEquals(
            listOf("https://a.example", "https://b.example"),
            ServiceEndpoints.candidates(" https://a.example ", "https://b.example/"),
        )
        assertEquals(
            listOf("https://only.example"),
            ServiceEndpoints.candidates("https://only.example", ""),
        )
    }

    @Test
    fun hostLabel_stripsScheme() {
        assertEquals("router.example", ServiceEndpoints.hostLabel("https://router.example/"))
    }
}
