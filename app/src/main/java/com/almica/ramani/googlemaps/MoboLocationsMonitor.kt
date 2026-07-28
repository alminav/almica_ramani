package com.almica.ramani.googlemaps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.almica.ramani.charts.MonitorGraphLocations
import com.almica.ramani.charts.PlotResult
import com.google.android.gms.maps.model.LatLng

import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.LatLngH
import com.almica.ramani.charts.DataPointWithDist
import com.almica.ramani.charts.GraphDataPoints
import com.almica.ramani.ui.theme.RamaniTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoboLocationsMonitor(
    lllh: List<LatLngH>?,
    plotResult: PlotResult?,
    result: (LatLng?) -> Unit
)
{
    ModalBottomSheet(onDismissRequest = { result(null) }) {
        Box(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) {
            //PerformanceChart()

            MonitorGraphLocations(lllh,
                plotResult,
                0L, { _ ->
                    result(null)
                },
                map = { result(null) },
                {}
            )

        }
    }
}

@Preview(showBackground = true)
@Composable
fun MoboLocationsMonitorPreview() {
    val samplePoints = mutableListOf(
        DataPointWithDist(0f, 100f, LatLng(0.0, 0.0), 0L, 0.0),
        DataPointWithDist(1f, 110f, LatLng(0.01, 0.01), 1000L, 0.1)
    )
    val samplePlotResult = PlotResult(
        lines = GraphDataPoints(
            dataPointsAlti = samplePoints.toMutableList(),
            dataPointsSrtm = samplePoints.toMutableList(),
            dataPointsSpeed = samplePoints.toMutableList(),
            dataPointsSpeedAvg = samplePoints.toMutableList()
        ),
        distKM = 0.1f
    )

    RamaniTheme {
        MoboLocationsMonitor(
            lllh = null,
            plotResult = samplePlotResult
        ) {}
    }
}
