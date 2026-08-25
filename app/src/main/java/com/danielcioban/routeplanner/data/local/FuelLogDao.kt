package com.danielcioban.routeplanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelLogDao {
    @Query("SELECT * FROM fuel_log ORDER BY loggedAtEpochMs DESC")
    fun observeAll(): Flow<List<FuelLogEntity>>

    @Query("SELECT * FROM fuel_log ORDER BY loggedAtEpochMs DESC")
    suspend fun getAll(): List<FuelLogEntity>

    @Query("SELECT * FROM fuel_log WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): FuelLogEntity?

    @Insert
    suspend fun insert(entry: FuelLogEntity): Long

    @Update
    suspend fun update(entry: FuelLogEntity)

    @Query("DELETE FROM fuel_log WHERE id = :id")
    suspend fun delete(id: Long)
}
