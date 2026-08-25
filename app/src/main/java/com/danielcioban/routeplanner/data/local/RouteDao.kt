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
    @Query("SELECT * FROM routes WHERE deletedAtEpochMs IS NULL ORDER BY updatedAtEpochMs DESC")
    fun observeRoutes(): Flow<List<RouteWithStops>>

    @Transaction
    @Query("SELECT * FROM routes WHERE deletedAtEpochMs IS NOT NULL ORDER BY deletedAtEpochMs DESC")
    fun observeTrashRoutes(): Flow<List<RouteWithStops>>

    @Transaction
    @Query("SELECT * FROM routes WHERE id = :routeId")
    fun observeRoute(routeId: Long): Flow<RouteWithStops?>

    @Transaction
    @Query("SELECT * FROM routes WHERE id = :routeId")
    suspend fun getRoute(routeId: Long): RouteWithStops?

    @Query("SELECT * FROM routes WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getRouteByRemoteId(remoteId: String): RouteEntity?

    @Query("SELECT * FROM stops WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getStopByRemoteId(remoteId: String): StopEntity?

    @Query("SELECT * FROM routes ORDER BY updatedAtEpochMs DESC")
    suspend fun getAllRoutes(): List<RouteEntity>

    @Query("SELECT * FROM stops WHERE routeId = :routeId ORDER BY position ASC")
    suspend fun getStopsForRoute(routeId: Long): List<StopEntity>

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

    @Query(
        """
        UPDATE stops SET isCompleted = 0, isVisited = 0, visitedAtEpochMs = NULL,
            failureReason = NULL, failurePhotoPath = NULL, failureSignaturePath = NULL,
            tasksDeferred = 0, tasksDeferredNote = '',
            podPhotoPath = NULL, podSignaturePath = NULL, podCapturedAtEpochMs = NULL,
            codCollected = 0
        WHERE routeId = :routeId
        """,
    )
    suspend fun resetStopCompletions(routeId: Long)

    @Query("SELECT * FROM stops WHERE routeId = :routeId AND isOrigin = 1")
    suspend fun getOriginStops(routeId: Long): List<StopEntity>

    @Query("UPDATE routes SET updatedAtEpochMs = :updatedAt WHERE id = :routeId")
    suspend fun touchRoute(routeId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query(
        """
        SELECT DISTINCT routes.* FROM routes
        INNER JOIN stops ON stops.routeId = routes.id
        WHERE stops.libraryStopId = :libraryStopId
          AND routes.deletedAtEpochMs IS NULL
        ORDER BY routes.name COLLATE NOCASE ASC
        """,
    )
    suspend fun getRoutesUsingLibraryStop(libraryStopId: Long): List<RouteEntity>

    @Query("SELECT * FROM stops WHERE libraryStopId = :libraryStopId")
    suspend fun getStopsWithLibraryId(libraryStopId: Long): List<StopEntity>

    @Query("UPDATE stops SET libraryStopId = :keepId WHERE libraryStopId = :dropId")
    suspend fun reassignLibraryStop(dropId: Long, keepId: Long)

    @Query(
        """
        UPDATE stops SET
            name = :name,
            addressHint = :addressHint,
            notes = :notes,
            latitude = :latitude,
            longitude = :longitude
        WHERE libraryStopId = :libraryStopId
        """,
    )
    suspend fun propagateLibraryPlace(
        libraryStopId: Long,
        name: String,
        addressHint: String,
        notes: String,
        latitude: Double,
        longitude: Double,
    )
}
