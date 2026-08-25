package com.danielcioban.routeplanner.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.local.FuelLogEntity
import com.danielcioban.routeplanner.util.TripStats
import com.danielcioban.routeplanner.util.TripStatsSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class TripStatsViewModel(
    repository: RouteRepository,
) : ViewModel() {
    val summary: StateFlow<TripStatsSummary> = combine(
        repository.observeTripHistory(),
        repository.observeRoutes(),
    ) { trips, routes ->
        val cod = routes.sumOf { route ->
            route.deliveryStops.filter { it.codCollected }.sumOf { it.codAmount }
        }
        TripStats.summarize(trips, codCollected = cod)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TripStats.summarize(emptyList()),
    )

    val fuel: StateFlow<List<FuelLogEntity>> = repository.observeFuelLog()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    class Factory(
        private val repository: RouteRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TripStatsViewModel(repository) as T
        }
    }
}
