package com.danielcioban.routeplanner.data.routing

import com.danielcioban.routeplanner.ui.map.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OsrmRoutingClient(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = "https://router.project-osrm.org",
) {
    suspend fun routeDriving(
        from: LatLng,
        to: LatLng,
    ): Result<DrivingRoute> = withContext(Dispatchers.IO) {
        runCatching {
            val url =
                "$baseUrl/route/v1/driving/" +
                    "${from.longitude},${from.latitude};${to.longitude},${to.latitude}" +
                    "?overview=full&geometries=geojson&steps=true&annotations=false"

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Routing failed (${response.code})")
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) error("Empty routing response")
                parseRoute(JSONObject(body))
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
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
