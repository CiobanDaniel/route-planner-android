package com.danielcioban.routeplanner.data

import com.danielcioban.routeplanner.data.local.AppDatabase
import com.danielcioban.routeplanner.data.local.RouteEntity
import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import kotlinx.coroutines.flow.Flow

data class StopDraft(
    val name: String,
    val addressHint: String = "",
    val notes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isCompleted: Boolean = false,
    val libraryStopId: Long? = null,
)

sealed class AddFromLibraryResult {
    data class Added(val stopId: Long) : AddFromLibraryResult()
    data class AlreadyOnRoute(val stopId: Long) : AddFromLibraryResult()
    data object LibraryMissing : AddFromLibraryResult()
}

class RouteRepository(db: AppDatabase) {
    private val dao = db.routeDao()
    private val libraryDao = db.stopLibraryDao()

    fun observeRoutes(): Flow<List<RouteWithStops>> = dao.observeRoutes()

    fun observeRoute(routeId: Long): Flow<RouteWithStops?> = dao.observeRoute(routeId)

    fun observeStopLibrary(): Flow<List<StopLibraryEntity>> = libraryDao.observeAll()

    suspend fun getRoute(routeId: Long): RouteWithStops? = dao.getRoute(routeId)

    suspend fun getLibraryStop(id: Long): StopLibraryEntity? = libraryDao.getById(id)

    suspend fun upsertLibraryStop(
        name: String,
        addressHint: String,
        notes: String,
        latitude: Double,
        longitude: Double,
        existingId: Long? = null,
    ): Long {
        val now = System.currentTimeMillis()
        val entity = if (existingId != null) {
            libraryDao.getById(existingId)?.copy(
                name = name.trim(),
                addressHint = addressHint.trim(),
                notes = notes.trim(),
                latitude = latitude,
                longitude = longitude,
                updatedAtEpochMs = now,
            ) ?: StopLibraryEntity(
                name = name.trim(),
                addressHint = addressHint.trim(),
                notes = notes.trim(),
                latitude = latitude,
                longitude = longitude,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            )
        } else {
            StopLibraryEntity(
                name = name.trim(),
                addressHint = addressHint.trim(),
                notes = notes.trim(),
                latitude = latitude,
                longitude = longitude,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            )
        }
        return if (entity.id == 0L) {
            libraryDao.upsert(entity)
        } else {
            libraryDao.update(entity)
            entity.id
        }
    }

    suspend fun saveRouteStopToLibrary(stop: StopEntity): Long? {
        val lat = stop.latitude ?: return null
        val lng = stop.longitude ?: return null
        return upsertLibraryStop(
            name = stop.name,
            addressHint = stop.addressHint,
            notes = stop.notes,
            latitude = lat,
            longitude = lng,
            existingId = stop.libraryStopId,
        )
    }

    suspend fun deleteLibraryStop(id: Long) {
        libraryDao.delete(id)
    }

    /**
     * Copy a library stop onto a route (delivery progress stays per-route).
     * If this route already has a stop linked to the same library id, returns
     * [AddFromLibraryResult.AlreadyOnRoute] instead of inserting a duplicate.
     */
    suspend fun addStopFromLibrary(routeId: Long, libraryStopId: Long): AddFromLibraryResult {
        if (libraryDao.getById(libraryStopId) == null) {
            return AddFromLibraryResult.LibraryMissing
        }
        val existing = dao.getRoute(routeId)
            ?.orderedStops
            ?.firstOrNull { it.libraryStopId == libraryStopId }
        if (existing != null) {
            return AddFromLibraryResult.AlreadyOnRoute(existing.id)
        }
        val lib = libraryDao.getById(libraryStopId)!!
        val stopId = addStop(
            routeId,
            StopDraft(
                name = lib.name,
                addressHint = lib.addressHint,
                notes = lib.notes,
                latitude = lib.latitude,
                longitude = lib.longitude,
                libraryStopId = lib.id,
            ),
        )
        return AddFromLibraryResult.Added(stopId)
    }

    /** Overwrite a route stop's fields from its linked library entry (copy-on-add stays intact). */
    suspend fun updateStopFromLibrary(stopId: Long): Boolean {
        val stop = dao.getStop(stopId) ?: return false
        val libId = stop.libraryStopId ?: return false
        val lib = libraryDao.getById(libId) ?: return false
        dao.updateStop(
            stop.copy(
                name = lib.name,
                addressHint = lib.addressHint,
                notes = lib.notes,
                latitude = lib.latitude,
                longitude = lib.longitude,
            ),
        )
        dao.touchRoute(stop.routeId)
        return true
    }

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

    suspend fun resetStopCompletions(routeId: Long) {
        dao.resetStopCompletions(routeId)
        dao.touchRoute(routeId)
    }

    suspend fun addStop(routeId: Long, stop: StopDraft): Long {
        val nextPosition = dao.maxStopPosition(routeId) + 1
        val id = dao.insertStop(
            StopEntity(
                routeId = routeId,
                position = nextPosition,
                name = stop.name.trim(),
                addressHint = stop.addressHint.trim(),
                notes = stop.notes.trim(),
                latitude = stop.latitude,
                longitude = stop.longitude,
                isCompleted = stop.isCompleted,
                libraryStopId = stop.libraryStopId,
            ),
        )
        dao.touchRoute(routeId)
        return id
    }

    suspend fun updateStop(stop: StopEntity) {
        dao.updateStop(stop)
        dao.touchRoute(stop.routeId)
    }

    suspend fun deleteStop(stopId: Long, routeId: Long) {
        dao.deleteStop(stopId)
        dao.touchRoute(routeId)
    }

    /** Swap a stop one slot toward the start (`delta = -1`) or end (`delta = +1`). */
    suspend fun moveStop(routeId: Long, stopId: Long, delta: Int) {
        if (delta == 0) return
        val ordered = dao.getRoute(routeId)?.orderedStops?.toMutableList() ?: return
        val index = ordered.indexOfFirst { it.id == stopId }
        val newIndex = index + delta
        if (index < 0 || newIndex !in ordered.indices) return
        val moved = ordered.removeAt(index)
        ordered.add(newIndex, moved)
        ordered.forEachIndexed { position, stop ->
            if (stop.position != position) {
                dao.updateStop(stop.copy(position = position))
            }
        }
        dao.touchRoute(routeId)
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
                    libraryStopId = stop.libraryStopId,
                )
            },
        )
    }
}
