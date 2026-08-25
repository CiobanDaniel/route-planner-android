package com.danielcioban.routeplanner.ui.navigation

object AppDestinations {
    const val ROUTE_LIST = "route_list"
    const val ROUTE_DETAIL = "route_detail/{routeId}?autostart={autostart}"
    const val ROUTE_EDIT = "route_edit/{routeId}"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val STOP_LIBRARY = "stop_library"
    const val ACCOUNT = "account"
    const val QUICK_DRIVE = "quick_drive"
    const val TRIP_HISTORY = "trip_history"
    const val TRIP_STATS = "trip_stats"
    const val FUEL_LOG = "fuel_log"
    const val TRASH = "trash"

    fun routeDetail(routeId: Long, autostart: Boolean = false) =
        "route_detail/$routeId?autostart=$autostart"
    fun routeCreate() = "route_edit/-1"
    fun routeEdit(routeId: Long) = "route_edit/$routeId"
}
