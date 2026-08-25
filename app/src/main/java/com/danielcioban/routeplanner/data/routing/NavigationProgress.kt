package com.danielcioban.routeplanner.data.routing

import com.danielcioban.routeplanner.ui.map.LatLng
import com.danielcioban.routeplanner.util.GeoUtils
import kotlin.math.max
import kotlin.math.roundToInt

data class NavGuidance(
    val currentStep: ManeuverStep?,
    val thenStep: ManeuverStep? = null,
    val distanceToManeuverMeters: Double,
    val remainingDistanceMeters: Double,
    val remainingDurationSeconds: Double,
    val distanceToRouteMeters: Double,
    val arrived: Boolean,
)

object NavigationProgress {
    const val offRouteThresholdMeters = 50.0

    fun evaluate(
        route: DrivingRoute,
        user: LatLng,
        destination: LatLng,
        arrivalRadiusMeters: Double = 40.0,
    ): NavGuidance {
        val toDest = GeoUtils.distanceMeters(
            user.latitude,
            user.longitude,
            destination.latitude,
            destination.longitude,
        )
        if (toDest <= arrivalRadiusMeters) {
            val arrive = route.steps.lastOrNull { it.type == "arrive" } ?: route.steps.lastOrNull()
            return NavGuidance(
                currentStep = arrive,
                thenStep = null,
                distanceToManeuverMeters = 0.0,
                remainingDistanceMeters = toDest,
                remainingDurationSeconds = 0.0,
                distanceToRouteMeters = 0.0,
                arrived = true,
            )
        }

        val nearest = nearestOnPolyline(route.coordinates, user)
        val remainingFromSnap = remainingPathDistance(route.coordinates, nearest.index, nearest.point)
        val remaining = max(remainingFromSnap, toDest * 0.15)

        val stepIndex = upcomingStepIndex(route, nearest)
        val step = route.steps.getOrNull(stepIndex)
        val then = route.steps.drop(stepIndex + 1).firstOrNull { isActionable(it) }
        val distanceToManeuver = if (step != null) {
            distanceAlongRouteTo(route.coordinates, nearest, step.location)
                .coerceAtLeast(
                    GeoUtils.distanceMeters(
                        user.latitude,
                        user.longitude,
                        step.location.latitude,
                        step.location.longitude,
                    ) * 0.5,
                )
        } else {
            remaining
        }

        val speedMps = if (route.durationSeconds > 0) {
            route.distanceMeters / route.durationSeconds
        } else {
            8.0
        }
        val etaSeconds = if (speedMps > 0.5) remaining / speedMps else remaining / 8.0

        return NavGuidance(
            currentStep = step,
            thenStep = then,
            distanceToManeuverMeters = distanceToManeuver,
            remainingDistanceMeters = remaining,
            remainingDurationSeconds = etaSeconds,
            distanceToRouteMeters = nearest.distanceMeters,
            arrived = false,
        )
    }

    fun formatEta(seconds: Double): String {
        val totalMin = (seconds / 60.0).roundToInt().coerceAtLeast(1)
        return if (totalMin < 60) {
            "$totalMin min"
        } else {
            val h = totalMin / 60
            val m = totalMin % 60
            if (m == 0) "${h}h" else "${h}h ${m}m"
        }
    }

    private fun isActionable(step: ManeuverStep): Boolean {
        if (step.type == "arrive") return true
        if (step.type == "new name" || step.type == "notification") return false
        if (step.type == "continue" && (step.modifier == null || step.modifier == "straight")) {
            return step.distanceMeters > 120
        }
        return true
    }

    private data class Nearest(
        val index: Int,
        val point: LatLng,
        val distanceMeters: Double,
        val traveledMeters: Double,
    )

    private fun nearestOnPolyline(coords: List<LatLng>, user: LatLng): Nearest {
        var bestDist = Double.MAX_VALUE
        var bestIndex = 0
        var bestPoint = coords.first()
        var traveledBefore = 0.0
        var bestTraveled = 0.0
        for (i in 0 until coords.lastIndex) {
            val a = coords[i]
            val b = coords[i + 1]
            val projected = projectOnSegment(user, a, b)
            val d = GeoUtils.distanceMeters(
                user.latitude,
                user.longitude,
                projected.latitude,
                projected.longitude,
            )
            val along = GeoUtils.distanceMeters(a.latitude, a.longitude, projected.latitude, projected.longitude)
            if (d < bestDist) {
                bestDist = d
                bestIndex = i
                bestPoint = projected
                bestTraveled = traveledBefore + along
            }
            traveledBefore += GeoUtils.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
        }
        return Nearest(bestIndex, bestPoint, bestDist, bestTraveled)
    }

    private fun projectOnSegment(p: LatLng, a: LatLng, b: LatLng): LatLng {
        val ax = a.longitude
        val ay = a.latitude
        val bx = b.longitude
        val by = b.latitude
        val px = p.longitude
        val py = p.latitude
        val dx = bx - ax
        val dy = by - ay
        if (dx == 0.0 && dy == 0.0) return a
        val t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)
        val clamped = t.coerceIn(0.0, 1.0)
        return LatLng(latitude = ay + clamped * dy, longitude = ax + clamped * dx)
    }

    private fun remainingPathDistance(
        coords: List<LatLng>,
        segmentIndex: Int,
        fromPoint: LatLng,
    ): Double {
        var sum = GeoUtils.distanceMeters(
            fromPoint.latitude,
            fromPoint.longitude,
            coords[segmentIndex + 1].latitude,
            coords[segmentIndex + 1].longitude,
        )
        for (i in (segmentIndex + 1) until coords.lastIndex) {
            sum += GeoUtils.distanceMeters(
                coords[i].latitude,
                coords[i].longitude,
                coords[i + 1].latitude,
                coords[i + 1].longitude,
            )
        }
        return sum
    }

    private fun upcomingStepIndex(route: DrivingRoute, nearest: Nearest): Int {
        val steps = route.steps
        if (steps.isEmpty()) return -1
        // Pick the next actionable maneuver whose point is still ahead on the path.
        var best = steps.lastIndex
        for (i in steps.indices) {
            val step = steps[i]
            if (!isActionable(step) && step.type != "arrive") continue
            val along = approximateDistanceAlong(route.coordinates, step.location)
            if (along >= nearest.traveledMeters - 15) {
                best = i
                break
            }
        }
        val current = steps.getOrNull(best) ?: return best
        val toCurrent = GeoUtils.distanceMeters(
            nearest.point.latitude,
            nearest.point.longitude,
            current.location.latitude,
            current.location.longitude,
        )
        return if (toCurrent < 20 && best < steps.lastIndex) {
            steps.drop(best + 1).indexOfFirst { isActionable(it) }
                .takeIf { it >= 0 }
                ?.let { best + 1 + it }
                ?: best
        } else {
            best
        }
    }

    private fun approximateDistanceAlong(coords: List<LatLng>, target: LatLng): Double {
        val nearest = nearestOnPolyline(coords, target)
        return nearest.traveledMeters
    }

    private fun distanceAlongRouteTo(
        coords: List<LatLng>,
        from: Nearest,
        target: LatLng,
    ): Double {
        val targetAlong = approximateDistanceAlong(coords, target)
        return max(0.0, targetAlong - from.traveledMeters)
    }
}
