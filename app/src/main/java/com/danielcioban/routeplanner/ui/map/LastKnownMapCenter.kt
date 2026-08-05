package com.danielcioban.routeplanner.ui.map

/**
 * Last known user position for instant map centering across screens
 * (avoids flashing a default city while GPS warms up).
 */
object LastKnownMapCenter {
    @Volatile
    var coordinate: LatLng? = null
        private set

    fun update(value: LatLng) {
        coordinate = value
    }
}
