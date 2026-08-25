package com.danielcioban.routeplanner.ui.map

import androidx.annotation.StringRes
import com.danielcioban.routeplanner.R

enum class MapViewMode(
    val id: String,
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int,
) {
    MAP("map", R.string.map_type_map, R.string.map_type_map_desc),
    NIGHT("night", R.string.map_type_night, R.string.map_type_night_desc),
    DRIVING("driving", R.string.map_type_driving, R.string.map_type_driving_desc),
    SATELLITE("satellite", R.string.map_type_satellite, R.string.map_type_satellite_desc),
    TERRAIN("terrain", R.string.map_type_terrain, R.string.map_type_terrain_desc),
}
