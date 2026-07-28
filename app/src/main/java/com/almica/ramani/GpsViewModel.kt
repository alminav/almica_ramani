package com.almica.ramani

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

class GpsViewModel(private val repository: GpsRepository = GpsRepository.getInstance()) : ViewModel() {
    val distance: StateFlow<Double> = repository.distance
    val speed: StateFlow<Float> = repository.speed
    val bearing: StateFlow<Float> = repository.bearing
    val latitude: StateFlow<Double> = repository.latitude
    val longitude: StateFlow<Double> = repository.longitude
    val altitude: StateFlow<Double> = repository.altitude
    val time: StateFlow<Long> = repository.time

    companion object {
        // Static access for legacy code and background service
        val distance: StateFlow<Double> get() = GpsRepository.getInstance().distance
        val speed: StateFlow<Float> get() = GpsRepository.getInstance().speed
        val bearing: StateFlow<Float> get() = GpsRepository.getInstance().bearing
        val latitude: StateFlow<Double> get() = GpsRepository.getInstance().latitude
        val longitude: StateFlow<Double> get() = GpsRepository.getInstance().longitude
        val altitude: StateFlow<Double> get() = GpsRepository.getInstance().altitude
        val travelledTime: StateFlow<Long> get() = GpsRepository.getInstance().travelledTime
        val time: StateFlow<Long> get() = GpsRepository.getInstance().time

        val stepCounterFlow: StateFlow<Int> get() = GpsRepository.getInstance().stepCounter

        var stepCounter: Int
            get() = GpsRepository.getInstance().stepCounter.value
            set(value) { GpsRepository.getInstance().updateStepCounter(value) }

        fun loadDistance(d: Double) = GpsRepository.getInstance().updateDistance(d)
        fun loadSpeed(v: Float) = GpsRepository.getInstance().updateSpeed(v)
        fun loadBearing(v: Float) = GpsRepository.getInstance().updateBearing(v)
        fun loadLatitude(lat: Double) = GpsRepository.getInstance().updateLatitude(lat)
        fun loadLongitude(lon: Double) = GpsRepository.getInstance().updateLongitude(lon)
        fun loadAltitude(alt: Double) = GpsRepository.getInstance().updateAltitude(alt)
        fun loadTravelledTime(t: Long) = GpsRepository.getInstance().updateTravelledTime(t)
        fun loadTime(t: Long) = GpsRepository.getInstance().updateTime(t)
    }
}
