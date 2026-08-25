package com.danielcioban.routeplanner.util

import java.util.IdentityHashMap

/**
 * Nearest-neighbor then 2-opt reorder for courier stop lists.
 * Unpinned items keep their relative order at the end.
 */
object RouteOrderOptimizer {
    fun <T> nearestNeighborOrder(
        items: List<T>,
        latitude: (T) -> Double?,
        longitude: (T) -> Double?,
        startLatitude: Double? = null,
        startLongitude: Double? = null,
        roundTrip: Boolean = false,
    ): List<T> {
        if (items.size <= 1) return items
        val pinned = mutableListOf<T>()
        val unpinned = mutableListOf<T>()
        for (item in items) {
            val lat = latitude(item)
            val lng = longitude(item)
            if (lat != null && lng != null) pinned += item else unpinned += item
        }
        if (pinned.size <= 1) return items

        val remaining = pinned.toMutableList()
        val ordered = ArrayList<T>(items.size)
        var currentLat: Double
        var currentLng: Double
        val originLat: Double?
        val originLng: Double?
        if (startLatitude != null && startLongitude != null) {
            currentLat = startLatitude
            currentLng = startLongitude
            originLat = startLatitude
            originLng = startLongitude
        } else {
            val first = remaining.removeAt(0)
            ordered += first
            currentLat = latitude(first)!!
            currentLng = longitude(first)!!
            originLat = currentLat
            originLng = currentLng
        }
        while (remaining.isNotEmpty()) {
            var bestIndex = 0
            var bestDistance = Double.POSITIVE_INFINITY
            remaining.forEachIndexed { index, item ->
                val distance = GeoUtils.distanceMeters(
                    currentLat,
                    currentLng,
                    latitude(item)!!,
                    longitude(item)!!,
                )
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestIndex = index
                }
            }
            val next = remaining.removeAt(bestIndex)
            ordered += next
            currentLat = latitude(next)!!
            currentLng = longitude(next)!!
        }
        val improved = twoOpt(
            ordered,
            latitude,
            longitude,
            originLat,
            originLng,
            roundTrip,
        )
        return improved + unpinned
    }

    /** Keep completed stops in place, then optimize the rest from [start] or last completed pin. */
    fun <T> optimizeRemaining(
        items: List<T>,
        isCompleted: (T) -> Boolean,
        latitude: (T) -> Double?,
        longitude: (T) -> Double?,
        startLatitude: Double? = null,
        startLongitude: Double? = null,
        roundTrip: Boolean = false,
        isFixedOrder: (T) -> Boolean = { false },
        arriveByEpochMs: (T) -> Long? = { null },
        arriveByMinutes: (T) -> Int? = { null },
        serviceMinutes: (T) -> Int = { 0 },
        nowEpochMs: Long = System.currentTimeMillis(),
    ): List<T> {
        val completed = items.filter(isCompleted)
        val remaining = items.filterNot(isCompleted)
        val originLat: Double?
        val originLng: Double?
        if (startLatitude != null && startLongitude != null) {
            originLat = startLatitude
            originLng = startLongitude
        } else {
            val lastPin = completed.lastOrNull { latitude(it) != null && longitude(it) != null }
            originLat = lastPin?.let(latitude)
            originLng = lastPin?.let(longitude)
        }
        val withAnchors = optimizeMovableSegments(
            remaining,
            latitude,
            longitude,
            originLat,
            originLng,
            isFixedOrder,
        )
        val withWindows = enforceTimeWindows(
            withAnchors,
            latitude,
            longitude,
            originLat,
            originLng,
            roundTrip,
            isFixedOrder,
            arriveByEpochMs,
            arriveByMinutes,
            serviceMinutes,
            nowEpochMs,
        )
        return completed + withWindows
    }

    /** Keep completed stops in place and reverse the remaining list. */
    fun <T> reverseRemaining(
        items: List<T>,
        isCompleted: (T) -> Boolean,
    ): List<T> {
        val remaining = items.filterNot(isCompleted)
        if (remaining.size <= 1) return items
        return items.filter(isCompleted) + remaining.reversed()
    }

    /**
     * Keep completed stops in place, then order remaining by promised arrival
     * (soonest first). Stops without a time keep their relative order at the end.
     */
    fun <T> sortRemainingByArriveBy(
        items: List<T>,
        isCompleted: (T) -> Boolean,
        arriveByMinutes: (T) -> Int?,
        arriveByEpochMs: (T) -> Long? = { null },
        nowEpochMs: Long = System.currentTimeMillis(),
        isFixedOrder: (T) -> Boolean = { false },
    ): List<T> {
        val remaining = items.filterNot(isCompleted)
        if (remaining.none { arriveByMinutes(it) != null || arriveByEpochMs(it) != null }) {
            return items
        }
        val sortedRemaining = sortMovableByKey(remaining, isFixedOrder) { item ->
            RouteEta.promisedDeadlineMs(
                arriveByEpochMs(item),
                arriveByMinutes(item),
                nowEpochMs,
            )
        }
        return items.filter(isCompleted) + sortedRemaining
    }

    /**
     * If the current order would arrive after a promised window, reorder movable
     * stops by deadline while keeping [isFixedOrder] anchors in place.
     */
    fun <T> enforceTimeWindows(
        remaining: List<T>,
        latitude: (T) -> Double?,
        longitude: (T) -> Double?,
        originLat: Double?,
        originLng: Double?,
        roundTrip: Boolean,
        isFixedOrder: (T) -> Boolean,
        arriveByEpochMs: (T) -> Long?,
        arriveByMinutes: (T) -> Int?,
        serviceMinutes: (T) -> Int,
        nowEpochMs: Long,
    ): List<T> {
        if (remaining.none { arriveByEpochMs(it) != null || arriveByMinutes(it) != null }) {
            return remaining
        }
        if (!hasLateStop(
                remaining,
                latitude,
                longitude,
                originLat,
                originLng,
                arriveByEpochMs,
                arriveByMinutes,
                serviceMinutes,
                nowEpochMs,
            )
        ) {
            return remaining
        }
        return sortMovableByKey(remaining, isFixedOrder) { item ->
            RouteEta.promisedDeadlineMs(
                arriveByEpochMs(item),
                arriveByMinutes(item),
                nowEpochMs,
            )
        }
    }

    private fun <T> optimizeMovableSegments(
        remaining: List<T>,
        latitude: (T) -> Double?,
        longitude: (T) -> Double?,
        originLat: Double?,
        originLng: Double?,
        isFixedOrder: (T) -> Boolean,
    ): List<T> {
        if (remaining.none(isFixedOrder)) {
            return nearestNeighborOrder(
                remaining,
                latitude,
                longitude,
                originLat,
                originLng,
                roundTrip = false,
            )
        }
        val result = ArrayList<T>(remaining.size)
        var fromLat = originLat
        var fromLng = originLng
        var index = 0
        while (index < remaining.size) {
            if (isFixedOrder(remaining[index])) {
                val fixed = remaining[index]
                result += fixed
                fromLat = latitude(fixed) ?: fromLat
                fromLng = longitude(fixed) ?: fromLng
                index++
                continue
            }
            val run = mutableListOf<T>()
            while (index < remaining.size && !isFixedOrder(remaining[index])) {
                run += remaining[index]
                index++
            }
            val orderedRun = nearestNeighborOrder(
                run,
                latitude,
                longitude,
                fromLat,
                fromLng,
                roundTrip = false,
            )
            result += orderedRun
            val lastPin = orderedRun.lastOrNull { latitude(it) != null && longitude(it) != null }
            if (lastPin != null) {
                fromLat = latitude(lastPin)
                fromLng = longitude(lastPin)
            }
        }
        return result
    }

    private fun <T> sortMovableByKey(
        remaining: List<T>,
        isFixedOrder: (T) -> Boolean,
        key: (T) -> Long?,
    ): List<T> {
        val movable = remaining.filterNot(isFixedOrder)
        val timed = movable.filter { key(it) != null }.sortedBy { key(it) }
        val untimed = movable.filter { key(it) == null }
        val queue = ArrayDeque(timed + untimed)
        return remaining.map { item ->
            if (isFixedOrder(item)) item else queue.removeFirst()
        }
    }

    private fun <T> hasLateStop(
        remaining: List<T>,
        latitude: (T) -> Double?,
        longitude: (T) -> Double?,
        originLat: Double?,
        originLng: Double?,
        arriveByEpochMs: (T) -> Long?,
        arriveByMinutes: (T) -> Int?,
        serviceMinutes: (T) -> Int,
        nowEpochMs: Long,
    ): Boolean {
        var cursorMs = nowEpochMs
        var prevLat = originLat
        var prevLng = originLng
        val speed = RouteEta.DEFAULT_SPEED_MPS
        for (item in remaining) {
            val lat = latitude(item)
            val lng = longitude(item)
            if (lat != null && lng != null) {
                val fromLat = prevLat
                val fromLng = prevLng
                if (fromLat != null && fromLng != null) {
                    val meters = GeoUtils.distanceMeters(fromLat, fromLng, lat, lng)
                    cursorMs += ((meters / speed) * 1000.0).toLong()
                }
                if (RouteEta.isArrivalLate(
                        cursorMs,
                        arriveByEpochMs(item),
                        arriveByMinutes(item),
                        nowEpochMs,
                    )
                ) {
                    return true
                }
                prevLat = lat
                prevLng = lng
            }
            cursorMs += java.util.concurrent.TimeUnit.MINUTES.toMillis(
                serviceMinutes(item).coerceAtLeast(0).toLong(),
            )
        }
        return false
    }

    /**
     * 2-opt using a pairwise cost (seconds or meters). [cost] is indexed in [items].
     * [originCostTo] / [costToOrigin] are used when the path starts/ends at an external origin.
     */
    fun <T> twoOptByCost(
        items: List<T>,
        cost: (fromIndex: Int, toIndex: Int) -> Double,
        originCostTo: ((Int) -> Double)? = null,
        costToOrigin: ((Int) -> Double)? = null,
        roundTrip: Boolean = false,
    ): List<T> {
        if (items.size < 3) return items
        val best = items.toMutableList()
        var improved = true
        var guard = 0
        while (improved && guard < 40) {
            improved = false
            guard++
            val n = best.size
            val currentLength = pathCost(best, items, cost, originCostTo, costToOrigin, roundTrip)
            loop@ for (i in 0 until n - 1) {
                for (j in i + 2 until n) {
                    best.subList(i, j + 1).reverse()
                    val nextLength = pathCost(best, items, cost, originCostTo, costToOrigin, roundTrip)
                    if (nextLength + 0.05 < currentLength) {
                        improved = true
                        break@loop
                    }
                    best.subList(i, j + 1).reverse()
                }
            }
        }
        return best
    }

    /**
     * Apply [twoOptByCost] when the duration matrix includes an optional origin at index 0.
     * Matrix size is n (no origin) or n+1 (origin + items).
     */
    fun <T> twoOptWithDurationMatrix(
        items: List<T>,
        durationSeconds: Array<DoubleArray>,
        hasOrigin: Boolean,
        roundTrip: Boolean,
    ): List<T> {
        val n = items.size
        val expected = if (hasOrigin) n + 1 else n
        if (durationSeconds.size != expected) return items
        val itemIndex: (Int) -> Int = { i -> if (hasOrigin) i + 1 else i }
        return twoOptByCost(
            items,
            cost = { from, to -> durationSeconds[itemIndex(from)][itemIndex(to)] },
            originCostTo = if (hasOrigin) {
                { i -> durationSeconds[0][itemIndex(i)] }
            } else {
                null
            },
            costToOrigin = if (hasOrigin) {
                { i -> durationSeconds[itemIndex(i)][0] }
            } else {
                null
            },
            roundTrip = roundTrip,
        )
    }

    private fun <T> pathCost(
        current: List<T>,
        original: List<T>,
        cost: (fromIndex: Int, toIndex: Int) -> Double,
        originCostTo: ((Int) -> Double)?,
        costToOrigin: ((Int) -> Double)?,
        roundTrip: Boolean,
    ): Double {
        if (current.isEmpty()) return 0.0
        val indexOf = IdentityHashMap<T, Int>(original.size)
        original.forEachIndexed { i, item -> indexOf[item] = i }
        fun idx(item: T): Int = indexOf[item] ?: error("item not in original")
        var sum = 0.0
        if (originCostTo != null) {
            sum += originCostTo(idx(current.first()))
        }
        for (i in 0 until current.lastIndex) {
            sum += cost(idx(current[i]), idx(current[i + 1]))
        }
        if (roundTrip) {
            sum += if (costToOrigin != null) {
                costToOrigin(idx(current.last()))
            } else {
                cost(idx(current.last()), idx(current.first()))
            }
        }
        return sum
    }

    private fun <T> twoOpt(
        items: List<T>,
        latitude: (T) -> Double?,
        longitude: (T) -> Double?,
        originLat: Double?,
        originLng: Double?,
        roundTrip: Boolean,
    ): List<T> {
        if (items.size < 3) return items
        val best = items.toMutableList()
        var improved = true
        var guard = 0
        while (improved && guard < 40) {
            improved = false
            guard++
            val n = best.size
            val currentLength = pathMeters(best, latitude, longitude, originLat, originLng, roundTrip)
            loop@ for (i in 0 until n - 1) {
                for (j in i + 2 until n) {
                    best.subList(i, j + 1).reverse()
                    val nextLength = pathMeters(best, latitude, longitude, originLat, originLng, roundTrip)
                    if (nextLength + 0.5 < currentLength) {
                        improved = true
                        break@loop
                    }
                    best.subList(i, j + 1).reverse()
                }
            }
        }
        return best
    }

    private fun <T> pathMeters(
        items: List<T>,
        latitude: (T) -> Double?,
        longitude: (T) -> Double?,
        originLat: Double?,
        originLng: Double?,
        roundTrip: Boolean,
    ): Double {
        if (items.isEmpty()) return 0.0
        var sum = 0.0
        var prevLat = originLat ?: latitude(items.first())!!
        var prevLng = originLng ?: longitude(items.first())!!
        val startIndex = if (originLat == null || originLng == null) 1 else 0
        for (index in startIndex until items.size) {
            val lat = latitude(items[index])!!
            val lng = longitude(items[index])!!
            sum += GeoUtils.distanceMeters(prevLat, prevLng, lat, lng)
            prevLat = lat
            prevLng = lng
        }
        if (roundTrip) {
            val destLat = originLat ?: latitude(items.first())!!
            val destLng = originLng ?: longitude(items.first())!!
            sum += GeoUtils.distanceMeters(prevLat, prevLng, destLat, destLng)
        }
        return sum
    }
}
