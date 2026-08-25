package com.danielcioban.routeplanner.util

import android.content.Context
import android.media.AudioManager

object CallAudio {
    fun isInVoiceCall(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.mode == AudioManager.MODE_IN_CALL ||
            am.mode == AudioManager.MODE_IN_COMMUNICATION
    }
}
