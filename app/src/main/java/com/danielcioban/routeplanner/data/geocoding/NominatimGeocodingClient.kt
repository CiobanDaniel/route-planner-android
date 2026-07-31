package com.danielcioban.routeplanner.data.geocoding

import com.danielcioban.routeplanner.ui.map.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

data class PlaceSearchResult(
    val displayName: String,
    val shortName: String,
    val latitude: Double,
    val longitude: Double,
) {
    val coordinate: LatLng get() = LatLng(latitude, longitude)
}

/**
 * OpenStreetMap Nominatim search.
 * Requires a descriptive User-Agent per Nominatim usage policy.
 */
class NominatimGeocodingClient(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = "https://nominatim.openstreetmap.org",
    private val userAgent: String = USER_AGENT,
) {
    suspend fun search(
        query: String,
        limit: Int = 8,
        languageTag: String? = null,
        near: LatLng? = null,
    ): Result<List<PlaceSearchResult>> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmed = query.trim()
            if (trimmed.length < 3) return@runCatching emptyList()

            val urlBuilder = "$baseUrl/search".toHttpUrl().newBuilder()
                .addQueryParameter("q", trimmed)
                .addQueryParameter("format", "json")
                .addQueryParameter("limit", limit.toString())
                .addQueryParameter("addressdetails", "0")

            if (near != null) {
                // Soft bias around the user without hard-clipping results.
                val delta = 0.35
                urlBuilder
                    .addQueryParameter(
                        "viewbox",
                        "${near.longitude - delta},${near.latitude + delta}," +
                            "${near.longitude + delta},${near.latitude - delta}",
                    )
                    .addQueryParameter("bounded", "0")
            }

            val requestBuilder = Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .get()

            if (!languageTag.isNullOrBlank()) {
                requestBuilder.header("Accept-Language", languageTag)
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Search failed (${response.code})")
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@use emptyList()
                parseResults(JSONArray(body))
            }
        }
    }

    private fun parseResults(array: JSONArray): List<PlaceSearchResult> {
        val out = ArrayList<PlaceSearchResult>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val lat = obj.optString("lat").toDoubleOrNull() ?: continue
            val lon = obj.optString("lon").toDoubleOrNull() ?: continue
            val display = obj.optString("display_name").trim()
            if (display.isEmpty()) continue
            val named = obj.optString("name").trim()
            val shortName = named.ifBlank {
                display.substringBefore(',').trim().ifBlank { display }
            }
            out += PlaceSearchResult(
                displayName = display,
                shortName = shortName,
                latitude = lat,
                longitude = lon,
            )
        }
        return out
    }

    companion object {
        const val USER_AGENT =
            "RoutePlannerAndroid/0.3 (com.danielcioban.routeplanner; personal delivery planner)"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
