package com.danielcioban.routeplanner.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.StopDraft
import com.danielcioban.routeplanner.data.account.AccountSession
import com.danielcioban.routeplanner.data.account.AccountSessionStore
import com.danielcioban.routeplanner.data.delivery.DeliverySession
import com.danielcioban.routeplanner.data.delivery.DeliverySessionStore
import com.danielcioban.routeplanner.data.local.RouteWithStops
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RouteListViewModel(
    private val repository: RouteRepository,
    private val deliverySessionStore: DeliverySessionStore,
    private val accountSessionStore: AccountSessionStore,
) : ViewModel() {
    val routes: StateFlow<List<RouteWithStops>> = repository.observeRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val deliverySession: StateFlow<DeliverySession> = deliverySessionStore.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeliverySession())

    val accountSession: StateFlow<AccountSession> = accountSessionStore.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountSession.SignedOut)

    fun deleteRoute(routeId: Long) {
        viewModelScope.launch {
            repository.deleteRoute(routeId)
            deliverySessionStore.clearIfRoute(routeId)
        }
    }

    fun signOut() {
        viewModelScope.launch { accountSessionStore.signOut() }
    }

    fun archiveRoute(routeId: Long, archived: Boolean) {
        viewModelScope.launch { repository.setRouteArchived(routeId, archived) }
    }

    fun resumeTrip() {
        viewModelScope.launch { deliverySessionStore.setPaused(false) }
    }

    fun duplicateRoute(routeId: Long, copySuffix: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val newId = repository.duplicateRoute(routeId, copySuffix) ?: return@launch
            onCreated(newId)
        }
    }

    fun addPlaceToRoute(
        routeId: Long,
        name: String,
        latitude: Double,
        longitude: Double,
        addressHint: String,
        libraryStopId: Long?,
        onDone: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val libId = libraryStopId ?: repository.upsertLibraryStop(
                name = name,
                addressHint = addressHint,
                notes = "",
                latitude = latitude,
                longitude = longitude,
            )
            repository.addStopFromLibrary(routeId, libId)
            onDone(routeId)
        }
    }

    fun createRouteFromPlace(
        name: String,
        latitude: Double,
        longitude: Double,
        addressHint: String,
        libraryStopId: Long?,
        roundTrip: Boolean = false,
        onCreated: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val libId = libraryStopId ?: repository.upsertLibraryStop(
                name = name,
                addressHint = addressHint,
                notes = "",
                latitude = latitude,
                longitude = longitude,
            )
            val routeId = repository.createRoute(
                name = name,
                notes = "",
                stops = listOf(
                    StopDraft(
                        name = name,
                        addressHint = addressHint,
                        latitude = latitude,
                        longitude = longitude,
                        libraryStopId = libId,
                    ),
                ),
                roundTrip = roundTrip,
            )
            onCreated(routeId)
        }
    }

    fun savePlaceToLibrary(
        name: String,
        latitude: Double,
        longitude: Double,
        addressHint: String,
    ) {
        viewModelScope.launch {
            repository.upsertLibraryStop(
                name = name,
                addressHint = addressHint,
                notes = "",
                latitude = latitude,
                longitude = longitude,
            )
        }
    }

    class Factory(
        private val repository: RouteRepository,
        private val deliverySessionStore: DeliverySessionStore,
        private val accountSessionStore: AccountSessionStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RouteListViewModel(repository, deliverySessionStore, accountSessionStore) as T
        }
    }
}
