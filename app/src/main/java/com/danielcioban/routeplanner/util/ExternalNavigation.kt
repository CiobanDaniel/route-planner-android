package com.danielcioban.routeplanner.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.danielcioban.routeplanner.R

/**
 * Opens turn-by-turn navigation in Google Maps, Waze, or any geo-capable app.
 */
object ExternalNavigation {
        const val MAX_MAPS_STOPS = 10
        const val NEXT_STOPS_LIMIT = 3

    fun openDrivingDirections(
        context: Context,
        latitude: Double,
        longitude: Double,
        label: String = "",
    ): Boolean {
        val fallbackLabel = context.getString(R.string.external_nav_stop_fallback)
        val encodedLabel = Uri.encode(label.ifBlank { fallbackLabel })
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
                    val chooser = Intent.createChooser(
                        primary,
                        context.getString(R.string.external_nav_chooser),
                    ).apply {
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

    /**
     * Opens Google Maps multi-stop driving directions. [origin] is typically the
     * courier's GPS; [destinations] are remaining pinned stops in visit order.
     * Google Maps URLs accept about 10 destination points.
     */
    fun openMultiStopDriving(
        context: Context,
        destinations: List<Pair<Double, Double>>,
        origin: Pair<Double, Double>? = null,
    ): Boolean {
        val dests = destinations.take(MAX_MAPS_STOPS)
        if (dests.isEmpty()) return false
        if (dests.size == 1 && origin == null) {
            return openDrivingDirections(context, dests.first().first, dests.first().second)
        }
        val path = buildList {
            origin?.let { add("${it.first},${it.second}") }
            dests.forEach { add("${it.first},${it.second}") }
        }
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/dir/" + path.joinToString("/")),
        )
        val maps = Intent(web).apply {
            setPackage("com.google.android.apps.maps")
        }
        return try {
            val launch = if (maps.resolveActivity(context.packageManager) != null) maps else web
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    /**
     * Open the next few remaining pins: Google Maps multi-stop, plus Waze for the
     * immediate next stop when installed.
     */
    fun openNextStops(
        context: Context,
        destinations: List<Pair<Double, Double>>,
        origin: Pair<Double, Double>? = null,
        limit: Int = NEXT_STOPS_LIMIT,
    ): Boolean {
        val dests = destinations.take(limit)
        if (dests.isEmpty()) return false
        val first = dests.first()
        val maps = mapsMultiStopIntent(context, dests, origin)
        val waze = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://waze.com/ul?ll=${first.first},${first.second}&navigate=yes"),
        ).apply {
            setPackage("com.waze")
        }
        val packageManager = context.packageManager
        val options = buildList {
            if (maps.resolveActivity(packageManager) != null) add(maps)
            if (waze.resolveActivity(packageManager) != null) add(waze)
        }
        return try {
            when {
                options.size >= 2 -> {
                    val chooser = Intent.createChooser(
                        options.first(),
                        context.getString(R.string.external_nav_chooser),
                    ).apply {
                        putExtra(Intent.EXTRA_INITIAL_INTENTS, options.drop(1).toTypedArray())
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                }
                options.size == 1 -> {
                    context.startActivity(options.first().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                else -> openMultiStopDriving(context, dests, origin)
            }
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun mapsMultiStopIntent(
        context: Context,
        dests: List<Pair<Double, Double>>,
        origin: Pair<Double, Double>?,
    ): Intent {
        val path = buildList {
            origin?.let { add("${it.first},${it.second}") }
            dests.forEach { add("${it.first},${it.second}") }
        }
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/dir/" + path.joinToString("/")),
        )
        return Intent(web).apply {
            setPackage("com.google.android.apps.maps")
            if (resolveActivity(context.packageManager) == null) {
                setPackage(null)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
