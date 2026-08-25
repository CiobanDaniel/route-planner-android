package com.danielcioban.routeplanner.data.settings

/**
 * How quickly in-app nav asks OSRM for a new path after leaving the polyline.
 * Does not change GPS-gated start.
 */
enum class RerouteAggressiveness(
    val thresholdMeters: Double,
    val minIntervalMs: Long,
    val requiredHits: Int,
) {
    CALM(90.0, 12_000L, 2),
    NORMAL(50.0, 8_000L, 1),
    SHARP(28.0, 4_000L, 1),
    ;

    companion object {
        fun fromStored(raw: String?): RerouteAggressiveness =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: NORMAL
    }
}
