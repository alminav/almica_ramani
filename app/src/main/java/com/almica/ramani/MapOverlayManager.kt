package com.almica.ramani

import android.content.Intent
import android.location.Location
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.almica.ramani.FeatureProperties.Companion.LINES_TAG
import com.almica.ramani.LayersControlAction.GeojsonBbbike12
import com.almica.ramani.LayersControlAction.GeojsonBbbike13
import com.almica.ramani.LayersControlAction.MvtBbbike09
import com.almica.ramani.LayersControlAction.PmtilesBbbike11
import com.almica.ramani.LayersControlAction.ToggleRoutesGeojson
import com.almica.ramani.MainSnackbarSelection.Bbbike
import com.almica.ramani.MainSnackbarSelection.Drive
import com.almica.ramani.MainSnackbarSelection.RouteCalculation
import com.almica.ramani.MainSnackbarSelection.SelectMvt
import com.almica.ramani.MainSnackbarSelection.SetStop
import com.almica.ramani.MapLongClickAction.ChangeDimmerState
import com.almica.ramani.MapLongClickAction.ClearGpsCircles
import com.almica.ramani.MapLongClickAction.SatStatus
import com.almica.ramani.OverlayType.ADDITIONAL_MAPS
import com.almica.ramani.OverlayType.BBBIKE_FUNCTIONS
import com.almica.ramani.OverlayType.GEO_CODER
import com.almica.ramani.OverlayType.GH_FOLDERS
import com.almica.ramani.OverlayType.HAIRCROSS
import com.almica.ramani.OverlayType.LAYERS_CONTROL
import com.almica.ramani.OverlayType.LOCATIONS
import com.almica.ramani.OverlayType.LOCATION_STATISTIC
import com.almica.ramani.OverlayType.MAP_LONG_CLICK
import com.almica.ramani.OverlayType.MAP_MENU
import com.almica.ramani.OverlayType.MAP_TYPE
import com.almica.ramani.OverlayType.MVT_LIST
import com.almica.ramani.OverlayType.PDF_ROUTES
import com.almica.ramani.OverlayType.PDF_VIEWER
import com.almica.ramani.OverlayType.POI_DATABASE
import com.almica.ramani.OverlayType.PREFERENCES
import com.almica.ramani.OverlayType.RASTER_MAPS
import com.almica.ramani.OverlayType.ROUTE_FILES
import com.almica.ramani.OverlayType.ROUTE_FILES_REGION
import com.almica.ramani.OverlayType.ROUTE_FOLDERS
import com.almica.ramani.OverlayType.ROUTE_MONITOR
import com.almica.ramani.OverlayType.ROUTE_SAVING
import com.almica.ramani.OverlayType.SAT_STATUS
import com.almica.ramani.OverlayType.VEHICLE_MENU
import com.almica.ramani.compass.CompassViewModel
import com.almica.ramani.geojsonMaps.AdditionalMapsManager
import com.almica.ramani.geojsonMaps.GeojsonMapRepository
import com.almica.ramani.googlemaps.MaptypeMenu
import com.almica.ramani.navigation.RamaniApp
import com.almica.ramani.pois.PoiDatabaseScreen
import com.almica.ramani.pois.PoiItemAction
import com.almica.ramani.pois.PoiRepository
import com.almica.ramani.routes.RouteDialogMode
import com.almica.ramani.routes.RouteEntity
import com.almica.ramani.routes.RouteFileSaveMoBoSheet
import com.almica.ramani.routes.RouteMenu
import com.almica.ramani.routes.RoutesManager
import com.almica.ramani.speedometer.SpeedView
import com.almica.ramani.speedometer.components.Section
import com.almica.ramani.utils.DocumentViewer
import com.almica.ramani.utils.DriveSharedLinks
import com.almica.ramani.utils.FeatureItem
import com.almica.ramani.utils.GeoJsonUtils.Companion.createGeojsonMapBoundFeatures
import com.almica.ramani.utils.GeoJsonUtils.Companion.getRouteEntityFromGeojsonByName
import com.almica.ramani.utils.GeoJsonUtils.Companion.pointToTile
import com.almica.ramani.utils.GhHelper
import com.almica.ramani.utils.addPoiDao
import com.almica.ramani.utils.changeLayerVisibility
import com.almica.ramani.utils.formatDistM
import com.almica.ramani.utils.getCenter
import com.almica.ramani.utils.getDistanceFromLllh
import com.almica.ramani.utils.getLayer
import com.almica.ramani.utils.getVisibleMapFeatures
import com.almica.ramani.utils.ghCalc
import com.almica.ramani.utils.initMapsGridRaster
import com.almica.ramani.utils.isNetworkAvailable
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.utils.kmlString2Lllh
import com.almica.ramani.utils.launchOrsRouting
import com.almica.ramani.utils.lllhToKmlString
import com.almica.ramani.utils.removeLayers
import com.almica.ramani.utils.setPlanetVisibility
import com.almica.ramani.utils.toggleLayerVisibility
import com.almica.ramani.utils.zlibDecompress
import com.almica.ramani_lib.CameraPosition
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.ibrahimsn.library.LiveSharedPreferences
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import timber.log.Timber
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.pois.PoiEntity
import com.almica.ramani.ui.theme.RamaniTheme
import com.google.android.gms.maps.model.LatLng as GmsLatLng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.MapOverlayManager(
    viewModel: MainViewModel?,
    uiState: MainUiState,
    map: MapLibreMap?,
    cameraPosition: MutableState<CameraPosition>,
    cameraMode: MutableState<Int>,
    userLocation: MutableState<Location>,
    prefMaptypeKey: Int,
    liveSharedPreferences: LiveSharedPreferences,
    onPopupSnackMsg: (String?) -> Unit,
    onRoutesGeoJsonStringChange: (String?) -> Unit,
    onRenderModeMapChange: (String) -> Unit,
    onUseCyclewayOverlaysChange: (Boolean) -> Unit,
    onToggleButtonsBottomBarChange: (Boolean) -> Unit,
    startTime: Long,
    locationCircles: MutableList<LatLng>
) {
    MapOverlayManagerContent(
        uiState = uiState,
        map = map,
        cameraPosition = cameraPosition,
        cameraMode = cameraMode,
        userLocation = userLocation,
        prefMaptypeKey = prefMaptypeKey,
        liveSharedPreferences = liveSharedPreferences,
        onPopupSnackMsg = onPopupSnackMsg,
        onRoutesGeoJsonStringChange = onRoutesGeoJsonStringChange,
        onRenderModeMapChange = onRenderModeMapChange,
        onUseCyclewayOverlaysChange = onUseCyclewayOverlaysChange,
        onToggleButtonsBottomBarChange = onToggleButtonsBottomBarChange,
        startTime = startTime,
        locationCircles = locationCircles,
        setToggleGeojsonMapVisibility = { viewModel?.setToggleGeojsonMapVisibility(it) },
        closeOverlay = { viewModel?.closeOverlay() },
        setSelectedFeature = { viewModel?.setSelectedFeature(it) },
        setStopPosition = { viewModel?.setStopPosition(it) },
        setProgress = { viewModel?.setProgress(it) },
        updatePolygon = { viewModel?.updatePolygon(it) },
        setLoadedRoute = { viewModel?.setLoadedRoute(it) },
        setHighlightRoutePoint = { viewModel?.setHighlightRoutePoint(it) },
        setRecalcRequired = { viewModel?.setRecalcRequired(it) },
        setStopDragged = { viewModel?.setStopDragged(it) },
        setSnackbar = { viewModel?.setSnackbar(it) },
        setMapFeatures = { viewModel?.setMapFeatures(it) },
        setOverlay = { viewModel?.setOverlay(it) },
        setLogCount = { viewModel?.setLogCount(it) },
        setMapManagerPosition = { viewModel?.setMapManagerPosition(it) },
        setAppRestartRequired = { viewModel?.setAppRestartRequired(it) },
        setDimmer = { viewModel?.setDimmer(it) },
        setGradientRoute = { viewModel?.setGradientRoute(it) },
        setChartRoute = { viewModel?.setChartRoute(it) },
        addPoiEntity = { viewModel?.addPoiEntity(it) },
        setRouteMonitorState = { viewModel?.setRouteMonitorState(it) },
        setRoutesRegionFilter = { viewModel?.setRoutesRegionFilter(it) },
    ) { viewModel?.setRouteInfo(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.MapOverlayManagerContent(
    uiState: MainUiState,
    map: MapLibreMap?,
    cameraPosition: MutableState<CameraPosition>,
    cameraMode: MutableState<Int>,
    userLocation: MutableState<Location>,
    prefMaptypeKey: Int,
    liveSharedPreferences: LiveSharedPreferences,
    onPopupSnackMsg: (String?) -> Unit,
    onRoutesGeoJsonStringChange: (String?) -> Unit,
    onRenderModeMapChange: (String) -> Unit,
    onUseCyclewayOverlaysChange: (Boolean) -> Unit,
    onToggleButtonsBottomBarChange: (Boolean) -> Unit,
    startTime: Long,
    locationCircles: MutableList<LatLng>,
    setToggleGeojsonMapVisibility: (String?) -> Unit,
    closeOverlay: () -> Unit,
    setSelectedFeature: (FeatureItem?) -> Unit,
    setStopPosition: (LatLng?) -> Unit,
    setProgress: (String?) -> Unit,
    updatePolygon: (PolygonState) -> Unit,
    setLoadedRoute: (RouteEntity?) -> Unit,
    setHighlightRoutePoint: (Int) -> Unit,
    setRecalcRequired: (Boolean) -> Unit,
    setStopDragged: (Boolean) -> Unit,
    setSnackbar: (MainSnackbarData?) -> Unit,
    setMapFeatures: (List<FeatureItem>?) -> Unit,
    setOverlay: (OverlayType) -> Unit,
    setLogCount: (Int) -> Unit,
    setMapManagerPosition: (LatLng?) -> Unit,
    setAppRestartRequired: (Boolean) -> Unit,
    setDimmer: (Boolean) -> Unit,
    setGradientRoute: (RouteEntity?) -> Unit,
    setChartRoute: (RouteEntity?) -> Unit,
    addPoiEntity: (PoiEntity?) -> Unit,
    setRouteMonitorState: (Int) -> Unit,
    setRoutesRegionFilter: (String) -> Unit,
    setRouteInfo: (File?) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val poiRepository = PoiRepository.getInstance(context, Executors.newSingleThreadExecutor())

    val showPreferenceScreen = uiState.activeOverlay == PREFERENCES
    val showGhFolders = uiState.activeOverlay == GH_FOLDERS
    val showVehicleMenu = uiState.activeOverlay == VEHICLE_MENU
    val showGeoCoder = uiState.activeOverlay == GEO_CODER
    val showRouteMonitorMenu = uiState.activeOverlay == ROUTE_MONITOR
    val showMapLongClickMenu = uiState.activeOverlay == MAP_LONG_CLICK
    val showRasterMapsMenu = uiState.activeOverlay == RASTER_MAPS
    val showMaptypeMenu = uiState.activeOverlay == MAP_TYPE
    val showSatStatus = uiState.activeOverlay == SAT_STATUS
    val showPdfViewer = uiState.activeOverlay == PDF_VIEWER
    val showPdfRoutes = uiState.activeOverlay == PDF_ROUTES
    val showLayersControlMenu = uiState.activeOverlay == LAYERS_CONTROL
    val showBbbikeFunctionsMenu = uiState.activeOverlay == BBBIKE_FUNCTIONS
    val showHaircrossMenu = uiState.activeOverlay == HAIRCROSS
    val showMapMenu = uiState.activeOverlay == MAP_MENU
    val showPoiDatabase = uiState.activeOverlay == POI_DATABASE
    val showLocationsMenu = uiState.activeOverlay == LOCATIONS
    val showRouteFiles = uiState.activeOverlay == ROUTE_FILES
    val showRouteFilesRegionList = uiState.activeOverlay == ROUTE_FILES_REGION
    val showRouteFolders = uiState.activeOverlay == ROUTE_FOLDERS
    val showMvtList = uiState.activeOverlay == MVT_LIST
    val showAdditionalMapsManager = uiState.activeOverlay == ADDITIONAL_MAPS
    val showLocationStatistic = uiState.activeOverlay == LOCATION_STATISTIC
    val showRouteSavingScreen = uiState.activeOverlay == ROUTE_SAVING

    val stopPosition = uiState.stopPosition
    val polygonState = uiState.polygonState
    val loadedRouteEntity = uiState.loadedRouteEntity
    val routeMonitorState = uiState.routeMonitorState
    val mapManagerPosition = uiState.mapManagerPosition
    val toggleGeojsonMapVisibility = uiState.toggleGeojsonMapVisibility
    val pdfRoutes = uiState.pdfRoutes
    val mapFeatures = uiState.mapFeatures
    val selectedFeatureItem = uiState.selectedFeatureItem
    val progressMsg = uiState.progressMsg
    val gpsValueState = uiState.gpsValueState
    val highlightRoutePoint = uiState.highlightRoutePoint
    val dimmerState = uiState.dimmerState
    val routesRegionFilter = uiState.routesRegionFilter
    val showRouteInfo = uiState.showRouteInfo

    if (toggleGeojsonMapVisibility != null) {
        GeojsonMapsMenu(context, toggleGeojsonMapVisibility) { action, tileName ->
            setToggleGeojsonMapVisibility(null)
            when (action) {
                GeojsonMapsMenuAction.ToggleGrid -> {
                    map?.let { it1 ->
                        val layerId = resources.getString(R.string.geojson_maps_grid)
                        toggleLayerVisibility(it1, layerId)
                    }
                }
                GeojsonMapsMenuAction.ToggleVisibility -> {
                    tileName?.let { name ->
                        val mapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
                        mapRepository.toggleGeojsonMapStatus(name) {
                            createGeojsonMapBoundFeatures(context, map) { _ -> }
                        }
                    }
                }
                GeojsonMapsMenuAction.Remove -> {
                    val geojsonMapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
                    tileName?.let { name ->
                        geojsonMapRepository.removeGeojsonMapByName(name) {
                            val targetFolder = File(context.filesDir, com.almica.ramani.filepicker.Const.GEOJSON_MAP_FOLDER)
                            val f = File(targetFolder, "${name}${Const.GEOJSON_EXT}")
                            if (!f.delete()) {
                                File(targetFolder, "${name}#${Const.GEOJSON_EXT}").delete()
                            }
                            createGeojsonMapBoundFeatures(context, map) { _ -> }
                        }
                    }
                }
                GeojsonMapsMenuAction.Nothing -> {}
                GeojsonMapsMenuAction.Share -> {
                    tileName?.let { name ->
                        val cacheFile = File(context.cacheDir, "${name}${Const.HASHTAG}${Const.GEOJSON_EXT}")
                        val geojsonMapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
                        val geojsonMapEntity = geojsonMapRepository.getGeojsonMapSimpleByName(name)
                        if (geojsonMapEntity != null) {
                            geojsonMapEntity.data?.zlibDecompress()?.let { cacheFile.writeText(it, Charsets.UTF_8) }
                            val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", cacheFile)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                type = "*/*"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share file to.."))
                        }
                    }
                }
            }
        }
    }

    if (showHaircrossMenu) {
        HairCrossBottomMenu(cameraPosition.value.target, stopPosition) { action ->
            when (action) {
                HairCrossAction.Close -> closeOverlay()
                HairCrossAction.AddPoi -> {
                    setSelectedFeature(cameraPosition.value.target?.let {
                        FeatureItem(Const.UNKNOWN, 0.0, it.latitude, it.longitude, 0.0, 0, "", null, null, false, null, null)
                    })
                    closeOverlay()
                }
                HairCrossAction.AddStop -> {
                    cameraPosition.value.target?.let { latLng ->
                        val newStop = LatLng(latLng.latitude, latLng.longitude)
                        setStopPosition(newStop)
                        CompassViewModel.setDestination(com.google.android.gms.maps.model.LatLng(newStop.latitude, newStop.longitude), newStop.altitude.toInt())
                    }
                    closeOverlay()
                }
                HairCrossAction.Calc -> {
                    val startLat = cameraPosition.value.target?.latitude
                    val startLon = cameraPosition.value.target?.longitude
                    if (startLat != null && startLon != null && stopPosition.isNotNull()) {
                        setProgress("${resources.getString(R.string.graphhopper_route_calculation)} ${GhHelper.getGhFilename(context)}")
                        stopPosition?.let { stop ->
                            ghCalc(context, startLat, startLon, stop.latitude, stop.longitude) { lllh, name, success, _ ->
                                setProgress(null)
                                val dist = lllh.getDistanceFromLllh()
                                val center = lllh.getCenter()
                                val newState = PolygonState(lllh, name, dist).apply {
                                    polygonData = PolygonData(lllh, name, dist, false, null)
                                    polygonData?.createPolygonMarkers(context, 0.0)
                                }
                                updatePolygon(newState)
                                setLoadedRoute(RouteEntity(UUID.randomUUID(), name, Const.GH_TAG, startLat, startLon, latitudeCenter = center.latitude, longitudeCenter = center.longitude, latitudeStop = lllh[lllh.lastIndex].latitude, longitudeStop = lllh[lllh.lastIndex].longitude, kmlString = lllh.lllhToKmlString(name)))
                                setHighlightRoutePoint(-1)
                                CompassViewModel.setRouteThumbnail(null)
                                setRecalcRequired(false)
                                setStopDragged(false)
                            }
                        }
                    } else setSnackbar(MainSnackbarData(resources.getString(R.string.no_stop_marker), null, null, null))
                    closeOverlay()
                }
                HairCrossAction.MapFeatures -> {
                    closeOverlay()
                    if (map != null) {
                        setProgress(resources.getString(R.string.map_features))
                        lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                            setMapFeatures(getVisibleMapFeatures(context, map, com.google.android.gms.maps.model.LatLng(userLocation.value.latitude, userLocation.value.longitude)))
                        }.invokeOnCompletion { setProgress(null) }
                    } else setSnackbar(MainSnackbarData(resources.getString(R.string.no_map_features), null, null, null))
                }
                HairCrossAction.RoutingVehicle -> {
                    closeOverlay()
                    setOverlay(VEHICLE_MENU)
                }
                HairCrossAction.OrsCalc, HairCrossAction.OrsRoundtrip -> {
                    if (context.isNetworkAvailable()) {
                        val startLat = cameraPosition.value.target?.latitude
                        val startLon = cameraPosition.value.target?.longitude
                        if (startLat != null && startLon != null && stopPosition.isNotNull()) {
                            setProgress(resources.getString(R.string.ors_route_calculation))
                            stopPosition?.let { stop ->
                                launchOrsRouting(context, startLat, startLon, stop.latitude, stop.longitude, action == HairCrossAction.OrsRoundtrip) { lllh, name, success ->
                                    setProgress(null)
                                    val dist = lllh.getDistanceFromLllh()
                                    val center = lllh.getCenter()
                                    val newState = PolygonState(lllh, name, dist).apply {
                                        polygonData = PolygonData(lllh, name, dist, false, null)
                                        polygonData?.createPolygonMarkers(context, 0.0)
                                    }
                                    updatePolygon(newState)
                                    setLoadedRoute(RouteEntity(UUID.randomUUID(), name, Const.GH_TAG, startLat, startLon, latitudeCenter = center.latitude, longitudeCenter = center.longitude, latitudeStop = lllh[lllh.lastIndex].latitude, longitudeStop = lllh[lllh.lastIndex].longitude, kmlString = lllh.lllhToKmlString(name)))
                                    setHighlightRoutePoint(-1)
                                    CompassViewModel.setRouteThumbnail(null)
                                    setRecalcRequired(false)
                                }
                            }
                        } else setSnackbar(MainSnackbarData(resources.getString(R.string.no_stop_marker), null, null, null))
                        closeOverlay()
                    } else {
                        closeOverlay()
                        setSnackbar(MainSnackbarData(resources.getString(R.string.no_connection), null, null, null))
                    }
                }
                HairCrossAction.GeoCoder -> {
                    closeOverlay()
                    setOverlay(GEO_CODER)
                }
                HairCrossAction.RemoveStop -> {
                    setStopPosition(null)
                    closeOverlay()
                    CompassViewModel.setDestination(null, null)
                    CompassViewModel.setRouteThumbnail(null)
                }
                HairCrossAction.GhFolder -> {
                    closeOverlay()
                    setOverlay(GH_FOLDERS)
                }
                HairCrossAction.NearestPoi -> {
                    closeOverlay()
                    cameraPosition.value.target?.let { target ->
                        poiRepository.getNearestPoi(target.latitude, target.longitude) { nearest ->
                            setSnackbar(if (nearest != null) {
                                MainSnackbarData("Nearest: ${nearest.name} (${nearest.category})", resources.getString(R.string.set_stop), SetStop, LatLng(nearest.latitude, nearest.longitude))
                            } else MainSnackbarData("No POIs found", null, null, null))
                        }
                    }
                }
            }
        }
    }

    if (showRouteSavingScreen) {
        RouteFileSaveMoBoSheet(polygonState.name.replace(Const.JPG_EXT, "").replace(Const.GPX_EXT, "").replace(Const.KML_EXT, "")) { targetFileName, targetRouteFolder ->
            closeOverlay()
            if (targetRouteFolder != null) {
                val routeFolder = File(File(context.filesDir, Const.ROUTEFOLDER), targetRouteFolder.first)
                val routeFile = File(routeFolder, targetFileName.replace(Const.JPG_EXT, "").replace(Const.GPX_EXT, "").replace(Const.KML_EXT, "") + Const.KML_EXT)
                val result = com.almica.ramani.Helpers.writeLllh2KmlFile(polygonState.lllh, routeFile.path)
                setSnackbar(MainSnackbarData("${routeFile.name} ${resources.getString(R.string.route_save_result)}: " + if (result) resources.getString(R.string.ok) else resources.getString(R.string.error), null, null, null))
            }
        }
    }

    if (showLocationsMenu) {
        LocationsBottomMenu { msg, action ->
            when (action) {
                LocationsAction.Close -> closeOverlay()
                LocationsAction.Save -> {
                    setSnackbar(msg?.let { MainSnackbarData(it, null, null, null) })
                    closeOverlay()
                }
                LocationsAction.Monitor -> setOverlay(LOCATION_STATISTIC)
                LocationsAction.Reset -> {
                    setLogCount(0)
                    setSnackbar(msg?.let { MainSnackbarData(it, null, null, null) })
                    closeOverlay()
                }
                LocationsAction.DeleteTracks -> {
                    setSnackbar(msg?.let { MainSnackbarData(it, null, null, null) })
                    closeOverlay()
                }
                else -> {}
            }
        }
    }

    if (showMapMenu) {
        MapBottomMenu(onAction = { action ->
            when (action) {
                ActionMapBottomMenu.Home -> closeOverlay()
                ActionMapBottomMenu.ManageAdditionalMaps -> {
                    setMapManagerPosition(cameraPosition.value.target)
                    setOverlay(ADDITIONAL_MAPS)
                }
                ActionMapBottomMenu.Preferences -> setOverlay(PREFERENCES)
                ActionMapBottomMenu.LayersControlFunctions -> setOverlay(LAYERS_CONTROL)
            }
        })
    }

    if (mapFeatures?.isNotEmpty() == true) {
        ListFeaturesScreen(cameraPosition.value.target?.let { com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude) }, mapFeatures, onDismissRequest = { setMapFeatures(null) }, onItemClick = { featureItem ->
            if (featureItem.poicatText == "GeoJsonTile" || featureItem.description == "geojsonTile") {
                val tileName = if (featureItem.name?.startsWith("geojsonTile") == true) featureItem.name else "geojsonTile_${featureItem.name}"
                cameraPosition.value = CameraPosition(cameraPosition.value).apply { target = LatLng(featureItem.lat, featureItem.lon) }
                setToggleGeojsonMapVisibility(tileName)
                setMapFeatures(null)
            } else {
                cameraPosition.value = CameraPosition(cameraPosition.value).apply { target = LatLng(featureItem.lat, featureItem.lon) }
                setMapFeatures(null)
                setSelectedFeature(featureItem)
            }
        }, featuresReady = {})
    }

    if (selectedFeatureItem != null) {
        if (selectedFeatureItem.region.isNotNull() && selectedFeatureItem.name.isNotNull()) {
            val routeEntity = getRouteEntityFromGeojsonByName(context, selectedFeatureItem.name!!)
            setLoadedRoute(routeEntity)
            if (routeEntity.isNotNull()) {
                val lllh = routeEntity!!.kmlString.kmlString2Lllh()
                updatePolygon(PolygonState(lllh, routeEntity.name, routeEntity.distance))
                cameraPosition.value = CameraPosition(cameraPosition.value).apply {
                    target = LatLng(routeEntity.latitudeStart, routeEntity.longitudeStart)
                    bearing = 0.0
                    animationDurationMs = 300
                }
            }
            setSelectedFeature(null)
        } else {
            PoiCatMoBoSheet(selectedFeatureItem.name.toString()) { name, category ->
                if (category != null) {
                    addPoiDao(context, name, com.google.android.gms.maps.model.LatLng(selectedFeatureItem.lat,
                        selectedFeatureItem.lon), -1.0, category) { poi ->
                        //loadPoiData()
                        addPoiEntity(poi)
                    }
                }
                setSelectedFeature(null)
            }
        }
    }

    if (showPreferenceScreen) {
        PrefComposeScreen {
            closeOverlay()
            onRenderModeMapChange(preferences.getString(resources.getString(R.string.pref_render_mode), Const.RENDER_MODE_COMPASS) ?: Const.RENDER_MODE_COMPASS)
        }
    }

    if (showAdditionalMapsManager) {
        if (map != null) {
            AdditionalMapsManager(map, mapManagerPosition, newMvtName = {}) { restartRequired ->
                onUseCyclewayOverlaysChange(preferences.getBoolean(Const.PREF_USE_CYCLEWAYS_OVERLAY, false))
                setMapManagerPosition(null)
                if (restartRequired) {
                    setAppRestartRequired(true)
                    onToggleButtonsBottomBarChange(true)
                    setSnackbar(MainSnackbarData(resources.getString(R.string.restart_is_required), null, null, null))
                    createGeojsonMapBoundFeatures(context, map) { _ -> }
                }
                closeOverlay()
            }
        } else {
            closeOverlay()
            setSnackbar(MainSnackbarData(resources.getString(R.string.map_initialization_error), null, null, null))
        }
    }

    if (showVehicleMenu) VehicleMenu(context) { closeOverlay() }

    if (showGeoCoder) {
        GeoCoderLauncher(cameraPosition.value.target?.let { com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude) }, showInMap = { name: String?, _: String?, latlng: LatLng? ->
            if (name != null) {
                cameraPosition.setCameraTarget(latlng)
            }
            closeOverlay()
        })
    }

    if (showRasterMapsMenu) {
        RasterMapsMenu(context, mapManagerPosition) { action, _, _ ->
            closeOverlay()
            when (action) {
                RasterMapsMenuAction.ToggleGrid -> {
                    map?.let { it1 ->
                        val layerId = resources.getString(R.string.raster_maps_grid)
                        toggleLayerVisibility(it1, layerId)
                    }
                }
                RasterMapsMenuAction.Nothing -> {}
                RasterMapsMenuAction.RasterMapType -> setOverlay(MAP_TYPE)
            }
        }
    }

    if (showMaptypeMenu) {
        MaptypeMenu(context) {
            closeOverlay()
            map?.let { m ->
                removeLayers(m, resources.getString(R.string.raster_maps_grid))
                initMapsGridRaster(context, Property.VISIBLE, "${System.currentTimeMillis()}") { mapsGridRaster ->
                    if (mapsGridRaster != null) {
                        m.style?.addSource(mapsGridRaster.first)
                        m.style?.addLayer(mapsGridRaster.second)
                    }
                }
            }
        }
    }

    if (showMapLongClickMenu) {
        MapLongClickMenu(map, changeGridState = { state, layerId ->
            changeLayerVisibility(map, if (state) Property.VISIBLE else Property.NONE, layerId)
        }, finished = { selection ->
            closeOverlay()
            when (selection) {
                ChangeDimmerState -> setDimmer(!dimmerState)
                SatStatus -> setOverlay(SAT_STATUS)
                ClearGpsCircles -> locationCircles.clear()
                MapLongClickAction.MvtBbbike09 -> {
                    cameraPosition.value.target?.let { cp ->
                        val mvtTile = pointToTile(cp.longitude, cp.latitude, 9.0)
                        val mvtBounds = com.almica.ramani.utils.GeoJsonUtils.tileToGmsBounds(mvtTile)
                        val currentMvtPath = preferences.getString(Const.PREF_MVT_FILEPATH, null)
                        val driveMap = DriveSharedLinks.Companion.MvtRegions().list
                        val mvtname = "mvt_${mvtTile.x}_${mvtTile.y}_${mvtTile.z}${Const.MBTILES_EXT}"
                        val mvtFile = File(File(context.filesDir, Const.MVT_FOLDER), mvtname)
                        if (mvtFile.exists()) {
                            setSnackbar(if (currentMvtPath == mvtFile.path) MainSnackbarData(resources.getString(R.string.map_is_active, mvtname), null, null, null) else MainSnackbarData(resources.getString(R.string.map_is_available_, mvtname), resources.getString(R.string.select_map), SelectMvt, mvtFile.path))
                        } else {
                            val driveUrl = driveMap[mvtname.replace(Const.MBTILES_EXT, "")]
                            if (driveUrl != null) {
                                setSnackbar(MainSnackbarData(resources.getString(R.string.map_available_on_drive, mvtname), resources.getString(android.R.string.ok), Drive, mvtname))
                            } else {
                                val bbbikeUrl = com.almica.ramani.utils.GeoJsonUtils.getBbbikeUrl("mvt_${mvtTile.x}_${mvtTile.y}_${mvtTile.z}", mvtBounds, "mbtiles-basic.zip")
                                bbbikeUrl?.let { setSnackbar(MainSnackbarData(resources.getString(R.string.bbbike_mvt, mvtname), resources.getString(android.R.string.ok), Bbbike, it)) }
                            }
                        }
                    }
                }
                MapLongClickAction.RouteFolders -> setOverlay(ROUTE_FOLDERS)
                MapLongClickAction.PdfViewer -> setOverlay(PDF_VIEWER)
                MapLongClickAction.Nothing -> {}
            }
        }, navigateToHome = { home ->
            closeOverlay()
            cameraPosition.value.target?.let { cp ->
                ghCalc(context, cp.latitude, cp.longitude, home.latitude, home.longitude) { lllh, name, _, _ ->
                    setProgress(null)
                    val dist = lllh.getDistanceFromLllh()
                    setSnackbar(MainSnackbarData(resources.getString(R.string.distance_, dist.formatDistM(true)), null, null, null))
                    val center = lllh.getCenter()
                    val newState = PolygonState(lllh, name, dist).apply {
                        polygonData = PolygonData(lllh, name, dist, false, null)
                        polygonData?.createPolygonMarkers(context, 0.0)
                    }
                    updatePolygon(newState)
                    setLoadedRoute(RouteEntity(UUID.randomUUID(), name, Const.GH_TAG, lllh[0].latitude, lllh[0].longitude, latitudeCenter = center.latitude, longitudeCenter = center.longitude, latitudeStop = lllh[lllh.lastIndex].latitude, longitudeStop = lllh[lllh.lastIndex].longitude, distance = dist, kmlString = lllh.lllhToKmlString(name)))
                    setHighlightRoutePoint(-1)
                    CompassViewModel.setRouteThumbnail(null)
                    setRecalcRequired(false)
                    setStopDragged(false)
                }
            }
        })
    }

    if (gpsValueState == GpsValue.Speedometer) {
        var speed by remember { mutableFloatStateOf(0f) }
        val currentSpeed by animateFloatAsState(targetValue = speed, animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing))
        val sections: ImmutableList<Section> = persistentListOf(Section(0f, 1f, Color(0xFFFF0000.toInt()), width = 15.dp))
        val locomotion = preferences.getString(resources.getString(R.string.setting_locomotion), Const.DEFAULT_LOCOMOTION)
        speed = 3.6f * (GpsViewModel.speed.value ?: 0f)
        if (!showLocationStatistic)
            SpeedView(maxSpeed = when (locomotion) {
                "0.0", "0.1" -> 10f
                "1.1", "1.0" -> 50f
                "2.1", "2.0" -> 200f
                "3.1", "3.0" -> 1200f
                else -> 100f
            }, marksCount = when (locomotion) {
                "0.0", "0.1", "1.1", "1.0" -> 4
                "2.1", "2.0", "3.1", "3.0" -> 9
                else -> 9
            },
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.TopEnd)
                    .padding(
                        bottom = 0.dp,
                        top = 164.dp
                    ),
                unitUnderSpeed = true,
                sections = sections,
                speed = currentSpeed
            )
    }

    if (showPdfRoutes) {
        PdfRoutesDropdownMenu(pdfRoutes, finish = { closeOverlay() }, routeSelection = { name, region ->
            closeOverlay()
            if (region != null && name != null) {
                val routeFile = File(File(File(context.filesDir, Const.ROUTEFOLDER), region), name)
                if (routeFile.exists()) {
                    loadRouteFromFile(routeFile, context, cameraMode, cameraPosition, onLoaded = { entity, state ->
                        setLoadedRoute(entity)
                        updatePolygon(state)
                        setHighlightRoutePoint(-1)
                        setRecalcRequired(false)
                        onPopupSnackMsg(resources.getString(R.string.route_loaded_, routeFile.name))
                    }, loadFailed = { onPopupSnackMsg(resources.getString(R.string.route_load_failed_, routeFile.name)) })
                } else onPopupSnackMsg(resources.getString(R.string.route_not_found_, routeFile.path))
            }
        })
    }

    if (showPdfViewer) {
        DocumentViewer(finish = { closeOverlay() }, routeDataTripleSelection = { triple ->
            closeOverlay()
            triple.first?.let { name ->
                loadRouteFromLllh(triple.third, name, context, cameraMode, cameraPosition, onLoaded = { entity, state ->
                    setLoadedRoute(entity)
                    updatePolygon(state)
                    setHighlightRoutePoint(-1)
                    setRecalcRequired(false)
                    onPopupSnackMsg(resources.getString(R.string.route_loaded_, triple.first))
                })
            }
        })
    }

    if (showSatStatus) SatStatusComposeScreen { closeOverlay() }

    if (showBbbikeFunctionsMenu) {
        BbbikeFunctionsMenu(context) { selection ->
            closeOverlay()
            cameraPosition.value.target?.let { cp ->
                when (selection) {
                    GeojsonBbbike12, GeojsonBbbike13 -> {
                        val zoom = if (selection == GeojsonBbbike12) 12.0 else 13.0
                        val tile = pointToTile(cp.longitude, cp.latitude, zoom)
                        val bounds = com.almica.ramani.utils.GeoJsonUtils.tileToGmsBounds(tile)
                        val suffix = if (selection == GeojsonBbbike13) FeatureProperties.HASHTAG else ""
                        val url = com.almica.ramani.utils.GeoJsonUtils.getBbbikeUrl("geojsonTile_${tile.x}_${tile.y}_${tile.z}$suffix", bounds, "geojson.xz")
                        url?.let { context.startActivity(Intent(Intent.ACTION_VIEW, it)) }
                    }
                    MvtBbbike09 -> {
                        val tile = pointToTile(cp.longitude, cp.latitude, 9.0)
                        val bounds = com.almica.ramani.utils.GeoJsonUtils.tileToGmsBounds(tile)
                        val url = com.almica.ramani.utils.GeoJsonUtils.getBbbikeUrl("mvt_${tile.x}_${tile.y}_${tile.z}", bounds, "mbtiles-basic.zip")
                        url?.let { context.startActivity(Intent(Intent.ACTION_VIEW, it)) }
                    }
                    PmtilesBbbike11, com.almica.ramani.LayersControlAction.PmtilesBbbike10 -> {
                        val zoom = if (selection == PmtilesBbbike11) 11.0 else 10.0
                        val tile = pointToTile(cp.longitude, cp.latitude, zoom)
                        val bounds = com.almica.ramani.utils.GeoJsonUtils.tileToGmsBounds(tile)
                        val url = com.almica.ramani.utils.GeoJsonUtils.getBbbikeUrl("pmtiles_${tile.x}_${tile.y}_${tile.z}", bounds, "pmtiles-basic.zip")
                        url?.let { context.startActivity(Intent(Intent.ACTION_VIEW, it)) }
                    }
                    else -> {}
                }
            }
        }
    }

    if (showLayersControlMenu) {
        if (map != null) {
            LayersControlMenu(context, map, prefMaptypeKey, changeGridState = { state, layerId ->
                changeLayerVisibility(map, if (state) Property.VISIBLE else Property.NONE, layerId)
            }, finished = { selection ->
                closeOverlay()
                if (selection == ToggleRoutesGeojson) setOverlay(ROUTE_FILES_REGION)
            }, changePlanetState = { state -> setPlanetVisibility(context, state, map.style) }, changeRoutesLayerState = { state ->
                val vis = if (state) Property.VISIBLE else Property.NONE
                changeLayerVisibility(map, vis, resources.getString(R.string.routes) + LINES_TAG)
                changeLayerVisibility(map, vis, resources.getString(R.string.routes) + FeatureProperties.HITLAYER_TAG)
            })
        } else closeOverlay()
    }

    if (showMvtList) {
        ListMvtScreen(PaddingValues(), newMvtMap = {}) { result ->
            setAppRestartRequired(result)
            onToggleButtonsBottomBarChange(uiState.appRestartRequired)
            closeOverlay()
        }
    }

    if (showRouteFilesRegionList) {
        RouteRegionsMenu(context, routesRegionFilter) { action, region ->
            val layerId = resources.getString(R.string.routes) + FeatureProperties.LINES_TAG
            map?.let { m ->
                (getLayer(m, layerId) as? LineLayer)?.let { layer ->
                    when (action) {
                        RouteRegionsMenuAction.All -> {
                            layer.setProperties(PropertyFactory.visibility(Property.VISIBLE))
                            liveSharedPreferences.preferences.edit {
                                putString(
                                    resources.getString(R.string.pref_routes_geojson_visibility),
                                    Property.VISIBLE
                                )
                            }
                            layer.setFilter(Expression.gt(Expression.get("region"), Expression.literal("")))
                            setRoutesRegionFilter(resources.getString(R.string.all))
                        }
                        RouteRegionsMenuAction.None -> {
                            layer.setProperties(PropertyFactory.visibility(Property.NONE))
                            liveSharedPreferences.preferences.edit {
                                putString(
                                    resources.getString(R.string.pref_routes_geojson_visibility),
                                    Property.NONE
                                )
                            }
                            setRoutesRegionFilter(resources.getString(R.string.none))
                        }
                        RouteRegionsMenuAction.Region -> {
                            layer.setFilter(Expression.eq(Expression.get("region"), Expression.literal(region.toString())))
                            layer.setProperties(PropertyFactory.visibility(Property.VISIBLE))
                            liveSharedPreferences.preferences.edit {
                                putString(
                                    resources.getString(R.string.pref_routes_geojson_visibility),
                                    Property.VISIBLE
                                )
                            }
                            setRoutesRegionFilter(region ?: resources.getString(R.string.none))
                        }
                        else -> {}
                    }
                }
            }
            closeOverlay()
        }
    }

    if (showRouteMonitorMenu) {
        RouteMonitorMenu(context) { selection ->
            Timber.i("RouteMonitorMenu ${selection.name}")
            closeOverlay()
            when (selection) {
                RouteMonitorSelection.Gradient -> {
                    setGradientRoute(loadedRouteEntity?.copy())
                    setChartRoute(null)
                    setRouteMonitorState(1)
                }
                RouteMonitorSelection.Elevation -> {
                    setChartRoute(loadedRouteEntity?.copy())
                    setGradientRoute(null)
                    setRouteMonitorState(2)
                }
                RouteMonitorSelection.Nothing -> {
                    setRouteMonitorState(0)
                    setChartRoute(null)
                    setGradientRoute(null)
                }
                RouteMonitorSelection.Save -> setOverlay(ROUTE_SAVING)
                RouteMonitorSelection.Remove -> {
                    polygonState.polygonData?.polygonMarkerDataList = listOf()
                    updatePolygon(PolygonState(arrayListOf(), "", 0.0))
                    setHighlightRoutePoint(-1)
                    CompassViewModel.setRouteThumbnail(null)
                    CompassViewModel.setDestination(stopPosition?.let { GmsLatLng(it.latitude, it.longitude) }, stopPosition?.altitude?.toInt())
                    setLoadedRoute(null)
                    setRecalcRequired(false)
                }
                RouteMonitorSelection.Reverse -> {
                    reverseRoute(context, polygonState, loadedRouteEntity) { ns, ne, sb ->
                        updatePolygon(ns)
                        setLoadedRoute(ne)
                        setSnackbar(sb)
                    }
                    setHighlightRoutePoint(-1)
                    CompassViewModel.setRouteThumbnail(null)
                    setRecalcRequired(false)
                }
                RouteMonitorSelection.SrtmRefresh -> {
                    cameraPosition.value.target?.let { pos ->
                        refreshRouteElevation(context, pos, polygonState, loadedRouteEntity, onSuccess = { ns, ne, sb ->
                            updatePolygon(ns)
                            setLoadedRoute(ne)
                            setSnackbar(sb)
                            setHighlightRoutePoint(-1)
                            CompassViewModel.setRouteThumbnail(null)
                            setRecalcRequired(false)
                        }, onFailure = { sb -> setSnackbar(sb) })
                    }
                }
            }
            if (routeMonitorState > 0) {
                loadedRouteEntity?.let { re ->
                    cameraPosition.value = CameraPosition(cameraPosition.value).apply { target = LatLng(re.latitudeStart, re.longitudeStart) }
                }
            }
        }
    }

    if (showGhFolders) { ListGhScreen { closeOverlay() } }

    if (showPoiDatabase) {
        PoiDatabaseScreen(0f, cameraPosition.value.target?.let { GmsLatLng(it.latitude, it.longitude) }) { poiEntity, action ->
            poiEntity?.let {
                when (action) {
                    PoiItemAction.Map -> {
                        cameraPosition.value = CameraPosition(cameraPosition.value).apply { target = LatLng(it.latitude, it.longitude) }
                        closeOverlay()
                    }
                    PoiItemAction.Delete -> setSnackbar(MainSnackbarData(resources.getString(R.string.restart_required), null, null, null))
                    PoiItemAction.Stop -> {
                        setStopPosition(LatLng(it.latitude, it.longitude))
                        closeOverlay()
                        setSnackbar(MainSnackbarData("${resources.getString(R.string.stop_marker_set)}, ${resources.getString(R.string.route_calculation)}?", resources.getString(android.R.string.ok), RouteCalculation, null))
                        CompassViewModel.setDestination(GmsLatLng(it.latitude, it.longitude), it.altitude.toInt())
                    }
                    PoiItemAction.ElevationRefresh -> Timber.i("ElevationRefresh ${it.name}")
                }
            }
            closeOverlay()
        }
    }

    showRouteInfo?.let { routeFile ->
        RouteDialog(context.filesDir, routeFile, finish = { setRouteInfo(null) }, alert = { msg ->
            setRouteInfo(null)
            onPopupSnackMsg(msg)
        }, share = {
            setRouteInfo(null)
            onPopupSnackMsg(resources.getString(R.string.does_nothing_here))
        }, refresh = {
            setRouteInfo(null)
            onPopupSnackMsg(resources.getString(R.string.does_nothing_here))
        }, select = {
            closeOverlay()
            setRouteInfo(null)
            if (routeFile.extension == Const.GEOJSON_EXT.replace(".", "")) {
                val routesGeojsonSource = map?.style?.getSource("routes${Const.GEOJSON_EXT}") as? GeoJsonSource
                val geojson = routeFile.readText()
                onRoutesGeoJsonStringChange(geojson)
                routesGeojsonSource?.setGeoJson(geojson)
                val routesHitGeojsonSource = map?.style?.getSource("routes${Const.GEOJSON_EXT}${FeatureProperties.HITLAYER_TAG}") as? GeoJsonSource
                routesHitGeojsonSource?.setGeoJson(geojson)
                liveSharedPreferences.preferences.edit {
                    putString(
                        resources.getString(R.string.pref_routes_geojson_visibility),
                        Property.VISIBLE
                    )
                }
                onPopupSnackMsg(resources.getString(R.string.geojson_loaded_, routeFile.nameWithoutExtension))
            } else {
                loadRouteFromFile(routeFile, context, cameraMode, cameraPosition, onLoaded = { entity, state ->
                    setLoadedRoute(entity)
                    updatePolygon(state)
                    setHighlightRoutePoint(-1)
                    setRecalcRequired(false)
                }, loadFailed = {
                    if (routeFile.extension == Const.JPG_EXT.replace(".", "")) {
                        val checkGeojson = com.almica.ramani.Helpers.getImageDescriptionFromExif(routeFile)
                        if (checkGeojson?.startsWith(Const.GEOJSON_ROOT_FOLDER) == true) {
                            val pair = getGeojsonFromSnapshot(routeFile, map, liveSharedPreferences, context)
                            onPopupSnackMsg(pair.first)
                            onRoutesGeoJsonStringChange(pair.second)
                        }
                    } else onPopupSnackMsg(resources.getString(R.string.route_load_failed_, routeFile.name))
                })
            }
        }, dialogModeOrdinal = RouteDialogMode.MapProvider.ordinal)
    }

    if (showRouteFolders) {
        RamaniApp(onDocumentViewerFinish = { closeOverlay() }, onDocumentViewerResult = { resultRouteTriple ->
            closeOverlay()
            resultRouteTriple.first?.let { name ->
                loadRouteFromLllh(resultRouteTriple.third, name, context, cameraMode, cameraPosition, onLoaded = { entity, state ->
                    setLoadedRoute(entity)
                    updatePolygon(state)
                    setHighlightRoutePoint(-1)
                    setRecalcRequired(false)
                    onPopupSnackMsg(resources.getString(R.string.route_loaded_, resultRouteTriple.first))
                })
            }
        }, onRouteFolderSelected = {}, onRouteFolderFinished = { closeOverlay() }, onRouteSelected = { routeFile ->
            closeOverlay()
            when {
                routeFile.extension == Const.JPG_EXT.replace(".", "") && routeFile.path.contains(Const.THUMBNAILS) -> setRouteInfo(routeFile)
                routeFile.extension == Const.GEOJSON_EXT.replace(".", "") -> {
                    val routesGeojsonSource = map?.style?.getSource("routes${Const.GEOJSON_EXT}") as? GeoJsonSource
                    val geojson = routeFile.readText()
                    onRoutesGeoJsonStringChange(geojson)
                    routesGeojsonSource?.setGeoJson(geojson)
                    val routesHitGeojsonSource = map?.style?.getSource("routes${Const.GEOJSON_EXT}${FeatureProperties.HITLAYER_TAG}") as? GeoJsonSource
                    routesHitGeojsonSource?.setGeoJson(geojson)
                    liveSharedPreferences.preferences.edit {
                        putString(
                            resources.getString(R.string.pref_routes_geojson_visibility),
                            Property.VISIBLE
                        )
                    }
                    onPopupSnackMsg(resources.getString(R.string.geojson_loaded_, routeFile.nameWithoutExtension))
                }
                else -> {
                    loadRouteFromFile(routeFile, context, cameraMode, cameraPosition, onLoaded = { entity, state ->
                        setLoadedRoute(entity)
                        updatePolygon(state)
                        setHighlightRoutePoint(-1)
                        setRecalcRequired(false)
                    }, loadFailed = {
                        if (routeFile.extension == Const.JPG_EXT.replace(".", "")) {
                            val checkGeojson = com.almica.ramani.Helpers.getImageDescriptionFromExif(routeFile)
                            if (checkGeojson?.startsWith(Const.GEOJSON_ROOT_FOLDER) == true) {
                                val pair = getGeojsonFromSnapshot(routeFile, map, liveSharedPreferences, context)
                                onPopupSnackMsg(pair.first)
                                onRoutesGeoJsonStringChange(pair.second)
                            }
                        } else onPopupSnackMsg(resources.getString(R.string.route_load_failed_, routeFile.name))
                    })
                }
            }
        }, onRouteInfoSelected = { setRouteInfo(it) }, createSnapshots = {}, dialogMode = RouteDialogMode.MapProvider.ordinal)
    }

    if (showRouteFiles) {
        RoutesManager(userLocation.value) { routeEntity, routeAction ->
            closeOverlay()
            when (routeAction) {
                RouteMenu.Home -> closeOverlay()
                RouteMenu.Map -> displayRouteOnMap(routeEntity, context, cameraMode, cameraPosition) { entity, state ->
                    setLoadedRoute(entity)
                    updatePolygon(state)
                    setHighlightRoutePoint(-1)
                    setRecalcRequired(false)
                }
                RouteMenu.Chart, RouteMenu.Gradient -> {
                    loadedRouteEntity?.let {
                        if (routeAction == RouteMenu.Chart) setChartRoute(it.copy()) else setGradientRoute(it.copy())
                        val cameraModeClone = cameraMode.value
                        cameraMode.value = CameraMode.NONE
                        cameraPosition.value = CameraPosition(cameraPosition.value).apply {
                            target = LatLng(it.latitudeStart, it.longitudeStart)
                            bearing = 0.0
                            animationDurationMs = 300
                        }
                        cameraMode.value = cameraModeClone
                    }
                }
                else -> {}
            }
        }
    }

    if (progressMsg != null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.fillMaxHeight(0.2f))
            TextButton(onClick = { setProgress(null) }) {
                Text(text = progressMsg, textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineMedium, modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(corner = CornerSize(10.dp))
                    ))
            }
            CircularProgressIndicator(modifier = Modifier.size(50.dp), color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.surfaceVariant, strokeWidth = 6.dp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapOverlayManagerPreview() {
    val context = LocalContext.current
    val liveSharedPreferences = remember { LiveSharedPreferences(PreferenceManager.getDefaultSharedPreferences(context)) }
    val cameraPosition = remember { mutableStateOf(CameraPosition()) }
    val cameraMode = remember { mutableIntStateOf(0) }
    val userLocation = remember { mutableStateOf(Location("dummy")) }
    val locationCircles = remember { mutableListOf<LatLng>() }

    RamaniTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            MapOverlayManagerContent(
                uiState = MainUiState(progressMsg = "Loading maps..."),
                map = null,
                cameraPosition = cameraPosition,
                cameraMode = cameraMode,
                userLocation = userLocation,
                prefMaptypeKey = 0,
                liveSharedPreferences = liveSharedPreferences,
                onPopupSnackMsg = {},
                onRoutesGeoJsonStringChange = {},
                onRenderModeMapChange = {},
                onUseCyclewayOverlaysChange = {},
                onToggleButtonsBottomBarChange = {},
                startTime = 0L,
                locationCircles = locationCircles,
                setToggleGeojsonMapVisibility = {},
                closeOverlay = {},
                setSelectedFeature = {},
                setStopPosition = {},
                setProgress = {},
                updatePolygon = {},
                setLoadedRoute = {},
                setHighlightRoutePoint = {},
                setRecalcRequired = {},
                setStopDragged = {},
                setSnackbar = {},
                setMapFeatures = {},
                setOverlay = {},
                setLogCount = {},
                setMapManagerPosition = {},
                setAppRestartRequired = {},
                setDimmer = {},
                setGradientRoute = {},
                setChartRoute = {},
                addPoiEntity = {},
                setRouteMonitorState = {},
                setRoutesRegionFilter = {},
            ) {}
        }
    }
}
