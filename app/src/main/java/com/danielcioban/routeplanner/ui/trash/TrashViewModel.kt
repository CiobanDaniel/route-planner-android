package com.danielcioban.routeplanner.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(
    private val repository: RouteRepository,
) : ViewModel() {
    val routes: StateFlow<List<RouteWithStops>> = repository.observeTrashRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val library: StateFlow<List<StopLibraryEntity>> = repository.observeTrashLibrary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restoreRoute(id: Long) {
        viewModelScope.launch { repository.restoreRoute(id) }
    }

    fun purgeRoute(id: Long) {
        viewModelScope.launch { repository.purgeRoute(id) }
    }

    fun restoreLibrary(id: Long) {
        viewModelScope.launch { repository.restoreLibraryStop(id) }
    }

    fun purgeLibrary(id: Long) {
        viewModelScope.launch { repository.purgeLibraryStop(id) }
    }

    class Factory(
        private val repository: RouteRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TrashViewModel(repository) as T
        }
    }
}
