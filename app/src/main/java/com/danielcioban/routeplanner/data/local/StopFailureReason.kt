package com.danielcioban.routeplanner.data.local

object StopFailureReason {
    const val NOT_HOME = "NOT_HOME"
    const val REFUSED = "REFUSED"
    const val CLOSED = "CLOSED"
    const val OTHER = "OTHER"

    val all = listOf(NOT_HOME, REFUSED, CLOSED, OTHER)
}
