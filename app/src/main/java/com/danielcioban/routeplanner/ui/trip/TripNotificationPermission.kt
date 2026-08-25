package com.danielcioban.routeplanner.ui.trip

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TripNotificationPermission(tripActive: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val permissions = remember {
        buildList {
            add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= 36) {
                add("android.permission.POST_PROMOTED_NOTIFICATIONS")
            }
        }
    }
    val permissionState = rememberMultiplePermissionsState(permissions)
    LaunchedEffect(tripActive) {
        if (tripActive && !permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        }
    }
}
