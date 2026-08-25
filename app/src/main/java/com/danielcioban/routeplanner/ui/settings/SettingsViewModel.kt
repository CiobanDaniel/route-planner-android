package com.danielcioban.routeplanner.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.ServiceHealth
import com.danielcioban.routeplanner.data.ServiceHealthSnapshot
import com.danielcioban.routeplanner.data.backup.AutoBackup
import com.danielcioban.routeplanner.data.backup.BackupImportOptions
import com.danielcioban.routeplanner.data.backup.BackupImportResult
import com.danielcioban.routeplanner.data.backup.BackupPreview
import com.danielcioban.routeplanner.data.backup.CsvImportResult
import com.danielcioban.routeplanner.data.backup.EncryptedBackup
import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.data.local.TaskTemplateEntity
import com.danielcioban.routeplanner.data.settings.AppLanguage
import com.danielcioban.routeplanner.data.settings.AppSettings
import com.danielcioban.routeplanner.data.settings.ClockFormat
import com.danielcioban.routeplanner.data.settings.DistanceUnit
import com.danielcioban.routeplanner.data.settings.GeofenceAction
import com.danielcioban.routeplanner.data.settings.RerouteAggressiveness
import com.danielcioban.routeplanner.data.settings.SettingsRepository
import com.danielcioban.routeplanner.data.settings.ThemeMode
import com.danielcioban.routeplanner.data.settings.TtsVerbosity
import com.danielcioban.routeplanner.data.settings.VehicleProfile
import com.danielcioban.routeplanner.util.ShareFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val routeRepository: RouteRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _importResult = MutableStateFlow<BackupImportResult?>(null)
    val importResult: StateFlow<BackupImportResult?> = _importResult.asStateFlow()

    private val _importFailed = MutableStateFlow(false)
    val importFailed: StateFlow<Boolean> = _importFailed.asStateFlow()

    private val _csvResult = MutableStateFlow<CsvImportResult?>(null)
    val csvResult: StateFlow<CsvImportResult?> = _csvResult.asStateFlow()

    private val _csvFailed = MutableStateFlow(false)
    val csvFailed: StateFlow<Boolean> = _csvFailed.asStateFlow()

    private val _backupSaved = MutableStateFlow(false)
    val backupSaved: StateFlow<Boolean> = _backupSaved.asStateFlow()

    private val _passwordWrong = MutableStateFlow(false)
    val passwordWrong: StateFlow<Boolean> = _passwordWrong.asStateFlow()

    private val _preview = MutableStateFlow<BackupPreview?>(null)
    val preview: StateFlow<BackupPreview?> = _preview.asStateFlow()

    private val _vacuumRemoved = MutableStateFlow<Int?>(null)
    val vacuumRemoved: StateFlow<Int?> = _vacuumRemoved.asStateFlow()

    private val _health = MutableStateFlow<ServiceHealthSnapshot?>(null)
    val health: StateFlow<ServiceHealthSnapshot?> = _health.asStateFlow()

    private val _healthBusy = MutableStateFlow(false)
    val healthBusy: StateFlow<Boolean> = _healthBusy.asStateFlow()

    val taskTemplates: StateFlow<List<TaskTemplateEntity>> = routeRepository.observeTaskTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val routes: StateFlow<List<RouteWithStops>> = routeRepository.observeRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language.tag),
            )
        }
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch { settingsRepository.setDistanceUnit(unit) }
    }

    fun setClockFormat(format: ClockFormat) {
        viewModelScope.launch { settingsRepository.setClockFormat(format) }
    }

    fun setKeepScreenOnDuringNav(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOnDuringNav(enabled) }
    }

    fun setKeepScreenOnOnlyWhileMoving(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOnOnlyWhileMoving(enabled) }
    }

    fun setSpeakManeuvers(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSpeakManeuvers(enabled) }
    }

    fun setTtsVerbosity(verbosity: TtsVerbosity) {
        viewModelScope.launch { settingsRepository.setTtsVerbosity(verbosity) }
    }

    fun setMuteTtsDuringCalls(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMuteTtsDuringCalls(enabled) }
    }

    fun setDefaultRoundTrip(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDefaultRoundTrip(enabled) }
    }

    fun setDefaultFollowMe(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDefaultFollowMe(enabled) }
    }

    fun setDataSaver(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDataSaver(enabled) }
    }

    fun setGeofenceAction(action: GeofenceAction) {
        viewModelScope.launch { settingsRepository.setGeofenceAction(action) }
    }

    fun setGeofenceRadiusMeters(meters: Int) {
        viewModelScope.launch { settingsRepository.setGeofenceRadiusMeters(meters) }
    }

    fun setGeofenceDwellSeconds(seconds: Int) {
        viewModelScope.launch { settingsRepository.setGeofenceDwellSeconds(seconds) }
    }

    fun setVehicleProfile(profile: VehicleProfile) {
        viewModelScope.launch { settingsRepository.setVehicleProfile(profile) }
    }

    fun setAvoidTolls(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAvoidTolls(enabled) }
    }

    fun setAvoidMotorways(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAvoidMotorways(enabled) }
    }

    fun checkServices() {
        viewModelScope.launch {
            _healthBusy.value = true
            _health.value = runCatching { ServiceHealth.probe() }.getOrElse {
                ServiceHealthSnapshot(
                    routingOk = false,
                    routingUsedFallback = false,
                    routingHost = "",
                    searchOk = false,
                    searchUsedFallback = false,
                    searchHost = "",
                )
            }
            _healthBusy.value = false
        }
    }

    fun setGeofenceHaptic(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGeofenceHaptic(enabled) }
    }

    fun setGeofenceBeep(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGeofenceBeep(enabled) }
    }

    fun setVolumeKeyDone(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVolumeKeyDone(enabled) }
    }

    fun setNorthUpWhileDriving(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNorthUpWhileDriving(enabled) }
    }

    fun setWalkLastMile(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setWalkLastMile(enabled) }
    }

    fun setRerouteAggressiveness(value: RerouteAggressiveness) {
        viewModelScope.launch { settingsRepository.setRerouteAggressiveness(value) }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReduceMotion(enabled) }
    }

    fun setHighContrastPins(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHighContrastPins(enabled) }
    }

    fun setHome(latitude: Double, longitude: Double, name: String) {
        viewModelScope.launch { settingsRepository.setHome(latitude, longitude, name) }
    }

    fun clearHome() {
        viewModelScope.launch { settingsRepository.clearHome() }
    }

    fun setUseHomeAsOrigin(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setUseHomeAsOrigin(enabled) }
    }

    fun setDispatcherVanName(name: String) {
        viewModelScope.launch { settingsRepository.setDispatcherVanName(name) }
    }

    fun setAskProofOnDone(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAskProofOnDone(enabled) }
    }

    fun setOnboardingDismissed(dismissed: Boolean) {
        viewModelScope.launch { settingsRepository.setOnboardingDismissed(dismissed) }
    }

    suspend fun exportBackupJson(): String = routeRepository.exportBackupJson()

    suspend fun exportBackupBytes(password: String): ByteArray {
        val json = routeRepository.exportBackupJson()
        return if (password.isBlank()) {
            json.toByteArray(Charsets.UTF_8)
        } else {
            EncryptedBackup.encrypt(json, password)
        }
    }

    fun markBackupExported() {
        viewModelScope.launch {
            settingsRepository.setLastBackupEpochMs(System.currentTimeMillis())
            _backupSaved.value = true
        }
    }

    fun saveBackupToUri(context: Context, uri: Uri, password: String) {
        viewModelScope.launch {
            runCatching {
                val bytes = exportBackupBytes(password)
                AutoBackup.writeUri(context, uri, bytes)
            }.onSuccess { ok ->
                if (ok) markBackupExported() else _importFailed.value = true
            }.onFailure { _importFailed.value = true }
        }
    }

    fun shareBackupFile(context: Context, chooser: String, subject: String, password: String) {
        viewModelScope.launch {
            runCatching {
                val bytes = exportBackupBytes(password)
                val name = if (password.isBlank()) {
                    "route-planner-backup.json"
                } else {
                    "route-planner-backup.rpenc"
                }
                val file = ShareFile.writeCache(context, name, bytes)
                ShareFile.share(context, file, "application/octet-stream", chooser, subject)
            }
        }
    }

    fun setBackupFolder(context: Context, uri: Uri) {
        viewModelScope.launch {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            settingsRepository.setBackupFolderUri(uri.toString())
            val json = routeRepository.exportBackupJson()
            if (AutoBackup.write(context, uri.toString(), json)) {
                markBackupExported()
            }
        }
    }

    fun clearBackupFolder() {
        viewModelScope.launch { settingsRepository.setBackupFolderUri("") }
    }

    fun setTripRetentionDays(days: Int) {
        viewModelScope.launch { settingsRepository.setTripRetentionDays(days) }
    }

    fun vacuumOldTrips() {
        viewModelScope.launch {
            val days = settings.value.tripRetentionDays
            if (days <= 0) {
                _vacuumRemoved.value = 0
                return@launch
            }
            val cutoff = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
            _vacuumRemoved.value = routeRepository.deleteTripsOlderThan(cutoff)
        }
    }

    fun previewBackup(json: String) {
        viewModelScope.launch {
            _preview.value = runCatching { routeRepository.previewBackupJson(json) }.getOrNull()
        }
    }

    fun decodeBackup(bytes: ByteArray, password: String?): String? {
        _passwordWrong.value = false
        return try {
            if (EncryptedBackup.isEncrypted(bytes)) {
                val pass = password.orEmpty()
                if (pass.isEmpty()) return null
                EncryptedBackup.decrypt(bytes, pass)
            } else {
                bytes.toString(Charsets.UTF_8)
            }
        } catch (_: Exception) {
            _passwordWrong.value = true
            null
        }
    }

    fun importBackupJson(json: String, options: BackupImportOptions) {
        viewModelScope.launch {
            _importFailed.value = false
            _importResult.value = null
            runCatching {
                routeRepository.importBackupJson(json, options)
            }.onSuccess { result ->
                _importResult.value = result
            }.onFailure {
                _importFailed.value = true
            }
        }
    }

    suspend fun exportLibraryCsv(): String = routeRepository.exportLibraryCsv()

    suspend fun exportLibraryGpx(): String = routeRepository.exportLibraryGpx()

    suspend fun exportLibraryKml(): String = routeRepository.exportLibraryKml()

    suspend fun exportLibraryGeoJson(): String = routeRepository.exportLibraryGeoJson()

    fun importCsv(context: Context, uri: Uri, defaultRouteName: String) {
        viewModelScope.launch {
            _csvFailed.value = false
            _csvResult.value = null
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: error("Empty file")
                val fromFile = displayName(context, uri)
                    ?.substringBeforeLast('.')
                    ?.takeIf { it.isNotBlank() }
                routeRepository.importStopsCsv(
                    text,
                    fromFile ?: defaultRouteName,
                    roundTrip = settings.value.defaultRoundTrip,
                )
            }.onSuccess { result ->
                if (result.imported == 0) {
                    _csvFailed.value = true
                } else {
                    _csvResult.value = result
                }
            }.onFailure {
                _csvFailed.value = true
            }
        }
    }

    fun importCsvIntoRoute(context: Context, uri: Uri, routeId: Long) {
        viewModelScope.launch {
            _csvFailed.value = false
            _csvResult.value = null
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: error("Empty file")
                routeRepository.importStopsCsvIntoRoute(routeId, text)
            }.onSuccess { result ->
                if (result.imported == 0) {
                    _csvFailed.value = true
                } else {
                    _csvResult.value = result
                }
            }.onFailure {
                _csvFailed.value = true
            }
        }
    }

    fun clearImportFeedback() {
        _importResult.value = null
        _importFailed.value = false
        _csvResult.value = null
        _csvFailed.value = false
        _backupSaved.value = false
        _passwordWrong.value = false
        _preview.value = null
        _vacuumRemoved.value = null
    }

    fun addTaskTemplate(title: String, required: Boolean) {
        viewModelScope.launch { routeRepository.addTaskTemplate(title, required) }
    }

    fun deleteTaskTemplate(id: Long) {
        viewModelScope.launch { routeRepository.deleteTaskTemplate(id) }
    }

    private fun displayName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val routeRepository: RouteRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository, routeRepository) as T
        }
    }
}
