package com.danielcioban.routeplanner.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.danielcioban.routeplanner.ui.map.MapViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AppLanguage(val tag: String) {
    ENGLISH("en"),
    ROMANIAN("ro"),
    FRENCH("fr"),
    GERMAN("de"),
    ITALIAN("it"),
    SPANISH("es"),
    PORTUGUESE("pt"),
    ;

    companion object {
        /** Matches `en`, `ro`, … and BCP-47 tags like `pt-BR` / `en-US` by primary subtag. */
        fun fromTag(tag: String?): AppLanguage {
            if (tag.isNullOrBlank()) return ENGLISH
            val primary = tag.trim().replace('_', '-').substringBefore('-').lowercase()
            return entries.firstOrNull { it.tag == primary } ?: ENGLISH
        }
    }
}

enum class DistanceUnit {
    METRIC,
    IMPERIAL,
}

data class SavedMapCamera(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
)

/** Same ~walking threshold as heading GPS-vs-compass (~5 km/h). */
const val KeepAwakeMovingSpeedMps = 1.4f

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val distanceUnit: DistanceUnit = DistanceUnit.METRIC,
    val clockFormat: ClockFormat = ClockFormat.SYSTEM,
    val keepScreenOnDuringNav: Boolean = true,
    val keepScreenOnOnlyWhileMoving: Boolean = false,
    val speakManeuvers: Boolean = true,
    val ttsVerbosity: TtsVerbosity = TtsVerbosity.NORMAL,
    val muteTtsDuringCalls: Boolean = true,
    val defaultRoundTrip: Boolean = false,
    val defaultFollowMe: Boolean = true,
    val dataSaver: Boolean = false,
    /** Last browse map style (never Driving — that's session follow-me). */
    val preferredMapStyle: MapViewMode = MapViewMode.MAP,
    val lastMapCamera: SavedMapCamera? = null,
    val geofenceAction: GeofenceAction = GeofenceAction.VISITED,
    val geofenceRadiusMeters: Int = StopGeofence.DEFAULT_RADIUS_METERS,
    val geofenceDwellSeconds: Int = 0,
    val vehicleProfile: VehicleProfile = VehicleProfile.CAR,
    val avoidTolls: Boolean = false,
    val avoidMotorways: Boolean = false,
    val geofenceHaptic: Boolean = true,
    val geofenceBeep: Boolean = true,
    val volumeKeyDone: Boolean = false,
    val northUpWhileDriving: Boolean = false,
    val walkLastMile: Boolean = false,
    val rerouteAggressiveness: RerouteAggressiveness = RerouteAggressiveness.NORMAL,
    val reduceMotion: Boolean = false,
    val highContrastPins: Boolean = false,
    val onboardingDismissed: Boolean = false,
    val fgsLocationExplained: Boolean = false,
    val lastOpenedRouteId: Long? = null,
    val homeLatitude: Double? = null,
    val homeLongitude: Double? = null,
    val homeName: String = "",
    val useHomeAsOrigin: Boolean = false,
    val dispatcherVanName: String = "",
    val askProofOnDone: Boolean = false,
    val lastBackupEpochMs: Long = 0L,
    val backupFolderUri: String = "",
    val tripRetentionDays: Int = 0,
) {
    val hasHome: Boolean
        get() = homeLatitude != null && homeLongitude != null

    fun keepScreenAwake(tripActive: Boolean, speedMps: Float?): Boolean {
        if (!keepScreenOnDuringNav || !tripActive) return false
        if (!keepScreenOnOnlyWhileMoving) return true
        return (speedMps ?: 0f) >= KeepAwakeMovingSpeedMps
    }

    fun effectiveBrowseStyle(): MapViewMode =
        if (dataSaver && preferredMapStyle == MapViewMode.SATELLITE) {
            MapViewMode.MAP
        } else {
            preferredMapStyle
        }
}

class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val language = stringPreferencesKey("language")
        val distanceUnit = stringPreferencesKey("distance_unit")
        val clockFormat = stringPreferencesKey("clock_format")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on_nav")
        val keepScreenOnOnlyWhileMoving = booleanPreferencesKey("keep_screen_on_moving")
        val speakManeuvers = booleanPreferencesKey("speak_maneuvers")
        val ttsVerbosity = stringPreferencesKey("tts_verbosity")
        val muteTtsDuringCalls = booleanPreferencesKey("mute_tts_calls")
        val defaultRoundTrip = booleanPreferencesKey("default_round_trip")
        val defaultFollowMe = booleanPreferencesKey("default_follow_me")
        val dataSaver = booleanPreferencesKey("data_saver")
        val preferredMapStyle = stringPreferencesKey("preferred_map_style")
        val mapCameraLat = doublePreferencesKey("map_camera_lat")
        val mapCameraLng = doublePreferencesKey("map_camera_lng")
        val mapCameraZoom = doublePreferencesKey("map_camera_zoom")
        val geofenceAction = stringPreferencesKey("geofence_action")
        val geofenceRadius = intPreferencesKey("geofence_radius_m")
        val geofenceDwellSeconds = intPreferencesKey("geofence_dwell_s")
        val vehicleProfile = stringPreferencesKey("vehicle_profile")
        val avoidTolls = booleanPreferencesKey("avoid_tolls")
        val avoidMotorways = booleanPreferencesKey("avoid_motorways")
        val geofenceHaptic = booleanPreferencesKey("geofence_haptic")
        val geofenceBeep = booleanPreferencesKey("geofence_beep")
        val volumeKeyDone = booleanPreferencesKey("volume_key_done")
        val northUpWhileDriving = booleanPreferencesKey("north_up_driving")
        val walkLastMile = booleanPreferencesKey("walk_last_mile")
        val rerouteAggressiveness = stringPreferencesKey("reroute_aggressiveness")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val highContrastPins = booleanPreferencesKey("high_contrast_pins")
        val onboardingDismissed = booleanPreferencesKey("onboarding_dismissed")
        val fgsLocationExplained = booleanPreferencesKey("fgs_location_explained")
        val lastOpenedRouteId = longPreferencesKey("last_opened_route_id")
        val homeLatitude = doublePreferencesKey("home_latitude")
        val homeLongitude = doublePreferencesKey("home_longitude")
        val homeName = stringPreferencesKey("home_name")
        val useHomeAsOrigin = booleanPreferencesKey("use_home_as_origin")
        val dispatcherVanName = stringPreferencesKey("dispatcher_van_name")
        val askProofOnDone = booleanPreferencesKey("ask_proof_on_done")
        val lastBackupEpochMs = longPreferencesKey("last_backup_epoch_ms")
        val backupFolderUri = stringPreferencesKey("backup_folder_uri")
        val tripRetentionDays = intPreferencesKey("trip_retention_days")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.theme]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
            } ?: ThemeMode.SYSTEM,
            language = AppLanguage.fromTag(prefs[Keys.language]),
            distanceUnit = prefs[Keys.distanceUnit]?.let {
                runCatching { DistanceUnit.valueOf(it) }.getOrDefault(DistanceUnit.METRIC)
            } ?: DistanceUnit.METRIC,
            clockFormat = ClockFormat.fromStored(prefs[Keys.clockFormat]),
            keepScreenOnDuringNav = prefs[Keys.keepScreenOn] ?: true,
            keepScreenOnOnlyWhileMoving = prefs[Keys.keepScreenOnOnlyWhileMoving] ?: false,
            speakManeuvers = prefs[Keys.speakManeuvers] ?: true,
            ttsVerbosity = TtsVerbosity.fromStored(prefs[Keys.ttsVerbosity]),
            muteTtsDuringCalls = prefs[Keys.muteTtsDuringCalls] ?: true,
            defaultRoundTrip = prefs[Keys.defaultRoundTrip] ?: false,
            defaultFollowMe = prefs[Keys.defaultFollowMe] ?: true,
            dataSaver = prefs[Keys.dataSaver] ?: false,
            preferredMapStyle = prefs[Keys.preferredMapStyle]?.let { id ->
                MapViewMode.entries.firstOrNull { it.id == id && it != MapViewMode.DRIVING }
            } ?: MapViewMode.MAP,
            lastMapCamera = run {
                val lat = prefs[Keys.mapCameraLat]
                val lng = prefs[Keys.mapCameraLng]
                val zoom = prefs[Keys.mapCameraZoom]
                if (lat != null && lng != null && zoom != null) {
                    SavedMapCamera(lat, lng, zoom)
                } else {
                    null
                }
            },
            geofenceAction = GeofenceAction.fromStored(prefs[Keys.geofenceAction]),
            geofenceRadiusMeters = prefs[Keys.geofenceRadius]?.let(StopGeofence::clampRadius)
                ?: StopGeofence.DEFAULT_RADIUS_METERS,
            geofenceDwellSeconds = prefs[Keys.geofenceDwellSeconds]?.let(GeofenceDwell::clampSeconds)
                ?: 0,
            vehicleProfile = VehicleProfile.fromStored(prefs[Keys.vehicleProfile]),
            avoidTolls = prefs[Keys.avoidTolls] ?: false,
            avoidMotorways = prefs[Keys.avoidMotorways] ?: false,
            geofenceHaptic = prefs[Keys.geofenceHaptic] ?: true,
            geofenceBeep = prefs[Keys.geofenceBeep] ?: true,
            volumeKeyDone = prefs[Keys.volumeKeyDone] ?: false,
            northUpWhileDriving = prefs[Keys.northUpWhileDriving] ?: false,
            walkLastMile = prefs[Keys.walkLastMile] ?: false,
            rerouteAggressiveness = RerouteAggressiveness.fromStored(prefs[Keys.rerouteAggressiveness]),
            reduceMotion = prefs[Keys.reduceMotion] ?: false,
            highContrastPins = prefs[Keys.highContrastPins] ?: false,
            onboardingDismissed = prefs[Keys.onboardingDismissed] ?: false,
            fgsLocationExplained = prefs[Keys.fgsLocationExplained] ?: false,
            lastOpenedRouteId = prefs[Keys.lastOpenedRouteId]?.takeIf { it > 0L },
            homeLatitude = prefs[Keys.homeLatitude],
            homeLongitude = prefs[Keys.homeLongitude],
            homeName = prefs[Keys.homeName].orEmpty(),
            useHomeAsOrigin = prefs[Keys.useHomeAsOrigin] ?: false,
            dispatcherVanName = prefs[Keys.dispatcherVanName].orEmpty(),
            askProofOnDone = prefs[Keys.askProofOnDone] ?: false,
            lastBackupEpochMs = prefs[Keys.lastBackupEpochMs] ?: 0L,
            backupFolderUri = prefs[Keys.backupFolderUri].orEmpty(),
            tripRetentionDays = prefs[Keys.tripRetentionDays] ?: 0,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.theme] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.language] = language.tag }
    }

    suspend fun setDistanceUnit(unit: DistanceUnit) {
        context.dataStore.edit { it[Keys.distanceUnit] = unit.name }
    }

    suspend fun setClockFormat(format: ClockFormat) {
        context.dataStore.edit { it[Keys.clockFormat] = format.name }
    }

    suspend fun setKeepScreenOnDuringNav(enabled: Boolean) {
        context.dataStore.edit { it[Keys.keepScreenOn] = enabled }
    }

    suspend fun setKeepScreenOnOnlyWhileMoving(enabled: Boolean) {
        context.dataStore.edit { it[Keys.keepScreenOnOnlyWhileMoving] = enabled }
    }

    suspend fun setSpeakManeuvers(enabled: Boolean) {
        context.dataStore.edit { it[Keys.speakManeuvers] = enabled }
    }

    suspend fun setTtsVerbosity(verbosity: TtsVerbosity) {
        context.dataStore.edit { it[Keys.ttsVerbosity] = verbosity.name }
    }

    suspend fun setMuteTtsDuringCalls(enabled: Boolean) {
        context.dataStore.edit { it[Keys.muteTtsDuringCalls] = enabled }
    }

    suspend fun setDefaultRoundTrip(enabled: Boolean) {
        context.dataStore.edit { it[Keys.defaultRoundTrip] = enabled }
    }

    suspend fun setDefaultFollowMe(enabled: Boolean) {
        context.dataStore.edit { it[Keys.defaultFollowMe] = enabled }
    }

    suspend fun setDataSaver(enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.dataSaver] = enabled
            if (enabled && it[Keys.preferredMapStyle] == MapViewMode.SATELLITE.id) {
                it[Keys.preferredMapStyle] = MapViewMode.MAP.id
            }
        }
    }

    suspend fun setPreferredMapStyle(mode: MapViewMode) {
        if (mode == MapViewMode.DRIVING) return
        context.dataStore.edit { it[Keys.preferredMapStyle] = mode.id }
    }

    suspend fun setLastMapCamera(camera: SavedMapCamera) {
        if (camera.zoom < 4.0) return
        context.dataStore.edit {
            it[Keys.mapCameraLat] = camera.latitude
            it[Keys.mapCameraLng] = camera.longitude
            it[Keys.mapCameraZoom] = camera.zoom
        }
    }

    suspend fun setGeofenceAction(action: GeofenceAction) {
        context.dataStore.edit { it[Keys.geofenceAction] = action.name }
    }

    suspend fun setGeofenceRadiusMeters(meters: Int) {
        context.dataStore.edit { it[Keys.geofenceRadius] = StopGeofence.clampRadius(meters) }
    }

    suspend fun setGeofenceDwellSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.geofenceDwellSeconds] = GeofenceDwell.clampSeconds(seconds) }
    }

    suspend fun setVehicleProfile(profile: VehicleProfile) {
        context.dataStore.edit { it[Keys.vehicleProfile] = profile.name }
    }

    suspend fun setAvoidTolls(enabled: Boolean) {
        context.dataStore.edit { it[Keys.avoidTolls] = enabled }
    }

    suspend fun setAvoidMotorways(enabled: Boolean) {
        context.dataStore.edit { it[Keys.avoidMotorways] = enabled }
    }

    suspend fun setGeofenceHaptic(enabled: Boolean) {
        context.dataStore.edit { it[Keys.geofenceHaptic] = enabled }
    }

    suspend fun setGeofenceBeep(enabled: Boolean) {
        context.dataStore.edit { it[Keys.geofenceBeep] = enabled }
    }

    suspend fun setVolumeKeyDone(enabled: Boolean) {
        context.dataStore.edit { it[Keys.volumeKeyDone] = enabled }
    }

    suspend fun setNorthUpWhileDriving(enabled: Boolean) {
        context.dataStore.edit { it[Keys.northUpWhileDriving] = enabled }
    }

    suspend fun setWalkLastMile(enabled: Boolean) {
        context.dataStore.edit { it[Keys.walkLastMile] = enabled }
    }

    suspend fun setRerouteAggressiveness(value: RerouteAggressiveness) {
        context.dataStore.edit { it[Keys.rerouteAggressiveness] = value.name }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.reduceMotion] = enabled }
    }

    suspend fun setHighContrastPins(enabled: Boolean) {
        context.dataStore.edit { it[Keys.highContrastPins] = enabled }
    }

    suspend fun setOnboardingDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[Keys.onboardingDismissed] = dismissed }
    }

    suspend fun setFgsLocationExplained(explained: Boolean) {
        context.dataStore.edit { it[Keys.fgsLocationExplained] = explained }
    }

    suspend fun setLastOpenedRouteId(routeId: Long) {
        if (routeId <= 0L) return
        context.dataStore.edit { it[Keys.lastOpenedRouteId] = routeId }
    }

    suspend fun setHome(
        latitude: Double,
        longitude: Double,
        name: String,
    ) {
        context.dataStore.edit {
            it[Keys.homeLatitude] = latitude
            it[Keys.homeLongitude] = longitude
            it[Keys.homeName] = name.trim()
        }
    }

    suspend fun clearHome() {
        context.dataStore.edit {
            it.remove(Keys.homeLatitude)
            it.remove(Keys.homeLongitude)
            it.remove(Keys.homeName)
        }
    }

    suspend fun setUseHomeAsOrigin(enabled: Boolean) {
        context.dataStore.edit { it[Keys.useHomeAsOrigin] = enabled }
    }

    suspend fun setDispatcherVanName(name: String) {
        context.dataStore.edit { it[Keys.dispatcherVanName] = name.trim() }
    }

    suspend fun setAskProofOnDone(enabled: Boolean) {
        context.dataStore.edit { it[Keys.askProofOnDone] = enabled }
    }

    suspend fun setLastBackupEpochMs(epochMs: Long) {
        context.dataStore.edit { it[Keys.lastBackupEpochMs] = epochMs }
    }

    suspend fun setBackupFolderUri(uri: String) {
        context.dataStore.edit {
            if (uri.isBlank()) it.remove(Keys.backupFolderUri) else it[Keys.backupFolderUri] = uri
        }
    }

    suspend fun setTripRetentionDays(days: Int) {
        context.dataStore.edit { it[Keys.tripRetentionDays] = days.coerceAtLeast(0) }
    }
}
