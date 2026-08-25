package com.danielcioban.routeplanner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.delivery.DeliverySessionStore
import com.danielcioban.routeplanner.data.local.TripHistoryEntity
import com.danielcioban.routeplanner.data.local.TripKind
import com.danielcioban.routeplanner.data.local.TripStatus
import com.danielcioban.routeplanner.util.HistoryFilter
import com.danielcioban.routeplanner.util.HistoryFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class TripHistoryAction {
    data class OpenRoute(val routeId: Long) : TripHistoryAction()
    data object OpenQuickDrive : TripHistoryAction()
}

class TripHistoryViewModel(
    private val repository: RouteRepository,
    private val deliverySessionStore: DeliverySessionStore,
) : ViewModel() {
    private val filter = MutableStateFlow(HistoryFilter.ALL)
    private val query = MutableStateFlow("")
    val trips: StateFlow<List<TripHistoryEntity>> = combine(
        repository.observeTripHistory(),
        filter,
        query,
    ) { all, selected, q ->
        HistoryFilters.apply(all, selected, q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedFilter: StateFlow<HistoryFilter> = filter
    val routeQuery: StateFlow<String> = query

    fun setFilter(value: HistoryFilter) {
        filter.value = value
    }

    fun setQuery(value: String) {
        query.value = value
    }

    suspend fun exportFilteredCsv(): String =
        com.danielcioban.routeplanner.data.backup.TripHistoryCsv.export(trips.value)

    fun delete(tripId: Long) {
        viewModelScope.launch { repository.deleteTrip(tripId) }
    }

    fun resumeOrOpen(trip: TripHistoryEntity, onAction: (TripHistoryAction) -> Unit) {
        viewModelScope.launch {
            when {
                trip.kind == TripKind.ROUTE && trip.routeId != null && trip.routeId > 0 -> {
                    if (trip.status == TripStatus.IN_PROGRESS) {
                        deliverySessionStore.setActiveRoute(trip.routeId, trip.id)
                    }
                    onAction(TripHistoryAction.OpenRoute(trip.routeId))
                }
                trip.kind == TripKind.QUICK &&
                    trip.destLatitude != null &&
                    trip.destLongitude != null -> {
                    if (trip.status == TripStatus.IN_PROGRESS) {
                        deliverySessionStore.setQuickDrive(
                            name = trip.destName.ifBlank { trip.title },
                            latitude = trip.destLatitude,
                            longitude = trip.destLongitude,
                            tripHistoryId = trip.id,
                            libraryStopId = trip.libraryStopId,
                        )
                    } else {
                        val tripId = repository.startQuickTrip(
                            name = trip.destName.ifBlank { trip.title },
                            latitude = trip.destLatitude,
                            longitude = trip.destLongitude,
                            libraryStopId = trip.libraryStopId,
                        )
                        deliverySessionStore.setQuickDrive(
                            name = trip.destName.ifBlank { trip.title },
                            latitude = trip.destLatitude,
                            longitude = trip.destLongitude,
                            tripHistoryId = tripId,
                            libraryStopId = trip.libraryStopId,
                        )
                    }
                    onAction(TripHistoryAction.OpenQuickDrive)
                }
            }
        }
    }

    class Factory(
        private val repository: RouteRepository,
        private val deliverySessionStore: DeliverySessionStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TripHistoryViewModel(repository, deliverySessionStore) as T
        }
    }
}
