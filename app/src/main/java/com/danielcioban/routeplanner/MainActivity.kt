package com.danielcioban.routeplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.danielcioban.routeplanner.ui.RoutePlannerApp
import com.danielcioban.routeplanner.ui.theme.RoutePlannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoutePlannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RoutePlannerApp()
                }
            }
        }
    }
}
