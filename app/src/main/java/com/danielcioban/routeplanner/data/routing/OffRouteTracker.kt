package com.danielcioban.routeplanner.data.routing

import com.danielcioban.routeplanner.data.settings.RerouteAggressiveness

/** Consecutive off-route samples + cooldown before a recalc. */
class OffRouteTracker {
    private var hits: Int = 0
    var lastRecalcAtMs: Long = 0L
        private set

    fun shouldRecalc(
        distanceToRouteMeters: Double,
        policy: RerouteAggressiveness,
        nowMs: Long = System.currentTimeMillis(),
    ): Boolean {
        if (distanceToRouteMeters <= policy.thresholdMeters) {
            hits = 0
            return false
        }
        hits += 1
        if (hits < policy.requiredHits) return false
        if (nowMs - lastRecalcAtMs < policy.minIntervalMs) return false
        markRecalc(nowMs)
        return true
    }

    fun markRecalc(nowMs: Long = System.currentTimeMillis()) {
        lastRecalcAtMs = nowMs
        hits = 0
    }
}
