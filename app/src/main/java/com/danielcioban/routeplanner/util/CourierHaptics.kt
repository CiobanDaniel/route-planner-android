package com.danielcioban.routeplanner.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.danielcioban.routeplanner.RoutePlannerApplication
import com.danielcioban.routeplanner.data.settings.AppSettings

object CourierHaptics {
    fun tick(context: Context) {
        val app = context.applicationContext as? RoutePlannerApplication
        if (app?.latestSettings?.reduceMotion == true) return
        vibrate(context, 25L)
    }

    fun geofenceArrive(context: Context, settings: AppSettings) {
        if (!settings.reduceMotion && settings.geofenceHaptic) vibrate(context, 80L)
        if (settings.geofenceBeep) beep()
    }

    private fun vibrate(context: Context, millis: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(millis)
        }
    }

    private fun beep() {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                runCatching { tone.release() }
            }, 200)
        }
    }
}
