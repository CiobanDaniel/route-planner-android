package com.danielcioban.routeplanner.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.StopDraft
import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.routing.DrivingRoute
import com.danielcioban.routeplanner.data.routing.NavGuidance
import com.danielcioban.routeplanner.data.routing.NavigationProgress
import com.danielcioban.routeplanner.data.routing.OsrmRoutingClient
import com.danielcioban.routeplanner.ui.map.LatLng
import com.danielcioban.routeplanner.util.GeoUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PendingStopPin(
    val latitude: Double,
    val longitude: Double,
)

data class DeliveryProgress(
    val nextStop: StopEntity?,
    val remaining: Int,
    val approxFromPrevious: String?,
    val stopIndex: Int = 0,
    val totalStops: Int = 0,
)

enum class NavigationPhase {
    Idle,
    LoadingRoute,
    Navigating,
    Arrived,
    Error,
}

data class NavigationUiState(
    val phase: NavigationPhase = NavigationPhase.Idle,
    val targetStopId: Long? = null,
    val route: DrivingRoute? = null,
    val guidance: NavGuidance? = null,
    val errorMessage: String? = null,
    val navLineJson: String? = null,
    val navFitToken: Int = 0,
)

class RouteDetailViewModel(
    private val repository: RouteRepository,
    private val routeId: Long,
    private val routingClient: OsrmRoutingClient = OsrmRoutingClient(),
) : ViewModel() {
    val route: StateFlow<RouteWithStops?> = repository.observeRoute(routeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _deliveryActive = MutableStateFlow(false)
    val deliveryActive: StateFlow<Boolean> = _deliveryActive.asStateFlow()

    private val _pendingPin = MutableStateFlow<PendingStopPin?>(null)
    val pendingPin: StateFlow<PendingStopPin?> = _pendingPin.asStateFlow()

    private val _selectedStopId = MutableStateFlow<Long?>(null)
    val selectedStopId: StateFlow<Long?> = _selectedStopId.asStateFlow()

    private val _navigation = MutableStateFlow(NavigationUiState())
    val navigation: StateFlow<NavigationUiState> = _navigation.asStateFlow()

    private var routeJob: Job? = null
    private var lastRecalcAtMs: Long = 0L
    private var lastUserFix: LatLng? = null

    fun setDeliveryActive(active: Boolean) {
        _deliveryActive.value = active
        if (!active) {
            stopNavigation()
        }
    }

    fun selectStop(stopId: Long?) {
        _selectedStopId.value = stopId
    }

    fun beginAddStopAt(latitude: Double, longitude: Double) {
        _pendingPin.value = PendingStopPin(latitude, longitude)
    }

    fun cancelPendingPin() {
        _pendingPin.value = null
    }

    fun confirmPendingStop(name: String, notes: String = "") {
        val pin = _pendingPin.value ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.addStop(
                routeId,
                StopDraft(
                    name = trimmed,
                    notes = notes.trim(),
                    latitude = pin.latitude,
                    longitude = pin.longitude,
                ),
            )
            _pendingPin.value = null
        }
    }

    fun updateStopDetails(
        stopId: Long,
        name: String,
        notes: String,
        addressHint: String = "",
    ) {
        viewModelScope.launch {
            val existing = route.value?.orderedStops?.firstOrNull { it.id == stopId } ?: return@launch
            repository.updateStop(
                existing.copy(
                    name = name.trim().ifEmpty { existing.name },
                    notes = notes.trim(),
                    addressHint = addressHint.trim(),
                ),
            )
        }
    }

    fun addStopAtCurrentLocation(name: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            repository.addStop(
                routeId,
                StopDraft(
                    name = name.trim().ifEmpty { "Current location" },
                    latitude = latitude,
                    longitude = longitude,
                ),
            )
        }
    }

    fun setStopCompleted(stopId: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.setStopCompleted(stopId, completed)
        }
    }

    fun completeNextStop(userLocation: LatLng? = lastUserFix) {
        val stops = route.value?.orderedStops.orEmpty()
        val next = stops.firstOrNull { !it.isCompleted } ?: return
        viewModelScope.launch {
            repository.setStopCompleted(next.id, true)
            if (!_deliveryActive.value) {
                stopNavigation()
                return@launch
            }
            val following = stops.firstOrNull { it.id != next.id && !it.isCompleted }
            val fix = userLocation ?: lastUserFix
            if (following == null) {
                _navigation.value = NavigationUiState(phase = NavigationPhase.Arrived)
                return@launch
            }
            val lat = following.latitude
            val lng = following.longitude
            if (fix == null) {
                _navigation.value = NavigationUiState(
                    phase = NavigationPhase.Error,
                    targetStopId = following.id,
                    errorMessage = "Waiting for GPS to route to the next stop",
                )
                return@launch
            }
            if (lat == null || lng == null) {
                _navigation.value = NavigationUiState(
                    phase = NavigationPhase.Error,
                    targetStopId = following.id,
                    errorMessage = "Next stop has no map pin",
                )
                return@launch
            }
            fetchRoute(
                from = fix,
                to = LatLng(lat, lng),
                targetStopId = following.id,
                fitMap = true,
            )
        }
    }

    fun startNavigationToNext(userLocation: LatLng) {
        lastUserFix = userLocation
        val next = route.value?.orderedStops?.firstOrNull { !it.isCompleted }
        if (next == null) {
            _navigation.value = NavigationUiState(
                phase = NavigationPhase.Arrived,
                errorMessage = null,
            )
            return
        }
        startNavigationToStop(next, userLocation)
    }

    fun startNavigationToStop(stop: StopEntity, userLocation: LatLng) {
        lastUserFix = userLocation
        val lat = stop.latitude
        val lng = stop.longitude
        if (lat == null || lng == null) {
            _navigation.value = NavigationUiState(
                phase = NavigationPhase.Error,
                targetStopId = stop.id,
                errorMessage = "This stop has no map pin",
            )
            return
        }
        _deliveryActive.value = true
        fetchRoute(
            from = userLocation,
            to = LatLng(lat, lng),
            targetStopId = stop.id,
            fitMap = true,
        )
    }

    fun retryNavigation(userLocation: LatLng?) {
        val fix = userLocation ?: lastUserFix
        if (fix != null) startNavigationToNext(fix)
    }

    fun stopNavigation() {
        routeJob?.cancel()
        routeJob = null
        _navigation.value = NavigationUiState()
    }

    fun onUserLocationUpdated(user: LatLng) {
        lastUserFix = user
        val nav = _navigation.value
        if (nav.phase != NavigationPhase.Navigating && nav.phase != NavigationPhase.Arrived) return
        val driving = nav.route ?: return
        val targetId = nav.targetStopId ?: return
        val stop = route.value?.orderedStops?.firstOrNull { it.id == targetId } ?: return
        val lat = stop.latitude ?: return
        val lng = stop.longitude ?: return
        val dest = LatLng(lat, lng)

        val guidance = NavigationProgress.evaluate(driving, user, dest)
        _navigation.update {
            it.copy(
                guidance = guidance,
                phase = if (guidance.arrived) NavigationPhase.Arrived else NavigationPhase.Navigating,
            )
        }

        if (!guidance.arrived &&
            guidance.distanceToRouteMeters > NavigationProgress.offRouteThresholdMeters
        ) {
            val now = System.currentTimeMillis()
            if (now - lastRecalcAtMs >= 8_000L) {
                lastRecalcAtMs = now
                fetchRoute(from = user, to = dest, targetStopId = targetId, fitMap = false)
            }
        }
    }

    private fun fetchRoute(
        from: LatLng,
        to: LatLng,
        targetStopId: Long,
        fitMap: Boolean,
    ) {
        routeJob?.cancel()
        _navigation.update {
            it.copy(
                phase = NavigationPhase.LoadingRoute,
                targetStopId = targetStopId,
                errorMessage = null,
            )
        }
        routeJob = viewModelScope.launch {
            val result = routingClient.routeDriving(from, to)
            result.fold(
                onSuccess = { driving ->
                    lastRecalcAtMs = System.currentTimeMillis()
                    val guidance = NavigationProgress.evaluate(driving, from, to)
                    _navigation.update {
                        val fitToken = if (fitMap) it.navFitToken + 1 else it.navFitToken
                        it.copy(
                            phase = if (guidance.arrived) NavigationPhase.Arrived else NavigationPhase.Navigating,
                            targetStopId = targetStopId,
                            route = driving,
                            guidance = guidance,
                            errorMessage = null,
                            navLineJson = driving.toLineGeoJson(),
                            navFitToken = fitToken,
                        )
                    }
                },
                onFailure = { err ->
                    _navigation.update {
                        it.copy(
                            phase = NavigationPhase.Error,
                            targetStopId = targetStopId,
                            errorMessage = err.message ?: "Couldn’t fetch a driving route",
                            route = null,
                            guidance = null,
                            navLineJson = null,
                        )
                    }
                },
            )
        }
    }

    fun deliveryProgress(): DeliveryProgress {
        val stops = route.value?.orderedStops.orEmpty()
        val next = stops.firstOrNull { !it.isCompleted }
        val remaining = stops.count { !it.isCompleted }
        val stopIndex = next?.let { target ->
            stops.indexOfFirst { it.id == target.id }.takeIf { it >= 0 }?.plus(1) ?: 0
        } ?: stops.size
        val previous = next?.let { target ->
            stops.lastOrNull { it.position < target.position && it.latitude != null && it.longitude != null }
        }
        val approx = if (
            next?.latitude != null && next.longitude != null &&
            previous?.latitude != null && previous.longitude != null
        ) {
            val meters = GeoUtils.distanceMeters(
                previous.latitude!!,
                previous.longitude!!,
                next.latitude!!,
                next.longitude!!,
            )
            val bearing = GeoUtils.bearingDegrees(
                previous.latitude!!,
                previous.longitude!!,
                next.latitude!!,
                next.longitude!!,
            )
            "${GeoUtils.formatDistance(meters)} · ${GeoUtils.formatBearing(bearing)}"
        } else {
            null
        }
        return DeliveryProgress(
            nextStop = next,
            remaining = remaining,
            approxFromPrevious = approx,
            stopIndex = stopIndex,
            totalStops = stops.size,
        )
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
