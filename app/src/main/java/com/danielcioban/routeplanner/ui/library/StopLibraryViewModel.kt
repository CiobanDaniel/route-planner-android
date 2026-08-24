package com.danielcioban.routeplanner.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.data.LibraryDeleteScope
import com.danielcioban.routeplanner.data.LibraryEditScope
import com.danielcioban.routeplanner.data.LibraryUsage
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryStopItem(
    val stop: StopLibraryEntity,
    val usedOn: List<String>,
)

class StopLibraryViewModel(
    private val repository: RouteRepository,
) : ViewModel() {
    val items: StateFlow<List<LibraryStopItem>> = combine(
        repository.observeStopLibrary(),
        repository.observeRoutes(),
    ) { library, routes ->
        library.map { stop ->
            LibraryStopItem(
                stop = stop,
                usedOn = routes
                    .filter { route -> route.orderedStops.any { it.libraryStopId == stop.id } }
                    .map { it.route.name },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(
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
            )
        }
    }

    fun applyEdit(
        id: Long,
        name: String,
        addressHint: String,
        notes: String,
        latitude: Double,
        longitude: Double,
        scope: LibraryEditScope,
    ) {
        viewModelScope.launch {
            repository.applyLibraryPlaceEdit(
                libraryStopId = id,
                name = name,
                addressHint = addressHint,
                notes = notes,
                latitude = latitude,
                longitude = longitude,
                scope = scope,
            )
        }
    }

    fun delete(id: Long, scope: LibraryDeleteScope = LibraryDeleteScope.Everywhere) {
        viewModelScope.launch {
            repository.deleteLibraryStop(id, scope)
        }
    }

    fun usage(id: Long, onResult: (LibraryUsage) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getLibraryUsage(id))
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
