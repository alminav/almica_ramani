package com.almica.ramani.googlemaps


import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.almica.ramani.Const
import com.almica.ramani.GpsViewModel
import com.almica.ramani.R
import com.almica.ramani.locations.LocationRepository
import com.almica.ramani.utils.format
import com.almica.room.data.location.LocationEntity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.util.Date
import java.util.concurrent.Executors

private const val logtag = "MapViewModel"

data class GmsMapUiState(
    val userLocation: LatLng? = null,
    val userBearing: Float? = null,
    val userAltitude: Double? = null,
    val userSpeed: Float? = null,
    val locationEnabled: Boolean = true,
    val showLocationStatistic: Boolean = false,
    val gradientRouteData: RouteData? = null,
    val elevationRouteData: RouteData? = null,
    val simulationLatLngList: List<LatLng>? = null,
    val showDropDownMenu: Boolean = false,
    val showDropDownRasterMaptype: Boolean = false,
    val showHairCrossDropDownMenu: LatLng? = null,
    val showVehicleMenu: Boolean = false,
    val showGhFolders: Boolean = false,
    val showRouteRegionList: Boolean = false,
    val showRouteMgr: Boolean = false,
    val showPoiDatabase: Boolean = false,
    val showListMbTiles: Boolean = false,
    val routesRegionFilter: String = "",
    val routeData: RouteData? = null,
    val startLatLng: LatLng? = null,
    val tileProviderLatLng: LatLng? = null,
    val tileProviderMbTiles: Pair<String, String> = Pair("", ""),
    val snackMsg: String? = null,
    val zoom: Float = 10f,
    val selectedCircle: CircleInfo? = null,
    val selectedRoute: RouteData? = null,
    val highlightedRoute: RouteData? = null,
    val showRouteSavingScreen: Boolean = false,
    val circleData: List<CircleInfo> = emptyList(),
    val showPoiInfo: MapUtils.PoiInfo? = null,
    val tempMarkerLatLng: LatLng? = null,
    val animatedLatLng: LatLng? = null,
    val poiMarkerData: PoiMarkerData? = null,
    val stopMarkerData: PoiMarkerData? = null,
    val selectedLocation: LatLng? = null,
    val mapProperties: MapProperties = MapProperties(
        mapType = MapType.TERRAIN,
        isTrafficEnabled = false,
        minZoomPreference = 10f,
        maxZoomPreference = 18f,
        isMyLocationEnabled = true
    ),
    val tileOverlayVisibility: Boolean = true,
    val coordinatesOverlayVisibility: Boolean = false,
    val northUp: Boolean = true
)

class GmsMapViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GmsMapUiState())
    val uiState: StateFlow<GmsMapUiState> = _uiState.asStateFlow()

    var lastRoomLocation: Location? = null
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
        .setWaitForAccurateLocation(false)
        .setMinUpdateDistanceMeters(5f)
        .setMinUpdateIntervalMillis(1000)
        .setMaxUpdateDelayMillis(5000)
        .build()

    val locationRepository by lazy {
        LocationRepository.getInstance(application, Executors.newSingleThreadExecutor())
    }
    val preferences: android.content.SharedPreferences? by lazy {
        PreferenceManager.getDefaultSharedPreferences(application)
    }
    val altitudeCorrection by lazy {
        preferences?.getInt(
            application.getString(R.string.pref_gps_altitude_correction_key),
            Const.ALTITUDE_CORRECTION
        )
    }

    private val preferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == Const.PREF_GMS_NORTH_UP) {
            _uiState.update { it.copy(northUp = prefs.getBoolean(key, true)) }
        }
    }

    init {
        preferences?.let { prefs ->
            prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
            _uiState.update { it.copy(northUp = prefs.getBoolean(Const.PREF_GMS_NORTH_UP, true)) }
        }
    }

    override fun onCleared() {
        preferences?.unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }

    val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if (_uiState.value.locationEnabled) {
                result.locations.lastOrNull()?.let { location ->
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    val correctedAltitude = location.altitude + (altitudeCorrection?.toDouble() ?: 0.0)
                    
                    _uiState.update { state ->
                        state.copy(
                            userLocation = userLatLng,
                            userBearing = location.bearing,
                            userAltitude = location.altitude,
                            userSpeed = location.speed * 3.6f // Convert to km/h
                        )
                    }

                    GpsViewModel.loadSpeed(location.speed * 3.6f) // Convert to km/h
                    GpsViewModel.loadTime(location.time)
                    GpsViewModel.loadLatitude(userLatLng.latitude)
                    GpsViewModel.loadLongitude(userLatLng.longitude)
                    GpsViewModel.loadBearing(location.bearing)
                    GpsViewModel.loadAltitude(correctedAltitude)
                    addRoomLocation(location)
                }
            }
        }
    }

    fun addRoomLocation(currentLocation: Location) {
        var deltaDistRoom = Double.MIN_VALUE
        val currentUserLocation = _uiState.value.userLocation
        if (currentUserLocation != null) {
            if (lastRoomLocation == null) {
                lastRoomLocation = Location(currentLocation)
            } else {
                deltaDistRoom = SphericalUtil.computeDistanceBetween(
                    currentUserLocation,
                    LatLng(lastRoomLocation!!.latitude, lastRoomLocation!!.longitude)
                )
            }
        }

        if (deltaDistRoom > 20.0) {
            Timber.i("${deltaDistRoom.format(1)}m")

            lastRoomLocation = Location(currentLocation)
            val correctedAltitude = altitudeCorrection?.let { 0.0.coerceAtLeast(currentLocation.altitude + it) }
                ?: 0.0.coerceAtLeast(currentLocation.altitude)
            val locationEntity = LocationEntity(
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                altitude = correctedAltitude,
                speed = currentLocation.speed * 3.6f,
                bearing = currentLocation.bearing,
                hasBearing = currentLocation.hasBearing(),
                time = currentLocation.time,
                recordedAt = Date(currentLocation.time)
            )
            locationRepository.addLocation(locationEntity)
            Timber.i("addLocation: ${Date(currentLocation.time)}")
        }
    }

    fun initializeTileProvider(context: Context, latLng: LatLng) {
        _uiState.update { it.copy(
            tileProviderLatLng = latLng,
            tileProviderMbTiles = getMbTileName(context, latLng)
        ) }
    }

    fun stopUserLocation(fusedLocationClient: FusedLocationProviderClient) { // 15apr2026
        Timber.i("stopUserLocation")
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // Function to fetch the user's location and update the state
    fun fetchUserLocation(context: Context, fusedLocationClient: FusedLocationProviderClient) {
        Timber.i("fetchUserLocation")
        // Check if the location permission is granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                // Fetch the last known location
/*
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        // Update the user's location in the state
                        val userLatLng = LatLng(it.latitude, it.longitude)
                        _userLocation.value = userLatLng
                        Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: userLatLng: $userLatLng")
                    }
                }
 */
            } catch (e: SecurityException) {
                Timber.e( "${Thread.currentThread().stackTrace[2].lineNumber}: Permission for location access was revoked: ${e.localizedMessage}")
            }
        } else {
            Timber.e("${Thread.currentThread().stackTrace[2].lineNumber}:Location permission is not granted.")
        }
    }

    fun setLocationState(state: Boolean, latLng: LatLng) {
        _uiState.update { it.copy(
            locationEnabled = state,
            userLocation = if (!state) latLng else it.userLocation
        ) }
    }

    fun setShowDropDownMenu(show: Boolean) {
        _uiState.update { it.copy(showDropDownMenu = show) }
    }

    fun setShowHairCrossMenu(latLng: LatLng?) {
        _uiState.update { it.copy(showHairCrossDropDownMenu = latLng) }
    }

    fun setShowRouteSavingScreen(show: Boolean) {
        _uiState.update { it.copy(showRouteSavingScreen = show) }
    }

    fun setSnackMsg(msg: String?) {
        _uiState.update { it.copy(snackMsg = msg) }
    }

    fun updateState(transform: (GmsMapUiState) -> GmsMapUiState) {
        _uiState.update(transform)
    }

    // Function to geocode the selected place and update the selected location state
    fun selectLocation(selectedPlace: String, context: Context) {
        viewModelScope.launch {
            val geocoder = Geocoder(context)
            var addresses : MutableList<Address>? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(selectedPlace, 1) { ads ->
                    addresses = ads
                }
            } else {
                addresses = withContext(Dispatchers.IO) {
                    // Perform geocoding on a background thread
                    geocoder.getFromLocationName(selectedPlace, 1)
                }
            }
            if (!addresses.isNullOrEmpty()) {
                // Update the selected location in the state
                val address = addresses!![0]
                val latLng = LatLng(address.latitude, address.longitude)
                _uiState.update { it.copy(selectedLocation = latLng) }
            } else {
                Timber.e("${Thread.currentThread().stackTrace[2].lineNumber}:No location found for the selected place.")
            }
        }
    }
}
