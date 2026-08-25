package com.danielcioban.routeplanner.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.account.AccountSession
import com.danielcioban.routeplanner.data.account.isDebugDeveloper
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
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
    onTripHistory: () -> Unit,
    onTrash: () -> Unit,
    onStats: () -> Unit,
    onFuelLog: () -> Unit,
    onAccount: () -> Unit,
    onProfile: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val signedIn = accountSession is AccountSession.SignedIn
    val accountSubtitle = when {
        accountSession.isDebugDeveloper() -> stringResource(R.string.account_developer_menu)
        accountSession is AccountSession.SignedOut -> stringResource(R.string.account_menu_signed_out)
        accountSession is AccountSession.SignedIn -> accountSession.email
        else -> stringResource(R.string.account_menu_signed_out)
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
                icon = Icons.Default.History,
                label = stringResource(R.string.menu_trip_history),
                onClick = onTripHistory,
            )
            MenuRow(
                icon = Icons.Default.Delete,
                label = stringResource(R.string.menu_trash),
                onClick = onTrash,
            )
            MenuRow(
                icon = Icons.Default.Place,
                label = stringResource(R.string.menu_stats),
                onClick = onStats,
            )
            MenuRow(
                icon = Icons.Default.Place,
                label = stringResource(R.string.menu_fuel),
                onClick = onFuelLog,
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
                IslandColors.onSurfaceMuted
            } else {
                IslandColors.onSurfaceMuted.copy(alpha = 0.45f)
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

@Composable
fun ScreenMenuButton(
    onClick: () -> Unit,
    embedded: Boolean = true,
) {
    FloatingCircleButton(onClick = onClick, embedded = embedded) {
        Icon(
            Icons.Default.Menu,
            contentDescription = stringResource(R.string.menu_open),
            tint = IslandColors.onSurface,
        )
    }
}

@Composable
fun AppMenuOverlay(
    visible: Boolean,
    accountSession: AccountSession,
    onDismiss: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onStopLibrary: () -> Unit,
    onTripHistory: () -> Unit,
    onTrash: () -> Unit,
    onStats: () -> Unit,
    onFuelLog: () -> Unit,
    onAccount: () -> Unit,
    onLogout: () -> Unit,
) {
    if (!visible) return
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_menu_overlay")
            .background(IslandColors.scrim.copy(alpha = 0.28f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        AppMenuPanel(
            accountSession = accountSession,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 72.dp, end = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            onSettings = {
                onDismiss()
                onSettings()
            },
            onAbout = {
                onDismiss()
                onAbout()
            },
            onStopLibrary = {
                onDismiss()
                onStopLibrary()
            },
            onTripHistory = {
                onDismiss()
                onTripHistory()
            },
            onTrash = {
                onDismiss()
                onTrash()
            },
            onStats = {
                onDismiss()
                onStats()
            },
            onFuelLog = {
                onDismiss()
                onFuelLog()
            },
            onAccount = {
                onDismiss()
                onAccount()
            },
            onProfile = {
                onDismiss()
                onAccount()
            },
            onLogin = {
                onDismiss()
                onAccount()
            },
            onLogout = {
                onDismiss()
                onLogout()
            },
        )
    }
}
