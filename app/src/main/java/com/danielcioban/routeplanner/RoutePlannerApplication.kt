package com.danielcioban.routeplanner

import android.app.Application
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.local.AppDatabase

class RoutePlannerApplication : Application() {
    lateinit var repository: RouteRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = RouteRepository(AppDatabase.get(this))
    }
}
