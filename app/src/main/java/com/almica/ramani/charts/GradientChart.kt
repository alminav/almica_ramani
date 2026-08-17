package com.almica.ramani.charts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almica.ramani.LatLngH
import com.almica.ramani.routes.RouteEntity
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.RouteSmoothingUtil.simplifyToTargetCount
import com.almica.ramani.utils.getDistanceFromLllh
import com.almica.ramani.utils.kmlString2Lllh
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientChart(routeEntity: RouteEntity,
                  moveMap: (LatLng?) -> Unit,
                  result: (LatLng?) -> Unit) {
    var simplifiedPoints by remember { mutableStateOf(emptyList<LatLngH>()) }
    var routeDistance by remember { mutableDoubleStateOf(-1.0) }
    var barChartDataModel by remember { mutableStateOf(GradientChartDataModel(emptyList(), -1, 0.0)) }

    LaunchedEffect(routeEntity) {
        val lllh = routeEntity.kmlString.kmlString2Lllh()
        routeDistance = lllh.getDistanceFromLllh()
        Timber.i("${routeEntity.name} lllh.size:${lllh.size}")
        val stepCount = (0.001 * routeDistance).toInt().coerceAtMost(42)
        if (lllh.isNotEmpty()) {
            simplifiedPoints = lllh.simplifyToTargetCount(stepCount)
        }
        Timber.i("simplifiedPoints.size:${simplifiedPoints.size} routeDistance:${routeDistance.toInt()}")
        barChartDataModel = GradientChartDataModel(simplifiedPoints, -1, routeDistance)
        Timber.i("barChartDataModel bars: ${barChartDataModel.barChartData.first?.bars?.size}")
    }
    GradientChartContent(
        name = routeEntity.name,
        simplifiedPoints = simplifiedPoints,
        barChartDataModel = barChartDataModel,
        moveMap = moveMap,
        result = result
    )
}

@Composable
fun GradientChartContent(
    name: String,
    simplifiedPoints: List<LatLngH>,
    barChartDataModel: GradientChartDataModel,
    moveMap: (LatLng?) -> Unit,
    result: (LatLng?) -> Unit
) {
    Column {
        if (barChartDataModel.barChartData.second != null)
            Text(text = "${barChartDataModel.barChartData.second}  $name", fontSize = 14.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        else
            Text(text = name, fontSize = 14.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(10.dp))
        BarChartRow(barChartDataModel = barChartDataModel, System.currentTimeMillis(), false)
        var sliderPosition by remember{mutableFloatStateOf(0f)}
        Column(modifier = Modifier.padding(start = 32.dp, end = 4.dp)) {
            Slider(
                value = sliderPosition,
                onValueChange = {
                    sliderPosition = it.roundToInt().toFloat()
                    if (it.roundToInt() < simplifiedPoints.size) {
                        val latLng = LatLng(simplifiedPoints[it.roundToInt()].latitude,
                            simplifiedPoints[it.roundToInt()].longitude)
                        moveMap(latLng)
                    }
                },
                onValueChangeFinished = {
                    Timber.i("sliderPosition: $sliderPosition")
                    if (sliderPosition.roundToInt() < simplifiedPoints.size) {
                        val latLng = LatLng(simplifiedPoints[sliderPosition.roundToInt()].latitude,
                            simplifiedPoints[sliderPosition.roundToInt()].longitude)
                        result(latLng)
                    }
                },
                steps = (barChartDataModel.barChartData.first?.bars?.size ?: 1) - 1,
                valueRange=0f..(barChartDataModel.barChartData.first?.bars?.size?.toFloat() ?: 0f)
            )
            Text(text = "$sliderPosition km",
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GradientChartPreview() {
    val sampleRoute = RouteEntity(
        name = "Sample Route",
        region = "Sample Region",
        latitudeStart = -1.2833,
        longitudeStart = 36.8167,
        distance = 5000.0,
        kmlString = ""
    )
    RamaniTheme {
        GradientChart(
            routeEntity = sampleRoute,
            moveMap = {},
            result = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GradientChartContentPreview() {
    val samplePoints = listOf(
        LatLngH(-1.2833, 36.8167, 1600.0),
        LatLngH(-1.2843, 36.8177, 1610.0),
        LatLngH(-1.2853, 36.8187, 1620.0),
        LatLngH(-1.2863, 36.8197, 1615.0),
        LatLngH(-1.2873, 36.8207, 1605.0)
    )
    val sampleDataModel = GradientChartDataModel(samplePoints, -1, 5.0)
    RamaniTheme {
        GradientChartContent(
            name = "Sample Route",
            simplifiedPoints = samplePoints,
            barChartDataModel = sampleDataModel,
            moveMap = {},
            result = {}
        )
    }
}