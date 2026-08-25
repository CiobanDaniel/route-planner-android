package com.danielcioban.routeplanner.data

import com.danielcioban.routeplanner.data.local.RouteEntity

enum class LibraryEditScope {
    /** Update the library entry and every route that references it. */
    Global,
    /** Fork a copy and attach it only to [routeStopId]. */
    ThisRoute,
    /** Insert a new library stop; existing route references stay on the original. */
    SaveAsCopy,
}

enum class LibraryDeleteScope {
    /** Remove the stop from one route; keep the library entry. */
    ThisRouteOnly,
    /** Delete the library entry and every route stop that references it. */
    Everywhere,
}

data class LibraryUsage(
    val libraryStopId: Long,
    val routes: List<RouteEntity>,
) {
    val count: Int get() = routes.size
    val routeNames: List<String> get() = routes.map { it.name }
}

/** Optional library fields. Null on update means keep the stored value. */
data class LibraryPlaceExtras(
    val tags: String? = null,
    val what3words: String? = null,
    val plusCode: String? = null,
    val defaultGeofenceRadiusMeters: Int? = null,
    val setDefaultGeofence: Boolean = false,
    val isFavorite: Boolean? = null,
)
