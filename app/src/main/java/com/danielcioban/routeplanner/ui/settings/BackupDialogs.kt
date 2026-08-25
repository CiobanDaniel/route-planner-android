package com.danielcioban.routeplanner.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.backup.BackupImportOptions
import com.danielcioban.routeplanner.data.backup.BackupPreview
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun BackupPasswordDialog(
    required: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.backup_password_title),
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = {
            if (!required || password.isNotEmpty()) onConfirm(password)
        },
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(
            text = stringResource(
                if (required) R.string.backup_password_needed else R.string.backup_password_body,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        SoftOutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.backup_password_field),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun SelectiveImportDialog(
    preview: BackupPreview?,
    onDismiss: () -> Unit,
    onImport: (BackupImportOptions) -> Unit,
) {
    var includeRoutes by remember { mutableStateOf(true) }
    var includeLibrary by remember { mutableStateOf(true) }
    var includeHistory by remember { mutableStateOf(true) }
    var includeTemplates by remember { mutableStateOf(true) }
    var includeFuel by remember { mutableStateOf(true) }
    fun options(keepLocal: Boolean) = BackupImportOptions(
        includeLibrary = includeLibrary,
        includeRoutes = includeRoutes,
        includeHistory = includeHistory,
        includeTemplates = includeTemplates,
        includeFuel = includeFuel,
        keepLocalOnConflict = keepLocal,
    )
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.backup_select_title),
        confirmLabel = stringResource(R.string.backup_use_file),
        onConfirm = { onImport(options(keepLocal = false)) },
        dismissLabel = if ((preview?.wouldOverwrite ?: 0) > 0) {
            stringResource(R.string.backup_keep_mine)
        } else {
            stringResource(R.string.action_cancel)
        },
        onDismissButton = {
            if ((preview?.wouldOverwrite ?: 0) > 0) {
                onImport(options(keepLocal = true))
            } else {
                onDismiss()
            }
        },
    ) {
        ImportChip(R.string.backup_select_routes, includeRoutes) { includeRoutes = it }
        ImportChip(R.string.backup_select_library, includeLibrary) { includeLibrary = it }
        ImportChip(R.string.backup_select_history, includeHistory) { includeHistory = it }
        ImportChip(R.string.backup_select_templates, includeTemplates) { includeTemplates = it }
        ImportChip(R.string.backup_select_fuel, includeFuel) { includeFuel = it }
        val overwrites = preview?.wouldOverwrite ?: 0
        if (overwrites > 0) {
            Text(
                text = stringResource(R.string.backup_conflict_body, overwrites),
                style = MaterialTheme.typography.bodyMedium,
                color = IslandColors.onSurface,
            )
        }
    }
}

@Composable
private fun ImportChip(labelRes: Int, selected: Boolean, onChange: (Boolean) -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { onChange(!selected) },
        label = { Text(stringResource(labelRes)) },
    )
}
