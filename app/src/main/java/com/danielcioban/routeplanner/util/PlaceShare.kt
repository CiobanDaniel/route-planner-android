package com.danielcioban.routeplanner.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

object PlaceShare {
    fun coordinatesText(latitude: Double, longitude: Double): String {
        val plus = OpenLocationCode.encode(latitude, longitude)
        return "${OpenLocationCode.formatLatLng(latitude, longitude)}\n$plus"
    }

    fun copyCoordinates(context: Context, latitude: Double, longitude: Double) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("coordinates", coordinatesText(latitude, longitude)),
        )
    }

    fun shareText(
        context: Context,
        subject: String,
        body: String,
        mimeType: String = "text/plain",
        chooserTitle: String,
    ) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(send, chooserTitle))
    }
}
