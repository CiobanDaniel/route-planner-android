package com.danielcioban.routeplanner.ui.map

enum class MapViewMode(val id: String, val label: String, val description: String) {
    MAP("map", "Map", "Clear streets — best everyday view"),
    DRIVING("driving", "Driving", "Heading-up nav with street focus"),
    SATELLITE("satellite", "Satellite", "Aerial imagery"),
    TERRAIN("terrain", "Terrain", "Hills and elevation"),
}
