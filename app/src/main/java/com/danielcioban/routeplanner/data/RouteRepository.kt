package com.danielcioban.routeplanner.data

import androidx.room.withTransaction
import com.danielcioban.routeplanner.data.backup.BackupImportResult
import com.danielcioban.routeplanner.data.backup.RouteBackupManager
import com.danielcioban.routeplanner.data.local.AppDatabase
import com.danielcioban.routeplanner.data.local.RouteEntity
import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import com.danielcioban.routeplanner.data.local.StopTaskEntity
import com.danielcioban.routeplanner.data.local.StopTaskProgress
import com.danielcioban.routeplanner.util.RouteOrderOptimizer
import com.danielcioban.routeplanner.util.newRemoteId
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

sealed class StopCompletionResult {
    data object Updated : StopCompletionResult()
    data class BlockedByRequiredTasks(val incompleteCount: Int) : StopCompletionResult()
    data object StopMissing : StopCompletionResult()
}

class RouteRepository(private val db: AppDatabase) {
    private val dao = db.routeDao()
    private val libraryDao = db.stopLibraryDao()
    private val taskDao = db.stopTaskDao()
    private val backupManager = RouteBackupManager(dao, libraryDao, taskDao)

    fun observeRoutes(): Flow<List<RouteWithStops>> = dao.observeRoutes()

    fun observeRoute(routeId: Long): Flow<RouteWithStops?> = dao.observeRoute(routeId)

    fun observeStopLibrary(): Flow<List<StopLibraryEntity>> = libraryDao.observeAll()

    fun observeStopTasks(stopId: Long): Flow<List<StopTaskEntity>> = taskDao.observeForStop(stopId)

    fun observeStopTaskProgress(routeId: Long): Flow<List<StopTaskProgress>> =
        taskDao.observeProgressForRoute(routeId)

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

    suspend fun getLibraryUsage(libraryStopId: Long): LibraryUsage {
        return LibraryUsage(
            libraryStopId = libraryStopId,
            routes = dao.getRoutesUsingLibraryStop(libraryStopId),
        )
    }

    suspend fun applyLibraryPlaceEdit(
        libraryStopId: Long,
        name: String,
        addressHint: String,
        notes: String,
        latitude: Double,
        longitude: Double,
        scope: LibraryEditScope,
        routeStopId: Long? = null,
    ): Long {
        val trimmedName = name.trim().ifEmpty { return libraryStopId }
        return db.withTransaction {
            when (scope) {
                LibraryEditScope.Global -> {
                    upsertLibraryStop(
                        name = trimmedName,
                        addressHint = addressHint,
                        notes = notes,
                        latitude = latitude,
                        longitude = longitude,
                        existingId = libraryStopId,
                    )
                    dao.propagateLibraryPlace(
                        libraryStopId = libraryStopId,
                        name = trimmedName,
                        addressHint = addressHint.trim(),
                        notes = notes.trim(),
                        latitude = latitude,
                        longitude = longitude,
                    )
                    dao.getStopsWithLibraryId(libraryStopId).forEach { dao.touchRoute(it.routeId) }
                    libraryStopId
                }
                LibraryEditScope.ThisRoute -> {
                    val stop = routeStopId?.let { dao.getStop(it) } ?: return@withTransaction libraryStopId
                    val newId = upsertLibraryStop(
                        name = trimmedName,
                        addressHint = addressHint,
                        notes = notes,
                        latitude = latitude,
                        longitude = longitude,
                    )
                    dao.updateStop(
                        stop.copy(
                            name = trimmedName,
                            addressHint = addressHint.trim(),
                            notes = notes.trim(),
                            latitude = latitude,
                            longitude = longitude,
                            libraryStopId = newId,
                        ),
                    )
                    dao.touchRoute(stop.routeId)
                    newId
                }
                LibraryEditScope.SaveAsCopy -> {
                    upsertLibraryStop(
                        name = trimmedName,
                        addressHint = addressHint,
                        notes = notes,
                        latitude = latitude,
                        longitude = longitude,
                    )
                }
            }
        }
    }

    suspend fun saveRouteStopToLibrary(stop: StopEntity): Long? {
        val lat = stop.latitude ?: return null
        val lng = stop.longitude ?: return null
        val libraryId = upsertLibraryStop(
            name = stop.name,
            addressHint = stop.addressHint,
            notes = stop.notes,
            latitude = lat,
            longitude = lng,
            existingId = stop.libraryStopId,
        )
        if (stop.libraryStopId != libraryId) {
            dao.updateStop(stop.copy(libraryStopId = libraryId))
            dao.touchRoute(stop.routeId)
        } else {
            dao.propagateLibraryPlace(
                libraryStopId = libraryId,
                name = stop.name.trim(),
                addressHint = stop.addressHint.trim(),
                notes = stop.notes.trim(),
                latitude = lat,
                longitude = lng,
            )
            dao.getStopsWithLibraryId(libraryId).forEach { dao.touchRoute(it.routeId) }
        }
        return libraryId
    }

    suspend fun deleteLibraryStop(
        id: Long,
        scope: LibraryDeleteScope = LibraryDeleteScope.Everywhere,
        routeStopId: Long? = null,
        routeId: Long? = null,
    ) {
        db.withTransaction {
            when (scope) {
                LibraryDeleteScope.ThisRouteOnly -> {
                    val stopId = routeStopId ?: return@withTransaction
                    val route = routeId ?: dao.getStop(stopId)?.routeId ?: return@withTransaction
                    dao.deleteStop(stopId)
                    dao.touchRoute(route)
                }
                LibraryDeleteScope.Everywhere -> {
                    dao.getStopsWithLibraryId(id).forEach { stop ->
                        dao.deleteStop(stop.id)
                        dao.touchRoute(stop.routeId)
                    }
                    libraryDao.delete(id)
                }
            }
        }
    }

    /**
     * Attach a library stop to a route. Delivery progress stays per-route.
     * If this route already references the same library id, returns
     * [AddFromLibraryResult.AlreadyOnRoute] instead of inserting a duplicate.
     */
    suspend fun addStopFromLibrary(routeId: Long, libraryStopId: Long): AddFromLibraryResult {
        val lib = libraryDao.getById(libraryStopId) ?: return AddFromLibraryResult.LibraryMissing
        val existing = dao.getRoute(routeId)
            ?.orderedStops
            ?.firstOrNull { it.libraryStopId == libraryStopId }
        if (existing != null) {
            return AddFromLibraryResult.AlreadyOnRoute(existing.id)
        }
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

    suspend fun setStopCompleted(stopId: Long, completed: Boolean): StopCompletionResult {
        val stop = dao.getStop(stopId) ?: return StopCompletionResult.StopMissing
        if (completed) {
            val incompleteRequired = taskDao.incompleteRequiredCount(stopId)
            if (incompleteRequired > 0) {
                return StopCompletionResult.BlockedByRequiredTasks(incompleteRequired)
            }
        }
        dao.setStopCompleted(stopId, completed)
        dao.touchRoute(stop.routeId)
        return StopCompletionResult.Updated
    }

    suspend fun addStopTask(stopId: Long, title: String, required: Boolean): Long? {
        val stop = dao.getStop(stopId) ?: return null
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return null
        val taskId = taskDao.insert(
            StopTaskEntity(
                stopId = stopId,
                title = trimmedTitle,
                isRequired = required,
            ),
        )
        dao.touchRoute(stop.routeId)
        return taskId
    }

    suspend fun updateStopTask(taskId: Long, title: String, required: Boolean) {
        val task = taskDao.getById(taskId) ?: return
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        taskDao.update(task.copy(title = trimmed, isRequired = required))
        dao.getStop(task.stopId)?.let { dao.touchRoute(it.routeId) }
    }

    suspend fun setStopTaskCompleted(taskId: Long, completed: Boolean, note: String) {
        val task = taskDao.getById(taskId) ?: return
        taskDao.update(
            task.copy(
                isCompleted = completed,
                completedAtEpochMs = if (completed) System.currentTimeMillis() else null,
                completionNote = if (completed) note.trim() else "",
            ),
        )
        dao.getStop(task.stopId)?.let { dao.touchRoute(it.routeId) }
    }

    suspend fun updateStopTaskCompletionNote(taskId: Long, note: String) {
        val task = taskDao.getById(taskId) ?: return
        taskDao.update(task.copy(completionNote = note.trim()))
        dao.getStop(task.stopId)?.let { dao.touchRoute(it.routeId) }
    }

    suspend fun deleteStopTask(taskId: Long) {
        val task = taskDao.getById(taskId)
        taskDao.delete(taskId)
        task?.let { dao.getStop(it.stopId)?.let { stop -> dao.touchRoute(stop.routeId) } }
    }

    suspend fun resetStopCompletions(routeId: Long) {
        dao.resetStopCompletions(routeId)
        taskDao.resetCompletionsForRoute(routeId)
        dao.touchRoute(routeId)
    }

    suspend fun addStop(routeId: Long, stop: StopDraft): Long {
        val libraryId = ensureLibraryId(stop)
        val nextPosition = dao.maxStopPosition(routeId) + 1
        val place = resolvedPlace(stop, libraryId)
        val id = dao.insertStop(
            StopEntity(
                routeId = routeId,
                position = nextPosition,
                name = place.name,
                addressHint = place.addressHint,
                notes = place.notes,
                latitude = place.latitude,
                longitude = place.longitude,
                isCompleted = stop.isCompleted,
                libraryStopId = libraryId,
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
        writeStopPositions(routeId, ordered)
    }

    suspend fun reorderStops(routeId: Long, orderedStopIds: List<Long>) {
        if (orderedStopIds.isEmpty()) return
        val current = dao.getRoute(routeId)?.orderedStops ?: return
        if (current.map { it.id }.toSet() != orderedStopIds.toSet()) return
        val byId = current.associateBy { it.id }
        val ordered = orderedStopIds.mapNotNull { byId[it] }
        writeStopPositions(routeId, ordered)
    }

    /**
     * Nearest-neighbor reorder. Completed stops stay first; remaining pinned stops
     * are ordered from [startLatitude]/[startLongitude] or the last completed pin.
     */
    suspend fun optimizeStopOrder(
        routeId: Long,
        startLatitude: Double? = null,
        startLongitude: Double? = null,
    ): Boolean {
        val ordered = dao.getRoute(routeId)?.orderedStops ?: return false
        val optimized = RouteOrderOptimizer.optimizeRemaining(
            ordered,
            isCompleted = { it.isCompleted },
            latitude = { it.latitude },
            longitude = { it.longitude },
            startLatitude = startLatitude,
            startLongitude = startLongitude,
        )
        if (optimized.map { it.id } == ordered.map { it.id }) return false
        writeStopPositions(routeId, optimized)
        return true
    }

    /**
     * Copy a route (stops + tasks) with new remote IDs. Delivery progress is reset.
     */
    suspend fun duplicateRoute(routeId: Long, copySuffix: String): Long? {
        val source = dao.getRoute(routeId) ?: return null
        val drafts = source.orderedStops.map { stop ->
            StopDraft(
                name = stop.name,
                addressHint = stop.addressHint,
                notes = stop.notes,
                latitude = stop.latitude,
                longitude = stop.longitude,
                isCompleted = false,
                libraryStopId = stop.libraryStopId,
            )
        }
        val copiedName = source.route.name.trim().ifEmpty { "Route" }
        val newId = createRoute("$copiedName$copySuffix", source.route.notes, drafts)
        val newStops = dao.getStopsForRoute(newId)
        source.orderedStops.zip(newStops).forEach { (oldStop, newStop) ->
            taskDao.getForStop(oldStop.id).forEach { task ->
                taskDao.insert(
                    StopTaskEntity(
                        remoteId = newRemoteId(),
                        stopId = newStop.id,
                        title = task.title,
                        isRequired = task.isRequired,
                    ),
                )
            }
        }
        return newId
    }

    private suspend fun writeStopPositions(routeId: Long, ordered: List<StopEntity>) {
        ordered.forEachIndexed { position, stop ->
            if (stop.position != position) {
                dao.updateStop(stop.copy(position = position))
            }
        }
        dao.touchRoute(routeId)
    }

    private suspend fun replaceStops(routeId: Long, stops: List<StopDraft>) {
        val current = dao.getStopsForRoute(routeId)
        val usedIds = mutableSetOf<Long>()
        val remaining = current.toMutableList()
        val nextRows = stops.mapIndexed { index, draft ->
            val matched = draft.libraryStopId?.let { libId ->
                remaining.firstOrNull { it.libraryStopId == libId && it.id !in usedIds }
            } ?: remaining.firstOrNull {
                it.id !in usedIds &&
                    it.name == draft.name.trim() &&
                    it.latitude == draft.latitude &&
                    it.longitude == draft.longitude
            }
            val libraryId = ensureLibraryId(
                draft.copy(libraryStopId = draft.libraryStopId ?: matched?.libraryStopId),
            )
            val place = resolvedPlace(draft, libraryId)
            if (matched != null) {
                usedIds += matched.id
                matched.copy(
                    position = index,
                    name = place.name,
                    addressHint = place.addressHint,
                    notes = place.notes,
                    latitude = place.latitude,
                    longitude = place.longitude,
                    isCompleted = draft.isCompleted,
                    libraryStopId = libraryId,
                )
            } else {
                StopEntity(
                    routeId = routeId,
                    position = index,
                    name = place.name,
                    addressHint = place.addressHint,
                    notes = place.notes,
                    latitude = place.latitude,
                    longitude = place.longitude,
                    isCompleted = draft.isCompleted,
                    libraryStopId = libraryId,
                )
            }
        }
        current.filter { it.id !in usedIds }.forEach { dao.deleteStop(it.id) }
        nextRows.forEach { stop ->
            if (stop.id == 0L) {
                dao.insertStop(stop)
            } else {
                dao.updateStop(stop)
            }
        }
        nextRows.mapNotNull { it.libraryStopId }.distinct().forEach { libraryId ->
            val sample = nextRows.first { it.libraryStopId == libraryId }
            val lat = sample.latitude ?: return@forEach
            val lng = sample.longitude ?: return@forEach
            upsertLibraryStop(
                name = sample.name,
                addressHint = sample.addressHint,
                notes = sample.notes,
                latitude = lat,
                longitude = lng,
                existingId = libraryId,
            )
            dao.propagateLibraryPlace(
                libraryStopId = libraryId,
                name = sample.name,
                addressHint = sample.addressHint,
                notes = sample.notes,
                latitude = lat,
                longitude = lng,
            )
        }
    }

    private suspend fun ensureLibraryId(draft: StopDraft): Long? {
        draft.libraryStopId?.let { existing ->
            if (libraryDao.getById(existing) != null) return existing
        }
        val lat = draft.latitude ?: return null
        val lng = draft.longitude ?: return null
        return upsertLibraryStop(
            name = draft.name,
            addressHint = draft.addressHint,
            notes = draft.notes,
            latitude = lat,
            longitude = lng,
        )
    }

    private suspend fun resolvedPlace(draft: StopDraft, libraryId: Long?): PlaceFields {
        val lib = libraryId?.let { libraryDao.getById(it) }
        return PlaceFields(
            name = draft.name.trim().ifEmpty { lib?.name.orEmpty() },
            addressHint = draft.addressHint.trim(),
            notes = draft.notes.trim(),
            latitude = draft.latitude ?: lib?.latitude,
            longitude = draft.longitude ?: lib?.longitude,
        )
    }

    suspend fun exportBackupJson(): String = backupManager.exportJson()

    suspend fun importBackupJson(json: String): BackupImportResult = backupManager.importJson(json)
}

private data class PlaceFields(
    val name: String,
    val addressHint: String,
    val notes: String,
    val latitude: Double?,
    val longitude: Double?,
)
