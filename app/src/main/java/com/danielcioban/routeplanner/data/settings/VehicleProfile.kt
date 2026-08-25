package com.danielcioban.routeplanner.data.settings

enum class VehicleProfile(val osrmPath: String) {
    CAR("driving"),
    BIKE("cycling"),
    WALK("walking"),
    ;

    companion object {
        fun fromStored(raw: String?): VehicleProfile =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: CAR
    }
}

data class OsrmOptions(
    val profile: String = VehicleProfile.CAR.osrmPath,
    val exclude: String? = null,
)

fun AppSettings.osrmOptions(): OsrmOptions {
    val exclude = buildList {
        if (avoidTolls) add("toll")
        if (avoidMotorways) add("motorway")
    }.takeIf { it.isNotEmpty() }?.joinToString(",")
    return OsrmOptions(profile = vehicleProfile.osrmPath, exclude = exclude)
}
