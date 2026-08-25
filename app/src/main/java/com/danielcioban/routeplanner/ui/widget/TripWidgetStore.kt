package com.danielcioban.routeplanner.ui.widget

import android.content.Context
import com.danielcioban.routeplanner.ui.trip.TripHudSnapshot

object TripWidgetStore {
    private const val PREFS = "trip_widget"
    private const val KEY_ACTIVE = "active"
    private const val KEY_TITLE = "title"
    private const val KEY_DISTANCE = "distance"
    private const val KEY_PROGRESS = "progress"
    private const val KEY_CAN_DONE = "can_done"
    private const val KEY_TASKS = "tasks_blocked"
    private const val KEY_ALL_DONE = "all_done"

    fun write(context: Context, snapshot: TripHudSnapshot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ACTIVE, snapshot.active && !snapshot.paused)
            .putString(KEY_TITLE, snapshot.title)
            .putString(KEY_DISTANCE, snapshot.distance.ifBlank { snapshot.progressLabel })
            .putString(KEY_PROGRESS, snapshot.progressLabel)
            .putBoolean(KEY_CAN_DONE, snapshot.canMarkDone && !snapshot.tasksBlocked && !snapshot.allDone)
            .putBoolean(KEY_TASKS, snapshot.tasksBlocked)
            .putBoolean(KEY_ALL_DONE, snapshot.allDone)
            .apply()
    }

    fun read(context: Context): WidgetTrip {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return WidgetTrip(
            active = prefs.getBoolean(KEY_ACTIVE, false),
            title = prefs.getString(KEY_TITLE, "").orEmpty(),
            distance = prefs.getString(KEY_DISTANCE, "").orEmpty(),
            progress = prefs.getString(KEY_PROGRESS, "").orEmpty(),
            canMarkDone = prefs.getBoolean(KEY_CAN_DONE, false),
            tasksBlocked = prefs.getBoolean(KEY_TASKS, false),
            allDone = prefs.getBoolean(KEY_ALL_DONE, false),
        )
    }

    data class WidgetTrip(
        val active: Boolean,
        val title: String,
        val distance: String,
        val progress: String,
        val canMarkDone: Boolean,
        val tasksBlocked: Boolean,
        val allDone: Boolean,
    )
}
