package com.danielcioban.routeplanner.ui.settings

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.backup.BackupImportResult
import com.danielcioban.routeplanner.data.settings.AppLanguage
import com.danielcioban.routeplanner.data.settings.AppSettings
import com.danielcioban.routeplanner.data.settings.DistanceUnit
import com.danielcioban.routeplanner.data.settings.SettingsRepository
import com.danielcioban.routeplanner.data.settings.ThemeMode
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

    fun setKeepScreenOnDuringNav(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOnDuringNav(enabled) }
    }

    suspend fun exportBackupJson(): String = routeRepository.exportBackupJson()

    fun importBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            _importFailed.value = false
            _importResult.value = null
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: error("Empty file")
                routeRepository.importBackupJson(json)
            }.onSuccess { result ->
                _importResult.value = result
            }.onFailure {
                _importFailed.value = true
            }
        }
    }

    fun clearImportFeedback() {
        _importResult.value = null
        _importFailed.value = false
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
