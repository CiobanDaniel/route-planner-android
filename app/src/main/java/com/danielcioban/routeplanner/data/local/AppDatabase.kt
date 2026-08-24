package com.danielcioban.routeplanner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(
    entities = [RouteEntity::class, StopEntity::class, StopLibraryEntity::class, StopTaskEntity::class],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun stopLibraryDao(): StopLibraryDao
    abstract fun stopTaskDao(): StopTaskDao

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

        internal val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

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
