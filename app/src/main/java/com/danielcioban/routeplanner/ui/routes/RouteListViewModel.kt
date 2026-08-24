package com.danielcioban.routeplanner.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.data.RouteRepository
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

    fun duplicateRoute(routeId: Long, copySuffix: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val newId = repository.duplicateRoute(routeId, copySuffix) ?: return@launch
            onCreated(newId)
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
