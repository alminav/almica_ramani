package com.almica.ramani.routes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun ElevationScreen(file: File, onPointSelected: (DataPoint?) -> Unit, onClose: () -> Unit) {
    var chartData by remember { mutableStateOf<List<DataPoint>>(emptyList()) }

    LaunchedEffect(file) {
        try {
            val inputStream = file.inputStream()
            chartData = when {
                file.name.endsWith(".kml", ignoreCase = true) -> KmlParser.parseInputStream(inputStream)
                file.name.endsWith(".gpx", ignoreCase = true) -> GpxParser.parseInputStream(inputStream)
                else -> emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    ElevationScreenContent(chartData, onPointSelected, onClose)
}

@Composable
fun ElevationScreen(kmlData: String, onPointSelected: (DataPoint?) -> Unit, onClose: () -> Unit) {
    var chartData by remember { mutableStateOf<List<DataPoint>>(emptyList()) }

    LaunchedEffect(kmlData) {
        try {
            chartData = KmlParser.parseInputStream(kmlData.byteInputStream())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    ElevationScreenContent(chartData, onPointSelected, onClose)
}

@Composable
private fun ElevationScreenContent(chartData: List<DataPoint>, onPointSelected: (DataPoint?) -> Unit, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        ElevationChart(
            dataPoints = chartData,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            onPointSelected = onPointSelected,
            onClose = {onClose()}
        )
    }
}
