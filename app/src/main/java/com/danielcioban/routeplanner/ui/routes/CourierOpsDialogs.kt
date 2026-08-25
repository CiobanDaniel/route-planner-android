package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun ProofOfDeliveryDialog(
    photoAttached: Boolean,
    signed: Boolean,
    codAmount: Double,
    collectCod: Boolean,
    onCollectCodChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onSign: () -> Unit,
    onSkip: () -> Unit,
    onSave: () -> Unit,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.pod_title),
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = onSave,
        dismissLabel = stringResource(R.string.pod_skip),
        onDismissButton = onSkip,
    ) {
        Text(
            text = stringResource(R.string.pod_body),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        if (photoAttached) {
            Text(
                text = stringResource(R.string.fail_evidence_photo_attached),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (signed) {
            Text(
                text = stringResource(R.string.fail_evidence_signed),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (codAmount > 0.0) {
            Text(
                text = stringResource(R.string.cod_collect_label, "%.2f".format(codAmount)),
                style = MaterialTheme.typography.bodyMedium,
                color = IslandColors.onSurface,
            )
            OutlinedButton(
                onClick = { onCollectCodChange(!collectCod) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    stringResource(
                        if (collectCod) R.string.cod_collected else R.string.cod_mark_collected,
                    ),
                )
            }
        }
        OutlinedButton(
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.fail_evidence_photo))
        }
        OutlinedButton(
            onClick = onSign,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.fail_evidence_sign))
        }
    }
}

@Composable
fun ScanCodeDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    onScanCamera: () -> Unit,
) {
    var value by remember { mutableStateOf("") }
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.scan_title),
        confirmLabel = stringResource(R.string.scan_match),
        onConfirm = {
            if (value.isNotBlank()) onSubmit(value)
        },
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(
            text = stringResource(R.string.scan_body),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        SoftOutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = stringResource(R.string.scan_field),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = onScanCamera,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.scan_camera))
        }
    }
}

@Composable
fun NfcWriteDialog(
    nfcReady: Boolean,
    onDismiss: () -> Unit,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.nfc_write_title),
        dismissLabel = stringResource(R.string.action_close),
    ) {
        Text(
            text = stringResource(
                if (nfcReady) R.string.nfc_write_hold else R.string.nfc_unavailable,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
    }
}
