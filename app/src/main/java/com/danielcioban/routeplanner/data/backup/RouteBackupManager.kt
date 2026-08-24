package com.danielcioban.routeplanner.data.backup

import com.danielcioban.routeplanner.data.local.RouteDao
import com.danielcioban.routeplanner.data.local.RouteEntity
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.local.StopLibraryDao
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import com.danielcioban.routeplanner.data.local.StopTaskDao
import com.danielcioban.routeplanner.data.local.StopTaskEntity
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
)

/** JSON snapshot export/import — rehearsal for future cloud sync payloads. */
class RouteBackupManager(
    private val routeDao: RouteDao,
    private val libraryDao: StopLibraryDao,
    private val taskDao: StopTaskDao,
) {
    suspend fun exportJson(): String {
        val routes = routeDao.getAllRoutes()
        val library = libraryDao.getAll()
        val tasks = taskDao.getAll()
        val libraryById = library.associateBy { it.id }
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
        }.toString(2)
    }

    suspend fun importJson(raw: String): BackupImportResult {
        val root = JSONObject(raw.trim())
        require(root.optInt(FORMAT_VERSION) == CURRENT_FORMAT_VERSION) {
            "Unsupported backup format"
        }

        var routesAdded = 0
        var routesUpdated = 0
        var libraryAdded = 0
        var libraryUpdated = 0
        var stopsAdded = 0
        var stopsUpdated = 0
        var tasksAdded = 0
        var tasksUpdated = 0

        val libraryRemoteToLocal = mutableMapOf<String, Long>()
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
                } else if (incoming.updatedAtEpochMs >= existing.updatedAtEpochMs) {
                    libraryDao.update(incoming.copy(id = existing.id))
                    libraryRemoteToLocal[remoteId] = existing.id
                    libraryUpdated++
                } else {
                    libraryRemoteToLocal[remoteId] = existing.id
                }
            }
        }

        val routeRemoteToLocal = mutableMapOf<String, Long>()
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
                } else if (incoming.updatedAtEpochMs >= existing.updatedAtEpochMs) {
                    routeDao.updateRoute(incoming.copy(id = existing.id))
                    routeRemoteToLocal[remoteId] = existing.id
                    routesUpdated++
                } else {
                    routeRemoteToLocal[remoteId] = existing.id
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
                val libraryStopId = libraryRemoteId?.let { libraryRemoteToLocal[it] ?: libraryDao.getByRemoteId(it)?.id }
                val incoming = item.toStopEntity(remoteId, routeId, libraryStopId)
                val existing = routeDao.getStopByRemoteId(remoteId)
                if (existing == null) {
                    val id = routeDao.insertStop(incoming)
                    stopRemoteToLocal[remoteId] = id
                    stopsAdded++
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
                } else {
                    taskDao.update(incoming.copy(id = existing.id))
                    tasksUpdated++
                }
            }
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
        )
    }

    companion object {
        const val CURRENT_FORMAT_VERSION = 1
        private const val FORMAT_VERSION = "formatVersion"
        private const val EXPORTED_AT = "exportedAtEpochMs"
        private const val JSON_ROUTES = "routes"
        private const val JSON_STOPS = "stops"
        private const val JSON_LIBRARY = "library"
        private const val JSON_TASKS = "tasks"
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
    deletedAtEpochMs?.let { put("deletedAtEpochMs", it) }
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
    deletedAtEpochMs?.let { put("deletedAtEpochMs", it) }
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

private fun JSONObject.optNullableLong(key: String): Long? =
    if (has(key) && !isNull(key)) optLong(key) else null
