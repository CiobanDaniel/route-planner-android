package com.danielcioban.routeplanner.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
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
    private val dbName = "migration_test_2_3"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate2To3_createsStopTasksTable() {
        createVersion2Database()

        val room = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()
        room.openHelper.writableDatabase.use { migrated ->
            migrated.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='stop_tasks'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
        }
        room.close()
    }

    @Test
    fun migrate3To4_addsRemoteIdColumns() {
        createVersion3Database()

        val room = Room.databaseBuilder(context, AppDatabase::class.java, "migration_test_3_4")
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()
        room.openHelper.writableDatabase.use { migrated ->
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
        room.close()
        context.deleteDatabase("migration_test_3_4")
    }

    @Test
    fun migrate4To5_backfillsLibraryReferences() {
        createVersion4Database()

        val room = Room.databaseBuilder(context, AppDatabase::class.java, "migration_test_4_5")
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()
        room.openHelper.writableDatabase.use { migrated ->
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
        room.close()
        context.deleteDatabase("migration_test_4_5")
    }

    private fun createVersion3Database() {
        context.deleteDatabase("migration_test_3_4")
        val sqlite = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(
            context.getDatabasePath("migration_test_3_4").path,
            null,
        )
        sqlite.version = 3
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

    private fun createVersion2Database() {
        val sqlite = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(
            context.getDatabasePath(dbName).path,
            null,
        )
        sqlite.version = 2
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
        sqlite.execSQL("CREATE INDEX index_stop_library_name ON stop_library (name)")
        sqlite.execSQL(
            "CREATE INDEX index_stop_library_updatedAtEpochMs ON stop_library (updatedAtEpochMs)",
        )
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

    private fun createVersion4Database() {
        context.deleteDatabase("migration_test_4_5")
        val sqlite = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(
            context.getDatabasePath("migration_test_4_5").path,
            null,
        )
        sqlite.version = 4
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
}
