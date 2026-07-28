package com.almica.ramani.bglocationaccess

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import timber.log.Timber


/**
 * @Author: Abdul Rehman
 * @Date: 06/05/2024.
 */
class GPSLocationClient {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var locationUpdatesCallBack: LocationUpdatesCallBack? = null
    private var locationCallback: LocationCallback? = null
    fun setLocationUpdatesCallBack(locationUpdatesCallBack: LocationUpdatesCallBack?) {
        this.locationUpdatesCallBack = locationUpdatesCallBack
    }

    fun getLocationUpdates(context: Context) {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled =
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!isGpsEnabled && !isNetworkEnabled) {
            locationUpdatesCallBack?.locationException("GPS is OFF")
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateDistanceMeters(5f)
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdateDelayMillis(5000)
            .build();


        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.locations.lastOrNull()?.let { location ->
                    locationUpdatesCallBack?.onLocationUpdate(location)
                }
            }
        }
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationCallback?.let {
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    it,
                    null
                )
            }
        } else {
            locationUpdatesCallBack?.locationException("Permission is not granted")
        }
    }

    fun stopLocations() {   // 09apr2026
        Timber.i("stopLocations")
        locationCallback?.let { fusedLocationProviderClient.removeLocationUpdates(it) }
    }
}