package com.danielcioban.routeplanner.util

import android.app.Activity
import android.view.KeyEvent
import com.danielcioban.routeplanner.RoutePlannerApplication
import com.danielcioban.routeplanner.ui.location.HeadingGpsSpeedThresholdMps
import com.danielcioban.routeplanner.ui.trip.TripGuidanceService
import com.danielcioban.routeplanner.ui.trip.TripHudStore

/** Opt-in volume / media Done: only when parked, arrived, and not blocked by tasks. */
object VolumeKeyDone {
    fun handle(activity: Activity, event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.repeatCount > 0) return false
        val key = event.keyCode
        if (key != KeyEvent.KEYCODE_VOLUME_UP &&
            key != KeyEvent.KEYCODE_VOLUME_DOWN &&
            key != KeyEvent.KEYCODE_HEADSETHOOK &&
            key != KeyEvent.KEYCODE_MEDIA_PLAY &&
            key != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        ) {
            return false
        }
        val app = activity.application as? RoutePlannerApplication ?: return false
        if (!app.latestSettings.volumeKeyDone) return false
        val hud = TripHudStore.snapshot.value
        if (!hud.active || hud.paused || !hud.arrived || hud.tasksBlocked || hud.allDone) {
            return false
        }
        if (!hud.canMarkDone) return false
        if ((hud.speedMps ?: 0f) >= HeadingGpsSpeedThresholdMps) return false
        TripGuidanceService.sendAction(activity, TripGuidanceService.ACTION_MARK_DONE)
        return true
    }
}
