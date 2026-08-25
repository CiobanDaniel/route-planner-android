package com.danielcioban.routeplanner.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.danielcioban.routeplanner.util.newRemoteId

@Entity(
    tableName = "fuel_log",
    indices = [
        Index("loggedAtEpochMs"),
        Index(value = ["remoteId"], unique = true),
    ],
)
data class FuelLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String = newRemoteId(),
    val loggedAtEpochMs: Long = System.currentTimeMillis(),
    /** Odometer reading in kilometres. Null when unknown. */
    val odometerKm: Double? = null,
    val liters: Double? = null,
    /** Optional amount paid, in the courier's local currency (no FX). */
    val amount: Double? = null,
    val notes: String = "",
)
