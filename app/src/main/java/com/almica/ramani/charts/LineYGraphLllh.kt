package com.almica.ramani.charts

import android.annotation.SuppressLint
import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.axis.AxisData
import co.yml.charts.common.extensions.formatNoPrecision
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import com.almica.ramani.GpsViewModel
import com.almica.ramani.Helpers
import com.almica.ramani.LatLngH
import com.almica.ramani.R
import com.almica.ramani.utils.RouteSmoothingUtil
import com.almica.ramani.utils.RouteSmoothingUtil.simplifyToTargetCount
import com.almica.ramani.utils.formatDistM
import com.almica.ramani.utils.getDistanceFromLllh
import com.almica.ramani.utils.offsetYByPercent
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 17apr2026
 * liveSharedPreferences replaced by GpsViewModel Observer for time
 */
private const val logtag = "LineYGraphLllh"
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
internal fun LineYGraphLllh(
    lllhSource: java.util.ArrayList<LatLngH>?,
    name: String,
    offsetYByPercent: Float,
    finish: (LatLng?) -> Unit,
    map: (LatLng?) -> Unit,
    homeIcon: ImageVector
) {
    //val context = LocalContext.current
    //val lllhSmooth = lllhSource?.let { RouteSmoothingUtil.smoothRoute(it) as ArrayList<LatLngH>? }
    val dist = lllhSource?.getDistanceFromLllh()
    val lllh = (lllhSource as List<LatLngH>).simplifyToTargetCount(42) as ArrayList<LatLngH>?
    var routePointer by remember { mutableIntStateOf(-1) }
    var viewModel: RouteEleChartViewModel by remember {
        mutableStateOf(
            RouteEleChartViewModel(
                lllh
            )
        )
    }
    val latModel = GpsViewModel.latitude.collectAsState()
    val lonModel = GpsViewModel.longitude.collectAsState()
    latModel.value?.let { latitude ->
        lonModel.value?.let { longitude ->
            val recordedLatLng = LatLng(latitude, longitude)
            val nearestPoint =
                recordedLatLng.let { Helpers.nearestPointOnPath(it, lllh) }
            routePointer = nearestPoint.route_pointer ?: -1
            Timber.i("routePointer: $routePointer")
        }
    }

    Surface(modifier = Modifier.offsetYByPercent(offsetYByPercent)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                RouteAltiChartWithGridLines(
                    routePointer,
                    viewModel
                ) { dataPoint ->
                    map(dataPoint?.geoLocation)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            //ScreenRouter.navigateHome()
                            finish(null)
                        }
                    ) {
                        Icon(
                            imageVector = homeIcon,
                            contentDescription = "Go back home"
                        )
                    }
                    Text(text = "$name ${dist?.formatDistM(true)}", fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * Single line chart with grid lines
 *
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RouteAltiChartWithGridLines(
    routePointer: Int,
    viewModel: RouteEleChartViewModel,
    map: (DataPointWithDist?) -> Unit
) {
//    if (mapboxMap.isNotNull().and(mapType.isNotNull()))
    val isLoading by viewModel.isLoading.collectAsState()
    Timber.i("viewModel.isLoading:$isLoading")
    if (isLoading) {
        Timber.i("isLoading")
        Box(modifier = Modifier //.padding(top = TopAppBarDefaults.TopAppBarExpandedHeight.value.dp)
            .fillMaxWidth()
            .height(160.dp)) {
            //CircularProgressIndicator()
            Text(
                stringResource(R.string.create_chart), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        RouteEleChart(
            modifier = Modifier
                //.padding(paddingValues)//.padding(top = 18.dp)
                .fillMaxWidth()
                .height(160.dp),
            lineChartData = viewModel.routeChartData,
            routePointer,
            map = { dataPoint ->
                map(dataPoint)
            }
        )
    }
}

class RouteEleChartViewModel(
    lllh: java.util.ArrayList<LatLngH>?
) : ViewModel() {
    lateinit var routeChartData: RouteChartData
    val isLoading = MutableStateFlow(true)
    var points = ArrayList<DataPointWithDist>()
    val pointsData = ArrayList<Point>()
    lateinit var xAxisData: AxisData
    lateinit var yAxisData: AxisData

    init {
        lllh?.let {
            points = getLineChartData(it) }
        viewModelScope.launch {
            points.forEachIndexed { index, dataPoint ->
                pointsData.add(Point(index.toFloat(), dataPoint.y))
            }
            refresh()
            delay(500)
            isLoading.value = false
        }
    }
    fun refresh() {
        val ySteps = 3
        val xSteps = ((0.1 * (pointsData.size)).toInt()).coerceAtLeast(1)
        xAxisData = AxisData.Builder()
            .axisStepSize(8.dp)
            .topPadding(105.dp)
            //.steps(25)
            .steps(xSteps)
            //.labelData { i -> if (i < pointsData.size) (0.1 * pointsData[i].x).toInt().toString() else ""}
            .labelData { i -> if (i < points.size) (1000 * points[i].distKm).formatDistM(true) else ""}
            .labelAndAxisLinePadding(15.dp)
            .build()
        val yMin = pointsData.minOfOrNull { it.y } ?: 0f
        val yMax = pointsData.maxOfOrNull { it.y } ?: 0f
        val yScale = (yMax - yMin) / ySteps

        yAxisData = AxisData.Builder()
            .steps(ySteps)
            //.topPadding(40.dp)
            .labelAndAxisLinePadding(20.dp)
            .labelData { i ->
                // Add yMin to get the negative axis values to the scale
                ((i * yScale) + yMin).formatNoPrecision()
            }.build()
        routeChartData = RouteChartData(
            linePlotData = RoutePlotData(
                lines = listOf(
                    RouteLine(
                        dataPoints = points,
                        LineStyle(),
                        null, //IntersectionPoint(),
                        selectionHighlightPoint =
                            SelectionHighlightPoint(color = Color.Blue),
                        ShadowUnderLine(),
                        //RouteHighlightPopUp(points)
                        selectionHighlightPopUp =
                            RouteHighlightPopUp(
                                points,
                                backgroundColor = Color.Black,
                                backgroundStyle = Stroke(2f),
                                labelColor = Color.Blue,
                                labelTypeface = Typeface.DEFAULT_BOLD
                            )
                    )
                )
            ),
            xAxisData = xAxisData,
            yAxisData = yAxisData,
            //gridLines = GridLines()
        )
    }
}
/**
 * Returns list of points
 * @param lllh: List of LatLngH
 */
fun getLineChartData(lllh: List<LatLngH>): ArrayList<DataPointWithDist> {
    val dataPoints = ArrayList<DataPointWithDist>()
    if (lllh.isNotEmpty()) {
        Timber.i("lllh ${lllh.size}")

        var distKM = 0F
        var lastLatLng: LatLng? = null
        //for (locationEntity in locationsEntities) {
        lllh.forEachIndexed { index, latLng ->
            if (lastLatLng != null) {
                distKM += 0.001F * SphericalUtil.computeDistanceBetween(
                    LatLng(
                        latLng.latitude,
                        latLng.longitude
                    ), lastLatLng
                ).toFloat()
            }
            lastLatLng = LatLng(
                latLng.latitude,
                latLng.longitude
            )
            dataPoints.add(
                DataPointWithDist(
                    index.toFloat(),
                    latLng.altitude.toFloat(),
                    latLng.latLng,
                    0L,
                    distKM.toDouble()
                )
            )
        }
    }
    return dataPoints
}

@Preview(showBackground = true)
@Composable
fun LineYGraphLllhPreview() {
    val sampleLllh = arrayListOf(
        LatLngH(1.0, 1.0, 100.0),
        LatLngH(1.01, 1.01, 150.0),
        LatLngH(1.02, 1.02, 120.0),
        LatLngH(1.03, 1.03, 200.0)
    )
    com.almica.ramani.ui.theme.RamaniTheme {
        LineYGraphLllh(
            lllhSource = sampleLllh,
            name = "Sample Route",
            offsetYByPercent = 0f,
            finish = {},
            map = {},
            homeIcon = Icons.Default.Home
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RouteAltiChartWithGridLinesPreview() {
    val sampleLllh = arrayListOf(
        LatLngH(1.0, 1.0, 100.0),
        LatLngH(1.01, 1.01, 150.0),
        LatLngH(1.02, 1.02, 120.0),
        LatLngH(1.03, 1.03, 200.0)
    )
    val viewModel = remember { RouteEleChartViewModel(sampleLllh) }
    com.almica.ramani.ui.theme.RamaniTheme {
        RouteAltiChartWithGridLines(
            routePointer = 1,
            viewModel = viewModel,
            map = {}
        )
    }
}
