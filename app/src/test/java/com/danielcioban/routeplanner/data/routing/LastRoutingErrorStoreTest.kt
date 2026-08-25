package com.danielcioban.routeplanner.data.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastRoutingErrorStoreTest {
    @Test
    fun record_keepsLatestInMemoryWithoutApplication() {
        LastRoutingErrorStore.clear()
        LastRoutingErrorStore.record("routing_timeout", "https://router.example")
        val snap = LastRoutingErrorStore.snapshot()!!
        assertEquals("routing_timeout", snap.summary)
        assertEquals("https://router.example", snap.host)
        LastRoutingErrorStore.record("routing_rate_limit", "https://other.example")
        assertEquals("routing_rate_limit", LastRoutingErrorStore.snapshot()!!.summary)
        LastRoutingErrorStore.clear()
        assertNull(LastRoutingErrorStore.snapshot())
    }

    @Test
    fun record_blankSummary_usesFallback() {
        LastRoutingErrorStore.clear()
        LastRoutingErrorStore.record("   ")
        assertEquals("routing_failed", LastRoutingErrorStore.snapshot()!!.summary)
        LastRoutingErrorStore.clear()
    }
}
