package com.danielcioban.routeplanner.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.danielcioban.routeplanner.util.newRemoteId

@Entity(
    tableName = "task_templates",
    indices = [Index(value = ["remoteId"], unique = true)],
)
data class TaskTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String = newRemoteId(),
    val title: String,
    val isRequired: Boolean = false,
    val sortOrder: Int = 0,
)
