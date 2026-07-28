package com.strongtogether.googlemapsjetpackcompose.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MapViewModel: ViewModel() {

    // State to hold the user's location as LatLng (latitude and longitude)
    private val _userLocation = mutableStateOf<LatLng?>(null)
    val userLocation: State<LatLng?> = _userLocation

    // State to hold the selected place location as LatLng
    private val _selectedLocation = mutableStateOf<LatLng?>(null)
    val selectedLocation: State<LatLng?> = _selectedLocation
    private val _selectedLocationName = mutableStateOf<String?>(null)
    val selectedLocationName: State<String?> = _selectedLocationName

    // Function to fetch the user's location and update the state
    fun fetchUserLocation(context: Context, fusedLocationClient: FusedLocationProviderClient) {
        // Check if the location permission is granted
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                // Fetch the last known location
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        // Update the user's location in the state
                        val userLatLng = LatLng(it.latitude, it.longitude)
                        _userLocation.value = userLatLng
                        Timber.i("userLatLng: $userLatLng")
                    }
                }
            } catch (e: SecurityException) {
                Timber.e("Permission for location access was revoked: ${e.localizedMessage}")
            }
        } else {
            Timber.e("Location permission is not granted.")
        }
    }

    // Function to geocode the selected place and update the selected location state
    fun selectLocation(selectedPlace: String, latLng: LatLng, context: Context) {
        Timber.i("selectedPlace: $selectedPlace latLng: $latLng")
        viewModelScope.launch {
            val geocoder = Geocoder(context)
            val geocoderAddress = if (selectedPlace.isNotEmpty())
                geocoder.getAddress(selectedPlace) else geocoder.getAddress(latLng.latitude, latLng.longitude)
            when (geocoderAddress) {
                null -> Timber.e("No location found for the selected place.")
                else -> {
                    geocoderAddress.let {
                        val address = it.getAddressLine(0)
                        val latLng = LatLng(geocoderAddress.latitude, geocoderAddress.longitude)
                        _selectedLocation.value = latLng
                        _selectedLocationName.value = address
                    }
                }
            }
        }
    }
}

private suspend fun Geocoder.getAddress(
    latitude: Double,
    longitude: Double,
): Address? = withContext(Dispatchers.IO) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCoroutine { cont ->
                getFromLocation(latitude, longitude, 1) {
                    cont.resume(it.firstOrNull())
                }
            }
        } else {
            suspendCoroutine { cont ->
                @Suppress("DEPRECATION")
                val address = getFromLocation(latitude, longitude, 1)?.firstOrNull()
                cont.resume(address)
            }
        }
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
}

private suspend fun Geocoder.getAddress(
    selectedPlace: String
): Address? = withContext(Dispatchers.IO) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCoroutine { cont ->
                getFromLocationName(selectedPlace, 1) {
                    cont.resume(it.firstOrNull())
                }
            }
        } else {
            suspendCoroutine { cont ->
                @Suppress("DEPRECATION")
                val address = getFromLocationName(selectedPlace, 1)?.firstOrNull()
                cont.resume(address)
            }
        }
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
}
