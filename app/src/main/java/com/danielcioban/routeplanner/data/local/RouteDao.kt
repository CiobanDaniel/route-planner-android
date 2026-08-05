package com.danielcioban.routeplanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Transaction
    @Query("SELECT * FROM routes ORDER BY updatedAtEpochMs DESC")
    fun observeRoutes(): Flow<List<RouteWithStops>>

    @Transaction
    @Query("SELECT * FROM routes WHERE id = :routeId")
    fun observeRoute(routeId: Long): Flow<RouteWithStops?>

    @Transaction
    @Query("SELECT * FROM routes WHERE id = :routeId")
    suspend fun getRoute(routeId: Long): RouteWithStops?

    @Insert
    suspend fun insertRoute(route: RouteEntity): Long

    @Update
    suspend fun updateRoute(route: RouteEntity)

    @Query("DELETE FROM routes WHERE id = :routeId")
    suspend fun deleteRoute(routeId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<StopEntity>)

    @Insert
    suspend fun insertStop(stop: StopEntity): Long

    @Update
    suspend fun updateStop(stop: StopEntity)

    @Query("SELECT * FROM stops WHERE id = :stopId")
    suspend fun getStop(stopId: Long): StopEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM stops WHERE routeId = :routeId")
    suspend fun maxStopPosition(routeId: Long): Int

    @Query("DELETE FROM stops WHERE routeId = :routeId")
    suspend fun deleteStopsForRoute(routeId: Long)

    @Query("DELETE FROM stops WHERE id = :stopId")
    suspend fun deleteStop(stopId: Long)

    @Query("UPDATE stops SET isCompleted = :completed WHERE id = :stopId")
    suspend fun setStopCompleted(stopId: Long, completed: Boolean)

    @Query("UPDATE stops SET isCompleted = 0 WHERE routeId = :routeId")
    suspend fun resetStopCompletions(routeId: Long)

    @Query("UPDATE routes SET updatedAtEpochMs = :updatedAt WHERE id = :routeId")
    suspend fun touchRoute(routeId: Long, updatedAt: Long = System.currentTimeMillis())
}
