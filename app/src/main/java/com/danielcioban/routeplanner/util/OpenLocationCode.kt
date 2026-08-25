package com.danielcioban.routeplanner.util

import java.util.Locale
import kotlin.math.floor
import kotlin.math.min

/**
 * Open Location Code (plus code) encode/decode. No network, no What3Words API.
 * 10-character codes are ~14 m. User-typed What3Words stay a separate free-text field.
 *
 * Integer algorithm matches Google's open-location-code (pair section, default 10 digits).
 */
object OpenLocationCode {
    private const val ALPHABET = "23456789CFGHJMPQRVWX"
    private const val SEPARATOR = '+'
    private const val SEPARATOR_INDEX = 8
    private const val DEFAULT_LENGTH = 10
    private const val PAIR_CODE_LENGTH = 10
    private const val ENCODING_BASE = 20
    private const val GRID_COLUMNS = 4
    private const val GRID_ROWS = 5
    private const val GRID_CODE_LENGTH = 5
    private const val LAT_INTEGER_MULTIPLIER = 8000L * 3125L
    private const val LNG_INTEGER_MULTIPLIER = 8000L * 1024L
    private const val LAT_MSP_VALUE = LAT_INTEGER_MULTIPLIER * ENCODING_BASE * ENCODING_BASE
    private const val LNG_MSP_VALUE = LNG_INTEGER_MULTIPLIER * ENCODING_BASE * ENCODING_BASE
    private val CODE_REGEX = Regex(
        "^[23456789CFGHJMPQRVWX]{8}\\+[23456789CFGHJMPQRVWX]{2,}$",
        RegexOption.IGNORE_CASE,
    )

    fun isFullCode(value: String): Boolean = CODE_REGEX.matches(value.trim())

    fun encode(latitude: Double, longitude: Double, codeLength: Int = DEFAULT_LENGTH): String {
        val length = codeLength.coerceIn(4, DEFAULT_LENGTH).let { if (it % 2 != 0) it - 1 else it }
        var lat = floor(latitude * LAT_INTEGER_MULTIPLIER).toLong()
        var lng = floor(longitude * LNG_INTEGER_MULTIPLIER).toLong()
        lat += 90L * LAT_INTEGER_MULTIPLIER
        if (lat < 0L) {
            lat = 0L
        } else {
            val latMax = 2L * 90L * LAT_INTEGER_MULTIPLIER
            if (lat >= latMax) lat = latMax - 1L
        }
        lng += 180L * LNG_INTEGER_MULTIPLIER
        val lngSpan = 2L * 180L * LNG_INTEGER_MULTIPLIER
        if (lng < 0L) {
            lng = lng % lngSpan + lngSpan
        } else if (lng >= lngSpan) {
            lng %= lngSpan
        }
        repeat(GRID_CODE_LENGTH) {
            lat /= GRID_ROWS
            lng /= GRID_COLUMNS
        }
        val reversed = StringBuilder()
        repeat(PAIR_CODE_LENGTH / 2) { i ->
            reversed.append(ALPHABET[(lng % ENCODING_BASE).toInt()])
            reversed.append(ALPHABET[(lat % ENCODING_BASE).toInt()])
            lat /= ENCODING_BASE
            lng /= ENCODING_BASE
            if (i == 0) reversed.append(SEPARATOR)
        }
        val code = reversed.reverse().toString()
        return code.substring(0, maxOf(SEPARATOR_INDEX + 1, length + 1))
    }

    data class LatLng(val latitude: Double, val longitude: Double)

    fun decode(code: String): LatLng? {
        val trimmed = code.trim().uppercase()
        if (!isFullCode(trimmed)) return null
        val clean = trimmed.replace(SEPARATOR.toString(), "")
        var latVal = -90L * LAT_INTEGER_MULTIPLIER
        var lngVal = -180L * LNG_INTEGER_MULTIPLIER
        var latPlaceVal = LAT_MSP_VALUE
        var lngPlaceVal = LNG_MSP_VALUE
        val pairEnd = min(clean.length, PAIR_CODE_LENGTH)
        var index = 0
        while (index + 1 < pairEnd) {
            latPlaceVal /= ENCODING_BASE
            lngPlaceVal /= ENCODING_BASE
            val latDigit = ALPHABET.indexOf(clean[index])
            val lngDigit = ALPHABET.indexOf(clean[index + 1])
            if (latDigit < 0 || lngDigit < 0) return null
            latVal += latDigit * latPlaceVal
            lngVal += lngDigit * lngPlaceVal
            index += 2
        }
        while (index < min(clean.length, 15)) {
            latPlaceVal /= GRID_ROWS
            lngPlaceVal /= GRID_COLUMNS
            val digit = ALPHABET.indexOf(clean[index])
            if (digit < 0) return null
            latVal += (digit / GRID_COLUMNS) * latPlaceVal
            lngVal += (digit % GRID_COLUMNS) * lngPlaceVal
            index++
        }
        val latitudeLo = latVal.toDouble() / LAT_INTEGER_MULTIPLIER
        val longitudeLo = lngVal.toDouble() / LNG_INTEGER_MULTIPLIER
        val latitudeHi = (latVal + latPlaceVal).toDouble() / LAT_INTEGER_MULTIPLIER
        val longitudeHi = (lngVal + lngPlaceVal).toDouble() / LNG_INTEGER_MULTIPLIER
        return LatLng(
            latitude = (latitudeLo + latitudeHi) / 2.0,
            longitude = (longitudeLo + longitudeHi) / 2.0,
        )
    }

    fun formatLatLng(latitude: Double, longitude: Double): String =
        String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
}
