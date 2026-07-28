package com.danielcioban.routeplanner.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.local.RouteWithStops
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RouteDetailViewModel(
    private val repository: RouteRepository,
    routeId: Long,
) : ViewModel() {
    val route: StateFlow<RouteWithStops?> = repository.observeRoute(routeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setStopCompleted(stopId: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.setStopCompleted(stopId, completed)
        }
    }

    class Factory(
        private val repository: RouteRepository,
        private val routeId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RouteDetailViewModel(repository, routeId) as T
        }
    }
}
