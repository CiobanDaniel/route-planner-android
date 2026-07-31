package com.danielcioban.routeplanner.data.delivery

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.deliverySessionStore: DataStore<Preferences> by preferencesDataStore(
    name = "delivery_session",
)

data class DeliverySession(
    val activeRouteId: Long? = null,
)

class DeliverySessionStore(private val context: Context) {
    private object Keys {
        val activeRouteId = longPreferencesKey("active_route_id")
    }

    val session: Flow<DeliverySession> = context.deliverySessionStore.data.map { prefs ->
        val id = prefs[Keys.activeRouteId]
        DeliverySession(activeRouteId = id?.takeIf { it > 0 })
    }

    suspend fun setActiveRoute(routeId: Long?) {
        context.deliverySessionStore.edit { prefs ->
            if (routeId == null || routeId <= 0) {
                prefs.remove(Keys.activeRouteId)
            } else {
                prefs[Keys.activeRouteId] = routeId
            }
        }
    }

    suspend fun clearIfRoute(routeId: Long) {
        context.deliverySessionStore.edit { prefs ->
            if (prefs[Keys.activeRouteId] == routeId) {
                prefs.remove(Keys.activeRouteId)
            }
        }
    }
}
