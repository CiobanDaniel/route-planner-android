package com.danielcioban.routeplanner.data.delivery

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.deliverySessionStore: DataStore<Preferences> by preferencesDataStore(
    name = "delivery_session",
)

enum class DriveKind {
    NONE,
    ROUTE,
    QUICK,
}

data class DeliverySession(
    val activeRouteId: Long? = null,
    val tripHistoryId: Long? = null,
    val quickName: String? = null,
    val quickLatitude: Double? = null,
    val quickLongitude: Double? = null,
    val quickLibraryStopId: Long? = null,
    val paused: Boolean = false,
) {
    val kind: DriveKind
        get() = when {
            activeRouteId != null -> DriveKind.ROUTE
            quickLatitude != null && quickLongitude != null -> DriveKind.QUICK
            else -> DriveKind.NONE
        }

    val isActive: Boolean get() = kind != DriveKind.NONE

    val isGuiding: Boolean get() = isActive && !paused

    val resumeLabel: String
        get() = when (kind) {
            DriveKind.QUICK -> quickName.orEmpty()
            else -> ""
        }
}

class DeliverySessionStore(private val context: Context) {
    private object Keys {
        val activeRouteId = longPreferencesKey("active_route_id")
        val tripHistoryId = longPreferencesKey("trip_history_id")
        val quickName = stringPreferencesKey("quick_name")
        val quickLat = doublePreferencesKey("quick_lat")
        val quickLng = doublePreferencesKey("quick_lng")
        val quickLibraryId = longPreferencesKey("quick_library_id")
        val paused = booleanPreferencesKey("paused")
    }

    val session: Flow<DeliverySession> = context.deliverySessionStore.data.map { prefs ->
        DeliverySession(
            activeRouteId = prefs[Keys.activeRouteId]?.takeIf { it > 0 },
            tripHistoryId = prefs[Keys.tripHistoryId]?.takeIf { it > 0 },
            quickName = prefs[Keys.quickName],
            quickLatitude = prefs[Keys.quickLat],
            quickLongitude = prefs[Keys.quickLng],
            quickLibraryStopId = prefs[Keys.quickLibraryId]?.takeIf { it > 0 },
            paused = prefs[Keys.paused] ?: false,
        )
    }

    suspend fun setActiveRoute(routeId: Long?, tripHistoryId: Long? = null) {
        context.deliverySessionStore.edit { prefs ->
            prefs.remove(Keys.quickName)
            prefs.remove(Keys.quickLat)
            prefs.remove(Keys.quickLng)
            prefs.remove(Keys.quickLibraryId)
            prefs.remove(Keys.paused)
            if (routeId == null || routeId <= 0) {
                prefs.remove(Keys.activeRouteId)
                prefs.remove(Keys.tripHistoryId)
            } else {
                prefs[Keys.activeRouteId] = routeId
                if (tripHistoryId != null && tripHistoryId > 0) {
                    prefs[Keys.tripHistoryId] = tripHistoryId
                } else {
                    prefs.remove(Keys.tripHistoryId)
                }
            }
        }
    }

    suspend fun setQuickDrive(
        name: String,
        latitude: Double,
        longitude: Double,
        tripHistoryId: Long,
        libraryStopId: Long? = null,
    ) {
        context.deliverySessionStore.edit { prefs ->
            prefs.remove(Keys.activeRouteId)
            prefs.remove(Keys.paused)
            prefs[Keys.quickName] = name
            prefs[Keys.quickLat] = latitude
            prefs[Keys.quickLng] = longitude
            prefs[Keys.tripHistoryId] = tripHistoryId
            if (libraryStopId != null && libraryStopId > 0) {
                prefs[Keys.quickLibraryId] = libraryStopId
            } else {
                prefs.remove(Keys.quickLibraryId)
            }
        }
    }

    suspend fun setTripHistoryId(tripHistoryId: Long?) {
        context.deliverySessionStore.edit { prefs ->
            if (tripHistoryId == null || tripHistoryId <= 0) {
                prefs.remove(Keys.tripHistoryId)
            } else {
                prefs[Keys.tripHistoryId] = tripHistoryId
            }
        }
    }

    suspend fun setPaused(paused: Boolean) {
        context.deliverySessionStore.edit { prefs ->
            if (paused) {
                prefs[Keys.paused] = true
            } else {
                prefs.remove(Keys.paused)
            }
        }
    }

    suspend fun clear() {
        context.deliverySessionStore.edit { it.clear() }
    }

    suspend fun clearIfRoute(routeId: Long) {
        context.deliverySessionStore.edit { prefs ->
            if (prefs[Keys.activeRouteId] == routeId) {
                prefs.clear()
            }
        }
    }
}
