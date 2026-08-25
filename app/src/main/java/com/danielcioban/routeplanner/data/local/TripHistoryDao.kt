package com.danielcioban.routeplanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripHistoryDao {
    @Query("SELECT * FROM trip_history ORDER BY startedAtEpochMs DESC")
    fun observeAll(): Flow<List<TripHistoryEntity>>

    @Query("SELECT * FROM trip_history ORDER BY startedAtEpochMs DESC")
    suspend fun getAll(): List<TripHistoryEntity>

    @Query("SELECT * FROM trip_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TripHistoryEntity?

    @Query(
        """
        SELECT * FROM trip_history
        WHERE kind = 'ROUTE' AND routeId = :routeId AND status = 'IN_PROGRESS'
        ORDER BY startedAtEpochMs DESC
        LIMIT 1
        """,
    )
    suspend fun inProgressForRoute(routeId: Long): TripHistoryEntity?

    @Query("SELECT * FROM trip_history WHERE status = 'IN_PROGRESS'")
    suspend fun inProgressAll(): List<TripHistoryEntity>

    @Query("SELECT * FROM trip_history WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): TripHistoryEntity?

    @Insert
    suspend fun insert(trip: TripHistoryEntity): Long

    @Update
    suspend fun update(trip: TripHistoryEntity)

    @Query("DELETE FROM trip_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        """
        DELETE FROM trip_history
        WHERE startedAtEpochMs < :olderThanEpochMs
          AND status != 'IN_PROGRESS'
        """,
    )
    suspend fun deleteOlderThan(olderThanEpochMs: Long): Int
}
