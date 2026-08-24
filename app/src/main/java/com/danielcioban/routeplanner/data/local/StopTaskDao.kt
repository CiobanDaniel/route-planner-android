package com.danielcioban.routeplanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StopTaskDao {
    @Query("SELECT * FROM stop_tasks WHERE stopId = :stopId ORDER BY id ASC")
    fun observeForStop(stopId: Long): Flow<List<StopTaskEntity>>

    @Query("SELECT * FROM stop_tasks WHERE id = :taskId")
    suspend fun getById(taskId: Long): StopTaskEntity?

    @Query("SELECT * FROM stop_tasks WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): StopTaskEntity?

    @Query("SELECT * FROM stop_tasks WHERE stopId = :stopId ORDER BY id ASC")
    suspend fun getForStop(stopId: Long): List<StopTaskEntity>

    @Query("SELECT * FROM stop_tasks ORDER BY id ASC")
    suspend fun getAll(): List<StopTaskEntity>

    @Insert
    suspend fun insert(task: StopTaskEntity): Long

    @Update
    suspend fun update(task: StopTaskEntity)

    @Query("DELETE FROM stop_tasks WHERE id = :taskId")
    suspend fun delete(taskId: Long)

    @Query(
        "UPDATE stop_tasks SET isCompleted = 0, completedAtEpochMs = NULL, completionNote = '' " +
            "WHERE stopId IN (SELECT id FROM stops WHERE routeId = :routeId)",
    )
    suspend fun resetCompletionsForRoute(routeId: Long)

    @Query(
        "SELECT COUNT(*) FROM stop_tasks " +
            "WHERE stopId = :stopId AND isRequired = 1 AND isCompleted = 0",
    )
    suspend fun incompleteRequiredCount(stopId: Long): Int

    @Query(
        """
        SELECT stopId AS stopId,
               COUNT(*) AS total,
               SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) AS completed,
               SUM(CASE WHEN isRequired = 1 AND isCompleted = 0 THEN 1 ELSE 0 END) AS requiredRemaining
        FROM stop_tasks
        WHERE stopId IN (SELECT id FROM stops WHERE routeId = :routeId)
        GROUP BY stopId
        """,
    )
    fun observeProgressForRoute(routeId: Long): Flow<List<StopTaskProgress>>
}

data class StopTaskProgress(
    val stopId: Long,
    val total: Int,
    val completed: Int,
    val requiredRemaining: Int,
)
