package com.danielcioban.routeplanner.ui.map

import com.danielcioban.routeplanner.data.settings.SavedMapCamera

/**
 * Last map camera from user pan/zoom, independent of GPS fly-to-user.
 */
object LastKnownMapView {
    @Volatile
    var camera: SavedMapCamera? = null
        private set

    fun update(value: SavedMapCamera) {
        if (value.zoom < 4.0) return
        camera = value
    }

    fun resolve(persisted: SavedMapCamera?): SavedMapCamera? = camera ?: persisted
}
