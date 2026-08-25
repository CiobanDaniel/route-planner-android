package com.danielcioban.routeplanner.util

import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

/** Write / parse NDEF URIs for a library pin (`https://routeplanner.local/l/{remoteId}`). */
object NfcPlaceTag {
    const val HOST = "routeplanner.local"
    const val PATH_PREFIX = "/l/"

    private val pendingWrite = AtomicReference<PendingWrite?>(null)
    private var boundActivity: WeakReference<Activity>? = null

    data class PendingWrite(
        val uri: String,
        val onResult: (Boolean) -> Unit,
    )

    fun uriForLibrary(remoteId: String): String =
        "https://$HOST$PATH_PREFIX${remoteId.trim()}"

    fun libraryRemoteIdFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        val data = intent.data
        if (data != null &&
            data.host.equals(HOST, ignoreCase = true) &&
            data.path?.startsWith(PATH_PREFIX) == true
        ) {
            return data.path?.removePrefix(PATH_PREFIX)?.takeIf { it.isNotBlank() }
        }
        val raw = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        }
        val message = raw?.firstOrNull() as? NdefMessage ?: return null
        val payload = message.records.firstOrNull()?.toUri()?.toString().orEmpty()
        return BarcodeMatch.libraryRemoteId(payload)
    }

    fun beginWrite(activity: Activity, uri: String, onResult: (Boolean) -> Unit) {
        pendingWrite.set(PendingWrite(uri, onResult))
        bind(activity)
    }

    fun cancelWrite(activity: Activity) {
        pendingWrite.set(null)
        unbind(activity)
    }

    fun available(activity: Activity): Boolean =
        NfcAdapter.getDefaultAdapter(activity)?.isEnabled == true

    private fun bind(activity: Activity) {
        boundActivity = WeakReference(activity)
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        adapter.enableReaderMode(
            activity,
            { tag -> handleTag(tag) },
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null,
        )
    }

    private fun unbind(activity: Activity) {
        NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity)
    }

    private fun handleTag(tag: Tag) {
        val pending = pendingWrite.getAndSet(null) ?: return
        val ok = writeUri(tag, pending.uri)
        boundActivity?.get()?.let { unbind(it) }
        pending.onResult(ok)
    }

    private fun writeUri(tag: Tag, uri: String): Boolean {
        val record = NdefRecord.createUri(uri)
        val message = NdefMessage(arrayOf(record))
        val bytes = message.toByteArray()
        runCatching {
            Ndef.get(tag)?.use { ndef ->
                ndef.connect()
                if (!ndef.isWritable || ndef.maxSize < bytes.size) return false
                ndef.writeNdefMessage(message)
                return true
            }
            NdefFormatable.get(tag)?.use { format ->
                format.connect()
                format.format(message)
                return true
            }
        }
        return false
    }
}
