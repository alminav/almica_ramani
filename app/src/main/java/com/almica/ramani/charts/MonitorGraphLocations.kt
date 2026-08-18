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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almica.ramani.utils.toBitmap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import timber.log.Timber
import java.util.concurrent.Executors

private const val logtag = "MonitorGraphLocations"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonitorGraphLocations(
    lllh: List<LatLngH>?,
    _plotResult: PlotResult?,
    startTime: Long,
    result: (LatLng?) -> Unit,
    map: (LatLng?) -> Unit,
    highlightRoutePoint: (Int) -> Unit,
    viewModel: MonitorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(startTime, _plotResult) {
        viewModel.initialize(startTime, _plotResult)
    }

    MonitorGraphLocationsContent(
        uiState = uiState,
        lllh = lllh,
        onChartTypeChange = { viewModel.setChartType(it) },
        onScaffoldHeightChange = { viewModel.setScaffoldHeight(it) },
        onTitleValueChange = { viewModel.setTitleValue(it) },
        updateNearestPoi = { pos, callback -> viewModel.updateNearestPoi(pos, callback) },
        result = result,
        map = map,
        highlightRoutePoint = highlightRoutePoint
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonitorGraphLocationsContent(
    uiState: MonitorUiState,
    lllh: List<LatLngH>?,
    onChartTypeChange: (MonitorGraphType) -> Unit,
    onScaffoldHeightChange: (Dp) -> Unit,
    onTitleValueChange: (String) -> Unit,
    updateNearestPoi: (LatLng, (PoiEntity?, Bitmap?) -> Unit) -> Unit,
    result: (LatLng?) -> Unit,
    map: (LatLng?) -> Unit,
    highlightRoutePoint: (Int) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val posBmp = Icons.Outlined.Navigation.toBitmap(width = 30.dp, height = 30.dp, layoutDirection = LayoutDirection.Ltr)

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.height(uiState.scaffoldHeight),
        topBar = {
            MonitorTopBar(
                chartType = uiState.chartType,
                titleValue = uiState.titleValue,
                titleUnit = uiState.titleUnit,
                onChartTypeChange = onChartTypeChange,
                onBack = {
                    if (uiState.latLng != null)
                        uiState.latLng?.let { result(it) }
                    else
                        result(null)
                }
            )
        }
    ) { paddingValues ->
        if (uiState.plotResult.distKM > 0) {
            val currentBearing by animateFloatAsState(
                targetValue = uiState.locationBearing,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                label = "bearing"
            )
            val currentSpeed by animateFloatAsState(
                targetValue = uiState.locationSpeed,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                label = "speed"
            )

            Column(
                modifier = Modifier
                    .padding(
                        start = 5.dp, end = 5.dp, bottom = 5.dp,
                        top = paddingValues.calculateTopPadding()
                    )
            ) {
                when (uiState.chartType) {
                    MonitorGraphType.ALTITUDE -> {
                        onScaffoldHeightChange(240.dp)
                        AltitudeGraphSection(
                            plotResult = uiState.plotResult,
                            onSelect = { alti, ll ->
                                onTitleValueChange(alti)
                                map(ll)
                            }
                        )
                    }

                    MonitorGraphType.SPEED -> {
                        onScaffoldHeightChange(240.dp)
                        SpeedGraphSection(
                            plotResult = uiState.plotResult,
                            onSelect = { speed, ll ->
                                onTitleValueChange(speed)
                                map(ll)
                            }
                        )
                    }

                    MonitorGraphType.COMPASS -> {
                        val destinationLatLng by CompassViewModel.destination.collectAsState()
                        onScaffoldHeightChange(if (lllh.isNullOrEmpty() && destinationLatLng == null) 240.dp else 300.dp)

                        CompassSectionContent(
                            lllh = lllh,
                            currentLatLng = uiState.latLng,
                            currentAltitude = uiState.locationAltitude,
                            currentBearing = currentBearing,
                            posBmp = posBmp,
                            density = density,
                            updateNearestPoi = updateNearestPoi,
                            highlightRoutePoint = highlightRoutePoint
                        )
                    }

                    MonitorGraphType.SPEEDOMETER -> {
                        SpeedometerSection(currentSpeed = currentSpeed)
                    }
                }
            }
        } else {
            EmptyStateCard(paddingValues)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonitorTopBar(
    chartType: MonitorGraphType,
    titleValue: String,
    titleUnit: String,
    onChartTypeChange: (MonitorGraphType) -> Unit,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {},
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        actions = {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(modifier = Modifier.weight(0.15f), onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back home"
                    )
                }
                ChartTypeButton(
                    modifier = Modifier.weight(0.2f),
                    type = MonitorGraphType.ALTITUDE,
                    isSelected = chartType == MonitorGraphType.ALTITUDE,
                    icon = Icons.Outlined.AreaChart,
                    label = stringResource(R.string.elevation_short),
                    onClick = { onChartTypeChange(MonitorGraphType.ALTITUDE) }
                )
                ChartTypeButton(
                    modifier = Modifier.weight(0.2f),
                    type = MonitorGraphType.SPEED,
                    isSelected = chartType == MonitorGraphType.SPEED,
                    icon = Icons.Outlined.AreaChart,
                    label = stringResource(R.string.speed_short),
                    onClick = { onChartTypeChange(MonitorGraphType.SPEED) }
                )
                ChartTypeButton(
                    modifier = Modifier.weight(0.2f),
                    type = MonitorGraphType.SPEEDOMETER,
                    isSelected = chartType == MonitorGraphType.SPEEDOMETER,
                    icon = Icons.Outlined.Speed,
                    label = stringResource(R.string.speed_short),
                    onClick = { onChartTypeChange(MonitorGraphType.SPEEDOMETER) }
                )
                ChartTypeButton(
                    modifier = Modifier.weight(0.2f),
                    type = MonitorGraphType.COMPASS,
                    isSelected = chartType == MonitorGraphType.COMPASS,
                    icon = Icons.Outlined.Directions,
                    label = stringResource(R.string.directions_short),
                    onClick = { onChartTypeChange(MonitorGraphType.COMPASS) }
                )
                Column(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .weight(0.25f)
                        .align(Alignment.CenterVertically)
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        text = titleValue,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    if (chartType != MonitorGraphType.COMPASS) {
                        Text(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            text = titleUnit,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun ChartTypeButton(
    modifier: Modifier = Modifier,
    type: MonitorGraphType,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = modifier,
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.Black,
            containerColor = if (isSelected) Color.Cyan else Color.Transparent
        ),
        shape = MaterialTheme.shapes.small,
        border = if (isSelected) ButtonDefaults.outlinedButtonBorder() else null
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.scale(1.5f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AltitudeGraphSection(
    plotResult: PlotResult,
    onSelect: (String, LatLng?) -> Unit
) {
    val pointsAltiReversed = remember(plotResult.lines.dataPointsAlti, plotResult.distKM) {
        plotResult.lines.dataPointsAlti.map { dp ->
            DataPointWithDist(plotResult.distKM - dp.x, dp.y, dp.geoLocation, dp.time, dp.distKm)
        }
    }
    val pointsSrtmReversed = remember(plotResult.lines.dataPointsSrtm, plotResult.distKM) {
        plotResult.lines.dataPointsSrtm.map { dp ->
            DataPointWithDist(plotResult.distKM - dp.x, dp.y, dp.geoLocation, dp.time, dp.distKm)
        }
    }

    LineGraphAlti(
        lines = Pair(pointsAltiReversed, pointsSrtmReversed),
        inverseXAxis = true,
        onSelect = { alti, ll, _ -> onSelect(alti, ll) }
    )
}

@Composable
private fun SpeedGraphSection(
    plotResult: PlotResult,
    onSelect: (String, LatLng?) -> Unit
) {
    val pointsSpeedReversed = remember(plotResult.lines.dataPointsSpeed, plotResult.distKM) {
        plotResult.lines.dataPointsSpeed.map { dp ->
            DataPointWithDist(plotResult.distKM - dp.x, dp.y, dp.geoLocation, dp.time, dp.distKm)
        }
    }
    val pointsSpeedAvgReversed = remember(plotResult.lines.dataPointsSpeedAvg, plotResult.distKM) {
        plotResult.lines.dataPointsSpeedAvg.map { dp ->
            DataPointWithDist(plotResult.distKM - dp.x, dp.y, dp.geoLocation, dp.time, dp.distKm)
        }
    }

    LineGraphSpeed(
        lines = Pair(pointsSpeedReversed, pointsSpeedAvgReversed),
        inverseXAxis = true,
        onSelect = { speed, ll, _ -> onSelect(speed, ll) }
    )
}

@Composable
private fun CompassSectionContent(
    lllh: List<LatLngH>?,
    currentLatLng: LatLng?,
    currentAltitude: Double,
    currentBearing: Float,
    posBmp: Bitmap,
    density: Float,
    updateNearestPoi: (LatLng, (PoiEntity?, Bitmap?) -> Unit) -> Unit,
    highlightRoutePoint: (Int) -> Unit
) {
    if (currentLatLng == null) return

    LaunchedEffect(currentLatLng, lllh, currentBearing) {
        if (!lllh.isNullOrEmpty()) {
            val i = nearestRoutePoint(currentBearing.toDouble(), currentLatLng, lllh)
            updateNearestPoi(currentLatLng) { poiEntity, poiBmp ->
                val b = createRouteBitmap(
                    lllh, currentLatLng,
                    (160 * density).toInt(),
                    (160 * density).toInt(), posBmp, i, currentBearing,
                    poiEntity, poiBmp
                )
                CompassViewModel.setHaircrossThumbnail(b)
                CompassViewModel.setDestination(lllh[i].latLngGms, lllh[i].altitude.toInt())
                CompassViewModel.setDistance(lllh.getDistanceFromLllh())
                highlightRoutePoint(i)
                CompassViewModel.setCurrentLocation(currentLatLng, currentAltitude.toInt())
                CompassViewModel.setNearestPoiName(poiEntity?.name)
                CompassViewModel.setpoiBmp(poiBmp)
            }
        } else {
            CompassViewModel.setCurrentLocation(currentLatLng, currentAltitude.toInt())
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        val progress = if (!lllh.isNullOrEmpty()) {
            val i = nearestRoutePoint(currentBearing.toDouble(), currentLatLng, lllh)
            i.toDouble() / lllh.size.toDouble()
        } else 0.0
        
        CompassScreen(currentBearing.toInt(), progress)
    }
}

@Composable
private fun SpeedometerSection(currentSpeed: Float) {
    val context = LocalContext.current
    val sharedPreferences = remember { getDefaultSharedPreferences(context) }
    val locomotion = sharedPreferences.getString(
        stringResource(R.string.setting_locomotion),
        Const.DEFAULT_LOCOMOTION
    )

    val maxSpeed = when (locomotion) {
        "0.0", "0.1" -> 10f
        "1.1", "1.0" -> 50f
        "2.1", "2.0" -> 200f
        "3.1", "3.0" -> 1200f
        else -> 100f
    }
    val marksCount = when (locomotion) {
        "0.0", "0.1", "1.1", "1.0" -> 4
        else -> 9
    }

    val sections: ImmutableList<Section> = remember {
        persistentListOf(Section(0f, 1f, Color.Red, width = 20.dp))
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        SpeedView(
            maxSpeed = maxSpeed,
            marksCount = marksCount,
            modifier = Modifier
                .padding(top = 4.dp)
                .size(150.dp),
            unitUnderSpeed = true,
            sections = sections,
            speed = currentSpeed
        )
    }
}

@Composable
private fun EmptyStateCard(paddingValues: PaddingValues) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = paddingValues.calculateTopPadding(), bottom = 16.dp)
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

@Composable
fun DrawLine(modifierWeight: Float, xFraction: Float = 0.5f, yFraction: Float = 0.5f) {
    Column(
        modifier = Modifier.fillMaxWidth(modifierWeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            drawLine(
                start = Offset(x = canvasWidth, y = yFraction * canvasHeight),
                end = Offset(x = 0f, y = yFraction * canvasHeight),
                color = Color.Red,
                strokeWidth = 2F
            )
            drawLine(
                start = Offset(x = xFraction * canvasWidth, y = 0f),
                end = Offset(x = xFraction * canvasWidth, y = canvasHeight),
                color = Color.Red,
                strokeWidth = 2F
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun MonitorGraphLocationsPreview() {
    val samplePoints = listOf(
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
    val uiState = MonitorUiState(
        plotResult = plotResult,
        locationAltitude = 100.0,
        locationSpeed = 10.0f,
        locationBearing = 45f,
        latLng = LatLng(0.0, 0.0),
        titleValue = "100",
        titleUnit = "m"
    )
    RamaniTheme {
        MonitorGraphLocationsContent(
            uiState = uiState,
            lllh = null,
            onChartTypeChange = {},
            onScaffoldHeightChange = {},
            onTitleValueChange = {},
            updateNearestPoi = { _, _ -> },
            result = {},
            map = {},
            highlightRoutePoint = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty State")
@Composable
internal fun MonitorGraphLocationsEmptyPreview() {
    val uiState = MonitorUiState()
    RamaniTheme {
        MonitorGraphLocationsContent(
            uiState = uiState,
            lllh = null,
            onChartTypeChange = {},
            onScaffoldHeightChange = {},
            onTitleValueChange = {},
            updateNearestPoi = { _, _ -> },
            result = {},
            map = {},
            highlightRoutePoint = {}
        )
    }
}

