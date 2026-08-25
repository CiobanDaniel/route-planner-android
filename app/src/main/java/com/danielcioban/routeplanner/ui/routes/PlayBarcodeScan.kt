package com.danielcioban.routeplanner.ui.routes

import android.content.Context
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

object PlayBarcodeScan {
    fun start(context: Context, onRaw: (String?) -> Unit) {
        runCatching {
            GmsBarcodeScanning.getClient(context)
                .startScan()
                .addOnSuccessListener { barcode -> onRaw(barcode.rawValue) }
                .addOnCanceledListener { onRaw(null) }
                .addOnFailureListener { onRaw(null) }
        }.onFailure {
            onRaw(null)
        }
    }
}
