package com.almica.ramani.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Success(val weather: WeatherResponse) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

class WeatherViewModel : ViewModel() {
    // 1. Hier wird die Instanz erstellt
    private val repository = WeatherRepository()

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState

    init {
        // 2. Wird direkt beim Start des ViewModels getriggert
        loadWeather()
    }

    fun loadWeather(lat: Double? = null, lon: Double? = null) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                // 3. HIER findet der tatsächliche Netzwerk-Aufruf statt!
                Timber.i("WeatherViewModel loadWeather: $lat, $lon")
                val data = if (lat != null && lon != null) {
                    repository.fetchCurrentWeather(lat, lon)
                } else {
                    repository.fetchCurrentWeather()
                }

                // 4. Die Daten werden in den UI-State geladen
                _uiState.value = WeatherUiState.Success(data)
                Timber.i("WeatherUiState Success weather_code: ${data.current.weather_code}")
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error(e.localizedMessage ?: "Unbekannter Fehler")
            }
        }
    }
}

