package com.danielcioban.routeplanner.util

import com.danielcioban.routeplanner.data.local.StopLibraryEntity

object LibraryDuplicates {
    const val MERGE_RADIUS_METERS = 35.0

    fun similarName(a: String, b: String): Boolean {
        val x = a.trim().lowercase()
        val y = b.trim().lowercase()
        if (x.isEmpty() || y.isEmpty()) return true
        return x == y || x.contains(y) || y.contains(x)
    }

    /** Pairs of active library pins within [MERGE_RADIUS_METERS] with similar names. */
    fun pairs(stops: List<StopLibraryEntity>): List<Pair<StopLibraryEntity, StopLibraryEntity>> {
        val active = stops.filter { it.deletedAtEpochMs == null }.sortedBy { it.id }
        val out = mutableListOf<Pair<StopLibraryEntity, StopLibraryEntity>>()
        for (i in active.indices) {
            for (j in i + 1 until active.size) {
                val a = active[i]
                val b = active[j]
                val distance = GeoUtils.distanceMeters(
                    a.latitude,
                    a.longitude,
                    b.latitude,
                    b.longitude,
                )
                if (distance <= MERGE_RADIUS_METERS && similarName(a.name, b.name)) {
                    out += a to b
                }
            }
        }
        return out
    }

    fun preferKeep(a: StopLibraryEntity, b: StopLibraryEntity): StopLibraryEntity {
        val byUse = a.useCount.compareTo(b.useCount)
        if (byUse != 0) return if (byUse > 0) a else b
        val byRecent = a.lastUsedAtEpochMs.compareTo(b.lastUsedAtEpochMs)
        if (byRecent != 0) return if (byRecent > 0) a else b
        return if (a.id <= b.id) a else b
    }
}
