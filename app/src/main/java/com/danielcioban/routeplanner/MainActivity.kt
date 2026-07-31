package com.danielcioban.routeplanner

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.data.settings.AppSettings
import com.danielcioban.routeplanner.ui.RoutePlannerApp
import com.danielcioban.routeplanner.ui.theme.RoutePlannerTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsRepository = (application as RoutePlannerApplication).settingsRepository
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )
            RoutePlannerTheme(themeMode = settings.themeMode) {
                RoutePlannerApp(settings = settings)
            }
        }
    }
}
