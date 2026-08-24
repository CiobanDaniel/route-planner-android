package com.danielcioban.routeplanner.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.account.AccountSession
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.components.IslandListDivider
import com.danielcioban.routeplanner.ui.components.IslandListItem
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun AppMenuPanel(
    accountSession: AccountSession,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onStopLibrary: () -> Unit,
    onAccount: () -> Unit,
    onProfile: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val signedIn = accountSession is AccountSession.SignedIn
    val accountSubtitle = when (accountSession) {
        AccountSession.SignedOut -> stringResource(R.string.account_menu_signed_out)
        is AccountSession.SignedIn -> accountSession.email
    }
    val profileSubtitle = when (accountSession) {
        AccountSession.SignedOut -> stringResource(R.string.coming_soon)
        is AccountSession.SignedIn -> accountSession.displayName
    }

    FloatingIsland(
        modifier = modifier.width(280.dp),
        shape = RoundedCornerShape(24.dp),
        contentPadding = 8.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.menu_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = IslandColors.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
            MenuRow(
                icon = Icons.Default.Settings,
                label = stringResource(R.string.menu_settings),
                onClick = onSettings,
            )
            MenuRow(
                icon = Icons.Default.Place,
                label = stringResource(R.string.menu_stop_library),
                onClick = onStopLibrary,
            )
            MenuRow(
                icon = Icons.Default.Info,
                label = stringResource(R.string.menu_about),
                onClick = onAbout,
            )
            MenuRow(
                icon = Icons.Default.AccountCircle,
                label = stringResource(R.string.menu_account),
                subtitle = accountSubtitle,
                onClick = onAccount,
            )
            MenuRow(
                icon = Icons.Default.Person,
                label = stringResource(R.string.menu_profile),
                subtitle = profileSubtitle,
                enabled = signedIn,
                onClick = onProfile,
            )
            IslandListDivider()
            if (signedIn) {
                MenuRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = stringResource(R.string.menu_logout),
                    onClick = onLogout,
                )
            } else {
                MenuRow(
                    icon = Icons.AutoMirrored.Filled.Login,
                    label = stringResource(R.string.menu_login),
                    onClick = onLogin,
                )
            }
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
    IslandListItem(onClick = onClick, enabled = enabled) {
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
