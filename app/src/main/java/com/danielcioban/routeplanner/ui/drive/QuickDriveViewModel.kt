package com.danielcioban.routeplanner.ui.drive

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.StopDraft
import com.danielcioban.routeplanner.data.delivery.DeliverySession
import com.danielcioban.routeplanner.data.delivery.DeliverySessionStore
import com.danielcioban.routeplanner.data.delivery.DriveKind
import com.danielcioban.routeplanner.data.local.TripStatus
import com.danielcioban.routeplanner.data.routing.DrivingRoute
import com.danielcioban.routeplanner.data.routing.NavigationProgress
import com.danielcioban.routeplanner.data.routing.OffRouteTracker
import com.danielcioban.routeplanner.data.routing.OsrmRoutingClient
import com.danielcioban.routeplanner.data.settings.AppSettings
import com.danielcioban.routeplanner.data.settings.StopGeofence
import com.danielcioban.routeplanner.data.settings.osrmOptions
import com.danielcioban.routeplanner.ui.map.LatLng
import com.danielcioban.routeplanner.ui.routes.NavigationPhase
import com.danielcioban.routeplanner.ui.routes.NavigationUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QuickDriveViewModel(
    private val repository: RouteRepository,
    private val deliverySessionStore: DeliverySessionStore,
    private val routingClient: OsrmRoutingClient = OsrmRoutingClient(),
    private val isOnline: () -> Boolean = { true },
) : ViewModel() {
    val session: StateFlow<DeliverySession> = deliverySessionStore.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeliverySession())

    private val _navigation = MutableStateFlow(NavigationUiState())
    val navigation: StateFlow<NavigationUiState> = _navigation.asStateFlow()

    @StringRes
    private val _noticeMessageRes = MutableStateFlow<Int?>(null)
    val noticeMessageRes: StateFlow<Int?> = _noticeMessageRes.asStateFlow()

    private var routeJob: Job? = null
    private val offRouteTracker = OffRouteTracker()
    private var lastUserFix: LatLng? = null
    private var lastAppSettings: AppSettings = AppSettings()
    private var started = false

    private val _sessionReady = MutableStateFlow<Boolean?>(null)
    val sessionReady: StateFlow<Boolean?> = _sessionReady.asStateFlow()

    init {
        viewModelScope.launch {
            val snap = deliverySessionStore.session.first()
            _sessionReady.value = snap.kind == DriveKind.QUICK &&
                snap.quickLatitude != null &&
                snap.quickLongitude != null
        }
    }

    fun consumeNotice() {
        _noticeMessageRes.value = null
    }

    fun destLatLng(): LatLng? {
        val snap = session.value
        val lat = snap.quickLatitude ?: return null
        val lng = snap.quickLongitude ?: return null
        return LatLng(lat, lng)
    }

    fun startIfNeeded(userLocation: LatLng) {
        lastUserFix = userLocation
        if (session.value.kind != DriveKind.QUICK) return
        if (started && _navigation.value.phase != NavigationPhase.Idle) return
        val dest = destLatLng() ?: return
        started = true
        fetchRoute(from = userLocation, to = dest, fitMap = true)
    }

    fun retry(userLocation: LatLng?) {
        val fix = userLocation ?: lastUserFix ?: return
        started = true
        fetchRoute(from = fix, to = destLatLng() ?: return, fitMap = true)
    }

    fun onUserLocationUpdated(user: LatLng, settings: AppSettings) {
        lastAppSettings = settings
        lastUserFix = user
        val nav = _navigation.value
        if (nav.phase != NavigationPhase.Navigating && nav.phase != NavigationPhase.Arrived) return
        val driving = nav.route ?: return
        val dest = destLatLng() ?: return
        val guidance = NavigationProgress.evaluate(
            driving,
            user,
            dest,
            settings.geofenceRadiusMeters.toDouble().coerceAtLeast(
                StopGeofence.DEFAULT_RADIUS_METERS.toDouble(),
            ),
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
            fetchRoute(from = user, to = dest, fitMap = false)
        }
    }

    fun confirmArrived(onDone: () -> Unit) {
        viewModelScope.launch {
            finish(TripStatus.COMPLETED, stopsCompleted = 1)
            onDone()
        }
    }

    fun cancel(onDone: () -> Unit) {
        viewModelScope.launch {
            finish(TripStatus.CANCELLED, stopsCompleted = 0)
            onDone()
        }
    }

    fun saveAsRoute(roundTrip: Boolean = false, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val snap = deliverySessionStore.session.first()
            val lat = snap.quickLatitude ?: return@launch
            val lng = snap.quickLongitude ?: return@launch
            val name = snap.quickName.orEmpty().ifBlank { "Destination" }
            val libId = snap.quickLibraryStopId ?: repository.upsertLibraryStop(
                name = name,
                addressHint = "",
                notes = "",
                latitude = lat,
                longitude = lng,
            )
            val routeId = repository.createRoute(
                name = name,
                notes = "",
                stops = listOf(
                    StopDraft(
                        name = name,
                        latitude = lat,
                        longitude = lng,
                        libraryStopId = libId,
                    ),
                ),
                roundTrip = roundTrip,
            )
            onCreated(routeId)
        }
    }

    private suspend fun finish(status: String, stopsCompleted: Int) {
        val tripId = deliverySessionStore.session.first().tripHistoryId
        if (tripId != null) {
            repository.finishTrip(tripId, status, stopsCompleted, 1)
        }
        deliverySessionStore.clear()
        started = false
        _navigation.value = NavigationUiState()
    }

    private fun fetchRoute(from: LatLng, to: LatLng, fitMap: Boolean) {
        routeJob?.cancel()
        _navigation.update {
            it.copy(phase = NavigationPhase.LoadingRoute, errorMessageRes = null)
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
            val previous = _navigation.value.route
            val driving = result.getOrElse {
                DrivingRoute.recoverAfterFailure(previous, from, to)
            }
            offRouteTracker.markRecalc()
            val radius = lastAppSettings.geofenceRadiusMeters.toDouble()
            val guidance = NavigationProgress.evaluate(driving, from, to, radius)
            _navigation.update {
                val fitToken = if (fitMap) it.navFitToken + 1 else it.navFitToken
                it.copy(
                    phase = if (guidance.arrived) NavigationPhase.Arrived else NavigationPhase.Navigating,
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

    class Factory(
        private val repository: RouteRepository,
        private val deliverySessionStore: DeliverySessionStore,
        private val isOnline: () -> Boolean = { true },
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return QuickDriveViewModel(repository, deliverySessionStore, isOnline = isOnline) as T
        }
    }
}
