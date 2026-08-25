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
    /** Minutes from local midnight (0–1439). Null means no promised arrival. */
    val arriveByMinutes: Int? = null,
    /** Minutes to spend at this stop before driving to the next one. */
    val serviceMinutes: Int = 0,
    /** Meters. Null uses the route or app default radius. */
    val geofenceRadiusMeters: Int? = null,
    /** True after the courier has entered this stop's radius during delivery. */
    val isVisited: Boolean = false,
    val visitedAtEpochMs: Long? = null,
    /** GPS start of the route; not written to the library. Hidden from the stop list. */
    val isOrigin: Boolean = false,
    /** NOT_HOME, REFUSED, CLOSED, OTHER when the stop was failed rather than delivered. */
    val failureReason: String? = null,
    /** Absolute promised arrival. When set, takes priority over [arriveByMinutes]. */
    val arriveByEpochMs: Long? = null,
    /** Optimizer must not move this stop relative to its neighbors. */
    val isFixedOrder: Boolean = false,
    /** Lunch / break — not a delivery. */
    val isBreak: Boolean = false,
    val phone: String = "",
    val doorCode: String = "",
    val failurePhotoPath: String? = null,
    val failureSignaturePath: String? = null,
    /** Required tasks were skipped with a log note. */
    val tasksDeferred: Boolean = false,
    val tasksDeferredNote: String = "",
    /** Successful delivery photo (optional). */
    val podPhotoPath: String? = null,
    /** Successful delivery signature (optional). */
    val podSignaturePath: String? = null,
    /** When proof of delivery was captured, or when Done stamped a timestamp. */
    val podCapturedAtEpochMs: Long? = null,
    /** Cash on delivery amount. 0 means none. */
    val codAmount: Double = 0.0,
    val codCollected: Boolean = false,
    /** Optional barcode / QR payload that should match this stop. */
    val barcode: String = "",
)
