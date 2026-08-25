package com.danielcioban.routeplanner.data.routing

import com.danielcioban.routeplanner.data.EndpointProbe
import com.danielcioban.routeplanner.data.ServiceEndpoints
import com.danielcioban.routeplanner.ui.map.LatLng
import com.danielcioban.routeplanner.util.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class OsrmRoutingClient(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = ServiceEndpoints.osrmBaseUrl,
    private val fallbackUrl: String = ServiceEndpoints.osrmFallbackUrl,
    private val cache: RoutePolylineCache = RoutePolylineCache.Shared,
) {
    private val bases: List<String>
        get() = ServiceEndpoints.candidates(baseUrl, fallbackUrl).ifEmpty { listOf(baseUrl) }

    private val probeClient: OkHttpClient by lazy {
        client.newBuilder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    suspend fun routeDriving(
        from: LatLng,
        to: LatLng,
        profile: String = "driving",
        exclude: String? = null,
    ): Result<DrivingRoute> = withContext(Dispatchers.IO) {
        val cacheKey = RoutePolylineCache.key(from, to, profile, exclude)
        try {
            val route = firstSuccessful { base ->
                requestRoute(base, from, to, profile, exclude)
            }
            cache.put(cacheKey, route)
            Result.success(route)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            cache.get(cacheKey)?.let { Result.success(it) } ?: Result.failure(e)
        }
    }

    /**
     * Vehicle (or bike) path, then an optional foot leg from the snapped curb to the pin.
     */
    suspend fun routeForNavigation(
        from: LatLng,
        to: LatLng,
        profile: String = "driving",
        exclude: String? = null,
        walkLastMile: Boolean = false,
    ): Result<DrivingRoute> {
        val main = routeDriving(from, to, profile = profile, exclude = exclude)
        val route = main.getOrElse { return main }
        if (!walkLastMile || profile == "walking" || route.isApproximate || route.isCached) {
            return main
        }
        val snap = route.coordinates.lastOrNull() ?: return main
        val gap = GeoUtils.distanceMeters(
            snap.latitude,
            snap.longitude,
            to.latitude,
            to.longitude,
        )
        if (gap < LAST_MILE_WALK_METERS) return main
        val foot = routeDriving(snap, to, profile = "walking").getOrNull() ?: return main
        return Result.success(route.appendFootLeg(foot))
    }

    /**
     * OSRM Table API durations in seconds. Index order matches [points].
     * Null cells become +Infinity. Caps at [MAX_TABLE_POINTS] coordinates.
     */
    suspend fun tableDurations(
        points: List<LatLng>,
        profile: String = "driving",
        exclude: String? = null,
    ): Result<Array<DoubleArray>> = withContext(Dispatchers.IO) {
        runCatching {
            require(points.size >= 2) { "Need at least two points" }
            require(points.size <= MAX_TABLE_POINTS) { "Too many points for table" }
            val coords = points.joinToString(";") { "${it.longitude},${it.latitude}" }
            firstSuccessful { base ->
                val url = buildUrl(
                    base = base,
                    kind = "table",
                    profile = profile,
                    coords = coords,
                    query = "annotations=duration",
                    exclude = exclude,
                )
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("Table failed (${response.code})")
                    }
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) error("Empty table response")
                    parseTableDurations(JSONObject(body), points.size)
                }
            }
        }
    }

    fun probe(): EndpointProbe {
        val list = bases
        list.forEachIndexed { index, base ->
            if (ping(base)) return EndpointProbe(ok = true, usedFallback = index > 0, host = base)
        }
        return EndpointProbe(ok = false, usedFallback = false, host = list.firstOrNull().orEmpty())
    }

    private fun ping(base: String): Boolean {
        return runCatching {
            val url = buildUrl(
                base = base,
                kind = "nearest",
                profile = "driving",
                coords = "0,0",
                query = "number=1",
                exclude = null,
            )
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .get()
                .build()
            probeClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                response.isSuccessful && body.contains("\"code\"")
            }
        }.getOrDefault(false)
    }

    private fun requestRoute(
        base: String,
        from: LatLng,
        to: LatLng,
        profile: String,
        exclude: String?,
    ): DrivingRoute {
        val url = buildUrl(
            base = base,
            kind = "route",
            profile = profile,
            coords = "${from.longitude},${from.latitude};${to.longitude},${to.latitude}",
            query = "overview=full&geometries=geojson&steps=true&annotations=false",
            exclude = exclude,
        )
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    when (response.code) {
                        429, 503 -> error(RoutingFailure.RATE_LIMIT)
                        else -> error("Routing failed (${response.code})")
                    }
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) error("Empty routing response")
                return parseRoute(JSONObject(body))
            }
        } catch (e: SocketTimeoutException) {
            error(RoutingFailure.TIMEOUT)
        } catch (e: InterruptedIOException) {
            error(RoutingFailure.TIMEOUT)
        }
    }

    private fun <T> firstSuccessful(block: (String) -> T): T {
        var last: Exception? = null
        var lastHost = ""
        for (base in bases) {
            try {
                val value = block(base)
                LastRoutingErrorStore.clear()
                return value
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                last = e
                lastHost = base
            }
        }
        LastRoutingErrorStore.record(
            summary = last?.message ?: last?.javaClass?.simpleName ?: "routing_failed",
            host = lastHost,
        )
        throw last ?: IllegalStateException("Routing failed")
    }

    private fun buildUrl(
        base: String,
        kind: String,
        profile: String,
        coords: String,
        query: String,
        exclude: String?,
    ): String {
        val safeProfile = profile.ifBlank { "driving" }
        val excludePart = exclude?.takeIf { it.isNotBlank() }?.let { "&exclude=$it" }.orEmpty()
        return "$base/$kind/v1/$safeProfile/$coords?$query$excludePart"
    }

    private fun parseTableDurations(json: JSONObject, expectedSize: Int): Array<DoubleArray> {
        val code = json.optString("code")
        if (code != "Ok") {
            error(json.optString("message").ifBlank { "Table failed ($code)" })
        }
        val durations = json.getJSONArray("durations")
        if (durations.length() != expectedSize) {
            error("Table size mismatch")
        }
        return Array(expectedSize) { row ->
            val cells = durations.getJSONArray(row)
            DoubleArray(expectedSize) { col ->
                if (col >= cells.length() || cells.isNull(col)) {
                    Double.POSITIVE_INFINITY
                } else {
                    cells.optDouble(col, Double.POSITIVE_INFINITY)
                }
            }
        }
    }

    private fun parseRoute(json: JSONObject): DrivingRoute {
        val code = json.optString("code")
        if (code != "Ok") {
            error(json.optString("message").ifBlank { "No route found ($code)" })
        }
        val routes = json.getJSONArray("routes")
        if (routes.length() == 0) error("No route found")
        val route = routes.getJSONObject(0)
        val geometry = route.getJSONObject("geometry")
        val coordsJson = geometry.getJSONArray("coordinates")
        val coordinates = buildList {
            for (i in 0 until coordsJson.length()) {
                val pair = coordsJson.getJSONArray(i)
                add(LatLng(latitude = pair.getDouble(1), longitude = pair.getDouble(0)))
            }
        }
        if (coordinates.size < 2) error("Route geometry too short")

        val steps = mutableListOf<ManeuverStep>()
        val legs = route.getJSONArray("legs")
        for (li in 0 until legs.length()) {
            val leg = legs.getJSONObject(li)
            val stepArr = leg.getJSONArray("steps")
            for (si in 0 until stepArr.length()) {
                val step = stepArr.getJSONObject(si)
                val maneuver = step.getJSONObject("maneuver")
                val loc = maneuver.getJSONArray("location")
                val type = maneuver.optString("type", "turn")
                val modifier = maneuver.optString("modifier").ifBlank { null }
                val name = step.optString("name").orEmpty()
                val ref = step.optString("ref").orEmpty()
                val road = when {
                    name.isNotBlank() && ref.isNotBlank() -> "$name ($ref)"
                    name.isNotBlank() -> name
                    ref.isNotBlank() -> ref
                    else -> ""
                }
                steps += ManeuverStep(
                    // Display uses [ManeuverFormatter] with type/modifier/name — not this field.
                    instruction = "",
                    type = type,
                    modifier = modifier,
                    name = road,
                    distanceMeters = step.optDouble("distance", 0.0),
                    durationSeconds = step.optDouble("duration", 0.0),
                    location = LatLng(latitude = loc.getDouble(1), longitude = loc.getDouble(0)),
                )
            }
        }

        return DrivingRoute(
            coordinates = coordinates,
            distanceMeters = route.optDouble("distance", 0.0),
            durationSeconds = route.optDouble("duration", 0.0),
            steps = steps,
        )
    }

    companion object {
        const val MAX_TABLE_POINTS = 21
        const val LAST_MILE_WALK_METERS = 25.0

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
