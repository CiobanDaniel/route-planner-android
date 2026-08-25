package com.danielcioban.routeplanner.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.BuildConfig
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.account.AccountSession
import com.danielcioban.routeplanner.data.account.AccountSessionStore
import com.danielcioban.routeplanner.data.account.isDebugDeveloper
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.layout.AppPanes
import com.danielcioban.routeplanner.ui.layout.readableWidth
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.map.RouteMapBackdrop
import com.danielcioban.routeplanner.ui.menu.ScreenMenuButton
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    var previewName by rememberSaveable { mutableStateOf("") }
    var previewEmail by rememberSaveable { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        RouteMapBackdrop(stops = emptyList())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FloatingCircleButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = IslandColors.onSurface,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                ScreenMenuButton(onClick = onOpenMenu, embedded = false)
                Spacer(modifier = Modifier.width(12.dp))
                FloatingIsland(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = AppPanes.TitleMaxWidth),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = 16.dp,
                ) {
                    Text(
                        text = stringResource(R.string.account_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = IslandColors.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
            FloatingIsland(
                modifier = Modifier.readableWidth(),
                shape = RoundedCornerShape(28.dp),
                contentPadding = 20.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (val current = session) {
                        AccountSession.SignedOut -> {
                            Text(
                                text = stringResource(R.string.account_signed_out_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = IslandColors.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.account_signed_out_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = IslandColors.onSurfaceMuted,
                            )
                            Button(
                                onClick = { },
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Text(stringResource(R.string.account_google_sign_in))
                            }
                            Text(
                                text = stringResource(R.string.account_google_privacy),
                                style = MaterialTheme.typography.bodySmall,
                                color = IslandColors.onSurfaceMuted,
                            )
                            if (BuildConfig.DEBUG) {
                                Text(
                                    text = stringResource(R.string.account_preview_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IslandColors.onSurfaceMuted,
                                )
                                SoftOutlinedTextField(
                                    value = previewName,
                                    onValueChange = { previewName = it },
                                    label = stringResource(R.string.account_display_name),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                SoftOutlinedTextField(
                                    value = previewEmail,
                                    onValueChange = { previewEmail = it },
                                    label = stringResource(R.string.account_email),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Button(
                                    onClick = {
                                        viewModel.previewSignIn(previewName, previewEmail)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = previewEmail.trim().isNotEmpty(),
                                    shape = RoundedCornerShape(16.dp),
                                ) {
                                    Text(stringResource(R.string.account_preview_sign_in))
                                }
                                Text(
                                    text = stringResource(R.string.account_developer_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IslandColors.onSurfaceMuted,
                                )
                                OutlinedButton(
                                    onClick = { viewModel.signInDeveloper() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                ) {
                                    Text(stringResource(R.string.account_developer_sign_in))
                                }
                            }
                        }
                        is AccountSession.SignedIn -> {
                            Text(
                                text = stringResource(R.string.account_signed_in_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = IslandColors.onSurface,
                            )
                            Text(
                                text = current.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                color = IslandColors.onSurface,
                            )
                            Text(
                                text = current.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = IslandColors.onSurfaceMuted,
                            )
                            Text(
                                text = stringResource(
                                    R.string.account_signed_in_provider,
                                    accountProviderLabel(current.providerId),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = IslandColors.onSurfaceMuted,
                            )
                            Text(
                                text = stringResource(R.string.account_signed_in_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = IslandColors.onSurfaceMuted,
                            )
                            if (current.isDebugDeveloper()) {
                                Text(
                                    text = stringResource(R.string.account_developer_active),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = IslandColors.onSurface,
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.signOut() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Text(
                                    text = if (current.providerId == AccountSessionStore.GOOGLE_PROVIDER) {
                                        stringResource(R.string.account_google_sign_out)
                                    } else {
                                        stringResource(R.string.menu_logout)
                                    },
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun accountProviderLabel(providerId: String): String = when (providerId) {
    AccountSessionStore.GOOGLE_PROVIDER -> stringResource(R.string.account_provider_google)
    AccountSessionStore.DEVELOPER_PROVIDER -> stringResource(R.string.account_provider_developer)
    AccountSessionStore.PREVIEW_PROVIDER -> stringResource(R.string.account_provider_preview)
    else -> providerId
}

/** Reserved copy for Google Sign-In UI once Bucket 12 is wired. */
@Suppress("unused")
private val reservedGoogleAuthCopy = intArrayOf(
    R.string.account_google_connecting,
    R.string.account_google_failed,
    R.string.account_google_body,
    R.string.account_google_unavailable,
    R.string.account_cloud_sync_soon,
    R.string.account_unlink,
    R.string.account_not_in_this_build,
)
