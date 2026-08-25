package com.danielcioban.routeplanner.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object FailureEvidence {
    fun newPhotoFile(context: Context): File {
        val dir = File(context.filesDir, "failures").apply { mkdirs() }
        return File(dir, "photo_${System.currentTimeMillis()}.jpg")
    }

    fun uriFor(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    fun saveSignature(context: Context, bitmap: Bitmap): String {
        val dir = File(context.filesDir, "failures").apply { mkdirs() }
        val file = File(dir, "sig_${System.currentTimeMillis()}.png")
        file.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        }
        return file.absolutePath
    }
}
