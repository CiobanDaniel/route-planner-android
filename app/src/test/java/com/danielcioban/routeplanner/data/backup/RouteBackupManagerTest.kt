package com.danielcioban.routeplanner.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.danielcioban.routeplanner.data.DuplicateStopResult
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.StopDraft
import com.danielcioban.routeplanner.data.local.AppDatabase
import com.danielcioban.routeplanner.data.local.TripStatus
import com.danielcioban.routeplanner.data.settings.RouteGeofenceMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RouteBackupManagerTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: RouteRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RouteRepository(db, routingClient = null)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun export_producesFormatVersion6() = runTest {
        repository.createRoute("Morning", "", emptyList())
        val exported = repository.exportBackupJson()
        val json = JSONObject(exported)
        assertEquals(6, json.getInt("formatVersion"))
        assertTrue(json.has("trips"))
        assertTrue(json.has("taskTemplates"))
        assertTrue(json.has("libraryDefaultTasks"))
        assertTrue(json.has("savedSearches"))
        assertTrue(json.has("fuelLogs"))
    }

    @Test
    fun importTwice_mergeByRemoteId() = runTest {
        repository.createRoute(
            "Route",
            "",
            listOf(StopDraft(name = "Stop", latitude = 45.0, longitude = 21.0)),
        )
        val json = repository.exportBackupJson()
        val first = repository.importBackupJson(json)
        val second = repository.importBackupJson(json)
        assertTrue(first.routesAdded + first.routesUpdated >= 1)
        assertEquals(0, second.routesAdded)
    }

    @Test
    fun importFormatVersion1_withoutTrips_stillWorks() = runTest {
        val json = JSONObject()
            .put("formatVersion", 1)
            .put(
                "routes",
                JSONArray().put(
                    JSONObject()
                        .put("remoteId", "legacy-route")
                        .put("name", "Legacy")
                        .put("notes", "")
                        .put("createdAtEpochMs", 1L)
                        .put("updatedAtEpochMs", 1L)
                        .put("roundTrip", false)
                        .put("geofenceMode", "INHERIT"),
                ),
            )
            .put("stops", JSONArray())
            .put("library", JSONArray())
            .put("tasks", JSONArray())
            .toString()
        val result = repository.importBackupJson(json)
        assertEquals(1, result.routesAdded)
        assertEquals(0, result.tripsAdded)
        assertEquals("Legacy", repository.getRoute(db.routeDao().getAllRoutes().first().id)?.route?.name)
    }

    @Test
    fun importFormatVersion3_promotesOrigin_keepsFailureAndTemplates() = runTest {
        val json = JSONObject()
            .put("formatVersion", 3)
            .put(
                "routes",
                JSONArray().put(
                    JSONObject()
                        .put("remoteId", "v3-route")
                        .put("name", "v3 morning")
                        .put("notes", "")
                        .put("createdAtEpochMs", 1L)
                        .put("updatedAtEpochMs", 1L)
                        .put("roundTrip", false)
                        .put("geofenceMode", "INHERIT"),
                ),
            )
            .put(
                "stops",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("remoteId", "v3-origin")
                            .put("routeRemoteId", "v3-route")
                            .put("position", 0)
                            .put("name", "Depot")
                            .put("latitude", 45.75)
                            .put("longitude", 21.23)
                            .put("isOrigin", true)
                            .put("isCompleted", false),
                    )
                    .put(
                        JSONObject()
                            .put("remoteId", "v3-stop")
                            .put("routeRemoteId", "v3-route")
                            .put("position", 1)
                            .put("name", "Shop")
                            .put("latitude", 45.76)
                            .put("longitude", 21.24)
                            .put("isOrigin", false)
                            .put("isCompleted", true)
                            .put("failureReason", "NOT_HOME"),
                    ),
            )
            .put("library", JSONArray())
            .put("tasks", JSONArray())
            .put(
                "taskTemplates",
                JSONArray().put(
                    JSONObject()
                        .put("remoteId", "v3-tpl")
                        .put("title", "COD")
                        .put("isRequired", true)
                        .put("sortOrder", 0),
                ),
            )
            .toString()
        val result = repository.importBackupJson(json)
        assertEquals(1, result.routesAdded)
        assertEquals(1, result.templatesAdded)
        val route = repository.getRoute(db.routeDao().getAllRoutes().first().id)!!
        assertEquals(45.75, route.route.originLatitude!!, 0.0001)
        assertEquals(21.23, route.route.originLongitude!!, 0.0001)
        assertTrue(route.orderedStops.none { it.isOrigin })
        val shop = route.deliveryStops.single()
        assertEquals("Shop", shop.name)
        assertEquals("NOT_HOME", shop.failureReason)
        val templates = db.taskTemplateDao().getAll()
        assertTrue(templates.any { it.title == "COD" && it.isRequired })
    }

    @Test
    fun exportImport_secondDatabase_preservesLibraryTasksGeofenceHistory() = runTest {
        val libId = repository.upsertLibraryStop(
            name = "Depot",
            addressHint = "Yard",
            notes = "Gate code 1",
            latitude = 45.75,
            longitude = 21.23,
        )
        val routeId = repository.createRoute(
            name = "Morning",
            notes = "Van A",
            stops = listOf(
                StopDraft(
                    name = "Depot",
                    addressHint = "Yard",
                    notes = "Gate code 1",
                    latitude = 45.75,
                    longitude = 21.23,
                    libraryStopId = libId,
                    geofenceRadiusMeters = 80,
                ),
            ),
            roundTrip = true,
            geofenceMode = RouteGeofenceMode.COMPLETE.name,
            geofenceRadiusMeters = 120,
        )
        val stopId = repository.getRoute(routeId)!!.orderedStops.first().id
        repository.addStopTask(stopId, "Scan parcel", required = true)
        val tripId = repository.startRouteTrip(routeId, "Morning", stopsTotal = 1)
        repository.finishTrip(tripId, TripStatus.COMPLETED, stopsCompleted = 1, stopsTotal = 1)
        val inProgressId = repository.startQuickTrip("Cafe", 45.76, 21.24, libraryStopId = libId)

        val json = repository.exportBackupJson()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val db2 = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo2 = RouteRepository(db2, routingClient = null)
            val result = repo2.importBackupJson(json)
            assertEquals(1, result.routesAdded)
            assertEquals(1, result.libraryAdded)
            assertEquals(1, result.stopsAdded)
            assertEquals(1, result.tasksAdded)
            assertTrue(result.tripsAdded >= 2)

            val restored = db2.routeDao().getAllRoutes().first()
            assertEquals("Morning", restored.name)
            assertTrue(restored.roundTrip)
            assertEquals(RouteGeofenceMode.COMPLETE.name, restored.geofenceMode)
            assertEquals(120, restored.geofenceRadiusMeters)

            val stop = db2.routeDao().getStopsForRoute(restored.id).first()
            assertNotNull(stop.libraryStopId)
            assertEquals(80, stop.geofenceRadiusMeters)
            val lib = db2.stopLibraryDao().getById(stop.libraryStopId!!)
            assertEquals("Depot", lib?.name)
            assertEquals("Yard", lib?.addressHint)

            val tasks = db2.stopTaskDao().getAll()
            assertEquals(1, tasks.size)
            assertEquals("Scan parcel", tasks.first().title)
            assertTrue(tasks.first().isRequired)

            val trips = db2.tripHistoryDao().getAll()
            assertTrue(trips.any { it.status == TripStatus.COMPLETED && it.title == "Morning" })
            val importedLive = trips.first { it.remoteId == db.tripHistoryDao().getById(inProgressId)!!.remoteId }
            assertEquals(TripStatus.CANCELLED, importedLive.status)
            assertNotNull(importedLive.endedAtEpochMs)
            assertEquals(lib?.id, importedLive.libraryStopId)
        } finally {
            db2.close()
        }
    }

    @Test
    fun preview_sameDatabase_countsOverwrite() = runTest {
        repository.createRoute("Morning", "", emptyList())
        val preview = repository.previewBackupJson(repository.exportBackupJson())
        assertEquals(1, preview.routeOverwrites)
        assertEquals(1, preview.wouldOverwrite)
    }

    @Test
    fun import_keepLocalOnConflict_skipsNewerFile() = runTest {
        val routeId = repository.createRoute("Local", "", emptyList())
        val json = JSONObject(repository.exportBackupJson())
        json.getJSONArray("routes").getJSONObject(0)
            .put("name", "From file")
            .put("updatedAtEpochMs", System.currentTimeMillis() + 60_000)
        val mutated = json.toString()
        val skipped = repository.importBackupJson(
            mutated,
            BackupImportOptions(keepLocalOnConflict = true),
        )
        assertTrue(skipped.routesSkipped >= 1)
        assertEquals("Local", repository.getRoute(routeId)?.route?.name)
        val overwritten = repository.importBackupJson(
            mutated,
            BackupImportOptions(keepLocalOnConflict = false),
        )
        assertTrue(overwritten.routesUpdated >= 1)
        assertEquals("From file", repository.getRoute(routeId)?.route?.name)
    }

    @Test
    fun import_selectiveHistoryOff_skipsTrips() = runTest {
        val tripId = repository.startQuickTrip("Cafe", 45.76, 21.24)
        repository.finishTrip(tripId, TripStatus.COMPLETED, 1, 1)
        val json = repository.exportBackupJson()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db2 = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo2 = RouteRepository(db2, routingClient = null)
            val result = repo2.importBackupJson(
                json,
                BackupImportOptions(includeHistory = false),
            )
            assertEquals(0, result.tripsAdded)
            assertTrue(db2.tripHistoryDao().getAll().isEmpty())
        } finally {
            db2.close()
        }
    }

    @Test
    fun softDelete_hidesFromObserveRoutes() = runTest {
        val routeId = repository.createRoute("Gone", "", emptyList())
        repository.deleteRoute(routeId)
        assertTrue(repository.observeRoutes().first().isEmpty())
        assertEquals(1, repository.observeTrashRoutes().first().size)
        repository.restoreRoute(routeId)
        assertEquals(1, repository.observeRoutes().first().size)
    }

    @Test
    fun duplicateStopToRoute_copiesPinnedStop() = runTest {
        val sourceId = repository.createRoute(
            "A",
            "",
            listOf(StopDraft(name = "Shop", latitude = 45.0, longitude = 21.0)),
        )
        val targetId = repository.createRoute("B", "", emptyList())
        val stopId = repository.getRoute(sourceId)!!.orderedStops.first().id
        val copied = repository.duplicateStopToRoute(stopId, targetId)
        assertTrue(copied is DuplicateStopResult.Copied)
        assertEquals("Shop", repository.getRoute(targetId)!!.orderedStops.first().name)
    }

    @Test
    fun duplicateStopToRoute_sameRoute_isSameRoute() = runTest {
        val sourceId = repository.createRoute(
            "A",
            "",
            listOf(StopDraft(name = "Shop", latitude = 45.0, longitude = 21.0)),
        )
        val stopId = repository.getRoute(sourceId)!!.orderedStops.first().id
        assertEquals(DuplicateStopResult.SameRoute, repository.duplicateStopToRoute(stopId, sourceId))
    }

    @Test
    fun vacuum_keepsInProgressTrips() = runTest {
        val oldId = repository.startQuickTrip("Old", 45.0, 21.0)
        repository.finishTrip(oldId, TripStatus.COMPLETED, 1, 1)
        val old = db.tripHistoryDao().getById(oldId)!!
        db.tripHistoryDao().update(old.copy(startedAtEpochMs = 1L))
        val liveId = repository.startQuickTrip("Live", 45.1, 21.1)
        val removed = repository.deleteTripsOlderThan(1_000L)
        assertEquals(1, removed)
        assertFalse(db.tripHistoryDao().getAll().any { it.id == oldId })
        assertNotNull(db.tripHistoryDao().getById(liveId))
    }
}
