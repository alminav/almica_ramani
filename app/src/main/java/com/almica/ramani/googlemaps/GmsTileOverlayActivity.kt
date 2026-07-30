package com.almica.ramani.googlemaps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.LocationDisabled
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.Const
import com.almica.ramani.Const.Companion.EXTRA_ACTIVITY
import com.almica.ramani.Const.Companion.EXTRA_LATLNG
import com.almica.ramani.GpsViewModel
import com.almica.ramani.Helpers
import com.almica.ramani.LatLngH
import com.almica.ramani.ListGhScreen
import com.almica.ramani.ListMbTilesScreen
import com.almica.ramani.R
import com.almica.ramani.RouteGeojsonList
import com.almica.ramani.VehicleMenu
import com.almica.ramani.charts.MbsElevationChart
import com.almica.ramani.charts.MbsGradientChart
import com.almica.ramani.charts.theme.Teal200
import com.almica.ramani.charts.theme.White
import com.almica.ramani.compass.CompassViewModel
import com.almica.ramani.googlemaps.MapUtils.downloadPoiInfo
import com.almica.ramani.googlemaps.MenuAction.ShowPoiDatabase
import com.almica.ramani.googlemaps.MenuAction.ShowRasterMapsList
import com.almica.ramani.googlemaps.MenuAction.ShowRasterMaptypePref
import com.almica.ramani.googlemaps.MenuAction.ShowRegionList
import com.almica.ramani.googlemaps.MenuAction.ShowRouteMgr
import com.almica.ramani.pois.PoiDatabaseScreen
import com.almica.ramani.pois.PoiItemAction
import com.almica.ramani.routes.RouteEntity
import com.almica.ramani.routes.RouteFileSaveMoBoSheet
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.BackPressHandler
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.GhHelper
import com.almica.ramani.utils.HgtReader
import com.almica.ramani.utils.formatDistM
import com.almica.ramani.utils.getDistanceFromLllh
import com.almica.ramani.utils.ghCalc
import com.almica.ramani.utils.gmsOnlineCalc
import com.almica.ramani.utils.offsetYByPercent
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.AdvancedMarker
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.ibrahimsn.library.LiveSharedPreferences
import org.maplibre.android.utils.BitmapUtils
import timber.log.Timber
import java.io.File
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class GmsTileOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val latitude = intent.getDoubleExtra(Const.EXTRA_LATITUDE, 0.0)
        val longitude = intent.getDoubleExtra(Const.EXTRA_LONGITUDE, 0.0)
        val latLng = LatLng(latitude, longitude)
        val zoom = intent.getDoubleExtra(Const.EXTRA_ZOOM, 10.0)
        val kmlString = intent.getStringExtra(Const.EXTRA_KMLSTRING)
        
        val routeData = kmlString?.let { Helpers.kmlString2RouteData(it) } ?: RouteData(arrayListOf(), "", 0.0, false, null)
        
        GpsViewModel.apply {
            loadDistance(0.0)
            loadLatitude(Double.NEGATIVE_INFINITY)
            loadLongitude(Double.NEGATIVE_INFINITY)
        }
        CompassViewModel.apply {
            setDestination(null, null)
            setRouteThumbnail(null)
            setCurrentLocation(null, null)
        }

        val preferences = getDefaultSharedPreferences(this)
        val routesRegionFilter = preferences.getString(Const.PREF_ROUTES_REGION_FILTER, getString(R.string.all)) ?: getString(R.string.all)

        val liveSharedPreferences = LiveSharedPreferences(preferences)
        liveSharedPreferences.getBoolean(Const.PREF_KEEP_SCREEN_ON, true).observe(this) { value ->
            if (value == true) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        setContent {
            RamaniTheme {
                val gmsMapViewModel: GmsMapViewModel = viewModel()
                GmsTileOverlayContent(
                    gmsMapViewModel = gmsMapViewModel,
                    initialLatLng = latLng,
                    startZoom = zoom,
                    initialRouteData = routeData,
                    initialRegionFilter = routesRegionFilter,
                    onBackPressed = { fusedLocationClient ->
                        gmsMapViewModel.stopUserLocation(fusedLocationClient)
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_ACTIVITY, this@GmsTileOverlayActivity::class.java.simpleName)
                            gmsMapViewModel.lastRoomLocation?.let {
                                putExtra(EXTRA_LATLNG, doubleArrayOf(it.latitude, it.longitude))
                            }
                        }
                        setResult(RESULT_OK, resultIntent)
                        //setResult(RESULT_OK)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun GmsTileOverlayContent(
    gmsMapViewModel: GmsMapViewModel,
    initialLatLng: LatLng,
    startZoom: Double,
    initialRouteData: RouteData?,
    initialRegionFilter: String,
    onBackPressed: (FusedLocationProviderClient) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val preferences = remember { getDefaultSharedPreferences(context) }
    val altitudeCorrection = remember { 
        preferences.getInt(resources.getString(R.string.pref_gps_altitude_correction_key), Const.ALTITUDE_CORRECTION)
    }
    Timber.d("altitudeCorrection: $altitudeCorrection")
    LaunchedEffect(Unit) {
        gmsMapViewModel.initializeTileProvider(context, initialLatLng)
        gmsMapViewModel.updateState { state -> state.copy(
            zoom = (startZoom + 1).toFloat(),
            routeData = initialRouteData,
            routesRegionFilter = initialRegionFilter,
            startLatLng = initialLatLng
        ) }
    }

    val uiState by gmsMapViewModel.uiState.collectAsStateWithLifecycle()

    GmsContent(
        viewModel = gmsMapViewModel,
        uiState = uiState,
        userAltitudeCorrection = altitudeCorrection,
        startZoom = startZoom,
        onBackPressed = onBackPressed
    )
}

@Composable
private fun GmsContent(
    viewModel: GmsMapViewModel,
    uiState: GmsMapUiState,
    userAltitudeCorrection: Int,
    startZoom: Double,
    onBackPressed: (FusedLocationProviderClient) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cameraPositionState = rememberCameraPositionState()

    val userAltitude = uiState.userAltitude?.plus(userAltitudeCorrection)

    BackPressHandler {
        onBackPressed(fusedLocationClient)
    }

    var uiSettings by remember {
        mutableStateOf(MapUiSettings(
            myLocationButtonEnabled = false,
            zoomControlsEnabled = !uiState.locationEnabled
        ))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.fetchUserLocation(context, fusedLocationClient)
        }
    }

    var routeEntities by remember { mutableStateOf<List<RouteEntity>>(emptyList()) }

    LaunchedEffect(uiState.animatedLatLng) {
        uiState.animatedLatLng?.let {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newCameraPosition(
                    CameraPosition(it, cameraPositionState.position.zoom, 0f,
                        if (uiState.northUp) 0F else cameraPositionState.position.bearing)
                ),
                durationMs = 200
            )
        }
    }

    var routeGeojsonFile by remember { mutableStateOf<File?>(null) }
    LaunchedEffect(routeGeojsonFile) {
        routeGeojsonFile?.let { file ->
            lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val entities = GeoJsonUtils.getRouteEntitiesFromGeojson(context, file)
                routeEntities = entities.sortedBy { it.region + it.name }
            }
        } ?: run {
            routeEntities = emptyList()
        }
    }

    LaunchedEffect(uiState.tempMarkerLatLng) {
        if (uiState.tempMarkerLatLng != null) {
            delay(2000.milliseconds)
            viewModel.updateState { state -> state.copy(tempMarkerLatLng = null) }
        }
    }

    LaunchedEffect(uiState.snackMsg) {
        if (uiState.snackMsg != null) {
            delay(5000.milliseconds)
            viewModel.updateState { state -> state.copy(snackMsg = null) }
        }
    }

    LaunchedEffect(Unit) {
        delay(100.milliseconds)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            viewModel.fetchUserLocation(context, fusedLocationClient)
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            contentPadding = PaddingValues(bottom = 64.dp, top = 64.dp),
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = uiState.mapProperties,
            uiSettings = uiSettings,
            onMapClick = {
                if (!uiState.locationEnabled) {
                    viewModel.updateState { state -> state.copy(selectedCircle = null) }
                }
                viewModel.updateState { state -> state.copy(
                    selectedRoute = null,
                    gradientRouteData = null,
                    elevationRouteData = null,
                    showPoiInfo = null,
                    showLocationStatistic = false
                ) }
            },
            onMyLocationClick = {
                viewModel.updateState { state -> state.copy(
                    showHairCrossDropDownMenu = LatLng(
                        cameraPositionState.position.target.latitude,
                        cameraPositionState.position.target.longitude
                    )
                ) }
            },
            onPOIClick = { poi ->
                downloadPoiInfo(context, poi.name, poi.latLng, poi.placeId) { poiInfo ->
                    viewModel.updateState { state -> state.copy(snackMsg = null, showPoiInfo = poiInfo) }
                }
                viewModel.updateState { state -> state.copy(snackMsg = poi.name) }
            },
        ) {
            MapOverlayContent(
                uiState = uiState,
                userAltitude = userAltitude,
                startZoom = startZoom,
                cameraPositionState = cameraPositionState,
                routeEntities = routeEntities,
                viewModel = viewModel,
                uiSettings = uiSettings,
                onUiSettingsChange = { uiSettings = it }
            )
        }

        MapDialogs(
            uiState = uiState,
            viewModel = viewModel,
            cameraPositionState = cameraPositionState,
            onUiSettingsChange = { uiSettings = it },
            onRouteGeojsonFileChange = { routeGeojsonFile = it }
        )

        MapControls(
            uiState = uiState,
            cameraPositionState = cameraPositionState,
            uiSettings = uiSettings,
            onUiSettingsChange = { uiSettings = it },
            viewModel = viewModel
        )
    }
}

@Composable
private fun MapOverlayContent(
    uiState: GmsMapUiState,
    userAltitude: Double?,
    startZoom: Double,
    cameraPositionState: CameraPositionState,
    routeEntities: List<RouteEntity>,
    viewModel: GmsMapViewModel,
    uiSettings: MapUiSettings,
    onUiSettingsChange: (MapUiSettings) -> Unit
) {
    val context = LocalContext.current
    
    if (uiState.tileOverlayVisibility) {
        uiState.tileProviderLatLng?.let { UpdateTileOverlay(it, uiState.tileProviderMbTiles.first) }
    }
    
    if (uiState.coordinatesOverlayVisibility) {
        UpdateCoordinateOverlay(uiState.tileProviderMbTiles.second)
    }

    LaunchedEffect(uiState.locationEnabled, uiState.userLocation) {
        Timber.d("locationEnabled: ${uiState.locationEnabled}, northup: ${uiState.northUp}, userLocation:" +
                " ${uiState.userLocation?.latitude} ${uiState.userLocation?.longitude}")
        if (uiState.locationEnabled) {
            uiState.userLocation?.let { latLng ->
                val cameraPositionBuilder = CameraPosition.builder()
//                if (!uiState.northUp) {
//                    uiState.userBearing?.let { cameraPositionBuilder.bearing(it) }
//                }
                cameraPositionBuilder.target(latLng).zoom(uiState.zoom).bearing(if (uiState.northUp) 0F else uiState.userBearing ?: 0F)
                cameraPositionState.position = cameraPositionBuilder.build()
                viewModel.updateState { state -> state.copy(startLatLng = null) }
            }
        }
    }

    LaunchedEffect(uiState.userLocation) {
        uiState.userLocation?.let { latLng ->
            val newCircle = processCircleData(ArrayList(uiState.circleData), latLng, uiState.userSpeed, userAltitude)
            if (newCircle != null) {
                viewModel.updateState { state -> state.copy(
                    circleData = state.circleData + newCircle,
                    selectedCircle = if (state.locationEnabled) newCircle else state.selectedCircle
                ) }
            }
        }
    }

    if (uiState.startLatLng != null) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(uiState.startLatLng, startZoom.toFloat())
    }

    uiState.circleData.forEach { circleInfo ->
        Circle(
            center = circleInfo.center,
            clickable = true,
            fillColor = Color.Blue.copy(alpha = 0.3f),
            radius = 16.0,
            strokeColor = Color.Magenta,
            strokeWidth = 2f,
            tag = circleInfo,
            onClick = { circle ->
                viewModel.updateState { state -> state.copy(selectedCircle = circle.tag as? CircleInfo) }
            }
        )
    }

    uiState.tempMarkerLatLng?.let {
        Marker(
            state = rememberUpdatedMarkerState(position = it),
            title = "",
            snippet = "$it",
        )
    }

    if (uiState.poiMarkerData != null) {
        val poiData = uiState.poiMarkerData
        Marker(
            state = rememberUpdatedMarkerState(position = poiData.latLng),
            title = poiData.name,
            snippet = poiData.description,
            onInfoWindowClick = {
                viewModel.updateState { state -> state.copy(poiMarkerData = null) }
            }
        )
    }

    uiState.stopMarkerData?.let {
        val icon = remember {
            val drawable = AppCompatResources.getDrawable(context, R.drawable.circle_filled_red_24px)
            BitmapUtils.getBitmapFromDrawable(drawable)?.let { BitmapDescriptorFactory.fromBitmap(it) }
        }
        AdvancedMarker(
            state = rememberUpdatedMarkerState(position = it.latLng),
            collisionBehavior = 1,
            icon = icon,
            anchor = Offset(0.5f, 0.5f),
            title = stringResource(R.string.stop_marker)
        )
    }

    ProcessRouteData(uiState.routeData, Color.Red, true, cameraPositionState.position.zoom, 3f, 10f) {
        viewModel.updateState { state -> state.copy(selectedRoute = uiState.routeData) }
    }

    if (routeEntities.isNotEmpty()) {
        ProcessGeojsonRoutes(cameraPositionState.position.zoom, routeEntities, uiState.highlightedRoute) { route ->
            viewModel.updateState { state -> state.copy(selectedRoute = route, highlightedRoute = route) }
        }
    }

    uiState.simulationLatLngList?.let { simulationLll ->
        val icon = remember {
            val drawable = AppCompatResources.getDrawable(context, R.drawable.ic_marker_auto)
            BitmapUtils.getBitmapFromDrawable(drawable)?.let { BitmapDescriptorFactory.fromBitmap(it) }
        }
        icon?.let {
            onUiSettingsChange(uiSettings.copy(zoomControlsEnabled = true))
            viewModel.updateState { state -> state.copy(locationEnabled = false, selectedCircle = null) }
            SimulateCarMovement(cameraPositionState, simulationLll, it, 10.0,
                moveMap = { latLng ->
                    viewModel.updateState { state -> state.copy(animatedLatLng = latLng) }
                }, {
                    viewModel.updateState { state -> state.copy(simulationLatLngList = null) }
                }
            )
        }
    }
}

@Composable
private fun MapControls(
    uiState: GmsMapUiState,
    cameraPositionState: CameraPositionState,
    uiSettings: MapUiSettings,
    onUiSettingsChange: (MapUiSettings) -> Unit,
    viewModel: GmsMapViewModel
) {
    val scaleBackground = White.copy(alpha = 0.5f)
    val scaleBorderStroke = BorderStroke(width = 1.dp, Color.DarkGray.copy(alpha = 0.2f))

    // Location Toggle
    Box(modifier = Modifier.fillMaxSize().padding(top = 36.dp), contentAlignment = Alignment.TopEnd) {
        IconButton(onClick = {
            val newState = !uiState.locationEnabled
            viewModel.setLocationState(newState, cameraPositionState.position.target)
            onUiSettingsChange(uiSettings.copy(zoomControlsEnabled = !newState))
            if (newState) {
                viewModel.updateState { state -> state.copy(simulationLatLngList = null, showPoiInfo = null) }
            }
        }) {
            Icon(
                imageVector = if (uiState.locationEnabled) Icons.Filled.MyLocation else Icons.Outlined.LocationDisabled,
                contentDescription = null
            )
        }
    }

    // Scale Bar
    Box(
        modifier = Modifier.padding(top = 36.dp, start = 5.dp)
            .background(scaleBackground, shape = MaterialTheme.shapes.medium),
    ) {
        ScaleBar(modifier = Modifier.padding(end = 4.dp), cameraPositionState = cameraPositionState)
    }

    // Bottom Start Menu Button
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 4.dp, start = 4.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        MapControlButton(
            imageVector = Icons.Outlined.Menu,
            contentDescription = stringResource(R.string.menu),
            onClick = { viewModel.setShowDropDownMenu(true) }
        )
    }

    // Bottom End Navigation Button
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 4.dp, end = 4.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        MapControlButton(
            imageVector = Icons.Outlined.Navigation,
            contentDescription = stringResource(R.string.navigation),
            onClick = { viewModel.setShowHairCrossMenu(cameraPositionState.position.target) }
        )
    }

    // Map Refresh Button
    GmsMapRefreshButton(cameraPositionState) { latLng, mbTiles ->
        if (mbTiles != null) {
            viewModel.updateState { state -> state.copy(tileProviderLatLng = latLng, tileProviderMbTiles = mbTiles) }
        }
    }

    // Route Selection Overlay
    uiState.selectedRoute?.let { route ->
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 60.dp, start = 10.dp, end = 10.dp), contentAlignment = Alignment.BottomCenter) {
            GmsRouteSelection(
                route = route,
                selectedRoute = uiState.selectedRoute,
                highlightedRoute = uiState.highlightedRoute,
                selectRoute = { pair ->
                    CompassViewModel.setRouteThumbnail(null)
                    viewModel.updateState { state -> state.copy(selectedRoute = pair.selectedRoute, routeData = pair.selectedRoute, highlightedRoute = pair.highlightedRoute) }
                },
                simulationLatLngList = uiState.simulationLatLngList,
                gradientRouteData = { data -> viewModel.updateState { state -> state.copy(gradientRouteData = data) } },
                elevationRouteData = { data -> viewModel.updateState { state -> state.copy(elevationRouteData = data) } },
                showRouteSavingScreen = { _ -> viewModel.setShowRouteSavingScreen(true) },
                simulation = { lll ->
                    viewModel.updateState { state -> state.copy(simulationLatLngList = if (state.simulationLatLngList == null) lll else null) }
                }
            )
        }
    }

    // Zoom Buttons
    GmsZoomButtons(uiState.locationEnabled, uiState.zoom) { newZoom ->
        viewModel.updateState { state -> state.copy(zoom = newZoom) }
        cameraPositionState.move(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition(
                    cameraPositionState.position.target,
                    newZoom,
                    cameraPositionState.position.tilt,
                    if (uiState.northUp) 0F else cameraPositionState.position.bearing
                )
            )
        )
    }

    // Snack Bar
    AnimatedVisibility(visible = uiState.snackMsg != null, modifier = Modifier.fillMaxSize(), enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) {
        Box(contentAlignment = Alignment.Center) {
            OutlinedCard(modifier = Modifier.offsetYByPercent(0.4f).padding(horizontal = 10.dp)) {
                uiState.snackMsg?.let {
                    Text(text = it, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    // Center Crosshair Button
    AnimatedVisibility(visible = !uiState.locationEnabled, modifier = Modifier.fillMaxSize(), enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) {
        Box(contentAlignment = Alignment.Center) {
            OutlinedButton(onClick = { viewModel.updateState { state -> state.copy(showHairCrossDropDownMenu = cameraPositionState.position.target) } }) {
                Text("+", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

@Composable
private fun MapDialogs(
    uiState: GmsMapUiState,
    viewModel: GmsMapViewModel,
    cameraPositionState: CameraPositionState,
    onUiSettingsChange: (MapUiSettings) -> Unit,
    onRouteGeojsonFileChange: (File?) -> Unit
) {
    val context = LocalContext.current

    if (uiState.showLocationStatistic) {
        val lllhMarkers = arrayListOf<LatLngH>()
        if (uiState.routeData?.routeMarkerDataList.isNullOrEmpty() && uiState.stopMarkerData != null) {
            val altitude = HgtReader(context, null).getElevationFromHgt(uiState.stopMarkerData.latLng)
            lllhMarkers.add(LatLngH(uiState.stopMarkerData.latLng.latitude, uiState.stopMarkerData.latLng.longitude, altitude = altitude))
        }
        uiState.routeData?.routeMarkerDataList?.forEach { lllhMarkers.add(it.latLngH) }
        MoboLocationsMonitor(lllhMarkers, null) { viewModel.updateState { state -> state.copy(showLocationStatistic = false) } }
    }

    uiState.showPoiInfo?.let { place ->
        viewModel.updateState { state -> state.copy(locationEnabled = false, selectedCircle = null, animatedLatLng = place.latLng) }
        onUiSettingsChange(MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = true))
        GmsPoiInfo(place)
    }

    val routeSaveResultText = stringResource(R.string.route_save_result)
    val okText = stringResource(R.string.ok)
    val errorText = stringResource(R.string.error)
    val ghRouteCalcText = stringResource(R.string.graphhopper_route_calculation)
    val gmsRouteCalcText = stringResource(R.string.gms_route_calculation)
    val gmsErrorText = stringResource(R.string.gms_error)
    val noStopMarkerText = stringResource(R.string.no_stop_marker)
    val stopMarkerText = stringResource(R.string.stop_marker)
    val stopMarkerSetText = stringResource(R.string.stop_marker_set_)

    if (uiState.showRouteSavingScreen) {
        uiState.selectedRoute?.let { route ->
            RouteFileSaveMoBoSheet(route.name) { name, folder ->
                viewModel.updateState { state -> state.copy(showRouteSavingScreen = false) }
                if (folder != null) {
                    val file = File(File(context.filesDir, Const.ROUTEFOLDER), folder.first + "/" + name.replace(Regex(Const.JPG_EXT + "|" + Const.GPX_EXT + "|" + Const.KML_EXT), "") + Const.KML_EXT)
                    val success = Helpers.writeLllh2KmlFile(route.lllh, file.path)
                    val msg = "${file.name} $routeSaveResultText: ${if (success) okText else errorText}"
                    viewModel.setSnackMsg(msg)
                }
            }
        }
    }

    uiState.selectedCircle?.let { GmsCircleInfo(it) { viewModel.updateState { state -> state.copy(showLocationStatistic = it) } } }

    uiState.showHairCrossDropDownMenu?.let { pos ->
        GmsHairCrossMenu(context, northUp = { _ -> }, routingVehicle = { viewModel.updateState { state -> state.copy(showVehicleMenu = true, showHairCrossDropDownMenu = null) } }, ghFolder = { viewModel.updateState { state -> state.copy(showGhFolders = true, showHairCrossDropDownMenu = null) } }) { action ->
            when (action) {
                GmsHairCrossMenuAction.SetStopMarker -> {
                    viewModel.updateState { state -> state.copy(stopMarkerData = PoiMarkerData(pos, stopMarkerText, stopMarkerText), showHairCrossDropDownMenu = null) }
                    CompassViewModel.setDestination(pos, null)
                }
                GmsHairCrossMenuAction.CalculateRoute -> {
                    if (uiState.stopMarkerData != null) {
                        viewModel.updateState { state -> state.copy(snackMsg = "$ghRouteCalcText ${GhHelper.getGhFilename(context)}") }
                        ghCalc(context, pos.latitude, pos.longitude, uiState.stopMarkerData.latLng.latitude, uiState.stopMarkerData.latLng.longitude) { lllh, name, _, _ ->
                            viewModel.updateState { state -> state.copy(routeData = RouteData(lllh, name, lllh.getDistanceFromLllh(), false, null), showHairCrossDropDownMenu = null, snackMsg = null) }
                        }
                    } else viewModel.updateState { state -> state.copy(snackMsg = noStopMarkerText) }
                }
                GmsHairCrossMenuAction.CalculateGmsOnlineRoute, GmsHairCrossMenuAction.CalculateGmsRoundTrip -> {
                    if (uiState.stopMarkerData != null) {
                        viewModel.updateState { state -> state.copy(snackMsg = gmsRouteCalcText) }
                        gmsOnlineCalc(context, pos.latitude, pos.longitude, uiState.stopMarkerData.latLng.latitude, uiState.stopMarkerData.latLng.longitude, action == GmsHairCrossMenuAction.CalculateGmsRoundTrip) { lllh, name, success ->
                            if (success) viewModel.updateState { state -> state.copy(routeData = RouteData(lllh, name, lllh.getDistanceFromLllh(), false, null), showHairCrossDropDownMenu = null, snackMsg = null) }
                            else viewModel.updateState { state -> state.copy(snackMsg = gmsErrorText) }
                        }
                    } else viewModel.updateState { state -> state.copy(snackMsg = noStopMarkerText) }
                }
                GmsHairCrossMenuAction.Nothing -> viewModel.updateState { state -> state.copy(showHairCrossDropDownMenu = null) }
            }
        }
    }

    if (uiState.showGhFolders) ListGhScreen { viewModel.updateState { state -> state.copy(showGhFolders = false) } }
    if (uiState.showVehicleMenu) VehicleMenu(context) { viewModel.updateState { state -> state.copy(showVehicleMenu = false) } }
    if (uiState.showDropDownRasterMaptype) MaptypeMenu(context) { maptype ->
        viewModel.updateState { state -> state.copy(showDropDownRasterMaptype = false) }
        if (maptype != null) {
            uiState.userLocation?.let { latLng ->
                val tile = GeoJsonUtils.pointToTile(latLng.longitude, latLng.latitude, 10.0)
                viewModel.updateState { state -> state.copy(tileProviderMbTiles = Pair("tile_${tile.x}_${tile.y}_${tile.z}_$maptype", maptype), coordinatesOverlayVisibility = false) }
            }
        }
    }

    uiState.gradientRouteData?.let { route ->
        MbsGradientChart(route.getRouteEntity(context), moveMap = { latLng -> latLng?.let { cameraPositionState.position = CameraPosition.fromLatLngZoom(it, cameraPositionState.position.zoom) } }, result = { latLng ->
            if (latLng == null) viewModel.updateState { state -> state.copy(gradientRouteData = null) }
            else { cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, cameraPositionState.position.zoom); viewModel.updateState { state -> state.copy(tempMarkerLatLng = latLng) } }
        })
    }

    uiState.elevationRouteData?.let { route ->
        MbsElevationChart(routeEntity = route.getRouteEntity(context), onSelectLocation = { latLng -> cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, cameraPositionState.position.zoom); viewModel.updateState { state -> state.copy(tempMarkerLatLng = latLng) } }, onClose = { viewModel.updateState { state -> state.copy(elevationRouteData = null) } })
    }

    if (uiState.showDropDownMenu) {
        DropdownMapMenu(
            mapProperties = uiState.mapProperties,
            tileOverlayVisibility = uiState.tileOverlayVisibility,
            coordinatesOverlayVisibility = uiState.coordinatesOverlayVisibility,
            tileOverlayVisibilityChanged = { visible -> viewModel.updateState { state -> state.copy(tileOverlayVisibility = visible) } },
            coordinatesOverlayVisibilityChanged = { visible -> viewModel.updateState { state -> state.copy(coordinatesOverlayVisibility = visible) } },
            trafficChanged = { enabled -> viewModel.updateState { state -> state.copy(mapProperties = state.mapProperties.copy(isTrafficEnabled = enabled)) } },
            gmsMapTypeChanged = { isSatellite -> viewModel.updateState { state -> state.copy(mapProperties = state.mapProperties.copy(mapType = if (isSatellite) MapType.SATELLITE else MapType.TERRAIN)) } },
            finished = { action ->
                viewModel.updateState { state -> state.copy(showDropDownMenu = false) }
                when (action) {
                    ShowRegionList -> viewModel.updateState { state -> state.copy(showRouteRegionList = true) }
                    ShowPoiDatabase -> viewModel.updateState { state -> state.copy(showPoiDatabase = true) }
                    ShowRouteMgr -> viewModel.updateState { state -> state.copy(showRouteMgr = true) }
                    ShowRasterMaptypePref -> viewModel.updateState { state -> state.copy(showDropDownRasterMaptype = true) }
                    ShowRasterMapsList -> viewModel.updateState { state -> state.copy(showListMbTiles = true) }
                    else -> {}
                }
            }
        )
    }

    if (uiState.showListMbTiles) ListMbTilesScreen(PaddingValues()) { viewModel.updateState { state -> state.copy(showListMbTiles = false) } }

    if (uiState.showPoiDatabase) {
        PoiDatabaseScreen(0f, LatLng(cameraPositionState.position.target.latitude, cameraPositionState.position.target.longitude)) { poi, action ->
            if (poi != null) {
                when (action) {
                    PoiItemAction.Map -> {
                        val pos = LatLng(poi.latitude, poi.longitude)
                        viewModel.updateState { state -> state.copy(locationEnabled = false, selectedCircle = null, showPoiDatabase = false, poiMarkerData = PoiMarkerData(pos, poi.name, poi.category), tempMarkerLatLng = pos) }
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(pos, cameraPositionState.position.zoom)
                    }
                    PoiItemAction.Stop -> {
                        val pos = LatLng(poi.latitude, poi.longitude)
                        viewModel.updateState { state -> state.copy(stopMarkerData = PoiMarkerData(pos, poi.name, stopMarkerText), showPoiDatabase = false, snackMsg = String.format(stopMarkerSetText, poi.name)) }
                        CompassViewModel.setDestination(pos, poi.altitude.toInt())
                    }
                    else -> viewModel.updateState { state -> state.copy(showPoiDatabase = false) }
                }
            } else viewModel.updateState { state -> state.copy(showPoiDatabase = false) }
        }
    }

    if (uiState.showRouteMgr) {
        RoutesMgr(cameraPositionState.position.target, selectRouteEntity = { entity ->
            if (entity != null) {
                val data = Helpers.kmlString2RouteData(entity.kmlString)
                data?.let { cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(it.lllh[0].latitude, it.lllh[0].longitude), cameraPositionState.position.zoom); viewModel.updateState { state -> state.copy(routeData = data) } }
            }
            viewModel.updateState { state -> state.copy(showRouteMgr = false) }
        }, selectRouteGeojson = { entity ->
            val data = entity?.getRouteData(context)
            viewModel.updateState { state -> state.copy(selectedRoute = data, highlightedRoute = data, locationEnabled = false, selectedCircle = null, showRouteMgr = false) }
            data?.lllh?.firstOrNull()?.let { cameraPositionState.position = CameraPosition.fromLatLngZoom(it.latLngGms, cameraPositionState.position.zoom) }
        })
    }

    if (uiState.showRouteRegionList) {
        RouteGeojsonList(context) { file ->
            viewModel.updateState { state -> state.copy(showRouteRegionList = false) }
            onRouteGeojsonFileChange(file)
        }
    }
}

fun getMbTileName(context: Context, latLng: LatLng): Pair<String, String> {
    val preferences = getDefaultSharedPreferences(context)
    val mapType = preferences.getString(context.getString(R.string.pref_tilemaker_maptype), Const.OUTDOOR)
    val tile10 = GeoJsonUtils.pointToTile(latLng.longitude, latLng.latitude, 10.0)
    return Pair("tile_${tile10.x}_${tile10.y}_${tile10.z}_$mapType", mapType.toString())
}

@Composable
private fun ProcessGeojsonRoutes(zoom: Float, routeEntities: List<RouteEntity>, highlightedRoute: RouteData?, selected: (RouteData) -> Unit) {
    routeEntities.forEach { routeEntity ->
        val routeData = Helpers.kmlString2RouteData(routeEntity.kmlString)
        ProcessRouteData(routeData, if (highlightedRoute?.name == routeEntity.name) Color.Magenta else Color.Green, highlightedRoute?.name == routeEntity.name, zoom, if (highlightedRoute?.name == routeEntity.name) 2f else 1f, if (highlightedRoute?.name == routeEntity.name) 9f else 6f, selected = { routeData?.let { selected(it) } })
    }
}

@Composable
private fun ProcessRouteData(routeData: RouteData?, color: Color, withMarker: Boolean, zoom: Float, zIndex: Float, width: Float, selected: () -> Unit) {
    val context = LocalContext.current
    val routePoints = routeData?.lllh?.map { LatLng(it.latitude, it.longitude) } ?: emptyList()
    if (routePoints.isNotEmpty()) {
        Polyline(points = routePoints, color = color, width = width, pattern = listOf(Dash(20f), Gap(20f), Dash(20f)), clickable = true, zIndex = zIndex, onClick = { selected() })
        if (withMarker) {
            Marker(state = remember { MarkerState(position = routePoints[0]) }, title = routeData?.name, snippet = routeData?.distance?.formatDistM(true))
            val maxH = routeData?.routeMarkerDataList?.maxOfOrNull { it.latLngH.altitude } ?: 0.0
            routeData?.routeMarkerDataList?.forEach { marker ->
                AdvancedMarker(state = remember { MarkerState(position = LatLng(marker.latLngH.latitude, marker.latLngH.longitude)) }, collisionBehavior = 1, icon = marker.icon, visible = zoom > 12, title = if (maxH > 10) String.format(Locale.ENGLISH, "%.0fkm: %.1f%% %s%.1fm", marker.distanceKm, marker.gradient, Const.UC_ELE_ARROW, marker.latLngH.altitude) else String.format(Locale.ENGLISH, "%s%.1fkm", Const.UC_DISTANCE_ARROW, marker.distanceKm))
            }
        }
    }
}

private fun processCircleData(circleData: ArrayList<CircleInfo>, latLng: LatLng, userSpeed: Float?, userAltitude: Double?): CircleInfo? {
    if (circleData.isNotEmpty()) {
        if (SphericalUtil.computeDistanceBetween(circleData.last().center, latLng) < 50) return null
    }
    var dist = 0.0
    circleData.forEachIndexed { i, circle -> if (i > 0) dist += SphericalUtil.computeDistanceBetween(circle.center, circleData[i - 1].center) }
    val deltaTime = if (circleData.isNotEmpty()) 0.001 * (System.currentTimeMillis() - circleData[0].time) else 0.0
    val dur = deltaTime.toLong().toDuration(DurationUnit.SECONDS)
    val durString = dur.toComponents { hours, minutes, seconds, _ -> String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds) }
    return CircleInfo(dist.formatDistM(true), latLng, durString, System.currentTimeMillis(), userAltitude, userSpeed, if (deltaTime > 0) (3.6 * dist / deltaTime) else 0.0)
}

enum class MenuAction {
    ShowRegionList,
    ShowRouteMgr,
    ShowPoiDatabase,
    ShowRasterMapsList,
    ShowRasterMaptypePref,
    Nothing
}

data class PoiMarkerData(
    var latLng: LatLng,
    val name: String,
    val description: String
)

@Preview(showBackground = true)
@Composable
fun GmsContentPreview() {
    RamaniTheme {
        Text("GmsContent Preview - Requires ViewModel and Context")
    }
}
