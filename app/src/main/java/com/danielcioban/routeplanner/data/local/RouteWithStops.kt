package com.danielcioban.routeplanner.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class RouteWithStops(
    @Embedded val route: RouteEntity,
    @Relation(parentColumn = "id", entityColumn = "routeId")
    val stops: List<StopEntity>,
) {
    val orderedStops: List<StopEntity>
        get() = stops.sortedBy { it.position }

    val completedCount: Int
        get() = stops.count { it.isCompleted }
}
