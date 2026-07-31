package com.danielcioban.routeplanner.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danielcioban.routeplanner.data.RouteRepository
import com.danielcioban.routeplanner.data.StopDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
)

data class EditRouteUiState(
    val routeName: String = "",
    val routeNotes: String = "",
    val stops: List<EditableStop> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedRouteId: Long? = null,
)

class EditRouteViewModel(
    private val repository: RouteRepository,
    private val routeId: Long?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditRouteUiState(isLoading = routeId != null))
    val uiState: StateFlow<EditRouteUiState> = _uiState.asStateFlow()

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
                _uiState.update { it.copy(isLoading = false, errorMessage = "Route not found") }
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

    fun updateRouteName(value: String) = _uiState.update { it.copy(routeName = value, errorMessage = null) }

    fun updateRouteNotes(value: String) = _uiState.update { it.copy(routeNotes = value) }

    fun addStop() {
        _uiState.update {
            it.copy(stops = it.stops + EditableStop(localId = nextLocalId++))
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
            _uiState.update { it.copy(errorMessage = "Route name is required") }
            return
        }

        val parsedStops = mutableListOf<StopDraft>()
        for ((index, stop) in state.stops.withIndex()) {
            if (stop.name.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Stop ${index + 1} needs a name") }
                return
            }
            val latResult = parseCoordinate(stop.latitudeText, "latitude", index)
            if (latResult is CoordParse.Invalid) {
                _uiState.update { it.copy(errorMessage = latResult.message) }
                return
            }
            val lonResult = parseCoordinate(stop.longitudeText, "longitude", index)
            if (lonResult is CoordParse.Invalid) {
                _uiState.update { it.copy(errorMessage = lonResult.message) }
                return
            }
            val lat = (latResult as CoordParse.Value).value
            val lon = (lonResult as CoordParse.Value).value
            if ((lat == null) != (lon == null)) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Stop ${index + 1}: enter both latitude and longitude, or leave both empty",
                    )
                }
                return
            }
            if (lat != null && (lat < -90.0 || lat > 90.0)) {
                _uiState.update {
                    it.copy(errorMessage = "Stop ${index + 1}: latitude must be between -90 and 90")
                }
                return
            }
            if (lon != null && (lon < -180.0 || lon > 180.0)) {
                _uiState.update {
                    it.copy(errorMessage = "Stop ${index + 1}: longitude must be between -180 and 180")
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
            )
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val savedId = if (routeId == null) {
                    repository.createRoute(name, state.routeNotes, parsedStops)
                } else {
                    repository.updateRoute(routeId, name, state.routeNotes, parsedStops)
                    routeId
                }
                _uiState.update { it.copy(isSaving = false, savedRouteId = savedId) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "Could not save route")
                }
            }
        }
    }

    private sealed class CoordParse {
        data class Value(val value: Double?) : CoordParse()
        data class Invalid(val message: String) : CoordParse()
    }

    private fun parseCoordinate(text: String, label: String, stopIndex: Int): CoordParse {
        val trimmed = text.trim().replace(',', '.')
        if (trimmed.isEmpty()) return CoordParse.Value(null)
        val value = trimmed.toDoubleOrNull()
            ?: return CoordParse.Invalid("Stop ${stopIndex + 1}: invalid $label")
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
