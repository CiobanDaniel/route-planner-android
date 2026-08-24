package com.danielcioban.routeplanner.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Shared map chrome: view mode, follow-me, recenter token, layers menu visibility.
 * Keep the layers **menu** as a Dialog owned by a fillMaxSize root — never expand
 * inside a height-wrapping chrome column (see docs/DEV_STATUS.md).
 */
class MapChromeState(
    initialBrowseMode: MapViewMode = MapViewMode.MAP,
) {
    var onPreferredModeChange: (MapViewMode) -> Unit = {}
    private var preferredBrowseMode =
        if (initialBrowseMode == MapViewMode.DRIVING) MapViewMode.MAP else initialBrowseMode

    var mapViewMode by mutableStateOf(preferredBrowseMode)
        private set
    var driveFollow by mutableStateOf(false)
    var recenterToken by mutableIntStateOf(0)
        private set
    var layersMenuOpen by mutableStateOf(false)

    fun bumpRecenter() {
        recenterToken++
    }

    fun openLayersMenu() {
        layersMenuOpen = true
    }

    fun dismissLayersMenu() {
        layersMenuOpen = false
    }

    /** Selecting Driving turns follow on; other styles turn follow off and persist. */
    fun selectMapMode(mode: MapViewMode) {
        mapViewMode = mode
        if (mode == MapViewMode.DRIVING) {
            driveFollow = true
            bumpRecenter()
        } else {
            driveFollow = false
            preferredBrowseMode = mode
            onPreferredModeChange(mode)
        }
    }

    fun setFollowEnabled(enabled: Boolean) {
        driveFollow = enabled
        if (enabled) {
            mapViewMode = MapViewMode.DRIVING
            bumpRecenter()
        } else {
            mapViewMode = preferredBrowseMode
        }
    }

    fun enterDrivingFollow() {
        mapViewMode = MapViewMode.DRIVING
        driveFollow = true
        bumpRecenter()
    }

    fun exitDrivingFollow() {
        driveFollow = false
        mapViewMode = preferredBrowseMode
    }

    fun applyPreferredIfIdle(mode: MapViewMode) {
        if (mode == MapViewMode.DRIVING) return
        preferredBrowseMode = mode
        if (!driveFollow && mapViewMode != MapViewMode.DRIVING) {
            mapViewMode = mode
        }
    }
}

@Composable
fun rememberMapChromeState(
    preferredBrowseMode: MapViewMode = MapViewMode.MAP,
    onPreferredModeChange: (MapViewMode) -> Unit = {},
): MapChromeState {
    val state = remember { MapChromeState(preferredBrowseMode) }
    state.onPreferredModeChange = onPreferredModeChange
    LaunchedEffect(preferredBrowseMode) {
        state.applyPreferredIfIdle(preferredBrowseMode)
    }
    return state
}

/**
 * Renders [MapLayersMenuDialog] when open. Pass [followChecked] when the switch
 * should reflect a derived flag (e.g. delivery active) instead of [MapChromeState.driveFollow].
 */
@Composable
fun MapLayersMenuHost(
    chrome: MapChromeState,
    followChecked: Boolean = chrome.driveFollow,
) {
    if (!chrome.layersMenuOpen) return
    MapLayersMenuDialog(
        selected = chrome.mapViewMode,
        onSelected = chrome::selectMapMode,
        driveFollow = followChecked,
        onDriveFollowChange = chrome::setFollowEnabled,
        onDismiss = chrome::dismissLayersMenu,
    )
}
