package com.almica.ramani

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.almica.ramani.routes.RouteEntity
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.format
import com.almica.ramani.utils.getLayerVisibility
import com.almica.ramani_lib.CameraPosition
import me.ibrahimsn.library.LiveSharedPreferences
import org.maplibre.android.maps.MapLibreMap
import android.content.Context
import com.almica.ramani.Const.Companion.LATLNG_GRID_LAYER
import timber.log.Timber

@Composable
fun BoxScope.MapControlsLayer(
    viewModel: MainViewModel?,
    uiState: MainUiState,
    map: MapLibreMap?,
    cameraPosition: MutableState<CameraPosition>,
    mapPositionLatitude: Double,
    mapPositionLongitude: Double,
    mapPositionZoom: MutableState<Double?>,
    toggleButtonsBottomBar: Boolean,
    onToggleButtonsBottomBarChange: (Boolean) -> Unit,
    renderModeMap: String,
    onRenderModeMapChange: (String) -> Unit,
    onRecalc: () -> Unit,
    onRestart: () -> Unit
) {
    //Timber.i("uiState.logCount: ${uiState.logCount}")
    MapControlsLayerContent(
        activeOverlay = uiState.activeOverlay,
        gpsValueState = uiState.gpsValueState,
        logCount = uiState.logCount,
        recalcRequired = uiState.recalcRequired,
        stopDragged = uiState.stopDragged,
        mapSwitchOption = uiState.mapSwitchOption,
        loadedRouteEntity = uiState.loadedRouteEntity,
        gradientRouteEntity = uiState.gradientRouteEntity,
        chartRouteEntity = uiState.chartRouteEntity,
        appRestartRequired = uiState.appRestartRequired,
        prefMaptypeKey = uiState.prefMaptypeKey,
        map = map,
        cameraPosition = cameraPosition,
        mapPositionLatitude = mapPositionLatitude,
        mapPositionLongitude = mapPositionLongitude,
        mapPositionZoom = mapPositionZoom,
        toggleButtonsBottomBar = toggleButtonsBottomBar,
        renderModeMap = renderModeMap,
        setOverlay = { viewModel?.setOverlay(it); Timber.i("setOverlay: ${it.name}") },
        setGpsValueState = { viewModel?.setGpsValueState(it) },
        setSnackbar = { viewModel?.setSnackbar(it) },
        setDimmer = { viewModel?.setDimmer(it) },
        onToggleButtonsBottomBarChange = onToggleButtonsBottomBarChange,
        onRenderModeMapChange = onRenderModeMapChange,
        onRecalc = onRecalc,
        onRestart = onRestart
    )
}

@Composable
fun BoxScope.MapControlsLayerContent(
    activeOverlay: OverlayType,
    gpsValueState: GpsValue,
    logCount: Int,
    recalcRequired: Boolean,
    stopDragged: Boolean,
    mapSwitchOption: String?,
    loadedRouteEntity: RouteEntity?,
    gradientRouteEntity: RouteEntity?,
    chartRouteEntity: RouteEntity?,
    appRestartRequired: Boolean,
    prefMaptypeKey: Int,
    map: MapLibreMap?,
    cameraPosition: MutableState<CameraPosition>,
    mapPositionLatitude: Double,
    mapPositionLongitude: Double,
    mapPositionZoom: MutableState<Double?>,
    toggleButtonsBottomBar: Boolean,
    renderModeMap: String,
    setOverlay: (OverlayType) -> Unit,
    setGpsValueState: (GpsValue) -> Unit,
    setSnackbar: (MainSnackbarData?) -> Unit,
    setDimmer: (Boolean) -> Unit,
    onToggleButtonsBottomBarChange: (Boolean) -> Unit,
    onRenderModeMapChange: (String) -> Unit,
    onRecalc: () -> Unit,
    onRestart: () -> Unit
) {
    val showLocationStatistic = activeOverlay == OverlayType.LOCATION_STATISTIC
    val showRouteFiles = activeOverlay == OverlayType.ROUTE_FILES
    val showGhFolders = activeOverlay == OverlayType.GH_FOLDERS
    val showPoiDatabase = activeOverlay == OverlayType.POI_DATABASE
    val showAdditionalMapsManager = activeOverlay == OverlayType.ADDITIONAL_MAPS
    val showMvtList = activeOverlay == OverlayType.MVT_LIST
    val showRouteFolders = activeOverlay == OverlayType.ROUTE_FOLDERS
    val showPreferenceScreen = activeOverlay == OverlayType.PREFERENCES

    if (map != null && mapPositionLatitude != 0.0 && getLayerVisibility(map, LATLNG_GRID_LAYER)) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${mapPositionLatitude.format(4)}° ${Const.UC_RIGHT_ARROW}",
                Modifier
                    .fillMaxWidth(0.25f)
                    .background(color = Color(0xA0CCCCCC)),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }

    if (map != null && mapPositionLongitude != 0.0 && getLayerVisibility(map, LATLNG_GRID_LAYER)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(alignment = Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${Const.UC_UP_ARROW}\n${mapPositionLongitude.format(4)}°",
                Modifier
                    .fillMaxWidth(0.2f)
                    .background(color = Color(0xA0CCCCCC)),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }

    Box(
        modifier = if (showLocationStatistic)
            Modifier
                .align(alignment = Alignment.BottomCenter)
                .padding(bottom = 4.dp)
        else Modifier
    ) {
        MainTopButtonBar(
            setMapLongClickMenu = { setOverlay(if (it) OverlayType.MAP_LONG_CLICK else OverlayType.NONE) },
            setLocationStatistic = { setOverlay(if (it) OverlayType.LOCATION_STATISTIC else OverlayType.NONE) },
            setGpsValueState = { setGpsValueState(it) },
            showLocationStatistic,
            gpsValueState,
            logCount
        )
    }

    MainMvtMapSwitchButton(
        mapSwitchOption,
        mainSnackbarData = { setSnackbar(it) }
    )

    AnimatedVisibility(
        visible = recalcRequired || stopDragged,
        enter = fadeIn(animationSpec = tween(durationMillis = 800)),
        exit = fadeOut(animationSpec = tween(durationMillis = 800)),
        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
    ) {
        MainRecalcButton {
            onRecalc()
        }
    }

    MainRestartButton(
        map,
        appRestartRequired,
        restart = onRestart
    )

    MainZoomButtons(
        visibility = showRouteFiles.not()
            .and(gradientRouteEntity == null)
            .and(chartRouteEntity == null)
            .and(!showGhFolders)
            .and(!showPoiDatabase)
            .and(!showAdditionalMapsManager)
            .and(!showMvtList)
            .and(!showRouteFolders)
            .and(!showPreferenceScreen)
            .and(mapPositionZoom.value != null),
        mapPositionZoom = mapPositionZoom,
        map = map,
        cameraPosition = cameraPosition,
        setZoom = { newZoom ->
            mapPositionZoom.value = newZoom
            cameraPosition.value = CameraPosition(cameraPosition.value).apply {
                this.motionType = com.almica.ramani_lib.CameraMotionType.INSTANT
                this.zoom = newZoom
                this.animationDurationMs = 0
            }
            if (prefMaptypeKey == MaptypeKey.GeoJson.ordinal) {
                // map?.let { checkGeojsonMaps(it, context) }
            }
        }
    )

    MainCameraModeSwitchButton(
        toggleButtonsBottomBar,
        cameraPosition,
        renderModeMap,
        setSatStatus = { setOverlay(if (it) OverlayType.SAT_STATUS else OverlayType.NONE) },
        setButtonsBottomBar = onToggleButtonsBottomBarChange,
        setRenderMode = onRenderModeMapChange
    )
    //Timber.i("logCount: $logCount")
    MainBottomButtonBar(
        showRouteFiles.not().and(toggleButtonsBottomBar)
            .and(gradientRouteEntity == null)
            .and(chartRouteEntity == null).and(!showGhFolders)
            .and(!showLocationStatistic)
            .and(!showPoiDatabase).and(!showAdditionalMapsManager)
            .and(!showMvtList)
            .and(!showPreferenceScreen),
        loadedRouteEntity,
        logCount,
        setRouteMonitorMenu = { setOverlay(if (it) OverlayType.ROUTE_MONITOR else OverlayType.NONE) },
        setHaircrossMenu = { setOverlay(if (it) OverlayType.HAIRCROSS else OverlayType.NONE) },
        setMapMenu = { setOverlay(if (it) OverlayType.MAP_MENU else OverlayType.NONE) },
        setPoiDatabase = { setOverlay(if (it) OverlayType.POI_DATABASE else OverlayType.NONE) },
        setRouteFiles = { setOverlay(if (it) OverlayType.ROUTE_FILES else OverlayType.NONE) },
        setLocationsMenu = { setOverlay(if (it) OverlayType.LOCATIONS else OverlayType.NONE) },
        dimmerState = { setDimmer(it) }
    )
}

@Preview(showBackground = true)
@Composable
fun MapControlsLayerPreview() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
    val liveSharedPreferences = LiveSharedPreferences(sharedPreferences)
    val cameraPosition = remember { mutableStateOf(CameraPosition()) }
    val mapPositionZoom = remember { mutableStateOf<Double?>(13.0) }

    RamaniTheme {
        CompositionLocalProvider(LocalLiveSharedPreferences provides liveSharedPreferences) {
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                MapControlsLayerContent(
                    activeOverlay = OverlayType.NONE,
                    gpsValueState = GpsValue.Velocity,
                    logCount = 5,
                    recalcRequired = false,
                    stopDragged = false,
                    mapSwitchOption = null,
                    loadedRouteEntity = null,
                    gradientRouteEntity = null,
                    chartRouteEntity = null,
                    appRestartRequired = false,
                    prefMaptypeKey = 0,
                    map = null, // Can't easily mock MapLibreMap for preview
                    cameraPosition = cameraPosition,
                    mapPositionLatitude = 48.0,
                    mapPositionLongitude = 11.0,
                    mapPositionZoom = mapPositionZoom,
                    toggleButtonsBottomBar = true,
                    renderModeMap = "normal",
                    setOverlay = {},
                    setGpsValueState = {},
                    setSnackbar = {},
                    setDimmer = {},
                    onToggleButtonsBottomBarChange = {},
                    onRenderModeMapChange = {},
                    onRecalc = {},
                    onRestart = {}
                )
            }
        }
    }
}
