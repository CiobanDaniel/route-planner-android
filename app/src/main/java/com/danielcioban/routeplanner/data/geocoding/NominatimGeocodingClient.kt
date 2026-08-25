package com.danielcioban.routeplanner.data.geocoding

import com.danielcioban.routeplanner.BuildConfig
import com.danielcioban.routeplanner.data.EndpointProbe
import com.danielcioban.routeplanner.data.ServiceEndpoints
import com.danielcioban.routeplanner.ui.map.LatLng
import com.danielcioban.routeplanner.util.OpenLocationCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

data class PlaceSearchResult(
    val displayName: String,
    val shortName: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String = "",
) {
    val coordinate: LatLng get() = LatLng(latitude, longitude)
}

/**
 * OpenStreetMap Nominatim search.
 * Requires a descriptive User-Agent per Nominatim usage policy.
 */
class NominatimGeocodingClient(
    private val client: OkHttpClient = defaultClient(),
    private val baseUrl: String = ServiceEndpoints.nominatimBaseUrl,
    private val fallbackUrl: String = ServiceEndpoints.nominatimFallbackUrl,
    private val userAgent: String = USER_AGENT,
) {
    private val bases: List<String>
        get() = ServiceEndpoints.candidates(baseUrl, fallbackUrl).ifEmpty { listOf(baseUrl) }
    suspend fun search(
        query: String,
        limit: Int = 8,
        languageTag: String? = null,
        near: LatLng? = null,
        nearMeOnly: Boolean = false,
    ): Result<List<PlaceSearchResult>> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmed = query.trim()
            OpenLocationCode.decode(trimmed)?.let { decoded ->
                val code = trimmed.uppercase()
                return@runCatching listOf(
                    PlaceSearchResult(
                        displayName = code,
                        shortName = code,
                        latitude = decoded.latitude,
                        longitude = decoded.longitude,
                    ),
                )
            }
            if (trimmed.length < 3) return@runCatching emptyList()

            val body = fetchFromAny(languageTag) { base ->
                "$base/search".toHttpUrl().newBuilder()
                    .addQueryParameter("q", trimmed)
                    .addQueryParameter("format", "json")
                    .addQueryParameter("limit", limit.toString())
                    .addQueryParameter("addressdetails", "0")
                    .apply {
                        if (near != null) {
                            val delta = if (nearMeOnly) 0.12 else 0.35
                            addQueryParameter(
                                "viewbox",
                                "${near.longitude - delta},${near.latitude + delta}," +
                                    "${near.longitude + delta},${near.latitude - delta}",
                            )
                            addQueryParameter("bounded", if (nearMeOnly) "1" else "0")
                        }
                    }
                    .build()
                    .toString()
            }
            if (body.isBlank()) emptyList() else parseResults(JSONArray(body))
        }
    }

    suspend fun reverse(
        latitude: Double,
        longitude: Double,
        languageTag: String? = null,
    ): Result<PlaceSearchResult?> = withContext(Dispatchers.IO) {
        runCatching {
            val body = fetchFromAny(languageTag) { base ->
                "$base/reverse".toHttpUrl().newBuilder()
                    .addQueryParameter("lat", latitude.toString())
                    .addQueryParameter("lon", longitude.toString())
                    .addQueryParameter("format", "json")
                    .addQueryParameter("zoom", "18")
                    .addQueryParameter("addressdetails", "0")
                    .build()
                    .toString()
            }
            if (body.isBlank()) null else parseReverse(JSONObject(body))
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
        if (tryGet("$base/status") { body ->
                body.contains("OK", ignoreCase = true) || body.contains("status", ignoreCase = true)
            }
        ) {
            return true
        }
        val searchUrl = "$base/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", "OpenStreetMap")
            .addQueryParameter("format", "json")
            .addQueryParameter("limit", "1")
            .build()
            .toString()
        return tryGet(searchUrl) { it.trim().startsWith("[") }
    }

    private fun tryGet(url: String, accept: (String) -> Boolean): Boolean {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                response.isSuccessful && accept(body)
            }
        }.getOrDefault(false)
    }

    private suspend fun fetchFromAny(languageTag: String?, buildUrl: (String) -> String): String {
        var last: Exception? = null
        for (base in bases) {
            try {
                return fetch(buildUrl(base), languageTag)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: IllegalStateException("Search failed")
    }

    private suspend fun fetch(url: String, languageTag: String?): String {
        NominatimRateLimit.awaitTurn()
        return execute(url, languageTag)
    }

    private fun execute(url: String, languageTag: String?): String {
        val requestBuilder = Request.Builder()
            .url(url)
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
            return response.body?.string().orEmpty()
        }
    }

    private fun parseReverse(obj: JSONObject): PlaceSearchResult? {
        val lat = obj.optString("lat").toDoubleOrNull() ?: return null
        val lon = obj.optString("lon").toDoubleOrNull() ?: return null
        val display = obj.optString("display_name").trim()
        if (display.isEmpty()) return null
        val named = obj.optString("name").trim()
        val shortName = named.ifBlank {
            display.substringBefore(',').trim().ifBlank { display }
        }
        return PlaceSearchResult(
            displayName = display,
            shortName = shortName,
            latitude = lat,
            longitude = lon,
        )
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
        val USER_AGENT =
            "RoutePlannerAndroid/${BuildConfig.VERSION_NAME} (com.danielcioban.routeplanner; personal delivery planner)"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
