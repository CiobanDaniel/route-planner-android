package com.danielcioban.routeplanner.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens turn-by-turn navigation in Google Maps, Waze, or any geo-capable app.
 */
object ExternalNavigation {
    fun openDrivingDirections(
        context: Context,
        latitude: Double,
        longitude: Double,
        label: String = "Stop",
    ): Boolean {
        val encodedLabel = Uri.encode(label.ifBlank { "Stop" })
        val packageManager = context.packageManager

        val googleMaps = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=$latitude,$longitude"),
        ).apply {
            setPackage("com.google.android.apps.maps")
        }

        val waze = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://waze.com/ul?ll=$latitude,$longitude&navigate=yes"),
        ).apply {
            setPackage("com.waze")
        }

        val geo = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)"),
        )

        val webFallback = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                "https://www.google.com/maps/dir/?api=1" +
                    "&destination=$latitude,$longitude" +
                    "&travelmode=driving",
            ),
        )

        val installed = buildList {
            if (googleMaps.resolveActivity(packageManager) != null) add(googleMaps)
            if (waze.resolveActivity(packageManager) != null) add(waze)
            if (geo.resolveActivity(packageManager) != null) add(geo)
        }

        return try {
            when {
                installed.size >= 2 -> {
                    val primary = installed.first()
                    val extras = installed.drop(1).toTypedArray()
                    val chooser = Intent.createChooser(primary, "Navigate with").apply {
                        putExtra(Intent.EXTRA_INITIAL_INTENTS, extras)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                }
                installed.size == 1 -> {
                    context.startActivity(
                        installed.first().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
                else -> {
                    context.startActivity(
                        webFallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
