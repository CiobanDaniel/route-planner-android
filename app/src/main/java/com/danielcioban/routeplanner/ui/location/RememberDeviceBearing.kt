package com.danielcioban.routeplanner.ui.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.round

/**
 * Device heading in degrees clockwise from north (0–359).
 *
 * Handles phone flat on a table (top-of-phone heading) and upright (facing heading)
 * via [SensorManager.remapCoordinateSystem], plus display rotation.
 */
@Composable
fun rememberDeviceBearing(enabled: Boolean): Float? {
    val context = LocalContext.current
    var bearing by remember { mutableFloatStateOf(Float.NaN) }

    DisposableEffect(enabled) {
        if (!enabled) {
            return@DisposableEffect onDispose { }
        }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: return@DisposableEffect onDispose { }

        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        }

        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val remapped = FloatArray(9)
            private val orientation = FloatArray(3)
            private var filtered = Float.NaN

            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val displayRotation = display?.rotation ?: Surface.ROTATION_0
                val (axisX, axisY) = axesForPose(rotationMatrix, displayRotation)
                if (!SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remapped)) {
                    System.arraycopy(rotationMatrix, 0, remapped, 0, 9)
                }
                SensorManager.getOrientation(remapped, orientation)
                var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                azimuth = (azimuth + 360f) % 360f

                // Geographic heading clockwise from north. Leaflet's setBearing
                // uses the opposite sense — that invert lives only in map.html
                // applyMapBearing(), so GPS course and this compass stay aligned.

                if (filtered.isNaN()) {
                    filtered = azimuth
                } else {
                    var delta = azimuth - filtered
                    if (delta > 180f) delta -= 360f
                    if (delta < -180f) delta += 360f
                    filtered = (filtered + delta * 0.28f + 360f) % 360f
                }

                val next = round(filtered)
                if (bearing.isNaN() || abs(bearing - next) >= 1f) {
                    bearing = next
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(
            listener,
            rotationSensor,
            SensorManager.SENSOR_DELAY_GAME,
        )
        onDispose { sensorManager.unregisterListener(listener) }
    }

    return bearing.takeUnless { it.isNaN() }
}

/**
 * Flat (screen roughly skyward): heading = top of phone on the table.
 * Upright: heading = direction the device faces (screen normal on ground plane).
 */
private fun axesForPose(rotationMatrix: FloatArray, displayRotation: Int): Pair<Int, Int> {
    // R[8] ≈ device-Z · world-up. Near ±1 ⇒ phone is flat on a table.
    val flat = abs(rotationMatrix[8]) > 0.55f
    return if (flat) {
        displayAxes(displayRotation)
    } else {
        uprightFacingAxes(displayRotation)
    }
}

private fun displayAxes(rotation: Int): Pair<Int, Int> = when (rotation) {
    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
}

private fun uprightFacingAxes(rotation: Int): Pair<Int, Int> = when (rotation) {
    Surface.ROTATION_90 -> SensorManager.AXIS_Z to SensorManager.AXIS_MINUS_X
    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Z
    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Z to SensorManager.AXIS_X
    else -> SensorManager.AXIS_X to SensorManager.AXIS_Z
}
