package com.danielcioban.routeplanner.ui.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.ui.dev.DevLocationSim
import com.danielcioban.routeplanner.ui.map.LatLng

/** Speed (m/s) above which GPS course is preferred over compass. */
const val HeadingGpsSpeedThresholdMps = 1.4f

/**
 * Merge GPS course with device compass for map heading.
 *
 * Policy (DEV_STATUS): while moving, prefer GPS course; while stationary, prefer
 * remapped compass so a stale north course does not lock the chevron.
 *
 * @param compassWhenActive when false, compass is only a last resort (home map
 *   with Follow me off can keep showing GPS/last course).
 */
fun LatLng.mergeWithCompass(
    compassBearing: Float?,
    compassWhenActive: Boolean = true,
): LatLng {
    val moving = (speedMps ?: 0f) >= HeadingGpsSpeedThresholdMps
    val gpsCourse = bearingDegrees
    val heading = when {
        moving && gpsCourse != null -> gpsCourse
        compassWhenActive && compassBearing != null -> compassBearing
        gpsCourse != null -> gpsCourse
        compassBearing != null -> compassBearing
        else -> null
    }
    return copy(bearingDegrees = heading)
}

/**
 * @param active true while follow-me / delivery / in-app nav — enables compass
 *   and prefers it when stationary.
 */
@Composable
fun rememberMergedUserFix(
    coordinate: LatLng?,
    compassBearing: Float?,
    active: Boolean,
): LatLng? {
    val simOn by DevLocationSim.enabled.collectAsStateWithLifecycle()
    val base = coordinate ?: return null
    if (simOn) return base
    return remember(base, compassBearing, active) {
        base.mergeWithCompass(
            compassBearing = compassBearing,
            compassWhenActive = active,
        )
    }
}
