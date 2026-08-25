package com.danielcioban.routeplanner.ui.dev

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Release stub — no joystick in production builds. */
@Composable
fun DevSimOverlay(
    onJumpToMapCenter: () -> Unit,
    onJumpToNextStop: () -> Unit,
    modifier: Modifier = Modifier,
) = Unit
