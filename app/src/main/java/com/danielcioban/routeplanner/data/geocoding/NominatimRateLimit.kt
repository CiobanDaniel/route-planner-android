package com.danielcioban.routeplanner.data.geocoding

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Nominatim usage policy: at most one request per second per User-Agent. */
object NominatimRateLimit {
    const val MIN_INTERVAL_MS = 1_100L

    private val mutex = Mutex()
    private var lastAtEpochMs = 0L

    suspend fun awaitTurn(nowMs: Long = System.currentTimeMillis()) {
        mutex.withLock {
            val wait = lastAtEpochMs + MIN_INTERVAL_MS - nowMs
            if (wait > 0) delay(wait)
            lastAtEpochMs = System.currentTimeMillis()
        }
    }

    internal fun resetForTests() {
        lastAtEpochMs = 0L
    }
}
