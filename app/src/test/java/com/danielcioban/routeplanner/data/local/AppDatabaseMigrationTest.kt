package com.danielcioban.routeplanner.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
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
class AppDatabaseMigrationTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        deleteAllTestDatabases()
    }

    @After
    fun tearDown() {
        deleteAllTestDatabases()
    }

    @Test
    fun migrate2To3_createsStopTasksTable() {
        createVersion2Database(DB_2_3)

        withMigrated(DB_2_3) { migrated ->
            migrated.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='stop_tasks'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
        }
    }

    @Test
    fun migrate3To4_addsRemoteIdColumns() {
        createVersion3Database(DB_3_4)

        withMigrated(DB_3_4) { migrated ->
            migrated.query("PRAGMA table_info(routes)").use { cursor ->
                val columns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
                }
                assertTrue(columns.contains("remoteId"))
                assertTrue(columns.contains("deletedAtEpochMs"))
            }
            migrated.query("SELECT remoteId FROM routes WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.getString(0).isNotBlank())
            }
        }
    }

    @Test
    fun migrate4To5_backfillsLibraryReferences() {
        createVersion4Database(DB_4_5)

        withMigrated(DB_4_5) { migrated ->
            migrated.query("PRAGMA index_list(stops)").use { cursor ->
                val names = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    names += cursor.getString(cursor.getColumnIndexOrThrow("name"))
                }
                assertTrue(names.any { it.contains("libraryStopId") })
            }
            migrated.query("SELECT libraryStopId FROM stops WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.getLong(0) > 0L)
            }
            migrated.query("SELECT COUNT(*) FROM stop_library").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.getLong(0) >= 1L)
            }
        }
    }

    private fun withMigrated(name: String, block: (SupportSQLiteDatabase) -> Unit) {
        val room = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            // Do not Closeable.use() Room's SupportSQLiteDatabase — that closes the
            // connection pool. Query, then close the RoomDatabase.
            block(room.openHelper.writableDatabase)
        } finally {
            room.close()
        }
    }

    private fun openFixture(name: String, version: Int): SQLiteDatabase {
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val sqlite = SQLiteDatabase.openOrCreateDatabase(file.path, null)
        sqlite.version = version
        return sqlite
    }

    private fun createStopLibraryIndexes(sqlite: SQLiteDatabase) {
        sqlite.execSQL("CREATE INDEX index_stop_library_name ON stop_library (name)")
        sqlite.execSQL(
            "CREATE INDEX index_stop_library_updatedAtEpochMs ON stop_library (updatedAtEpochMs)",
        )
    }

    private fun createVersion2Database(name: String) {
        val sqlite = openFixture(name, version = 2)
        sqlite.execSQL(
            """
            CREATE TABLE routes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                notes TEXT NOT NULL,
                createdAtEpochMs INTEGER NOT NULL,
                updatedAtEpochMs INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        sqlite.execSQL(
            """
            CREATE TABLE stops (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                routeId INTEGER NOT NULL,
                position INTEGER NOT NULL,
                name TEXT NOT NULL,
                addressHint TEXT NOT NULL,
                notes TEXT NOT NULL,
                latitude REAL,
                longitude REAL,
                isCompleted INTEGER NOT NULL,
                libraryStopId INTEGER,
                FOREIGN KEY(routeId) REFERENCES routes(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE INDEX index_stops_routeId ON stops (routeId)")
        sqlite.execSQL(
            """
            CREATE TABLE stop_library (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                addressHint TEXT NOT NULL,
                notes TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                createdAtEpochMs INTEGER NOT NULL,
                updatedAtEpochMs INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        createStopLibraryIndexes(sqlite)
        sqlite.execSQL(
            "INSERT INTO routes (name, notes, createdAtEpochMs, updatedAtEpochMs) VALUES ('R', '', 1, 1)",
        )
        sqlite.execSQL(
            """
            INSERT INTO stops (
                routeId, position, name, addressHint, notes,
                latitude, longitude, isCompleted, libraryStopId
            ) VALUES (1, 0, 'S', '', '', 45.0, 21.0, 0, NULL)
            """.trimIndent(),
        )
        sqlite.close()
    }

    private fun createVersion3Database(name: String) {
        val sqlite = openFixture(name, version = 3)
        sqlite.execSQL(
            """
            CREATE TABLE routes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                notes TEXT NOT NULL,
                createdAtEpochMs INTEGER NOT NULL,
                updatedAtEpochMs INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        sqlite.execSQL(
            """
            CREATE TABLE stops (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                routeId INTEGER NOT NULL,
                position INTEGER NOT NULL,
                name TEXT NOT NULL,
                addressHint TEXT NOT NULL,
                notes TEXT NOT NULL,
                latitude REAL,
                longitude REAL,
                isCompleted INTEGER NOT NULL,
                libraryStopId INTEGER,
                FOREIGN KEY(routeId) REFERENCES routes(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE INDEX index_stops_routeId ON stops (routeId)")
        sqlite.execSQL(
            """
            CREATE TABLE stop_library (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                addressHint TEXT NOT NULL,
                notes TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                createdAtEpochMs INTEGER NOT NULL,
                updatedAtEpochMs INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        createStopLibraryIndexes(sqlite)
        sqlite.execSQL(
            """
            CREATE TABLE stop_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                stopId INTEGER NOT NULL,
                title TEXT NOT NULL,
                isRequired INTEGER NOT NULL,
                isCompleted INTEGER NOT NULL,
                completedAtEpochMs INTEGER,
                completionNote TEXT NOT NULL,
                FOREIGN KEY(stopId) REFERENCES stops(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE INDEX index_stop_tasks_stopId ON stop_tasks (stopId)")
        sqlite.execSQL(
            "INSERT INTO routes (name, notes, createdAtEpochMs, updatedAtEpochMs) VALUES ('R', '', 1, 1)",
        )
        sqlite.close()
    }

    private fun createVersion4Database(name: String) {
        val sqlite = openFixture(name, version = 4)
        sqlite.execSQL(
            """
            CREATE TABLE routes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                name TEXT NOT NULL,
                notes TEXT NOT NULL,
                createdAtEpochMs INTEGER NOT NULL,
                updatedAtEpochMs INTEGER NOT NULL,
                deletedAtEpochMs INTEGER
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE UNIQUE INDEX index_routes_remoteId ON routes (remoteId)")
        sqlite.execSQL(
            """
            CREATE TABLE stops (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                routeId INTEGER NOT NULL,
                position INTEGER NOT NULL,
                name TEXT NOT NULL,
                addressHint TEXT NOT NULL,
                notes TEXT NOT NULL,
                latitude REAL,
                longitude REAL,
                isCompleted INTEGER NOT NULL,
                libraryStopId INTEGER,
                deletedAtEpochMs INTEGER,
                FOREIGN KEY(routeId) REFERENCES routes(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE INDEX index_stops_routeId ON stops (routeId)")
        sqlite.execSQL("CREATE UNIQUE INDEX index_stops_remoteId ON stops (remoteId)")
        sqlite.execSQL(
            """
            CREATE TABLE stop_library (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                name TEXT NOT NULL,
                addressHint TEXT NOT NULL,
                notes TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                createdAtEpochMs INTEGER NOT NULL,
                updatedAtEpochMs INTEGER NOT NULL,
                deletedAtEpochMs INTEGER
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE UNIQUE INDEX index_stop_library_remoteId ON stop_library (remoteId)")
        createStopLibraryIndexes(sqlite)
        sqlite.execSQL(
            """
            CREATE TABLE stop_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                stopId INTEGER NOT NULL,
                title TEXT NOT NULL,
                isRequired INTEGER NOT NULL,
                isCompleted INTEGER NOT NULL,
                completedAtEpochMs INTEGER,
                completionNote TEXT NOT NULL,
                deletedAtEpochMs INTEGER,
                FOREIGN KEY(stopId) REFERENCES stops(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE INDEX index_stop_tasks_stopId ON stop_tasks (stopId)")
        sqlite.execSQL("CREATE UNIQUE INDEX index_stop_tasks_remoteId ON stop_tasks (remoteId)")
        sqlite.execSQL(
            "INSERT INTO routes (remoteId, name, notes, createdAtEpochMs, updatedAtEpochMs) VALUES ('r1', 'R', '', 1, 1)",
        )
        sqlite.execSQL(
            """
            INSERT INTO stops (
                remoteId, routeId, position, name, addressHint, notes,
                latitude, longitude, isCompleted, libraryStopId
            ) VALUES ('s1', 1, 0, 'S', '', '', 45.0, 21.0, 0, NULL)
            """.trimIndent(),
        )
        sqlite.close()
    }

    @Test
    fun migrate8To9_addsOriginFailureAndTemplates() {
        createVersion8Database(DB_8_9)

        withMigrated(DB_8_9) { migrated ->
            migrated.query("PRAGMA table_info(stops)").use { cursor ->
                val columns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
                }
                assertTrue(columns.contains("isOrigin"))
                assertTrue(columns.contains("failureReason"))
            }
            migrated.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='task_templates'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
        }
    }

    @Test
    fun migrate9To10_promotesOriginOntoRouteAndAddsVanDayColumns() {
        createVersion9Database(DB_9_10)

        withMigrated(DB_9_10) { migrated ->
            migrated.query("PRAGMA table_info(routes)").use { cursor ->
                val columns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
                }
                assertTrue(columns.contains("originLatitude"))
                assertTrue(columns.contains("originLongitude"))
                assertTrue(columns.contains("colorHex"))
                assertTrue(columns.contains("vanName"))
                assertTrue(columns.contains("shiftName"))
                assertTrue(columns.contains("archived"))
            }
            migrated.query("PRAGMA table_info(stops)").use { cursor ->
                val columns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
                }
                assertTrue(columns.contains("arriveByEpochMs"))
                assertTrue(columns.contains("isFixedOrder"))
                assertTrue(columns.contains("isBreak"))
                assertTrue(columns.contains("phone"))
                assertTrue(columns.contains("doorCode"))
                assertTrue(columns.contains("failurePhotoPath"))
                assertTrue(columns.contains("failureSignaturePath"))
                assertTrue(columns.contains("tasksDeferred"))
            }
            migrated.query(
                "SELECT originLatitude, originLongitude FROM routes WHERE id = 1",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(45.75, cursor.getDouble(0), 0.0001)
                assertEquals(21.23, cursor.getDouble(1), 0.0001)
            }
            migrated.query("SELECT COUNT(*) FROM stops WHERE isOrigin = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0L, cursor.getLong(0))
            }
            migrated.query("SELECT COUNT(*) FROM stops").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1L, cursor.getLong(0))
            }
        }
    }

    @Test
    fun migrate10To11_addsLibraryPlaceFieldsAndSavedSearches() {
        createVersion10Database(DB_10_11)

        withMigrated(DB_10_11) { migrated ->
            migrated.query("PRAGMA table_info(stop_library)").use { cursor ->
                val columns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
                }
                assertTrue(columns.contains("tags"))
                assertTrue(columns.contains("plusCode"))
                assertTrue(columns.contains("what3words"))
                assertTrue(columns.contains("lastUsedAtEpochMs"))
                assertTrue(columns.contains("useCount"))
                assertTrue(columns.contains("defaultGeofenceRadiusMeters"))
            }
            migrated.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='library_default_tasks'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
            migrated.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='saved_searches'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
            migrated.query("SELECT tags, useCount FROM stop_library WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
            }
        }
    }

    @Test
    fun migrate11To12_addsPodCodFuelAndFavorites() {
        createVersion11Database(DB_11_12)

        withMigrated(DB_11_12) { migrated ->
            migrated.query("PRAGMA table_info(stops)").use { cursor ->
                val columns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
                }
                assertTrue(columns.contains("podPhotoPath"))
                assertTrue(columns.contains("codAmount"))
                assertTrue(columns.contains("barcode"))
            }
            migrated.query("PRAGMA table_info(routes)").use { cursor ->
                val columns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
                }
                assertTrue(columns.contains("shiftSlot"))
            }
            migrated.query("PRAGMA table_info(stop_library)").use { cursor ->
                val columns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
                }
                assertTrue(columns.contains("isFavorite"))
            }
            migrated.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='fuel_log'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
            migrated.query("SELECT shiftSlot FROM routes WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("", cursor.getString(0))
            }
        }
    }

    private fun createVersion8Database(name: String) {
        val sqlite = openFixture(name, version = 8)
        sqlite.execSQL(
            """
            CREATE TABLE routes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                name TEXT NOT NULL,
                notes TEXT NOT NULL,
                createdAtEpochMs INTEGER NOT NULL,
                updatedAtEpochMs INTEGER NOT NULL,
                deletedAtEpochMs INTEGER,
                roundTrip INTEGER NOT NULL,
                geofenceMode TEXT NOT NULL,
                geofenceRadiusMeters INTEGER
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE UNIQUE INDEX index_routes_remoteId ON routes (remoteId)")
        sqlite.execSQL(
            """
            CREATE TABLE stop_library (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                name TEXT NOT NULL,
                addressHint TEXT NOT NULL,
                notes TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                createdAtEpochMs INTEGER NOT NULL,
                updatedAtEpochMs INTEGER NOT NULL,
                deletedAtEpochMs INTEGER
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE UNIQUE INDEX index_stop_library_remoteId ON stop_library (remoteId)")
        createStopLibraryIndexes(sqlite)
        sqlite.execSQL(
            """
            CREATE TABLE stops (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                routeId INTEGER NOT NULL,
                position INTEGER NOT NULL,
                name TEXT NOT NULL,
                addressHint TEXT NOT NULL,
                notes TEXT NOT NULL,
                latitude REAL,
                longitude REAL,
                isCompleted INTEGER NOT NULL,
                libraryStopId INTEGER,
                deletedAtEpochMs INTEGER,
                arriveByMinutes INTEGER,
                serviceMinutes INTEGER NOT NULL,
                geofenceRadiusMeters INTEGER,
                isVisited INTEGER NOT NULL,
                visitedAtEpochMs INTEGER,
                FOREIGN KEY(routeId) REFERENCES routes(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE INDEX index_stops_routeId ON stops (routeId)")
        sqlite.execSQL("CREATE INDEX index_stops_libraryStopId ON stops (libraryStopId)")
        sqlite.execSQL("CREATE UNIQUE INDEX index_stops_remoteId ON stops (remoteId)")
        sqlite.execSQL(
            """
            CREATE TABLE stop_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                stopId INTEGER NOT NULL,
                title TEXT NOT NULL,
                isRequired INTEGER NOT NULL,
                isCompleted INTEGER NOT NULL,
                completedAtEpochMs INTEGER,
                completionNote TEXT NOT NULL,
                deletedAtEpochMs INTEGER,
                FOREIGN KEY(stopId) REFERENCES stops(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE INDEX index_stop_tasks_stopId ON stop_tasks (stopId)")
        sqlite.execSQL("CREATE UNIQUE INDEX index_stop_tasks_remoteId ON stop_tasks (remoteId)")
        sqlite.execSQL(
            """
            CREATE TABLE trip_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                kind TEXT NOT NULL,
                status TEXT NOT NULL,
                title TEXT NOT NULL,
                startedAtEpochMs INTEGER NOT NULL,
                endedAtEpochMs INTEGER,
                routeId INTEGER,
                libraryStopId INTEGER,
                destLatitude REAL,
                destLongitude REAL,
                destName TEXT NOT NULL,
                stopsCompleted INTEGER NOT NULL,
                stopsTotal INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        sqlite.execSQL(
            "CREATE INDEX index_trip_history_startedAtEpochMs ON trip_history (startedAtEpochMs)",
        )
        sqlite.execSQL("CREATE INDEX index_trip_history_status ON trip_history (status)")
        sqlite.execSQL("CREATE INDEX index_trip_history_routeId ON trip_history (routeId)")
        sqlite.execSQL("CREATE UNIQUE INDEX index_trip_history_remoteId ON trip_history (remoteId)")
        sqlite.execSQL(
            """
            INSERT INTO routes (
                remoteId, name, notes, createdAtEpochMs, updatedAtEpochMs,
                roundTrip, geofenceMode
            ) VALUES ('r1', 'R', '', 1, 1, 0, 'INHERIT')
            """.trimIndent(),
        )
        sqlite.execSQL(
            """
            INSERT INTO stops (
                remoteId, routeId, position, name, addressHint, notes,
                latitude, longitude, isCompleted, libraryStopId, serviceMinutes, isVisited
            ) VALUES ('s1', 1, 0, 'S', '', '', 45.0, 21.0, 0, NULL, 0, 0)
            """.trimIndent(),
        )
        sqlite.close()
    }

    private fun createVersion9Database(name: String) {
        val sqlite = openFixture(name, version = 9)
        sqlite.execSQL(
            """
            CREATE TABLE routes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                name TEXT NOT NULL,
                notes TEXT NOT NULL,
                createdAtEpochMs INTEGER NOT NULL,
                updatedAtEpochMs INTEGER NOT NULL,
                deletedAtEpochMs INTEGER,
                roundTrip INTEGER NOT NULL,
                geofenceMode TEXT NOT NULL,
                geofenceRadiusMeters INTEGER
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE UNIQUE INDEX index_routes_remoteId ON routes (remoteId)")
        sqlite.execSQL(
            """
            CREATE TABLE stop_library (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                name TEXT NOT NULL,
                addressHint TEXT NOT NULL,
                notes TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                createdAtEpochMs INTEGER NOT NULL,
                updatedAtEpochMs INTEGER NOT NULL,
                deletedAtEpochMs INTEGER
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE UNIQUE INDEX index_stop_library_remoteId ON stop_library (remoteId)")
        createStopLibraryIndexes(sqlite)
        sqlite.execSQL(
            """
            CREATE TABLE stops (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                routeId INTEGER NOT NULL,
                position INTEGER NOT NULL,
                name TEXT NOT NULL,
                addressHint TEXT NOT NULL,
                notes TEXT NOT NULL,
                latitude REAL,
                longitude REAL,
                isCompleted INTEGER NOT NULL,
                libraryStopId INTEGER,
                deletedAtEpochMs INTEGER,
                arriveByMinutes INTEGER,
                serviceMinutes INTEGER NOT NULL,
                geofenceRadiusMeters INTEGER,
                isVisited INTEGER NOT NULL,
                visitedAtEpochMs INTEGER,
                isOrigin INTEGER NOT NULL,
                failureReason TEXT,
                FOREIGN KEY(routeId) REFERENCES routes(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE INDEX index_stops_routeId ON stops (routeId)")
        sqlite.execSQL("CREATE INDEX index_stops_libraryStopId ON stops (libraryStopId)")
        sqlite.execSQL("CREATE UNIQUE INDEX index_stops_remoteId ON stops (remoteId)")
        sqlite.execSQL(
            """
            CREATE TABLE stop_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                stopId INTEGER NOT NULL,
                title TEXT NOT NULL,
                isRequired INTEGER NOT NULL,
                isCompleted INTEGER NOT NULL,
                completedAtEpochMs INTEGER,
                completionNote TEXT NOT NULL,
                deletedAtEpochMs INTEGER,
                FOREIGN KEY(stopId) REFERENCES stops(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE INDEX index_stop_tasks_stopId ON stop_tasks (stopId)")
        sqlite.execSQL("CREATE UNIQUE INDEX index_stop_tasks_remoteId ON stop_tasks (remoteId)")
        sqlite.execSQL(
            """
            CREATE TABLE trip_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                kind TEXT NOT NULL,
                status TEXT NOT NULL,
                title TEXT NOT NULL,
                startedAtEpochMs INTEGER NOT NULL,
                endedAtEpochMs INTEGER,
                routeId INTEGER,
                libraryStopId INTEGER,
                destLatitude REAL,
                destLongitude REAL,
                destName TEXT NOT NULL,
                stopsCompleted INTEGER NOT NULL,
                stopsTotal INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        sqlite.execSQL(
            "CREATE INDEX index_trip_history_startedAtEpochMs ON trip_history (startedAtEpochMs)",
        )
        sqlite.execSQL("CREATE INDEX index_trip_history_status ON trip_history (status)")
        sqlite.execSQL("CREATE INDEX index_trip_history_routeId ON trip_history (routeId)")
        sqlite.execSQL("CREATE UNIQUE INDEX index_trip_history_remoteId ON trip_history (remoteId)")
        sqlite.execSQL(
            """
            CREATE TABLE task_templates (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                title TEXT NOT NULL,
                isRequired INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE UNIQUE INDEX index_task_templates_remoteId ON task_templates (remoteId)")
        sqlite.execSQL(
            """
            INSERT INTO routes (
                remoteId, name, notes, createdAtEpochMs, updatedAtEpochMs,
                roundTrip, geofenceMode
            ) VALUES ('r1', 'R', '', 1, 1, 0, 'INHERIT')
            """.trimIndent(),
        )
        sqlite.execSQL(
            """
            INSERT INTO stops (
                remoteId, routeId, position, name, addressHint, notes,
                latitude, longitude, isCompleted, libraryStopId, serviceMinutes, isVisited, isOrigin
            ) VALUES ('origin', 1, 0, 'Van', '', '', 45.75, 21.23, 0, NULL, 0, 0, 1)
            """.trimIndent(),
        )
        sqlite.execSQL(
            """
            INSERT INTO stops (
                remoteId, routeId, position, name, addressHint, notes,
                latitude, longitude, isCompleted, libraryStopId, serviceMinutes, isVisited, isOrigin
            ) VALUES ('s1', 1, 1, 'S', '', '', 45.0, 21.0, 0, NULL, 0, 0, 0)
            """.trimIndent(),
        )
        sqlite.close()
    }

    private fun createVersion10Database(name: String) {
        val sqlite = openFixture(name, version = 10)
        sqlite.execSQL(
            """
            CREATE TABLE routes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                name TEXT NOT NULL,
                notes TEXT NOT NULL,
                createdAtEpochMs INTEGER NOT NULL,
                updatedAtEpochMs INTEGER NOT NULL,
                deletedAtEpochMs INTEGER,
                roundTrip INTEGER NOT NULL,
                geofenceMode TEXT NOT NULL,
                geofenceRadiusMeters INTEGER,
                originLatitude REAL,
                originLongitude REAL,
                colorHex TEXT NOT NULL,
                vanName TEXT NOT NULL,
                shiftName TEXT NOT NULL,
                archived INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE UNIQUE INDEX index_routes_remoteId ON routes (remoteId)")
        sqlite.execSQL(
            """
            CREATE TABLE stop_library (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                name TEXT NOT NULL,
                addressHint TEXT NOT NULL,
                notes TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                createdAtEpochMs INTEGER NOT NULL,
                updatedAtEpochMs INTEGER NOT NULL,
                deletedAtEpochMs INTEGER
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE UNIQUE INDEX index_stop_library_remoteId ON stop_library (remoteId)")
        createStopLibraryIndexes(sqlite)
        sqlite.execSQL(
            """
            CREATE TABLE stops (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                routeId INTEGER NOT NULL,
                position INTEGER NOT NULL,
                name TEXT NOT NULL,
                addressHint TEXT NOT NULL,
                notes TEXT NOT NULL,
                latitude REAL,
                longitude REAL,
                isCompleted INTEGER NOT NULL,
                libraryStopId INTEGER,
                deletedAtEpochMs INTEGER,
                arriveByMinutes INTEGER,
                serviceMinutes INTEGER NOT NULL,
                geofenceRadiusMeters INTEGER,
                isVisited INTEGER NOT NULL,
                visitedAtEpochMs INTEGER,
                isOrigin INTEGER NOT NULL,
                failureReason TEXT,
                arriveByEpochMs INTEGER,
                isFixedOrder INTEGER NOT NULL,
                isBreak INTEGER NOT NULL,
                phone TEXT NOT NULL,
                doorCode TEXT NOT NULL,
                failurePhotoPath TEXT,
                failureSignaturePath TEXT,
                tasksDeferred INTEGER NOT NULL,
                tasksDeferredNote TEXT NOT NULL,
                FOREIGN KEY(routeId) REFERENCES routes(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE INDEX index_stops_routeId ON stops (routeId)")
        sqlite.execSQL("CREATE INDEX index_stops_libraryStopId ON stops (libraryStopId)")
        sqlite.execSQL("CREATE UNIQUE INDEX index_stops_remoteId ON stops (remoteId)")
        sqlite.execSQL(
            """
            CREATE TABLE stop_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                stopId INTEGER NOT NULL,
                title TEXT NOT NULL,
                isRequired INTEGER NOT NULL,
                isCompleted INTEGER NOT NULL,
                completedAtEpochMs INTEGER,
                completionNote TEXT NOT NULL,
                deletedAtEpochMs INTEGER,
                FOREIGN KEY(stopId) REFERENCES stops(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE INDEX index_stop_tasks_stopId ON stop_tasks (stopId)")
        sqlite.execSQL("CREATE UNIQUE INDEX index_stop_tasks_remoteId ON stop_tasks (remoteId)")
        sqlite.execSQL(
            """
            CREATE TABLE trip_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                kind TEXT NOT NULL,
                status TEXT NOT NULL,
                title TEXT NOT NULL,
                startedAtEpochMs INTEGER NOT NULL,
                endedAtEpochMs INTEGER,
                routeId INTEGER,
                libraryStopId INTEGER,
                destLatitude REAL,
                destLongitude REAL,
                destName TEXT NOT NULL,
                stopsCompleted INTEGER NOT NULL,
                stopsTotal INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        sqlite.execSQL(
            "CREATE INDEX index_trip_history_startedAtEpochMs ON trip_history (startedAtEpochMs)",
        )
        sqlite.execSQL("CREATE INDEX index_trip_history_status ON trip_history (status)")
        sqlite.execSQL("CREATE INDEX index_trip_history_routeId ON trip_history (routeId)")
        sqlite.execSQL("CREATE UNIQUE INDEX index_trip_history_remoteId ON trip_history (remoteId)")
        sqlite.execSQL(
            """
            CREATE TABLE task_templates (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                title TEXT NOT NULL,
                isRequired INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        sqlite.execSQL("CREATE UNIQUE INDEX index_task_templates_remoteId ON task_templates (remoteId)")
        sqlite.execSQL(
            """
            INSERT INTO routes (
                remoteId, name, notes, createdAtEpochMs, updatedAtEpochMs,
                roundTrip, geofenceMode, colorHex, vanName, shiftName, archived
            ) VALUES ('r1', 'R', '', 1, 1, 0, 'INHERIT', '', '', '', 0)
            """.trimIndent(),
        )
        sqlite.execSQL(
            """
            INSERT INTO stop_library (
                remoteId, name, addressHint, notes, latitude, longitude,
                createdAtEpochMs, updatedAtEpochMs, deletedAtEpochMs
            ) VALUES ('lib1', 'Shop', '', '', 45.0, 21.0, 1, 1, NULL)
            """.trimIndent(),
        )
        sqlite.close()
    }

    private fun createVersion11Database(name: String) {
        createVersion10Database(name)
        val sqlite = SQLiteDatabase.openDatabase(
            context.getDatabasePath(name).path,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        )
        sqlite.execSQL("ALTER TABLE stop_library ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
        sqlite.execSQL("ALTER TABLE stop_library ADD COLUMN plusCode TEXT NOT NULL DEFAULT ''")
        sqlite.execSQL("ALTER TABLE stop_library ADD COLUMN what3words TEXT NOT NULL DEFAULT ''")
        sqlite.execSQL(
            "ALTER TABLE stop_library ADD COLUMN lastUsedAtEpochMs INTEGER NOT NULL DEFAULT 0",
        )
        sqlite.execSQL("ALTER TABLE stop_library ADD COLUMN useCount INTEGER NOT NULL DEFAULT 0")
        sqlite.execSQL("ALTER TABLE stop_library ADD COLUMN defaultGeofenceRadiusMeters INTEGER")
        sqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS library_default_tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                libraryStopId INTEGER NOT NULL,
                title TEXT NOT NULL,
                isRequired INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL,
                FOREIGN KEY(libraryStopId) REFERENCES stop_library(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        sqlite.execSQL(
            "CREATE INDEX IF NOT EXISTS index_library_default_tasks_libraryStopId ON library_default_tasks (libraryStopId)",
        )
        sqlite.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_library_default_tasks_remoteId ON library_default_tasks (remoteId)",
        )
        sqlite.execSQL(
            """
            CREATE TABLE IF NOT EXISTS saved_searches (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId TEXT NOT NULL,
                query TEXT NOT NULL,
                nearMeOnly INTEGER NOT NULL,
                createdAtEpochMs INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        sqlite.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_saved_searches_remoteId ON saved_searches (remoteId)",
        )
        sqlite.version = 11
        sqlite.close()
    }

    private fun deleteAllTestDatabases() {
        listOf(DB_2_3, DB_3_4, DB_4_5, DB_8_9, DB_9_10, DB_10_11, DB_11_12).forEach {
            context.deleteDatabase(it)
        }
    }

    companion object {
        private const val DB_2_3 = "migration_test_2_3"
        private const val DB_3_4 = "migration_test_3_4"
        private const val DB_4_5 = "migration_test_4_5"
        private const val DB_8_9 = "migration_test_8_9"
        private const val DB_9_10 = "migration_test_9_10"
        private const val DB_10_11 = "migration_test_10_11"
        private const val DB_11_12 = "migration_test_11_12"
    }
}
