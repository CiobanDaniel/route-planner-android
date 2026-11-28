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
        var file: DocumentFile? = null
        val verified = runCatching {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(treeUriString)) ?: return@runCatching false
            if (!tree.canWrite()) return@runCatching false
            // A fresh name keeps all previous backups intact on failure.
            val name = "route-planner-auto-${System.currentTimeMillis()}-${java.util.UUID.randomUUID()}.json"
            val created = tree.createFile("application/json", name) ?: return@runCatching false
            file = created
            val bytes = json.toByteArray(Charsets.UTF_8)
            context.contentResolver.openOutputStream(created.uri)?.use { stream ->
                stream.write(bytes)
            } ?: return@runCatching false
            context.contentResolver.openInputStream(created.uri)?.use { it.readBytes().contentEquals(bytes) } == true
        }.getOrDefault(false)
        if (!verified) runCatching { file?.delete() }
        return verified
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
