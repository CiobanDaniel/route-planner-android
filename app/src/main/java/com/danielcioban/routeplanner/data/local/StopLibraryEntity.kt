package com.danielcioban.routeplanner.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Device-local reusable stop (not owned by a route).
 * Copy-on-add into [StopEntity] for deliveries; [libraryStopId] can link back later.
 * Sync / sharing comes after accounts — this is the local groundwork.
 */
@Entity(
    tableName = "stop_library",
    indices = [Index("name"), Index("updatedAtEpochMs")],
)
data class StopLibraryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val addressHint: String = "",
    val notes: String = "",
    val latitude: Double,
    val longitude: Double,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
)
