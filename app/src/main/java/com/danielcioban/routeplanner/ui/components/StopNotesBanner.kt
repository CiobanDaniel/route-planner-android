package com.danielcioban.routeplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun StopNotesBanner(
    notes: String,
    addressHint: String = "",
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    phone: String = "",
    doorCode: String = "",
    extraLine: String = "",
) {
    val body = buildString {
        if (addressHint.isNotBlank()) append(addressHint.trim())
        if (phone.isNotBlank()) {
            if (isNotEmpty()) append('\n')
            append(phone.trim())
        }
        if (doorCode.isNotBlank()) {
            if (isNotEmpty()) append('\n')
            append(doorCode.trim())
        }
        if (extraLine.isNotBlank()) {
            if (isNotEmpty()) append('\n')
            append(extraLine.trim())
        }
        if (notes.isNotBlank()) {
            if (isNotEmpty()) append('\n')
            append(notes.trim())
        }
    }
    if (body.isBlank()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = IslandColors.noteFill,
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.StickyNote2,
            contentDescription = null,
            tint = IslandColors.badge,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.notes_banner_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = IslandColors.badge,
            )
            Text(
                text = body,
                style = if (compact) {
                    MaterialTheme.typography.bodySmall
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = IslandColors.onSurface,
                maxLines = if (compact) 3 else 6,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
