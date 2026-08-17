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
fun ElevationScreen(file: File, onClose: () -> Unit) {
    //val context = LocalContext.current
    var chartData by remember { mutableStateOf<List<DataPoint>>(emptyList()) }

    // Datei asynchron beim Starten laden
    LaunchedEffect(file) {
        try {
            // Platziere deine Datei in: src/main/assets/route.kml
            val inputStream = file.inputStream()
            chartData = KmlParser.parseInputStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        ElevationChart(
            dataPoints = chartData,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp) // Feste Höhe für die Chart-Box
        ) {
            onClose()
        }
    }

}