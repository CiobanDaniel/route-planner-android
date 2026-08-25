package com.danielcioban.routeplanner.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.BuildConfig
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.ServiceEndpoints
import com.danielcioban.routeplanner.data.settings.AppLanguage
import com.danielcioban.routeplanner.data.settings.ClockFormat
import com.danielcioban.routeplanner.data.settings.DistanceUnit
import com.danielcioban.routeplanner.data.settings.GeofenceAction
import com.danielcioban.routeplanner.data.settings.GeofenceDwell
import com.danielcioban.routeplanner.data.settings.RerouteAggressiveness
import com.danielcioban.routeplanner.data.settings.StopGeofence
import com.danielcioban.routeplanner.data.settings.ThemeMode
import com.danielcioban.routeplanner.data.settings.TtsVerbosity
import com.danielcioban.routeplanner.data.settings.VehicleProfile
import com.danielcioban.routeplanner.ui.components.AddressSearchDialog
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.components.IslandListDivider
import com.danielcioban.routeplanner.ui.components.IslandListItem
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.layout.AppPanes
import com.danielcioban.routeplanner.ui.layout.readableWidth
import com.danielcioban.routeplanner.ui.location.rememberUserLocation
import com.danielcioban.routeplanner.ui.menu.ScreenMenuButton
import com.danielcioban.routeplanner.ui.map.MapAssetReload
import com.danielcioban.routeplanner.ui.map.RouteMapBackdrop
import com.danielcioban.routeplanner.ui.places.CsvImportTargetDialog
import com.danielcioban.routeplanner.ui.routes.GeofenceRadiusField
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.PlaceShare
import com.danielcioban.routeplanner.util.ProblemReport
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
    onOpenTrash: () -> Unit = {},
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val importResult by viewModel.importResult.collectAsStateWithLifecycle()
    val importFailed by viewModel.importFailed.collectAsStateWithLifecycle()
    val csvResult by viewModel.csvResult.collectAsStateWithLifecycle()
    val csvFailed by viewModel.csvFailed.collectAsStateWithLifecycle()
    val backupSaved by viewModel.backupSaved.collectAsStateWithLifecycle()
    val passwordWrong by viewModel.passwordWrong.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val vacuumRemoved by viewModel.vacuumRemoved.collectAsStateWithLifecycle()
    val health by viewModel.health.collectAsStateWithLifecycle()
    val healthBusy by viewModel.healthBusy.collectAsStateWithLifecycle()
    val templates by viewModel.taskTemplates.collectAsStateWithLifecycle()
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    var newTemplateTitle by remember { mutableStateOf("") }
    var newTemplateRequired by remember { mutableStateOf(false) }
    var pendingCsvUri by remember { mutableStateOf<Uri?>(null) }
    var pickingCsvTarget by remember { mutableStateOf(false) }
    var showHomeSearch by remember { mutableStateOf(false) }
    var requestHomeGps by remember { mutableStateOf(false) }
    var pendingImportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var decodedJson by remember { mutableStateOf<String?>(null) }
    var askExportPassword by remember { mutableStateOf(false) }
    var exportIsShare by remember { mutableStateOf(false) }
    var pendingExportPassword by remember { mutableStateOf("") }
    var askImportPassword by remember { mutableStateOf(false) }
    var showSelectiveImport by remember { mutableStateOf(false) }
    val homeGps = rememberUserLocation(autoRequest = false)
    val homeFallbackName = stringResource(R.string.settings_home_name)
    var openGroup by rememberSaveable { mutableStateOf(SettingsGroupId.Display.name) }

    LaunchedEffect(requestHomeGps, homeGps.coordinate) {
        if (!requestHomeGps) return@LaunchedEffect
        if (!homeGps.hasPermission) {
            homeGps.requestPermission()
            return@LaunchedEffect
        }
        val fix = homeGps.coordinate
        if (fix == null) {
            homeGps.refresh()
            return@LaunchedEffect
        }
        viewModel.setHome(
            fix.latitude,
            fix.longitude,
            settings.homeName.ifBlank { homeFallbackName },
        )
        requestHomeGps = false
    }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { picked ->
            val bytes = com.danielcioban.routeplanner.data.backup.AutoBackup.readUri(context, picked)
            if (bytes == null) {
                viewModel.clearImportFeedback()
            } else {
                pendingImportBytes = bytes
                if (com.danielcioban.routeplanner.data.backup.EncryptedBackup.isEncrypted(bytes)) {
                    askImportPassword = true
                } else {
                    val json = viewModel.decodeBackup(bytes, null)
                    if (json != null) {
                        decodedJson = json
                        viewModel.previewBackup(json)
                        showSelectiveImport = true
                    }
                }
            }
        }
    }
    val saveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val password = pendingExportPassword
        pendingExportPassword = ""
        uri?.let { viewModel.saveBackupToUri(context, it, password) }
    }
    val saveEncryptedBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val password = pendingExportPassword
        pendingExportPassword = ""
        uri?.let { viewModel.saveBackupToUri(context, it, password) }
    }
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let { viewModel.setBackupFolder(context, it) }
    }
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            viewModel.importCsv(
                context,
                it,
                context.getString(R.string.csv_import_default_route),
            )
        }
    }
    val csvIntoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            pendingCsvUri = it
            pickingCsvTarget = true
        }
    }

    LaunchedEffect(importResult, importFailed, csvResult, csvFailed, backupSaved, vacuumRemoved, passwordWrong) {
        if (importResult != null || importFailed || csvResult != null || csvFailed ||
            backupSaved || vacuumRemoved != null || passwordWrong
        ) {
            kotlinx.coroutines.delay(8_000)
            viewModel.clearImportFeedback()
        }
    }

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
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = IslandColors.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
            FloatingIsland(
                modifier = Modifier
                    .readableWidth()
                    .fillMaxHeight(),
                shape = RoundedCornerShape(28.dp),
                contentPadding = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    SettingsAccordion(
                        title = stringResource(R.string.settings_group_display),
                        summary = listOf(
                            themeLabel(settings.themeMode),
                            languageLabel(settings.language),
                            unitLabel(settings.distanceUnit),
                        ).joinToString(" · "),
                        expanded = openGroup == SettingsGroupId.Display.name,
                        onToggle = {
                            openGroup = if (openGroup == SettingsGroupId.Display.name) {
                                ""
                            } else {
                                SettingsGroupId.Display.name
                            }
                        },
                    ) {
                    SettingsDropdownRow(
                        label = stringResource(R.string.settings_appearance),
                        selected = settings.themeMode,
                        options = ThemeMode.entries.toList(),
                        optionLabel = { themeLabel(it) },
                        onSelected = viewModel::setThemeMode,
                    )
                    SettingsDropdownRow(
                        label = stringResource(R.string.settings_language),
                        selected = settings.language,
                        options = AppLanguage.entries.toList(),
                        optionLabel = { languageLabel(it) },
                        onSelected = viewModel::setLanguage,
                    )
                    SettingsDropdownRow(
                        label = stringResource(R.string.settings_units),
                        selected = settings.distanceUnit,
                        options = DistanceUnit.entries.toList(),
                        optionLabel = { unitLabel(it) },
                        onSelected = viewModel::setDistanceUnit,
                    )
                    SettingsDropdownRow(
                        label = stringResource(R.string.settings_clock),
                        selected = settings.clockFormat,
                        options = ClockFormat.entries.toList(),
                        optionLabel = { clockLabel(it) },
                        onSelected = viewModel::setClockFormat,
                    )
                        SettingsSwitchRow(
                            label = stringResource(R.string.settings_reduce_motion),
                            subtitle = stringResource(R.string.settings_reduce_motion_hint),
                            checked = settings.reduceMotion,
                            onCheckedChange = viewModel::setReduceMotion,
                        )
                        SettingsSwitchRow(
                            label = stringResource(R.string.settings_high_contrast_pins),
                            subtitle = stringResource(R.string.settings_high_contrast_pins_hint),
                            checked = settings.highContrastPins,
                            onCheckedChange = viewModel::setHighContrastPins,
                        )
                    }

                    SettingsAccordion(
                        title = stringResource(R.string.settings_navigation),
                        summary = buildList {
                            if (settings.speakManeuvers) {
                                add(stringResource(R.string.settings_speak_maneuvers))
                            }
                            if (settings.defaultFollowMe) {
                                add(stringResource(R.string.settings_default_follow))
                            }
                            add(rerouteLabel(settings.rerouteAggressiveness))
                        }.joinToString(" · "),
                        expanded = openGroup == SettingsGroupId.Navigation.name,
                        onToggle = {
                            openGroup = if (openGroup == SettingsGroupId.Navigation.name) {
                                ""
                            } else {
                                SettingsGroupId.Navigation.name
                            }
                        },
                    ) {
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_keep_screen_on),
                        subtitle = stringResource(R.string.settings_keep_screen_on_hint),
                        checked = settings.keepScreenOnDuringNav,
                        onCheckedChange = viewModel::setKeepScreenOnDuringNav,
                    )
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_keep_screen_moving),
                        subtitle = stringResource(R.string.settings_keep_screen_moving_hint),
                        checked = settings.keepScreenOnOnlyWhileMoving,
                        onCheckedChange = viewModel::setKeepScreenOnOnlyWhileMoving,
                    )
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_speak_maneuvers),
                        subtitle = stringResource(R.string.settings_speak_maneuvers_hint),
                        checked = settings.speakManeuvers,
                        onCheckedChange = viewModel::setSpeakManeuvers,
                    )
                    SettingsDropdownRow(
                        label = stringResource(R.string.settings_tts_verbosity),
                        selected = settings.ttsVerbosity,
                        options = TtsVerbosity.entries.toList(),
                        optionLabel = { ttsVerbosityLabel(it) },
                        onSelected = viewModel::setTtsVerbosity,
                    )
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_mute_calls),
                        subtitle = stringResource(R.string.settings_mute_calls_hint),
                        checked = settings.muteTtsDuringCalls,
                        onCheckedChange = viewModel::setMuteTtsDuringCalls,
                    )
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_volume_key_done),
                        subtitle = stringResource(R.string.settings_volume_key_done_hint),
                        checked = settings.volumeKeyDone,
                        onCheckedChange = viewModel::setVolumeKeyDone,
                    )
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_north_up),
                        subtitle = stringResource(R.string.settings_north_up_hint),
                        checked = settings.northUpWhileDriving,
                        onCheckedChange = viewModel::setNorthUpWhileDriving,
                    )
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_default_follow),
                        subtitle = stringResource(R.string.settings_default_follow_hint),
                        checked = settings.defaultFollowMe,
                        onCheckedChange = viewModel::setDefaultFollowMe,
                    )
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_default_round_trip),
                        subtitle = stringResource(R.string.settings_default_round_trip_hint),
                        checked = settings.defaultRoundTrip,
                        onCheckedChange = viewModel::setDefaultRoundTrip,
                    )
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_data_saver),
                        subtitle = stringResource(R.string.settings_data_saver_hint),
                        checked = settings.dataSaver,
                        onCheckedChange = viewModel::setDataSaver,
                    )
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_walk_last_mile),
                        subtitle = stringResource(R.string.settings_walk_last_mile_hint),
                        checked = settings.walkLastMile,
                        onCheckedChange = viewModel::setWalkLastMile,
                    )
                    SettingsDropdownRow(
                        label = stringResource(R.string.settings_reroute),
                        selected = settings.rerouteAggressiveness,
                        options = RerouteAggressiveness.entries.toList(),
                        optionLabel = { rerouteLabel(it) },
                        onSelected = viewModel::setRerouteAggressiveness,
                    )
                    OutlinedButton(
                        onClick = {
                            MapAssetReload.bump()
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.settings_reload_map_done),
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.settings_reload_map))
                    }
                    }

                    SettingsAccordion(
                        title = stringResource(R.string.settings_depot),
                        summary = if (settings.hasHome) {
                            settings.homeName.ifBlank {
                                stringResource(
                                    R.string.settings_home_set,
                                    "${"%.4f".format(settings.homeLatitude)}",
                                )
                            }
                        } else {
                            stringResource(R.string.home_not_set)
                        },
                        expanded = openGroup == SettingsGroupId.Home.name,
                        onToggle = {
                            openGroup = if (openGroup == SettingsGroupId.Home.name) {
                                ""
                            } else {
                                SettingsGroupId.Home.name
                            }
                        },
                    ) {
                    Text(
                        text = if (settings.hasHome) {
                            stringResource(
                                R.string.settings_home_set,
                                settings.homeName.ifBlank {
                                    "${"%.5f".format(settings.homeLatitude)} , ${"%.5f".format(settings.homeLongitude)}"
                                },
                            )
                        } else {
                            stringResource(R.string.home_not_set)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = IslandColors.onSurfaceMuted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                    SoftOutlinedTextField(
                        value = settings.homeName,
                        onValueChange = { name ->
                            val lat = settings.homeLatitude
                            val lng = settings.homeLongitude
                            if (lat != null && lng != null) {
                                viewModel.setHome(lat, lng, name)
                            }
                        },
                        label = stringResource(R.string.settings_home_name),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                    OutlinedButton(
                        onClick = { showHomeSearch = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.settings_home_search))
                    }
                    OutlinedButton(
                        onClick = { requestHomeGps = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.settings_home_set_gps))
                    }
                    if (settings.hasHome) {
                        TextButton(
                            onClick = viewModel::clearHome,
                            modifier = Modifier.padding(horizontal = 6.dp),
                        ) {
                            Text(stringResource(R.string.settings_home_clear))
                        }
                    }
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_use_home_origin),
                        subtitle = stringResource(R.string.settings_use_home_origin_hint),
                        checked = settings.useHomeAsOrigin,
                        onCheckedChange = viewModel::setUseHomeAsOrigin,
                    )
                    SoftOutlinedTextField(
                        value = settings.dispatcherVanName,
                        onValueChange = viewModel::setDispatcherVanName,
                        label = stringResource(R.string.settings_dispatcher_van),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                    Text(
                        text = stringResource(R.string.settings_dispatcher_van_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = IslandColors.onSurfaceMuted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                    )
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_ask_pod),
                        subtitle = stringResource(R.string.settings_ask_pod_hint),
                        checked = settings.askProofOnDone,
                        onCheckedChange = viewModel::setAskProofOnDone,
                    )
                    }

                    SettingsAccordion(
                        title = stringResource(R.string.settings_vehicle),
                        summary = vehicleLabel(settings.vehicleProfile),
                        expanded = openGroup == SettingsGroupId.Vehicle.name,
                        onToggle = {
                            openGroup = if (openGroup == SettingsGroupId.Vehicle.name) {
                                ""
                            } else {
                                SettingsGroupId.Vehicle.name
                            }
                        },
                    ) {
                    Text(
                        text = stringResource(R.string.settings_vehicle_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = IslandColors.onSurfaceMuted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                    SettingsDropdownRow(
                        label = stringResource(R.string.settings_vehicle),
                        selected = settings.vehicleProfile,
                        options = VehicleProfile.entries.toList(),
                        optionLabel = { vehicleLabel(it) },
                        onSelected = viewModel::setVehicleProfile,
                    )
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_avoid_tolls),
                        subtitle = stringResource(R.string.settings_avoid_tolls_hint),
                        checked = settings.avoidTolls,
                        onCheckedChange = viewModel::setAvoidTolls,
                    )
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_avoid_motorways),
                        subtitle = stringResource(R.string.settings_avoid_motorways_hint),
                        checked = settings.avoidMotorways,
                        onCheckedChange = viewModel::setAvoidMotorways,
                    )
                    }

                    SettingsAccordion(
                        title = stringResource(R.string.settings_services),
                        summary = ServiceEndpoints.hostLabel(ServiceEndpoints.osrmBaseUrl),
                        expanded = openGroup == SettingsGroupId.Services.name,
                        onToggle = {
                            openGroup = if (openGroup == SettingsGroupId.Services.name) {
                                ""
                            } else {
                                SettingsGroupId.Services.name
                            }
                        },
                    ) {
                    Text(
                        text = stringResource(
                            R.string.settings_services_routing_host,
                            ServiceEndpoints.hostLabel(ServiceEndpoints.osrmBaseUrl),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = IslandColors.onSurfaceMuted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                    val searchHost = ServiceEndpoints.hostLabel(ServiceEndpoints.nominatimBaseUrl)
                    Text(
                        text = stringResource(R.string.settings_services_search_host, searchHost),
                        style = MaterialTheme.typography.bodySmall,
                        color = IslandColors.onSurfaceMuted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                    if (ServiceEndpoints.osrmFallbackUrl.isNotBlank()) {
                        Text(
                            text = stringResource(
                                R.string.settings_services_fallback_host,
                                ServiceEndpoints.hostLabel(ServiceEndpoints.osrmFallbackUrl),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = IslandColors.onSurfaceMuted,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }
                    if (ServiceEndpoints.nominatimFallbackUrl.isNotBlank()) {
                        Text(
                            text = stringResource(
                                R.string.settings_services_fallback_search_host,
                                ServiceEndpoints.hostLabel(ServiceEndpoints.nominatimFallbackUrl),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = IslandColors.onSurfaceMuted,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }
                    if (ServiceEndpoints.usesPublicDemoOsrm ||
                        ServiceEndpoints.usesPublicDemoNominatim
                    ) {
                        Text(
                            text = stringResource(R.string.settings_services_demo_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = IslandColors.onSurfaceMuted,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = viewModel::checkServices,
                        enabled = !healthBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            stringResource(
                                if (healthBusy) R.string.settings_services_checking
                                else R.string.settings_services_check,
                            ),
                        )
                    }
                    health?.let { snap ->
                        Text(
                            text = stringResource(
                                when {
                                    snap.routingOk && snap.routingUsedFallback ->
                                        R.string.settings_services_routing_fallback
                                    snap.routingOk -> R.string.settings_services_routing_ok
                                    else -> R.string.settings_services_routing_fail
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (snap.routingOk) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                        )
                        Text(
                            text = stringResource(
                                when {
                                    snap.searchOk && snap.searchUsedFallback ->
                                        R.string.settings_services_search_fallback
                                    snap.searchOk -> R.string.settings_services_search_ok
                                    else -> R.string.settings_services_search_fail
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (snap.searchOk) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                        )
                    }
                    }

                    SettingsAccordion(
                        title = stringResource(R.string.settings_geofence),
                        summary = geofenceActionLabel(settings.geofenceAction),
                        expanded = openGroup == SettingsGroupId.Arrive.name,
                        onToggle = {
                            openGroup = if (openGroup == SettingsGroupId.Arrive.name) {
                                ""
                            } else {
                                SettingsGroupId.Arrive.name
                            }
                        },
                    ) {
                    Text(
                        text = stringResource(R.string.settings_geofence_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = IslandColors.onSurfaceMuted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                    SettingsDropdownRow(
                        label = stringResource(R.string.settings_geofence),
                        selected = settings.geofenceAction,
                        options = GeofenceAction.entries.toList(),
                        optionLabel = { geofenceActionLabel(it) },
                        onSelected = viewModel::setGeofenceAction,
                    )
                    GeofenceRadiusField(
                        radiusMeters = settings.geofenceRadiusMeters,
                        onChange = { meters ->
                            viewModel.setGeofenceRadiusMeters(
                                meters ?: StopGeofence.DEFAULT_RADIUS_METERS,
                            )
                        },
                        optional = false,
                        resetKey = "settings",
                    )
                    SettingsDropdownRow(
                        label = stringResource(R.string.settings_geofence_dwell),
                        selected = settings.geofenceDwellSeconds,
                        options = GeofenceDwell.OPTIONS_SECONDS,
                        optionLabel = { seconds -> dwellLabel(seconds) },
                        onSelected = viewModel::setGeofenceDwellSeconds,
                    )
                    Text(
                        text = stringResource(R.string.settings_geofence_dwell_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = IslandColors.onSurfaceMuted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                    )
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_geofence_haptic),
                        subtitle = stringResource(R.string.settings_geofence_haptic_hint),
                        checked = settings.geofenceHaptic,
                        onCheckedChange = viewModel::setGeofenceHaptic,
                    )
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_geofence_beep),
                        subtitle = stringResource(R.string.settings_geofence_beep_hint),
                        checked = settings.geofenceBeep,
                        onCheckedChange = viewModel::setGeofenceBeep,
                    )
                    }

                    SettingsAccordion(
                        title = stringResource(R.string.settings_data),
                        summary = stringResource(R.string.settings_backup_json_title),
                        expanded = openGroup == SettingsGroupId.Data.name,
                        onToggle = {
                            openGroup = if (openGroup == SettingsGroupId.Data.name) {
                                ""
                            } else {
                                SettingsGroupId.Data.name
                            }
                        },
                    ) {
                    Text(
                        text = stringResource(R.string.settings_backup_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = IslandColors.onSurfaceMuted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                    OutlinedButton(
                        onClick = {
                            exportIsShare = false
                            askExportPassword = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.backup_save_file))
                    }
                    OutlinedButton(
                        onClick = {
                            exportIsShare = true
                            askExportPassword = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.backup_share_file))
                    }
                    Button(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.backup_import_action))
                    }
                    OutlinedButton(
                        onClick = { folderLauncher.launch(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.backup_folder_action))
                    }
                    if (settings.backupFolderUri.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.backup_folder_set),
                            style = MaterialTheme.typography.bodySmall,
                            color = IslandColors.onSurfaceMuted,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                        TextButton(onClick = { viewModel.clearBackupFolder() }) {
                            Text(stringResource(R.string.backup_folder_clear))
                        }
                    }
                    val lastBackupLabel = remember(settings.lastBackupEpochMs) {
                        if (settings.lastBackupEpochMs <= 0L) null
                        else java.text.DateFormat.getDateTimeInstance(
                            java.text.DateFormat.MEDIUM,
                            java.text.DateFormat.SHORT,
                        ).format(java.util.Date(settings.lastBackupEpochMs))
                    }
                    Text(
                        text = if (lastBackupLabel == null) {
                            context.getString(R.string.backup_never)
                        } else {
                            context.getString(R.string.backup_last, lastBackupLabel)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = IslandColors.onSurfaceMuted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                    if (com.danielcioban.routeplanner.data.backup.AutoBackup.isReminderDue(settings.lastBackupEpochMs)) {
                        Text(
                            text = stringResource(R.string.backup_reminder),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenTrash,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.menu_trash))
                    }
                    SettingsSection(title = stringResource(R.string.settings_trip_retention))
                    Text(
                        text = stringResource(R.string.settings_trip_retention_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = IslandColors.onSurfaceMuted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                    SettingsDropdownRow(
                        label = stringResource(R.string.settings_trip_retention),
                        selected = settings.tripRetentionDays,
                        options = listOf(0, 30, 90),
                        optionLabel = { days -> retentionLabel(days) },
                        onSelected = viewModel::setTripRetentionDays,
                    )
                    OutlinedButton(
                        onClick = { viewModel.vacuumOldTrips() },
                        enabled = settings.tripRetentionDays > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.settings_trip_retention_run))
                    }
                    vacuumRemoved?.let { removed ->
                        Text(
                            text = stringResource(R.string.settings_trip_retention_done, removed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }
                    importResult?.let { result ->
                        Text(
                            text = stringResource(
                                R.string.backup_import_success,
                                result.routesAdded + result.routesUpdated,
                                result.libraryAdded + result.libraryUpdated,
                                result.tripsAdded + result.tripsUpdated,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }
                    if (importFailed) {
                        Text(
                            text = stringResource(R.string.backup_import_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }
                    importResult?.let { result ->
                        if (result.routesSkipped > 0 || result.librarySkipped > 0) {
                            Text(
                                text = stringResource(
                                    R.string.backup_skipped,
                                    result.routesSkipped,
                                    result.librarySkipped,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = IslandColors.onSurfaceMuted,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            )
                        }
                    }
                    if (backupSaved) {
                        Text(
                            text = stringResource(R.string.backup_saved),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }
                    if (passwordWrong) {
                        Text(
                            text = stringResource(R.string.backup_password_wrong),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }

                    IslandListDivider()
                    SettingsSection(title = stringResource(R.string.settings_csv_title))
                    Text(
                        text = stringResource(R.string.csv_import_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = IslandColors.onSurfaceMuted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                    OutlinedButton(
                        onClick = {
                            csvLauncher.launch(
                                arrayOf(
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "text/plain",
                                    "*/*",
                                ),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.csv_import_action))
                    }
                    OutlinedButton(
                        onClick = {
                            csvIntoLauncher.launch(
                                arrayOf(
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "text/plain",
                                    "*/*",
                                ),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.csv_import_into_route))
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val csv = viewModel.exportLibraryCsv()
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.csv_export_subject))
                                    putExtra(Intent.EXTRA_TEXT, csv)
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        send,
                                        context.getString(R.string.csv_export_chooser),
                                    ),
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.csv_export_action))
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                PlaceShare.shareText(
                                    context = context,
                                    subject = context.getString(R.string.csv_export_subject),
                                    body = viewModel.exportLibraryGpx(),
                                    mimeType = "application/gpx+xml",
                                    chooserTitle = context.getString(R.string.export_places_chooser),
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.export_library_gpx))
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                PlaceShare.shareText(
                                    context = context,
                                    subject = context.getString(R.string.csv_export_subject),
                                    body = viewModel.exportLibraryKml(),
                                    mimeType = "application/vnd.google-earth.kml+xml",
                                    chooserTitle = context.getString(R.string.export_places_chooser),
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.export_library_kml))
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                PlaceShare.shareText(
                                    context = context,
                                    subject = context.getString(R.string.csv_export_subject),
                                    body = viewModel.exportLibraryGeoJson(),
                                    mimeType = "application/geo+json",
                                    chooserTitle = context.getString(R.string.export_places_chooser),
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.export_library_geojson))
                    }
                    csvResult?.let { result ->
                        Text(
                            text = stringResource(
                                R.string.csv_import_success,
                                result.imported,
                                result.routeName,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }
                    if (csvFailed) {
                        Text(
                            text = stringResource(R.string.csv_import_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }

                    }

                    SettingsAccordion(
                        title = stringResource(R.string.settings_task_templates),
                        summary = templates.size.toString(),
                        expanded = openGroup == SettingsGroupId.Templates.name,
                        onToggle = {
                            openGroup = if (openGroup == SettingsGroupId.Templates.name) {
                                ""
                            } else {
                                SettingsGroupId.Templates.name
                            }
                        },
                    ) {
                    Text(
                        text = stringResource(R.string.settings_task_templates_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = IslandColors.onSurfaceMuted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                    templates.forEach { template ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = template.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = IslandColors.onSurface,
                                )
                                if (template.isRequired) {
                                    Text(
                                        text = stringResource(R.string.tasks_required),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            TextButton(onClick = { viewModel.deleteTaskTemplate(template.id) }) {
                                Text(
                                    stringResource(R.string.action_delete),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    SoftOutlinedTextField(
                        value = newTemplateTitle,
                        onValueChange = { newTemplateTitle = it },
                        label = stringResource(R.string.task_templates_title_label),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = newTemplateRequired,
                            onClick = { newTemplateRequired = !newTemplateRequired },
                            label = { Text(stringResource(R.string.tasks_required)) },
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                viewModel.addTaskTemplate(newTemplateTitle, newTemplateRequired)
                                newTemplateTitle = ""
                                newTemplateRequired = false
                            },
                            enabled = newTemplateTitle.isNotBlank(),
                        ) {
                            Text(stringResource(R.string.task_templates_add))
                        }
                    }

                    }

                    IslandListDivider()

                    TextButton(
                        onClick = { uriHandler.openUri(BuildConfig.PRIVACY_POLICY_URL) },
                        modifier = Modifier.padding(horizontal = 6.dp),
                    ) {
                        Text(stringResource(R.string.privacy_policy))
                    }
                    TextButton(
                        onClick = { ProblemReport.start(context) },
                        modifier = Modifier.padding(horizontal = 6.dp),
                    ) {
                        Text(stringResource(R.string.report_problem))
                    }

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

        if (showHomeSearch) {
            AddressSearchDialog(
                onDismissRequest = { showHomeSearch = false },
                onPlaceSelected = { place ->
                    showHomeSearch = false
                    viewModel.setHome(
                        place.latitude,
                        place.longitude,
                        place.shortName.ifBlank { settings.homeName },
                    )
                },
            )
        }

        if (askExportPassword) {
            BackupPasswordDialog(
                required = false,
                onDismiss = { askExportPassword = false },
                onConfirm = { password ->
                    pendingExportPassword = password
                    askExportPassword = false
                    if (exportIsShare) {
                        viewModel.shareBackupFile(
                            context,
                            context.getString(R.string.backup_export_chooser),
                            context.getString(R.string.backup_export_subject),
                            password,
                        )
                    } else if (password.isBlank()) {
                        saveBackupLauncher.launch("route-planner-backup.json")
                    } else {
                        saveEncryptedBackupLauncher.launch("route-planner-backup.rpenc")
                    }
                },
            )
        }

        if (askImportPassword) {
            BackupPasswordDialog(
                required = true,
                onDismiss = {
                    askImportPassword = false
                    pendingImportBytes = null
                },
                onConfirm = { password ->
                    val bytes = pendingImportBytes
                    askImportPassword = false
                    if (bytes == null) return@BackupPasswordDialog
                    val json = viewModel.decodeBackup(bytes, password)
                    if (json != null) {
                        decodedJson = json
                        viewModel.previewBackup(json)
                        showSelectiveImport = true
                    }
                },
            )
        }

        if (showSelectiveImport) {
            SelectiveImportDialog(
                preview = preview,
                onDismiss = {
                    showSelectiveImport = false
                    decodedJson = null
                    pendingImportBytes = null
                },
                onImport = { options ->
                    val json = decodedJson
                    showSelectiveImport = false
                    pendingImportBytes = null
                    decodedJson = null
                    if (json != null) viewModel.importBackupJson(json, options)
                },
            )
        }

        if (pickingCsvTarget) {
            CsvImportTargetDialog(
                routes = routes.filter { !it.route.archived },
                onDismiss = {
                    pickingCsvTarget = false
                    pendingCsvUri = null
                },
                onNewRoute = {
                    val uri = pendingCsvUri
                    pickingCsvTarget = false
                    pendingCsvUri = null
                    uri?.let {
                        viewModel.importCsv(
                            context,
                            it,
                            context.getString(R.string.csv_import_default_route),
                        )
                    }
                },
                onExistingRoute = { routeId ->
                    val uri = pendingCsvUri
                    pickingCsvTarget = false
                    pendingCsvUri = null
                    uri?.let { viewModel.importCsvIntoRoute(context, it, routeId) }
                },
            )
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
private fun SettingsSwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    IslandListItem {
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
private fun dwellLabel(seconds: Int): String = if (seconds == 0) {
    stringResource(R.string.settings_geofence_dwell_off)
} else {
    stringResource(R.string.settings_geofence_dwell_seconds, seconds)
}

@Composable
private fun retentionLabel(days: Int): String = when (days) {
    30 -> stringResource(R.string.settings_trip_retention_30)
    90 -> stringResource(R.string.settings_trip_retention_90)
    else -> stringResource(R.string.settings_trip_retention_off)
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
    AppLanguage.FRENCH -> stringResource(R.string.language_french)
    AppLanguage.GERMAN -> stringResource(R.string.language_german)
    AppLanguage.ITALIAN -> stringResource(R.string.language_italian)
    AppLanguage.SPANISH -> stringResource(R.string.language_spanish)
    AppLanguage.PORTUGUESE -> stringResource(R.string.language_portuguese)
}

@Composable
private fun unitLabel(unit: DistanceUnit): String = when (unit) {
    DistanceUnit.METRIC -> stringResource(R.string.units_metric)
    DistanceUnit.IMPERIAL -> stringResource(R.string.units_imperial)
}

@Composable
private fun clockLabel(format: ClockFormat): String = when (format) {
    ClockFormat.SYSTEM -> stringResource(R.string.settings_clock_system)
    ClockFormat.HOURS_24 -> stringResource(R.string.settings_clock_24h)
    ClockFormat.HOURS_12 -> stringResource(R.string.settings_clock_12h)
}

@Composable
private fun ttsVerbosityLabel(level: TtsVerbosity): String = when (level) {
    TtsVerbosity.MINIMAL -> stringResource(R.string.settings_tts_minimal)
    TtsVerbosity.NORMAL -> stringResource(R.string.settings_tts_normal)
    TtsVerbosity.VERBOSE -> stringResource(R.string.settings_tts_verbose)
}

@Composable
private fun geofenceActionLabel(action: GeofenceAction): String = when (action) {
    GeofenceAction.OFF -> stringResource(R.string.geofence_action_off)
    GeofenceAction.VISITED -> stringResource(R.string.geofence_action_visited)
    GeofenceAction.COMPLETE -> stringResource(R.string.geofence_action_complete)
}

@Composable
private fun vehicleLabel(profile: VehicleProfile): String = when (profile) {
    VehicleProfile.CAR -> stringResource(R.string.settings_vehicle_car)
    VehicleProfile.BIKE -> stringResource(R.string.settings_vehicle_bike)
    VehicleProfile.WALK -> stringResource(R.string.settings_vehicle_walk)
}

@Composable
private fun rerouteLabel(level: RerouteAggressiveness): String = when (level) {
    RerouteAggressiveness.CALM -> stringResource(R.string.settings_reroute_calm)
    RerouteAggressiveness.NORMAL -> stringResource(R.string.settings_reroute_normal)
    RerouteAggressiveness.SHARP -> stringResource(R.string.settings_reroute_sharp)
}
