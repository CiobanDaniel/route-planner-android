package com.danielcioban.routeplanner.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.danielcioban.routeplanner.util.newRemoteId

@Entity(
    tableName = "library_default_tasks",
    foreignKeys = [
        ForeignKey(
            entity = StopLibraryEntity::class,
            parentColumns = ["id"],
            childColumns = ["libraryStopId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("libraryStopId"), Index(value = ["remoteId"], unique = true)],
)
data class LibraryDefaultTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String = newRemoteId(),
    val libraryStopId: Long,
    val title: String,
    val isRequired: Boolean = false,
    val sortOrder: Int = 0,
)
