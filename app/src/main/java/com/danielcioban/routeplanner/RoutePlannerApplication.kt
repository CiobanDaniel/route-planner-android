package com.danielcioban.routeplanner

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.local.AppDatabase
import com.danielcioban.routeplanner.data.delivery.DeliverySessionStore
import com.danielcioban.routeplanner.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class RoutePlannerApplication : Application() {
    lateinit var repository: RouteRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var deliverySessionStore: DeliverySessionStore
        private set

    override fun onCreate() {
        super.onCreate()
        repository = RouteRepository(AppDatabase.get(this))
        settingsRepository = SettingsRepository(this)
        deliverySessionStore = DeliverySessionStore(this)
        runBlocking {
            val language = settingsRepository.settings.first().language
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language.tag),
            )
        }
    }
}
