package com.danielcioban.routeplanner.ui.trip

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.MainActivity
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.RoutePlannerApplication
import com.danielcioban.routeplanner.data.settings.AppSettings
import com.danielcioban.routeplanner.ui.layout.readableWidth
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.ui.theme.LocalReduceMotion
import com.danielcioban.routeplanner.ui.theme.RoutePlannerTheme
import com.danielcioban.routeplanner.util.VolumeKeyDone

/**
 * Compose-only lock-screen HUD. No Leaflet WebView — next stop, distance, maneuver,
 * Done / End / Open map. Shown when the phone sleeps during a trip (always-on)
 * or from the Driving notification HUD action.
 */
class LockScreenHudActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        val app = application as RoutePlannerApplication
        setContent {
            val settings by app.settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )
            val snapshot by TripHudStore.snapshot.collectAsStateWithLifecycle()
            LaunchedEffect(snapshot.active) {
                if (!snapshot.active && !TripGuidanceService.isRunning) finish()
            }
            RoutePlannerTheme(themeMode = settings.themeMode) {
                CompositionLocalProvider(LocalReduceMotion provides settings.reduceMotion) {
                    LockScreenHud(
                        snapshot = snapshot,
                        keepScreenOn = snapshot.keepScreenOn,
                        onDone = { TripGuidanceService.sendAction(this, TripGuidanceService.ACTION_MARK_DONE) },
                        onEnd = {
                            TripGuidanceService.sendAction(this, TripGuidanceService.ACTION_END)
                            finish()
                        },
                        onOpenMap = {
                            startActivity(
                                Intent(this, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    putExtra(MainActivity.EXTRA_OPEN_ACTIVE_DRIVE, true)
                                },
                            )
                            finish()
                        },
                    )
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (VolumeKeyDone.handle(this, event)) return true
        return super.dispatchKeyEvent(event)
    }

    companion object {
        fun show(context: Context) {
            val intent = Intent(context, LockScreenHudActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP,
                )
            }
            if (Build.VERSION.SDK_INT >= 34) {
                @Suppress("DEPRECATION")
                val options = ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                )
                context.startActivity(intent, options.toBundle())
            } else {
                context.startActivity(intent)
            }
        }

        fun pendingIntent(context: Context): android.app.PendingIntent {
            val intent = Intent(context, LockScreenHudActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return android.app.PendingIntent.getActivity(
                context,
                4,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}

@Composable
private fun LockScreenHud(
    snapshot: TripHudSnapshot,
    keepScreenOn: Boolean,
    onDone: () -> Unit,
    onEnd: () -> Unit,
    onOpenMap: () -> Unit,
) {
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .readableWidth()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.labelLarge,
                color = IslandColors.onSurfaceMuted,
            )
            if (snapshot.progressLabel.isNotBlank()) {
                Text(
                    text = snapshot.progressLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = IslandColors.onSurfaceMuted,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = snapshot.distance.ifBlank { stringResource(R.string.trip_hud_waiting) },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = IslandColors.onSurface,
            )
            Text(
                text = snapshot.title.ifBlank { stringResource(R.string.trip_notification_starting) },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = IslandColors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (snapshot.maneuver.isNotBlank()) {
                Text(
                    text = snapshot.maneuver,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = IslandColors.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (snapshot.thenManeuver.isNotBlank()) {
                Text(
                    text = snapshot.thenManeuver,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = IslandColors.onSurfaceMuted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (snapshot.eta.isNotBlank()) {
                Text(
                    text = snapshot.eta,
                    style = MaterialTheme.typography.bodyLarge,
                    color = IslandColors.onSurfaceMuted,
                )
            }
            if (snapshot.tasksBlocked) {
                Text(
                    text = stringResource(R.string.trip_notification_tasks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (snapshot.canMarkDone && !snapshot.allDone) {
                Button(
                    onClick = onDone,
                    enabled = !snapshot.tasksBlocked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.nav_mark_done))
                }
            }
            OutlinedButton(
                onClick = onOpenMap,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.trip_hud_open_map))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onEnd) {
                    Text(stringResource(R.string.trip_notification_end))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        }
    }
}
