package com.almica.ramani

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.MainSnackbarSelection.*
import com.almica.ramani.charts.MonitorGraphLocations
import com.almica.ramani.charts.MbsElevationChart
import com.almica.ramani.charts.MbsGradientChart
import com.almica.ramani.compass.CompassViewModel
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.locationupdates.LocationUpdatesScreen
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.utils.*
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.googlemaps.NewMapAction
import com.almica.ramani_lib.CameraMotionType
import com.almica.ramani_lib.CameraPosition
import com.almica.ramani_lib.LocationRequestProperties
import com.almica.ramani_lib.rememberMapViewWithLifecycle
import me.ibrahimsn.library.LiveSharedPreferences
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import timber.log.Timber
import java.io.File
import androidx.compose.runtime.collectAsState

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "LocalContextGetResourceValueCall", "MissingPermission")
@Composable
fun MainContent(
    localStyleUri: String?,
    startLatLng: LatLng,
    reComposition: (Boolean, String?, LatLng?) -> Unit,
    mapViewReady: (MapView) -> Unit
) {
    val viewModel: MainViewModel? = if (LocalInspectionMode.current) null else viewModel()
    val uiState by viewModel?.uiState?.collectAsState() ?: remember { mutableStateOf(MainUiState()) }
    val context = LocalContext.current
    val resources = LocalResources.current
    val preferences = remember { getDefaultSharedPreferences(context) }
    val liveSharedPreferences = viewModel?.liveSharedPreferences ?: remember { LiveSharedPreferences(preferences) }

    ClipboardManagerEffect(viewModel)

    LaunchedEffect(Unit) {
        viewModel?.setMvtPath(preferences.getString(Const.PREF_MVT_FILEPATH, null))
        viewModel?.setCameraPosition(CameraPosition(target = startLatLng, zoom = 13.0))
    }

    Scaffold { innerPadding ->
        RamaniTheme {
            CompositionLocalProvider(LocalLiveSharedPreferences provides liveSharedPreferences) {
                Box(modifier = Modifier.padding(innerPadding)) {
                    MainScaffoldContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        localStyleUri = localStyleUri,
                        startLatLng = startLatLng,
                        reComposition = reComposition,
                        mapViewReady = mapViewReady,
                        preferences = preferences,
                        resources = resources
                    )
                }
            }
        }
    }
}

@Composable
fun ClipboardManagerEffect(viewModel: MainViewModel?) {
    val uiState by if (viewModel != null) viewModel.uiState.collectAsState() else remember { mutableStateOf(MainUiState()) }
    val clipboardManager = LocalClipboard.current
    
    LaunchedEffect(uiState.clipText) {
        uiState.clipText?.let { text ->
            if (text.isNotEmpty()) {
                Timber.i("clipText: $text")
                val clipData = ClipData.newPlainText(NewMapAction.Import.name, text)
                clipboardManager.setClipEntry(ClipEntry(clipData))
                viewModel?.setClipText(null)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun MainScaffoldContent(
    viewModel: MainViewModel?,
    uiState: MainUiState,
    localStyleUri: String?,
    startLatLng: LatLng,
    reComposition: (Boolean, String?, LatLng?) -> Unit,
    mapViewReady: (MapView) -> Unit,
    preferences: android.content.SharedPreferences,
    resources: android.content.res.Resources
) {
    val context = LocalContext.current
    val cameraPosition = rememberSaveable { mutableStateOf(CameraPosition(target = startLatLng, zoom = 13.0)) }
    val userLocation = rememberSaveable { mutableStateOf(Location(null)) }
    val cameraMode = remember { mutableIntStateOf(uiState.cameraMode) }
    val renderMode = remember { mutableIntStateOf(uiState.renderMode) }
    
    val isTrackingEnabled by viewModel?.isTrackingEnabled?.collectAsState(initial = true) ?: remember { mutableStateOf(true) }
    
    var renderModeMap by remember {
        mutableStateOf(preferences.getString(resources.getString(R.string.pref_render_mode), Const.RENDER_MODE_COMPASS))
    }

    val startTime = remember { System.currentTimeMillis() }
    val localStyleBuilder = remember(localStyleUri) {
        localStyleUri?.let { Style.Builder().fromUri(it) }
    }
    
    val styleUrlMaptypeRaster = rememberSaveable {
        val rootFolder = context.filesDir
        val mvtFolder = File(rootFolder, Const.MVT_FOLDER)
        val planetStyleFile = File(mvtFolder, Const.PLANET_STYLE_FILENAME)
        mutableStateOf(Uri.fromFile(planetStyleFile).toString())
    }
    //Timber.i("styleUrlMaptypeRaster: ${styleUrlMaptypeRaster.value}")
    val styleBuilderMaptypeRaster = remember(styleUrlMaptypeRaster.value) { Style.Builder().fromUri(styleUrlMaptypeRaster.value) }
    val mapView = rememberMapViewWithLifecycle()
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    
    var mapPositionLatitude by remember { mutableDoubleStateOf(0.0) }
    var mapPositionLongitude by remember { mutableDoubleStateOf(0.0) }
    val mapPositionZoom = remember { mutableStateOf<Double?>(null) }
    val locationCircles = remember { mutableStateListOf<LatLng>() }
    val locationProperties = remember { LocationRequestProperties() }

    BackHandler {
        viewModel?.onBackPress(reComposition) { (context as Activity).finish() }
    }

    LaunchedEffect(renderModeMap) {
        Timber.i("renderModeMap: $renderModeMap")
        when (renderModeMap) {
            Const.RENDER_MODE_COMPASS -> { renderMode.intValue = RenderMode.COMPASS; cameraMode.intValue = CameraMode.TRACKING_GPS_NORTH }
            Const.RENDER_MODE_TRACKING -> { renderMode.intValue = RenderMode.COMPASS; cameraMode.intValue = CameraMode.TRACKING_GPS }
            Const.RENDER_MODE_FREE -> { renderMode.intValue = RenderMode.NORMAL; cameraMode.intValue = CameraMode.NONE }
        }
    }

    MainPopupMessage(
        message = uiState.popupSnackMsg,
        onDismiss = { viewModel?.setPopupSnackMsg(null) }
    )

    val imageList = remember { GeoJsonUtils.createSymbolImageList() }

    Box(modifier = Modifier.fillMaxSize()) {
        MainMapView(
            uiState = uiState,
            map = map,
            onMapChange = { map = it },
            mapView = mapView,
            cameraPosition = cameraPosition,
            userLocation = userLocation,
            cameraMode = cameraMode,
            renderMode = renderMode,
            localStyleBuilder = localStyleBuilder,
            styleBuilderMaptypeRaster = styleBuilderMaptypeRaster,
            imageList = imageList,
            locationProperties = locationProperties,
            locationCircles = locationCircles,
            poiEntities = uiState.poiEntities,
            poiCategoryMap = uiState.poiCategoryMap,
            onMapClick = { viewModel?.onMapClick(context, it, map, cameraPosition.value) },
            onMapLongClick = { if (uiState.dimmerState) viewModel?.setDimmer(false) },
            onStyleLoaded = { style ->
                //val prefMaptypeKey = preferences.getInt(Const.PREF_MAPTYPE_KEY, 0)
                //Timber.i("prefMaptypeKey (0): $prefMaptypeKey")
                Timber.i("prefMaptypeKey (1): ${uiState.prefMaptypeKey}") // returns always 0 ??? AI has solution 23jul2026
                if (uiState.prefMaptypeKey == MaptypeKey.Raster.ordinal) {
                //if (prefMaptypeKey == MaptypeKey.Raster.ordinal) {
                    initMapComponentsMaptypeRaster(context, style, uiState.useCyclewayOverlays, uiState.prefMaptypeKey) {
                        viewModel?.setProgress(null)
                    }
                } else {
                    initMapComponentsLocalStyle(context, style, uiState.useCyclewayOverlays, uiState.mvtBounds, uiState.mvtPath) { progMsg ->
                        viewModel?.setProgress(progMsg)
                    }
                }
                uiState.mvtPath?.let {
                    if (File(it).name == Const.COUNTRIES_MVT_FILENAME || File(it).name == Const.PLANET_MVT_FILENAME) {
                        setPlanetVisibility(context, true, style)
                    }
                }
            },
            onMapMove = { latLng, zoom ->
                mapPositionLatitude = latLng.latitude
                mapPositionLongitude = latLng.longitude
                mapPositionZoom.value = zoom
                // We don't re-assign cameraPosition.value here to avoid a feedback loop
                // that cancels ongoing camera animations (like zooming).
                // The properties of the existing cameraPosition.value object are 
                // already being updated by MapLibre's internal listeners.
            },
            onMapReady = { maplibreMap ->
                viewModel?.setProgress(null)
                mapViewReady(mapView)
                initScaleBar(mapView, maplibreMap)
                Timber.d("Map Ready with prefMaptypeKey: ${uiState.prefMaptypeKey}")
                viewModel?.setUseCyclewayOverlays(preferences.getBoolean(Const.PREF_USE_CYCLEWAYS_OVERLAY, false))
                mapPositionZoom.value = cameraPosition.value.zoom!!
            },
            onStopClick = { viewModel?.setSnackbar(MainSnackbarData(resources.getString(R.string.remove_stop_marker), resources.getString(android.R.string.ok), RemoveStop, null)) },
            onStopDragFinished = { viewModel?.handleSetStop(it) },
            onMarkerClick = { _, pmd, distKm ->
                viewModel?.setSnackbar(MainSnackbarData("${pmd.distanceKm.format(0)}km / ${distKm.format(0)}km", "${pmd.gradient.format(1)}% ${Const.UC_MENU}", RouteSideBar, null))
                cameraPosition.value = CameraPosition(cameraPosition.value).apply { target = LatLng(pmd.latLngH.latitude, pmd.latLngH.longitude) }
            },
            onMarkerLongClick = {
                viewModel?.updatePolygon(PolygonState(arrayListOf(), "", 0.0))
                viewModel?.setHighlightRoutePoint(-1)
                CompassViewModel.setRouteThumbnail(null)
                viewModel?.setRecalcRequired(false)
                CompassViewModel.setDestination(uiState.stopPosition?.let { com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude) }, uiState.stopPosition?.altitude?.toInt())
                viewModel?.setLoadedRoute(null)
            },
            onPoiClick = { poi ->
                viewModel?.setSnackbar(MainSnackbarData("${poi.name} - ${poi.category}", resources.getString(R.string.set_stop), SetStop, LatLng(poi.latitude, poi.longitude)))
                cameraPosition.value = CameraPosition(cameraPosition.value).apply { target = LatLng(poi.latitude, poi.longitude) }
            },
            onLogCountChange = { viewModel?.setLogCount(it) },
            context = context
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(16.dp),
            shape = CircleShape,
            onClick = {
                val msg = if (isTrackingEnabled) "Tracking is active" else "Tracking is disabled"
                viewModel?.setSnackbar(
                    MainSnackbarData(msg, resources.getString(R.string.toggle),
                        ToggleTracking, isTrackingEnabled)
                )
            },
            color = if (isTrackingEnabled) Color.Red else Color.White,
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
        ) {}

        if (uiState.mainSnackbarData != null) {
            MainMoboSnack(uiState.mainSnackbarData) { action ->
                handleSnackbarAction(context, action, viewModel, uiState, cameraPosition, preferences, reComposition)
            }
        }

        MapOverlayManager(
            viewModel = viewModel,
            uiState = uiState,
            map = map,
            cameraPosition = cameraPosition,
            cameraMode = cameraMode,
            userLocation = userLocation,
            prefMaptypeKey = uiState.prefMaptypeKey,
            onPopupSnackMsg = { viewModel?.setPopupSnackMsg(it) },
            onRoutesGeoJsonStringChange = { viewModel?.setRoutesGeoJsonString(it) },
            onRenderModeMapChange = { renderModeMap = it },
            onUseCyclewayOverlaysChange = { viewModel?.setUseCyclewayOverlays(it) },
            onToggleButtonsBottomBarChange = { viewModel?.setToggleButtonsBottomBar(it) },
            startTime = startTime,
            locationCircles = locationCircles
        )

        LaunchedEffect(userLocation.value) {
            if (userLocation.value.latitude != 0.0 || userLocation.value.longitude != 0.0) {
                val newLatLng = LatLng(userLocation.value.latitude, userLocation.value.longitude)
                val lastCircle = locationCircles.lastOrNull()
                //Timber.i("uiState.prefMaptypeKey: ${uiState.prefMaptypeKey}")
                if (lastCircle == null || lastCircle.distanceTo(newLatLng) > 10.0) {
                    locationCircles.add(newLatLng)
                }
            }
        }

        ChartOverlays(uiState, viewModel, cameraPosition, startTime)
        LocationUpdatesScreen()
        if (uiState.activeOverlay != OverlayType.ROUTE_FOLDERS
            && uiState.activeOverlay != OverlayType.POI_DATABASE
            && uiState.activeOverlay != OverlayType.ROUTE_FILES
            && uiState.activeOverlay != OverlayType.ADDITIONAL_MAPS
            && uiState.activeOverlay != OverlayType.PREFERENCES
            && uiState.activeOverlay != OverlayType.SAT_STATUS) {
            MapControlsLayer(
                viewModel = viewModel,
                uiState = uiState,
                map = map,
                cameraPosition = cameraPosition,
                mapPositionLatitude = mapPositionLatitude,
                mapPositionLongitude = mapPositionLongitude,
                mapPositionZoom = mapPositionZoom,
                toggleButtonsBottomBar = uiState.toggleButtonsBottomBar,
                onToggleButtonsBottomBarChange = { viewModel?.setToggleButtonsBottomBar(it) },
                renderModeMap = renderModeMap ?: Const.RENDER_MODE_COMPASS,
                onRenderModeMapChange = { renderModeMap = it },
                onRecalc = {
                    val llStop = if (uiState.stopDragged && uiState.stopPosition != null)
                        com.google.android.gms.maps.model.LatLng(
                            uiState.stopPosition.latitude,
                            uiState.stopPosition.longitude
                        )
                    else uiState.loadedRouteEntity?.let {
                        com.google.android.gms.maps.model.LatLng(
                            it.latitudeStop,
                            it.longitudeStop
                        )
                    }

                    llStop?.let { stop ->
                        viewModel?.calculateRoute(
                            context,
                            userLocation.value.latitude,
                            userLocation.value.longitude,
                            stop.latitude,
                            stop.longitude
                        )
                    }
                },
                onRestart = {
                    reComposition(false, uiState.mvtPath, cameraPosition.value.target)
                }
            )
            HairCrossOverlay(
                visible = renderModeMap == Const.RENDER_MODE_FREE && cameraMode.intValue == CameraMode.NONE,
                hairCrossOffsetFraction = uiState.hairCrossOffsetFraction,
                onClick = {
                    Timber.i("HairCrossOverlay onClick()")
                    viewModel?.onHairCrossClicked()
                }
            )
        }
    }
}

@Composable
fun ChartOverlays(uiState: MainUiState, viewModel: MainViewModel?, cameraPosition: MutableState<CameraPosition>, startTime: Long) {
    if (uiState.activeOverlay == OverlayType.LOCATION_STATISTIC) {
        MonitorGraphLocations(
            lllh = uiState.polygonState.lllh,
            _plotResult = null,
            startTime = startTime,
            result = { viewModel?.closeOverlay() },
            map = { latLng ->
                latLng?.let {
                    cameraPosition.value = CameraPosition(cameraPosition.value).apply { target = LatLng(it.latitude, it.longitude) }
                }
            },
            highlightRoutePoint = { viewModel?.setHighlightRoutePoint(it) }
        )
    }

    uiState.gradientRouteEntity?.let { route ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp), contentAlignment = Alignment.BottomCenter) {
            MbsGradientChart(route, moveMap = { latLng ->
                cameraPosition.value = CameraPosition(cameraPosition.value).apply {
                    target = latLng?.let { LatLng(it.latitude, it.longitude) }
                }
            }, result = { viewModel?.setGradientRoute(null) })
        }
    }

    uiState.chartRouteEntity?.let { route ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp), contentAlignment = Alignment.BottomCenter) {
            MbsElevationChart(routeEntity = route, onSelectLocation = { gmsLatLng ->
                cameraPosition.value = CameraPosition(cameraPosition.value).apply {
                    target = LatLng(gmsLatLng.latitude, gmsLatLng.longitude)
                }
            }, onClose = { viewModel?.setChartRoute(null) })
        }
    }
}

private fun handleSnackbarAction(
    context: Context,
    action: MainSnackbarSelection,
    viewModel: MainViewModel?,
    uiState: MainUiState,
    cameraPosition: MutableState<CameraPosition>,
    preferences: android.content.SharedPreferences,
    reComposition: (Boolean, String?, LatLng?) -> Unit
) {
    when (action) {
        RouteCalculation -> {
            val target = cameraPosition.value.target
            if (target != null && uiState.stopPosition != null) {
                viewModel?.calculateRoute(context, target.latitude, target.longitude, uiState.stopPosition.latitude, uiState.stopPosition.longitude)
                viewModel?.setSnackbar(null)
            } else viewModel?.setSnackbar(MainSnackbarData(context.getString(R.string.no_stop_marker), null, null, null))
        }
        SetStop -> {
            val newStop = (uiState.mainSnackbarData?.data as? LatLng) ?: cameraPosition.value.target
            newStop?.let { viewModel?.handleSetStop(it) }
        }
        SaveRoute -> { viewModel?.setOverlay(OverlayType.ROUTE_SAVING); viewModel?.setSnackbar(null) }
        MapManager -> {
            uiState.mapManagerPosition?.let {
                cameraPosition.value = CameraPosition(cameraPosition.value).apply {
                    target = LatLng(it.latitude, it.longitude)
                    bearing = 0.0
                    motionType = CameraMotionType.INSTANT
                    animationDurationMs = 0
                }
            } ?: viewModel?.setMapManagerPosition(cameraPosition.value.target)
            viewModel?.setSnackbar(null)
            viewModel?.setOverlay(OverlayType.ADDITIONAL_MAPS)
        }
        RemoveStop -> viewModel?.handleRemoveStop()
        Drive -> {
            viewModel?.setClipText(uiState.mainSnackbarData?.data.toString())
            context.startActivity(Intent(context, FileImportActivity::class.java).setAction(context.getString(R.string.import_title)).putExtra(Const.EXTRA_FILETYPE, FileType.Mvt.name))
            viewModel?.setSnackbar(null)
        }
        SelectMvt -> viewModel?.handleSelectMvt(preferences, uiState.mainSnackbarData?.data as? String)
        ChangeMvt -> { viewModel?.setSnackbar(null); viewModel?.setOverlay(OverlayType.MVT_LIST) }
        Bbbike -> {
            (uiState.mainSnackbarData?.data as? Uri)?.let { context.startActivity(Intent(Intent.ACTION_VIEW, it)) }
            viewModel?.setSnackbar(null)
        }
        RouteSideBar -> { viewModel?.setOverlay(OverlayType.ROUTE_MONITOR); viewModel?.setSnackbar(null) }
        Nothing -> viewModel?.setSnackbar(null)
        AppRestart -> {
            reComposition(true, uiState.mvtPath, cameraPosition.value.target)
        }

        ToggleTracking -> {
            val isTrackingEnabled = uiState.mainSnackbarData?.data as? Boolean
            isTrackingEnabled?.let { viewModel?.setIsTrackingEnabled(!it) }
            viewModel?.setSnackbar(null)
        }
    }
}

enum class MaptypeKey { None, GeoJson, Raster, Mvt }

@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    RamaniTheme {
        MainContent(localStyleUri = null, startLatLng = LatLng(52.0, 10.0), reComposition = { _, _, _ -> }) { _ -> }
    }
}
