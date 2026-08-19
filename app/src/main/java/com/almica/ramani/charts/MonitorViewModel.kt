package com.almica.ramani.charts

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almica.ramani.GpsViewModel
import com.almica.ramani.Helpers
import com.almica.ramani.LatLngH
import com.almica.ramani.compass.CardinalDirection
import com.almica.ramani.locations.LocationRepository
import com.almica.ramani.pois.PoiEntity
import com.almica.ramani.pois.PoiRepository
import com.almica.ramani.utils.format
import com.almica.room.data.location.LocationEntity
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.Executors
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.almica.ramani.utils.KiThumbnailer.GPSPoint
import com.almica.ramani.utils.KiThumbnailer.drawRouteThumbnail
import com.almica.ramani.utils.isNotNull

enum class MonitorGraphType {
    ALTITUDE, SPEED, SPEEDOMETER, COMPASS, COMPASS_THUMBNAIL
}

data class PlotResult(val lines: GraphDataPoints, var distKM: Float)
data class GraphDataPoints(val dataPointsAlti: List<DataPointWithDist>,
                           val dataPointsSrtm: List<DataPointWithDist>,
                           val dataPointsSpeed: List<DataPointWithDist>,
                           val dataPointsSpeedAvg: List<DataPointWithDist>)

data class MonitorUiState(
    val chartType: MonitorGraphType = MonitorGraphType.ALTITUDE,
    val plotResult: PlotResult = PlotResult(
        GraphDataPoints(emptyList(), emptyList(), emptyList(), emptyList()),
        0f
    ),
    val locationBearing: Float = 0f,
    val locationSpeed: Float = 0f,
    val locationAltitude: Double = 0.0,
    val locationTime: Long = 0L,
    val latLng: LatLng? = null,
    val poiCategoryMap: Map<String, Pair<Int, Int>> = emptyMap(),
    val titleValue: String = "",
    val titleUnit: String = "",
    val scaffoldHeight: Dp = 240.dp,
    val nearestPoi: PoiEntity? = null,
    val poiBmp: Bitmap? = null
)

class MonitorViewModel(application: Application) : AndroidViewModel(application) {
    private val executor = Executors.newSingleThreadExecutor()
    private val locationRepository = LocationRepository.getInstance(application, executor)
    private val poiRepository = PoiRepository.getInstance(application, executor)

    private val _uiState = MutableStateFlow(MonitorUiState())
    val uiState: StateFlow<MonitorUiState> = _uiState.asStateFlow()

    private var startTime: Long = 0L

    init {
        loadPoiCategoryMap()
        observeGpsData()
    }

    private fun loadPoiCategoryMap() {
        viewModelScope.launch {
            val categories = Helpers.getPoiDrawableMap(getApplication())
            _uiState.update { it.copy(poiCategoryMap = categories) }
        }
    }

    private fun observeGpsData() {
        viewModelScope.launch {
            launch {
                GpsViewModel.latitude.collectLatest { lat ->
                    _uiState.update { it.copy(latLng = LatLng(lat, it.latLng?.longitude ?: 0.0)) }
                    updateDataPoint()
                }
            }
            launch {
                GpsViewModel.longitude.collectLatest { lng ->
                    _uiState.update { it.copy(latLng = LatLng(it.latLng?.latitude ?: 0.0, lng)) }
                    updateDataPoint()
                }
            }
            launch {
                GpsViewModel.speed.collectLatest { speed ->
                    _uiState.update { it.copy(locationSpeed = speed * 3.6f) }
                    updateDataPoint()
                }
            }
            launch {
                GpsViewModel.bearing.collectLatest { bearing ->
                    _uiState.update { it.copy(locationBearing = bearing) }
                    updateDataPoint()
                }
            }
            launch {
                GpsViewModel.altitude.collectLatest { altitude ->
                    _uiState.update { it.copy(locationAltitude = altitude) }
                    updateDataPoint()
                }
            }
            launch {
                GpsViewModel.time.collectLatest { time ->
                    _uiState.update { it.copy(locationTime = time) }
                    updateDataPoint()
                }
            }
        }
    }

    fun initialize(startTime: Long, initialPlotResult: PlotResult?) {
        this.startTime = startTime
        if (initialPlotResult != null) {
            _uiState.update { it.copy(plotResult = initialPlotResult) }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val result = createPlotDataResult(locationRepository, startTime)
                _uiState.update { it.copy(plotResult = result) }
            }
        }
    }

    fun setChartType(type: MonitorGraphType) {
        _uiState.update { it.copy(chartType = type) }
        updateTitleAndUnit()
    }

    fun setScaffoldHeight(height: Dp) {
        _uiState.update { it.copy(scaffoldHeight = height) }
    }

    fun setTitleValue(value: String) {
        _uiState.update { it.copy(titleValue = value) }
    }

    private fun updateTitleAndUnit() {
        val state = _uiState.value
        val (value, unit) = when (state.chartType) {
            MonitorGraphType.ALTITUDE -> Pair(state.locationAltitude.format(0), "m")
            MonitorGraphType.SPEED -> Pair(state.locationSpeed.format(0), "KmH")
            MonitorGraphType.SPEEDOMETER -> Pair(state.locationSpeed.format(0), "KmH")
            MonitorGraphType.COMPASS,
            MonitorGraphType.COMPASS_THUMBNAIL -> {
                val cardinal = CardinalDirection.getDirectionFromAzimuthShort(state.locationBearing)
                Pair(getApplication<Application>().getString(cardinal.dirName), "")
            }
        }
        _uiState.update { it.copy(titleValue = value, titleUnit = unit) }
    }

    private fun updateDataPoint() {
        val state = _uiState.value
        if (startTime == 0L || state.latLng == null) return

        val locEntity = LocationEntity(
            latitude = state.latLng.latitude,
            longitude = state.latLng.longitude,
            altitude = state.locationAltitude,
            bearing = state.locationBearing,
            speed = state.locationSpeed,
            time = state.locationTime
        )

        // Only update if time is valid and progressing
        // Note: In real app, we might want more complex filtering
        
        val currentLines = state.plotResult.lines
        val newPointsAlti = currentLines.dataPointsAlti.toMutableList()
        val newPointsSpeed = currentLines.dataPointsSpeed.toMutableList()
        val newPointsSpeedAvg = currentLines.dataPointsSpeedAvg.toMutableList()

        val lastLatLng = if (newPointsAlti.isNotEmpty()) newPointsAlti.last().geoLocation else null

        val result = addDatapointInternal(
            newPointsAlti,
            newPointsSpeed,
            newPointsSpeedAvg,
            startTime,
            lastLatLng,
            locEntity,
            state.plotResult.distKM
        )

        _uiState.update {
            it.copy(
                plotResult = PlotResult(
                    GraphDataPoints(newPointsAlti, currentLines.dataPointsSrtm, newPointsSpeed, newPointsSpeedAvg),
                    result.second
                )
            )
        }
        
        // Auto-update title if not manually overridden by selection
        updateTitleAndUnit()
    }

    fun updateNearestPoi(currentPos: LatLng, callback: (PoiEntity?, Bitmap?) -> Unit) {
        poiRepository.getNearestPoi(currentPos.latitude, currentPos.longitude, 20000.0) { nearest ->
            var poiBmp: Bitmap? = null
            if (nearest != null) {
                val imageId = _uiState.value.poiCategoryMap[nearest.category]?.first
                imageId?.let { id ->
                    poiBmp = Helpers.getBitmapFromVectorDrawable(getApplication(), id)
                }
            }
            _uiState.update { it.copy(nearestPoi = nearest, poiBmp = poiBmp) }
            callback(nearest, poiBmp)
        }
    }
}

private fun addDatapointInternal(
    pointsGps: MutableList<DataPointWithDist>,
    pointsSpeed: MutableList<DataPointWithDist>,
    pointsSpeedAvg: MutableList<DataPointWithDist>,
    startTime: Long,
    lastLatLng: LatLng?,
    locationEntity: LocationEntity,
    distKM: Float
): Triple<LatLng, Float, Double> {
    var newDistKm = distKM
    val latLng = LatLng(locationEntity.latitude, locationEntity.longitude)
    if (lastLatLng != null) {
        newDistKm += 0.001F * SphericalUtil.computeDistanceBetween(latLng, lastLatLng).toFloat()
    }

    val dpAltiGps = DataPointWithDist(
        newDistKm,
        locationEntity.altitude.toFloat(), latLng, time = locationEntity.time,
        distKm = newDistKm.toDouble()
    )
    pointsGps.add(dpAltiGps)

    val dpSpeed = DataPointWithDist(newDistKm, locationEntity.speed, latLng, locationEntity.time, newDistKm.toDouble())
    pointsSpeed.add(dpSpeed)

    val deltaTime = locationEntity.time - startTime
    val dpSpeedAvgY = if (deltaTime <= 0L) 0f else 3600000f * newDistKm / deltaTime
    val dpSpeedAvg = DataPointWithDist(newDistKm, dpSpeedAvgY, latLng, locationEntity.time, newDistKm.toDouble())
    pointsSpeedAvg.add(dpSpeedAvg)

    return Triple(latLng, newDistKm, locationEntity.altitude)
}

fun createPlotDataResult(
    locationRepository: LocationRepository,
    startTime: Long
): PlotResult {
    val locationsEntities = locationRepository.getLocationsAscFromTime(startTime)
    val pointsGps = ArrayList<DataPointWithDist>()
    val pointsSrtm = ArrayList<DataPointWithDist>()
    val pointsSpeed = ArrayList<DataPointWithDist>()
    val pointsSpeedAvg = ArrayList<DataPointWithDist>()
    
    var distKM = 0F
    var lastLatLng: LatLng? = null
    
    locationsEntities.forEach { locationEntity ->
        val result = addDatapointInternal(
            pointsGps, pointsSpeed, pointsSpeedAvg, startTime,
            lastLatLng,
            locationEntity,
            distKM
        )
        lastLatLng = result.first
        distKM = result.second
    }
    
    val lines = GraphDataPoints(pointsGps, pointsSrtm, pointsSpeed, pointsSpeedAvg)
    return PlotResult(lines, distKM)
}

fun nearestRoutePoint(
    heading: Double,
    latLong: LatLng,
    listLatLng: List<LatLngH>
): Int {
    var prevDist = Int.MAX_VALUE
    var routePointer = 0
    for (i in listLatLng.indices) {
        val newDist = SphericalUtil.computeDistanceBetween(listLatLng[i].latLngGms, latLong).toInt()
        var headingPoint = SphericalUtil.computeHeading(latLong, listLatLng[i].latLngGms)
        if (headingPoint < 0) headingPoint += 360
        if (headingPoint > heading - 90 && headingPoint < heading + 90 && newDist < prevDist) {
            prevDist = newDist
            routePointer = i
        }
    }
    return routePointer
}

fun createRouteBitmap(
    lllh: List<LatLngH>,
    currentLatLng: LatLng,
    width: Int,
    height: Int,
    posBmp: Bitmap,
    routePoint: Int,
    currentBearing: Float,
    poiEntity: PoiEntity?,
    poiBmp: Bitmap?
): Bitmap {
    val gpsRoute = lllh.map { GPSPoint(it.latitude, it.longitude, it.altitude) }
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    canvas.drawColor("#F5F5F5".toColorInt())

    drawRouteThumbnail(canvas, width, height, gpsRoute,
        GPSPoint(currentLatLng.latitude, currentLatLng.longitude, 0.0),
        posBmp, routePoint, currentBearing, poiEntity, poiBmp)
    return bitmap
}
