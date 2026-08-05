package com.danielcioban.routeplanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StopLibraryDao {
    @Query("SELECT * FROM stop_library ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<StopLibraryEntity>>

    @Query("SELECT * FROM stop_library WHERE id = :id")
    suspend fun getById(id: Long): StopLibraryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stop: StopLibraryEntity): Long

    @Update
    suspend fun update(stop: StopLibraryEntity)

    @Query("DELETE FROM stop_library WHERE id = :id")
    suspend fun delete(id: Long)
}
