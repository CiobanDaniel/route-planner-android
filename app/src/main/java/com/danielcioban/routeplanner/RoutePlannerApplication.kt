package com.danielcioban.routeplanner

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.danielcioban.routeplanner.BuildConfig
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.backup.AutoBackup
import com.danielcioban.routeplanner.data.account.AccountSession
import com.danielcioban.routeplanner.data.account.AccountSessionStore
import com.danielcioban.routeplanner.data.delivery.DeliverySessionStore
import com.danielcioban.routeplanner.data.local.AppDatabase
import com.danielcioban.routeplanner.data.routing.LastRoutingErrorStore
import com.danielcioban.routeplanner.data.settings.AppSettings
import com.danielcioban.routeplanner.data.settings.SettingsRepository
import com.danielcioban.routeplanner.ui.location.hasLocationPermission
import com.danielcioban.routeplanner.ui.shortcuts.AppShortcuts
import com.danielcioban.routeplanner.ui.trip.TripGuidanceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RoutePlannerApplication : Application() {
    lateinit var repository: RouteRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var deliverySessionStore: DeliverySessionStore
        private set
    lateinit var accountSessionStore: AccountSessionStore
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    var latestSettings: AppSettings = AppSettings()
        private set

    override fun onCreate() {
        super.onCreate()
        DebugStartup.install()
        LastRoutingErrorStore.install(this)
        repository = RouteRepository(AppDatabase.get(this))
        settingsRepository = SettingsRepository(this)
        deliverySessionStore = DeliverySessionStore(this)
        accountSessionStore = AccountSessionStore(this)
        if (!BuildConfig.DEBUG) {
            appScope.launch {
                val session = accountSessionStore.session.first()
                if (session is AccountSession.SignedIn &&
                    session.providerId == AccountSessionStore.DEVELOPER_PROVIDER
                ) {
                    accountSessionStore.signOut()
                }
            }
        }
        runBlocking {
            val settings = settingsRepository.settings.first()
            latestSettings = settings
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(settings.language.tag),
            )
        }
        appScope.launch {
            settingsRepository.settings.collect { latestSettings = it }
        }
        appScope.launch {
            repository.ensureDefaultTaskTemplates(
                listOf(
                    getString(R.string.task_template_cod) to true,
                    getString(R.string.task_template_id) to true,
                    getString(R.string.task_template_photo) to false,
                ),
            )
        }
        appScope.launch {
            combine(
                settingsRepository.settings,
                repository.observeRoutes(),
            ) { settings, routes ->
                val id = settings.lastOpenedRouteId
                val name = routes.firstOrNull { it.route.id == id }?.route?.name
                Triple(id, name, settings.hasHome)
            }.distinctUntilChanged().collect { (id, name, homeSet) ->
                AppShortcuts.publish(this@RoutePlannerApplication, id, name, homeSet)
            }
        }
        appScope.launch {
            deliverySessionStore.session
                .map { it.isGuiding }
                .distinctUntilChanged()
                .collect { guiding ->
                    if (guiding && hasLocationPermission() && latestSettings.fgsLocationExplained) {
                        runCatching { TripGuidanceService.start(this@RoutePlannerApplication) }
                    } else if (!guiding) {
                        runCatching { TripGuidanceService.stop(this@RoutePlannerApplication) }
                    }
                }
        }
        appScope.launch(Dispatchers.IO) {
            val settings = settingsRepository.settings.first()
            if (settings.tripRetentionDays > 0) {
                val cutoff = System.currentTimeMillis() -
                    settings.tripRetentionDays * 24L * 60L * 60L * 1000L
                repository.deleteTripsOlderThan(cutoff)
            }
            val folder = settings.backupFolderUri
            if (folder.isNotBlank() && AutoBackup.shouldWriteNow(settings.lastBackupEpochMs)) {
                val json = repository.exportBackupJson()
                if (AutoBackup.write(this@RoutePlannerApplication, folder, json)) {
                    settingsRepository.setLastBackupEpochMs(System.currentTimeMillis())
                }
            }
        }
    }
}
