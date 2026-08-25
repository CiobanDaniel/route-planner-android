package com.danielcioban.routeplanner.data

import androidx.room.withTransaction
import com.danielcioban.routeplanner.data.backup.BackupImportOptions
import com.danielcioban.routeplanner.data.backup.BackupImportResult
import com.danielcioban.routeplanner.data.backup.BackupPreview
import com.danielcioban.routeplanner.data.backup.CsvImportResult
import com.danielcioban.routeplanner.data.backup.CsvStopExporter
import com.danielcioban.routeplanner.data.backup.CsvStopImporter
import com.danielcioban.routeplanner.data.backup.PlaceFormatExporter
import com.danielcioban.routeplanner.data.backup.RouteBackupManager
import com.danielcioban.routeplanner.data.backup.TripHistoryCsv
import com.danielcioban.routeplanner.data.local.AppDatabase
import com.danielcioban.routeplanner.data.local.FuelLogEntity
import com.danielcioban.routeplanner.data.local.LibraryDefaultTaskEntity
import com.danielcioban.routeplanner.data.local.RouteEntity
import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.data.local.SavedSearchEntity
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.local.StopFailureReason
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import com.danielcioban.routeplanner.data.local.StopTaskEntity
import com.danielcioban.routeplanner.data.local.StopTaskProgress
import com.danielcioban.routeplanner.data.local.TaskTemplateEntity
import com.danielcioban.routeplanner.data.local.TripHistoryEntity
import com.danielcioban.routeplanner.data.local.TripKind
import com.danielcioban.routeplanner.data.local.TripStatus
import com.danielcioban.routeplanner.data.settings.RouteGeofenceMode
import com.danielcioban.routeplanner.data.settings.StopGeofence
import com.danielcioban.routeplanner.data.routing.OsrmRoutingClient
import com.danielcioban.routeplanner.ui.map.LatLng
import com.danielcioban.routeplanner.util.LibraryDuplicates
import com.danielcioban.routeplanner.util.LibraryTags
import com.danielcioban.routeplanner.util.OpenLocationCode
import com.danielcioban.routeplanner.util.RouteEta
import com.danielcioban.routeplanner.util.RouteOrderOptimizer
import com.danielcioban.routeplanner.util.newRemoteId
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

data class StopDraft(
    val name: String,
    val addressHint: String = "",
    val notes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isCompleted: Boolean = false,
    val libraryStopId: Long? = null,
    val arriveByMinutes: Int? = null,
    val serviceMinutes: Int = 0,
    val geofenceRadiusMeters: Int? = null,
    /** GPS start of the route; skip writing a library pin. */
    val isOrigin: Boolean = false,
    /** When true, do not create or attach a library entry. */
    val skipLibrary: Boolean = false,
    val arriveByEpochMs: Long? = null,
    val isFixedOrder: Boolean = false,
    val isBreak: Boolean = false,
    val phone: String = "",
    val doorCode: String = "",
    val codAmount: Double = 0.0,
    val barcode: String = "",
)

sealed class AddFromLibraryResult {
    data class Added(val stopId: Long) : AddFromLibraryResult()
    data class AlreadyOnRoute(val stopId: Long) : AddFromLibraryResult()
    data object LibraryMissing : AddFromLibraryResult()
}

sealed class DuplicateStopResult {
    data class Copied(val stopId: Long) : DuplicateStopResult()
    data class AlreadyOnRoute(val stopId: Long) : DuplicateStopResult()
    data object SameRoute : DuplicateStopResult()
    data object Failed : DuplicateStopResult()
}

sealed class StopCompletionResult {
    data object Updated : StopCompletionResult()
    data class BlockedByRequiredTasks(val incompleteCount: Int) : StopCompletionResult()
    data object StopMissing : StopCompletionResult()
}

data class BulkCompleteResult(
    val completedCount: Int,
    val blockedStopName: String? = null,
    val blockedTaskCount: Int = 0,
)

class RouteRepository(
    private val db: AppDatabase,
    private val routingClient: OsrmRoutingClient? = OsrmRoutingClient(),
) {
    private val dao = db.routeDao()
    private val libraryDao = db.stopLibraryDao()
    private val taskDao = db.stopTaskDao()
    private val tripHistoryDao = db.tripHistoryDao()
    private val templateDao = db.taskTemplateDao()
    private val libraryDefaultTaskDao = db.libraryDefaultTaskDao()
    private val savedSearchDao = db.savedSearchDao()
    private val fuelLogDao = db.fuelLogDao()
    private val backupManager = RouteBackupManager(
        dao,
        libraryDao,
        taskDao,
        tripHistoryDao,
        templateDao,
        libraryDefaultTaskDao,
        savedSearchDao,
        fuelLogDao,
    )

    fun observeRoutes(): Flow<List<RouteWithStops>> = dao.observeRoutes()

    fun observeTrashRoutes(): Flow<List<RouteWithStops>> = dao.observeTrashRoutes()

    fun observeTrashLibrary(): Flow<List<StopLibraryEntity>> = libraryDao.observeTrash()

    fun observeRoute(routeId: Long): Flow<RouteWithStops?> = dao.observeRoute(routeId)

    fun observeStopLibrary(): Flow<List<StopLibraryEntity>> = libraryDao.observeAll()

    fun observeStopTasks(stopId: Long): Flow<List<StopTaskEntity>> = taskDao.observeForStop(stopId)

    fun observeStopTaskProgress(routeId: Long): Flow<List<StopTaskProgress>> =
        taskDao.observeProgressForRoute(routeId)

    fun observeTripHistory(): Flow<List<TripHistoryEntity>> = tripHistoryDao.observeAll()

    fun observeTaskTemplates(): Flow<List<TaskTemplateEntity>> = templateDao.observeAll()

    fun observeSavedSearches(): Flow<List<SavedSearchEntity>> = savedSearchDao.observeAll()

    fun observeFuelLog(): Flow<List<FuelLogEntity>> = fuelLogDao.observeAll()

    fun observeLibraryDefaultTasks(libraryStopId: Long): Flow<List<LibraryDefaultTaskEntity>> =
        libraryDefaultTaskDao.observeForLibraryStop(libraryStopId)

    suspend fun getTrip(id: Long): TripHistoryEntity? = tripHistoryDao.getById(id)

    suspend fun getRoute(routeId: Long): RouteWithStops? = dao.getRoute(routeId)

    suspend fun getLibraryStop(id: Long): StopLibraryEntity? = libraryDao.getById(id)

    suspend fun getLibraryStopByRemoteId(remoteId: String): StopLibraryEntity? =
        libraryDao.getByRemoteId(remoteId)

    suspend fun upsertLibraryStop(
        name: String,
        addressHint: String,
        notes: String,
        latitude: Double,
        longitude: Double,
        existingId: Long? = null,
        extras: LibraryPlaceExtras? = null,
    ): Long {
        val now = System.currentTimeMillis()
        val encodedPlus = OpenLocationCode.encode(latitude, longitude)
        val entity = if (existingId != null) {
            libraryDao.getById(existingId)?.let { current ->
                current.copy(
                    name = name.trim(),
                    addressHint = addressHint.trim(),
                    notes = notes.trim(),
                    latitude = latitude,
                    longitude = longitude,
                    updatedAtEpochMs = now,
                    tags = extras?.tags?.let { LibraryTags.join(it) } ?: current.tags,
                    what3words = extras?.what3words?.trim() ?: current.what3words,
                    plusCode = extras?.plusCode?.trim()?.ifBlank { encodedPlus } ?: current.plusCode.ifBlank { encodedPlus },
                    defaultGeofenceRadiusMeters = if (extras?.setDefaultGeofence == true) {
                        extras.defaultGeofenceRadiusMeters?.let(StopGeofence::clampRadius)
                    } else {
                        current.defaultGeofenceRadiusMeters
                    },
                    isFavorite = extras?.isFavorite ?: current.isFavorite,
                )
            } ?: StopLibraryEntity(
                name = name.trim(),
                addressHint = addressHint.trim(),
                notes = notes.trim(),
                latitude = latitude,
                longitude = longitude,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
                tags = extras?.tags?.let(LibraryTags::join).orEmpty(),
                what3words = extras?.what3words?.trim().orEmpty(),
                plusCode = extras?.plusCode?.trim()?.ifBlank { encodedPlus } ?: encodedPlus,
                defaultGeofenceRadiusMeters = extras?.defaultGeofenceRadiusMeters?.let(StopGeofence::clampRadius),
                isFavorite = extras?.isFavorite ?: false,
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
                tags = extras?.tags?.let(LibraryTags::join).orEmpty(),
                what3words = extras?.what3words?.trim().orEmpty(),
                plusCode = extras?.plusCode?.trim()?.ifBlank { encodedPlus } ?: encodedPlus,
                defaultGeofenceRadiusMeters = extras?.defaultGeofenceRadiusMeters?.let(StopGeofence::clampRadius),
                isFavorite = extras?.isFavorite ?: false,
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
        extras: LibraryPlaceExtras? = null,
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
                        extras = extras,
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
                        extras = extras,
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
                        extras = extras,
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
                    val lib = libraryDao.getById(id) ?: return@withTransaction
                    libraryDao.update(
                        lib.copy(
                            deletedAtEpochMs = System.currentTimeMillis(),
                            updatedAtEpochMs = System.currentTimeMillis(),
                        ),
                    )
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
        if (lib.deletedAtEpochMs != null) return AddFromLibraryResult.LibraryMissing
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
                geofenceRadiusMeters = lib.defaultGeofenceRadiusMeters,
            ),
        )
        copyLibraryDefaultTasks(stopId, lib.id)
        recordLibraryUse(lib.id)
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

    suspend fun createRoute(
        name: String,
        notes: String,
        stops: List<StopDraft>,
        roundTrip: Boolean = false,
        geofenceMode: String = RouteGeofenceMode.INHERIT.name,
        geofenceRadiusMeters: Int? = null,
        colorHex: String = "",
        vanName: String = "",
        shiftName: String = "",
        shiftSlot: String = "",
        originLatitude: Double? = null,
        originLongitude: Double? = null,
    ): Long {
        val now = System.currentTimeMillis()
        val routeId = dao.insertRoute(
            RouteEntity(
                name = name.trim(),
                notes = notes.trim(),
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
                roundTrip = roundTrip,
                geofenceMode = RouteGeofenceMode.fromStored(geofenceMode).name,
                geofenceRadiusMeters = geofenceRadiusMeters?.let(StopGeofence::clampRadius),
                colorHex = colorHex.trim(),
                vanName = vanName.trim(),
                shiftName = shiftName.trim(),
                shiftSlot = com.danielcioban.routeplanner.data.local.ShiftSlot.fromStored(shiftSlot),
                originLatitude = originLatitude,
                originLongitude = originLongitude,
            ),
        )
        replaceStops(routeId, stops)
        return routeId
    }

    suspend fun updateRoute(
        routeId: Long,
        name: String,
        notes: String,
        stops: List<StopDraft>,
        roundTrip: Boolean = false,
        geofenceMode: String = RouteGeofenceMode.INHERIT.name,
        geofenceRadiusMeters: Int? = null,
        colorHex: String? = null,
        vanName: String? = null,
        shiftName: String? = null,
        shiftSlot: String? = null,
    ) {
        val existing = dao.getRoute(routeId)?.route ?: return
        dao.updateRoute(
            existing.copy(
                name = name.trim(),
                notes = notes.trim(),
                updatedAtEpochMs = System.currentTimeMillis(),
                roundTrip = roundTrip,
                geofenceMode = RouteGeofenceMode.fromStored(geofenceMode).name,
                geofenceRadiusMeters = geofenceRadiusMeters?.let(StopGeofence::clampRadius),
                colorHex = colorHex?.trim() ?: existing.colorHex,
                vanName = vanName?.trim() ?: existing.vanName,
                shiftName = shiftName?.trim() ?: existing.shiftName,
                shiftSlot = shiftSlot?.let {
                    com.danielcioban.routeplanner.data.local.ShiftSlot.fromStored(it)
                } ?: existing.shiftSlot,
            ),
        )
        replaceStops(routeId, stops)
    }

    suspend fun setRouteArchived(routeId: Long, archived: Boolean) {
        val existing = dao.getRoute(routeId)?.route ?: return
        dao.updateRoute(
            existing.copy(
                archived = archived,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun setRouteMeta(
        routeId: Long,
        colorHex: String? = null,
        vanName: String? = null,
        shiftName: String? = null,
    ) {
        val existing = dao.getRoute(routeId)?.route ?: return
        dao.updateRoute(
            existing.copy(
                colorHex = colorHex?.trim() ?: existing.colorHex,
                vanName = vanName?.trim() ?: existing.vanName,
                shiftName = shiftName?.trim() ?: existing.shiftName,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun deleteRoute(routeId: Long) {
        val existing = dao.getRoute(routeId)?.route ?: return
        dao.updateRoute(
            existing.copy(
                deletedAtEpochMs = System.currentTimeMillis(),
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun restoreRoute(routeId: Long) {
        val existing = dao.getRoute(routeId)?.route ?: return
        dao.updateRoute(
            existing.copy(
                deletedAtEpochMs = null,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun purgeRoute(routeId: Long) {
        dao.deleteRoute(routeId)
    }

    suspend fun restoreLibraryStop(id: Long) {
        val lib = libraryDao.getById(id) ?: return
        libraryDao.update(
            lib.copy(
                deletedAtEpochMs = null,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun purgeLibraryStop(id: Long) {
        libraryDao.delete(id)
    }

    suspend fun setStopCompleted(
        stopId: Long,
        completed: Boolean,
        deferRequiredTasks: Boolean = false,
        deferNote: String = "",
    ): StopCompletionResult {
        val stop = dao.getStop(stopId) ?: return StopCompletionResult.StopMissing
        if (stop.isCompleted == completed) return StopCompletionResult.Updated
        if (completed) {
            val incompleteRequired = taskDao.incompleteRequiredCount(stopId)
            if (incompleteRequired > 0 && !deferRequiredTasks && !stop.isBreak) {
                return StopCompletionResult.BlockedByRequiredTasks(incompleteRequired)
            }
        }
        val now = System.currentTimeMillis()
        dao.updateStop(
            stop.copy(
                isCompleted = completed,
                isVisited = if (completed) true else stop.isVisited,
                visitedAtEpochMs = if (completed && stop.visitedAtEpochMs == null) {
                    now
                } else {
                    stop.visitedAtEpochMs
                },
                failureReason = if (completed && !deferRequiredTasks) null else stop.failureReason,
                tasksDeferred = completed && deferRequiredTasks,
                tasksDeferredNote = if (completed && deferRequiredTasks) deferNote.trim() else "",
                failurePhotoPath = if (completed) stop.failurePhotoPath else null,
                failureSignaturePath = if (completed) stop.failureSignaturePath else null,
                podCapturedAtEpochMs = if (completed) {
                    stop.podCapturedAtEpochMs ?: now
                } else {
                    stop.podCapturedAtEpochMs
                },
            ),
        )
        dao.touchRoute(stop.routeId)
        return StopCompletionResult.Updated
    }

    /**
     * Mark unfinished delivery stops before [targetStopId] complete (jump-ahead).
     * Required-task gate still applies. Does not complete the target itself.
     */
    suspend fun completeStopsBefore(routeId: Long, targetStopId: Long): StopCompletionResult {
        val stops = getRoute(routeId)?.deliveryStops.orEmpty()
        if (stops.none { it.id == targetStopId }) return StopCompletionResult.StopMissing
        for (stop in stops) {
            if (stop.id == targetStopId) return StopCompletionResult.Updated
            if (stop.isCompleted) continue
            when (val result = setStopCompleted(stop.id, true)) {
                StopCompletionResult.Updated -> Unit
                else -> return result
            }
        }
        return StopCompletionResult.Updated
    }

    suspend fun markStopVisited(stopId: Long) {
        val stop = dao.getStop(stopId) ?: return
        if (stop.isVisited) return
        dao.updateStop(
            stop.copy(
                isVisited = true,
                visitedAtEpochMs = System.currentTimeMillis(),
            ),
        )
        dao.touchRoute(stop.routeId)
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

    /**
     * Mark a stop failed (not home / refused / closed / other). Completes it and
     * skips the required-task gate — the courier cannot finish tasks if nobody is there.
     */
    suspend fun failStop(
        stopId: Long,
        reason: String,
        photoPath: String? = null,
        signaturePath: String? = null,
    ): StopCompletionResult {
        val stop = dao.getStop(stopId) ?: return StopCompletionResult.StopMissing
        val normalized = if (reason in StopFailureReason.all) reason else StopFailureReason.OTHER
        val now = System.currentTimeMillis()
        dao.updateStop(
            stop.copy(
                isCompleted = true,
                isVisited = true,
                visitedAtEpochMs = stop.visitedAtEpochMs ?: now,
                failureReason = normalized,
                failurePhotoPath = photoPath,
                failureSignaturePath = signaturePath,
                tasksDeferred = false,
                tasksDeferredNote = "",
            ),
        )
        dao.touchRoute(stop.routeId)
        return StopCompletionResult.Updated
    }

    suspend fun saveProofOfDelivery(
        stopId: Long,
        photoPath: String?,
        signaturePath: String?,
        capturedAtEpochMs: Long = System.currentTimeMillis(),
    ) {
        val stop = dao.getStop(stopId) ?: return
        dao.updateStop(
            stop.copy(
                podPhotoPath = photoPath ?: stop.podPhotoPath,
                podSignaturePath = signaturePath ?: stop.podSignaturePath,
                podCapturedAtEpochMs = capturedAtEpochMs,
            ),
        )
        dao.touchRoute(stop.routeId)
    }

    suspend fun setCodCollected(stopId: Long, collected: Boolean) {
        val stop = dao.getStop(stopId) ?: return
        dao.updateStop(stop.copy(codCollected = collected && stop.codAmount > 0.0))
        dao.touchRoute(stop.routeId)
    }

    suspend fun setFavorite(libraryStopId: Long, favorite: Boolean) {
        val lib = libraryDao.getById(libraryStopId) ?: return
        libraryDao.update(lib.copy(isFavorite = favorite, updatedAtEpochMs = System.currentTimeMillis()))
    }

    suspend fun matchScan(routeId: Long, raw: String): com.danielcioban.routeplanner.util.BarcodeMatch.Result {
        val route = dao.getRoute(routeId) ?: return com.danielcioban.routeplanner.util.BarcodeMatch.Result.None
        val tasks = route.deliveryStops.associate { stop ->
            stop.id to taskDao.getForStop(stop.id)
        }
        val libraries = libraryDao.getAll().associateBy { it.id }
        return com.danielcioban.routeplanner.util.BarcodeMatch.match(
            raw,
            route.deliveryStops,
            tasks,
            libraries,
        )
    }

    suspend fun collectedCodTotal(): Double {
        return dao.getAllRoutes().sumOf { route ->
            dao.getStopsForRoute(route.id).filter { it.codCollected }.sumOf { it.codAmount }
        }
    }

    suspend fun addFuelLog(
        odometerKm: Double?,
        liters: Double?,
        amount: Double?,
        notes: String,
        loggedAtEpochMs: Long = System.currentTimeMillis(),
    ): Long {
        return fuelLogDao.insert(
            FuelLogEntity(
                loggedAtEpochMs = loggedAtEpochMs,
                odometerKm = odometerKm,
                liters = liters,
                amount = amount,
                notes = notes.trim(),
            ),
        )
    }

    suspend fun deleteFuelLog(id: Long) {
        fuelLogDao.delete(id)
    }

    /**
     * Move an unfinished stop to the end of the route. Optional new arrive-by, or
     * [addMinutes] added to the current promise (or now if none).
     */
    suspend fun rescheduleStop(
        stopId: Long,
        arriveByMinutes: Int? = null,
        addMinutes: Int? = null,
        arriveByEpochMs: Long? = null,
    ) {
        val stop = dao.getStop(stopId) ?: return
        val now = System.currentTimeMillis()
        val nextEpoch = when {
            arriveByEpochMs != null -> arriveByEpochMs
            addMinutes != null -> (stop.arriveByEpochMs ?: now) +
                addMinutes.coerceAtLeast(0) * 60_000L
            else -> stop.arriveByEpochMs
        }
        val nextArrive = when {
            nextEpoch != null -> RouteEta.minutesFromMidnight(nextEpoch)
            addMinutes != null -> {
                val base = stop.arriveByMinutes ?: minutesFromMidnight()
                (base + addMinutes).mod(24 * 60)
            }
            arriveByMinutes != null -> arriveByMinutes.coerceIn(0, 23 * 60 + 59)
            else -> stop.arriveByMinutes
        }
        dao.updateStop(
            stop.copy(
                isCompleted = false,
                failureReason = null,
                arriveByMinutes = nextArrive,
                arriveByEpochMs = nextEpoch,
            ),
        )
        moveStopToEdge(stop.routeId, stopId, toStart = false)
    }

    /**
     * Promote leftover origin stop rows onto the route, then delete them.
     * GPS origin is not a delivery and is not auto-completed on Start.
     */
    suspend fun completeOriginStops(routeId: Long) {
        val snapshot = dao.getRoute(routeId) ?: return
        val origins = dao.getOriginStops(routeId)
        if (origins.isEmpty()) return
        val first = origins.firstOrNull { it.latitude != null && it.longitude != null }
        if (snapshot.route.originLatitude == null && first != null) {
            dao.updateRoute(
                snapshot.route.copy(
                    originLatitude = first.latitude,
                    originLongitude = first.longitude,
                    updatedAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }
        origins.forEach { dao.deleteStop(it.id) }
        dao.touchRoute(routeId)
    }

    /**
     * Set the GPS start of the route. Not a list row and not written to the library.
     */
    suspend fun setGpsOrigin(
        routeId: Long,
        name: String,
        latitude: Double,
        longitude: Double,
    ): Long {
        val existing = dao.getRoute(routeId)?.route ?: return 0L
        dao.updateRoute(
            existing.copy(
                originLatitude = latitude,
                originLongitude = longitude,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
        dao.getOriginStops(routeId).forEach { dao.deleteStop(it.id) }
        return 0L
    }

    suspend fun completeRemainingStops(routeId: Long): BulkCompleteResult {
        val remaining = dao.getStopsForRoute(routeId).filter { !it.isOrigin && !it.isCompleted }
        var completed = 0
        for (stop in remaining) {
            when (val result = setStopCompleted(stop.id, true)) {
                StopCompletionResult.Updated -> completed++
                is StopCompletionResult.BlockedByRequiredTasks -> {
                    return BulkCompleteResult(
                        completedCount = completed,
                        blockedStopName = stop.name,
                        blockedTaskCount = result.incompleteCount,
                    )
                }
                StopCompletionResult.StopMissing -> Unit
            }
        }
        return BulkCompleteResult(completedCount = completed)
    }

    suspend fun ensureDefaultTaskTemplates(seeds: List<Pair<String, Boolean>>) {
        if (templateDao.count() > 0) return
        seeds.forEachIndexed { index, (title, required) ->
            val trimmed = title.trim()
            if (trimmed.isEmpty()) return@forEachIndexed
            templateDao.insert(
                TaskTemplateEntity(
                    title = trimmed,
                    isRequired = required,
                    sortOrder = index,
                ),
            )
        }
    }

    suspend fun addTaskTemplate(title: String, required: Boolean): Long? {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return null
        val nextOrder = (templateDao.getAll().maxOfOrNull { it.sortOrder } ?: -1) + 1
        return templateDao.insert(
            TaskTemplateEntity(title = trimmed, isRequired = required, sortOrder = nextOrder),
        )
    }

    suspend fun deleteTaskTemplate(id: Long) {
        templateDao.delete(id)
    }

    suspend fun applyTaskTemplate(stopId: Long, templateId: Long): Long? {
        val template = templateDao.getAll().firstOrNull { it.id == templateId } ?: return null
        return addStopTask(stopId, template.title, template.isRequired)
    }

    /** Copy a template onto this stop and every later unfinished stop on the same route. */
    suspend fun applyTaskTemplateToRemaining(stopId: Long, templateId: Long) {
        val stop = dao.getStop(stopId) ?: return
        val template = templateDao.getAll().firstOrNull { it.id == templateId } ?: return
        val targets = dao.getStopsForRoute(stop.routeId).filter {
            !it.isOrigin && !it.isCompleted && it.position >= stop.position
        }
        targets.forEach { addStopTask(it.id, template.title, template.isRequired) }
    }

    suspend fun resetStopCompletions(routeId: Long) {
        dao.resetStopCompletions(routeId)
        taskDao.resetCompletionsForRoute(routeId)
        dao.touchRoute(routeId)
    }

    suspend fun addStop(routeId: Long, stop: StopDraft): Long {
        if (stop.isOrigin) {
            val lat = stop.latitude ?: return 0L
            val lng = stop.longitude ?: return 0L
            return setGpsOrigin(routeId, stop.name, lat, lng)
        }
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
                arriveByMinutes = stop.arriveByMinutes,
                serviceMinutes = stop.serviceMinutes.coerceAtLeast(0),
                geofenceRadiusMeters = stop.geofenceRadiusMeters?.let(StopGeofence::clampRadius)
                    ?: libraryId?.let { libraryDao.getById(it)?.defaultGeofenceRadiusMeters }
                        ?.let(StopGeofence::clampRadius),
                isOrigin = false,
                arriveByEpochMs = stop.arriveByEpochMs,
                isFixedOrder = stop.isFixedOrder,
                isBreak = stop.isBreak,
                phone = stop.phone.trim(),
                doorCode = stop.doorCode.trim(),
                codAmount = stop.codAmount.coerceAtLeast(0.0),
                barcode = stop.barcode.trim(),
            ),
        )
        dao.touchRoute(routeId)
        copyLibraryDefaultTasks(id, libraryId)
        return id
    }

    suspend fun recordLibraryUse(libraryStopId: Long) {
        val lib = libraryDao.getById(libraryStopId) ?: return
        libraryDao.update(
            lib.copy(
                lastUsedAtEpochMs = System.currentTimeMillis(),
                useCount = lib.useCount + 1,
            ),
        )
    }

    private suspend fun copyLibraryDefaultTasks(stopId: Long, libraryStopId: Long?) {
        val libId = libraryStopId ?: return
        if (taskDao.getForStop(stopId).isNotEmpty()) return
        libraryDefaultTaskDao.getForLibraryStop(libId).forEach { template ->
            addStopTask(stopId, template.title, template.isRequired)
        }
    }

    suspend fun addLibraryDefaultTask(libraryStopId: Long, title: String, required: Boolean): Long? {
        val trimmed = title.trim()
        if (trimmed.isEmpty() || libraryDao.getById(libraryStopId) == null) return null
        val nextOrder = libraryDefaultTaskDao.getForLibraryStop(libraryStopId).size
        return libraryDefaultTaskDao.insert(
            LibraryDefaultTaskEntity(
                libraryStopId = libraryStopId,
                title = trimmed,
                isRequired = required,
                sortOrder = nextOrder,
            ),
        )
    }

    suspend fun deleteLibraryDefaultTask(id: Long) {
        libraryDefaultTaskDao.delete(id)
    }

    suspend fun saveSearch(query: String, nearMeOnly: Boolean) {
        val trimmed = query.trim()
        if (trimmed.length < 3 && !OpenLocationCode.isFullCode(trimmed)) return
        savedSearchDao.upsert(
            SavedSearchEntity(query = trimmed, nearMeOnly = nearMeOnly),
        )
    }

    suspend fun deleteSavedSearch(id: Long) {
        savedSearchDao.delete(id)
    }

    suspend fun mergeLibraryStops(keepId: Long, dropId: Long) {
        if (keepId == dropId) return
        val keep = libraryDao.getById(keepId) ?: return
        val drop = libraryDao.getById(dropId) ?: return
        db.withTransaction {
            dao.reassignLibraryStop(dropId, keepId)
            val keepTasks = libraryDefaultTaskDao.getForLibraryStop(keepId)
            val keepTitles = keepTasks.map { it.title.trim().lowercase() }.toMutableSet()
            libraryDefaultTaskDao.getForLibraryStop(dropId).forEach { task ->
                val key = task.title.trim().lowercase()
                if (key.isNotEmpty() && keepTitles.add(key)) {
                    libraryDefaultTaskDao.insert(
                        LibraryDefaultTaskEntity(
                            libraryStopId = keepId,
                            title = task.title,
                            isRequired = task.isRequired,
                            sortOrder = keepTitles.size,
                        ),
                    )
                }
            }
            libraryDao.update(
                keep.copy(
                    addressHint = keep.addressHint.ifBlank { drop.addressHint },
                    notes = listOf(keep.notes, drop.notes)
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                        .joinToString("\n"),
                    tags = LibraryTags.merge(keep.tags, drop.tags),
                    what3words = keep.what3words.ifBlank { drop.what3words },
                    plusCode = keep.plusCode.ifBlank { drop.plusCode },
                    lastUsedAtEpochMs = maxOf(keep.lastUsedAtEpochMs, drop.lastUsedAtEpochMs),
                    useCount = keep.useCount + drop.useCount,
                    defaultGeofenceRadiusMeters = keep.defaultGeofenceRadiusMeters
                        ?: drop.defaultGeofenceRadiusMeters,
                    updatedAtEpochMs = System.currentTimeMillis(),
                ),
            )
            libraryDao.delete(dropId)
            dao.getStopsWithLibraryId(keepId).forEach { dao.touchRoute(it.routeId) }
        }
    }

    suspend fun mergeNearbyLibraryDuplicates(): Int {
        val pairs = LibraryDuplicates.pairs(libraryDao.getAll())
        var merged = 0
        val dropped = mutableSetOf<Long>()
        for ((a, b) in pairs) {
            if (a.id in dropped || b.id in dropped) continue
            val keep = LibraryDuplicates.preferKeep(a, b)
            val drop = if (keep.id == a.id) b else a
            mergeLibraryStops(keep.id, drop.id)
            dropped += drop.id
            merged++
        }
        return merged
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

    suspend fun moveStopToEdge(routeId: Long, stopId: Long, toStart: Boolean) {
        val ordered = dao.getRoute(routeId)?.orderedStops?.toMutableList() ?: return
        val index = ordered.indexOfFirst { it.id == stopId }
        if (index < 0) return
        val dest = if (toStart) 0 else ordered.lastIndex
        if (index == dest) return
        val moved = ordered.removeAt(index)
        ordered.add(dest, moved)
        writeStopPositions(routeId, ordered)
    }

    /**
     * Nearest-neighbor + haversine 2-opt, then OSRM duration-matrix 2-opt when the
     * remaining pinned set is small enough. Completed stops stay first.
     */
    suspend fun optimizeStopOrder(
        routeId: Long,
        startLatitude: Double? = null,
        startLongitude: Double? = null,
        profile: String = "driving",
        exclude: String? = null,
        useRoadMatrix: Boolean = true,
    ): Boolean {
        val snapshot = dao.getRoute(routeId) ?: return false
        val ordered = snapshot.orderedStops
        val origins = ordered.filter { it.isOrigin }
        val rest = ordered.filter { !it.isOrigin }
        val roundTrip = snapshot.route.roundTrip
        val originLat = snapshot.route.originLatitude ?: startLatitude
        val originLng = snapshot.route.originLongitude ?: startLongitude
        val haversine = RouteOrderOptimizer.optimizeRemaining(
            rest,
            isCompleted = { it.isCompleted },
            latitude = { it.latitude },
            longitude = { it.longitude },
            startLatitude = originLat,
            startLongitude = originLng,
            roundTrip = roundTrip,
            isFixedOrder = { it.isFixedOrder },
            arriveByEpochMs = { it.arriveByEpochMs },
            arriveByMinutes = { it.arriveByMinutes },
            serviceMinutes = { it.serviceMinutes },
        )
        val optimizedRest = if (useRoadMatrix) {
            refineWithRoadDurations(
                haversine,
                startLatitude = originLat,
                startLongitude = originLng,
                roundTrip = roundTrip,
                profile = profile,
                exclude = exclude,
            )
        } else {
            haversine
        }
        val optimized = origins + optimizedRest
        if (optimized.map { it.id } == ordered.map { it.id }) return false
        writeStopPositions(routeId, optimized)
        return true
    }

    private suspend fun refineWithRoadDurations(
        ordered: List<StopEntity>,
        startLatitude: Double?,
        startLongitude: Double?,
        roundTrip: Boolean,
        profile: String,
        exclude: String?,
    ): List<StopEntity> {
        val completed = ordered.filter { it.isCompleted }
        val remaining = ordered.filterNot { it.isCompleted }
        val pinned = remaining.filter { it.latitude != null && it.longitude != null }
        val unpinned = remaining.filter { it.latitude == null || it.longitude == null }
        if (pinned.size < 2 || pinned.size > MAX_ROAD_OPTIMIZE_STOPS) return ordered

        val client = routingClient ?: return ordered
        val lastPin = completed.lastOrNull { it.latitude != null && it.longitude != null }
        val originLat = startLatitude ?: lastPin?.latitude
        val originLng = startLongitude ?: lastPin?.longitude
        val hasOrigin = originLat != null && originLng != null

        val points = buildList {
            if (hasOrigin) add(LatLng(originLat!!, originLng!!))
            pinned.forEach { add(LatLng(it.latitude!!, it.longitude!!)) }
        }
        val matrix = client.tableDurations(points, profile = profile, exclude = exclude)
            .getOrNull() ?: return ordered
        val improved = RouteOrderOptimizer.twoOptWithDurationMatrix(
            pinned,
            matrix,
            hasOrigin = hasOrigin,
            roundTrip = roundTrip,
        )
        val windowed = RouteOrderOptimizer.enforceTimeWindows(
            improved,
            latitude = { it.latitude },
            longitude = { it.longitude },
            originLat = originLat,
            originLng = originLng,
            roundTrip = roundTrip,
            isFixedOrder = { it.isFixedOrder },
            arriveByEpochMs = { it.arriveByEpochMs },
            arriveByMinutes = { it.arriveByMinutes },
            serviceMinutes = { it.serviceMinutes },
            nowEpochMs = System.currentTimeMillis(),
        )
        return completed + windowed + unpinned
    }

    suspend fun reverseRemainingStops(routeId: Long): Boolean {
        val ordered = dao.getRoute(routeId)?.orderedStops ?: return false
        val reversed = RouteOrderOptimizer.reverseRemaining(ordered) { it.isCompleted }
        if (reversed.map { it.id } == ordered.map { it.id }) return false
        writeStopPositions(routeId, reversed)
        return true
    }

    suspend fun sortRemainingByArriveBy(routeId: Long): Boolean {
        val ordered = dao.getRoute(routeId)?.orderedStops ?: return false
        val sorted = RouteOrderOptimizer.sortRemainingByArriveBy(
            ordered,
            isCompleted = { it.isCompleted },
            arriveByMinutes = { it.arriveByMinutes },
            arriveByEpochMs = { it.arriveByEpochMs },
            isFixedOrder = { it.isFixedOrder },
        )
        if (sorted.map { it.id } == ordered.map { it.id }) return false
        writeStopPositions(routeId, sorted)
        return true
    }

    /**
     * Copy a route (stops + tasks) with new remote IDs. Delivery progress is reset.
     */
    suspend fun duplicateRoute(routeId: Long, copySuffix: String): Long? {
        val source = dao.getRoute(routeId) ?: return null
        val drafts = source.orderedStops.filter { !it.isOrigin }.map { stop ->
            StopDraft(
                name = stop.name,
                addressHint = stop.addressHint,
                notes = stop.notes,
                latitude = stop.latitude,
                longitude = stop.longitude,
                isCompleted = false,
                libraryStopId = stop.libraryStopId,
                arriveByMinutes = stop.arriveByMinutes,
                serviceMinutes = stop.serviceMinutes,
                geofenceRadiusMeters = stop.geofenceRadiusMeters,
                arriveByEpochMs = stop.arriveByEpochMs,
                isFixedOrder = stop.isFixedOrder,
                isBreak = stop.isBreak,
                phone = stop.phone,
                doorCode = stop.doorCode,
            )
        }
        val copiedName = source.route.name.trim().ifEmpty { "Route" }
        val newId = createRoute(
            "$copiedName$copySuffix",
            source.route.notes,
            drafts,
            roundTrip = source.route.roundTrip,
            geofenceMode = source.route.geofenceMode,
            geofenceRadiusMeters = source.route.geofenceRadiusMeters,
            colorHex = source.route.colorHex,
            vanName = source.route.vanName,
            shiftName = source.route.shiftName,
            originLatitude = source.route.originLatitude,
            originLongitude = source.route.originLongitude,
        )
        val newStops = dao.getStopsForRoute(newId)
        source.orderedStops.filter { !it.isOrigin }.zip(newStops).forEach { (oldStop, newStop) ->
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

    suspend fun duplicateStopToRoute(sourceStopId: Long, targetRouteId: Long): DuplicateStopResult {
        val source = dao.getStop(sourceStopId) ?: return DuplicateStopResult.Failed
        if (source.routeId == targetRouteId) return DuplicateStopResult.SameRoute
        val libId = source.libraryStopId
        if (libId != null) {
            val lib = libraryDao.getById(libId)
            if (lib != null && lib.deletedAtEpochMs == null) {
                when (val result = addStopFromLibrary(targetRouteId, libId)) {
                    is AddFromLibraryResult.Added -> return DuplicateStopResult.Copied(result.stopId)
                    is AddFromLibraryResult.AlreadyOnRoute ->
                        return DuplicateStopResult.AlreadyOnRoute(result.stopId)
                    AddFromLibraryResult.LibraryMissing -> Unit
                }
            }
        }
        val stopId = addStop(
            targetRouteId,
            StopDraft(
                name = source.name,
                addressHint = source.addressHint,
                notes = source.notes,
                latitude = source.latitude,
                longitude = source.longitude,
                libraryStopId = source.libraryStopId,
                arriveByMinutes = source.arriveByMinutes,
                serviceMinutes = source.serviceMinutes,
                geofenceRadiusMeters = source.geofenceRadiusMeters,
                arriveByEpochMs = source.arriveByEpochMs,
                isFixedOrder = source.isFixedOrder,
                isBreak = source.isBreak,
                phone = source.phone,
                doorCode = source.doorCode,
                codAmount = source.codAmount,
                barcode = source.barcode,
            ),
        )
        taskDao.getForStop(source.id).forEach { task ->
            taskDao.insert(
                StopTaskEntity(
                    remoteId = newRemoteId(),
                    stopId = stopId,
                    title = task.title,
                    isRequired = task.isRequired,
                ),
            )
        }
        return DuplicateStopResult.Copied(stopId)
    }

    private suspend fun writeStopPositions(routeId: Long, ordered: List<StopEntity>) {
        val origins = ordered.filter { it.isOrigin }
        val rest = ordered.filter { !it.isOrigin }
        (origins + rest).forEachIndexed { position, stop ->
            if (stop.position != position) {
                dao.updateStop(stop.copy(position = position))
            }
        }
        dao.touchRoute(routeId)
    }

    private suspend fun replaceStops(routeId: Long, stops: List<StopDraft>) {
        val drafts = stops.filter { !it.isOrigin }
        val current = dao.getStopsForRoute(routeId).filter { !it.isOrigin }
        dao.getOriginStops(routeId).forEach { dao.deleteStop(it.id) }
        val usedIds = mutableSetOf<Long>()
        val remaining = current.toMutableList()
        val nextRows = drafts.mapIndexed { index, draft ->
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
                    arriveByMinutes = draft.arriveByMinutes,
                    serviceMinutes = draft.serviceMinutes.coerceAtLeast(0),
                    geofenceRadiusMeters = draft.geofenceRadiusMeters?.let(StopGeofence::clampRadius),
                    isVisited = matched.isVisited,
                    visitedAtEpochMs = matched.visitedAtEpochMs,
                    isOrigin = matched.isOrigin || draft.isOrigin,
                    failureReason = matched.failureReason,
                    arriveByEpochMs = draft.arriveByEpochMs,
                    isFixedOrder = draft.isFixedOrder,
                    isBreak = draft.isBreak,
                    phone = draft.phone.trim(),
                    doorCode = draft.doorCode.trim(),
                    codAmount = draft.codAmount.coerceAtLeast(0.0),
                    barcode = draft.barcode.trim(),
                    podPhotoPath = matched.podPhotoPath,
                    podSignaturePath = matched.podSignaturePath,
                    podCapturedAtEpochMs = matched.podCapturedAtEpochMs,
                    codCollected = matched.codCollected,
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
                    arriveByMinutes = draft.arriveByMinutes,
                    serviceMinutes = draft.serviceMinutes.coerceAtLeast(0),
                    geofenceRadiusMeters = draft.geofenceRadiusMeters?.let(StopGeofence::clampRadius),
                    isOrigin = draft.isOrigin,
                    arriveByEpochMs = draft.arriveByEpochMs,
                    isFixedOrder = draft.isFixedOrder,
                    isBreak = draft.isBreak,
                    phone = draft.phone.trim(),
                    doorCode = draft.doorCode.trim(),
                    codAmount = draft.codAmount.coerceAtLeast(0.0),
                    barcode = draft.barcode.trim(),
                )
            }
        }
        current.filter { it.id !in usedIds }.forEach { dao.deleteStop(it.id) }
        nextRows.forEach { stop ->
            if (stop.id == 0L) {
                val id = dao.insertStop(stop)
                copyLibraryDefaultTasks(id, stop.libraryStopId)
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
        if (draft.skipLibrary || draft.isOrigin) return null
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

    suspend fun previewBackupJson(json: String): BackupPreview = backupManager.previewJson(json)

    suspend fun importBackupJson(
        json: String,
        options: BackupImportOptions = BackupImportOptions(),
    ): BackupImportResult = backupManager.importJson(json, options)

    suspend fun exportTripHistoryCsv(): String = TripHistoryCsv.export(tripHistoryDao.getAll())

    suspend fun exportLibraryCsv(): String = CsvStopExporter.libraryCsv(libraryDao.getAll())

    suspend fun importStopsCsv(
        csvText: String,
        routeName: String,
        roundTrip: Boolean = false,
    ): CsvImportResult {
        val parsed = CsvStopImporter.parse(csvText)
        if (parsed.stops.isEmpty()) {
            return CsvImportResult(routeId = 0, routeName = routeName, imported = 0, skipped = parsed.skipped)
        }
        val drafts = parsed.stops.map { stop ->
            val libId = upsertLibraryStop(
                name = stop.name,
                addressHint = stop.addressHint,
                notes = stop.notes,
                latitude = stop.latitude,
                longitude = stop.longitude,
                extras = LibraryPlaceExtras(tags = stop.tags).takeIf { stop.tags.isNotBlank() },
            )
            StopDraft(
                name = stop.name,
                addressHint = stop.addressHint,
                notes = stop.notes,
                latitude = stop.latitude,
                longitude = stop.longitude,
                libraryStopId = libId,
            )
        }
        val name = routeName.trim().ifBlank { parsed.stops.first().name }
        val routeId = createRoute(name, "", drafts, roundTrip = roundTrip)
        return CsvImportResult(
            routeId = routeId,
            routeName = name,
            imported = drafts.size,
            skipped = parsed.skipped,
        )
    }

    suspend fun importStopsCsvIntoRoute(routeId: Long, csvText: String): CsvImportResult {
        val route = dao.getRoute(routeId)?.route
            ?: return CsvImportResult(routeId = 0, routeName = "", imported = 0, skipped = 0)
        val parsed = CsvStopImporter.parse(csvText)
        if (parsed.stops.isEmpty()) {
            return CsvImportResult(
                routeId = routeId,
                routeName = route.name,
                imported = 0,
                skipped = parsed.skipped,
            )
        }
        parsed.stops.forEach { stop ->
            val libId = upsertLibraryStop(
                name = stop.name,
                addressHint = stop.addressHint,
                notes = stop.notes,
                latitude = stop.latitude,
                longitude = stop.longitude,
                extras = LibraryPlaceExtras(tags = stop.tags).takeIf { stop.tags.isNotBlank() },
            )
            addStop(
                routeId,
                StopDraft(
                    name = stop.name,
                    addressHint = stop.addressHint,
                    notes = stop.notes,
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                    libraryStopId = libId,
                ),
            )
        }
        return CsvImportResult(
            routeId = routeId,
            routeName = route.name,
            imported = parsed.stops.size,
            skipped = parsed.skipped,
        )
    }

    suspend fun exportRouteCsv(routeId: Long): String? {
        val route = dao.getRoute(routeId) ?: return null
        return PlaceFormatExporter.routeCsv(route)
    }

    suspend fun exportLibraryGpx(): String = PlaceFormatExporter.libraryGpx(libraryDao.getAll())

    suspend fun exportLibraryKml(): String = PlaceFormatExporter.libraryKml(libraryDao.getAll())

    suspend fun exportLibraryGeoJson(): String = PlaceFormatExporter.libraryGeoJson(libraryDao.getAll())

    suspend fun exportRouteGpx(routeId: Long): String? =
        dao.getRoute(routeId)?.let(PlaceFormatExporter::routeGpx)

    suspend fun exportRouteKml(routeId: Long): String? =
        dao.getRoute(routeId)?.let(PlaceFormatExporter::routeKml)

    suspend fun exportRouteGeoJson(routeId: Long): String? =
        dao.getRoute(routeId)?.let(PlaceFormatExporter::routeGeoJson)

    suspend fun startRouteTrip(routeId: Long, title: String, stopsTotal: Int): Long {
        cancelOtherInProgress(keepRouteId = routeId)
        val existing = tripHistoryDao.inProgressForRoute(routeId)
        if (existing != null) {
            tripHistoryDao.update(
                existing.copy(
                    title = title,
                    stopsTotal = stopsTotal.coerceAtLeast(1),
                ),
            )
            return existing.id
        }
        return tripHistoryDao.insert(
            TripHistoryEntity(
                kind = TripKind.ROUTE,
                status = TripStatus.IN_PROGRESS,
                title = title,
                routeId = routeId,
                destName = title,
                stopsCompleted = 0,
                stopsTotal = stopsTotal.coerceAtLeast(1),
            ),
        )
    }

    suspend fun startQuickTrip(
        name: String,
        latitude: Double,
        longitude: Double,
        libraryStopId: Long? = null,
    ): Long {
        cancelOtherInProgress()
        return tripHistoryDao.insert(
            TripHistoryEntity(
                kind = TripKind.QUICK,
                status = TripStatus.IN_PROGRESS,
                title = name,
                libraryStopId = libraryStopId,
                destLatitude = latitude,
                destLongitude = longitude,
                destName = name,
                stopsCompleted = 0,
                stopsTotal = 1,
            ),
        )
    }

    private suspend fun cancelOtherInProgress(keepRouteId: Long? = null) {
        tripHistoryDao.inProgressAll().forEach { trip ->
            if (keepRouteId != null && trip.routeId == keepRouteId) return@forEach
            finishTrip(trip.id, TripStatus.CANCELLED, trip.stopsCompleted, trip.stopsTotal)
        }
    }

    suspend fun updateTripProgress(tripId: Long, stopsCompleted: Int, stopsTotal: Int) {
        val trip = tripHistoryDao.getById(tripId) ?: return
        tripHistoryDao.update(
            trip.copy(
                stopsCompleted = stopsCompleted.coerceAtLeast(0),
                stopsTotal = stopsTotal.coerceAtLeast(1),
            ),
        )
    }

    suspend fun finishTrip(
        tripId: Long,
        status: String,
        stopsCompleted: Int,
        stopsTotal: Int,
        distanceMeters: Double = 0.0,
        lateStops: Int = 0,
    ) {
        val trip = tripHistoryDao.getById(tripId) ?: return
        if (trip.status != TripStatus.IN_PROGRESS) return
        tripHistoryDao.update(
            trip.copy(
                status = status,
                endedAtEpochMs = System.currentTimeMillis(),
                stopsCompleted = stopsCompleted.coerceAtLeast(0),
                stopsTotal = stopsTotal.coerceAtLeast(1),
                distanceMeters = distanceMeters.coerceAtLeast(0.0),
                lateStops = lateStops.coerceAtLeast(0),
            ),
        )
    }

    suspend fun deleteTrip(tripId: Long) {
        tripHistoryDao.delete(tripId)
    }

    suspend fun deleteTripsOlderThan(olderThanEpochMs: Long): Int =
        tripHistoryDao.deleteOlderThan(olderThanEpochMs)

    private companion object {
        const val MAX_ROAD_OPTIMIZE_STOPS = 20

        fun minutesFromMidnight(): Int {
            val cal = Calendar.getInstance()
            return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        }
    }
}

private data class PlaceFields(
    val name: String,
    val addressHint: String,
    val notes: String,
    val latitude: Double?,
    val longitude: Double?,
)
