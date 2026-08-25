package com.danielcioban.routeplanner.ui.drive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.delivery.DeliverySession
import com.danielcioban.routeplanner.data.delivery.DriveKind
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.theme.IslandColors

data class DriveHereTarget(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val libraryStopId: Long? = null,
    val addressHint: String = "",
)

@Composable
fun DriveHereConfirmDialog(
    target: DriveHereTarget,
    onDismiss: () -> Unit,
    onConfirm: (name: String, saveToLibrary: Boolean) -> Unit,
) {
    val fallbackName = stringResource(R.string.drive_here_unnamed)
    var name by remember(target.latitude, target.longitude, target.name) {
        mutableStateOf(target.name.ifBlank { fallbackName })
    }
    var saveToLibrary by remember(target.libraryStopId) {
        mutableStateOf(target.libraryStopId == null)
    }
    val fromLibrary = target.libraryStopId != null

    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.drive_here_title),
        confirmLabel = stringResource(R.string.drive_here_confirm),
        onConfirm = {
            onConfirm(name.trim().ifBlank { fallbackName }, !fromLibrary && saveToLibrary)
        },
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(
            text = stringResource(R.string.drive_here_body),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        Text(
            text = "${"%.5f".format(target.latitude)}, ${"%.5f".format(target.longitude)}",
            style = MaterialTheme.typography.bodySmall,
            color = IslandColors.onSurfaceMuted,
        )
        if (target.addressHint.isNotBlank()) {
            Text(
                text = target.addressHint,
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.onSurfaceMuted,
            )
        }
        SoftOutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.dialog_stop_name),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!fromLibrary) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = saveToLibrary,
                    onCheckedChange = { saveToLibrary = it },
                )
                Text(
                    text = stringResource(R.string.drive_here_save_library),
                    style = MaterialTheme.typography.bodyMedium,
                    color = IslandColors.onSurface,
                )
            }
        }
    }
}

@Composable
fun ReturnToDrivingBar(
    session: DeliverySession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val named = session.kind == DriveKind.QUICK
    val destName = session.quickName.orEmpty().ifBlank {
        stringResource(R.string.drive_here_unnamed)
    }
    FloatingIsland(
        modifier = modifier
            .navigationBarsPadding()
            .padding(16.dp),
        shape = RoundedCornerShape(22.dp),
        contentPadding = 12.dp,
    ) {
        Row(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Default.Navigation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = if (named) {
                    stringResource(R.string.return_to_driving_named, destName)
                } else {
                    stringResource(R.string.return_to_driving)
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = IslandColors.onSurface,
            )
        }
    }
}
