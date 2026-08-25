package com.danielcioban.routeplanner.ui.fuel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.local.FuelLogEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FuelLogViewModel(
    private val repository: RouteRepository,
) : ViewModel() {
    val entries: StateFlow<List<FuelLogEntity>> = repository.observeFuelLog()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(odometerKm: Double?, liters: Double?, amount: Double?, notes: String) {
        viewModelScope.launch {
            repository.addFuelLog(odometerKm, liters, amount, notes)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteFuelLog(id) }
    }

    class Factory(
        private val repository: RouteRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FuelLogViewModel(repository) as T
        }
    }
}
