package com.danielcioban.routeplanner.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.StopDraft
import com.danielcioban.routeplanner.data.local.AppDatabase
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
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
        repository = RouteRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun export_producesFormatVersion() = runTest {
        repository.createRoute("Morning", "", emptyList())
        val exported = repository.exportBackupJson()
        assertEquals(1, JSONObject(exported).getInt("formatVersion"))
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
}
