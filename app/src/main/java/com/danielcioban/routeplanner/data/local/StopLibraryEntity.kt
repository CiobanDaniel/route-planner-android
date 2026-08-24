package com.danielcioban.routeplanner.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.danielcioban.routeplanner.util.newRemoteId

/**
 * Canonical stop. Routes reference this via [StopEntity.libraryStopId];
 * per-route state (order, completion, tasks) stays on the route stop.
 */
@Entity(
    tableName = "stop_library",
    indices = [Index("name"), Index("updatedAtEpochMs"), Index(value = ["remoteId"], unique = true)],
)
data class StopLibraryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String = newRemoteId(),
    val name: String,
    val addressHint: String = "",
    val notes: String = "",
    val latitude: Double,
    val longitude: Double,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val deletedAtEpochMs: Long? = null,
)
