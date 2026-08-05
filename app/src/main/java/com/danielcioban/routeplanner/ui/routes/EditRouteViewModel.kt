package com.danielcioban.routeplanner.ui.routes

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.StopDraft
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
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
)

data class EditRouteUiState(
    val routeName: String = "",
    val routeNotes: String = "",
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditRouteUiState(isLoading = routeId != null))
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
            val editable = routeWithStops.orderedStops.mapIndexed { index, stop ->
                EditableStop(
                    localId = stop.id.takeIf { it > 0 } ?: (index + 1L),
                    name = stop.name,
                    addressHint = stop.addressHint,
                    notes = stop.notes,
                    latitudeText = stop.latitude?.toString().orEmpty(),
                    longitudeText = stop.longitude?.toString().orEmpty(),
                    isCompleted = stop.isCompleted,
                    libraryStopId = stop.libraryStopId,
                )
            }.ifEmpty { emptyList() }
            nextLocalId = (editable.maxOfOrNull { it.localId } ?: 0) + 1
            _uiState.update {
                it.copy(
                    routeName = routeWithStops.route.name,
                    routeNotes = routeWithStops.route.notes,
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

    fun addStop() {
        _uiState.update {
            it.copy(stops = it.stops + EditableStop(localId = nextLocalId++))
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
            )
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorRes = null, errorArg = null) }
            try {
                val savedId = if (routeId == null) {
                    repository.createRoute(name, state.routeNotes, parsedStops)
                } else {
                    repository.updateRoute(routeId, name, state.routeNotes, parsedStops)
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
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditRouteViewModel(repository, routeId) as T
        }
    }
}
