package com.danielcioban.routeplanner.data.settings

import android.content.Context
import android.text.format.DateFormat as AndroidDateFormat

enum class ClockFormat {
    SYSTEM,
    HOURS_24,
    HOURS_12,
    ;

    fun is24Hour(context: Context): Boolean = when (this) {
        SYSTEM -> AndroidDateFormat.is24HourFormat(context)
        HOURS_24 -> true
        HOURS_12 -> false
    }

    companion object {
        fun fromStored(raw: String?): ClockFormat =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SYSTEM
    }
}
