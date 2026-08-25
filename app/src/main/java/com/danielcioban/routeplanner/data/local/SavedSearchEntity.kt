package com.danielcioban.routeplanner.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.danielcioban.routeplanner.util.newRemoteId

@Entity(
    tableName = "saved_searches",
    indices = [Index(value = ["remoteId"], unique = true)],
)
data class SavedSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String = newRemoteId(),
    val query: String,
    val nearMeOnly: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)
