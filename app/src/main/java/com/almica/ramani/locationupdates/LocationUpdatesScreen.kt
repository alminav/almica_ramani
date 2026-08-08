/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.almica.ramani.locationupdates

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.almica.ramani.Const
import com.almica.ramani.GpsViewModel
import com.almica.ramani.LocalLiveSharedPreferences
import com.almica.ramani.R
import com.almica.ramani.locations.LocationRepository
import com.almica.room.data.location.LocationDatabase
import com.almica.room.data.location.LocationEntity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import me.ibrahimsn.library.LiveSharedPreferences
import androidx.compose.runtime.livedata.observeAsState
import com.almica.ramani.GpsRepository
import timber.log.Timber
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.room.data.location.format
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


/**
 * 04aug2025
 * Maplibre internal location engine doesn't work on its own (Ulefone problem?).
 * In conjunction with a dedicated location manager, it works
 */
//private const val logtag = "LocationUpdatesScreen"
@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("MissingPermission")
@Composable
fun LocationUpdatesScreen() {
    //resetLocations(LocalContext.current)
    val permissions = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    // Requires at least coarse permission
    PermissionBox(
        permissions = permissions,
        requiredPermissions = listOf(permissions.first()),
    ) {
        LocationUpdatesContent()
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@RequiresApi(Build.VERSION_CODES.S)
@RequiresPermission(
    anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION],
)
@Composable
fun LocationUpdatesContent() {
    val liveSharedPreferences = LocalLiveSharedPreferences.current
    val context = LocalContext.current
    val locationRepository =
        LocationRepository.getInstance(context, Executors.newSingleThreadExecutor())

    // The location request that defines the location updates
    var locationRequest by remember {
        mutableStateOf<LocationRequest?>(null)
    }
    // Keeps track of received location updates as text
    var locationUpdates by remember {
        mutableStateOf("")
    }

    val startTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    locationRequest =
        LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, TimeUnit.SECONDS.toMillis(3))
            .setMinUpdateDistanceMeters(5f)
            .build()

    val altitudeCorrection by liveSharedPreferences
        .getInt(context.getString(R.string.pref_gps_altitude_correction_key), Const.ALTITUDE_CORRECTION)
        .observeAsState(Const.ALTITUDE_CORRECTION)
    
    //Timber.i( "${Thread.currentThread().getStackTrace()[2].lineNumber}: altitudeCorrection:$altitudeCorrection")
    var lastRoomLocation by remember { mutableStateOf<Location?>(null) }
    // Only register the location updates effect when we have a request
    if (locationRequest != null) {
        LocationUpdatesEffect(locationRequest!!) { result ->
            val distM = GpsViewModel.distance.value
            //Timber.i("GpsViewModel.distance distM: $distM")
            var deltaDistRoom: Double
            for (currentLocation in result.locations) {
                //Timber.i( "currentLocation.altitude: ${currentLocation.altitude}")
                locationUpdates = "${System.currentTimeMillis()}:\n" +
                        "- @lat: ${currentLocation.latitude}\n" +
                        "- @lng: ${currentLocation.longitude}\n" +
                        "- Accuracy: ${currentLocation.accuracy}\n\n" +
                        locationUpdates
                //GpsViewModel.latitude = currentLocation.latitude
                var deltaDist = 0.0
                GpsViewModel.longitude.value.let { modelLongitude ->
                    GpsViewModel.latitude.value.let { modelLatitude ->
                        deltaDist = if (modelLatitude != Double.NEGATIVE_INFINITY && modelLongitude != Double.NEGATIVE_INFINITY)
                            SphericalUtil.computeDistanceBetween(
                                LatLng(modelLatitude, modelLongitude),
                                LatLng(currentLocation.latitude, currentLocation.longitude)
                            )
                        else
                            0.0
                        //Timber.i("deltaDist: $deltaDist")
                    }
                }
                GpsViewModel.loadLatitude(currentLocation.latitude)
                //GpsViewModel.longitude = currentLocation.longitude
                GpsViewModel.loadLongitude(currentLocation.longitude)
                GpsViewModel.loadAltitude(currentLocation.altitude + altitudeCorrection) //Const.ALTITUDE_CORRECTION
                GpsViewModel.loadTravelledTime(currentLocation.time - startTime)
                GpsViewModel.loadTime(currentLocation.time)
                GpsViewModel.loadSpeed(currentLocation.speed)
                GpsViewModel.loadBearing(currentLocation.bearing)
                GpsViewModel.loadDistance(distM?.plus(deltaDist) ?: deltaDist)
                //Timber.i("GpsViewModel.distance: ${GpsViewModel.distance.value} deltaDist: $deltaDist")
                GpsViewModel.loadLatitude(currentLocation.latitude)
                GpsViewModel.loadLongitude(currentLocation.longitude)

                deltaDistRoom = if (lastRoomLocation == null) {
                    20.1 // force first tracking location
                } else {
                    SphericalUtil.computeDistanceBetween(
                        LatLng(currentLocation.latitude, currentLocation.longitude),
                        LatLng(lastRoomLocation!!.latitude, lastRoomLocation!!.longitude)
                    )
                }

                if (deltaDistRoom > 20.0) {
                    //Timber.i("${deltaDistRoom.format(1)}m")
                    lastRoomLocation = Location(currentLocation)
                    val locationEntity = LocationEntity(
                        latitude = currentLocation.latitude,
                        longitude = currentLocation.longitude,
                        altitude = if (currentLocation.altitude > 0) currentLocation.altitude + altitudeCorrection //Const.ALTITUDE_CORRECTION
                        else currentLocation.altitude,
                        speed = currentLocation.speed * 3.6f,
                        bearing = currentLocation.bearing,
                        hasBearing = currentLocation.hasBearing(),
                        time = currentLocation.time,
                        recordedAt = Date(currentLocation.time)
                    )
                    if (GpsRepository.getInstance().isTrackingEnabled.value) {
                        locationRepository.addLocation(locationEntity)
                        Timber.i( "addLocation: ${Date(currentLocation.time)} ${currentLocation.speed.format(1)}m/s")
                    }

                }
            }
        }
    } else
        Timber.e( "locationRequest = null")
}

/**
 * An effect that request location updates based on the provided request and ensures that the
 * updates are added and removed whenever the composable enters or exists the composition.
 */
@RequiresPermission(
    anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION],
)
@Composable
fun LocationUpdatesEffect(
    locationRequest: LocationRequest,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onUpdate: (result: LocationResult) -> Unit,
) {
    val context = LocalContext.current
    val currentOnUpdate by rememberUpdatedState(newValue = onUpdate)

    // Whenever on of these parameters changes, dispose and restart the effect.
    DisposableEffect(locationRequest, lifecycleOwner) {
        val locationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationCallback: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                //Timber.i( "LocationCallback altitude: ${result.locations[0].altitude}")
                currentOnUpdate(result)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    //Timber.i( "ON_START")
                    locationClient.requestLocationUpdates(
                        locationRequest, locationCallback, Looper.getMainLooper())
                }
                Lifecycle.Event.ON_STOP -> {
                    Timber.i( "ON_STOP removeLocationUpdates")
                    locationClient.removeLocationUpdates(locationCallback)
//                    saveLocations(context, 0L) {resultMessage ->
//                        Timber.i( "$resultMessage")
//                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    Timber.i( "$event")
                }

                Lifecycle.Event.ON_CREATE -> {}//Timber.i( "$event")
                Lifecycle.Event.ON_RESUME -> {}//Timber.i( "$event")
                Lifecycle.Event.ON_PAUSE -> {}//Timber.i( "$event")
                Lifecycle.Event.ON_ANY -> {}//Timber.i( "$event")
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer
        onDispose {
            Timber.i( "onDispose")
            locationClient.removeLocationUpdates(locationCallback)
            lifecycleOwner.lifecycle.removeObserver(observer)
//            saveLocations(context, 0L) {resultMessage ->
//                Timber.i( "$resultMessage")
//            }
        }
    }
}

fun resetLocations(activity: Context, invokeOnCompletion: (resultMessage: String) -> Unit) {
    val locationDb = LocationDatabase.getInstance(activity)
    locationDb.clearAllTables()
    // following two lines necessary for Confirmation?
    val locationCursor = locationDb.query("PRAGMA wal_checkpoint", arrayOf())
    locationCursor.moveToFirst()
    val feedback =  "reset locations ready"
    invokeOnCompletion(feedback)
}

@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("MissingPermission")
@Preview(showBackground = true)
@Composable
fun LocationUpdatesContentPreview() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("preview_prefs", Context.MODE_PRIVATE)
    val liveSharedPreferences = LiveSharedPreferences(prefs)
    RamaniTheme {
        CompositionLocalProvider(LocalLiveSharedPreferences provides liveSharedPreferences) {
            LocationUpdatesContent()
        }
    }
}
