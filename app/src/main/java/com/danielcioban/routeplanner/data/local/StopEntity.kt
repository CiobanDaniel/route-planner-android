package com.danielcioban.routeplanner.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.danielcioban.routeplanner.util.newRemoteId

@Entity(
    tableName = "stops",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routeId"), Index("libraryStopId"), Index(value = ["remoteId"], unique = true)],
)
data class StopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String = newRemoteId(),
    val routeId: Long,
    val position: Int,
    val name: String,
    val addressHint: String = "",
    val notes: String = "",
    /** WGS84 latitude. Null until the user sets coordinates (map or manual). */
    val latitude: Double? = null,
    /** WGS84 longitude. Null until the user sets coordinates. */
    val longitude: Double? = null,
    val isCompleted: Boolean = false,
    /** Reference to the canonical [StopLibraryEntity]. Null only for unpinned drafts. */
    val libraryStopId: Long? = null,
    val deletedAtEpochMs: Long? = null,
)
