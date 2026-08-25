package com.danielcioban.routeplanner.data.routing

object RoutingFailure {
    const val TIMEOUT = "routing_timeout"
    const val RATE_LIMIT = "routing_rate_limit"

    fun isTimeout(message: String?): Boolean =
        message?.contains(TIMEOUT, ignoreCase = true) == true

    fun isRateLimit(message: String?): Boolean =
        message?.contains(RATE_LIMIT, ignoreCase = true) == true
}
