package com.danielcioban.routeplanner.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.danielcioban.routeplanner.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RouteRepositoryLibraryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: RouteRepository

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RouteRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addStop_createsLibraryEntryAndReference() = runTest {
        val routeId = repository.createRoute("R", "", emptyList())

        val stopId = repository.addStop(
            routeId,
            StopDraft(name = "Bakery", latitude = 45.75, longitude = 21.23),
        )

        val stop = db.routeDao().getStop(stopId)!!
        assertNotNull(stop.libraryStopId)
        val lib = repository.getLibraryStop(stop.libraryStopId!!)!!
        assertEquals("Bakery", lib.name)
        assertEquals(45.75, lib.latitude, 0.0001)
    }

    @Test
    fun globalEdit_updatesAllRouteReferences() = runTest {
        val libraryId = repository.upsertLibraryStop("Shop", "", "", 45.0, 21.0)
        val a = repository.createRoute("A", "", emptyList())
        val b = repository.createRoute("B", "", emptyList())
        repository.addStopFromLibrary(a, libraryId)
        repository.addStopFromLibrary(b, libraryId)

        repository.applyLibraryPlaceEdit(
            libraryStopId = libraryId,
            name = "Shop renamed",
            addressHint = "Main st",
            notes = "",
            latitude = 45.1,
            longitude = 21.1,
            scope = LibraryEditScope.Global,
        )

        val usage = repository.getLibraryUsage(libraryId)
        assertEquals(2, usage.count)
        db.routeDao().getStopsWithLibraryId(libraryId).forEach { stop ->
            assertEquals("Shop renamed", stop.name)
            assertEquals(45.1, stop.latitude!!, 0.0001)
        }
    }

    @Test
    fun thisRouteEdit_forksLibraryCopy() = runTest {
        val libraryId = repository.upsertLibraryStop("Hub", "", "", 45.0, 21.0)
        val a = repository.createRoute("A", "", emptyList())
        val b = repository.createRoute("B", "", emptyList())
        val addedA = repository.addStopFromLibrary(a, libraryId) as AddFromLibraryResult.Added
        repository.addStopFromLibrary(b, libraryId)

        val newId = repository.applyLibraryPlaceEdit(
            libraryStopId = libraryId,
            name = "Hub local",
            addressHint = "",
            notes = "",
            latitude = 45.2,
            longitude = 21.2,
            scope = LibraryEditScope.ThisRoute,
            routeStopId = addedA.stopId,
        )

        assertNotEquals(libraryId, newId)
        assertEquals("Hub local", db.routeDao().getStop(addedA.stopId)!!.name)
        assertEquals(newId, db.routeDao().getStop(addedA.stopId)!!.libraryStopId)
        assertEquals(1, repository.getLibraryUsage(libraryId).count)
        assertEquals(1, repository.getLibraryUsage(newId).count)
        assertEquals("Hub", repository.getLibraryStop(libraryId)!!.name)
    }

    @Test
    fun deleteThisRoute_keepsLibraryAndOtherRoutes() = runTest {
        val libraryId = repository.upsertLibraryStop("Depot", "", "", 45.0, 21.0)
        val a = repository.createRoute("A", "", emptyList())
        val b = repository.createRoute("B", "", emptyList())
        val addedA = repository.addStopFromLibrary(a, libraryId) as AddFromLibraryResult.Added
        repository.addStopFromLibrary(b, libraryId)

        repository.deleteLibraryStop(
            id = libraryId,
            scope = LibraryDeleteScope.ThisRouteOnly,
            routeStopId = addedA.stopId,
            routeId = a,
        )

        assertNotNull(repository.getLibraryStop(libraryId))
        assertEquals(1, repository.getLibraryUsage(libraryId).count)
        assertNull(db.routeDao().getStop(addedA.stopId))
    }

    @Test
    fun deleteEverywhere_removesLibraryAndAllRouteStops() = runTest {
        val libraryId = repository.upsertLibraryStop("Yard", "", "", 45.0, 21.0)
        val a = repository.createRoute("A", "", emptyList())
        repository.addStopFromLibrary(a, libraryId)

        repository.deleteLibraryStop(libraryId, LibraryDeleteScope.Everywhere)

        assertNull(repository.getLibraryStop(libraryId))
        assertTrue(db.routeDao().getStopsForRoute(a).isEmpty())
    }

    @Test
    fun updateRoute_preservesTasksWhenReplacingStops() = runTest {
        val routeId = repository.createRoute(
            "R",
            "",
            listOf(StopDraft(name = "A", latitude = 45.0, longitude = 21.0)),
        )
        val stopId = db.routeDao().getStopsForRoute(routeId).first().id
        repository.addStopTask(stopId, "Scan", required = true)

        repository.updateRoute(
            routeId,
            "R",
            "",
            listOf(StopDraft(name = "A renamed", latitude = 45.0, longitude = 21.0, libraryStopId = db.routeDao().getStop(stopId)!!.libraryStopId)),
        )

        val tasks = repository.observeStopTasks(stopId).first()
        assertEquals(1, tasks.size)
        assertEquals("Scan", tasks.first().title)
        assertEquals("A renamed", db.routeDao().getStop(stopId)!!.name)
    }
}
