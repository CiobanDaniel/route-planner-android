package com.danielcioban.routeplanner.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.danielcioban.routeplanner.data.local.StopEntity

object ShareNextStop {
    fun sms(context: Context, stop: StopEntity, etaLabel: String? = null): Boolean {
        val maps = if (stop.latitude != null && stop.longitude != null) {
            "https://maps.google.com/?q=${stop.latitude},${stop.longitude}"
        } else {
            ""
        }
        val body = buildString {
            append(stop.name)
            if (stop.addressHint.isNotBlank()) {
                append('\n')
                append(stop.addressHint.trim())
            }
            if (!etaLabel.isNullOrBlank()) {
                append('\n')
                append(etaLabel)
            }
            if (maps.isNotBlank()) {
                append('\n')
                append(maps)
            }
        }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    fun shareEta(context: Context, stop: StopEntity, etaLabel: String?, chooserTitle: String): Boolean {
        val maps = if (stop.latitude != null && stop.longitude != null) {
            "https://maps.google.com/?q=${stop.latitude},${stop.longitude}"
        } else {
            ""
        }
        val body = buildString {
            append(stop.name)
            if (stop.addressHint.isNotBlank()) {
                append('\n')
                append(stop.addressHint.trim())
            }
            if (!etaLabel.isNullOrBlank()) {
                append('\n')
                append(etaLabel)
            }
            if (stop.phone.isNotBlank()) {
                append('\n')
                append(stop.phone.trim())
            }
            if (maps.isNotBlank()) {
                append('\n')
                append(maps)
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_SUBJECT, stop.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(Intent.createChooser(intent, chooserTitle))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    fun dial(context: Context, phone: String): Boolean {
        val trimmed = phone.trim()
        if (trimmed.isEmpty()) return false
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$trimmed")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
