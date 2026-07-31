package com.danielcioban.routeplanner.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: ENGLISH
    }
}

enum class DistanceUnit {
    METRIC,
    IMPERIAL,
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val distanceUnit: DistanceUnit = DistanceUnit.METRIC,
    val keepScreenOnDuringNav: Boolean = true,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val language = stringPreferencesKey("language")
        val distanceUnit = stringPreferencesKey("distance_unit")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on_nav")
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
            keepScreenOnDuringNav = prefs[Keys.keepScreenOn] ?: true,
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

    suspend fun setKeepScreenOnDuringNav(enabled: Boolean) {
        context.dataStore.edit { it[Keys.keepScreenOn] = enabled }
    }
}
