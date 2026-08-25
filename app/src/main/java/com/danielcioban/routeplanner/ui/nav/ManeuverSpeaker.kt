package com.danielcioban.routeplanner.ui.nav

import android.content.Context
import android.speech.tts.TextToSpeech
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.routing.ManeuverFormatter
import com.danielcioban.routeplanner.data.routing.NavGuidance
import com.danielcioban.routeplanner.data.settings.TtsVerbosity
import java.util.Locale

/** Speaks the current maneuver. Used by the driving screens and the background trip service. */
class ManeuverSpeaker(context: Context) {
    private var ready = false
    private val tts = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
    }

    fun setLanguage(languageTag: String) {
        val locale = Locale.forLanguageTag(languageTag).takeIf { it.language.isNotBlank() }
            ?: Locale.getDefault()
        if (ready) {
            tts.language = locale
        }
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    companion object {
        fun spokenKey(
            context: Context,
            guidance: NavGuidance,
            verbosity: TtsVerbosity = TtsVerbosity.NORMAL,
        ): String? {
            val step = guidance.currentStep ?: return null
            val resources = context.resources
            val current = when (verbosity) {
                TtsVerbosity.MINIMAL -> ManeuverFormatter.format(
                    resources,
                    step.type,
                    step.modifier,
                    "",
                )
                TtsVerbosity.NORMAL,
                TtsVerbosity.VERBOSE,
                -> ManeuverFormatter.formatStep(resources, step)
            }.trim().ifBlank { return null }
            if (verbosity != TtsVerbosity.VERBOSE) return current
            val thenStep = guidance.thenStep ?: return current
            val then = ManeuverFormatter.formatStep(resources, thenStep).trim()
            if (then.isBlank()) return current
            return resources.getString(R.string.maneuver_then_speech, current, then)
        }
    }
}
