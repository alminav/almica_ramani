package com.almica.ramani

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.location.Location
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.almica.ramani.FeatureProperties.Companion.LINES_TAG
import com.almica.ramani.LayersControlAction.GeojsonBbbike12
import com.almica.ramani.LayersControlAction.GeojsonBbbike13
import com.almica.ramani.LayersControlAction.MvtBbbike09
import com.almica.ramani.LayersControlAction.PmtilesBbbike11
import com.almica.ramani.LayersControlAction.ToggleRoutesGeojson
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
import com.almica.ramani.utils.launchTilemaker
import com.almica.ramani.utils.ValuePickerDialog
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
import kotlinx.coroutines.withContext
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
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.MapManagementAction.*
import com.almica.ramani.externalData.MagentaCloudDownloader
import com.almica.ramani.externalData.MagentaCloudMbtiles
import com.almica.ramani.externalData.MagentaCloudMvt
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.googlemaps.MapUtils
import com.almica.ramani.pois.PoiEntity
import com.almica.ramani.routes.MAX_ELEVATION_POINTS
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.RouteSmoothingUtil.simplifyToTargetCount
import com.almica.ramani.weather.WeatherScreen
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.CoroutineScope
import java.util.Locale
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
    onPopupSnackMsg: (String?) -> Unit,
    onRoutesGeoJsonStringChange: (String?) -> Unit,
    onRenderModeMapChange: (String) -> Unit,
    onUseCyclewayOverlaysChange: (Boolean) -> Unit,
    onToggleButtonsBottomBarChange: (Boolean) -> Unit,
    locationCircles: MutableList<LatLng>
) {
    MapOverlayManagerContent(
        uiState = uiState,
        map = map,
        cameraPosition = cameraPosition,
        cameraMode = cameraMode,
        userLocation = userLocation,
        prefMaptypeKey = prefMaptypeKey,
        onPopupSnackMsg = onPopupSnackMsg,
        onRoutesGeoJsonStringChange = onRoutesGeoJsonStringChange,
        onRenderModeMapChange = onRenderModeMapChange,
        onUseCyclewayOverlaysChange = onUseCyclewayOverlaysChange,
        onToggleButtonsBottomBarChange = onToggleButtonsBottomBarChange,
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
        confirmMvtChange = { viewModel?.confirmMvtChange() },
        dismissMvtConfirmation = { viewModel?.dismissMvtConfirmation() },
        setClipText = { viewModel?.setClipText(it) }
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
    onPopupSnackMsg: (String?) -> Unit,
    onRoutesGeoJsonStringChange: (String?) -> Unit,
    onRenderModeMapChange: (String) -> Unit,
    onUseCyclewayOverlaysChange: (Boolean) -> Unit,
    onToggleButtonsBottomBarChange: (Boolean) -> Unit,
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
    confirmMvtChange: () -> Unit,
    dismissMvtConfirmation: () -> Unit,
    setClipText: (String?) -> Unit,
    setRouteInfo: (File?) -> Unit,
) {
    val liveSharedPreferences = LocalLiveSharedPreferences.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val preferences = getDefaultSharedPreferences(context)
    val poiRepository = remember { PoiRepository.getInstance(context, Executors.newSingleThreadExecutor()) }

    val showPreferenceScreen = uiState.activeOverlay == PREFERENCES
    val showGhFolders = uiState.activeOverlay == GH_FOLDERS
    val showVehicleMenu = uiState.activeOverlay == VEHICLE_MENU
    val showGeoCoder = uiState.activeOverlay == GEO_CODER
    val showRouteMonitorMenu = uiState.activeOverlay == ROUTE_MONITOR
    val showMapLongClickMenu = uiState.activeOverlay == MAP_LONG_CLICK
    val showMapManagementMenu = uiState.activeOverlay == OverlayType.MAP_MANAGEMENT
    val showRasterMapsMenu = uiState.activeOverlay == RASTER_MAPS
    val showMaptypeMenu = uiState.activeOverlay == MAP_TYPE
    val showSatStatus = uiState.activeOverlay == SAT_STATUS
    val showPdfViewer = uiState.activeOverlay == PDF_VIEWER
    val showPdfRoutes = uiState.activeOverlay == PDF_ROUTES
    val showLayersControlMenu = uiState.activeOverlay == LAYERS_CONTROL
    val showRasterMaptypeMenu = uiState.activeOverlay == OverlayType.RASTER_MAPTYPE
    val showBbbikeFunctionsMenu = uiState.activeOverlay == BBBIKE_FUNCTIONS
    // Place Weather check here to ensure it can be evaluated alongside or after menus
    val showWeather = uiState.activeOverlay == OverlayType.WEATHER
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
    val showValuePicker = uiState.activeOverlay == OverlayType.VALUE_PICKER
    var roundTripFactor by remember { mutableFloatStateOf(0.5f) }
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
    val dimmerState = uiState.dimmerState
    val routesRegionFilter = uiState.routesRegionFilter
    val showRouteInfo = uiState.showRouteInfo
    val downloader: MagentaCloudDownloader = remember { MagentaCloudDownloader() }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadMessage by remember { mutableStateOf<String?>(null) }

    fun startDownload(
        fileName: String,
        link: String,
        onProcess: suspend (File) -> Unit
    ) {
        Timber.i("Start download of $fileName from $link")
        scope.launch {
            try {
                isDownloading = true
                downloadMessage = resources.getString(R.string.download_starting)
                val targetFile = File(fileName)
                val downloadedFile = downloader.downloadFile(link, targetFile)

                if (downloadedFile != null) {
                    Timber.i("Download successful: ${downloadedFile.absolutePath}")
                    downloadMessage = resources.getString(R.string.download_success, downloadedFile.name)

                    try {
                        onProcess(downloadedFile)
                        //onGhFoldersRefresh()
                    } catch (e: Exception) {
                        Timber.e(e, "Processing failed for $fileName")
                        downloadMessage = resources.getString(R.string.processing_failed)
                    } finally {
                        //val bCleanup = downloadedFile.delete()
                        //Timber.i("Cleanup: $bCleanup ${downloadedFile.path}")
                        Timber.i("successful download: ${downloadedFile.path}")
                    }
                } else {
                    Timber.e("Download failed.")
                    downloadMessage = resources.getString(R.string.download_failed)
                }
            } finally {
                isDownloading = false
            }
        }
    }
    if (isDownloading || downloadMessage != null) {
        AlertDialog(
            onDismissRequest = { if (!isDownloading) downloadMessage = null },
            title = { Text(if (isDownloading) "Download läuft" else "Download Status") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                    downloadMessage?.let { Text(it) }
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    TextButton(onClick = { downloadMessage = null }) {
                        Text("OK")
                    }
                }
            }
        )
    }
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
    if (dimmerState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        )
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
                HairCrossAction.Roundtrip -> {
                    setOverlay(OverlayType.VALUE_PICKER)
                }
                HairCrossAction.Calc -> {
                    val startLat = cameraPosition.value.target?.latitude
                    val startLon = cameraPosition.value.target?.longitude
                    calculateGhRoute(context, resources, startLat, startLon, stopPosition, false, roundTripFactor, setProgress, updatePolygon, setLoadedRoute, setHighlightRoutePoint, setRecalcRequired, setStopDragged, setSnackbar, closeOverlay)
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

    if (showValuePicker) {
        ValuePickerDialog(
            onDismissRequest = { closeOverlay() },
            onValueSelected = { value ->
                roundTripFactor = value
                closeOverlay()
                val startLat = cameraPosition.value.target?.latitude
                val startLon = cameraPosition.value.target?.longitude
                calculateGhRoute(context, resources, startLat, startLon, stopPosition, true, roundTripFactor, setProgress, updatePolygon, setLoadedRoute, setHighlightRoutePoint, setRecalcRequired, setStopDragged, setSnackbar, {})
            },
            initialValue = roundTripFactor,
            title = resources.getString(R.string.roundtrip_factor),
        )
    }

    if (showRouteSavingScreen) {
        RouteFileSaveMoBoSheet(polygonState.name.replace(Const.JPG_EXT, "").replace(Const.GPX_EXT, "").replace(Const.KML_EXT, "")) { targetFileName, targetRouteFolder ->
            closeOverlay()
            if (targetRouteFolder != null) {
                val routeFolder = File(File(context.filesDir, Const.ROUTEFOLDER), targetRouteFolder.first)
                val routeFile = File(routeFolder, targetFileName.replace(Const.JPG_EXT, "").replace(Const.GPX_EXT, "").replace(Const.KML_EXT, "") + Const.KML_EXT)
                val result = Helpers.writeLllh2KmlFile(polygonState.lllh, routeFile.path)
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
                ActionMapBottomMenu.RasterMaptype -> setOverlay(OverlayType.RASTER_MAPTYPE)
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
            PoiCatDialog(selectedFeatureItem.name.toString()) { name, category ->
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
                        m.style?.addSource(mapsGridRaster.source)
                        m.style?.addLayer(mapsGridRaster.lineLayer)
                    }
                }
            }
        }
    }

    if (showMapLongClickMenu) {
        MapLongClickMenu(map, changeGridState = { state, layerId ->
            changeLayerVisibility(map, if (state) Property.VISIBLE else Property.NONE, layerId)
        }, finished = { selection ->
            Timber.i("$selection")
            closeOverlay()
            when (selection) {
                ChangeDimmerState -> setDimmer(!dimmerState)
                SatStatus -> setOverlay(SAT_STATUS)
                ClearGpsCircles -> locationCircles.clear()
                MapLongClickAction.ManageMvtMap -> {
                    cameraPosition.value.target?.let { cp ->
                        val mvtTile = pointToTile(cp.longitude, cp.latitude, 9.0)
                        val currentMvtPath = preferences.getString(Const.PREF_MVT_FILEPATH, null)
                        val mvtname = "mvt_${mvtTile.x}_${mvtTile.y}_${mvtTile.z}${Const.MBTILES_EXT}"
                        val mvtFile = File(File(context.filesDir, Const.MVT_FOLDER), mvtname)
                        if (uiState.prefMaptypeKey == MaptypeKey.Mvt.ordinal && mvtFile.exists()) {
                            setSnackbar(if (currentMvtPath == mvtFile.path) MainSnackbarData(resources.getString(R.string.map_is_active, mvtname),
                                null, null, null) else MainSnackbarData(resources.getString(R.string.map_is_available_, mvtname), resources.getString(R.string.select_map), SelectMvt, mvtFile.path))
                        } else { // DropdownMenu Download, Import, Bbbike
                            setMapManagerPosition(cp)
                            setOverlay(OverlayType.MAP_MANAGEMENT)
                        }
                    }
                }
                MapLongClickAction.RouteFolders -> setOverlay(ROUTE_FOLDERS)
                MapLongClickAction.PdfViewer -> setOverlay(PDF_VIEWER)
                MapLongClickAction.Nothing -> {}
                MapLongClickAction.Weather -> setOverlay(OverlayType.WEATHER)
            }
        }, navigateToHome = { home ->
            closeOverlay()
            cameraPosition.value.target?.let { cp ->
                ghCalc(
                    context,
                    cp.latitude,
                    cp.longitude,
                    home.latitude,
                    home.longitude
                ) { lllh, name, _, _ ->
                    setProgress(null)
                    val dist = lllh.getDistanceFromLllh()
                    setSnackbar(
                        MainSnackbarData(
                            resources.getString(
                                R.string.distance_,
                                dist.formatDistM(true)
                            ), null, null, null
                        )
                    )
                    val center = lllh.getCenter()
                    val newState = PolygonState(lllh, name, dist).apply {
                        polygonData = PolygonData(lllh, name, dist, false, null)
                        polygonData?.createPolygonMarkers(context, 0.0)
                    }
                    updatePolygon(newState)
                    setLoadedRoute(
                        RouteEntity(
                            UUID.randomUUID(),
                            name,
                            Const.GH_TAG,
                            lllh[0].latitude,
                            lllh[0].longitude,
                            latitudeCenter = center.latitude,
                            longitudeCenter = center.longitude,
                            latitudeStop = lllh[lllh.lastIndex].latitude,
                            longitudeStop = lllh[lllh.lastIndex].longitude,
                            distance = dist,
                            kmlString = lllh.lllhToKmlString(name)
                        )
                    )
                    setHighlightRoutePoint(-1)
                    CompassViewModel.setRouteThumbnail(null)
                    setRecalcRequired(false)
                    setStopDragged(false)
                }
            }
        })
    }

    if (gpsValueState == GpsValue.Speedometer) {
        val gpsSpeed by GpsViewModel.speed.collectAsStateWithLifecycle()
        val currentSpeed by animateFloatAsState(targetValue = gpsSpeed * Const.MS_TO_KMH, animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing))
        val sections: ImmutableList<Section> = persistentListOf(Section(0f, 1f, Color(0xFFFF0000.toInt()), width = 15.dp))
        val locomotion = preferences.getString(resources.getString(R.string.setting_locomotion), Const.DEFAULT_LOCOMOTION)
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
        DocumentViewer(finish = { closeOverlay() }, routeDataSelection = { info ->
            closeOverlay()
            info.name?.let { name ->
                loadRouteFromLllh(info.points, name, context, cameraMode, cameraPosition, onLoaded = { entity, state ->
                    setLoadedRoute(entity)
                    updatePolygon(state)
                    setHighlightRoutePoint(-1)
                    setRecalcRequired(false)
                    onPopupSnackMsg(resources.getString(R.string.route_loaded_, info.name))
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
                        val bounds = GeoJsonUtils.tileToGmsBounds(tile)
                        val suffix = if (selection == GeojsonBbbike13) FeatureProperties.HASHTAG else ""
                        val url = GeoJsonUtils.getBbbikeUrl("geojsonTile_${tile.x}_${tile.y}_${tile.z}$suffix", bounds, "geojson.xz")
                        url?.let { context.startActivity(Intent(Intent.ACTION_VIEW, it)) }
                    }
                    MvtBbbike09 -> {
                        val tile = pointToTile(cp.longitude, cp.latitude, 9.0)
                        val bounds = GeoJsonUtils.tileToGmsBounds(tile)
                        val url = GeoJsonUtils.getBbbikeUrl("mvt_${tile.x}_${tile.y}_${tile.z}", bounds, "mbtiles-basic.zip")
                        url?.let { context.startActivity(Intent(Intent.ACTION_VIEW, it)) }
                    }
                    PmtilesBbbike11, LayersControlAction.PmtilesBbbike10 -> {
                        val zoom = if (selection == PmtilesBbbike11) 11.0 else 10.0
                        val tile = pointToTile(cp.longitude, cp.latitude, zoom)
                        val bounds = GeoJsonUtils.tileToGmsBounds(tile)
                        val url = GeoJsonUtils.getBbbikeUrl("pmtiles_${tile.x}_${tile.y}_${tile.z}", bounds, "pmtiles-basic.zip")
                        url?.let { context.startActivity(Intent(Intent.ACTION_VIEW, it)) }
                    }
                    else -> {}
                }
            }
        }
    }
    if (showRasterMaptypeMenu) {
        MaptypeMenu(context) { maptype ->
            Timber.i("RasterMaptype $maptype")
            closeOverlay()
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

    if (showMapManagementMenu) {
        MapManagementMenu(uiState.prefMaptypeKey,
            mapManagerPosition?.let { GmsLatLng(it.latitude, it.longitude) },
            finished =  { action, mapName, mapTile, link ->
            when (action) {
                Nothing -> {closeOverlay()}
                Download -> {
                    mapName?.let {
                        Timber.i("Download $it")
                        val mvtFolder = File(context.filesDir,
                            if (uiState.prefMaptypeKey == MaptypeKey.Mvt.ordinal) Const.MVT_FOLDER else Const.MBTILES_FOLDER)
                        val targetFile = File(mvtFolder, mapName)
                        link?.let { directDownloadUrl ->
                            startDownload(
                                targetFile.path, directDownloadUrl,
                                onProcess = { file ->
                                    Timber.i("onProcess ${file.path}")
                                }
                            )
                        }
                    }
                    closeOverlay()
                }
                Import -> {
                    Timber.i("Import $mapName")
                    setClipText(mapName)
                    FileImportActivity.launch(context,
                        if (uiState.prefMaptypeKey == MaptypeKey.Mvt.ordinal) FileType.Mvt
                                    else FileType.MbTiles)
                    closeOverlay()
                }
                Create -> {
                    if (uiState.prefMaptypeKey == MaptypeKey.Raster.ordinal) {
                        mapName?.let {
                            val rasterBounds = mapTile?.let { tile ->
                                GeoJsonUtils.tileToGmsBounds(tile)
                            }
                            rasterBounds?.let { bounds ->
                                val currentMapType = preferences.getString(
                                    resources.getString(R.string.pref_tilemaker_maptype),
                                    Const.OUTDOOR
                                ) ?: Const.OUTDOOR
                                launchTilemaker(
                                    context,
                                    it.replace(Const.MBTILES_EXT, ""),
                                    bounds,
                                    currentMapType
                                )
                            }
                        }
                    } else if (uiState.prefMaptypeKey == MaptypeKey.Mvt.ordinal) {
                        mapName?.let {
                            val mvtBounds = mapTile?.let { tile ->
                                GeoJsonUtils.tileToGmsBounds(tile)
                            }
                            mvtBounds?.let {
                                val bbbikeUrl = GeoJsonUtils.getBbbikeUrl(
                                    "mvt_${mapTile.x}_${mapTile.y}_${mapTile.z}",
                                    mvtBounds,
                                    "mbtiles-basic.zip"
                                )
                                bbbikeUrl?.let {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            it
                                        )
                                    )
                                }
                            }
                        }
                    }
                    closeOverlay()
                }
                null -> {
                    closeOverlay()
                }
            }
        }, onPopupSnackMsg = { msg ->
            Timber.i("onPopupSnackMsg $msg")
            onPopupSnackMsg(msg)
        })
    }

    if (showRouteFilesRegionList) {
        RouteRegionsMenu(context, routesRegionFilter) { action, region ->
            val layerId = resources.getString(R.string.routes) + LINES_TAG
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
                        scope.launch {
                            setProgress(resources.getString(R.string.refreshing_elevation))
                            refreshRouteElevation(
                                context,
                                pos,
                                polygonState,
                                loadedRouteEntity,
                                onSuccess = { ns, ne, sb ->
                                    updatePolygon(ns)
                                    setLoadedRoute(ne)
                                    setSnackbar(sb)
                                    setHighlightRoutePoint(-1)
                                    CompassViewModel.setRouteThumbnail(null)
                                    setRecalcRequired(false)
                                    setProgress(null)
                                },
                                onFailure = { sb -> setSnackbar(sb); setProgress(null) })
                        }
                    }
                }
                RouteMonitorSelection.GmsElevation -> {
                    val gmsLatLng = if (polygonState.lllh.size > MAX_ELEVATION_POINTS) {
                        polygonState.lllh.simplifyToTargetCount(MAX_ELEVATION_POINTS)
                    } else {
                        polygonState.lllh
                    }.map { it.latLngGms }
                    val encodedPolyline = PolyUtil.encode(gmsLatLng)
                    scope.launch {
                        setProgress(resources.getString(R.string.refreshing_elevation))
                        val refreshedLllh = MapUtils.gmsElevationService(context, "enc:${encodedPolyline}")
                        setProgress(null)
                        if (refreshedLllh.isNotEmpty()) {
                            updatePolygon(polygonState.copy(lllh = refreshedLllh))
                            setLoadedRoute(loadedRouteEntity?.copy(kmlString = refreshedLllh.lllhToKmlString(polygonState.name)))
                            setHighlightRoutePoint(-1)
                            CompassViewModel.setRouteThumbnail(null)
                            setRecalcRequired(false)

                            setSnackbar(
                                MainSnackbarData( resources.getString(R.string._gms_refresh_done, polygonState.name),
                                    action = MainSnackbarSelection.Nothing, actionText = null, data = null)
                            )
                        }
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

    if (showGhFolders) {
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 160.dp, bottom = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            ListGhScreen(latlng = cameraPosition.value.target, selectGhFolder = { _ -> closeOverlay() })
        }
    }

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
            handleRouteFileSelection(
                routeFile, map, context, cameraMode, cameraPosition, liveSharedPreferences,
                resources, lifecycleOwner.lifecycleScope, onRoutesGeoJsonStringChange, onPopupSnackMsg,
                setLoadedRoute, updatePolygon, setHighlightRoutePoint, setRecalcRequired, setRouteInfo,
                closeOverlay, clearRouteInfo = true
            )
        }, dialogModeOrdinal = RouteDialogMode.MapProvider.ordinal)
    }

    if (showRouteFolders) {
        RamaniApp(onDocumentViewerFinish = { closeOverlay() }, onDocumentViewerResult = { resultRouteInfo ->
            closeOverlay()
            resultRouteInfo.name?.let { name ->
                loadRouteFromLllh(resultRouteInfo.points, name, context, cameraMode, cameraPosition, onLoaded = { entity, state ->
                    setLoadedRoute(entity)
                    updatePolygon(state)
                    setHighlightRoutePoint(-1)
                    setRecalcRequired(false)
                    onPopupSnackMsg(resources.getString(R.string.route_loaded_, resultRouteInfo.name))
                })
            }
        }, onRouteFolderSelected = {}, onRouteFolderFinished = { closeOverlay() },
            onRouteSelected  = { routeFile ->
                Timber.i("onRouteSelected ${routeFile.name}")
                handleRouteFileSelection(
                    routeFile, map, context, cameraMode, cameraPosition, liveSharedPreferences,
                    resources, lifecycleOwner.lifecycleScope, onRoutesGeoJsonStringChange, onPopupSnackMsg,
                    setLoadedRoute, updatePolygon, setHighlightRoutePoint, setRecalcRequired, setRouteInfo,
                    closeOverlay
                )
            },
            onRouteSnapshotSelected  = { routeFileBundle ->
                Timber.i("onRouteSnapshotSelected ${routeFileBundle.routeFile.name}")
                handleRouteFileSelection(
                    routeFileBundle.routeFile, map, context, cameraMode, cameraPosition, liveSharedPreferences,
                    resources, lifecycleOwner.lifecycleScope, onRoutesGeoJsonStringChange, onPopupSnackMsg,
                    setLoadedRoute, updatePolygon, setHighlightRoutePoint, setRecalcRequired, setRouteInfo,
                    closeOverlay
                )
            },
            onRouteInfoSelected = { setRouteInfo(it) }, createSnapshots = {}, dialogMode = RouteDialogMode.MapProvider.ordinal)
    }

    // Render WeatherScreen last among overlays to ensure it is on top
    if (showWeather) { // showWeather = false after map click
        cameraPosition.value.target?.let { cp ->
            WeatherScreen(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(vertical = 0.dp, horizontal = 16.dp),
                latitude = cp.latitude,
                longitude = cp.longitude
            )
        }
    }

    if (uiState.showMvtConfirmation) {
        AlertDialog(
            onDismissRequest = { dismissMvtConfirmation() },
            title = { Text(text = resources.getString(R.string.map_available)) },
            text = { Text(text = resources.getString(R.string.switch_to_map_, File(uiState.pendingMvtPath ?: "").name)) },
            confirmButton = {
                TextButton(onClick = { confirmMvtChange() }) {
                    Text(resources.getString(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { dismissMvtConfirmation() }) {
                    Text(resources.getString(android.R.string.cancel))
                }
            }
        )
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

enum class MapManagementAction {
    Nothing,
    Download,
    Import,
    Create
}
@Composable
fun MapManagementMenu(
    mapTypeKey: Int,
    latLng: GmsLatLng?,
    // action, mapName, mapTile, link
    finished: (MapManagementAction?, String?, GeoJsonUtils.Companion.Tile?, String?) -> Unit,
    onPopupSnackMsg: (String) -> Unit
) {
    val resources = LocalResources.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cloudMbtilesMap = remember { MagentaCloudMbtiles.getAllData() }
    val cloudMvtMap = remember { MagentaCloudMvt.getAllData() }
    val driveRasterMap = remember { DriveSharedLinks.Companion.RasterMaps().list }
    val driveMvtMap = remember { DriveSharedLinks.Companion.MvtRegions().list }
    if (mapTypeKey == MaptypeKey.Raster.ordinal) {
        val rasterTile = latLng?.let { pointToTile(it.longitude, it.latitude, 10.0) }
        val tileName = "tile_${rasterTile?.x}_${rasterTile?.y}_${rasterTile?.z}"
        Timber.i("MapManagementMenu rasterTile: $rasterTile")
        val isTypeSelected = remember { mutableIntStateOf(-1) }
        val maptypeEntries = remember { resources.getStringArray(R.array.pref_tilemaker_maptypes_entries) }
        val preferences = remember { getDefaultSharedPreferences(context) }
        val currentMapType = remember {
            preferences.getString(
                resources.getString(R.string.pref_tilemaker_maptype),
                Const.OUTDOOR
            )
        }
        isTypeSelected.intValue = maptypeEntries.indexOf(currentMapType)
        AlertDialog(
            onDismissRequest = { finished(Nothing, null, null, null) },
            dismissButton = {
                TextButton(onClick = { finished(Nothing, null, null, null) }) {
                    Text(text = stringResource(R.string.uc_close))
                }
            },
            confirmButton = {},
            title = {
                Column {
                    rasterTile?.let {
                        Text(
                            text = rasterTile.x.toString() + " " + rasterTile.y.toString() + " " + rasterTile.z.toString(),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        //Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
                        maptypeEntries.forEachIndexed { index, name ->
                            item {
                                FilterChip(
                                    selected = isTypeSelected.intValue == index,
                                    onClick = {
                                        isTypeSelected.intValue = index
                                        preferences.edit {
                                            putString(
                                                resources.getString(R.string.pref_tilemaker_maptype),
                                                name
                                            )
                                        }
                                    },
                                    label = { Text(name, style = MaterialTheme.typography.bodyMedium) },
                                    leadingIcon = if (isTypeSelected.intValue == index) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
            },
            text = {
                if (isTypeSelected.intValue != -1) {
                    val selectedType = maptypeEntries.getOrNull(isTypeSelected.intValue) ?: ""
                    val baseTileName = "${tileName}_$selectedType"
                    val fullTileFileName = "$baseTileName${Const.MBTILES_EXT}"
                    Timber.i("fullTileFileName: $fullTileFileName")
                    val mbTilesRootFolder = File(context.filesDir, Const.MBTILES_FOLDER)
                    val fullTileFile = File(mbTilesRootFolder, fullTileFileName)
                    if (fullTileFile.exists()) {
                        Text(text = stringResource(R.string._is_available, fullTileFileName),
                            modifier = Modifier.clickable {
                                scope.launch(Dispatchers.IO) {
                                    val currentSet = preferences.getStringSet(Const.PREF_MBTILES_FILEPATH_SET, emptySet())?.toMutableSet() ?: mutableSetOf()
                                    val b = currentSet.add(fullTileFile.path)
                                    if (b) {
                                        preferences.edit { putStringSet(Const.PREF_MBTILES_FILEPATH_SET, currentSet) }
                                        onPopupSnackMsg(
                                            resources.getString(
                                                R.string._activated,
                                                baseTileName
                                            )
                                        )
                                    } else
                                        onPopupSnackMsg(resources.getString(R.string._is_active, baseTileName))
                                    finished(Nothing, null, null, null)
                                }
                            }
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            cloudMbtilesMap[fullTileFileName]?.let { cloudUrl ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(
                                                R.string.download_,
                                                baseTileName
                                            )
                                        )
                                    },
                                    onClick = {
                                        finished(Download, fullTileFileName, rasterTile, cloudUrl)
                                    }
                                )
                            }
                            if (driveRasterMap[fullTileFileName] != null) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(
                                                R.string.import_,
                                                baseTileName
                                            )
                                        )
                                    },
                                    onClick = {
                                        finished(Import, tileName, rasterTile, null)
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    val isAvailableRemotely = cloudMbtilesMap.containsKey(fullTileFileName) ||
                                            driveRasterMap.containsKey(fullTileFileName)
                                    Text(
                                        text = stringResource(R.string.create_, baseTileName),
                                        color = if (isAvailableRemotely) MaterialTheme.colorScheme.outline
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    finished(Create, fullTileFileName, rasterTile, null)
                                }
                            )
                        }
                    }
                }
            }
        )

    }

    if (mapTypeKey == MaptypeKey.Mvt.ordinal) {
        val mvtTile = latLng?.let { pointToTile(it.longitude, it.latitude, 9.0) }
        val mvtname = mvtTile?.let { "mvt_${it.x}_${it.y}_${it.z}${Const.MBTILES_EXT}" }
        val driveUrl = driveMvtMap[mvtname?.replace(Const.MBTILES_EXT, "")]
        Timber.i("MapManagementMenu $mvtname")
        DropdownMenu(
            expanded = true,
            onDismissRequest = { finished(Nothing, null, null, null) }
        ) {
            cloudMvtMap[mvtname]?.let {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(
                                R.string.download_mvt,
                                mvtname?.replace(Const.MBTILES_EXT, "") ?: ""
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = { finished(Download, mvtname, mvtTile, it) }
                )
            }

            HorizontalDivider()

            if (driveUrl != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(
                                R.string.import_mvt_,
                                mvtname?.replace(Const.MBTILES_EXT, "") ?: ""
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        if (latLng != null) {
                            finished(Import, mvtname, mvtTile, null)
                        } else {
                            finished(Import, null, null, null)
                        }
                    }
                )
            } else {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.bbbike_create),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = { finished(Create, mvtname, mvtTile, null) }
                )
            }
        }
    }
}

private fun calculateGhRoute(
    context: Context,
    resources: Resources,
    startLat: Double?,
    startLon: Double?,
    stopPosition: LatLng?,
    isRoundTrip: Boolean,
    roundTripFactor: Float,
    setProgress: (String?) -> Unit,
    updatePolygon: (PolygonState) -> Unit,
    setLoadedRoute: (RouteEntity?) -> Unit,
    setHighlightRoutePoint: (Int) -> Unit,
    setRecalcRequired: (Boolean) -> Unit,
    setStopDragged: (Boolean) -> Unit,
    setSnackbar: (MainSnackbarData?) -> Unit,
    closeOverlay: () -> Unit
) {
    if (startLat != null && startLon != null && stopPosition.isNotNull()) {
        setProgress("${resources.getString(R.string.graphhopper_route_calculation)} ${GhHelper.getGhFilename(context)}")
        ghCalc(context, startLat, startLon, stopPosition!!.latitude, stopPosition.longitude,
            roundTrip = isRoundTrip, roundTripFactor) { lllh, name, _, _ ->
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
    } else setSnackbar(MainSnackbarData(resources.getString(R.string.no_stop_marker), null, null, null))
    closeOverlay()
}

/**
 * Handles the selection of a route file, including thumbnails, GeoJSON, and other route formats.
 * Extracts common logic to avoid duplication and ensures IO operations are performed on a background thread.
 */
private fun handleRouteFileSelection(
    routeFile: File,
    map: MapLibreMap?,
    context: Context,
    cameraMode: MutableState<Int>,
    cameraPosition: MutableState<CameraPosition>,
    liveSharedPreferences: LiveSharedPreferences,
    resources: Resources,
    scope: CoroutineScope,
    onRoutesGeoJsonStringChange: (String?) -> Unit,
    onPopupSnackMsg: (String?) -> Unit,
    setLoadedRoute: (RouteEntity?) -> Unit,
    updatePolygon: (PolygonState) -> Unit,
    setHighlightRoutePoint: (Int) -> Unit,
    setRecalcRequired: (Boolean) -> Unit,
    setRouteInfo: (File?) -> Unit,
    closeOverlay: () -> Unit,
    clearRouteInfo: Boolean = false
) {
    if (clearRouteInfo) setRouteInfo(null)
    closeOverlay()

    val extension = routeFile.extension.lowercase(Locale.ROOT)
    val jpgExt = Const.JPG_EXT.removePrefix(".")
    val geojsonExt = Const.GEOJSON_EXT.removePrefix(".")

    when (extension) {
        jpgExt if routeFile.path.contains(Const.THUMBNAILS) -> {
            setRouteInfo(routeFile)
        }
        geojsonExt -> {
            scope.launch(Dispatchers.IO) {
                val geojson = try { routeFile.readText() } catch (e: Exception) { "" }
                withContext(Dispatchers.Main) {
                    onRoutesGeoJsonStringChange(geojson)
                    map?.style?.let { style ->
                        (style.getSource("routes${Const.GEOJSON_EXT}") as? GeoJsonSource)?.setGeoJson(geojson)
                        (style.getSource("routes${Const.GEOJSON_EXT}${FeatureProperties.HITLAYER_TAG}") as? GeoJsonSource)?.setGeoJson(geojson)
                    }
                    liveSharedPreferences.preferences.edit {
                        putString(resources.getString(R.string.pref_routes_geojson_visibility), Property.VISIBLE)
                    }
                    onPopupSnackMsg(resources.getString(R.string.geojson_loaded_, routeFile.nameWithoutExtension))
                }
            }
        }
        else -> {
            loadRouteFromFile(routeFile, context, cameraMode, cameraPosition, onLoaded = { entity, state ->
                setLoadedRoute(entity)
                updatePolygon(state)
                setHighlightRoutePoint(-1)
                setRecalcRequired(false)
            }, loadFailed = {
                if (extension == jpgExt) {
                    val checkGeojson = Helpers.getImageDescriptionFromExif(routeFile)
                    if (checkGeojson?.startsWith(Const.GEOJSON_ROOT_FOLDER) == true) {
                        val pair = getGeojsonFromSnapshot(routeFile, map, liveSharedPreferences, context)
                        onPopupSnackMsg(pair.first)
                        onRoutesGeoJsonStringChange(pair.second)
                    }
                } else {
                    onPopupSnackMsg(resources.getString(R.string.route_load_failed_, routeFile.name))
                }
            })
        }
    }
}

@ComposePreview(showBackground = true)
@Composable
fun MapOverlayManagerPreview() {
    val context = LocalContext.current
    val liveSharedPreferences = remember { LiveSharedPreferences(getDefaultSharedPreferences(context)) }
    val cameraPosition = remember { mutableStateOf(CameraPosition()) }
    val cameraMode = remember { mutableIntStateOf(0) }
    val userLocation = remember { mutableStateOf(Location("dummy")) }
    val locationCircles = remember { mutableListOf<LatLng>() }

    RamaniTheme {
        CompositionLocalProvider(LocalLiveSharedPreferences provides liveSharedPreferences) {
            Box(modifier = Modifier.fillMaxSize()) {
                MapOverlayManagerContent(
                    uiState = MainUiState(progressMsg = "Loading maps..."),
                    map = null,
                    cameraPosition = cameraPosition,
                    cameraMode = cameraMode,
                    userLocation = userLocation,
                    prefMaptypeKey = 0,
                    onPopupSnackMsg = {},
                    onRoutesGeoJsonStringChange = {},
                    onRenderModeMapChange = {},
                    onUseCyclewayOverlaysChange = {},
                    onToggleButtonsBottomBarChange = {},
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
                    confirmMvtChange = {},
                    dismissMvtConfirmation = {},
                    setClipText = {},
                ) {}
            }
        }
    }
}
