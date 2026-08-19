package com.almica.ramani.routes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almica.ramani.GpsViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ElevationUiState(
    val locationBearing: Float = 0f,
    val locationSpeed: Float = 0f,
    val locationAltitude: Double = 0.0,
    val locationTime: Long = 0L,
    val latLng: LatLng? = null
)

class ElevationViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ElevationUiState())
    val uiState: StateFlow<ElevationUiState> = _uiState.asStateFlow()
    init {
        observeGpsData()
    }
    private fun observeGpsData() {
        viewModelScope.launch {
            launch {
                GpsViewModel.latitude.collectLatest { lat ->
                    _uiState.update { it.copy(latLng = LatLng(lat, it.latLng?.longitude ?: 0.0)) }
                    updateDataPoint()
                }
            }
            launch {
                GpsViewModel.longitude.collectLatest { lng ->
                    _uiState.update { it.copy(latLng = LatLng(it.latLng?.latitude ?: 0.0, lng)) }
                    updateDataPoint()
                }
            }
            launch {
                GpsViewModel.speed.collectLatest { speed ->
                    _uiState.update { it.copy(locationSpeed = speed * 3.6f) }
                    updateDataPoint()
                }
            }
            launch {
                GpsViewModel.bearing.collectLatest { bearing ->
                    _uiState.update { it.copy(locationBearing = bearing) }
                    updateDataPoint()
                }
            }
            launch {
                GpsViewModel.altitude.collectLatest { altitude ->
                    _uiState.update { it.copy(locationAltitude = altitude) }
                    updateDataPoint()
                }
            }
            launch {
                GpsViewModel.time.collectLatest { time ->
                    _uiState.update { it.copy(locationTime = time) }
                    updateDataPoint()
                }
            }
        }
    }
    private fun updateDataPoint() {}
}
