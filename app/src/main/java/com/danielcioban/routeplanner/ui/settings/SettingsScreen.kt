package com.danielcioban.routeplanner.ui.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.settings.AppLanguage
import com.danielcioban.routeplanner.data.settings.DistanceUnit
import com.danielcioban.routeplanner.data.settings.ThemeMode
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.map.RouteMapBackdrop
import com.danielcioban.routeplanner.ui.theme.IslandColors

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

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
                Spacer(modifier = Modifier.width(12.dp))
                FloatingIsland(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = 16.dp,
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = IslandColors.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FloatingIsland(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                shape = RoundedCornerShape(28.dp),
                contentPadding = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    SettingsSection(title = stringResource(R.string.settings_appearance))
                    ThemeMode.entries.forEach { mode ->
                        SettingsChoiceRow(
                            label = themeLabel(mode),
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        color = IslandColors.fieldBorder.copy(alpha = 0.35f),
                    )

                    SettingsSection(title = stringResource(R.string.settings_language))
                    AppLanguage.entries.forEach { language ->
                        SettingsChoiceRow(
                            label = languageLabel(language),
                            selected = settings.language == language,
                            onClick = { viewModel.setLanguage(language) },
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        color = IslandColors.fieldBorder.copy(alpha = 0.35f),
                    )

                    SettingsSection(title = stringResource(R.string.settings_units))
                    DistanceUnit.entries.forEach { unit ->
                        SettingsChoiceRow(
                            label = unitLabel(unit),
                            selected = settings.distanceUnit == unit,
                            onClick = { viewModel.setDistanceUnit(unit) },
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        color = IslandColors.fieldBorder.copy(alpha = 0.35f),
                    )

                    SettingsSection(title = stringResource(R.string.settings_navigation))
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_keep_screen_on),
                        subtitle = stringResource(R.string.settings_keep_screen_on_hint),
                        checked = settings.keepScreenOnDuringNav,
                        onCheckedChange = viewModel::setKeepScreenOnDuringNav,
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_footer_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = IslandColors.onSurfaceMuted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
private fun SettingsChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = IslandColors.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = IslandColors.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.onSurfaceMuted,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
}

@Composable
private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.ENGLISH -> stringResource(R.string.language_english)
    AppLanguage.ROMANIAN -> stringResource(R.string.language_romanian)
}

@Composable
private fun unitLabel(unit: DistanceUnit): String = when (unit) {
    DistanceUnit.METRIC -> stringResource(R.string.units_metric)
    DistanceUnit.IMPERIAL -> stringResource(R.string.units_imperial)
}
