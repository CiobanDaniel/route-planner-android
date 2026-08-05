package com.danielcioban.routeplanner.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    indices = [Index("routeId")],
)
data class StopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    /** Optional link to [StopLibraryEntity] (copy-on-add; no hard FK yet). */
    val libraryStopId: Long? = null,
)
