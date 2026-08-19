package com.almica.ramani.routes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

@Composable
fun ElevationScreen(file: File, onPointSelected: (DataPoint?) -> Unit, onClose: () -> Unit,
                    viewModel: ElevationViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    val chartData by produceState<List<DataPoint>>(initialValue = persistentListOf(), key1 = file) {
        value = withContext(Dispatchers.IO) {
            try {
                file.inputStream().use { inputStream ->
                    val points = when {
                        file.name.endsWith(".kml", ignoreCase = true) -> KmlParser.parseInputStream(inputStream)
                        file.name.endsWith(".gpx", ignoreCase = true) -> GpxParser.parseInputStream(inputStream)
                        else -> emptyList()
                    }
                    points.toImmutableList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                persistentListOf()
            }
        }
    }

    ElevationScreenContent(chartData, onPointSelected, onClose, uiState)
}

@Composable
fun ElevationScreen(kmlData: String, onPointSelected: (DataPoint?) -> Unit, onClose: () -> Unit,
                        viewModel: ElevationViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val chartData by produceState<List<DataPoint>>(initialValue = persistentListOf(), key1 = kmlData) {
        value = withContext(Dispatchers.IO) {
            try {
                kmlData.byteInputStream().use { inputStream ->
                    KmlParser.parseInputStream(inputStream).toImmutableList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                persistentListOf()
            }
        }
    }

    ElevationScreenContent(chartData, onPointSelected, onClose, uiState)
}

@Composable
private fun ElevationScreenContent(
    chartData: List<DataPoint>,
    onPointSelected: (DataPoint?) -> Unit,
    onClose: () -> Unit,
    uiState: ElevationUiState
) {
    Timber.d("ElevationScreenContent: ${uiState.latLng}")
    Box(modifier = Modifier.fillMaxWidth()) {
        ElevationChart(
            dataPoints = chartData,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            onPointSelected = onPointSelected,
            onClose = {onClose()},
            currentLatLng = uiState.latLng
        )
    }
}
