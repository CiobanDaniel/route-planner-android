package com.danielcioban.routeplanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSearchDao {
    @Query("SELECT * FROM saved_searches ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<SavedSearchEntity>>

    @Query("SELECT * FROM saved_searches ORDER BY createdAtEpochMs DESC")
    suspend fun getAll(): List<SavedSearchEntity>

    @Query("SELECT * FROM saved_searches WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): SavedSearchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(search: SavedSearchEntity): Long

    @Query("DELETE FROM saved_searches WHERE id = :id")
    suspend fun delete(id: Long)
}
