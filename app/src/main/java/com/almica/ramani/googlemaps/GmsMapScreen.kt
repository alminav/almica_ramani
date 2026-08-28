package com.almica.ramani.googlemaps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.LocationDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.DarkGray
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import com.almica.ramani.LatLngH
import com.almica.ramani.R
import com.almica.ramani.charts.theme.White
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.BackPressHandler
import com.almica.ramani.utils.formatDistM
import com.almica.ramani.utils.simpleStringTime
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import timber.log.Timber
import java.time.ZonedDateTime
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds

/**
 * Not used
 * called from GoogleMapsActivity
 */

@SuppressLint("MutableCollectionMutableState", "LocalContextGetResourceValueCall")
@Composable
fun GmsMapScreen(
    gmsMapViewModel: GmsMapViewModel,
    zoomLevel: Double,
    routeData: RouteData,
    latLng: LatLng,
    cloudStyle: Boolean,
    backPressed: (FusedLocationProviderClient) -> Unit
) {
    val uiState by gmsMapViewModel.uiState.collectAsStateWithLifecycle()
    val userLocation = uiState.userLocation
    val userAltitude = uiState.userAltitude
    val userSpeed = uiState.userSpeed

    GmsMapScreenContent(
        userLocation = userLocation,
        userAltitude = userAltitude,
        userSpeed = userSpeed,
        zoomLevel = zoomLevel,
        routeData = routeData,
        latLng = latLng,
        cloudStyle = cloudStyle,
        onFetchUserLocation = { context, fusedLocationClient ->
            gmsMapViewModel.fetchUserLocation(context, fusedLocationClient)
        },
        backPressed = backPressed
    )
}

@SuppressLint("MutableCollectionMutableState", "LocalContextGetResourceValueCall")
@Composable
fun GmsMapScreenContent(
    userLocation: LatLng?,
    userAltitude: Double?,
    userSpeed: Float?,
    zoomLevel: Double,
    routeData: RouteData,
    latLng: LatLng,
    cloudStyle: Boolean,
    onFetchUserLocation: (Context, FusedLocationProviderClient) -> Unit,
    backPressed: (FusedLocationProviderClient) -> Unit
) {
    // Initialize the camera position state, which controls the camera's position on the map
    val cameraPositionState = rememberCameraPositionState()
    // Obtain the current context
    val context = LocalContext.current
    // Observe the user's location (Passed as parameter)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var zoom by remember { mutableFloatStateOf((zoomLevel+1).toFloat()) }
    var locationEnabled by remember { mutableStateOf(false) }

    // Create a mutable state to track the selected route
    var selectedRoute by remember { mutableStateOf<RouteData?>(null) }
    // Create a mutable state to track the selected circle
    var selectedCircle by remember { mutableStateOf<CircleInfo?>(null) }

    val circleData by remember { mutableStateOf(arrayListOf<CircleInfo>())}
    BackPressHandler {
        Timber.i( "Back Press intercepted")
        backPressed(fusedLocationClient)
    }
    var mapProperties by remember {
        mutableStateOf(MapProperties(mapType = MapType.NORMAL, //mapType = MapType.TERRAIN,
            //mapStyleOptions = mapStyle,
            isTrafficEnabled = false,
            minZoomPreference = 10f,
            maxZoomPreference = 18f,
            isMyLocationEnabled = true))
    }
    // Configuration of map UI
    var uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = !locationEnabled
                //mapToolbarEnabled = true,
            )
        )
    }

    //val gmo = GoogleMapOptions().mapId("b93c5dfa6c2af06ac7d3dd81")
    val mapOptions = remember {
        GoogleMapOptions()
            .mapId("b93c5dfa6c2af06ac7d3dd81")
    }
    // Observe the selected location from the ViewModel
    //val selectedLocation by mapViewModel.selectedLocation
    var selectedLocation by remember { mutableStateOf<LatLng?>(latLng) }

    // Animate camera when selectedLocation changes
    LaunchedEffect(selectedLocation) {
        selectedLocation?.let {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(it, zoom)
                ),
                durationMs = 200
            )
        }
    }

    // Handle permission requests for accessing fine location
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Fetch the user's location and update the camera if permission is granted
            onFetchUserLocation(context, fusedLocationClient)
        } else {
            // Handle the case when permission is denied
            Timber.e("Location permission was denied by the user.")
        }
    }

    // Request the location permission when the composable is launched
    LaunchedEffect(Unit) {
        delay(100.milliseconds)
        when (PackageManager.PERMISSION_GRANTED) {
            // Check if the location permission is already granted
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Fetch the user's location and update the camera
                onFetchUserLocation(context, fusedLocationClient)
            }
            else -> {
                // Request the location permission if it has not been granted
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    val scaleBackground = White.copy(alpha = 0.5f)
    val scaleBorderStroke = BorderStroke(width = 1.dp, DarkGray.copy(alpha = 0.2f))
    Column(modifier = Modifier.fillMaxSize()) {
/*
        Spacer(modifier = Modifier.height(18.dp)) // Add a spacer with a height of 18dp to push the search bar down

        // Add the search bar component
        SearchBar(
            onPlaceSelected = { place ->
                // When a place is selected from the search bar, update the selected location
                mapViewModel.selectLocation(place, context)
            }
        )
*/
        Box(Modifier.fillMaxSize()) {
            // Display the Google Map
            GoogleMap(
                contentPadding = PaddingValues(bottom = 64.dp, top = 100.dp),
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = uiSettings,
                googleMapOptionsFactory = { if (cloudStyle) mapOptions else GoogleMapOptions() }, // works only with mapType = MapType.NORMAL ??
                onMapLoaded = {
                    Timber.i("onMapLoaded")
                }, onMapClick = {
                    Timber.i("onMapClick")
                    selectedRoute = null
                    if (selectedCircle != null)
                        selectedCircle = null
                }
            ) {
                // handle userLocation from GmsMapViewModel
                userLocation?.let { latLng ->
                    val circleNumber = circleData.size.toString()
                    var dist = 0.0
                    circleData.forEachIndexed { i, circle ->
                        if (i > 0)
                            dist += SphericalUtil.computeDistanceBetween(circle.center, circleData[i-1].center)
                    }
                    val timeOffset = ZonedDateTime.now().offset.totalSeconds
                    val deltaTime = if (circleData.isNotEmpty())
                        System.currentTimeMillis() - circleData[0].time - timeOffset*1000 else 0L

                    val textDist = dist.formatDistM(true)
                    Timber.i( "circleNumber $circleNumber")
                    val avgSpeed = if (deltaTime.toDouble() > 0) (3.6 * dist / deltaTime.toDouble()) else 0.0
                    val circleInfo = CircleInfo(
                        textDist, latLng,
                        Date(deltaTime).simpleStringTime(),
                        System.currentTimeMillis(),
                        userAltitude,
                        userSpeed,
                        avgSpeed
                    )
                    circleData.add(circleInfo)
                    //Timber.i("circleData size: ${circleData.size}")
                    if (selectedCircle != null)
                        selectedCircle = circleInfo

                    //Timber.i("userLocation: $userLocation")
                    // Move the camera to the user's location with a zoom level of 10f cameraPositionState.position.zoom
                    //val cp = CameraPosition.builder().target(it).build()
                    if (locationEnabled)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, zoom)
                    val routeCoordinates = routeData.lllh
                    if (routeCoordinates.isNotEmpty()) {
                        val routePoints = routeCoordinates.map { it.latLng }
                        Timber.i("routeCoordinates size: ${routePoints.size}")
                        val routePattern = listOf(Dash(20f), Gap(20f), Dash(20f))

                        com.google.maps.android.compose.Polyline(
                            points = routePoints,
                            tag = routeData,
                            color = Color.Red,
                            width = 6f,
                            pattern = routePattern,
                            clickable = true,
                            onClick = { polyline ->
                                Timber.i("onclick ${routeData.name}")
                                selectedRoute = polyline.tag as? RouteData
                            }
                        )

                        val textDist = routeData.distance.formatDistM(true)
                        Marker(
                            state = rememberUpdatedMarkerState(
                                position = routePoints.first()
                            ),
                            title = routeData.name,
                            snippet = textDist,
                        )
                    } //else
                        //Timber.i("routeCoordinates is empty")
                }

                // Draw clickable circles for each location
                circleData.forEach { circleInfo ->
                    Circle(
                        center = circleInfo.center,
                        clickable = true,
                        fillColor = Color.Blue.copy(alpha = 0.3f),
                        radius = 8.0, // Specify the radius in meters
                        strokeColor = Color.Magenta,
                        strokeWidth = 2f,
                        tag = circleInfo,
                        onClick = { circle ->
                            // Handle circle click event
                            selectedCircle = circle.tag as? CircleInfo
                        }
                    )
                }
                // If a location was selected from the search bar, place a marker there
                selectedLocation?.let {
                    Marker(
                        state = rememberUpdatedMarkerState(position = it),
                        title = stringResource(R.string.selected_location),
                        snippet = stringResource(R.string.selected_location_snippet)
                    )
                }
            }

            selectedRoute?.let { route ->
                val textDist = route.distance.formatDistM( true)
                Timber.i("selectedRoute ${route.name} $textDist")

                Box(Modifier.align(alignment = Alignment.TopEnd).padding(top = 32.dp, start = 10.dp, end = 10.dp)
                    .clip(RoundedCornerShape(20))) {
                    Column(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(6.dp)
                    ) {
                        Text(text = route.name, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = textDist, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            selectedCircle?.let { circle ->
                Timber.i("circle: ${circle.name} ${circle.travelledTime}")

                Box(Modifier.align(alignment = Alignment.TopEnd).padding(top = 32.dp, start = 10.dp, end = 10.dp)
                    .clip(RoundedCornerShape(20))) {
                    Column(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(6.dp)
                    ) {
                        Text(text = circle.name, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = circle.travelledTime, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // scalebar
            Box(
                modifier = Modifier
                    .padding(top = 36.dp, start = 5.dp)
                    .align(Alignment.TopStart)
                    .background(
                        scaleBackground,
                        shape = MaterialTheme.shapes.medium
                    )
                    .border(
                        scaleBorderStroke,
                        shape = MaterialTheme.shapes.medium
                    ),
            ) {
                ScaleBar(
                    modifier = Modifier.padding(end = 4.dp),
                    cameraPositionState = cameraPositionState
                )
            }


            Box(modifier = Modifier.align(alignment = Alignment.TopEnd).padding(top = 32.dp)) {
                IconButton(
                    onClick = {
                        locationEnabled = !locationEnabled
                        if (locationEnabled) selectedLocation = null
                        uiSettings = uiSettings.copy(zoomControlsEnabled = !locationEnabled)
                    }
                ) {
                    Icon(
                        if (locationEnabled) {
                            Icons.Filled.MyLocation
                        } else {
                            Icons.Outlined.LocationDisabled
                        },
                        contentDescription = null
                    )
                }
            }
            Row(modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(vertical = 46.dp)) {
                Box(modifier = Modifier.align(alignment = Alignment.CenterVertically)) {
                    Text(
                        text = MapType.TERRAIN.name,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = mapProperties.mapType == MapType.SATELLITE,
                    onCheckedChange = {
                        //uiSettings = uiSettings.copy(zoomControlsEnabled = it)
                        mapProperties = if (it) {
                            mapProperties.copy(mapType = MapType.SATELLITE)
                        } else {
                            if (cloudStyle)
                                mapProperties.copy(mapType = MapType.NORMAL)
                            else
                                mapProperties.copy(mapType = MapType.TERRAIN)
                        }
                    }
                )
                Box(modifier = Modifier.align(alignment = Alignment.CenterVertically)) {
                    Text(
                        text = MapType.SATELLITE.name,
                        fontSize = 10.sp
                    )
                }
                Box(modifier = Modifier.align(alignment = Alignment.CenterVertically).padding(start = 5.dp,  end = 5.dp)) {
                    Divider(
                        color = Color.DarkGray,
                        modifier = Modifier
                            .height(24.dp)
                            .width(2.dp)
                    )
                }
                Switch(
                    checked = mapProperties.isTrafficEnabled,
                    onCheckedChange = {
                        //uiSettings = uiSettings.copy(zoomControlsEnabled = it)
                        mapProperties = if (it) {
                            mapProperties.copy(isTrafficEnabled = true)
                        } else {
                            mapProperties.copy(isTrafficEnabled = false) //, mapStyleOptions = null)
                        }
                    }
                )
                Box(modifier = Modifier.align(alignment = Alignment.CenterVertically)) {
                    Text(
                        text = stringResource(R.string.traffic),
                        fontSize = 10.sp
                    )
                }
            }
            // zoom
            Column(
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                AnimatedVisibility(visible = locationEnabled) {
                    Button(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.teal_200_trans)
                        ),
                        onClick = {
                            //val cameraModeClone = cameraMode
                            //cameraMode.intValue = CameraMode.NONE
                            zoom = zoom.plus(1f)
                            Timber.i("zoom: $zoom")
                        }) {
                        Text(
                            "+",
                            color = Color.White,
                            //modifier = Modifier.background(color = colorResource(R.color.teal_200)),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
                AnimatedVisibility(visible = locationEnabled) {
                    Button(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.teal_200_trans)
                        ),
                        onClick = {
                            //val cameraModeClone = cameraMode
                            //cameraMode.intValue = CameraMode.NONE
                            zoom = zoom.minus(1f)
                            Timber.i("zoom: $zoom")
                        },
                    ) {
                        Text(
                            "-",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }
        }
    }
}
data class CircleInfo(
    val name: String, val center: LatLng, val travelledTime: String, val time: Long,
    val altitude: Double?, val speed: Float?, val avgSpeed: Double
)

@Preview(showBackground = true)
@Composable
fun GmsMapScreenPreview() {
    val sampleLatLng = LatLng(-1.286389, 36.817223)
    val sampleRoute = RouteData(
        lllh = arrayListOf(
            LatLngH(sampleLatLng, 1600.0),
            LatLngH(LatLng(-1.287, 36.818), 1605.0)
        ),
        name = "Sample Route",
        distance = 500.0,
        state = true,
        routeMarkerDataList = null
    )
    RamaniTheme {
        GmsMapScreenContent(
            userLocation = sampleLatLng,
            userAltitude = 1600.0,
            userSpeed = 5.0f,
            zoomLevel = 15.0,
            routeData = sampleRoute,
            latLng = sampleLatLng,
            cloudStyle = false,
            onFetchUserLocation = { _, _ -> },
            backPressed = {}
        )
    }
}
