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
    /** After the last stop, plan / navigate back to the first pinned stop. */
    val roundTrip: Boolean = false,
    /** [com.danielcioban.routeplanner.data.settings.RouteGeofenceMode] name. */
    val geofenceMode: String = "INHERIT",
    /** Meters. Null uses the app default radius. */
    val geofenceRadiusMeters: Int? = null,
    /** GPS start of the route — not a delivery stop and not a library pin. */
    val originLatitude: Double? = null,
    val originLongitude: Double? = null,
    /** Display stripe, e.g. #E86A17. */
    val colorHex: String = "",
    val vanName: String = "",
    val shiftName: String = "",
    val archived: Boolean = false,
    /** [ShiftSlot] MORNING / AFTERNOON, or empty. */
    val shiftSlot: String = ShiftSlot.UNSET,
)
