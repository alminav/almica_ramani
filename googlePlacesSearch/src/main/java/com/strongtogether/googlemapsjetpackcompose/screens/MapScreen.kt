package com.strongtogether.googlemapsjetpackcompose.screens

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.strongtogether.googlemapsjetpackcompose.viewmodel.MapViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.strongtogether.googlemapsjetpackcompose.R
import com.strongtogether.googlemapsjetpackcompose.utils.format
import timber.log.Timber
import java.util.Locale

@Composable
fun MapScreen(
    mapViewModel: MapViewModel,
    startLat: Double,
    startLon: Double,
    selectPlace: (String?, LatLng?) -> Unit
) {
    // Initialize the camera position state, which controls the camera's position on the map
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(startLat, startLon), 10F)
    }
    //val cameraPositionState = rememberCameraPositionState()
    // Obtain the current context
    val context = LocalContext.current
    // Observe the user's location from the ViewModel
    val userLocation by mapViewModel.userLocation
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Observe the selected location from the ViewModel
    val selectedLocation by mapViewModel.selectedLocation
    //val gmo = GoogleMapOptions().mapId("a5ab1e11e4024e9d26df1651")
    val mapOptions = remember {
        GoogleMapOptions()
            .mapId("b93c5dfa6c2af06ac7d3dd81")
            //.mapId("a5ab1e11e4024e9d26df1651")
//            .ambientEnabled(false)
//            .mapToolbarEnabled(false)
//            .compassEnabled(false)
    }
    var mapProperties by remember {
        mutableStateOf(MapProperties(mapType = MapType.NORMAL, //mapType = MapType.TERRAIN,
            //mapStyleOptions = mapStyle,
            isTrafficEnabled = false,
            isMyLocationEnabled = true))
    }
    var uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                myLocationButtonEnabled = true,
                zoomControlsEnabled = true,
                mapToolbarEnabled = false,
            )
        )
    }
    // Handle permission requests for accessing fine location
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Fetch the user's location and update the camera if permission is granted
            mapViewModel.fetchUserLocation(context, fusedLocationClient)
        } else {
            // Handle the case when permission is denied
            Timber.e("Location permission was denied by the user.")
        }
    }

    // Request the location permission when the composable is launched
    LaunchedEffect(Unit) {
        when (PackageManager.PERMISSION_GRANTED) {
            // Check if the location permission is already granted
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Fetch the user's location and update the camera
                mapViewModel.fetchUserLocation(context, fusedLocationClient)
                Timber.i("fetchUserLocation")
            }
            else -> {
                // Request the location permission if it has not been granted
                permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(18.dp)) // Add a spacer with a height of 18dp to push the search bar down
        var cityName by remember { mutableStateOf("") }
        val keyboardController = LocalSoftwareKeyboardController.current
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = cityName, onValueChange = { cityName = it },
                modifier = Modifier.weight(0.85f),
                label = { Text(stringResource(R.string.city_name)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
            )
            Box(modifier = Modifier.weight(0.15f)) {
                IconButton(
                    onClick = {
                        Timber.i("cityName: $cityName")
                        val position = cameraPositionState.position.target
                        position.let { mapViewModel.selectLocation(cityName, it, context) }
                        keyboardController?.hide()
                    }
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null
                    )
                }
            }
        }
        /**
         * AutoCompleteTextView does not work on Doogee 29jan2026
        // Add the search bar component
        SearchBar(
            onPlaceSelected = { place ->
                // When a place is selected from the search bar, update the selected location
                mapViewModel.selectLocation(place, context)
            }
        )
        */

        // Display the Google Map
        Timber.i("mapId: ${mapOptions.mapId}")
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            googleMapOptionsFactory = { GoogleMapOptions() }, //mapOptions },
            cameraPositionState = cameraPositionState,
            contentPadding = PaddingValues(bottom = 64.dp, top = 100.dp),
            properties = mapProperties,
            uiSettings = uiSettings
        ) {
            Timber.i("")
            // If the user's location is available, place a marker on the map
            userLocation?.let {
/*
                Marker(
                    state = MarkerState(position = it), // Place the marker at the user's location
                    //title = "Your Location", // Set the title for the marker
                    title = "lat:${it.latitude.format(4)}° lon:${it.longitude.format(4)}°",
                    snippet = "This is where you are currently located." // Set the snippet for the marker
                )
 */
                // Move the camera to the user's location with a zoom level of 10f
                cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 10f)
            }

            // If a location was selected from the search bar, place a marker there
            selectedLocation?.let {
                Marker(
                    state = rememberUpdatedMarkerState(position = it), // Place the marker at the selected location
                    title = "lat:${selectedLocation!!.latitude.format(4)}° lon:${selectedLocation!!.longitude.format(4)}°",
                        //"Select Location", // Set the title for the marker
                    snippet = mapViewModel.selectedLocationName.value,
                        //"This is the place you selected." // Set the snippet for the marker
                    onInfoWindowClick = {
                        Timber.i(mapViewModel.selectedLocationName.value)
                        selectPlace(mapViewModel.selectedLocationName.value, mapViewModel.selectedLocation.value)
                    }
                )
                // Move the camera to the selected location with a zoom level of 15f
                cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 15f)
            }
        }
    }
    // scalebar
    Box(
        modifier = Modifier
            .padding(top = 100.dp, start = 5.dp)
    ) {
        ScaleBar(
            modifier = Modifier.padding(end = 4.dp),
            cameraPositionState = cameraPositionState
        )
    }
}
