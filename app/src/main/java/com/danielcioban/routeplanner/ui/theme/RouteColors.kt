package com.danielcioban.routeplanner.ui.theme

import androidx.compose.ui.graphics.Color

object RouteColors {
    const val DEFAULT_HEX = "#E86A17"

    val palette: List<String> = listOf(
        "#E86A17",
        "#1B6CA8",
        "#2E7D32",
        "#6A1B9A",
        "#C62828",
        "#455A64",
    )

    fun parse(hex: String): Color {
        val raw = hex.trim().removePrefix("#")
        val value = raw.toLongOrNull(16) ?: return BrandColors.orange
        val color = when (raw.length) {
            6 -> 0xFF000000L or value
            8 -> value
            else -> return BrandColors.orange
        }
        return Color(color.toInt())
    }
}
