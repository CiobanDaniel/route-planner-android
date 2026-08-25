package com.danielcioban.routeplanner.ui.about

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.BuildConfig
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.layout.AppPanes
import com.danielcioban.routeplanner.ui.layout.readableWidth
import com.danielcioban.routeplanner.ui.map.RouteMapBackdrop
import com.danielcioban.routeplanner.ui.menu.ScreenMenuButton
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.ProblemReport

@Composable
fun AboutScreen(
    appVersion: String,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
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
                        text = stringResource(R.string.about_title),
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
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = IslandColors.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.about_version, appVersion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = IslandColors.onSurfaceMuted,
                    )
                    Text(
                        text = stringResource(R.string.about_blurb),
                        style = MaterialTheme.typography.bodyLarge,
                        color = IslandColors.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.about_maps_heading),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = IslandColors.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.about_maps_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = IslandColors.onSurfaceMuted,
                    )
                    Text(
                        text = stringResource(R.string.about_routing_heading),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = IslandColors.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.about_routing_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = IslandColors.onSurfaceMuted,
                    )
                    Text(
                        text = stringResource(R.string.about_license),
                        style = MaterialTheme.typography.bodySmall,
                        color = IslandColors.onSurfaceMuted,
                    )
                    val uriHandler = LocalUriHandler.current
                    val context = LocalContext.current
                    TextButton(onClick = { uriHandler.openUri(BuildConfig.PRIVACY_POLICY_URL) }) {
                        Text(stringResource(R.string.privacy_policy))
                    }
                    TextButton(onClick = { ProblemReport.start(context) }) {
                        Text(stringResource(R.string.report_problem))
                    }
                }
            }
            }
        }
    }
}
