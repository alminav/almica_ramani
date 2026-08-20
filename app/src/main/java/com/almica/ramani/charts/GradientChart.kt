package com.almica.ramani.charts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almica.ramani.LatLngH
import com.almica.ramani.routes.RouteEntity
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.format
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber
import kotlin.math.roundToInt

@Composable
fun GradientChart(
    routeEntity: RouteEntity,
    moveMap: (LatLng?) -> Unit,
    result: (LatLng?) -> Unit,
    viewModel: GradientChartViewModel = viewModel()
) {
    LaunchedEffect(routeEntity) {
        viewModel.loadRoute(routeEntity)
    }

    val state = viewModel.uiState.collectAsState().value

    when (state) {
        is GradientChartUiState.Loading -> {
            // Potentially show a loading indicator
        }
        is GradientChartUiState.Success -> {
            GradientChartWithSlider(
                state = state,
                moveMap = moveMap,
                result = result
            )
        }
    }
}

@Composable
fun GradientChartWithSlider(
    state: GradientChartUiState.Success,
    moveMap: (LatLng?) -> Unit,
    result: (LatLng?) -> Unit
) {
    val dataModel = state.dataModel

    val currentLatLng by remember(state.points, dataModel) {
        derivedStateOf {
            val idx = dataModel.sliderPosition.roundToInt().coerceIn(state.points.indices)
            if (state.points.isNotEmpty()) {
                val p = state.points[idx]
                LatLng(p.latitude, p.longitude)
            } else null
        }
    }

    val distanceLabel by remember(state.distances, dataModel) {
        derivedStateOf {
            val idx = dataModel.sliderPosition.roundToInt().coerceIn(state.distances.indices)
            if (state.distances.isNotEmpty()) {
                "${(state.distances[idx] / 1000.0).format(1)} km"
            } else "0.0 km"
        }
    }

    Column {
        val title = dataModel.barChartData.second?.let { "$it  ${state.name}" } ?: state.name
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        BarChartRow(
            barChartDataModel = dataModel,
            locationTime = 0L,
            animated = false
        )
        
        Column(modifier = Modifier.padding(start = 32.dp, end = 4.dp)) {
            Slider(
                value = dataModel.sliderPosition,
                onValueChange = {
                    Timber.i("sliderPosition: $it")
                    dataModel.sliderPosition = it
                    moveMap(currentLatLng)
                },
                onValueChangeFinished = {
                    Timber.i("sliderPosition: ${dataModel.sliderPosition}")
                    result(currentLatLng)
                },
                steps = if (state.points.size > 1) state.points.size - 2 else 0,
                valueRange = 0f..if (state.points.isNotEmpty()) (state.points.size - 1).toFloat() else 0f
            )
            
            Text(
                text = distanceLabel,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
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
fun GradientChartWithSliderPreview() {
    val samplePoints = listOf(
        LatLngH(-1.2833, 36.8167, 1600.0),
        LatLngH(-1.2843, 36.8177, 1610.0),
        LatLngH(-1.2853, 36.8187, 1620.0),
        LatLngH(-1.2863, 36.8197, 1615.0),
        LatLngH(-1.2873, 36.8207, 1605.0)
    )
    val state = GradientChartUiState.Success(
        name = "Sample Route",
        points = samplePoints,
        distances = listOf(0.0, 150.0, 300.0, 450.0, 600.0),
        dataModel = GradientChartDataModel(samplePoints, -1, 0.6)
    )
    RamaniTheme {
        GradientChartWithSlider(
            state = state,
            moveMap = {},
            result = {}
        )
    }
}