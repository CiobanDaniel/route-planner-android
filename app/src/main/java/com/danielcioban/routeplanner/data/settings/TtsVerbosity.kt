package com.danielcioban.routeplanner.data.settings

enum class TtsVerbosity {
    MINIMAL,
    NORMAL,
    VERBOSE,
    ;

    companion object {
        fun fromStored(raw: String?): TtsVerbosity =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: NORMAL
    }
}
