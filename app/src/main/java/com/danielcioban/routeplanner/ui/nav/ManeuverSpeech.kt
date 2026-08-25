package com.danielcioban.routeplanner.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.danielcioban.routeplanner.data.settings.TtsVerbosity
import com.danielcioban.routeplanner.ui.routes.NavigationPhase
import com.danielcioban.routeplanner.ui.routes.NavigationUiState
import com.danielcioban.routeplanner.ui.trip.TripGuidanceService
import com.danielcioban.routeplanner.util.CallAudio

/** Speaks the current maneuver while the driving screen is open and the trip service is not. */
@Composable
fun ManeuverSpeechEffect(
    enabled: Boolean,
    navigation: NavigationUiState,
    languageTag: String,
    verbosity: TtsVerbosity = TtsVerbosity.NORMAL,
    muteDuringCalls: Boolean = true,
) {
    val context = LocalContext.current
    val speaker = remember { ManeuverSpeaker(context) }
    DisposableEffect(speaker) {
        onDispose { speaker.shutdown() }
    }
    LaunchedEffect(languageTag) {
        speaker.setLanguage(languageTag)
    }
    val inCall = muteDuringCalls && CallAudio.isInVoiceCall(context)
    val spokenKey = when {
        !enabled || inCall -> null
        TripGuidanceService.isRunning -> null
        navigation.phase != NavigationPhase.Navigating -> null
        navigation.route?.isApproximate == true -> null
        else -> navigation.guidance?.let { ManeuverSpeaker.spokenKey(context, it, verbosity) }
    }
    LaunchedEffect(spokenKey) {
        val text = spokenKey ?: return@LaunchedEffect
        speaker.speak(text)
    }
}
