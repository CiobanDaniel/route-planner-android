package com.danielcioban.routeplanner.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.danielcioban.routeplanner.util.newRemoteId

/** A local operational task that must be carried out at a route stop. */
@Entity(
    tableName = "stop_tasks",
    foreignKeys = [
        ForeignKey(
            entity = StopEntity::class,
            parentColumns = ["id"],
            childColumns = ["stopId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("stopId"), Index(value = ["remoteId"], unique = true)],
)
data class StopTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String = newRemoteId(),
    val stopId: Long,
    val title: String,
    val isRequired: Boolean = false,
    val isCompleted: Boolean = false,
    val completedAtEpochMs: Long? = null,
    val completionNote: String = "",
    val deletedAtEpochMs: Long? = null,
)
