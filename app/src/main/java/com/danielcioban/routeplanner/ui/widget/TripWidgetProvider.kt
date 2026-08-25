package com.danielcioban.routeplanner.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.danielcioban.routeplanner.MainActivity
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.ui.trip.TripGuidanceService
import com.danielcioban.routeplanner.ui.trip.TripHudSnapshot

class TripWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val trip = TripWidgetStore.read(context)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context, trip))
        }
    }

    companion object {
        fun updateAll(context: Context, snapshot: TripHudSnapshot) {
            TripWidgetStore.write(context, snapshot)
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, TripWidgetProvider::class.java),
            )
            if (ids.isEmpty()) return
            val views = buildViews(context, TripWidgetStore.read(context))
            manager.updateAppWidget(ids, views)
        }

        private fun buildViews(context: Context, trip: TripWidgetStore.WidgetTrip): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.trip_widget)
            views.setOnClickPendingIntent(R.id.widget_root, openApp(context))
            if (!trip.active) {
                views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_idle))
                views.setTextViewText(R.id.widget_meta, "")
                views.setViewVisibility(R.id.widget_done, View.GONE)
                return views
            }
            views.setTextViewText(
                R.id.widget_title,
                trip.title.ifBlank { context.getString(R.string.app_name) },
            )
            val meta = listOf(trip.distance, trip.progress)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(" · ")
            views.setTextViewText(R.id.widget_meta, meta)
            if (trip.canMarkDone) {
                views.setViewVisibility(R.id.widget_done, View.VISIBLE)
                views.setOnClickPendingIntent(R.id.widget_done, markDone(context))
            } else {
                views.setViewVisibility(R.id.widget_done, View.GONE)
            }
            return views
        }

        private fun openApp(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_ACTIVE_DRIVE, true)
            }
            return PendingIntent.getActivity(
                context,
                21,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun markDone(context: Context): PendingIntent {
            val intent = Intent(context, TripGuidanceService::class.java)
                .setAction(TripGuidanceService.ACTION_MARK_DONE)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(context, 22, intent, flags)
            } else {
                PendingIntent.getService(context, 22, intent, flags)
            }
        }
    }
}
