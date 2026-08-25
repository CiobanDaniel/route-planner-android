package com.danielcioban.routeplanner.data

import com.danielcioban.routeplanner.BuildConfig

/**
 * Production routing / geocoding bases. Override in `local.properties`:
 * `osrm.base.url`, `nominatim.base.url`, plus optional `osrm.fallback.url` /
 * `nominatim.fallback.url`. Defaults are public demos — not for scale.
 */
object ServiceEndpoints {
    val osrmBaseUrl: String
        get() = normalize(BuildConfig.OSRM_BASE_URL)

    val osrmFallbackUrl: String
        get() = normalize(BuildConfig.OSRM_FALLBACK_URL)

    val nominatimBaseUrl: String
        get() = normalize(BuildConfig.NOMINATIM_BASE_URL)

    val nominatimFallbackUrl: String
        get() = normalize(BuildConfig.NOMINATIM_FALLBACK_URL)

    fun osrmCandidates(): List<String> = candidates(osrmBaseUrl, osrmFallbackUrl)

    fun nominatimCandidates(): List<String> = candidates(nominatimBaseUrl, nominatimFallbackUrl)

    val usesPublicDemoOsrm: Boolean
        get() = osrmBaseUrl.contains("project-osrm.org", ignoreCase = true)

    val usesPublicDemoNominatim: Boolean
        get() = nominatimBaseUrl.contains("nominatim.openstreetmap.org", ignoreCase = true)

    fun hostLabel(url: String): String =
        normalize(url).removePrefix("https://").removePrefix("http://")

    fun candidates(primary: String, fallback: String): List<String> =
        listOf(primary, fallback)
            .map(::normalize)
            .filter { it.isNotBlank() }
            .distinct()

    private fun normalize(value: String): String = value.trim().trimEnd('/')
}
