package com.danielcioban.routeplanner.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.danielcioban.routeplanner.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RouteRepositoryStopTasksTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: RouteRepository
    private var routeId: Long = 0
    private var stopId: Long = 0

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RouteRepository(db)
        routeId = repository.createRoute("Test route", "", emptyList())
        stopId = repository.addStop(
            routeId,
            StopDraft(name = "Stop A", latitude = 45.75, longitude = 21.23),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun setStopCompleted_blocksWhenRequiredTaskIncomplete() = runTest {
        repository.addStopTask(stopId, "Collect signature", required = true)

        val blocked = repository.setStopCompleted(stopId, true)

        assertTrue(blocked is StopCompletionResult.BlockedByRequiredTasks)
        assertEquals(1, (blocked as StopCompletionResult.BlockedByRequiredTasks).incompleteCount)
        assertFalse(db.routeDao().getStop(stopId)!!.isCompleted)
    }

    @Test
    fun setStopCompleted_succeedsAfterRequiredTaskDone() = runTest {
        val taskId = repository.addStopTask(stopId, "Collect signature", required = true)!!
        repository.setStopTaskCompleted(taskId, completed = true, note = "")

        val result = repository.setStopCompleted(stopId, true)

        assertEquals(StopCompletionResult.Updated, result)
        assertTrue(db.routeDao().getStop(stopId)!!.isCompleted)
    }

    @Test
    fun optionalTaskDoesNotBlockStopCompletion() = runTest {
        repository.addStopTask(stopId, "Photo optional", required = false)

        assertEquals(StopCompletionResult.Updated, repository.setStopCompleted(stopId, true))
    }

    @Test
    fun resetStopCompletions_clearsTaskCompletion() = runTest {
        val taskId = repository.addStopTask(stopId, "Required", required = true)!!
        repository.setStopTaskCompleted(taskId, completed = true, note = "done")
        repository.setStopCompleted(stopId, true)

        repository.resetStopCompletions(routeId)

        val task = repository.observeStopTasks(stopId).first().single()
        assertFalse(task.isCompleted)
        assertFalse(db.routeDao().getStop(stopId)!!.isCompleted)
    }

    @Test
    fun addStopTask_rejectsBlankTitle() = runTest {
        assertNull(repository.addStopTask(stopId, "   ", required = false))
    }

    @Test
    fun duplicateRoute_copiesStopsAndRequiredTasks() = runTest {
        repository.addStopTask(stopId, "Collect signature", required = true)
        val copyId = repository.duplicateRoute(routeId, " (copy)")!!
        val copy = repository.getRoute(copyId)!!
        assertEquals("Test route (copy)", copy.route.name)
        assertEquals(1, copy.orderedStops.size)
        assertFalse(copy.orderedStops.single().isCompleted)
        val tasks = repository.observeStopTasks(copy.orderedStops.single().id).first()
        assertEquals("Collect signature", tasks.single().title)
        assertTrue(tasks.single().isRequired)
        assertFalse(tasks.single().isCompleted)
        assertTrue(copy.route.remoteId != repository.getRoute(routeId)!!.route.remoteId)
    }

    @Test
    fun optimizeStopOrder_putsCloserStopFirstFromOrigin() = runTest {
        repository.addStop(
            routeId,
            StopDraft(name = "Far", latitude = 46.0, longitude = 21.0),
        )
        repository.addStop(
            routeId,
            StopDraft(name = "Near", latitude = 45.76, longitude = 21.23),
        )
        val changed = repository.optimizeStopOrder(
            routeId,
            startLatitude = 45.75,
            startLongitude = 21.23,
        )
        assertTrue(changed)
        val names = repository.getRoute(routeId)!!.orderedStops.map { it.name }
        assertEquals(listOf("Stop A", "Near", "Far"), names)
    }
}
