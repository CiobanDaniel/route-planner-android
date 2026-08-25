package com.danielcioban.routeplanner.ui.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.danielcioban.routeplanner.MainActivity
import com.danielcioban.routeplanner.R

object AppShortcuts {
    private const val LAST_ROUTE_ID = "last_route"
    private const val DRIVE_HOME_ID = "drive_home"

    fun publish(
        context: Context,
        lastRouteId: Long?,
        lastRouteName: String?,
        homeSet: Boolean,
    ) {
        val shortcuts = mutableListOf<ShortcutInfoCompat>()
        if (homeSet) {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_DRIVE_HOME, true)
            }
            shortcuts += ShortcutInfoCompat.Builder(context, DRIVE_HOME_ID)
                .setShortLabel(context.getString(R.string.shortcut_drive_home_short))
                .setLongLabel(context.getString(R.string.shortcut_drive_home_long))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_stat_navigation))
                .setIntent(intent)
                .setRank(0)
                .build()
        }
        if (lastRouteId != null && lastRouteId > 0L && !lastRouteName.isNullOrBlank()) {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_ROUTE_ID, lastRouteId)
            }
            shortcuts += ShortcutInfoCompat.Builder(context, LAST_ROUTE_ID)
                .setShortLabel(context.getString(R.string.shortcut_last_route_short))
                .setLongLabel(context.getString(R.string.shortcut_last_route_long, lastRouteName))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_stat_navigation))
                .setIntent(intent)
                .setRank(1)
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        if (!homeSet) {
            ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(DRIVE_HOME_ID))
        }
        if (lastRouteId == null || lastRouteId <= 0L || lastRouteName.isNullOrBlank()) {
            ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(LAST_ROUTE_ID))
        }
    }
}
