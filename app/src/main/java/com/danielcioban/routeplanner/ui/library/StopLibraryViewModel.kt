package com.danielcioban.routeplanner.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.data.AddFromLibraryResult
import com.danielcioban.routeplanner.data.LibraryDeleteScope
import com.danielcioban.routeplanner.data.LibraryEditScope
import com.danielcioban.routeplanner.data.LibraryPlaceExtras
import com.danielcioban.routeplanner.data.LibraryUsage
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.local.LibraryDefaultTaskEntity
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import com.danielcioban.routeplanner.data.local.RouteWithStops
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryStopItem(
    val stop: StopLibraryEntity,
    val usedOn: List<String>,
)

@OptIn(ExperimentalCoroutinesApi::class)
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

    val routes: StateFlow<List<RouteWithStops>> = repository.observeRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val editingLibraryId = MutableStateFlow<Long?>(null)
    val editingDefaultTasks: StateFlow<List<LibraryDefaultTaskEntity>> = editingLibraryId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.observeLibraryDefaultTasks(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _mergeResult = MutableStateFlow<Int?>(null)
    val mergeResult: StateFlow<Int?> = _mergeResult.asStateFlow()

    fun setEditingLibraryId(id: Long?) {
        editingLibraryId.value = id
    }

    fun addToRoute(routeId: Long, libraryStopId: Long, onOpenRoute: (Long) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.addStopFromLibrary(routeId, libraryStopId)) {
                is AddFromLibraryResult.Added,
                is AddFromLibraryResult.AlreadyOnRoute,
                -> onOpenRoute(routeId)
                AddFromLibraryResult.LibraryMissing -> Unit
            }
        }
    }

    fun create(
        name: String,
        addressHint: String,
        notes: String,
        latitude: Double,
        longitude: Double,
        extras: LibraryPlaceExtras? = null,
    ) {
        viewModelScope.launch {
            repository.upsertLibraryStop(
                name = name,
                addressHint = addressHint,
                notes = notes,
                latitude = latitude,
                longitude = longitude,
                extras = extras,
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
        extras: LibraryPlaceExtras? = null,
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
                extras = extras,
            )
        }
    }

    fun addDefaultTask(libraryStopId: Long, title: String, required: Boolean) {
        viewModelScope.launch { repository.addLibraryDefaultTask(libraryStopId, title, required) }
    }

    fun deleteDefaultTask(id: Long) {
        viewModelScope.launch { repository.deleteLibraryDefaultTask(id) }
    }

    fun mergeNearbyDuplicates() {
        viewModelScope.launch {
            _mergeResult.value = repository.mergeNearbyLibraryDuplicates()
        }
    }

    fun clearMergeResult() {
        _mergeResult.value = null
    }

    fun setFavorite(id: Long, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(id, favorite) }
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
