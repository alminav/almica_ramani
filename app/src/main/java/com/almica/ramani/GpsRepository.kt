package com.almica.ramani

import android.app.Application
import android.content.Intent
import com.almica.ramani.bglocationaccess.LocationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

class GpsRepository private constructor() {
    private val _distance = MutableStateFlow(0.0)
    val distance: StateFlow<Double> = _distance.asStateFlow()

    private val _speed = MutableStateFlow(0.0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _bearing = MutableStateFlow(0.0f)
    val bearing: StateFlow<Float> = _bearing.asStateFlow()

    private val _latitude = MutableStateFlow(0.0)
    val latitude: StateFlow<Double> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow(0.0)
    val longitude: StateFlow<Double> = _longitude.asStateFlow()

    private val _altitude = MutableStateFlow(0.0)
    val altitude: StateFlow<Double> = _altitude.asStateFlow()

    private val _travelledTime = MutableStateFlow(0L)
    val travelledTime: StateFlow<Long> = _travelledTime.asStateFlow()

    private val _time = MutableStateFlow(System.currentTimeMillis())
    val time: StateFlow<Long> = _time.asStateFlow()

    private val _stepCounter = MutableStateFlow(0)
    val stepCounter: StateFlow<Int> = _stepCounter.asStateFlow()

    private val _isTrackingEnabled = MutableStateFlow(true)
    val isTrackingEnabled: StateFlow<Boolean> = _isTrackingEnabled.asStateFlow()

    fun updateDistance(d: Double) { _distance.value = d }
    fun updateSpeed(v: Float) { _speed.value = v }
    fun updateBearing(v: Float) { _bearing.value = v }
    fun updateLatitude(lat: Double) { _latitude.value = lat }
    fun updateLongitude(lon: Double) { _longitude.value = lon }
    fun updateAltitude(alt: Double) { _altitude.value = alt }
    fun updateTravelledTime(t: Long) { _travelledTime.value = t }
    fun updateTime(t: Long) { _time.value = t }
    fun updateStepCounter(steps: Int) { _stepCounter.value = steps }
    fun updateTrackingEnabled(enabled: Boolean) {
        _isTrackingEnabled.value = enabled
        Timber.i("updateTrackingEnabled: $enabled")
    }

    companion object {
        @Volatile
        private var instance: GpsRepository? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: GpsRepository().also { instance = it }
            }
    }
}
