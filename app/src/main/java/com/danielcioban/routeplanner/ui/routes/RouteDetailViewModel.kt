package com.danielcioban.routeplanner.ui.routes

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.AddFromLibraryResult
import com.danielcioban.routeplanner.data.LibraryDeleteScope
import com.danielcioban.routeplanner.data.LibraryEditScope
import com.danielcioban.routeplanner.data.LibraryUsage
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.StopCompletionResult
import com.danielcioban.routeplanner.data.StopDraft
import com.danielcioban.routeplanner.data.delivery.DeliverySessionStore
import com.danielcioban.routeplanner.data.local.RouteWithStops
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import com.danielcioban.routeplanner.data.local.StopTaskEntity
import com.danielcioban.routeplanner.data.local.StopTaskProgress
import com.danielcioban.routeplanner.data.routing.DrivingRoute
import com.danielcioban.routeplanner.data.routing.NavGuidance
import com.danielcioban.routeplanner.data.routing.NavigationProgress
import com.danielcioban.routeplanner.data.routing.OsrmRoutingClient
import com.danielcioban.routeplanner.ui.map.LatLng
import com.danielcioban.routeplanner.util.GeoUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PendingStopPin(
    val latitude: Double,
    val longitude: Double,
    val suggestedName: String = "",
    val addressHint: String = "",
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
    @param:StringRes val errorMessageRes: Int? = null,
    val navLineJson: String? = null,
    val navFitToken: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RouteDetailViewModel(
    private val repository: RouteRepository,
    private val routeId: Long,
    private val deliverySessionStore: DeliverySessionStore,
    private val routingClient: OsrmRoutingClient = OsrmRoutingClient(),
    private val isOnline: () -> Boolean = { true },
) : ViewModel() {
    val route: StateFlow<RouteWithStops?> = repository.observeRoute(routeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val stopLibrary: StateFlow<List<StopLibraryEntity>> = repository.observeStopLibrary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _deliveryActive = MutableStateFlow(false)
    val deliveryActive: StateFlow<Boolean> = _deliveryActive.asStateFlow()

    private val _pendingPin = MutableStateFlow<PendingStopPin?>(null)
    val pendingPin: StateFlow<PendingStopPin?> = _pendingPin.asStateFlow()

    private val _noticeMessageRes = MutableStateFlow<Int?>(null)
    val noticeMessageRes: StateFlow<Int?> = _noticeMessageRes.asStateFlow()

    fun consumeNotice() {
        _noticeMessageRes.value = null
    }

    private val _selectedStopId = MutableStateFlow<Long?>(null)
    val selectedStopId: StateFlow<Long?> = _selectedStopId.asStateFlow()

    val selectedStopTasks: StateFlow<List<StopTaskEntity>> = selectedStopId
        .flatMapLatest { stopId ->
            if (stopId == null) flowOf(emptyList()) else repository.observeStopTasks(stopId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val taskProgressByStopId: StateFlow<Map<Long, StopTaskProgress>> =
        repository.observeStopTaskProgress(routeId)
            .map { list -> list.associateBy { it.stopId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _navigation = MutableStateFlow(NavigationUiState())
    val navigation: StateFlow<NavigationUiState> = _navigation.asStateFlow()

    private var routeJob: Job? = null
    private var lastRecalcAtMs: Long = 0L
    private var lastUserFix: LatLng? = null

    init {
        viewModelScope.launch {
            val session = deliverySessionStore.session.first()
            if (session.activeRouteId == routeId) {
                _deliveryActive.value = true
            }
        }
    }

    fun setDeliveryActive(active: Boolean) {
        _deliveryActive.value = active
        viewModelScope.launch {
            if (active) {
                deliverySessionStore.setActiveRoute(routeId)
            } else {
                deliverySessionStore.clearIfRoute(routeId)
            }
        }
        if (!active) stopNavigation()
    }

    fun resetCompletions() {
        viewModelScope.launch {
            repository.resetStopCompletions(routeId)
            stopNavigation()
        }
    }

    fun endDelivery(resetCompletions: Boolean) {
        viewModelScope.launch {
            if (resetCompletions) {
                repository.resetStopCompletions(routeId)
            }
            _deliveryActive.value = false
            deliverySessionStore.clearIfRoute(routeId)
            stopNavigation()
        }
    }

    fun selectStop(stopId: Long?) {
        _selectedStopId.value = stopId
    }

    fun beginAddStopAt(
        latitude: Double,
        longitude: Double,
        suggestedName: String = "",
        addressHint: String = "",
    ) {
        _pendingPin.value = PendingStopPin(
            latitude = latitude,
            longitude = longitude,
            suggestedName = suggestedName,
            addressHint = addressHint,
        )
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
                    addressHint = pin.addressHint.trim(),
                    latitude = pin.latitude,
                    longitude = pin.longitude,
                ),
            )
            _pendingPin.value = null
        }
    }

    fun addStopFromLibrary(libraryStopId: Long) {
        viewModelScope.launch {
            when (val result = repository.addStopFromLibrary(routeId, libraryStopId)) {
                is AddFromLibraryResult.Added -> {
                    _selectedStopId.value = result.stopId
                    _noticeMessageRes.value = R.string.library_added_to_route
                }
                is AddFromLibraryResult.AlreadyOnRoute -> {
                    _selectedStopId.value = result.stopId
                    _noticeMessageRes.value = R.string.library_already_on_route
                }
                AddFromLibraryResult.LibraryMissing -> {
                    _noticeMessageRes.value = R.string.library_missing
                }
            }
        }
    }

    fun saveSelectedStopToLibrary(stopId: Long) {
        viewModelScope.launch {
            val stop = route.value?.orderedStops?.firstOrNull { it.id == stopId } ?: return@launch
            val libId = repository.saveRouteStopToLibrary(stop) ?: return@launch
            if (stop.libraryStopId != libId) {
                repository.updateStop(stop.copy(libraryStopId = libId))
            }
            _noticeMessageRes.value = if (stop.libraryStopId != null) {
                R.string.library_updated_entry
            } else {
                R.string.library_saved_entry
            }
        }
    }

    fun refreshStopFromLibrary(stopId: Long) {
        viewModelScope.launch {
            val ok = repository.updateStopFromLibrary(stopId)
            _noticeMessageRes.value = if (ok) {
                R.string.library_refreshed_from
            } else {
                R.string.library_refresh_failed
            }
        }
    }

    fun updateStopDetails(
        stopId: Long,
        name: String,
        notes: String,
        addressHint: String = "",
        scope: LibraryEditScope = LibraryEditScope.Global,
    ) {
        viewModelScope.launch {
            val existing = route.value?.orderedStops?.firstOrNull { it.id == stopId } ?: return@launch
            val trimmedName = name.trim().ifEmpty { existing.name }
            val trimmedNotes = notes.trim()
            val lat = existing.latitude
            val lng = existing.longitude
            val libraryId = existing.libraryStopId
            if (libraryId != null && lat != null && lng != null) {
                repository.applyLibraryPlaceEdit(
                    libraryStopId = libraryId,
                    name = trimmedName,
                    addressHint = addressHint.trim().ifEmpty { existing.addressHint },
                    notes = trimmedNotes,
                    latitude = lat,
                    longitude = lng,
                    scope = scope,
                    routeStopId = stopId,
                )
            } else {
                repository.updateStop(
                    existing.copy(
                        name = trimmedName,
                        notes = trimmedNotes,
                        addressHint = addressHint.trim(),
                    ),
                )
            }
        }
    }

    fun deleteStop(
        stopId: Long,
        scope: LibraryDeleteScope = LibraryDeleteScope.ThisRouteOnly,
    ) {
        viewModelScope.launch {
            val existing = route.value?.orderedStops?.firstOrNull { it.id == stopId }
            val libraryId = existing?.libraryStopId
            if (libraryId != null) {
                repository.deleteLibraryStop(
                    id = libraryId,
                    scope = scope,
                    routeStopId = stopId,
                    routeId = routeId,
                )
            } else {
                repository.deleteStop(stopId, routeId)
            }
            if (_selectedStopId.value == stopId) {
                _selectedStopId.value = null
            }
            if (_navigation.value.targetStopId == stopId) {
                stopNavigation()
            }
        }
    }

    fun addStopAtCurrentLocation(name: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            repository.addStop(
                routeId,
                StopDraft(
                    name = name.trim(),
                    latitude = latitude,
                    longitude = longitude,
                ),
            )
        }
    }

    fun setStopCompleted(stopId: Long, completed: Boolean) {
        viewModelScope.launch {
            when (repository.setStopCompleted(stopId, completed)) {
                is StopCompletionResult.BlockedByRequiredTasks -> {
                    _noticeMessageRes.value = R.string.tasks_required_before_stop_complete
                }
                else -> Unit
            }
        }
    }

    fun addSelectedStopTask(title: String, required: Boolean) {
        val stopId = _selectedStopId.value ?: return
        viewModelScope.launch { repository.addStopTask(stopId, title, required) }
    }

    fun loadLibraryUsage(libraryStopId: Long, onResult: (LibraryUsage) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getLibraryUsage(libraryStopId))
        }
    }

    fun updateStopTask(taskId: Long, title: String, required: Boolean) {
        viewModelScope.launch { repository.updateStopTask(taskId, title, required) }
    }

    fun setStopTaskCompleted(taskId: Long, completed: Boolean, note: String) {
        viewModelScope.launch { repository.setStopTaskCompleted(taskId, completed, note) }
    }

    fun updateStopTaskCompletionNote(taskId: Long, note: String) {
        viewModelScope.launch { repository.updateStopTaskCompletionNote(taskId, note) }
    }

    fun deleteStopTask(taskId: Long) {
        viewModelScope.launch { repository.deleteStopTask(taskId) }
    }

    fun moveStopUp(stopId: Long) {
        viewModelScope.launch {
            repository.moveStop(routeId, stopId, delta = -1)
        }
    }

    fun moveStopDown(stopId: Long) {
        viewModelScope.launch {
            repository.moveStop(routeId, stopId, delta = 1)
        }
    }

    fun optimizeStopOrder(userLocation: LatLng? = lastUserFix) {
        viewModelScope.launch {
            val changed = repository.optimizeStopOrder(
                routeId,
                startLatitude = userLocation?.latitude,
                startLongitude = userLocation?.longitude,
            )
            _noticeMessageRes.value = if (changed) {
                R.string.optimize_done
            } else {
                R.string.optimize_unchanged
            }
        }
    }

    /**
     * Mark every unfinished stop before [stopId] as done, then navigate to that stop.
     */
    fun jumpToStop(stopId: Long, userLocation: LatLng? = lastUserFix) {
        val stops = route.value?.orderedStops.orEmpty()
        val target = stops.firstOrNull { it.id == stopId } ?: return
        if (target.isCompleted) return
        viewModelScope.launch {
            for (stop in stops) {
                if (stop.id == target.id) break
                if (!stop.isCompleted) {
                    when (repository.setStopCompleted(stop.id, true)) {
                        StopCompletionResult.Updated -> Unit
                        is StopCompletionResult.BlockedByRequiredTasks -> {
                            _noticeMessageRes.value = R.string.tasks_required_before_stop_complete
                            return@launch
                        }
                        StopCompletionResult.StopMissing -> return@launch
                    }
                }
            }
            val fix = userLocation ?: lastUserFix
            if (fix != null) {
                startNavigationToStop(target, fix)
            } else {
                _deliveryActive.value = true
                deliverySessionStore.setActiveRoute(routeId)
                _navigation.value = NavigationUiState(
                    phase = NavigationPhase.Error,
                    targetStopId = target.id,
                    errorMessageRes = R.string.nav_error_waiting_gps,
                )
            }
        }
    }

    fun completeNextStop(userLocation: LatLng? = lastUserFix) {
        val stops = route.value?.orderedStops.orEmpty()
        val next = stops.firstOrNull { !it.isCompleted } ?: return
        viewModelScope.launch {
            when (repository.setStopCompleted(next.id, true)) {
                StopCompletionResult.Updated -> Unit
                is StopCompletionResult.BlockedByRequiredTasks -> {
                    _noticeMessageRes.value = R.string.tasks_required_before_stop_complete
                    return@launch
                }
                StopCompletionResult.StopMissing -> return@launch
            }
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
                    errorMessageRes = R.string.nav_error_waiting_gps,
                )
                return@launch
            }
            if (lat == null || lng == null) {
                _navigation.value = NavigationUiState(
                    phase = NavigationPhase.Error,
                    targetStopId = following.id,
                    errorMessageRes = R.string.nav_error_next_no_pin,
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
                errorMessageRes = null,
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
                errorMessageRes = R.string.nav_error_no_pin,
            )
            return
        }
        _deliveryActive.value = true
        viewModelScope.launch {
            deliverySessionStore.setActiveRoute(routeId)
        }
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
                errorMessageRes = null,
            )
        }
        routeJob = viewModelScope.launch {
            val result = routingClient.routeDriving(from, to)
            val usedFallback = result.isFailure
            val driving = result.getOrElse {
                DrivingRoute.straightLine(
                    from = from,
                    to = to,
                    headInstruction = "",
                    arriveInstruction = "",
                )
            }
            lastRecalcAtMs = System.currentTimeMillis()
            val guidance = NavigationProgress.evaluate(driving, from, to)
            _navigation.update {
                val fitToken = if (fitMap) it.navFitToken + 1 else it.navFitToken
                it.copy(
                    phase = if (guidance.arrived) NavigationPhase.Arrived else NavigationPhase.Navigating,
                    targetStopId = targetStopId,
                    route = driving,
                    guidance = guidance,
                    // Soft hint when we fell back — distinguish offline vs roads unavailable.
                    errorMessageRes = when {
                        !usedFallback -> null
                        !isOnline() -> R.string.nav_approx_offline
                        else -> R.string.nav_approx_roads_unavailable
                    },
                    navLineJson = driving.toLineGeoJson(),
                    navFitToken = fitToken,
                )
            }
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
        private val deliverySessionStore: DeliverySessionStore,
        private val isOnline: () -> Boolean = { true },
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RouteDetailViewModel(
                repository = repository,
                routeId = routeId,
                deliverySessionStore = deliverySessionStore,
                isOnline = isOnline,
            ) as T
        }
    }
}
