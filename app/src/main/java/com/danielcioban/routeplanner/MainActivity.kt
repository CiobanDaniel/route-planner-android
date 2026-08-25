package com.danielcioban.routeplanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.view.KeyEvent
import com.danielcioban.routeplanner.data.settings.AppSettings
import com.danielcioban.routeplanner.ui.RoutePlannerApp
import com.danielcioban.routeplanner.ui.theme.LocalReduceMotion
import com.danielcioban.routeplanner.ui.theme.RoutePlannerTheme
import com.danielcioban.routeplanner.util.VolumeKeyDone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : AppCompatActivity() {
    private val _launchIntent = MutableStateFlow<Intent?>(null)
    val launchIntent: StateFlow<Intent?> = _launchIntent.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _launchIntent.value = intent
        enableEdgeToEdge()
        val settingsRepository = (application as RoutePlannerApplication).settingsRepository
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )
            RoutePlannerTheme(themeMode = settings.themeMode) {
                CompositionLocalProvider(LocalReduceMotion provides settings.reduceMotion) {
                    RoutePlannerApp(settings = settings)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        _launchIntent.value = intent
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (VolumeKeyDone.handle(this, event)) return true
        return super.dispatchKeyEvent(event)
    }

    fun consumeOpenActiveDrive(): Boolean {
        val current = intent ?: return false
        if (!current.getBooleanExtra(EXTRA_OPEN_ACTIVE_DRIVE, false)) return false
        current.removeExtra(EXTRA_OPEN_ACTIVE_DRIVE)
        _launchIntent.value = current
        return true
    }

    fun consumeOpenLibrary(): Boolean {
        val current = intent ?: return false
        val flagged = current.getBooleanExtra(EXTRA_OPEN_LIBRARY, false) ||
            current.getStringExtra(EXTRA_OPEN_LIBRARY) == "true"
        if (!flagged) return false
        current.removeExtra(EXTRA_OPEN_LIBRARY)
        _launchIntent.value = current
        return true
    }

    fun consumeCreateRoute(): Boolean {
        val current = intent ?: return false
        val flagged = current.getBooleanExtra(EXTRA_CREATE_ROUTE, false) ||
            current.getStringExtra(EXTRA_CREATE_ROUTE) == "true"
        if (!flagged) return false
        current.removeExtra(EXTRA_CREATE_ROUTE)
        _launchIntent.value = current
        return true
    }

    fun consumeOpenRouteId(): Long? {
        val current = intent ?: return null
        if (!current.hasExtra(EXTRA_OPEN_ROUTE_ID)) return null
        val id = current.getLongExtra(EXTRA_OPEN_ROUTE_ID, -1L)
        current.removeExtra(EXTRA_OPEN_ROUTE_ID)
        _launchIntent.value = current
        return id.takeIf { it > 0L }
    }

    fun consumeDriveHome(): Boolean {
        val current = intent ?: return false
        val flagged = current.getBooleanExtra(EXTRA_DRIVE_HOME, false) ||
            current.getStringExtra(EXTRA_DRIVE_HOME) == "true"
        if (!flagged) return false
        current.removeExtra(EXTRA_DRIVE_HOME)
        _launchIntent.value = current
        return true
    }

    fun consumeNfcLibraryRemoteId(): String? {
        val current = intent ?: return null
        val fromNfc = com.danielcioban.routeplanner.util.NfcPlaceTag.libraryRemoteIdFromIntent(current)
        if (fromNfc != null) {
            current.data = null
            _launchIntent.value = current
            return fromNfc
        }
        return null
    }

    companion object {
        const val EXTRA_OPEN_ACTIVE_DRIVE = "open_active_drive"
        const val EXTRA_OPEN_LIBRARY = "open_library"
        const val EXTRA_CREATE_ROUTE = "create_route"
        const val EXTRA_OPEN_ROUTE_ID = "open_route_id"
        const val EXTRA_DRIVE_HOME = "drive_home"
    }
}
