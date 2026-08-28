package com.almica.ramani

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.almica.ramani.compass.CompassViewModel
import com.almica.ramani.googlemaps.MapUtils
import com.almica.ramani.pois.PoiEntity
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.utils.offsetYByPercent
import com.almica.ramani.utils.format
import com.almica.ramani_lib.CameraPosition
import com.almica.ramani_lib.Circle
import com.almica.ramani_lib.LocationRequestProperties
import com.almica.ramani_lib.LocationStyling
import com.almica.ramani_lib.MapLibre
import com.almica.ramani_lib.Margins
import com.almica.ramani_lib.Polyline
import com.almica.ramani_lib.SymbolVector
import com.almica.ramani_lib.UiSettings
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.gestures.MoveGestureDetector
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import timber.log.Timber
import java.util.ArrayList
import kotlin.math.pow

@Composable
fun MainMapView(
    uiState: MainUiState,
    map: MapLibreMap?,
    onMapChange: (MapLibreMap?) -> Unit,
    mapView: MapView,
    cameraPosition: MutableState<CameraPosition>,
    userLocation: MutableState<Location>,
    cameraMode: MutableIntState,
    renderMode: MutableIntState,
    localStyleBuilder: Style.Builder?,
    styleBuilderMaptypeRaster: Style.Builder,
    imageList: List<Pair<String, Int>>?,
    locationProperties: LocationRequestProperties,
    locationCircles: List<LatLng>,
    poiEntities: List<PoiEntity>,
    poiCategoryMap: Map<String, Pair<Int, Int>>,
    onMapClick: (LatLng) -> Unit,
    onMapLongClick: (LatLng) -> Unit,
    onStyleLoaded: (Style) -> Unit,
    onMapMove: (LatLng, Double) -> Unit,
    onMapReady: (MapLibreMap) -> Unit,
    onStopClick: (JsonElement?) -> Unit,
    onStopDragFinished: (LatLng) -> Unit,
    onMarkerClick: (Int, PolygonMarkerData, Double) -> Unit,
    onMarkerLongClick: (JsonElement?) -> Unit,
    onPoiClick: (PoiEntity) -> Unit,
    onLogCountChange: (Int) -> Unit,
    onHighlightRoutePoint: (Int) -> Unit,
    context: Context
) {
    val density = LocalDensity.current
    val uiSettings = remember(density) {
        UiSettings(
            compassMargins = Margins(top = with(density) { 100.dp.roundToPx() })
        )
    }
    
    val steps = uiState.polygonState.polygonData?.polygonMarkerDataList //uiState.polygonState.polygonData?.lllhKmSteps
    val target = cameraPosition.value.target
    val currentHighlight = uiState.highlightRoutePoint
    val onHighlightUpdated = rememberUpdatedState(onHighlightRoutePoint)

    LaunchedEffect(target, steps) {
        if (steps != null && target != null) {
            withContext(Dispatchers.Default) {
                val targetGms = com.google.android.gms.maps.model.LatLng(target.latitude, target.longitude)
                var highlightRoutePoint = steps.indices.minByOrNull { index ->
                    MapUtils.calculateHaversineDistance(targetGms, steps[index].latLngH.latLngGms)
                } ?: -1
                if (highlightRoutePoint != -1) highlightRoutePoint++
                if (highlightRoutePoint != currentHighlight) {
                    Timber.i("highlightRoutePoint: $highlightRoutePoint / ${steps.size}")
                    onHighlightUpdated.value(highlightRoutePoint)
                }
            }
        }
    }
    //Timber.i("localStyleBuilder: $localStyleBuilder")
    //Timber.i("styleBuilderMaptypeRaster: $styleBuilderMaptypeRaster")
    MapLibre(
        images = imageList,
        uiSettings = uiSettings,
        modifier = Modifier
            .fillMaxSize()
            .offsetYByPercent(uiState.hairCrossOffsetFraction),
        mapView = mapView,
        styleBuilder = localStyleBuilder ?: styleBuilderMaptypeRaster,
        cameraPosition = cameraPosition.value,
        locationRequestProperties = locationProperties,
        locationStyling = LocationStyling(enablePulse = true, pulseColor = 0xFFFFF200.toInt()),
        userLocation = userLocation,
        cameraMode = cameraMode,
        renderMode = renderMode,
        onStyleLoaded = onStyleLoaded,
        onMapClick = onMapClick,
        onMapLongClick = onMapLongClick
    ) {
        // Map Content (Circles, Polylines, etc.)
        uiState.stopPosition?.let { center ->
            Circle(center = center, color = "Red", radius = 12F, opacity = 0.6f, borderWidth = 1f, isDraggable = true,
                onClick = onStopClick,
                onDragFinished = { newCenter ->
                    onStopDragFinished(LatLng(newCenter.latitude, newCenter.longitude))
                })
        }

        uiState.polygonState.polygonData?.polygonMarkerDataList?.let { markerList ->
            val dynamicRadius = (cameraPosition.value.zoom?.toFloat() ?: 10f).pow(2) * 0.03f
            markerList.forEachIndexed { index, pmd ->
                if (cameraPosition.value.zoom!! > 9) {
                    val llhStep = uiState.polygonState.polygonData!!.lllhKmSteps?.get(index)
                    val distKm = markerList.last().distanceKm
                    val c = if (index == uiState.highlightRoutePoint) "#ea3680"
                    else if (pmd.gradient.absoluteValue > 9.0) "MAGENTA"
                    else if (pmd.gradient.absoluteValue > 6.0) "RED"
                    else if (pmd.gradient.absoluteValue > 3.0) "YELLOW"
                    else "GREEN"
                    
                    llhStep?.let { step ->
                        Circle(center = LatLng(step.latitude, step.longitude), color = c,
                            radius = if (index == uiState.highlightRoutePoint) 10F else dynamicRadius, opacity = 0.6f, borderWidth = 1f,
                            onClick = {
                                onMarkerClick(index, pmd, distKm)
                            },
                            onLongClick = onMarkerLongClick)
                    }
                }
            }
            val linePoints = List(uiState.polygonState.lllh.size) { LatLng(uiState.polygonState.lllh[it].latitude, uiState.polygonState.lllh[it].longitude) }
            Polyline(points = linePoints, color = "CYAN", lineWidth = 3f)
        }

        //Timber.i("locationCircles: ${locationCircles.size}")
        LaunchedEffect(locationCircles.size) {
            onLogCountChange(locationCircles.size)
        }

        locationCircles.forEachIndexed { index, ll ->
            val dynamicRadius = (cameraPosition.value.zoom?.toFloat() ?: 10f).pow(2) * 0.02f
            //Timber.i("dynamicRadius: $dynamicRadius")
            Circle(center = LatLng(ll.latitude, ll.longitude), color = "BLUE", radius = dynamicRadius,
                opacity = 0.5f, borderWidth = if (index % 20 == 0) 1f else 0F)
        }

        //Timber.i("poiEntities: ${poiEntities.size}")
        poiEntities.forEach { poi ->
            val dynamicRadius = (cameraPosition.value.zoom?.toFloat() ?: 10f) * 1.0f
            //Timber.i("dynamicRadius: $dynamicRadius")
            Circle(
                center = LatLng(poi.latitude, poi.longitude),
                color = "#70ff00",
                radius = dynamicRadius,
                opacity = if (poi.category.startsWith("city")) 0.0f else 0.4f,
                borderWidth = 1f,
                onClick = {
                    Timber.i("poi: ${poi.name} ${poi.category}")
                    onPoiClick(poi)
                })
            SymbolVector(
                context,
                size = 1f,
                center = LatLng(poi.latitude, poi.longitude),
                imageId = poiCategoryMap[poi.category]?.first
            )
        }
    }

    LaunchedEffect(map) {
        if (map == null) {
            mapView.getMapAsync { maplibreMap ->
                onMapChange(maplibreMap)
                maplibreMap.addOnMoveListener(object : MapLibreMap.OnMoveListener {
                    override fun onMoveBegin(p0: MoveGestureDetector) {
                        maplibreMap.cameraPosition.target?.let { onMapMove(it, maplibreMap.cameraPosition.zoom) }
                    }
                    override fun onMove(p0: MoveGestureDetector) {
                        maplibreMap.cameraPosition.target?.let { onMapMove(it, maplibreMap.cameraPosition.zoom) }
                    }
                    override fun onMoveEnd(p0: MoveGestureDetector) {
                        maplibreMap.cameraPosition.target?.let { onMapMove(it, maplibreMap.cameraPosition.zoom) }
                    }
                })
                maplibreMap.addOnCameraIdleListener {
                    maplibreMap.cameraPosition.target?.let { onMapMove(it, maplibreMap.cameraPosition.zoom) }
                }
                maplibreMap.addOnCameraMoveListener {
                    maplibreMap.cameraPosition.target?.let { onMapMove(it, maplibreMap.cameraPosition.zoom) }
                }
                maplibreMap.addOnScaleListener(object : MapLibreMap.OnScaleListener {
                    override fun onScaleBegin(p0: org.maplibre.android.gestures.StandardScaleGestureDetector) {}
                    override fun onScale(p0: org.maplibre.android.gestures.StandardScaleGestureDetector) {
                        maplibreMap.cameraPosition.target?.let { onMapMove(it, maplibreMap.cameraPosition.zoom) }
                    }
                    override fun onScaleEnd(p0: org.maplibre.android.gestures.StandardScaleGestureDetector) {}
                })
                onMapReady(maplibreMap)
            }
        }
    }
}
