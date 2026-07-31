package com.danielcioban.routeplanner.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun AppMenuPanel(
    onSettings: () -> Unit,
    onAccount: () -> Unit,
    onProfile: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingIsland(
        modifier = modifier.width(280.dp),
        shape = RoundedCornerShape(24.dp),
        contentPadding = 8.dp,
    ) {
        Column {
            Text(
                text = stringResource(R.string.menu_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = IslandColors.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
            MenuRow(
                icon = Icons.Default.Settings,
                label = stringResource(R.string.menu_settings),
                onClick = onSettings,
            )
            MenuRow(
                icon = Icons.Default.AccountCircle,
                label = stringResource(R.string.menu_account),
                subtitle = stringResource(R.string.coming_soon),
                enabled = false,
                onClick = onAccount,
            )
            MenuRow(
                icon = Icons.Default.Person,
                label = stringResource(R.string.menu_profile),
                subtitle = stringResource(R.string.coming_soon),
                enabled = false,
                onClick = onProfile,
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                color = IslandColors.fieldBorder.copy(alpha = 0.35f),
            )
            MenuRow(
                icon = Icons.AutoMirrored.Filled.Login,
                label = stringResource(R.string.menu_login),
                subtitle = stringResource(R.string.coming_soon),
                enabled = false,
                onClick = onLogin,
            )
            MenuRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                label = stringResource(R.string.menu_logout),
                subtitle = stringResource(R.string.coming_soon),
                enabled = false,
                onClick = onLogout,
            )
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                IslandColors.onSurfaceMuted.copy(alpha = 0.55f)
            },
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) IslandColors.onSurface else IslandColors.onSurfaceMuted.copy(alpha = 0.7f),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = IslandColors.onSurfaceMuted,
                )
            }
        }
        if (enabled) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = IslandColors.onSurfaceMuted,
            )
        }
    }
}
