package com.danielcioban.routeplanner.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/** Write JSON into a user-picked SAF tree. Auto-backup is always plaintext. */
object AutoBackup {
    const val FILE_NAME = "route-planner-auto.json"
    const val INTERVAL_MS = 24L * 60L * 60L * 1000L
    const val REMINDER_MS = 7L * INTERVAL_MS

    fun isReminderDue(lastBackupEpochMs: Long, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (lastBackupEpochMs <= 0L) return true
        return nowMs - lastBackupEpochMs >= REMINDER_MS
    }

    fun shouldWriteNow(lastBackupEpochMs: Long, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (lastBackupEpochMs <= 0L) return true
        return nowMs - lastBackupEpochMs >= INTERVAL_MS
    }

    fun write(context: Context, treeUriString: String, json: String): Boolean {
        if (treeUriString.isBlank()) return false
        val treeUri = Uri.parse(treeUriString)
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return false
        if (!tree.canWrite()) return false
        tree.findFile(FILE_NAME)?.delete()
        val file = tree.createFile("application/json", FILE_NAME) ?: return false
        return runCatching {
            context.contentResolver.openOutputStream(file.uri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            } != null
        }.getOrDefault(false)
    }

    fun writeUri(context: Context, uri: Uri, bytes: ByteArray): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(bytes)
            } != null
        }.getOrDefault(false)
    }

    fun readUri(context: Context, uri: Uri): ByteArray? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
    }
}
