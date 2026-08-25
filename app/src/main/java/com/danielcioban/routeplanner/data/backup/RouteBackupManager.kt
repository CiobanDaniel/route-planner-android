package com.danielcioban.routeplanner.data.backup

import com.danielcioban.routeplanner.data.local.FuelLogDao
import com.danielcioban.routeplanner.data.local.FuelLogEntity
import com.danielcioban.routeplanner.data.local.LibraryDefaultTaskDao
import com.danielcioban.routeplanner.data.local.LibraryDefaultTaskEntity
import com.danielcioban.routeplanner.data.local.RouteDao
import com.danielcioban.routeplanner.data.local.RouteEntity
import com.danielcioban.routeplanner.data.local.SavedSearchDao
import com.danielcioban.routeplanner.data.local.SavedSearchEntity
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.local.StopLibraryDao
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import com.danielcioban.routeplanner.data.local.StopTaskDao
import com.danielcioban.routeplanner.data.local.StopTaskEntity
import com.danielcioban.routeplanner.data.local.TaskTemplateDao
import com.danielcioban.routeplanner.data.local.TaskTemplateEntity
import com.danielcioban.routeplanner.data.local.TripHistoryDao
import com.danielcioban.routeplanner.data.local.TripHistoryEntity
import com.danielcioban.routeplanner.data.local.TripStatus
import com.danielcioban.routeplanner.data.settings.RouteGeofenceMode
import org.json.JSONArray
import org.json.JSONObject

data class BackupImportResult(
    val routesAdded: Int = 0,
    val routesUpdated: Int = 0,
    val libraryAdded: Int = 0,
    val libraryUpdated: Int = 0,
    val stopsAdded: Int = 0,
    val stopsUpdated: Int = 0,
    val tasksAdded: Int = 0,
    val tasksUpdated: Int = 0,
    val tripsAdded: Int = 0,
    val tripsUpdated: Int = 0,
    val templatesAdded: Int = 0,
    val templatesUpdated: Int = 0,
    val libraryTasksAdded: Int = 0,
    val libraryTasksUpdated: Int = 0,
    val savedSearchesAdded: Int = 0,
    val savedSearchesUpdated: Int = 0,
    val fuelLogsAdded: Int = 0,
    val fuelLogsUpdated: Int = 0,
    val routesSkipped: Int = 0,
    val librarySkipped: Int = 0,
)

/** JSON snapshot export/import — rehearsal for future cloud sync payloads. */
class RouteBackupManager(
    private val routeDao: RouteDao,
    private val libraryDao: StopLibraryDao,
    private val taskDao: StopTaskDao,
    private val tripHistoryDao: TripHistoryDao,
    private val templateDao: TaskTemplateDao,
    private val libraryDefaultTaskDao: LibraryDefaultTaskDao,
    private val savedSearchDao: SavedSearchDao,
    private val fuelLogDao: FuelLogDao,
) {
    suspend fun exportJson(): String {
        val routes = routeDao.getAllRoutes()
        val library = libraryDao.getAll()
        val tasks = taskDao.getAll()
        val trips = tripHistoryDao.getAll()
        val templates = templateDao.getAll()
        val libraryTasks = libraryDefaultTaskDao.getAll()
        val searches = savedSearchDao.getAll()
        val fuelLogs = fuelLogDao.getAll()
        val libraryById = library.associateBy { it.id }
        val routesById = routes.associateBy { it.id }
        val stops = routes.flatMap { route ->
            routeDao.getStopsForRoute(route.id).map { stop -> route to stop }
        }

        return JSONObject().apply {
            put(FORMAT_VERSION, CURRENT_FORMAT_VERSION)
            put(EXPORTED_AT, System.currentTimeMillis())
            put(JSON_ROUTES, JSONArray(routes.map { it.toJson() }))
            put(JSON_STOPS, JSONArray(stops.map { (route, stop) ->
                val libraryRemoteId = stop.libraryStopId?.let { libraryById[it]?.remoteId }
                stop.toJson(routeRemoteId = route.remoteId, libraryRemoteId = libraryRemoteId)
            }))
            put(JSON_LIBRARY, JSONArray(library.map { it.toJson() }))
            put(JSON_TASKS, JSONArray(tasks.map { task ->
                val stop = routeDao.getStop(task.stopId)!!
                task.toJson(stopRemoteId = stop.remoteId)
            }))
            put(JSON_TRIPS, JSONArray(trips.map { trip ->
                trip.toJson(
                    routeRemoteId = trip.routeId?.let { routesById[it]?.remoteId },
                    libraryRemoteId = trip.libraryStopId?.let { libraryById[it]?.remoteId },
                )
            }))
            put(JSON_TEMPLATES, JSONArray(templates.map { it.toJson() }))
            put(
                JSON_LIBRARY_TASKS,
                JSONArray(
                    libraryTasks.mapNotNull { task ->
                        val libraryRemoteId = libraryById[task.libraryStopId]?.remoteId ?: return@mapNotNull null
                        task.toJson(libraryRemoteId)
                    },
                ),
            )
            put(JSON_SAVED_SEARCHES, JSONArray(searches.map { it.toJson() }))
            put(JSON_FUEL_LOGS, JSONArray(fuelLogs.map { it.toJson() }))
        }.toString(2)
    }

    suspend fun previewJson(raw: String): BackupPreview {
        val root = JSONObject(raw.trim())
        val version = root.optInt(FORMAT_VERSION)
        require(version in MIN_FORMAT_VERSION..CURRENT_FORMAT_VERSION) {
            "Unsupported backup format"
        }
        var routeAdds = 0
        var routeOverwrites = 0
        var routeLocalNewer = 0
        var libraryAdds = 0
        var libraryOverwrites = 0
        var libraryLocalNewer = 0
        root.optJSONArray(JSON_LIBRARY)?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val remoteId = item.getString(KEY_REMOTE_ID)
                val incoming = item.toLibraryEntity(remoteId)
                val existing = libraryDao.getByRemoteId(remoteId)
                when {
                    existing == null -> libraryAdds++
                    incoming.updatedAtEpochMs >= existing.updatedAtEpochMs -> libraryOverwrites++
                    else -> libraryLocalNewer++
                }
            }
        }
        root.optJSONArray(JSON_ROUTES)?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val remoteId = item.getString(KEY_REMOTE_ID)
                val incoming = item.toRouteEntity(remoteId)
                val existing = routeDao.getRouteByRemoteId(remoteId)
                when {
                    existing == null -> routeAdds++
                    incoming.updatedAtEpochMs >= existing.updatedAtEpochMs -> routeOverwrites++
                    else -> routeLocalNewer++
                }
            }
        }
        return BackupPreview(
            routeAdds = routeAdds,
            routeOverwrites = routeOverwrites,
            routeLocalNewer = routeLocalNewer,
            libraryAdds = libraryAdds,
            libraryOverwrites = libraryOverwrites,
            libraryLocalNewer = libraryLocalNewer,
            tripCount = root.optJSONArray(JSON_TRIPS)?.length() ?: 0,
        )
    }

    suspend fun importJson(
        raw: String,
        options: BackupImportOptions = BackupImportOptions(),
    ): BackupImportResult {
        val root = JSONObject(raw.trim())
        val version = root.optInt(FORMAT_VERSION)
        require(version in MIN_FORMAT_VERSION..CURRENT_FORMAT_VERSION) {
            "Unsupported backup format"
        }

        var routesAdded = 0
        var routesUpdated = 0
        var routesSkipped = 0
        var libraryAdded = 0
        var libraryUpdated = 0
        var librarySkipped = 0
        var stopsAdded = 0
        var stopsUpdated = 0
        var tasksAdded = 0
        var tasksUpdated = 0
        var tripsAdded = 0
        var tripsUpdated = 0
        var templatesAdded = 0
        var templatesUpdated = 0
        var libraryTasksAdded = 0
        var libraryTasksUpdated = 0
        var savedSearchesAdded = 0
        var savedSearchesUpdated = 0
        var fuelLogsAdded = 0
        var fuelLogsUpdated = 0

        val libraryRemoteToLocal = mutableMapOf<String, Long>()
        if (options.includeLibrary) {
            root.optJSONArray(JSON_LIBRARY)?.let { arr ->
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val remoteId = item.getString(KEY_REMOTE_ID)
                    val incoming = item.toLibraryEntity(remoteId)
                    val existing = libraryDao.getByRemoteId(remoteId)
                    if (existing == null) {
                        val id = libraryDao.upsert(incoming)
                        libraryRemoteToLocal[remoteId] = if (id > 0) id else incoming.id
                        libraryAdded++
                    } else if (
                        !options.keepLocalOnConflict &&
                        incoming.updatedAtEpochMs >= existing.updatedAtEpochMs
                    ) {
                        libraryDao.update(incoming.copy(id = existing.id))
                        libraryRemoteToLocal[remoteId] = existing.id
                        libraryUpdated++
                    } else {
                        libraryRemoteToLocal[remoteId] = existing.id
                        librarySkipped++
                    }
                }
            }
        }

        val routeRemoteToLocal = mutableMapOf<String, Long>()
        if (options.includeRoutes) {
            root.optJSONArray(JSON_ROUTES)?.let { arr ->
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val remoteId = item.getString(KEY_REMOTE_ID)
                    val incoming = item.toRouteEntity(remoteId)
                    val existing = routeDao.getRouteByRemoteId(remoteId)
                    if (existing == null) {
                        val id = routeDao.insertRoute(incoming)
                        routeRemoteToLocal[remoteId] = id
                        routesAdded++
                    } else if (
                        !options.keepLocalOnConflict &&
                        incoming.updatedAtEpochMs >= existing.updatedAtEpochMs
                    ) {
                        routeDao.updateRoute(incoming.copy(id = existing.id))
                        routeRemoteToLocal[remoteId] = existing.id
                        routesUpdated++
                    } else {
                        routeRemoteToLocal[remoteId] = existing.id
                        routesSkipped++
                    }
                }
            }

            val stopRemoteToLocal = mutableMapOf<String, Long>()
            root.optJSONArray(JSON_STOPS)?.let { arr ->
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val remoteId = item.getString(KEY_REMOTE_ID)
                    val routeRemoteId = item.getString(KEY_ROUTE_REMOTE_ID)
                    val routeId = routeRemoteToLocal[routeRemoteId]
                        ?: routeDao.getRouteByRemoteId(routeRemoteId)?.id
                        ?: continue
                    val libraryRemoteId = item.optString(KEY_LIBRARY_REMOTE_ID).ifBlank { null }
                    val libraryStopId = libraryRemoteId?.let {
                        libraryRemoteToLocal[it] ?: libraryDao.getByRemoteId(it)?.id
                    }
                    val incoming = item.toStopEntity(remoteId, routeId, libraryStopId)
                    val existing = routeDao.getStopByRemoteId(remoteId)
                    if (existing == null) {
                        val id = routeDao.insertStop(incoming)
                        stopRemoteToLocal[remoteId] = id
                        stopsAdded++
                    } else if (options.keepLocalOnConflict) {
                        stopRemoteToLocal[remoteId] = existing.id
                    } else {
                        routeDao.updateStop(incoming.copy(id = existing.id))
                        stopRemoteToLocal[remoteId] = existing.id
                        stopsUpdated++
                    }
                }
            }

            root.optJSONArray(JSON_TASKS)?.let { arr ->
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val remoteId = item.getString(KEY_REMOTE_ID)
                    val stopRemoteId = item.getString(KEY_STOP_REMOTE_ID)
                    val stopId = stopRemoteToLocal[stopRemoteId]
                        ?: routeDao.getStopByRemoteId(stopRemoteId)?.id
                        ?: continue
                    val incoming = item.toTaskEntity(remoteId, stopId)
                    val existing = taskDao.getByRemoteId(remoteId)
                    if (existing == null) {
                        taskDao.insert(incoming)
                        tasksAdded++
                    } else if (!options.keepLocalOnConflict) {
                        taskDao.update(incoming.copy(id = existing.id))
                        tasksUpdated++
                    }
                }
            }
        }

        if (options.includeHistory) {
            root.optJSONArray(JSON_TRIPS)?.let { arr ->
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val remoteId = item.getString(KEY_REMOTE_ID)
                    val existing = tripHistoryDao.getByRemoteId(remoteId)
                    if (existing != null && existing.status == TripStatus.IN_PROGRESS) {
                        continue
                    }
                    val routeRemoteId = item.optString(KEY_ROUTE_REMOTE_ID).ifBlank { null }
                    val routeId = routeRemoteId?.let {
                        routeRemoteToLocal[it] ?: routeDao.getRouteByRemoteId(it)?.id
                    }
                    val libraryRemoteId = item.optString(KEY_LIBRARY_REMOTE_ID).ifBlank { null }
                    val libraryStopId = libraryRemoteId?.let {
                        libraryRemoteToLocal[it] ?: libraryDao.getByRemoteId(it)?.id
                    }
                    val incoming = item.toTripEntity(remoteId, routeId, libraryStopId)
                    if (existing == null) {
                        tripHistoryDao.insert(incoming)
                        tripsAdded++
                    } else if (!options.keepLocalOnConflict) {
                        tripHistoryDao.update(incoming.copy(id = existing.id))
                        tripsUpdated++
                    }
                }
            }
        }

        if (options.includeTemplates) {
            root.optJSONArray(JSON_TEMPLATES)?.let { arr ->
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val remoteId = item.getString(KEY_REMOTE_ID)
                    val incoming = item.toTemplateEntity(remoteId)
                    val existing = templateDao.getByRemoteId(remoteId)
                    if (existing == null) {
                        templateDao.insert(incoming)
                        templatesAdded++
                    } else if (!options.keepLocalOnConflict) {
                        templateDao.update(incoming.copy(id = existing.id))
                        templatesUpdated++
                    }
                }
            }

            root.optJSONArray(JSON_LIBRARY_TASKS)?.let { arr ->
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val remoteId = item.getString(KEY_REMOTE_ID)
                    val libraryRemoteId = item.getString(KEY_LIBRARY_REMOTE_ID)
                    val libraryStopId = libraryRemoteToLocal[libraryRemoteId]
                        ?: libraryDao.getByRemoteId(libraryRemoteId)?.id
                        ?: continue
                    val incoming = item.toLibraryDefaultTask(remoteId, libraryStopId)
                    val existing = libraryDefaultTaskDao.getByRemoteId(remoteId)
                    if (existing == null) {
                        libraryDefaultTaskDao.insert(incoming)
                        libraryTasksAdded++
                    } else if (!options.keepLocalOnConflict) {
                        libraryDefaultTaskDao.update(incoming.copy(id = existing.id))
                        libraryTasksUpdated++
                    }
                }
            }

            root.optJSONArray(JSON_SAVED_SEARCHES)?.let { arr ->
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val remoteId = item.getString(KEY_REMOTE_ID)
                    val incoming = item.toSavedSearch(remoteId)
                    val existing = savedSearchDao.getByRemoteId(remoteId)
                    if (existing == null) {
                        savedSearchDao.upsert(incoming)
                        savedSearchesAdded++
                    } else if (!options.keepLocalOnConflict) {
                        savedSearchDao.upsert(incoming.copy(id = existing.id))
                        savedSearchesUpdated++
                    }
                }
            }
        }

        if (options.includeFuel) {
            root.optJSONArray(JSON_FUEL_LOGS)?.let { arr ->
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val remoteId = item.getString(KEY_REMOTE_ID)
                    val incoming = item.toFuelLog(remoteId)
                    val existing = fuelLogDao.getByRemoteId(remoteId)
                    if (existing == null) {
                        fuelLogDao.insert(incoming)
                        fuelLogsAdded++
                    } else if (!options.keepLocalOnConflict) {
                        fuelLogDao.update(incoming.copy(id = existing.id))
                        fuelLogsUpdated++
                    }
                }
            }
        }

        if (options.includeRoutes) {
            migrateImportedOriginStops(routeRemoteToLocal.values.toSet())
        }

        return BackupImportResult(
            routesAdded = routesAdded,
            routesUpdated = routesUpdated,
            libraryAdded = libraryAdded,
            libraryUpdated = libraryUpdated,
            stopsAdded = stopsAdded,
            stopsUpdated = stopsUpdated,
            tasksAdded = tasksAdded,
            tasksUpdated = tasksUpdated,
            tripsAdded = tripsAdded,
            tripsUpdated = tripsUpdated,
            templatesAdded = templatesAdded,
            templatesUpdated = templatesUpdated,
            libraryTasksAdded = libraryTasksAdded,
            libraryTasksUpdated = libraryTasksUpdated,
            savedSearchesAdded = savedSearchesAdded,
            savedSearchesUpdated = savedSearchesUpdated,
            fuelLogsAdded = fuelLogsAdded,
            fuelLogsUpdated = fuelLogsUpdated,
            routesSkipped = routesSkipped,
            librarySkipped = librarySkipped,
        )
    }

    private suspend fun migrateImportedOriginStops(routeIds: Set<Long>) {
        routeIds.forEach { routeId ->
            val origins = routeDao.getOriginStops(routeId)
            if (origins.isEmpty()) return@forEach
            val route = routeDao.getRoute(routeId)?.route ?: return@forEach
            if (route.originLatitude == null) {
                val first = origins.firstOrNull { it.latitude != null && it.longitude != null }
                if (first != null) {
                    routeDao.updateRoute(
                        route.copy(
                            originLatitude = first.latitude,
                            originLongitude = first.longitude,
                        ),
                    )
                }
            }
            origins.forEach { routeDao.deleteStop(it.id) }
        }
    }

    companion object {
        const val CURRENT_FORMAT_VERSION = 6
        const val MIN_FORMAT_VERSION = 1
        private const val FORMAT_VERSION = "formatVersion"
        private const val EXPORTED_AT = "exportedAtEpochMs"
        private const val JSON_ROUTES = "routes"
        private const val JSON_STOPS = "stops"
        private const val JSON_LIBRARY = "library"
        private const val JSON_TASKS = "tasks"
        private const val JSON_TRIPS = "trips"
        private const val JSON_TEMPLATES = "taskTemplates"
        private const val JSON_LIBRARY_TASKS = "libraryDefaultTasks"
        private const val JSON_SAVED_SEARCHES = "savedSearches"
        private const val JSON_FUEL_LOGS = "fuelLogs"
        private const val KEY_REMOTE_ID = "remoteId"
        private const val KEY_ROUTE_REMOTE_ID = "routeRemoteId"
        private const val KEY_STOP_REMOTE_ID = "stopRemoteId"
        private const val KEY_LIBRARY_REMOTE_ID = "libraryRemoteId"
    }
}

private fun RouteEntity.toJson(): JSONObject = JSONObject().apply {
    put("remoteId", remoteId)
    put("name", name)
    put("notes", notes)
    put("createdAtEpochMs", createdAtEpochMs)
    put("updatedAtEpochMs", updatedAtEpochMs)
    put("roundTrip", roundTrip)
    put("geofenceMode", geofenceMode)
    geofenceRadiusMeters?.let { put("geofenceRadiusMeters", it) }
    deletedAtEpochMs?.let { put("deletedAtEpochMs", it) }
    originLatitude?.let { put("originLatitude", it) }
    originLongitude?.let { put("originLongitude", it) }
    put("colorHex", colorHex)
    put("vanName", vanName)
    put("shiftName", shiftName)
    put("archived", archived)
    put("shiftSlot", shiftSlot)
}

private fun StopEntity.toJson(routeRemoteId: String, libraryRemoteId: String?): JSONObject = JSONObject().apply {
    put("remoteId", remoteId)
    put("routeRemoteId", routeRemoteId)
    put("position", position)
    put("name", name)
    put("addressHint", addressHint)
    put("notes", notes)
    latitude?.let { put("latitude", it) }
    longitude?.let { put("longitude", it) }
    put("isCompleted", isCompleted)
    libraryRemoteId?.let { put("libraryRemoteId", it) }
    arriveByMinutes?.let { put("arriveByMinutes", it) }
    put("serviceMinutes", serviceMinutes)
    geofenceRadiusMeters?.let { put("geofenceRadiusMeters", it) }
    put("isVisited", isVisited)
    visitedAtEpochMs?.let { put("visitedAtEpochMs", it) }
    deletedAtEpochMs?.let { put("deletedAtEpochMs", it) }
    put("isOrigin", isOrigin)
    failureReason?.let { put("failureReason", it) }
    arriveByEpochMs?.let { put("arriveByEpochMs", it) }
    put("isFixedOrder", isFixedOrder)
    put("isBreak", isBreak)
    put("phone", phone)
    put("doorCode", doorCode)
    failurePhotoPath?.let { put("failurePhotoPath", it) }
    failureSignaturePath?.let { put("failureSignaturePath", it) }
    put("tasksDeferred", tasksDeferred)
    put("tasksDeferredNote", tasksDeferredNote)
    podPhotoPath?.let { put("podPhotoPath", it) }
    podSignaturePath?.let { put("podSignaturePath", it) }
    podCapturedAtEpochMs?.let { put("podCapturedAtEpochMs", it) }
    put("codAmount", codAmount)
    put("codCollected", codCollected)
    put("barcode", barcode)
}

private fun StopLibraryEntity.toJson(): JSONObject = JSONObject().apply {
    put("remoteId", remoteId)
    put("name", name)
    put("addressHint", addressHint)
    put("notes", notes)
    put("latitude", latitude)
    put("longitude", longitude)
    put("createdAtEpochMs", createdAtEpochMs)
    put("updatedAtEpochMs", updatedAtEpochMs)
    deletedAtEpochMs?.let { put("deletedAtEpochMs", it) }
    put("tags", tags)
    put("plusCode", plusCode)
    put("what3words", what3words)
    put("lastUsedAtEpochMs", lastUsedAtEpochMs)
    put("useCount", useCount)
    defaultGeofenceRadiusMeters?.let { put("defaultGeofenceRadiusMeters", it) }
    put("isFavorite", isFavorite)
}

private fun StopTaskEntity.toJson(stopRemoteId: String): JSONObject = JSONObject().apply {
    put("remoteId", remoteId)
    put("stopRemoteId", stopRemoteId)
    put("title", title)
    put("isRequired", isRequired)
    put("isCompleted", isCompleted)
    completedAtEpochMs?.let { put("completedAtEpochMs", it) }
    put("completionNote", completionNote)
    deletedAtEpochMs?.let { put("deletedAtEpochMs", it) }
}

private fun JSONObject.toRouteEntity(remoteId: String) = RouteEntity(
    remoteId = remoteId,
    name = getString("name"),
    notes = optString("notes", ""),
    createdAtEpochMs = optLong("createdAtEpochMs", System.currentTimeMillis()),
    updatedAtEpochMs = optLong("updatedAtEpochMs", System.currentTimeMillis()),
    deletedAtEpochMs = optNullableLong("deletedAtEpochMs"),
    roundTrip = optBoolean("roundTrip", false),
    geofenceMode = RouteGeofenceMode.fromStored(optString("geofenceMode", "INHERIT")).name,
    geofenceRadiusMeters = optNullableInt("geofenceRadiusMeters")?.let(
        com.danielcioban.routeplanner.data.settings.StopGeofence::clampRadius,
    ),
    originLatitude = optDouble("originLatitude").takeIf { has("originLatitude") && !isNull("originLatitude") },
    originLongitude = optDouble("originLongitude").takeIf { has("originLongitude") && !isNull("originLongitude") },
    colorHex = optString("colorHex", ""),
    vanName = optString("vanName", ""),
    shiftName = optString("shiftName", ""),
    archived = optBoolean("archived", false),
    shiftSlot = com.danielcioban.routeplanner.data.local.ShiftSlot.fromStored(optString("shiftSlot", "")),
)

private fun JSONObject.toStopEntity(
    remoteId: String,
    routeId: Long,
    libraryStopId: Long?,
) = StopEntity(
    remoteId = remoteId,
    routeId = routeId,
    position = getInt("position"),
    name = getString("name"),
    addressHint = optString("addressHint", ""),
    notes = optString("notes", ""),
    latitude = optDouble("latitude").takeIf { has("latitude") && !isNull("latitude") },
    longitude = optDouble("longitude").takeIf { has("longitude") && !isNull("longitude") },
    isCompleted = optBoolean("isCompleted", false),
    libraryStopId = libraryStopId,
    deletedAtEpochMs = optNullableLong("deletedAtEpochMs"),
    arriveByMinutes = optNullableInt("arriveByMinutes")?.coerceIn(0, 23 * 60 + 59),
    serviceMinutes = optInt("serviceMinutes", 0).coerceAtLeast(0),
    geofenceRadiusMeters = optNullableInt("geofenceRadiusMeters")?.let(
        com.danielcioban.routeplanner.data.settings.StopGeofence::clampRadius,
    ),
    isVisited = optBoolean("isVisited", false) || optBoolean("isCompleted", false),
    visitedAtEpochMs = optNullableLong("visitedAtEpochMs"),
    isOrigin = optBoolean("isOrigin", false),
    failureReason = optString("failureReason").takeIf { has("failureReason") && !isNull("failureReason") && it.isNotBlank() },
    arriveByEpochMs = optNullableLong("arriveByEpochMs"),
    isFixedOrder = optBoolean("isFixedOrder", false),
    isBreak = optBoolean("isBreak", false),
    phone = optString("phone", ""),
    doorCode = optString("doorCode", ""),
    failurePhotoPath = optString("failurePhotoPath").takeIf { has("failurePhotoPath") && !isNull("failurePhotoPath") && it.isNotBlank() },
    failureSignaturePath = optString("failureSignaturePath").takeIf { has("failureSignaturePath") && !isNull("failureSignaturePath") && it.isNotBlank() },
    tasksDeferred = optBoolean("tasksDeferred", false),
    tasksDeferredNote = optString("tasksDeferredNote", ""),
    podPhotoPath = optString("podPhotoPath").takeIf { has("podPhotoPath") && !isNull("podPhotoPath") && it.isNotBlank() },
    podSignaturePath = optString("podSignaturePath").takeIf { has("podSignaturePath") && !isNull("podSignaturePath") && it.isNotBlank() },
    podCapturedAtEpochMs = optNullableLong("podCapturedAtEpochMs"),
    codAmount = optDouble("codAmount", 0.0).coerceAtLeast(0.0),
    codCollected = optBoolean("codCollected", false),
    barcode = optString("barcode", ""),
)

private fun JSONObject.toLibraryEntity(remoteId: String) = StopLibraryEntity(
    remoteId = remoteId,
    name = getString("name"),
    addressHint = optString("addressHint", ""),
    notes = optString("notes", ""),
    latitude = getDouble("latitude"),
    longitude = getDouble("longitude"),
    createdAtEpochMs = optLong("createdAtEpochMs", System.currentTimeMillis()),
    updatedAtEpochMs = optLong("updatedAtEpochMs", System.currentTimeMillis()),
    deletedAtEpochMs = optNullableLong("deletedAtEpochMs"),
    tags = optString("tags", ""),
    plusCode = optString("plusCode", ""),
    what3words = optString("what3words", ""),
    lastUsedAtEpochMs = optLong("lastUsedAtEpochMs", 0L),
    useCount = optInt("useCount", 0),
    defaultGeofenceRadiusMeters = optNullableInt("defaultGeofenceRadiusMeters"),
    isFavorite = optBoolean("isFavorite", false),
)

private fun JSONObject.toTaskEntity(remoteId: String, stopId: Long) = StopTaskEntity(
    remoteId = remoteId,
    stopId = stopId,
    title = getString("title"),
    isRequired = optBoolean("isRequired", false),
    isCompleted = optBoolean("isCompleted", false),
    completedAtEpochMs = optNullableLong("completedAtEpochMs"),
    completionNote = optString("completionNote", ""),
    deletedAtEpochMs = optNullableLong("deletedAtEpochMs"),
)

private fun TaskTemplateEntity.toJson(): JSONObject = JSONObject().apply {
    put("remoteId", remoteId)
    put("title", title)
    put("isRequired", isRequired)
    put("sortOrder", sortOrder)
}

private fun JSONObject.toTemplateEntity(remoteId: String) = TaskTemplateEntity(
    remoteId = remoteId,
    title = getString("title"),
    isRequired = optBoolean("isRequired", false),
    sortOrder = optInt("sortOrder", 0),
)

private fun LibraryDefaultTaskEntity.toJson(libraryRemoteId: String): JSONObject = JSONObject().apply {
    put("remoteId", remoteId)
    put("libraryRemoteId", libraryRemoteId)
    put("title", title)
    put("isRequired", isRequired)
    put("sortOrder", sortOrder)
}

private fun JSONObject.toLibraryDefaultTask(remoteId: String, libraryStopId: Long) = LibraryDefaultTaskEntity(
    remoteId = remoteId,
    libraryStopId = libraryStopId,
    title = getString("title"),
    isRequired = optBoolean("isRequired", false),
    sortOrder = optInt("sortOrder", 0),
)

private fun SavedSearchEntity.toJson(): JSONObject = JSONObject().apply {
    put("remoteId", remoteId)
    put("query", query)
    put("nearMeOnly", nearMeOnly)
    put("createdAtEpochMs", createdAtEpochMs)
}

private fun JSONObject.toSavedSearch(remoteId: String) = SavedSearchEntity(
    remoteId = remoteId,
    query = getString("query"),
    nearMeOnly = optBoolean("nearMeOnly", false),
    createdAtEpochMs = optLong("createdAtEpochMs", System.currentTimeMillis()),
)

private fun TripHistoryEntity.toJson(routeRemoteId: String?, libraryRemoteId: String?): JSONObject =
    JSONObject().apply {
        put("remoteId", remoteId)
        put("kind", kind)
        put("status", status)
        put("title", title)
        put("startedAtEpochMs", startedAtEpochMs)
        endedAtEpochMs?.let { put("endedAtEpochMs", it) }
        routeRemoteId?.let { put("routeRemoteId", it) }
        libraryRemoteId?.let { put("libraryRemoteId", it) }
        destLatitude?.let { put("destLatitude", it) }
        destLongitude?.let { put("destLongitude", it) }
        put("destName", destName)
        put("stopsCompleted", stopsCompleted)
        put("stopsTotal", stopsTotal)
        put("distanceMeters", distanceMeters)
        put("lateStops", lateStops)
    }

private fun JSONObject.toTripEntity(
    remoteId: String,
    routeId: Long?,
    libraryStopId: Long?,
): TripHistoryEntity {
    val rawStatus = optString("status", TripStatus.COMPLETED)
    val wasInProgress = rawStatus == TripStatus.IN_PROGRESS
    val status = if (wasInProgress) TripStatus.CANCELLED else rawStatus
    val ended = optNullableLong("endedAtEpochMs")
        ?: if (wasInProgress) System.currentTimeMillis() else null
    return TripHistoryEntity(
        remoteId = remoteId,
        kind = optString("kind", "ROUTE"),
        status = status,
        title = optString("title", ""),
        startedAtEpochMs = optLong("startedAtEpochMs", System.currentTimeMillis()),
        endedAtEpochMs = ended,
        routeId = routeId,
        libraryStopId = libraryStopId,
        destLatitude = optDouble("destLatitude").takeIf { has("destLatitude") && !isNull("destLatitude") },
        destLongitude = optDouble("destLongitude").takeIf { has("destLongitude") && !isNull("destLongitude") },
        destName = optString("destName", ""),
        stopsCompleted = optInt("stopsCompleted", 0),
        stopsTotal = optInt("stopsTotal", 1).coerceAtLeast(1),
        distanceMeters = optDouble("distanceMeters", 0.0).coerceAtLeast(0.0),
        lateStops = optInt("lateStops", 0).coerceAtLeast(0),
    )
}

private fun FuelLogEntity.toJson(): JSONObject = JSONObject().apply {
    put("remoteId", remoteId)
    put("loggedAtEpochMs", loggedAtEpochMs)
    odometerKm?.let { put("odometerKm", it) }
    liters?.let { put("liters", it) }
    amount?.let { put("amount", it) }
    put("notes", notes)
}

private fun JSONObject.toFuelLog(remoteId: String) = FuelLogEntity(
    remoteId = remoteId,
    loggedAtEpochMs = optLong("loggedAtEpochMs", System.currentTimeMillis()),
    odometerKm = optDouble("odometerKm").takeIf { has("odometerKm") && !isNull("odometerKm") },
    liters = optDouble("liters").takeIf { has("liters") && !isNull("liters") },
    amount = optDouble("amount").takeIf { has("amount") && !isNull("amount") },
    notes = optString("notes", ""),
)

private fun JSONObject.optNullableLong(key: String): Long? =
    if (has(key) && !isNull(key)) optLong(key) else null

private fun JSONObject.optNullableInt(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null
