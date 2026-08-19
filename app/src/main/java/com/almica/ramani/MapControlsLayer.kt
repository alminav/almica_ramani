package com.almica.ramani

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import com.almica.ramani.ui.theme.GridLabelBackground
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
    val showLocationStatistic = false // 19aug 2026 activeOverlay == OverlayType.LOCATION_STATISTIC
    val showRouteFiles = activeOverlay == OverlayType.ROUTE_FILES
    val showGhFolders = activeOverlay == OverlayType.GH_FOLDERS
    val showPoiDatabase = activeOverlay == OverlayType.POI_DATABASE
    val showAdditionalMapsManager = activeOverlay == OverlayType.ADDITIONAL_MAPS
    val showMvtList = activeOverlay == OverlayType.MVT_LIST
    val showRouteFolders = activeOverlay == OverlayType.ROUTE_FOLDERS
    val showPreferenceScreen = activeOverlay == OverlayType.PREFERENCES

    // The visibility of the grid layer can change independently of the map object instance.
    // We use a local state that is updated whenever the UI recomposes or when the map is initially set.
    var isGridVisible by remember(map) {
        mutableStateOf(map?.let { getLayerVisibility(it, LATLNG_GRID_LAYER) } ?: false)
    }

    // Periodically sync or sync on effect to ensure visibility reflects the actual map state
    LaunchedEffect(map, activeOverlay) {
        map?.let {
            isGridVisible = getLayerVisibility(it, LATLNG_GRID_LAYER)
        }
    }

    //Timber.i("activeOverlay: ${activeOverlay.name} isGridVisible: $isGridVisible")
    if (isGridVisible && mapPositionLatitude != 0.0) {
        GridCoordinateLabel(
            text = "${mapPositionLatitude.format(4)}° ${Const.UC_RIGHT_ARROW}",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 3.dp)
        )
    }

    if (isGridVisible && mapPositionLongitude != 0.0) {
        GridCoordinateLabel(
            text = "${Const.UC_UP_ARROW}\n${mapPositionLongitude.format(4)}°",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 0.dp),
            maxWidthFraction = 0.2f
        )
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
            false, // 19aug2026 showLocationStatistic,
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
            // cameraPosition.value update is handled by map listeners after easeCamera
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
}

@Composable
private fun GridCoordinateLabel(
    text: String,
    modifier: Modifier = Modifier,
    maxWidthFraction: Float = 0.25f
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth(maxWidthFraction)
            .background(color = GridLabelBackground)
            .padding(vertical = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center
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
