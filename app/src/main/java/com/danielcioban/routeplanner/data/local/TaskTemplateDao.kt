package com.danielcioban.routeplanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskTemplateDao {
    @Query("SELECT * FROM task_templates ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<TaskTemplateEntity>>

    @Query("SELECT * FROM task_templates ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<TaskTemplateEntity>

    @Query("SELECT * FROM task_templates WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): TaskTemplateEntity?

    @Query("SELECT COUNT(*) FROM task_templates")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: TaskTemplateEntity): Long

    @Update
    suspend fun update(template: TaskTemplateEntity)

    @Query("DELETE FROM task_templates WHERE id = :id")
    suspend fun delete(id: Long)
}
