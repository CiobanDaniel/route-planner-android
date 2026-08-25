package com.danielcioban.routeplanner.ui.trip

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.danielcioban.routeplanner.MainActivity
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.settings.DistanceUnit
import com.danielcioban.routeplanner.util.GeoUtils
import kotlin.math.roundToInt

data class TripNotice(
    val title: String,
    val text: String,
    val subText: String,
    val chipText: String,
    val progress: Int,
    val progressMax: Int,
    val etaEpochMs: Long?,
    val arrived: Boolean,
    val canMarkDone: Boolean,
    val allDone: Boolean,
    val canSkip: Boolean = false,
    val phone: String? = null,
)

object TripNotification {
    const val CHANNEL_ID = "trip_guidance"
    const val LIVE_CHANNEL_ID = "trip_live_update"
    const val NOTIFICATION_ID = 41
    const val LIVE_UPDATE_ID = 42
    const val ACCENT = 0xFFE86A17.toInt()

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.trip_notification_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.trip_notification_channel_desc)
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
            )
        }
        if (Build.VERSION.SDK_INT >= 36 && manager.getNotificationChannel(LIVE_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    LIVE_CHANNEL_ID,
                    context.getString(R.string.trip_live_update_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.trip_live_update_channel_desc)
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
            )
        }
    }

    fun placeholder(context: Context): Notification {
        ensureChannel(context)
        return buildForeground(
            context,
            TripNotice(
                title = context.getString(R.string.trip_notification_starting),
                text = context.getString(R.string.trip_notification_waiting_gps),
                subText = context.getString(R.string.app_name),
                chipText = "GPS",
                progress = 0,
                progressMax = 1,
                etaEpochMs = null,
                arrived = false,
                canMarkDone = false,
                allDone = false,
            ),
        )
    }

    /** Foreground-service shade notification. Never request promotion on this one. */
    fun buildForeground(context: Context, notice: TripNotice): Notification {
        ensureChannel(context)
        return buildCompat(context, notice)
    }

    fun postLiveUpdate(context: Context, notice: TripNotice) {
        if (Build.VERSION.SDK_INT < 36) return
        ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!manager.canPostPromotedNotifications()) return
        manager.notify(LIVE_UPDATE_ID, buildLiveUpdate(context, notice))
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        NotificationManagerCompat.from(context).cancel(LIVE_UPDATE_ID)
    }

    fun chipDistance(meters: Double, unit: DistanceUnit): String {
        val compact = when (unit) {
            DistanceUnit.METRIC -> when {
                meters < 995 -> "${meters.roundToInt()}m"
                meters < 9_950 -> String.format("%.1fkm", meters / 1000.0)
                else -> "${(meters / 1000.0).roundToInt()}km"
            }
            DistanceUnit.IMPERIAL -> {
                val feet = meters * 3.28084
                when {
                    feet < 995 -> "${feet.roundToInt()}ft"
                    else -> {
                        val miles = meters / 1609.344
                        if (miles < 9.95) String.format("%.1fmi", miles) else "${miles.roundToInt()}mi"
                    }
                }
            }
        }
        return compact.take(7)
    }

    fun bodyDistance(meters: Double, unit: DistanceUnit): String =
        GeoUtils.formatDistance(meters, unit)

    private fun buildCompat(context: Context, notice: TripNotice): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_navigation)
            .setContentTitle(notice.title)
            .setContentText(notice.text)
            .setSubText(notice.subText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notice.text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(ACCENT)
            .setColorized(false)
            .setContentIntent(openAppIntent(context))
            .setFullScreenIntent(LockScreenHudActivity.pendingIntent(context), false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setProgress(notice.progressMax.coerceAtLeast(1), notice.progress, false)
        addShadeActions(context, builder, notice)
        return builder.build()
    }

    @RequiresApi(36)
    private fun buildLiveUpdate(context: Context, notice: TripNotice): Notification {
        val pct = if (notice.allDone) {
            100
        } else {
            val max = notice.progressMax.coerceAtLeast(1)
            ((notice.progress * 100) / max).coerceIn(0, 100)
        }
        val progressStyle = Notification.ProgressStyle()
            .setStyledByProgress(true)
            .setProgress(pct)
            .setProgressTrackerIcon(
                Icon.createWithResource(context, R.drawable.ic_stat_navigation),
            )
            .setProgressSegments(
                listOf(
                    Notification.ProgressStyle.Segment(100).setColor(ACCENT),
                ),
            )
        val builder = Notification.Builder(context, LIVE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_navigation)
            .setContentTitle(notice.title)
            .setContentText(notice.text)
            .setSubText(notice.subText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setColor(ACCENT)
            .setContentIntent(openAppIntent(context))
            .setStyle(progressStyle)
            .setShortCriticalText(notice.chipText.take(7))
        applyRequestPromotedOngoing(builder)
        notice.etaEpochMs?.let { eta ->
            builder.setWhen(eta)
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
            builder.setShowWhen(true)
        }
        if (notice.canMarkDone) {
            builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_stat_navigation),
                    context.getString(R.string.nav_mark_done),
                    serviceIntent(context, TripGuidanceService.ACTION_MARK_DONE, 12),
                ).build(),
            )
        } else if (notice.canSkip) {
            builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_stat_navigation),
                    context.getString(R.string.trip_notification_skip),
                    serviceIntent(context, TripGuidanceService.ACTION_SKIP, 14),
                ).build(),
            )
        } else {
            builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_stat_navigation),
                    context.getString(R.string.trip_notification_end),
                    serviceIntent(context, TripGuidanceService.ACTION_END, 13),
                ).build(),
            )
        }
        if (!notice.phone.isNullOrBlank()) {
            builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_stat_navigation),
                    context.getString(R.string.trip_notification_call),
                    serviceIntent(context, TripGuidanceService.ACTION_CALL, 15),
                ).build(),
            )
        }
        return builder.build()
    }

    private fun addShadeActions(
        context: Context,
        builder: NotificationCompat.Builder,
        notice: TripNotice,
    ) {
        var count = 0
        fun add(action: NotificationCompat.Action) {
            if (count >= 3) return
            builder.addAction(action)
            count++
        }
        if (notice.canMarkDone) {
            add(
                NotificationCompat.Action(
                    R.drawable.ic_stat_navigation,
                    context.getString(R.string.nav_mark_done),
                    serviceIntent(context, TripGuidanceService.ACTION_MARK_DONE, 2),
                ),
            )
        }
        if (!notice.phone.isNullOrBlank()) {
            add(
                NotificationCompat.Action(
                    R.drawable.ic_stat_navigation,
                    context.getString(R.string.trip_notification_call),
                    serviceIntent(context, TripGuidanceService.ACTION_CALL, 4),
                ),
            )
        }
        if (notice.canSkip) {
            add(
                NotificationCompat.Action(
                    R.drawable.ic_stat_navigation,
                    context.getString(R.string.trip_notification_skip),
                    serviceIntent(context, TripGuidanceService.ACTION_SKIP, 5),
                ),
            )
        }
        if (count < 3) {
            add(
                NotificationCompat.Action(
                    R.drawable.ic_stat_navigation,
                    context.getString(R.string.trip_notification_end),
                    serviceIntent(context, TripGuidanceService.ACTION_END, 3),
                ),
            )
        }
        if (count < 3) {
            add(
                NotificationCompat.Action(
                    R.drawable.ic_stat_navigation,
                    context.getString(R.string.trip_notification_hud),
                    LockScreenHudActivity.pendingIntent(context),
                ),
            )
        }
    }

    /**
     * [Notification.Builder.setRequestPromotedOngoing] is not always on the compile SDK
     * even at API 36. Call it by name so a device that has Live Updates still promotes.
     */
    private fun applyRequestPromotedOngoing(builder: Notification.Builder) {
        try {
            val method = Notification.Builder::class.java.getMethod(
                "setRequestPromotedOngoing",
                java.lang.Boolean.TYPE,
            )
            method.invoke(builder, java.lang.Boolean.TRUE)
        } catch (_: ReflectiveOperationException) {
            // Compile SDK or OEM without promoted ongoing.
        }
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_ACTIVE_DRIVE, true)
        }
        return PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun serviceIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, TripGuidanceService::class.java).setAction(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, requestCode, intent, flags)
        } else {
            PendingIntent.getService(context, requestCode, intent, flags)
        }
    }
}
