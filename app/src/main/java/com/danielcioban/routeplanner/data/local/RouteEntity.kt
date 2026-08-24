package com.danielcioban.routeplanner.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.danielcioban.routeplanner.util.newRemoteId

@Entity(
    tableName = "routes",
    indices = [Index(value = ["remoteId"], unique = true)],
)
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Stable id for backup import and future cloud sync. */
    val remoteId: String = newRemoteId(),
    val name: String,
    val notes: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    /** Non-null when soft-deleted for sync tombstones; local hard-delete still used today. */
    val deletedAtEpochMs: Long? = null,
)
