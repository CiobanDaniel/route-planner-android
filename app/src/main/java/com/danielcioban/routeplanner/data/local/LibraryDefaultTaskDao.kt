package com.danielcioban.routeplanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDefaultTaskDao {
    @Query(
        "SELECT * FROM library_default_tasks WHERE libraryStopId = :libraryStopId ORDER BY sortOrder ASC, id ASC",
    )
    fun observeForLibraryStop(libraryStopId: Long): Flow<List<LibraryDefaultTaskEntity>>

    @Query(
        "SELECT * FROM library_default_tasks WHERE libraryStopId = :libraryStopId ORDER BY sortOrder ASC, id ASC",
    )
    suspend fun getForLibraryStop(libraryStopId: Long): List<LibraryDefaultTaskEntity>

    @Query("SELECT * FROM library_default_tasks ORDER BY id ASC")
    suspend fun getAll(): List<LibraryDefaultTaskEntity>

    @Query("SELECT * FROM library_default_tasks WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): LibraryDefaultTaskEntity?

    @Insert
    suspend fun insert(task: LibraryDefaultTaskEntity): Long

    @Update
    suspend fun update(task: LibraryDefaultTaskEntity)

    @Query("DELETE FROM library_default_tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM library_default_tasks WHERE libraryStopId = :libraryStopId")
    suspend fun deleteForLibraryStop(libraryStopId: Long)
}
