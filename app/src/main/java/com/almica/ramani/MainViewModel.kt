package com.almica.ramani

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.almica.ramani.utils.GeoJsonUtils.Companion.pointToTile
import com.almica.ramani.Helpers.Companion.createMvtOfflineStyle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almica.ramani.MainSnackbarSelection.MapManager
import com.almica.ramani.compass.CompassViewModel
import com.almica.ramani.geojsonMaps.GeojsonMapRepository
import com.almica.ramani.locations.LocationRepository
import com.almica.ramani.pois.PoiEntity
import com.almica.ramani.pois.PoiRepository
import com.almica.ramani.routes.RouteEntity
import com.almica.ramani.utils.FeatureItem
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.GeoJsonUtils.Companion.getFeatureCollectionFromString
import com.almica.ramani.utils.GhHelper
import com.almica.ramani.utils.checkGeojsonMaps
import com.almica.ramani.utils.checkMvtMap
import com.almica.ramani.utils.getCenter
import com.almica.ramani.utils.getDistanceFromLllh
import com.almica.ramani.utils.getLayerVisibility
import com.almica.ramani.utils.ghCalc
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.utils.lllhToKmlString
import com.almica.ramani_lib.CameraPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import me.ibrahimsn.library.LiveSharedPreferences
import timber.log.Timber
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds

enum class OverlayType {
    NONE, PREFERENCES, GH_FOLDERS, VEHICLE_MENU, GEO_CODER, ROUTE_MONITOR, MAP_LONG_CLICK,
    RASTER_MAPS, MAP_TYPE, SAT_STATUS, LAYERS_CONTROL, BBBIKE_FUNCTIONS, HAIRCROSS, MAP_MENU,
    POI_DATABASE, LOCATIONS, ROUTE_FILES, ROUTE_FILES_REGION, ROUTE_FOLDERS, MVT_LIST,
    ADDITIONAL_MAPS, LOCATION_STATISTIC, ROUTE_SAVING, PDF_VIEWER, PDF_ROUTES, VALUE_PICKER
}

data class MainUiState(
    val activeOverlay: OverlayType = OverlayType.NONE,
    val progressMsg: String? = null,
    val popupSnackMsg: String? = null,
    val mainSnackbarData: MainSnackbarData? = null,
    val polygonState: PolygonState = PolygonState(arrayListOf(), "", 0.0),
    val loadedRouteEntity: RouteEntity? = null,
    val stopPosition: LatLng? = null,
    val mapManagerPosition: LatLng? = null,
    val showRouteInfo: File? = null,
    val highlightRoutePoint: Int = -1,
    val appRestartRequired: Boolean = false,
    val mapSwitchOption: String? = null,
    val gradientRouteEntity: RouteEntity? = null,
    val chartRouteEntity: RouteEntity? = null,
    val recalcRequired: Boolean = false,
    val stopDragged: Boolean = false,
    val mapFeatures: List<FeatureItem>? = null,
    val selectedFeatureItem: FeatureItem? = null,
    val pdfRoutes: List<RouteEntity>? = null,
    val routeMonitorState: Int = 0,
    val gpsValueState: GpsValue = GpsValue.Elevation,
    val dimmerState: Boolean = false,
    val hairCrossOffsetFraction: Float = 0f,
    val toggleGeojsonMapVisibility: String? = null,
    val logCount: Int = 0,
    val cameraMode: Int = CameraMode.TRACKING,
    val renderMode: Int = RenderMode.NORMAL,
    val useCyclewayOverlays: Boolean = false,
    val routesRegionFilter: String = "",
    val prefMaptypeKey: Int = 0,
    val toggleButtonsBottomBar: Boolean = false,
    val routesGeoJsonString: String? = null,
    val routeEntitiesMap: Map<String, ArrayList<LatLngH>>? = null,
    val cameraPosition: CameraPosition? = null,
    val mvtBounds: LatLngBounds? = null,
    val clipText: String? = null,
    val poiCategoryMap: Map<String, Pair<Int, Int>> = emptyMap(),
    val poiEntities: List<PoiEntity> = emptyList(),
    val mvtPath: String? = null,
    val styleUriToUse: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val liveSharedPreferences = LiveSharedPreferences(PreferenceManager.getDefaultSharedPreferences(application))
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val executor = Executors.newSingleThreadExecutor()
    private val locationRepository = LocationRepository.getInstance(application, executor)
    private val poiRepository = PoiRepository.getInstance(application, executor)
    private val geojsonMapRepository = GeojsonMapRepository.getInstance(application, executor)

    init {
        initializeMapType()
        loadPoiData()
    }

    fun loadPoiData() {
        viewModelScope.launch {
            val categories = Helpers.getPoiDrawableMap(getApplication())
            _uiState.update { it.copy(poiCategoryMap = categories) }
            poiRepository.getAllSimple(true) { pois ->
                Timber.i("loadPoiData pois.size: ${pois.size}")
                _uiState.update { it.copy(poiEntities = pois) }
            }
        }
    }

    private fun initializeMapType() {
        val context = getApplication<Application>()
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val mapType = preferences.getInt(Const.PREF_MAPTYPE_KEY, 0)
        _uiState.update { it.copy(prefMaptypeKey = mapType) }
    }

    fun addPoiEntity(poi: PoiEntity?) {
        poi?.let { newPoi ->
            Timber.i("addPoiEntity: ${newPoi.name} ${newPoi.category}")
            _uiState.update { state ->
                state.copy(poiEntities = state.poiEntities + newPoi)
            }
        }
    }

    fun setMvtPath(path: String?) {
        _uiState.update { it.copy(mvtPath = path) }
        path?.let {
            viewModelScope.launch {
                val mvtFile = File(it)
                if (mvtFile.exists()) {
                    val bounds = Helpers.getMvtBoundsFromMeta(mvtFile)
                    _uiState.update { it.copy(mvtBounds = bounds) }
                }
            }
        }
        recalculateStyle()
    }

    fun setClipText(text: String?) {
        _uiState.update { it.copy(clipText = text) }
    }

    fun onMapClick(context: Context, clickLatLng: LatLng, map: MapLibreMap?, cameraPosition: CameraPosition) {
        map?.let { m ->
            val pointF = m.projection.toScreenLocation(clickLatLng)
            val hitLayerId = context.getString(R.string.routes) + FeatureProperties.HITLAYER_TAG
            val features = m.queryRenderedFeatures(pointF, hitLayerId)

            if (features.isNotEmpty()) {
                val tappedFeature = features[0]
                tappedFeature.getStringProperty("name")?.let { name ->
                    setPopupSnackMsg(name)
                    val lllh = _uiState.value.routeEntitiesMap?.get(name)
                    if (!lllh.isNullOrEmpty()) {
                        val region = tappedFeature.getProperty("region").asString
                        val routeEntity = RouteEntity(UUID.randomUUID(), name, region, lllh[0].latitude, lllh[0].longitude, distance = lllh.getDistanceFromLllh(), kmlString = lllh.lllhToKmlString(name))
                        val dist = lllh.getDistanceFromLllh()
                        val newState = PolygonState(lllh, name, dist).apply {
                            polygonData = PolygonData(lllh, name, dist, false, null)
                            polygonData?.createPolygonMarkers(context, 0.0)
                        }
                        updatePolygon(newState)
                        setLoadedRoute(routeEntity)
                    }
                }
            }
            
            val rasterGridVisibility = getLayerVisibility(m, context.getString(R.string.raster_maps_grid))
            val geojsonGridVisibility = getLayerVisibility(m, context.getString(R.string.geojson_maps_grid) + FeatureProperties.LINES_TAG)
            val regionsGridVisibility = getLayerVisibility(m, context.getString(R.string.offline_regions_grid) + FeatureProperties.LINES_TAG)
            
            if (rasterGridVisibility) {
                setOverlay(OverlayType.RASTER_MAPS)
                setMapManagerPosition(clickLatLng)
            } else if (geojsonGridVisibility) {
                val tile13 = pointToTile(clickLatLng.longitude, clickLatLng.latitude, 13.0)
                val tile12 = pointToTile(clickLatLng.longitude, clickLatLng.latitude, 12.0)
                viewModelScope.launch {
                    val mapEntity12 = geojsonMapRepository.getGeojsonMapSimpleByName("geojsonTile_${tile12.x}_${tile12.y}_${tile12.z}")
                    val mapEntity13 = geojsonMapRepository.getGeojsonMapSimpleByName("geojsonTile_${tile13.x}_${tile13.y}_${tile13.z}")
                    if (mapEntity13.isNotNull()) setToggleGeojsonMapVisibility(mapEntity13?.name)
                    else if (mapEntity12.isNotNull()) setToggleGeojsonMapVisibility(mapEntity12?.name)
                    else setOverlay(OverlayType.BBBIKE_FUNCTIONS)
                }
            } else if (regionsGridVisibility) {
                val tile11 = pointToTile(clickLatLng.longitude, clickLatLng.latitude, 11.0)
                setMapManagerPosition(clickLatLng)
                setSnackbar(MainSnackbarData("Region: ${tile11.x}_${tile11.y}_${tile11.z}", context.getString(R.string.additional_maps), MapManager, null))
            }
        }
        
        if (_uiState.value.activeOverlay == OverlayType.LOCATION_STATISTIC) closeOverlay()
        else if (_uiState.value.gradientRouteEntity != null) setGradientRoute(null)
        else if (_uiState.value.chartRouteEntity != null) setChartRoute(null)

        if (_uiState.value.mvtPath != null) {
            setMapSwitchOption(checkMvtMap(LatLng(cameraPosition.target?.latitude ?: 0.0, cameraPosition.target?.longitude ?: 0.0), context))
        }

        if (_uiState.value.prefMaptypeKey == MaptypeKey.GeoJson.ordinal) {
            map?.let { m ->
                checkGeojsonMaps(m, context)
                m.triggerRepaint()
            }
        }
        closeOverlay()
    }

    fun onBackPress(reComposition: (Boolean, String?, LatLng?) -> Unit, finish: () -> Unit) {
        val state = _uiState.value
        when {
            state.activeOverlay == OverlayType.LOCATION_STATISTIC -> closeOverlay()
            state.gradientRouteEntity != null -> setGradientRoute(null)
            state.chartRouteEntity != null -> setChartRoute(null)
            state.polygonState.lllh.isNotEmpty() -> {
                setLoadedRoute(null)
                setRecalcRequired(false)
                state.polygonState.polygonData?.polygonMarkerDataList = listOf()
                updatePolygon(PolygonState(arrayListOf(), "", 0.0))
                setHighlightRoutePoint(-1)
                CompassViewModel.setRouteThumbnail(null)
                CompassViewModel.setDestination(state.stopPosition?.let { com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude) }, state.stopPosition?.altitude?.toInt())
                setSnackbar(MainSnackbarData(getApplication<Application>().getString(R.string.polygon_removed), null, null, null))
            }
            else -> {
                reComposition(false, state.mvtPath, state.cameraPosition?.target)
                finish()
            }
        }
    }

    fun setOverlay(overlay: OverlayType) {
        Timber.i("setOverlay: ${overlay.name}")
        _uiState.update { 
            it.copy(
                activeOverlay = overlay,
                hairCrossOffsetFraction = if (overlay == OverlayType.LOCATION_STATISTIC) 0.2f else it.hairCrossOffsetFraction
            ) 
        }
    }

    fun closeOverlay() {
        _uiState.update { 
            it.copy(
                activeOverlay = OverlayType.NONE, 
                hairCrossOffsetFraction = 0f,
                mapFeatures = null
            ) 
        }
    }

    fun setProgress(msg: String?) {
        _uiState.update { it.copy(progressMsg = msg) }
    }

    fun setSnackbar(data: MainSnackbarData?) {
        _uiState.update { it.copy(mainSnackbarData = data) }
        if (data != null) {
            viewModelScope.launch {
                delay(5000.milliseconds)
                _uiState.update { it.copy(mainSnackbarData = null) }
            }
        }
    }

    fun updatePolygon(state: PolygonState) {
        _uiState.update { it.copy(polygonState = state) }
    }

    fun setLoadedRoute(entity: RouteEntity?) {
        _uiState.update { it.copy(loadedRouteEntity = entity) }
    }

    fun setStopPosition(pos: LatLng?) {
        _uiState.update { it.copy(stopPosition = pos) }
    }

    fun setDimmer(enabled: Boolean) {
        _uiState.update { it.copy(dimmerState = enabled) }
    }

    fun setPdfRoutes(pdfRoutes: List<RouteEntity>?) {
        _uiState.update { it.copy(pdfRoutes = pdfRoutes) }
    }

    fun setMapFeatures(features: List<FeatureItem>?) {
        _uiState.update { it.copy(mapFeatures = features) }
    }
    
    fun setSelectedFeature(feature: FeatureItem?) {
        _uiState.update { it.copy(selectedFeatureItem = feature) }
    }

    fun setHighlightRoutePoint(index: Int) {
        _uiState.update { it.copy(highlightRoutePoint = index) }
    }
    
    fun setLogCount(count: Int) {
        //Timber.i("setLogCount: $count")
        _uiState.update { it.copy(logCount = count) }
    }

    fun onHairCrossClicked() {
        setOverlay(OverlayType.HAIRCROSS)
        setDimmer(false)
    }
    
    fun setGpsValueState(state: GpsValue) {
        _uiState.update { it.copy(gpsValueState = state) }
    }
    
    fun setToggleGeojsonMapVisibility(visibility: String?) {
        _uiState.update { it.copy(toggleGeojsonMapVisibility = visibility) }
    }

    fun setRouteInfo(file: File?) {
        _uiState.update { it.copy(showRouteInfo = file) }
    }
    
    fun setGradientRoute(entity: RouteEntity?) {
        Timber.i("setGradientRoute: ${entity?.name}")
        _uiState.update { 
            it.copy(
                gradientRouteEntity = entity,
                hairCrossOffsetFraction = if (entity != null) -0.2f else 0f
            ) 
        }
    }
    
    fun setChartRoute(entity: RouteEntity?) {
        _uiState.update { 
            it.copy(
                chartRouteEntity = entity,
                hairCrossOffsetFraction = if (entity != null) -0.2f else 0f
            ) 
        }
    }
    
    fun setMapManagerPosition(pos: LatLng?) {
        _uiState.update { it.copy(mapManagerPosition = pos) }
    }

    fun setAppRestartRequired(required: Boolean) {
        _uiState.update { it.copy(appRestartRequired = required) }
    }
    
    fun setRecalcRequired(required: Boolean) {
        _uiState.update { it.copy(recalcRequired = required) }
    }

    fun setStopDragged(dragged: Boolean) {
        _uiState.update { it.copy(stopDragged = dragged) }
    }

    fun setMapSwitchOption(option: String?) {
        _uiState.update { it.copy(mapSwitchOption = option) }
    }

    fun setRouteMonitorState(state: Int) {
        Timber.i("setRouteMonitorState: $state")
        _uiState.update { it.copy(routeMonitorState = state) }
    }

    fun setCameraMode(mode: Int) {
        _uiState.update { it.copy(cameraMode = mode) }
    }

    fun setRenderMode(mode: Int) {
        _uiState.update { it.copy(renderMode = mode) }
    }

    fun setUseCyclewayOverlays(enabled: Boolean) {
        _uiState.update { it.copy(useCyclewayOverlays = enabled) }
    }

    fun setRoutesRegionFilter(filter: String) {
        _uiState.update { it.copy(routesRegionFilter = filter) }
    }

    fun setPrefMaptypeKey(key: Int) {
        Timber.i("setPrefMaptypeKey: $key")
        _uiState.update { it.copy(prefMaptypeKey = key) }
        recalculateStyle()
    }

    private fun recalculateStyle() {
        val lastPos = _uiState.value.cameraPosition?.target
        if (lastPos != null) {
            calculateStyleUri(lastPos.latitude, lastPos.longitude)
        }
    }

    fun setToggleButtonsBottomBar(show: Boolean) {
        _uiState.update { it.copy(toggleButtonsBottomBar = show) }
        if (show) {
            viewModelScope.launch {
                delay(5000.milliseconds)
                _uiState.update { it.copy(toggleButtonsBottomBar = false) }
            }
        }
    }

    fun setRoutesGeoJsonString(json: String?) {
        _uiState.update { it.copy(routesGeoJsonString = json) }
        parseGeoJson(json)
    }

    private fun parseGeoJson(json: String?) {
        viewModelScope.launch {
            val features = json?.let { getFeatureCollectionFromString(it) }
            val mapGeojsonRoutes = mutableMapOf<String, ArrayList<LatLngH>>()
            features?.features()?.forEach { feature ->
                val name = feature.getStringProperty(FeatureProperties.NAME)
                val coordinates = GeoJsonUtils.getLllhFromGeometry(feature.geometry())
                name?.let { mapGeojsonRoutes[it] = coordinates }
            }
            _uiState.update { it.copy(routeEntitiesMap = mapGeojsonRoutes) }
            Timber.i("routeEntitiesMap: ${mapGeojsonRoutes.size}")
        }
    }

    fun setCameraPosition(pos: CameraPosition?) {
        _uiState.update { it.copy(cameraPosition = pos) }
    }
    
    fun setPopupSnackMsg(msg: String?) {
        _uiState.update { it.copy(popupSnackMsg = msg) }
        if (msg != null) {
            viewModelScope.launch {
                delay(5000.milliseconds)
                _uiState.update { it.copy(popupSnackMsg = null) }
            }
        }
    }
    
    fun handleSetStop(pos: LatLng) {
        setStopDragged(true)
        CompassViewModel.setDestination(com.google.android.gms.maps.model.LatLng(pos.latitude, pos.longitude), pos.altitude.toInt())
        setStopPosition(pos)
        setSnackbar(null)
    }

    fun handleRemoveStop() {
        setStopPosition(null)
        setSnackbar(null)
        CompassViewModel.setDestination(null, null)
        CompassViewModel.setRouteThumbnail(null)
    }

    fun handleSelectMvt(preferences: SharedPreferences, path: String?) {
        preferences.edit { putString(Const.PREF_MVT_FILEPATH, path) }
        setAppRestartRequired(true)
        setSnackbar(null)
        setToggleButtonsBottomBar(true)
    }

    override fun onCleared() {
        super.onCleared()
        liveSharedPreferences.unregister()
    }

    fun calculateStyleUri(latitude: Double, longitude: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val mapType = preferences.getInt(Const.PREF_MAPTYPE_KEY, 0)

            var styleUri: String? = null

            when (mapType) {
                MaptypeKey.Raster.ordinal -> {
                    styleUri = null
                }
                MaptypeKey.None.ordinal -> {
                    val dirMvtTiles = File(context.filesDir, Const.MVT_FOLDER)
                    dirMvtTiles.mkdir()
                    val styleFile = File(dirMvtTiles, Const.MAPTILER_REMOTE_STYLE_FILENAME)
                    styleUri = Uri.fromFile(styleFile).toString()
                }
                MaptypeKey.Mvt.ordinal -> {
                    val mvtTileMatch = pointToTile(longitude, latitude, 9.0)
                    val mvtFolder = File(context.filesDir, Const.MVT_FOLDER)
                    val mvtMatchingMap =
                        "${Const.MVT_PREFIX}${mvtTileMatch.x}_${mvtTileMatch.y}_${mvtTileMatch.z}"
                    val mvtMatchingFile = File(mvtFolder, mvtMatchingMap.plus(Const.MBTILES_EXT))
                    val mvtCurrentPath = preferences.getString(Const.PREF_MVT_FILEPATH, null)

                    if (mvtMatchingFile.exists() && mvtMatchingFile.path != mvtCurrentPath) {
                        preferences.edit { putString(Const.PREF_MVT_FILEPATH, mvtMatchingFile.path) }
                        styleUri = createMvtOfflineStyle(context, File(mvtMatchingFile.path))
                        mvtCurrentPath?.let {
                            Timber.i("mvt change: ${File(it).name} -> ${mvtMatchingFile.name}")
                        }
                    } else if (!mvtCurrentPath.isNullOrEmpty()) {
                        styleUri = createMvtOfflineStyle(context, File(mvtCurrentPath))
                    } else {
                        val emptyStyleFile = File(mvtFolder, Const.EMPTY_STYLE_FILENAME)
                        styleUri = Uri.fromFile(emptyStyleFile).toString()
                        Timber.i("styleUriToUse: $styleUri")
                    }
                }
                MaptypeKey.GeoJson.ordinal -> {
                    val rootFolder = context.filesDir
                    val mvtRootFolder = File(rootFolder, Const.MVT_FOLDER)
                    mvtRootFolder.mkdir()
                    val prefGeojsonFolderPath = preferences.getString(Const.PREF_GEOJSON_FILEPATH, "")
                    Timber.i("prefGeojsonFolderPath: $prefGeojsonFolderPath")

                    val styleFile = if (prefGeojsonFolderPath.isNullOrEmpty())
                        File(mvtRootFolder, Const.GEOJSON_OFFLINE_STYLE_FILENAME)
                    else File(mvtRootFolder, Const.GEOJSON_QGIS_STYLE_FILENAME)

                    styleUri = Uri.fromFile(styleFile).toString()
                    Timber.i("localStyleFile: $styleUri")
                }
            }
            _uiState.update { it.copy(styleUriToUse = styleUri) }
        }
    }


    fun calculateRoute(context: Context, startLat: Double, startLon: Double, stopLat: Double, stopLon: Double) {
        setProgress("${context.getString(R.string.graphhopper_route_calculation)} ${GhHelper.getGhFilename(context)}")
        ghCalc(
            context,
            startLat,
            startLon,
            stopLat,
            stopLon
        ) { lllh, name, success, _ ->
            setProgress(null)
            if (success) {
                val dist = lllh.getDistanceFromLllh()
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
                        startLat,
                        startLon,
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
    }
}
