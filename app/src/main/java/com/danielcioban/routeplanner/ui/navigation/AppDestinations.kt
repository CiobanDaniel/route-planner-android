package com.danielcioban.routeplanner.ui.navigation

object AppDestinations {
    const val ROUTE_LIST = "route_list"
    const val ROUTE_DETAIL = "route_detail/{routeId}"
    const val ROUTE_EDIT = "route_edit/{routeId}"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val STOP_LIBRARY = "stop_library"
    const val ACCOUNT = "account"

    fun routeDetail(routeId: Long) = "route_detail/$routeId"
    fun routeCreate() = "route_edit/-1"
    fun routeEdit(routeId: Long) = "route_edit/$routeId"
}
