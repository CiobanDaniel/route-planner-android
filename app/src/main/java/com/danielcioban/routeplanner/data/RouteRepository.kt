package com.danielcioban.routeplanner.data

import com.danielcioban.routeplanner.data.local.AppDatabase
import com.danielcioban.routeplanner.data.local.RouteEntity
import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.data.local.StopEntity
import kotlinx.coroutines.flow.Flow

data class StopDraft(
    val name: String,
    val addressHint: String = "",
    val notes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isCompleted: Boolean = false,
)

class RouteRepository(db: AppDatabase) {
    private val dao = db.routeDao()

    fun observeRoutes(): Flow<List<RouteWithStops>> = dao.observeRoutes()

    fun observeRoute(routeId: Long): Flow<RouteWithStops?> = dao.observeRoute(routeId)

    suspend fun getRoute(routeId: Long): RouteWithStops? = dao.getRoute(routeId)

    suspend fun createRoute(name: String, notes: String, stops: List<StopDraft>): Long {
        val now = System.currentTimeMillis()
        val routeId = dao.insertRoute(
            RouteEntity(
                name = name.trim(),
                notes = notes.trim(),
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
        replaceStops(routeId, stops)
        return routeId
    }

    suspend fun updateRoute(routeId: Long, name: String, notes: String, stops: List<StopDraft>) {
        val existing = dao.getRoute(routeId)?.route ?: return
        dao.updateRoute(
            existing.copy(
                name = name.trim(),
                notes = notes.trim(),
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
        replaceStops(routeId, stops)
    }

    suspend fun deleteRoute(routeId: Long) {
        dao.deleteRoute(routeId)
    }

    suspend fun setStopCompleted(stopId: Long, completed: Boolean) {
        dao.setStopCompleted(stopId, completed)
    }

    private suspend fun replaceStops(routeId: Long, stops: List<StopDraft>) {
        dao.deleteStopsForRoute(routeId)
        if (stops.isEmpty()) return
        dao.insertStops(
            stops.mapIndexed { index, stop ->
                StopEntity(
                    routeId = routeId,
                    position = index,
                    name = stop.name.trim(),
                    addressHint = stop.addressHint.trim(),
                    notes = stop.notes.trim(),
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                    isCompleted = stop.isCompleted,
                )
            },
        )
    }
}
