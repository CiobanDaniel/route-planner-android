package com.danielcioban.routeplanner.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StopLibraryViewModel(
    private val repository: RouteRepository,
) : ViewModel() {
    val stops: StateFlow<List<StopLibraryEntity>> = repository.observeStopLibrary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteLibraryStop(id)
        }
    }

    fun update(
        id: Long,
        name: String,
        addressHint: String,
        notes: String,
        latitude: Double,
        longitude: Double,
    ) {
        viewModelScope.launch {
            repository.upsertLibraryStop(
                name = name,
                addressHint = addressHint,
                notes = notes,
                latitude = latitude,
                longitude = longitude,
                existingId = id,
            )
        }
    }

    fun upsert(
        name: String,
        addressHint: String,
        notes: String,
        latitude: Double,
        longitude: Double,
        existingId: Long? = null,
    ) {
        viewModelScope.launch {
            repository.upsertLibraryStop(
                name = name,
                addressHint = addressHint,
                notes = notes,
                latitude = latitude,
                longitude = longitude,
                existingId = existingId,
            )
        }
    }

    class Factory(
        private val repository: RouteRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StopLibraryViewModel(repository) as T
        }
    }
}
