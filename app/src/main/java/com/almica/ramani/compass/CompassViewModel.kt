package com.almica.ramani.compass

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CompassViewModel : ViewModel() {
    private val _routeThumbnail: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val routeThumbnail = _routeThumbnail.asStateFlow()

    private val _haircrossThumbnail: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val haircrossThumbnail = _haircrossThumbnail.asStateFlow()

    private val _rotation: MutableStateFlow<Int> = MutableStateFlow(0)
    val rotation = _rotation.asStateFlow()

    private val _destination: MutableStateFlow<LatLng?> = MutableStateFlow(null)
    val destination = _destination.asStateFlow()

    private val _altitudeDestination: MutableStateFlow<Int?> = MutableStateFlow(0)
    val altitudeDestination = _altitudeDestination.asStateFlow()

    private val _distance: MutableStateFlow<Double?> = MutableStateFlow(null)
    val distance = _distance.asStateFlow()

    private val _currentAltitude: MutableStateFlow<Int?> = MutableStateFlow(0)
    val currentAltitude = _currentAltitude.asStateFlow()

    private val _currentLocation: MutableStateFlow<LatLng?> = MutableStateFlow(null)
    val currentLocation = _currentLocation.asStateFlow()

    private val _nearestPoiName: MutableStateFlow<String?> = MutableStateFlow(null)
    val nearestPoiName = _nearestPoiName.asStateFlow()

    private val _poiBmp: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val poiBmp = _poiBmp.asStateFlow()

    fun setRouteThumbnail(bmp: Bitmap?) {
        _routeThumbnail.value = bmp
        if (bmp == null) _haircrossThumbnail.value = null
    }

    fun setHaircrossThumbnail(bmp: Bitmap?) {
        _haircrossThumbnail.value = bmp
    }

    fun setRotation(r: Int) {
        _rotation.value = r
    }

    fun setDestination(latLng: LatLng?, altitude: Int?) {
        _destination.value = latLng
        _altitudeDestination.value = altitude
    }

    fun setDistance(distance: Double?) {
        _distance.value = distance
    }

    fun setCurrentLocation(latLng: LatLng?, altitude: Int?) {
        _currentLocation.value = latLng
        _currentAltitude.value = altitude
    }

    fun setNearestPoiName(name: String?) {
        _nearestPoiName.value = name
    }

    fun setPoiBmp(bmp: Bitmap?) {
        _poiBmp.value = bmp
    }

    companion object {
        // Static instance for global state access (legacy support)
        private val globalInstance = CompassViewModel()

        val routeThumbnail get() = globalInstance.routeThumbnail
        val haircrossThumbnail get() = globalInstance.haircrossThumbnail
        val rotation get() = globalInstance.rotation
        val destination get() = globalInstance.destination
        val altitudeDestination get() = globalInstance.altitudeDestination
        val distance get() = globalInstance.distance
        val currentAltitude get() = globalInstance.currentAltitude
        val currentLocation get() = globalInstance.currentLocation
        val nearestPoiName get() = globalInstance.nearestPoiName
        val poiBmp get() = globalInstance.poiBmp

        fun setRouteThumbnail(bmp: Bitmap?) = globalInstance.setRouteThumbnail(bmp)
        fun setHaircrossThumbnail(bmp: Bitmap?) = globalInstance.setHaircrossThumbnail(bmp)
        fun setRotation(r: Int) = globalInstance.setRotation(r)
        fun setDestination(latLng: LatLng?, altitude: Int?) = globalInstance.setDestination(latLng, altitude)
        fun setDistance(distance: Double?) = globalInstance.setDistance(distance)
        fun setCurrentLocation(latLng: LatLng?, altitude: Int?) = globalInstance.setCurrentLocation(latLng, altitude)
        fun setNearestPoiName(name: String?) = globalInstance.setNearestPoiName(name)
        fun setpoiBmp(bmp: Bitmap?) = globalInstance.setPoiBmp(bmp)
    }
}
