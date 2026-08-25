package com.danielcioban.routeplanner.ui.routes

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.StopDraft
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import com.danielcioban.routeplanner.data.settings.RouteGeofenceMode
import com.danielcioban.routeplanner.data.settings.StopGeofence
import com.danielcioban.routeplanner.util.RouteOrderOptimizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditableStop(
    val localId: Long,
    val name: String = "",
    val addressHint: String = "",
    val notes: String = "",
    val latitudeText: String = "",
    val longitudeText: String = "",
    val isCompleted: Boolean = false,
    val libraryStopId: Long? = null,
    val arriveByMinutes: Int? = null,
    val serviceMinutes: Int = 0,
    val geofenceRadiusMeters: Int? = null,
    val isOrigin: Boolean = false,
    val arriveByEpochMs: Long? = null,
    val isFixedOrder: Boolean = false,
    val isBreak: Boolean = false,
    val phone: String = "",
    val doorCode: String = "",
    val codAmountText: String = "",
    val barcode: String = "",
)

data class EditRouteUiState(
    val routeName: String = "",
    val routeNotes: String = "",
    val roundTrip: Boolean = false,
    val geofenceMode: RouteGeofenceMode = RouteGeofenceMode.INHERIT,
    val geofenceRadiusMeters: Int? = null,
    val colorHex: String = "",
    val vanName: String = "",
    val shiftName: String = "",
    val shiftSlot: String = "",
    val stops: List<EditableStop> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    @param:StringRes val errorRes: Int? = null,
    /** Optional 1-based stop index for formatted error strings. */
    val errorArg: Int? = null,
    val savedRouteId: Long? = null,
)

class EditRouteViewModel(
    private val repository: RouteRepository,
    private val routeId: Long?,
    private val homeOriginLat: Double? = null,
    private val homeOriginLng: Double? = null,
    defaultRoundTrip: Boolean = false,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        EditRouteUiState(
            isLoading = routeId != null,
            roundTrip = routeId == null && defaultRoundTrip,
        ),
    )
    val uiState: StateFlow<EditRouteUiState> = _uiState.asStateFlow()

    val stopLibrary: StateFlow<List<StopLibraryEntity>> = repository.observeStopLibrary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var nextLocalId = 2L

    init {
        if (routeId != null) {
            loadExisting(routeId)
        }
    }

    private fun loadExisting(id: Long) {
        viewModelScope.launch {
            val routeWithStops = repository.getRoute(id)
            if (routeWithStops == null) {
                _uiState.update {
                    it.copy(isLoading = false, errorRes = R.string.edit_error_route_not_found, errorArg = null)
                }
                return@launch
            }
            val editable = routeWithStops.deliveryStops.mapIndexed { index, stop ->
                EditableStop(
                    localId = stop.id.takeIf { it > 0 } ?: (index + 1L),
                    name = stop.name,
                    addressHint = stop.addressHint,
                    notes = stop.notes,
                    latitudeText = stop.latitude?.toString().orEmpty(),
                    longitudeText = stop.longitude?.toString().orEmpty(),
                    isCompleted = stop.isCompleted,
                    libraryStopId = stop.libraryStopId,
                    arriveByMinutes = stop.arriveByMinutes,
                    serviceMinutes = stop.serviceMinutes,
                    geofenceRadiusMeters = stop.geofenceRadiusMeters,
                    isOrigin = false,
                    arriveByEpochMs = stop.arriveByEpochMs,
                    isFixedOrder = stop.isFixedOrder,
                    isBreak = stop.isBreak,
                    phone = stop.phone,
                    doorCode = stop.doorCode,
                    codAmountText = if (stop.codAmount > 0.0) stop.codAmount.toString() else "",
                    barcode = stop.barcode,
                )
            }.ifEmpty { emptyList() }
            nextLocalId = (editable.maxOfOrNull { it.localId } ?: 0) + 1
            _uiState.update {
                it.copy(
                    routeName = routeWithStops.route.name,
                    routeNotes = routeWithStops.route.notes,
                    roundTrip = routeWithStops.route.roundTrip,
                    geofenceMode = RouteGeofenceMode.fromStored(routeWithStops.route.geofenceMode),
                    geofenceRadiusMeters = routeWithStops.route.geofenceRadiusMeters,
                    colorHex = routeWithStops.route.colorHex,
                    vanName = routeWithStops.route.vanName,
                    shiftName = routeWithStops.route.shiftName,
                    shiftSlot = routeWithStops.route.shiftSlot,
                    stops = editable,
                    isLoading = false,
                )
            }
        }
    }

    fun updateRouteName(value: String) = _uiState.update {
        it.copy(routeName = value, errorRes = null, errorArg = null)
    }

    fun updateRouteNotes(value: String) = _uiState.update { it.copy(routeNotes = value) }

    fun updateRoundTrip(enabled: Boolean) = _uiState.update { it.copy(roundTrip = enabled) }

    fun updateGeofenceMode(mode: RouteGeofenceMode) = _uiState.update { it.copy(geofenceMode = mode) }

    fun updateGeofenceRadius(meters: Int?) = _uiState.update {
        it.copy(geofenceRadiusMeters = meters?.let(StopGeofence::clampRadius))
    }

    fun updateColorHex(value: String) = _uiState.update { it.copy(colorHex = value) }

    fun updateVanName(value: String) = _uiState.update { it.copy(vanName = value) }

    fun updateShiftName(value: String) = _uiState.update { it.copy(shiftName = value) }

    fun updateShiftSlot(value: String) = _uiState.update { it.copy(shiftSlot = value) }

    fun addStop() {
        _uiState.update {
            it.copy(stops = it.stops + EditableStop(localId = nextLocalId++))
        }
    }

    fun addPinnedStop(
        name: String,
        latitude: Double,
        longitude: Double,
        addressHint: String = "",
        phone: String = "",
    ) {
        _uiState.update { state ->
            state.copy(
                stops = state.stops + EditableStop(
                    localId = nextLocalId++,
                    name = name,
                    addressHint = addressHint,
                    latitudeText = latitude.toString(),
                    longitudeText = longitude.toString(),
                    phone = phone,
                ),
                errorRes = null,
                errorArg = null,
            )
        }
    }

    /** Copy a library stop into the draft list (persists on Save with [StopDraft.libraryStopId]). */
    fun addStopFromLibrary(libraryStopId: Long) {
        viewModelScope.launch {
            if (_uiState.value.stops.any { it.libraryStopId == libraryStopId }) {
                _uiState.update {
                    it.copy(errorRes = R.string.edit_error_already_on_route, errorArg = null)
                }
                return@launch
            }
            val lib = repository.getLibraryStop(libraryStopId) ?: return@launch
            repository.recordLibraryUse(libraryStopId)
            _uiState.update { state ->
                state.copy(
                    stops = state.stops + EditableStop(
                        localId = nextLocalId++,
                        name = lib.name,
                        addressHint = lib.addressHint,
                        notes = lib.notes,
                        latitudeText = lib.latitude.toString(),
                        longitudeText = lib.longitude.toString(),
                        libraryStopId = lib.id,
                        geofenceRadiusMeters = lib.defaultGeofenceRadiusMeters,
                    ),
                    errorRes = null,
                    errorArg = null,
                )
            }
        }
    }

    fun removeStop(localId: Long) {
        _uiState.update { state ->
            state.copy(stops = state.stops.filterNot { it.localId == localId })
        }
    }

    fun updateStop(localId: Long, transform: (EditableStop) -> EditableStop) {
        _uiState.update { state ->
            state.copy(stops = state.stops.map { if (it.localId == localId) transform(it) else it })
        }
    }

    fun moveStopUp(localId: Long) {
        _uiState.update { state ->
            val index = state.stops.indexOfFirst { it.localId == localId }
            if (index <= 0) return@update state
            val mutable = state.stops.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(index - 1, item)
            state.copy(stops = mutable)
        }
    }

    fun moveStopDown(localId: Long) {
        _uiState.update { state ->
            val index = state.stops.indexOfFirst { it.localId == localId }
            if (index < 0 || index >= state.stops.lastIndex) return@update state
            val mutable = state.stops.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(index + 1, item)
            state.copy(stops = mutable)
        }
    }

    fun moveStopToTop(localId: Long) {
        _uiState.update { state ->
            val index = state.stops.indexOfFirst { it.localId == localId }
            if (index <= 0) return@update state
            val mutable = state.stops.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(0, item)
            state.copy(stops = mutable)
        }
    }

    fun moveStopToBottom(localId: Long) {
        _uiState.update { state ->
            val index = state.stops.indexOfFirst { it.localId == localId }
            if (index < 0 || index >= state.stops.lastIndex) return@update state
            val mutable = state.stops.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(item)
            state.copy(stops = mutable)
        }
    }

    fun optimizeStopOrder() {
        _uiState.update { state ->
            val pinned = state.stops.count { stop ->
                stop.latitudeText.toDoubleOrNull() != null && stop.longitudeText.toDoubleOrNull() != null
            }
            if (pinned < 2) {
                return@update state.copy(errorRes = R.string.optimize_need_pins, errorArg = null)
            }
            val reordered = RouteOrderOptimizer.optimizeRemaining(
                state.stops,
                isCompleted = { it.isCompleted },
                latitude = { it.latitudeText.toDoubleOrNull() },
                longitude = { it.longitudeText.toDoubleOrNull() },
                roundTrip = state.roundTrip,
                isFixedOrder = { it.isFixedOrder },
                arriveByEpochMs = { it.arriveByEpochMs },
                arriveByMinutes = { it.arriveByMinutes },
                serviceMinutes = { it.serviceMinutes },
            )
            state.copy(stops = reordered, errorRes = null, errorArg = null)
        }
    }

    fun reverseStopOrder() {
        _uiState.update { state ->
            val reordered = RouteOrderOptimizer.reverseRemaining(state.stops) { it.isCompleted }
            if (reordered == state.stops) {
                return@update state.copy(errorRes = R.string.reverse_unchanged, errorArg = null)
            }
            state.copy(stops = reordered, errorRes = null, errorArg = null)
        }
    }

    fun sortStopsByArriveBy() {
        _uiState.update { state ->
            val reordered = RouteOrderOptimizer.sortRemainingByArriveBy(
                state.stops,
                isCompleted = { it.isCompleted },
                arriveByMinutes = { it.arriveByMinutes },
                arriveByEpochMs = { it.arriveByEpochMs },
                isFixedOrder = { it.isFixedOrder },
            )
            if (reordered == state.stops) {
                return@update state.copy(errorRes = R.string.sort_arrive_by_unchanged, errorArg = null)
            }
            state.copy(stops = reordered, errorRes = null, errorArg = null)
        }
    }

    fun save() {
        val state = _uiState.value
        val name = state.routeName.trim()
        if (name.isEmpty()) {
            _uiState.update {
                it.copy(errorRes = R.string.edit_error_name_required, errorArg = null)
            }
            return
        }

        val parsedStops = mutableListOf<StopDraft>()
        for ((index, stop) in state.stops.withIndex()) {
            val stopNumber = index + 1
            if (stop.name.isBlank()) {
                _uiState.update {
                    it.copy(errorRes = R.string.edit_error_stop_name, errorArg = stopNumber)
                }
                return
            }
            val latResult = parseCoordinate(stop.latitudeText, isLatitude = true, stopNumber = stopNumber)
            if (latResult is CoordParse.Invalid) {
                _uiState.update {
                    it.copy(errorRes = latResult.messageRes, errorArg = latResult.stopNumber)
                }
                return
            }
            val lonResult = parseCoordinate(stop.longitudeText, isLatitude = false, stopNumber = stopNumber)
            if (lonResult is CoordParse.Invalid) {
                _uiState.update {
                    it.copy(errorRes = lonResult.messageRes, errorArg = lonResult.stopNumber)
                }
                return
            }
            val lat = (latResult as CoordParse.Value).value
            val lon = (lonResult as CoordParse.Value).value
            if ((lat == null) != (lon == null)) {
                _uiState.update {
                    it.copy(errorRes = R.string.edit_error_stop_coords_pair, errorArg = stopNumber)
                }
                return
            }
            if (lat != null && (lat < -90.0 || lat > 90.0)) {
                _uiState.update {
                    it.copy(errorRes = R.string.edit_error_stop_lat_range, errorArg = stopNumber)
                }
                return
            }
            if (lon != null && (lon < -180.0 || lon > 180.0)) {
                _uiState.update {
                    it.copy(errorRes = R.string.edit_error_stop_lon_range, errorArg = stopNumber)
                }
                return
            }
            parsedStops += StopDraft(
                name = stop.name,
                addressHint = stop.addressHint,
                notes = stop.notes,
                latitude = lat,
                longitude = lon,
                isCompleted = stop.isCompleted,
                libraryStopId = stop.libraryStopId,
                arriveByMinutes = stop.arriveByMinutes,
                serviceMinutes = stop.serviceMinutes.coerceAtLeast(0),
                geofenceRadiusMeters = stop.geofenceRadiusMeters,
                isOrigin = false,
                skipLibrary = false,
                arriveByEpochMs = stop.arriveByEpochMs,
                isFixedOrder = stop.isFixedOrder,
                isBreak = stop.isBreak,
                phone = stop.phone,
                doorCode = stop.doorCode,
                codAmount = stop.codAmountText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                barcode = stop.barcode,
            )
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorRes = null, errorArg = null) }
            try {
                val savedId = if (routeId == null) {
                    repository.createRoute(
                        name,
                        state.routeNotes,
                        parsedStops,
                        roundTrip = state.roundTrip,
                        geofenceMode = state.geofenceMode.name,
                        geofenceRadiusMeters = state.geofenceRadiusMeters,
                        colorHex = state.colorHex,
                        vanName = state.vanName,
                        shiftName = state.shiftName,
                        shiftSlot = state.shiftSlot,
                        originLatitude = homeOriginLat,
                        originLongitude = homeOriginLng,
                    )
                } else {
                    repository.updateRoute(
                        routeId,
                        name,
                        state.routeNotes,
                        parsedStops,
                        roundTrip = state.roundTrip,
                        geofenceMode = state.geofenceMode.name,
                        geofenceRadiusMeters = state.geofenceRadiusMeters,
                        colorHex = state.colorHex,
                        vanName = state.vanName,
                        shiftName = state.shiftName,
                        shiftSlot = state.shiftSlot,
                    )
                    routeId
                }
                _uiState.update { it.copy(isSaving = false, savedRouteId = savedId) }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorRes = R.string.edit_error_save_failed, errorArg = null)
                }
            }
        }
    }

    private sealed class CoordParse {
        data class Value(val value: Double?) : CoordParse()
        data class Invalid(@param:StringRes val messageRes: Int, val stopNumber: Int) : CoordParse()
    }

    private fun parseCoordinate(text: String, isLatitude: Boolean, stopNumber: Int): CoordParse {
        val trimmed = text.trim().replace(',', '.')
        if (trimmed.isEmpty()) return CoordParse.Value(null)
        val value = trimmed.toDoubleOrNull()
            ?: return CoordParse.Invalid(
                messageRes = if (isLatitude) {
                    R.string.edit_error_stop_invalid_lat
                } else {
                    R.string.edit_error_stop_invalid_lon
                },
                stopNumber = stopNumber,
            )
        return CoordParse.Value(value)
    }

    class Factory(
        private val repository: RouteRepository,
        private val routeId: Long?,
        private val homeOriginLat: Double? = null,
        private val homeOriginLng: Double? = null,
        private val defaultRoundTrip: Boolean = false,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditRouteViewModel(
                repository,
                routeId,
                homeOriginLat,
                homeOriginLng,
                defaultRoundTrip,
            ) as T
        }
    }
}
