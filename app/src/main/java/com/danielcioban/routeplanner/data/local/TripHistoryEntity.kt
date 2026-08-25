package com.danielcioban.routeplanner.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.danielcioban.routeplanner.util.newRemoteId

@Entity(
    tableName = "trip_history",
    indices = [
        Index("startedAtEpochMs"),
        Index("status"),
        Index("routeId"),
        Index(value = ["remoteId"], unique = true),
    ],
)
data class TripHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String = newRemoteId(),
    /** ROUTE or QUICK */
    val kind: String,
    /** IN_PROGRESS, COMPLETED, or CANCELLED */
    val status: String,
    val title: String,
    val startedAtEpochMs: Long = System.currentTimeMillis(),
    val endedAtEpochMs: Long? = null,
    val routeId: Long? = null,
    val libraryStopId: Long? = null,
    val destLatitude: Double? = null,
    val destLongitude: Double? = null,
    val destName: String = "",
    val stopsCompleted: Int = 0,
    val stopsTotal: Int = 1,
    /** Straight-line completed path when the trip ended. */
    val distanceMeters: Double = 0.0,
    val lateStops: Int = 0,
)

object TripKind {
    const val ROUTE = "ROUTE"
    const val QUICK = "QUICK"
}

object TripStatus {
    const val IN_PROGRESS = "IN_PROGRESS"
    const val COMPLETED = "COMPLETED"
    const val CANCELLED = "CANCELLED"
}
