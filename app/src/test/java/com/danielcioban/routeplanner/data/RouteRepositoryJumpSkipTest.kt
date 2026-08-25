package com.danielcioban.routeplanner.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.danielcioban.routeplanner.data.local.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RouteRepositoryJumpSkipTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: RouteRepository
    private var routeId: Long = 0
    private var stopA: Long = 0
    private var stopB: Long = 0
    private var stopC: Long = 0

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RouteRepository(db, routingClient = null)
        routeId = repository.createRoute("Jump skip", "", emptyList())
        stopA = repository.addStop(
            routeId,
            StopDraft(name = "A", latitude = 45.75, longitude = 21.23),
        )
        stopB = repository.addStop(
            routeId,
            StopDraft(name = "B", latitude = 45.76, longitude = 21.24),
        )
        stopC = repository.addStop(
            routeId,
            StopDraft(name = "C", latitude = 45.77, longitude = 21.25),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun skipCurrent_movesStopToEndWithoutCompleting() = runTest {
        repository.moveStopToEdge(routeId, stopA, toStart = false)
        val loaded = repository.getRoute(routeId)!!
        assertEquals(listOf(stopB, stopC, stopA), loaded.deliveryStops.map { it.id })
        assertEquals(stopB, loaded.nextIncompleteStop?.id)
        assertFalse(db.routeDao().getStop(stopA)!!.isCompleted)
    }

    @Test
    fun jumpMarkPreviousDone_completesPrefixOnly() = runTest {
        val result = repository.completeStopsBefore(routeId, stopC)
        assertEquals(StopCompletionResult.Updated, result)
        assertTrue(db.routeDao().getStop(stopA)!!.isCompleted)
        assertTrue(db.routeDao().getStop(stopB)!!.isCompleted)
        assertFalse(db.routeDao().getStop(stopC)!!.isCompleted)
        assertEquals(stopC, repository.getRoute(routeId)!!.nextIncompleteStop?.id)
    }

    @Test
    fun jumpGoOnly_completeStopsBeforeFirst_doesNotCompleteTarget() = runTest {
        val result = repository.completeStopsBefore(routeId, stopA)
        assertEquals(StopCompletionResult.Updated, result)
        assertFalse(db.routeDao().getStop(stopA)!!.isCompleted)
        assertFalse(db.routeDao().getStop(stopB)!!.isCompleted)
        assertEquals(stopA, repository.getRoute(routeId)!!.nextIncompleteStop?.id)
    }

    @Test
    fun jumpMarkPreviousDone_blockedByRequiredTask() = runTest {
        repository.addStopTask(stopA, "Collect signature", required = true)
        val result = repository.completeStopsBefore(routeId, stopC)
        assertTrue(result is StopCompletionResult.BlockedByRequiredTasks)
        assertFalse(db.routeDao().getStop(stopA)!!.isCompleted)
        assertFalse(db.routeDao().getStop(stopB)!!.isCompleted)
        assertFalse(db.routeDao().getStop(stopC)!!.isCompleted)
    }

    @Test
    fun completeStopsBefore_missingTarget() = runTest {
        assertEquals(
            StopCompletionResult.StopMissing,
            repository.completeStopsBefore(routeId, 9_999L),
        )
    }
}
