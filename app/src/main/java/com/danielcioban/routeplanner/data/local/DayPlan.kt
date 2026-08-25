package com.danielcioban.routeplanner.data.local

import com.danielcioban.routeplanner.data.local.ShiftSlot.AFTERNOON
import com.danielcioban.routeplanner.data.local.ShiftSlot.MORNING

data class DayPlanSection(
    val slot: String,
    val routes: List<RouteWithStops>,
)

object DayPlan {
    fun sections(
        routes: List<RouteWithStops>,
        groupBySlot: Boolean,
    ): List<DayPlanSection> {
        if (!groupBySlot) {
            return listOf(DayPlanSection(ShiftSlot.UNSET, routes))
        }
        val morning = routes.filter { it.route.shiftSlot == MORNING }
        val afternoon = routes.filter { it.route.shiftSlot == AFTERNOON }
        val rest = routes.filter {
            it.route.shiftSlot != MORNING && it.route.shiftSlot != AFTERNOON
        }
        return buildList {
            if (morning.isNotEmpty()) add(DayPlanSection(MORNING, morning))
            if (afternoon.isNotEmpty()) add(DayPlanSection(AFTERNOON, afternoon))
            if (rest.isNotEmpty()) add(DayPlanSection(ShiftSlot.UNSET, rest))
        }
    }

    fun matchesVan(route: RouteWithStops, dispatcherVan: String, myVanOnly: Boolean): Boolean {
        if (!myVanOnly || dispatcherVan.isBlank()) return true
        val van = route.route.vanName.trim()
        return van.isEmpty() || van.equals(dispatcherVan.trim(), ignoreCase = true)
    }

    fun matchesShift(route: RouteWithStops, slot: String): Boolean {
        if (slot.isEmpty()) return true
        return route.route.shiftSlot == slot
    }
}
