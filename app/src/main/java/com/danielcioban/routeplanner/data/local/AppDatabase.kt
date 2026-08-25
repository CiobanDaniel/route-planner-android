package com.danielcioban.routeplanner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(
    entities = [
        RouteEntity::class,
        StopEntity::class,
        StopLibraryEntity::class,
        StopTaskEntity::class,
        TripHistoryEntity::class,
        TaskTemplateEntity::class,
        LibraryDefaultTaskEntity::class,
        SavedSearchEntity::class,
        FuelLogEntity::class,
    ],
    version = 12,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun stopLibraryDao(): StopLibraryDao
    abstract fun stopTaskDao(): StopTaskDao
    abstract fun tripHistoryDao(): TripHistoryDao
    abstract fun taskTemplateDao(): TaskTemplateDao
    abstract fun libraryDefaultTaskDao(): LibraryDefaultTaskDao
    abstract fun savedSearchDao(): SavedSearchDao
    abstract fun fuelLogDao(): FuelLogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS stop_library (
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
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stop_library_name ON stop_library (name)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_stop_library_updatedAtEpochMs ON stop_library (updatedAtEpochMs)",
                )
                db.execSQL("ALTER TABLE stops ADD COLUMN libraryStopId INTEGER")
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS stop_tasks (
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
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stop_tasks_stopId ON stop_tasks (stopId)")
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("routes", "stops", "stop_library", "stop_tasks").forEach { table ->
                    db.execSQL("ALTER TABLE $table ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE $table ADD COLUMN deletedAtEpochMs INTEGER")
                    backfillRemoteIds(db, table)
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_${table}_remoteId ON $table (remoteId)",
                    )
                }
            }
        }

        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_stops_libraryStopId ON stops (libraryStopId)",
                )
                backfillLibraryReferences(db)
            }
        }

        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE routes ADD COLUMN roundTrip INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL("ALTER TABLE stops ADD COLUMN arriveByMinutes INTEGER")
                db.execSQL(
                    "ALTER TABLE stops ADD COLUMN serviceMinutes INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        internal val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE routes ADD COLUMN geofenceMode TEXT NOT NULL DEFAULT 'INHERIT'",
                )
                db.execSQL("ALTER TABLE routes ADD COLUMN geofenceRadiusMeters INTEGER")
                db.execSQL("ALTER TABLE stops ADD COLUMN geofenceRadiusMeters INTEGER")
                db.execSQL(
                    "ALTER TABLE stops ADD COLUMN isVisited INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL("ALTER TABLE stops ADD COLUMN visitedAtEpochMs INTEGER")
            }
        }

        internal val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trip_history (
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
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_trip_history_startedAtEpochMs ON trip_history (startedAtEpochMs)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_trip_history_status ON trip_history (status)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_trip_history_routeId ON trip_history (routeId)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_trip_history_remoteId ON trip_history (remoteId)",
                )
            }
        }

        internal val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE stops ADD COLUMN isOrigin INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL("ALTER TABLE stops ADD COLUMN failureReason TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        remoteId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        isRequired INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_task_templates_remoteId ON task_templates (remoteId)",
                )
            }
        }

        internal val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routes ADD COLUMN originLatitude REAL")
                db.execSQL("ALTER TABLE routes ADD COLUMN originLongitude REAL")
                db.execSQL("ALTER TABLE routes ADD COLUMN colorHex TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE routes ADD COLUMN vanName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE routes ADD COLUMN shiftName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE routes ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE stops ADD COLUMN arriveByEpochMs INTEGER")
                db.execSQL(
                    "ALTER TABLE stops ADD COLUMN isFixedOrder INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL("ALTER TABLE stops ADD COLUMN isBreak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE stops ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE stops ADD COLUMN doorCode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE stops ADD COLUMN failurePhotoPath TEXT")
                db.execSQL("ALTER TABLE stops ADD COLUMN failureSignaturePath TEXT")
                db.execSQL(
                    "ALTER TABLE stops ADD COLUMN tasksDeferred INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE stops ADD COLUMN tasksDeferredNote TEXT NOT NULL DEFAULT ''",
                )
                db.query(
                    """
                    SELECT routeId, latitude, longitude FROM stops
                    WHERE isOrigin = 1 AND latitude IS NOT NULL AND longitude IS NOT NULL
                    ORDER BY position ASC
                    """.trimIndent(),
                ).use { cursor ->
                    val routeIdx = cursor.getColumnIndexOrThrow("routeId")
                    val latIdx = cursor.getColumnIndexOrThrow("latitude")
                    val lngIdx = cursor.getColumnIndexOrThrow("longitude")
                    val seen = mutableSetOf<Long>()
                    while (cursor.moveToNext()) {
                        val routeId = cursor.getLong(routeIdx)
                        if (!seen.add(routeId)) continue
                        db.execSQL(
                            """
                            UPDATE routes SET originLatitude = ?, originLongitude = ?
                            WHERE id = ? AND originLatitude IS NULL
                            """.trimIndent(),
                            arrayOf<Any>(
                                cursor.getDouble(latIdx),
                                cursor.getDouble(lngIdx),
                                routeId,
                            ),
                        )
                    }
                }
                db.execSQL("DELETE FROM stops WHERE isOrigin = 1")
            }
        }

        internal val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stops ADD COLUMN podPhotoPath TEXT")
                db.execSQL("ALTER TABLE stops ADD COLUMN podSignaturePath TEXT")
                db.execSQL("ALTER TABLE stops ADD COLUMN podCapturedAtEpochMs INTEGER")
                db.execSQL("ALTER TABLE stops ADD COLUMN codAmount REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE stops ADD COLUMN codCollected INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE stops ADD COLUMN barcode TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "ALTER TABLE routes ADD COLUMN shiftSlot TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "ALTER TABLE stop_library ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE trip_history ADD COLUMN distanceMeters REAL NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE trip_history ADD COLUMN lateStops INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS fuel_log (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        remoteId TEXT NOT NULL,
                        loggedAtEpochMs INTEGER NOT NULL,
                        odometerKm REAL,
                        liters REAL,
                        amount REAL,
                        notes TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_fuel_log_loggedAtEpochMs ON fuel_log (loggedAtEpochMs)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_fuel_log_remoteId ON fuel_log (remoteId)",
                )
            }
        }

        internal val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stop_library ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE stop_library ADD COLUMN plusCode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE stop_library ADD COLUMN what3words TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "ALTER TABLE stop_library ADD COLUMN lastUsedAtEpochMs INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL("ALTER TABLE stop_library ADD COLUMN useCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE stop_library ADD COLUMN defaultGeofenceRadiusMeters INTEGER")
                db.execSQL(
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
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_library_default_tasks_libraryStopId ON library_default_tasks (libraryStopId)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_library_default_tasks_remoteId ON library_default_tasks (remoteId)",
                )
                db.execSQL(
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
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_saved_searches_remoteId ON saved_searches (remoteId)",
                )
            }
        }

        internal val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
        )

        private fun backfillLibraryReferences(db: SupportSQLiteDatabase) {
            db.query(
                """
                SELECT id, name, addressHint, notes, latitude, longitude
                FROM stops
                WHERE latitude IS NOT NULL AND longitude IS NOT NULL
                  AND (
                    libraryStopId IS NULL
                    OR NOT EXISTS (
                      SELECT 1 FROM stop_library WHERE stop_library.id = stops.libraryStopId
                    )
                  )
                """.trimIndent(),
            ).use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow("id")
                val nameIdx = cursor.getColumnIndexOrThrow("name")
                val hintIdx = cursor.getColumnIndexOrThrow("addressHint")
                val notesIdx = cursor.getColumnIndexOrThrow("notes")
                val latIdx = cursor.getColumnIndexOrThrow("latitude")
                val lngIdx = cursor.getColumnIndexOrThrow("longitude")
                while (cursor.moveToNext()) {
                    val stopId = cursor.getLong(idIdx)
                    val now = System.currentTimeMillis()
                    db.execSQL(
                        """
                        INSERT INTO stop_library (
                            remoteId, name, addressHint, notes, latitude, longitude,
                            createdAtEpochMs, updatedAtEpochMs, deletedAtEpochMs
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL)
                        """.trimIndent(),
                        arrayOf(
                            UUID.randomUUID().toString(),
                            cursor.getString(nameIdx),
                            cursor.getString(hintIdx),
                            cursor.getString(notesIdx),
                            cursor.getDouble(latIdx),
                            cursor.getDouble(lngIdx),
                            now,
                            now,
                        ),
                    )
                    var libraryId = 0L
                    db.query("SELECT last_insert_rowid()").use { idCursor ->
                        if (idCursor.moveToFirst()) libraryId = idCursor.getLong(0)
                    }
                    if (libraryId > 0L) {
                        db.execSQL(
                            "UPDATE stops SET libraryStopId = ? WHERE id = ?",
                            arrayOf(libraryId, stopId),
                        )
                    }
                }
            }
        }

        private fun backfillRemoteIds(db: SupportSQLiteDatabase, table: String) {
            db.query("SELECT id FROM $table WHERE remoteId = ''").use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    db.execSQL(
                        "UPDATE $table SET remoteId = ? WHERE id = ?",
                        arrayOf<Any>(UUID.randomUUID().toString(), id),
                    )
                }
            }
        }

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "route_planner.db",
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
