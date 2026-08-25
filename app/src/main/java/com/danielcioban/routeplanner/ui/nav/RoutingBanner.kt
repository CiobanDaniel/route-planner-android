package com.danielcioban.routeplanner.ui.nav

import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.routing.DrivingRoute
import com.danielcioban.routeplanner.data.routing.RoutingFailure

fun routingBannerRes(
    driving: DrivingRoute,
    failureMessage: String?,
    online: Boolean,
): Int? {
    if (driving.isCached) {
        return if (online) R.string.nav_cached_route_online else R.string.nav_cached_route
    }
    if (!driving.isApproximate) return null
    return when {
        !online -> R.string.nav_approx_offline
        RoutingFailure.isRateLimit(failureMessage) -> R.string.nav_approx_rate_limit
        RoutingFailure.isTimeout(failureMessage) -> R.string.nav_approx_timeout
        else -> R.string.nav_approx_roads_unavailable
    }
}
