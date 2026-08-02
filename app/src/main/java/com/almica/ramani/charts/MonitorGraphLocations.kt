package com.almica.ramani.charts

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AreaChart
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.Const
import com.almica.ramani.GpsViewModel
import com.almica.ramani.Helpers
import com.almica.ramani.LatLngH
import com.almica.ramani.R
import com.almica.ramani.compass.CardinalDirection
import com.almica.ramani.compass.CompassScreen
import com.almica.ramani.compass.CompassViewModel
import com.almica.ramani.locations.LocationRepository
import com.almica.ramani.pois.PoiEntity
import com.almica.ramani.pois.PoiRepository
import com.almica.ramani.speedometer.SpeedView
import com.almica.ramani.speedometer.components.Section
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.KiThumbnailer.GPSPoint
import com.almica.ramani.utils.KiThumbnailer.drawRouteThumbnail
import com.almica.ramani.utils.format
import com.almica.ramani.utils.getDistanceFromLllh
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.utils.toBitmap
import com.almica.room.data.location.LocationEntity
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import timber.log.Timber
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 17apr2026
 * liveSharedPreferences replaced by GpsViewModel Observer for time
 */
// Source - https://stackoverflow.com/a/69685893
// Posted by Nasib
// Retrieved 2026-04-23, License - CC BY-SA 4.0

private const val logtag = "MonitorGraphLocations"
val df: DateFormat = DateFormat.getDateTimeInstance(
    DateFormat.DEFAULT,
    DateFormat.DEFAULT,
    Locale.GERMAN
)
enum class MonitorGraphType {
    ALTITUDE, SPEED, SPEEDOMETER, COMPASS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonitorGraphLocations(
    lllh: List<LatLngH>?,
    _plotResult: PlotResult?,
    startTime: Long,
    result: (LatLng?) -> Unit,
    map: (LatLng?) -> Unit,
    highlightRoutePoint: (Int) -> Unit
) {
    //val marginTopDp = TopAppBarDefaults.TopAppBarExpandedHeight.value
    var chartType by remember { mutableIntStateOf(MonitorGraphType.ALTITUDE.ordinal) }
    var titleValue by remember { mutableStateOf("") }
    var titleUnit by remember { mutableStateOf("") }
    //var locTime by remember { mutableLongStateOf(0L) }
    var latLng by remember { mutableStateOf<LatLng?>(null) }
    var altitude by remember { mutableStateOf<Double?>(null) }
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val posBmp = Icons.Outlined.Navigation.toBitmap(width = 30.dp, height = 30.dp, layoutDirection = LayoutDirection.Ltr)
    val locationRepository = LocationRepository.getInstance(context, Executors.newSingleThreadExecutor())
    val poiRepository = PoiRepository.getInstance(context, Executors.newSingleThreadExecutor())
    val poiCategoryMap = produceState(initialValue = mapOf()) {  value = Helpers.getPoiDrawableMap(context) }
    var locationTime: Long by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var locationSpeed: Float by remember { mutableFloatStateOf(0F) }
    var locationBearing: Float by remember { mutableFloatStateOf(0F) }
    var locationAltitude: Double by remember { mutableDoubleStateOf(0.0) }
    var plotResult by remember { mutableStateOf(PlotResult(
        lines = GraphDataPoints(arrayListOf(), arrayListOf(),
            arrayListOf(), arrayListOf()),
        distKM = 0F))}    //createPlotDataResult(locationRepository, startTime)
    if (_plotResult != null)
        plotResult = _plotResult
    else if (plotResult.lines.dataPointsAlti.isEmpty()) {
        plotResult = createPlotDataResult(locationRepository, startTime)
    }
    //var nearestRoutePoint by remember { mutableIntStateOf(-1) }
    //val lines by remember { mutableStateOf(plotResult.lines)}
    //var distKM by remember { mutableFloatStateOf(plotResult.distKM) }
    // 16apr2026
    val latitudeModel = GpsViewModel.latitude.collectAsState()
    val longitudeModel = GpsViewModel.longitude.collectAsState()
    val speedModel = GpsViewModel.speed.collectAsState()
    //Timber.i("speedModel.value: ${speedModel.value} ${GpsViewModel.time}")
    locationSpeed = speedModel.value.times(3.6f)
    //Timber.i("locationSpeed: $locationSpeed")
    val timeModel = GpsViewModel.time.collectAsState()
    locationTime = timeModel.value
    val bearingModel = GpsViewModel.bearing.collectAsState()
    locationBearing = bearingModel.value
    val directionCardinal by remember(locationBearing) {
        derivedStateOf { CardinalDirection.getDirectionFromAzimuthShort(locationBearing) }
    }
    val altitudeModel = GpsViewModel.altitude.collectAsState()
    locationAltitude = altitudeModel.value
    var scaffoldHeight by remember { mutableStateOf(240.dp) }
    val locEntity = LocationEntity(latitude = latitudeModel.value, longitude = longitudeModel.value,
        altitude = locationAltitude, bearing = locationBearing, speed = locationSpeed,
        time = locationTime)
    val dataPointResult =
        addDatapoint(
            pointsGps = plotResult.lines.dataPointsAlti as? MutableList<DataPointWithDist>,
            pointsSpeed = plotResult.lines.dataPointsSpeed as? MutableList<DataPointWithDist>,
            pointsSpeedAvg = plotResult.lines.dataPointsSpeedAvg as? MutableList<DataPointWithDist>,
            startTime,
            lastLatLng = latLng,
            locationEntity = locEntity,
            plotResult.distKM
        )
    latLng = LatLng(locEntity.latitude, locEntity.longitude)
    altitude = locationAltitude
    plotResult.distKM = dataPointResult.second

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.height(scaffoldHeight),
        //Modifier.padding(paddingValues).fillMaxHeight(heightFraction), //1f-heightFraction),
        topBar = {
            TopAppBar(
                navigationIcon = {
                },
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    Row {
                        IconButton(modifier = Modifier.weight(0.15f),
                            onClick = {
                                if (latLng != null)
                                    latLng?.let { result(it) }
                                else
                                    result(null)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go back home"
                            )
                        }
                        OutlinedButton(modifier = Modifier.weight(0.2f),
                            onClick = { chartType = MonitorGraphType.ALTITUDE.ordinal },
                            colors = ButtonColors(
                                contentColor = Color.Black, disabledContentColor = Color.Transparent,
                                containerColor = Color.Cyan .takeIf { chartType == MonitorGraphType.ALTITUDE.ordinal } ?: Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),

                            shape = MaterialTheme.shapes.small,
                            border = ButtonDefaults.outlinedButtonBorder()
                                .takeIf { chartType == MonitorGraphType.ALTITUDE.ordinal }
                        ) {
                            Column {
                                Box(modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
                                    Icon(Icons.Outlined.AreaChart, null, modifier = Modifier.scale(1.5f))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                                    text = stringResource(R.string.elevation_short),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(0.2f),
                            onClick = { chartType = MonitorGraphType.SPEED.ordinal },
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonColors(
                                contentColor = Color.Black,
                                disabledContentColor = Color.Transparent,
                                containerColor = Color.Cyan.takeIf { chartType == MonitorGraphType.SPEED.ordinal }
                                    ?: Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),

                            border = ButtonDefaults.outlinedButtonBorder()
                                .takeIf { chartType == MonitorGraphType.SPEED.ordinal },
                        ) {
                            Column {
                                Box(modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
                                    Icon(Icons.Outlined.AreaChart, null, modifier = Modifier.scale(1.5f))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                                    text = stringResource(R.string.speed_short),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(0.2f),
                            onClick = { chartType = MonitorGraphType.SPEEDOMETER.ordinal },
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonColors(
                                contentColor = Color.Black,
                                disabledContentColor = Color.Transparent,
                                containerColor = Color.Cyan.takeIf { chartType == MonitorGraphType.SPEEDOMETER.ordinal }
                                    ?: Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),
                            border = ButtonDefaults.outlinedButtonBorder()
                                .takeIf { chartType == MonitorGraphType.SPEEDOMETER.ordinal },
                        ) {
                            Column {
                                Box(modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
                                    Icon(Icons.Outlined.Speed, null, modifier = Modifier.scale(1.5f))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                                    text = stringResource(R.string.speed_short),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(0.2f),
                            onClick = { chartType = MonitorGraphType.COMPASS.ordinal },
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonColors(
                                contentColor = Color.Black,
                                disabledContentColor = Color.Transparent,
                                containerColor = Color.Cyan.takeIf { chartType == MonitorGraphType.COMPASS.ordinal }
                                    ?: Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),

                            border = ButtonDefaults.outlinedButtonBorder()
                                .takeIf { chartType == MonitorGraphType.COMPASS.ordinal },
                        ) {
                            Column {
                                Box(modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
                                    Icon(Icons.Outlined.Directions, null, modifier = Modifier.scale(1.5f))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                                    text = stringResource(R.string.directions_short),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(start = 4.dp).weight(0.25f).align(alignment = Alignment.CenterVertically)) {
                            Text(
                                modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                                text = titleValue,
                                //fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (chartType != MonitorGraphType.COMPASS.ordinal) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                                    text = titleUnit,
                                    //fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            //fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            )
        })
    { paddingValues ->
        if (plotResult.distKM > 0) {
            val currentBearing by animateFloatAsState(
                targetValue = locationBearing,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
            val currentSpeed by animateFloatAsState(
                targetValue = locationSpeed,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
            val sections: ImmutableList<Section> = persistentListOf(
                Section(0f, 1f, Color(0xFFFF0000.toInt()), width = 20.dp),
            )
            Column(modifier = Modifier.padding(start = 5.dp, end = 5.dp, bottom = 5.dp,
                top = paddingValues.calculateTopPadding())) {
                when(chartType) {
                    MonitorGraphType.ALTITUDE.ordinal -> {
                        scaffoldHeight = 240.dp
                        titleValue = locationAltitude.format(0)
                        titleUnit = "m"
                        val pointsAltiReversed = ArrayList<DataPointWithDist>()
                        plotResult.lines.dataPointsAlti.forEachIndexed { i, dp ->
                            pointsAltiReversed.add(
                                DataPointWithDist(
                                    plotResult.distKM - dp.x,
                                    dp.y,
                                    dp.geoLocation,
                                    System.currentTimeMillis(),
                                    0.0
                                )
                            )
                        }
                        val pointsSrtmReversed = ArrayList<DataPointWithDist>()
                        plotResult.lines.dataPointsSrtm.forEachIndexed { i, dp ->
                            pointsSrtmReversed.add(
                                DataPointWithDist(
                                    plotResult.distKM - dp.x,
                                    dp.y,
                                    dp.geoLocation,
                                    System.currentTimeMillis(),
                                    0.0
                                )
                            )
                        }

                        LineGraphAlti(
                            Pair(pointsAltiReversed, pointsSrtmReversed),
                            inverseXAxis = true,
                            //Pair(plotResult.lines.first, plotResult.lines.second),
                            onSelect = { alti, ll, time ->
                                titleValue = alti
                                map(ll)
                            })
                    }
                    MonitorGraphType.SPEED.ordinal -> {
                        scaffoldHeight = 240.dp
                        titleValue = currentSpeed.format(0)
                        titleUnit = "KmH"
                        val pointsSpeedReversed = ArrayList<DataPointWithDist>()
                        val pointsSpeedAvgReversed = ArrayList<DataPointWithDist>()
                        plotResult.lines.dataPointsSpeed.forEachIndexed { i, dp ->
                            pointsSpeedReversed.add(
                                DataPointWithDist(
                                    plotResult.distKM - dp.x,
                                    dp.y,
                                    dp.geoLocation,
                                    System.currentTimeMillis(),
                                    0.0
                                )
                            )
                        }
                        plotResult.lines.dataPointsSpeedAvg.forEachIndexed { i, dp ->
                            pointsSpeedAvgReversed.add(
                                DataPointWithDist(
                                    plotResult.distKM - dp.x,
                                    dp.y,
                                    dp.geoLocation,
                                    System.currentTimeMillis(),
                                    0.0
                                )
                            )
                        }
                        LineGraphSpeed(
                            Pair(pointsSpeedReversed, pointsSpeedAvgReversed),
                            inverseXAxis = true,
                            onSelect = { speed, ll, _ ->
                                titleValue = speed
                                //locTime = time
                                map(ll)
                            })
                    }
                    MonitorGraphType.COMPASS.ordinal -> {
                        val destinationLatLng: LatLng? = CompassViewModel.destination.collectAsState().value
                        scaffoldHeight = if (lllh.isNullOrEmpty() && destinationLatLng == null) 240.dp else 300.dp
                        titleValue = stringResource(id = directionCardinal.dirName)//currentBearing.format(0)
                        titleUnit = ""
                        if (lllh.isNullOrEmpty())
                            CompassViewModel.setCurrentLocation(latLng, altitude?.toInt())
                        Box(modifier = Modifier.fillMaxWidth().align(alignment = Alignment.CenterHorizontally)) {
                            lllh?.let {
                                // lllh in km steps, no further simplification necessary
                                val i = nearestRoutePoint(locationBearing.toDouble(), latLng!!, lllh)
                                var poiEntity: PoiEntity? = null
                                if (lllh.isNotEmpty()) {
                                    var poiBmp : Bitmap? = null
                                    latLng?.let { currentPos ->
                                        poiRepository.getNearestPoi(currentPos.latitude, currentPos.longitude, 20000.0) { nearest ->
                                            if (nearest != null) {
                                                //Timber.i("Nearest: ${nearest.name} (${nearest.category})")
                                                val imageId = poiCategoryMap.value[nearest.category]?.first
                                                imageId?.let { id ->
                                                    poiBmp = Helpers.getBitmapFromVectorDrawable(context, id)
                                                            //BitmapFactory.decodeResource(resources, id)
                                                    //Timber.i("poiBmp: $poiBmp")
                                                    poiEntity = nearest
                                                    //Timber.i("nearest: $nearest")
                                                }?: Timber.e("imageId is null")
                                            }
                                            //val flagBmp = Icons.Filled.Flag.toBitmap(width = 24.dp, height = 24.dp, layoutDirection = LayoutDirection.Ltr)
                                            val b = createRouteBitmap(lllh, LatLng(latitudeModel.value, longitudeModel.value),
                                                (160 * density).toInt(),
                                                (160 * density).toInt(), posBmp, i, currentBearing,
                                                if (poiBmp.isNotNull()) poiEntity else null, poiBmp )
                                            CompassViewModel.setHaircrossThumbnail(b)
                                            CompassViewModel.setDestination(lllh[i].latLngGms, lllh[i].altitude.toInt())
                                            CompassViewModel.setDistance((lllh as ArrayList).getDistanceFromLllh())
                                            highlightRoutePoint(i)
                                            CompassViewModel.setCurrentLocation(latLng, altitude?.toInt())
                                            CompassViewModel.setNearestPoiName(poiEntity?.name)
                                            CompassViewModel.setpoiBmp(poiBmp)
                                        }
                                    }

                                    CompassScreen(
                                        currentBearing.toInt(),
                                        if (it.isNotEmpty()) i.toDouble() / lllh.size.toDouble() else 0.0
                                    )
                                } else {
                                    //Text(text = stringResource(R.string.no_route_loaded))
                                    CompassViewModel.setCurrentLocation(latLng, altitude?.toInt())
                                    CompassScreen(currentBearing.toInt(), 0.0)
                                }
                            } ?: CompassScreen(currentBearing.toInt(), 0.0)
                            //DrawLine()
                        }
                    }
                    MonitorGraphType.SPEEDOMETER.ordinal -> {
                        //scaffoldHeight = 260.dp
                        titleValue = currentSpeed.format(0)
                        titleUnit = "KmH"
                        val sharedPreferences = getDefaultSharedPreferences(context)
                        val s1s2 = sharedPreferences.getString(
                            stringResource(R.string.setting_locomotion),
                            Const.DEFAULT_LOCOMOTION
                        )
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            SpeedView(
                                maxSpeed = when (s1s2) {
                                    "0.0" -> 10f
                                    "0.1" -> 10f
                                    "1.1" -> 50f
                                    "1.0" -> 50f
                                    "2.1" -> 200f
                                    "2.0" -> 200f
                                    "3.1" -> 1200f
                                    "3.0" -> 1200f
                                    else -> 100f
                                }, marksCount = when (s1s2) {
                                    "0.0" -> 4
                                    "0.1" -> 4
                                    "1.1" -> 4
                                    "1.0" -> 4
                                    "2.1" -> 9
                                    "2.0" -> 9
                                    "3.1" -> 9
                                    "3.0" -> 9
                                    else -> 9
                                },
                                modifier = Modifier.padding(top = 4.dp)
                                    .size(150.dp).align(alignment = Alignment.Center),
                                unitUnderSpeed = true,
                                sections = sections,
                                speed = currentSpeed
                            )
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingValues.calculateTopPadding())
                ) {
                    Text(
                        text = stringResource(R.string.no_locations),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Elevation chart for locations
 */
fun createPlotDataResult(
    locationRepository: LocationRepository,
    startTime: Long
): PlotResult {
    val date = Date(startTime)
    val parts  = df.format(date).split(" ").toMutableList()
    val time = parts.lastOrNull()
    //Timber.i("startTime: $time")
    val locationsEntities = locationRepository.getLocationsAscFromTime(startTime) //locationRepository.getLocationsAsc()
    val pointsGps = ArrayList<DataPointWithDist>()
    val pointsSrtm = ArrayList<DataPointWithDist>()
    val pointsSpeed = ArrayList<DataPointWithDist>()
    val pointsSpeedAvg = ArrayList<DataPointWithDist>()
    var tileName = ""
    val lllh = ArrayList<LatLngH>()
    for (locationEntity in locationsEntities) {
        val latLng = LatLngH(locationEntity.latitude, locationEntity.longitude)
        latLng.let {
            lllh.add(LatLngH(it.latitude, it.longitude, 0.0))
            if (tileName.isEmpty()) {
                tileName = Helpers.getTileName(it.latitude, it.longitude)
            }
        }
    }
    Timber.i("tileName $tileName")
    var distKM = 0F
    var lastLatLng: LatLng? = null
    //for (locationEntity in locationsEntities) {
    locationsEntities.forEachIndexed { index, locationEntity ->
        val result =
            addDatapoint(
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
    for (i in 0..<listLatLng.size) {
        val newDist = SphericalUtil.computeDistanceBetween(listLatLng[i].latLngGms, latLong).toInt()
        var headingPoint = SphericalUtil.computeHeading(latLong, listLatLng[i].latLngGms)
        if (headingPoint < 0) headingPoint += 360
        //Timber.i("$i headingPoint: $headingPoint heading: $heading")
        if (headingPoint > heading - 90 && headingPoint < heading + 90
            && newDist < prevDist) {
            prevDist = newDist
            routePointer = i
        }
    }
    //Timber.i("nearestRoutePoint route_pointer: $routePointer")
    return routePointer
}

data class PlotResult(val lines: GraphDataPoints, var distKM: Float)
data class GraphDataPoints(val dataPointsAlti: List<DataPointWithDist>,
                           val dataPointsSrtm: List<DataPointWithDist>,
                           val dataPointsSpeed: List<DataPointWithDist>,
                           val dataPointsSpeedAvg: List<DataPointWithDist>)

fun addDatapoint(
    pointsGps: MutableList<DataPointWithDist>?,
    pointsSpeed: MutableList<DataPointWithDist>?,
    pointsSpeedAvg: MutableList<DataPointWithDist>?,
    startTime: Long,
    lastLatLng: LatLng?,
    locationEntity: LocationEntity,
    distKM: Float?
): Triple<LatLng, Float, Double> {
    val date = Date(startTime)
    val parts  = df.format(date).split(" ").toMutableList()
    val time = parts.lastOrNull()
    //Timber.i("startTime: $time")
    //Timber.i("pointsGps: ${pointsGps?.size}")
    var newDistKm = 0.0F
    if (distKM != null && !distKM.isNaN())
        newDistKm = distKM
    val latLng = LatLng(locationEntity.latitude, locationEntity.longitude)
    if (lastLatLng != null) {
        newDistKm = newDistKm.plus(
            0.001F * SphericalUtil.computeDistanceBetween(
                LatLng(
                    locationEntity.latitude,
                    locationEntity.longitude
                ), lastLatLng
            ).toFloat()
        )
    }
    //Timber.i("newDistKm $newDistKm")
    val dpAltiGps = DataPointWithDist(
        newDistKm,
        locationEntity.altitude.toFloat(), latLng, time = locationEntity.time,
        distKm = newDistKm.toDouble()
    )
    pointsGps?.add(dpAltiGps)
    //    pointsGps?.add(0, dpAltiGps)
    // val dpAltiSrtm = DataPoint(newDistKm, srtmAlti.toFloat(), latLng, locationEntity.time)
    // pointsSrtm?.add(dpAltiSrtm)
//    pointsSrtm?.add(0, dpAltiSrtm)

    val dpSpeed = DataPointWithDist(newDistKm, locationEntity.speed, latLng, locationEntity.time, newDistKm.toDouble())
    pointsSpeed?.add(dpSpeed)
    val deltaTime = locationEntity.time - startTime
    val dpSpeedAvgY = if (deltaTime == 0L) 0f else 3600000f * newDistKm / deltaTime
    val dpSpeedAvg = DataPointWithDist(newDistKm, dpSpeedAvgY, latLng, locationEntity.time, newDistKm.toDouble())
    pointsSpeedAvg?.add((dpSpeedAvg))
    //Timber.i("deltaTime:$deltaTime dpSpeed:${dpSpeed.y} dpSpeedAvg:${dpSpeedAvgY}")

    return Triple(latLng, newDistKm, locationEntity.altitude)
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
    //val lllh40 = lllh.simplifyToTargetCount(40)
    val gpsRoute = lllh.map { GPSPoint(it.latitude, it.longitude, it.altitude) }
    val bitmap = createBitmap(width, height)
    val canvas = android.graphics.Canvas(bitmap)

    // Optional: Hintergrundfarbe setzen (z.B. transparent oder leichtes Grau)
    canvas.drawColor("#F5F5F5".toColorInt())

    drawRouteThumbnail(canvas, width, height, gpsRoute,
        GPSPoint(currentLatLng.latitude, currentLatLng.longitude, 0.0),
        posBmp, routePoint, currentBearing, poiEntity, poiBmp)
    return bitmap
}

@Preview(showBackground = true)
@Composable
internal fun MonitorGraphLocationsPreview() {
    val samplePoints = arrayListOf(
        DataPointWithDist(0f, 100f, LatLng(0.0, 0.0), 0L, 0.0),
        DataPointWithDist(1f, 110f, LatLng(0.01, 0.01), 1000L, 1.0),
        DataPointWithDist(2f, 105f, LatLng(0.02, 0.02), 2000L, 2.0)
    )
    val plotResult = PlotResult(
        lines = GraphDataPoints(
            dataPointsAlti = samplePoints,
            dataPointsSrtm = samplePoints,
            dataPointsSpeed = samplePoints,
            dataPointsSpeedAvg = samplePoints
        ),
        distKM = 2f
    )
    RamaniTheme {
        MonitorGraphLocations(null,
            _plotResult = plotResult,
            startTime = System.currentTimeMillis(),
            result = {},
            map = {},
            highlightRoutePoint = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty State")
@Composable
internal fun MonitorGraphLocationsEmptyPreview() {
    RamaniTheme {
        MonitorGraphLocations(null,
            _plotResult = PlotResult(GraphDataPoints(emptyList(), emptyList(), emptyList(), emptyList()), 0f),
            startTime = System.currentTimeMillis(),
            result = {},
            map = {},
            highlightRoutePoint = {}
        )
    }
}

@Composable
fun DrawLine(modifierWeight: Float, xFraction: Float = 0.5f, yFraction: Float = 0.5f) {
    Column(
        modifier = Modifier.fillMaxWidth(modifierWeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Creating a Canvas for drawing a straight
        // line between two points x and y
        Canvas(modifier = Modifier.fillMaxSize()) {

            // Fetching width and height for
            // setting start x and end y
            val canvasWidth = size.width
            val canvasHeight = size.height

            // drawing a line between start(x,y) and end(x,y)
            drawLine(
                start = Offset(x = canvasWidth, y = yFraction * canvasHeight),
                end = Offset(x = 0f, y = yFraction * canvasHeight),
                color = Color.Red,
                strokeWidth = 2F
            )
            drawLine(
                start = Offset(x = xFraction * canvasWidth, y = 0f),
                end = Offset(x = xFraction* canvasWidth, y = canvasHeight),
                color = Color.Red,
                strokeWidth = 2F
            )
        }
    }
}