package com.danielcioban.routeplanner.data.local

/** Morning / afternoon queue for a van day. Empty means unscheduled. */
object ShiftSlot {
    const val UNSET = ""
    const val MORNING = "MORNING"
    const val AFTERNOON = "AFTERNOON"

    fun fromStored(raw: String?): String = when (raw?.trim()?.uppercase()) {
        MORNING -> MORNING
        AFTERNOON -> AFTERNOON
        else -> UNSET
    }
}
