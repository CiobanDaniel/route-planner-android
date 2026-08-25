package com.danielcioban.routeplanner.ui.routes

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.AddFromLibraryResult
import com.danielcioban.routeplanner.data.DuplicateStopResult
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
import com.danielcioban.routeplanner.data.local.TaskTemplateEntity
import com.danielcioban.routeplanner.data.local.TripStatus
import com.danielcioban.routeplanner.data.routing.DrivingRoute
import com.danielcioban.routeplanner.data.routing.NavGuidance
import com.danielcioban.routeplanner.data.routing.NavigationProgress
import com.danielcioban.routeplanner.data.routing.OffRouteTracker
import com.danielcioban.routeplanner.data.routing.OsrmRoutingClient
import com.danielcioban.routeplanner.data.settings.AppSettings
import com.danielcioban.routeplanner.data.settings.GeofenceAction
import com.danielcioban.routeplanner.data.settings.GeofenceDwellTracker
import com.danielcioban.routeplanner.data.settings.RouteGeofenceMode
import com.danielcioban.routeplanner.data.settings.StopGeofence
import com.danielcioban.routeplanner.data.settings.osrmOptions
import com.danielcioban.routeplanner.ui.map.LatLng
import com.danielcioban.routeplanner.ui.trip.TripGuidanceService
import com.danielcioban.routeplanner.util.BarcodeMatch
import com.danielcioban.routeplanner.util.GeoUtils
import com.danielcioban.routeplanner.util.TripStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PendingStopPin(
    val latitude: Double,
    val longitude: Double,
    val suggestedName: String = "",
    val addressHint: String = "",
    val phone: String = "",
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

data class UndoMarkDone(
    val snapshot: StopEntity,
    val expiresAtMs: Long,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RouteDetailViewModel(
    private val repository: RouteRepository,
    private val routeId: Long,
    private val deliverySessionStore: DeliverySessionStore,
    private val routingClient: OsrmRoutingClient = OsrmRoutingClient(),
    private val isOnline: () -> Boolean = { true },
) : ViewModel() {
    private val reorderMutex = Mutex()

    val route: StateFlow<RouteWithStops?> = repository.observeRoute(routeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val stopLibrary: StateFlow<List<StopLibraryEntity>> = repository.observeStopLibrary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val otherRoutes: StateFlow<List<RouteWithStops>> = repository.observeRoutes()
        .map { list -> list.filter { it.route.id != routeId && !it.route.archived } }
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

    val taskTemplates: StateFlow<List<TaskTemplateEntity>> =
        repository.observeTaskTemplates()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _navigation = MutableStateFlow(NavigationUiState())
    val navigation: StateFlow<NavigationUiState> = _navigation.asStateFlow()

    private val _previewLineJson = MutableStateFlow<String?>(null)
    val previewLineJson: StateFlow<String?> = _previewLineJson.asStateFlow()
    private val _previewFitToken = MutableStateFlow(0)
    val previewFitToken: StateFlow<Int> = _previewFitToken.asStateFlow()

    private var routeJob: Job? = null
    private var previewJob: Job? = null
    private val offRouteTracker = OffRouteTracker()
    private var lastUserFix: LatLng? = null
    private var lastAppSettings: AppSettings = AppSettings()
    private var geofenceBusy = false
    private var lastGeofenceBlockStopId: Long? = null
    private val geofenceDwell = GeofenceDwellTracker()
    private var undoJob: Job? = null

    private val _undoMarkDone = MutableStateFlow<UndoMarkDone?>(null)
    val undoMarkDone: StateFlow<UndoMarkDone?> = _undoMarkDone.asStateFlow()

    val deliveryPaused: StateFlow<Boolean> = deliverySessionStore.session
        .map { it.paused && it.activeRouteId == routeId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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
                repository.completeOriginStops(routeId)
                ensureRouteTrip()
            } else {
                val remaining = route.value?.remainingDeliveryStops?.size ?: 0
                finishRouteTrip(cancelled = remaining > 0)
                deliverySessionStore.clearIfRoute(routeId)
            }
        }
        if (!active) {
            lastGeofenceBlockStopId = null
            stopNavigation()
        }
    }

    fun resetCompletions() {
        viewModelScope.launch {
            repository.resetStopCompletions(routeId)
            stopNavigation()
            _previewLineJson.value = null
        }
    }

    fun runAgain(userLocation: LatLng) {
        viewModelScope.launch {
            repository.resetStopCompletions(routeId)
            stopNavigation()
            _previewLineJson.value = null
            val snapshot = repository.getRoute(routeId) ?: return@launch
            val next = snapshot.nextIncompleteStop ?: snapshot.deliveryStops.firstOrNull() ?: return@launch
            startNavigationToStop(next, userLocation)
        }
    }

    fun previewPathToNext(userLocation: LatLng) {
        lastUserFix = userLocation
        val next = route.value?.nextIncompleteStop ?: return
        val lat = next.latitude ?: return
        val lng = next.longitude ?: return
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            val osrm = lastAppSettings.osrmOptions()
            val result = routingClient.routeForNavigation(
                userLocation,
                LatLng(lat, lng),
                profile = osrm.profile,
                exclude = osrm.exclude,
                walkLastMile = lastAppSettings.walkLastMile,
            )
            val driving = result.getOrElse {
                DrivingRoute.straightLine(
                    from = userLocation,
                    to = LatLng(lat, lng),
                    headInstruction = "",
                    arriveInstruction = "",
                )
            }
            _previewLineJson.value = driving.toLineGeoJson()
            _previewFitToken.update { it + 1 }
        }
    }

    fun endDelivery(resetCompletions: Boolean) {
        viewModelScope.launch {
            val remaining = route.value?.remainingDeliveryStops?.size ?: 0
            finishRouteTrip(cancelled = remaining > 0)
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
        phone: String = "",
    ) {
        _pendingPin.value = PendingStopPin(
            latitude = latitude,
            longitude = longitude,
            suggestedName = suggestedName,
            addressHint = addressHint,
            phone = phone,
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
                    phone = pin.phone,
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

    fun duplicateStopToRoute(stopId: Long, targetRouteId: Long) {
        viewModelScope.launch {
            _noticeMessageRes.value = when (repository.duplicateStopToRoute(stopId, targetRouteId)) {
                is DuplicateStopResult.Copied -> R.string.stop_copied
                is DuplicateStopResult.AlreadyOnRoute -> R.string.stop_copy_already
                DuplicateStopResult.SameRoute -> R.string.stop_copy_same_route
                DuplicateStopResult.Failed -> R.string.stop_copy_failed
            }
            if (_selectedStopId.value == stopId) {
                _selectedStopId.value = null
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
        arriveByMinutes: Int? = null,
        serviceMinutes: Int = 0,
        geofenceRadiusMeters: Int? = null,
        arriveByEpochMs: Long? = null,
        phone: String = "",
        doorCode: String = "",
        isFixedOrder: Boolean = false,
        isBreak: Boolean = false,
        codAmount: Double? = null,
        barcode: String? = null,
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
            }
            val latest = repository.getRoute(routeId)?.orderedStops
                ?.firstOrNull { it.id == stopId }
                ?: existing
            repository.updateStop(
                latest.copy(
                    name = trimmedName,
                    notes = trimmedNotes,
                    addressHint = if (libraryId == null) addressHint.trim() else latest.addressHint,
                    arriveByMinutes = arriveByMinutes,
                    serviceMinutes = serviceMinutes.coerceAtLeast(0),
            geofenceRadiusMeters = geofenceRadiusMeters?.let(StopGeofence::clampRadius),
                    arriveByEpochMs = arriveByEpochMs,
                    phone = phone.trim(),
                    doorCode = doorCode.trim(),
                    isFixedOrder = isFixedOrder,
                    isBreak = isBreak,
                    codAmount = (codAmount ?: latest.codAmount).coerceAtLeast(0.0),
                    barcode = barcode?.trim() ?: latest.barcode,
                ),
            )
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

    fun setGpsOrigin(name: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            repository.setGpsOrigin(routeId, name, latitude, longitude)
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
            syncTripProgress()
        }
    }

    fun addSelectedStopTask(title: String, required: Boolean) {
        val stopId = _selectedStopId.value ?: return
        viewModelScope.launch { repository.addStopTask(stopId, title, required) }
    }

    fun applyTemplateToSelected(templateId: Long) {
        val stopId = _selectedStopId.value ?: return
        viewModelScope.launch { repository.applyTaskTemplate(stopId, templateId) }
    }

    fun applyTemplateToRemainingFromSelected(templateId: Long) {
        val stopId = _selectedStopId.value ?: return
        viewModelScope.launch { repository.applyTaskTemplateToRemaining(stopId, templateId) }
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
            reorderMutex.withLock {
                repository.moveStop(routeId, stopId, delta = -1)
            }
        }
    }

    fun moveStopDown(stopId: Long) {
        viewModelScope.launch {
            reorderMutex.withLock {
                repository.moveStop(routeId, stopId, delta = 1)
            }
        }
    }

    fun moveStopToTop(stopId: Long) {
        viewModelScope.launch {
            reorderMutex.withLock {
                repository.moveStopToEdge(routeId, stopId, toStart = true)
            }
        }
    }

    fun moveStopToBottom(stopId: Long) {
        viewModelScope.launch {
            reorderMutex.withLock {
                repository.moveStopToEdge(routeId, stopId, toStart = false)
            }
            retargetNavigationIfNeeded()
        }
    }

    private suspend fun retargetNavigationIfNeeded() {
        if (!_deliveryActive.value) return
        val next = repository.getRoute(routeId)?.nextIncompleteStop ?: return
        if (next.id == _navigation.value.targetStopId) return
        val fix = lastUserFix
        if (fix != null) {
            startNavigationToStop(next, fix)
        } else {
            _navigation.value = NavigationUiState(
                phase = NavigationPhase.Error,
                targetStopId = next.id,
                errorMessageRes = R.string.nav_error_waiting_gps,
            )
        }
    }

    fun optimizeStopOrder(userLocation: LatLng? = lastUserFix) {
        viewModelScope.launch {
            val changed = repository.optimizeStopOrder(
                routeId,
                startLatitude = userLocation?.latitude,
                startLongitude = userLocation?.longitude,
                profile = lastAppSettings.osrmOptions().profile,
                exclude = lastAppSettings.osrmOptions().exclude,
                useRoadMatrix = !lastAppSettings.dataSaver,
            )
            _noticeMessageRes.value = if (changed) {
                R.string.optimize_done
            } else {
                R.string.optimize_unchanged
            }
        }
    }

    fun reverseRemainingStops() {
        viewModelScope.launch {
            val changed = repository.reverseRemainingStops(routeId)
            _noticeMessageRes.value = if (changed) {
                R.string.reverse_done
            } else {
                R.string.reverse_unchanged
            }
        }
    }

    fun sortRemainingByArriveBy() {
        viewModelScope.launch {
            val changed = repository.sortRemainingByArriveBy(routeId)
            _noticeMessageRes.value = if (changed) {
                R.string.sort_arrive_by_done
            } else {
                R.string.sort_arrive_by_unchanged
            }
        }
    }

    /**
     * Navigate to [stopId]. When [markPreviousDone] is true, unfinished stops before it
     * are marked complete (blocked by required tasks).
     */
    fun jumpToStop(
        stopId: Long,
        userLocation: LatLng? = lastUserFix,
        markPreviousDone: Boolean = false,
    ) {
        val stops = route.value?.deliveryStops.orEmpty()
        val target = stops.firstOrNull { it.id == stopId } ?: return
        if (target.isCompleted) return
        viewModelScope.launch {
            if (markPreviousDone) {
                when (repository.completeStopsBefore(routeId, target.id)) {
                    StopCompletionResult.Updated -> syncTripProgress()
                    is StopCompletionResult.BlockedByRequiredTasks -> {
                        _noticeMessageRes.value = R.string.tasks_required_before_stop_complete
                        return@launch
                    }
                    StopCompletionResult.StopMissing -> return@launch
                }
            }
            val fix = userLocation ?: lastUserFix
            if (fix != null) {
                startNavigationToStop(target, fix)
            } else {
                _navigation.value = NavigationUiState(
                    phase = NavigationPhase.Error,
                    targetStopId = target.id,
                    errorMessageRes = R.string.nav_error_waiting_gps,
                )
            }
        }
    }

    fun completeNextStop(userLocation: LatLng? = lastUserFix) {
        val next = route.value?.nextIncompleteStop ?: return
        viewModelScope.launch {
            rememberUndo(next)
            when (repository.setStopCompleted(next.id, true)) {
                StopCompletionResult.Updated -> advanceAfterCurrentStop(next.id, userLocation)
                is StopCompletionResult.BlockedByRequiredTasks -> {
                    clearUndo()
                    _noticeMessageRes.value = R.string.tasks_required_before_stop_complete
                }
                StopCompletionResult.StopMissing -> clearUndo()
            }
        }
    }

    fun completeNextStopDeferred(note: String, userLocation: LatLng? = lastUserFix) {
        val next = route.value?.nextIncompleteStop ?: return
        viewModelScope.launch {
            rememberUndo(next)
            when (
                repository.setStopCompleted(
                    next.id,
                    true,
                    deferRequiredTasks = true,
                    deferNote = note,
                )
            ) {
                StopCompletionResult.Updated -> advanceAfterCurrentStop(next.id, userLocation)
                else -> clearUndo()
            }
        }
    }

    fun failCurrentStop(
        reason: String,
        userLocation: LatLng? = lastUserFix,
        photoPath: String? = null,
        signaturePath: String? = null,
    ) {
        val next = route.value?.nextIncompleteStop ?: return
        viewModelScope.launch {
            rememberUndo(next)
            repository.failStop(next.id, reason, photoPath, signaturePath)
            advanceAfterCurrentStop(next.id, userLocation)
        }
    }

    fun rescheduleCurrentStop(
        arriveByMinutes: Int? = null,
        addMinutes: Int? = null,
        arriveByEpochMs: Long? = null,
    ) {
        val next = route.value?.nextIncompleteStop ?: return
        viewModelScope.launch {
            repository.rescheduleStop(next.id, arriveByMinutes, addMinutes, arriveByEpochMs)
            syncTripProgress()
            if (!_deliveryActive.value) return@launch
            val following = repository.getRoute(routeId)?.nextIncompleteStop ?: return@launch
            val fix = lastUserFix ?: return@launch
            startNavigationToStop(following, fix)
        }
    }

    fun setTripPaused(paused: Boolean) {
        viewModelScope.launch {
            deliverySessionStore.setPaused(paused)
            if (paused) stopNavigation()
        }
    }

    fun completeRemainingStops() {
        viewModelScope.launch {
            val result = repository.completeRemainingStops(routeId)
            _noticeMessageRes.value = if (result.blockedStopName != null) {
                R.string.bulk_complete_blocked
            } else {
                R.string.bulk_complete_done
            }
            syncTripProgress()
        }
    }

    fun undoLastCompletion() {
        val pending = _undoMarkDone.value ?: return
        viewModelScope.launch {
            repository.updateStop(pending.snapshot)
            clearUndo()
            syncTripProgress()
        }
    }

    private fun rememberUndo(before: StopEntity) {
        undoJob?.cancel()
        _undoMarkDone.value = UndoMarkDone(
            snapshot = before,
            expiresAtMs = System.currentTimeMillis() + 10_000L,
        )
        undoJob = viewModelScope.launch {
            delay(10_000L)
            _undoMarkDone.value = null
        }
    }

    private fun clearUndo() {
        undoJob?.cancel()
        _undoMarkDone.value = null
    }

    private suspend fun advanceAfterCurrentStop(completedStopId: Long, userLocation: LatLng?) {
        syncTripProgress()
        if (!_deliveryActive.value) {
            stopNavigation()
            return
        }
        val following = repository.getRoute(routeId)?.remainingDeliveryStops
            ?.firstOrNull { it.id != completedStopId }
        val fix = userLocation ?: lastUserFix
        if (following == null) {
            _navigation.value = NavigationUiState(phase = NavigationPhase.Arrived)
            syncTripProgress()
            finishRouteTrip(cancelled = false)
            return
        }
        val lat = following.latitude
        val lng = following.longitude
        if (fix == null) {
            _navigation.value = NavigationUiState(
                phase = NavigationPhase.Error,
                targetStopId = following.id,
                errorMessageRes = R.string.nav_error_waiting_gps,
            )
            return
        }
        if (lat == null || lng == null) {
            _navigation.value = NavigationUiState(
                phase = NavigationPhase.Error,
                targetStopId = following.id,
                errorMessageRes = R.string.nav_error_next_no_pin,
            )
            return
        }
        fetchRoute(
            from = fix,
            to = LatLng(lat, lng),
            targetStopId = following.id,
            fitMap = true,
        )
    }

    fun startReturnToStart(userLocation: LatLng) {
        val first = route.value?.deliveryStops.orEmpty()
            .firstOrNull { it.latitude != null && it.longitude != null }
            ?: route.value?.orderedStops.orEmpty()
                .firstOrNull { it.latitude != null && it.longitude != null }
            ?: return
        startNavigationToStop(first, userLocation)
    }

    fun startNavigationToNext(userLocation: LatLng) {
        lastUserFix = userLocation
        viewModelScope.launch {
            repository.completeOriginStops(routeId)
            val next = repository.getRoute(routeId)?.nextIncompleteStop
            if (next == null) {
                _navigation.value = NavigationUiState(
                    phase = NavigationPhase.Arrived,
                    errorMessageRes = null,
                )
                return@launch
            }
            startNavigationToStop(next, userLocation)
        }
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
        _previewLineJson.value = null
        viewModelScope.launch {
            ensureRouteTrip()
        }
        fetchRoute(
            from = userLocation,
            to = LatLng(lat, lng),
            targetStopId = stop.id,
            fitMap = true,
        )
    }

    fun retryNavigation(userLocation: LatLng?) {
        val fix = userLocation ?: lastUserFix ?: return
        val targetId = _navigation.value.targetStopId
        val target = targetId?.let { id ->
            route.value?.deliveryStops?.firstOrNull { it.id == id && !it.isCompleted }
        }
        if (target != null) {
            startNavigationToStop(target, fix)
        } else {
            startNavigationToNext(fix)
        }
    }

    fun stopNavigation() {
        routeJob?.cancel()
        routeJob = null
        _navigation.value = NavigationUiState()
    }

    fun onUserLocationUpdated(user: LatLng, settings: AppSettings = lastAppSettings) {
        lastAppSettings = settings
        lastUserFix = user
        maybeApplyGeofence(user, settings)
        val nav = _navigation.value
        if (nav.phase == NavigationPhase.Error &&
            nav.errorMessageRes == R.string.nav_error_waiting_gps
        ) {
            val target = nav.targetStopId?.let { id ->
                route.value?.deliveryStops?.firstOrNull { it.id == id && !it.isCompleted }
            }
            if (target != null) {
                startNavigationToStop(target, user)
            } else if (_deliveryActive.value) {
                startNavigationToNext(user)
            }
            return
        }
        if (nav.phase != NavigationPhase.Navigating && nav.phase != NavigationPhase.Arrived) return
        val driving = nav.route ?: return
        val targetId = nav.targetStopId ?: return
        val stop = route.value?.orderedStops?.firstOrNull { it.id == targetId } ?: return
        val lat = stop.latitude ?: return
        val lng = stop.longitude ?: return
        val dest = LatLng(lat, lng)

        val guidance = NavigationProgress.evaluate(
            driving,
            user,
            dest,
            arrivalRadiusFor(stop),
        )
        _navigation.update {
            it.copy(
                guidance = guidance,
                phase = if (guidance.arrived) NavigationPhase.Arrived else NavigationPhase.Navigating,
            )
        }

        if (!guidance.arrived &&
            offRouteTracker.shouldRecalc(
                guidance.distanceToRouteMeters,
                settings.rerouteAggressiveness,
            )
        ) {
            fetchRoute(from = user, to = dest, targetStopId = targetId, fitMap = false)
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
            val osrm = lastAppSettings.osrmOptions()
            val result = routingClient.routeForNavigation(
                from,
                to,
                profile = osrm.profile,
                exclude = osrm.exclude,
                walkLastMile = lastAppSettings.walkLastMile,
            )
            val previous = _navigation.value.route.takeIf { _navigation.value.targetStopId == targetStopId }
            val driving = result.getOrElse {
                DrivingRoute.recoverAfterFailure(previous, from, to)
            }
            offRouteTracker.markRecalc()
            val guidance = NavigationProgress.evaluate(
                driving,
                from,
                to,
                arrivalRadiusForTarget(targetStopId),
            )
            _navigation.update {
                val fitToken = if (fitMap) it.navFitToken + 1 else it.navFitToken
                it.copy(
                    phase = if (guidance.arrived) NavigationPhase.Arrived else NavigationPhase.Navigating,
                    targetStopId = targetStopId,
                    route = driving,
                    guidance = guidance,
                    errorMessageRes = com.danielcioban.routeplanner.ui.nav.routingBannerRes(
                        driving,
                        result.exceptionOrNull()?.message,
                        isOnline(),
                    ),
                    navLineJson = driving.toLineGeoJson(),
                    navFitToken = fitToken,
                )
            }
        }
    }

    fun deliveryProgress(): DeliveryProgress {
        val stops = route.value?.deliveryStops.orEmpty()
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

    private fun arrivalRadiusFor(stop: StopEntity): Double {
        return StopGeofence.effectiveRadiusMeters(
            stop.geofenceRadiusMeters,
            route.value?.route?.geofenceRadiusMeters,
            lastAppSettings.geofenceRadiusMeters,
        ).toDouble()
    }

    private fun arrivalRadiusForTarget(targetStopId: Long): Double {
        val stop = route.value?.orderedStops?.firstOrNull { it.id == targetStopId } ?: return 40.0
        return arrivalRadiusFor(stop)
    }

    private fun maybeApplyGeofence(user: LatLng, settings: AppSettings) {
        if (!_deliveryActive.value || geofenceBusy) {
            if (!_deliveryActive.value) geofenceDwell.reset()
            return
        }
        if (deliveryPaused.value) {
            geofenceDwell.reset()
            return
        }
        if (TripGuidanceService.isRunning) return
        val snapshot = route.value ?: return
        val action = StopGeofence.effectiveAction(
            RouteGeofenceMode.fromStored(snapshot.route.geofenceMode),
            settings.geofenceAction,
        )
        if (action == GeofenceAction.OFF) return
        geofenceBusy = true
        viewModelScope.launch {
            try {
                val nextId = snapshot.nextIncompleteStop?.id
                for (stop in snapshot.deliveryStops) {
                    if (stop.isCompleted) continue
                    val lat = stop.latitude ?: continue
                    val lng = stop.longitude ?: continue
                    val radius = StopGeofence.effectiveRadiusMeters(
                        stop.geofenceRadiusMeters,
                        snapshot.route.geofenceRadiusMeters,
                        settings.geofenceRadiusMeters,
                    )
                    val inside = StopGeofence.isInside(
                        user.latitude,
                        user.longitude,
                        lat,
                        lng,
                        radius,
                        user.accuracyMeters,
                    )
                    if (!geofenceDwell.ready(
                            stop.id,
                            inside,
                            System.currentTimeMillis(),
                            settings.geofenceDwellSeconds * 1000L,
                        )
                    ) {
                        continue
                    }
                    if (!stop.isVisited) {
                        repository.markStopVisited(stop.id)
                        _noticeMessageRes.value = R.string.geofence_visited
                    }
                    if (action != GeofenceAction.COMPLETE || stop.id != nextId) continue
                    when (repository.setStopCompleted(stop.id, true)) {
                        StopCompletionResult.Updated -> {
                            syncTripProgress()
                            _noticeMessageRes.value = R.string.geofence_completed
                            val nav = _navigation.value
                            val navigatingHere = nav.targetStopId == stop.id &&
                                (
                                    nav.phase == NavigationPhase.Navigating ||
                                        nav.phase == NavigationPhase.Arrived
                                    )
                            if (navigatingHere) {
                                val following = snapshot.remainingDeliveryStops.firstOrNull {
                                    it.id != stop.id
                                }
                                if (following == null) {
                                    _navigation.value = NavigationUiState(phase = NavigationPhase.Arrived)
                                } else {
                                    val nextLat = following.latitude
                                    val nextLng = following.longitude
                                    if (nextLat != null && nextLng != null) {
                                        fetchRoute(
                                            from = user,
                                            to = LatLng(nextLat, nextLng),
                                            targetStopId = following.id,
                                            fitMap = true,
                                        )
                                    }
                                }
                            }
                        }
                        is StopCompletionResult.BlockedByRequiredTasks -> {
                            if (lastGeofenceBlockStopId != stop.id) {
                                lastGeofenceBlockStopId = stop.id
                                _noticeMessageRes.value = R.string.tasks_required_before_stop_complete
                            }
                        }
                        StopCompletionResult.StopMissing -> Unit
                    }
                }
            } finally {
                geofenceBusy = false
            }
        }
    }

    private suspend fun ensureRouteTrip() {
        val snapshot = route.value ?: return
        val tripId = repository.startRouteTrip(
            routeId = routeId,
            title = snapshot.route.name,
            stopsTotal = snapshot.deliveryStops.size,
        )
        deliverySessionStore.setActiveRoute(routeId, tripId)
        repository.updateTripProgress(tripId, snapshot.completedCount, snapshot.deliveryStops.size)
    }

    private suspend fun syncTripProgress() {
        val tripId = deliverySessionStore.session.first().tripHistoryId ?: return
        val snapshot = route.value ?: return
        repository.updateTripProgress(tripId, snapshot.completedCount, snapshot.deliveryStops.size)
        if (snapshot.remainingDeliveryStops.isEmpty() && snapshot.deliveryStops.isNotEmpty()) {
            repository.finishTrip(
                tripId,
                TripStatus.COMPLETED,
                snapshot.completedCount,
                snapshot.deliveryStops.size,
                distanceMeters = TripStats.completedPathMeters(snapshot),
                lateStops = TripStats.lateStopCount(snapshot.deliveryStops),
            )
        }
    }

    private suspend fun finishRouteTrip(cancelled: Boolean) {
        val tripId = deliverySessionStore.session.first().tripHistoryId ?: return
        val snapshot = route.value
        val completed = snapshot?.completedCount ?: 0
        val total = snapshot?.deliveryStops?.size ?: 1
        val distance = snapshot?.let { TripStats.completedPathMeters(it) } ?: 0.0
        val late = snapshot?.let { TripStats.lateStopCount(it.deliveryStops) } ?: 0
        repository.finishTrip(
            tripId,
            if (cancelled) TripStatus.CANCELLED else TripStatus.COMPLETED,
            completed,
            total,
            distanceMeters = distance,
            lateStops = late,
        )
    }

    fun saveProofOfDelivery(photoPath: String?, signaturePath: String?) {
        val next = route.value?.nextIncompleteStop ?: return
        viewModelScope.launch {
            repository.saveProofOfDelivery(next.id, photoPath, signaturePath)
        }
    }

    fun setCodCollected(collected: Boolean) {
        val next = route.value?.nextIncompleteStop ?: return
        viewModelScope.launch { repository.setCodCollected(next.id, collected) }
    }

    fun applyScan(raw: String) {
        viewModelScope.launch {
            when (val hit = repository.matchScan(routeId, raw)) {
                is BarcodeMatch.Result.Stop -> {
                    selectStop(hit.stopId)
                    _noticeMessageRes.value = R.string.scan_matched_stop
                }
                is BarcodeMatch.Result.Task -> {
                    repository.setStopTaskCompleted(hit.taskId, true, "")
                    _noticeMessageRes.value = R.string.scan_matched_task
                }
                is BarcodeMatch.Result.Place -> {
                    repository.addStop(
                        routeId,
                        StopDraft(
                            name = hit.name,
                            latitude = hit.latitude,
                            longitude = hit.longitude,
                        ),
                    )
                    _noticeMessageRes.value = R.string.scan_added_pin
                }
                is BarcodeMatch.Result.Library -> {
                    val lib = repository.getLibraryStopByRemoteId(hit.libraryRemoteId)
                    if (lib != null) {
                        repository.addStopFromLibrary(routeId, lib.id)
                        _noticeMessageRes.value = R.string.scan_matched_stop
                    } else {
                        _noticeMessageRes.value = R.string.scan_no_match
                    }
                }
                BarcodeMatch.Result.None -> {
                    _noticeMessageRes.value = R.string.scan_no_match
                }
            }
        }
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
