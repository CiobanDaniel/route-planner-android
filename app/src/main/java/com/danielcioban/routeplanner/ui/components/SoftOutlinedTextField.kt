package com.danielcioban.routeplanner.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun SoftOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    placeholder: String? = null,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = IslandColors.onSurfaceMuted) },
        placeholder = placeholder?.let { { Text(it, color = IslandColors.onSurfaceMuted.copy(alpha = 0.65f)) } },
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        shape = RoundedCornerShape(16.dp),
        colors = islandTextFieldColors(),
    )
}

@Composable
fun islandTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = IslandColors.onSurface,
    unfocusedTextColor = IslandColors.onSurface,
    disabledTextColor = IslandColors.onSurfaceMuted,
    focusedLabelColor = IslandColors.onSurfaceMuted,
    unfocusedLabelColor = IslandColors.onSurfaceMuted,
    cursorColor = IslandColors.fieldBorderFocused,
    focusedBorderColor = IslandColors.fieldBorderFocused,
    unfocusedBorderColor = IslandColors.fieldBorder,
    focusedContainerColor = IslandColors.rowSurface,
    unfocusedContainerColor = IslandColors.rowSurface,
    disabledContainerColor = IslandColors.rowSurface,
)
