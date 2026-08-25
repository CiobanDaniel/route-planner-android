package com.danielcioban.routeplanner.ui.trip

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TripHudSnapshot(
    val active: Boolean = false,
    val title: String = "",
    val maneuver: String = "",
    val thenManeuver: String = "",
    val distance: String = "",
    val eta: String = "",
    val progressLabel: String = "",
    val arrived: Boolean = false,
    val canMarkDone: Boolean = false,
    val allDone: Boolean = false,
    val tasksBlocked: Boolean = false,
    val keepScreenOn: Boolean = true,
    val paused: Boolean = false,
    val speedMps: Float? = null,
)

object TripHudStore {
    private val _snapshot = MutableStateFlow(TripHudSnapshot())
    val snapshot: StateFlow<TripHudSnapshot> = _snapshot.asStateFlow()

    fun publish(value: TripHudSnapshot) {
        _snapshot.value = value
    }

    fun clear() {
        _snapshot.value = TripHudSnapshot()
    }
}
