package com.almica.ramani.charts

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.Helpers
import com.almica.ramani.LatLngH
import com.almica.ramani.routes.RouteEntity
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.kmlString2Lllh
import com.google.android.gms.maps.model.LatLng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MbsElevationChart(
    routeEntity: RouteEntity,
    onSelectLocation: (LatLng) -> Unit = {},
    onClose: () -> Unit
) {
    val lllh = remember(routeEntity.kmlString) { routeEntity.kmlString.kmlString2Lllh() }
    val viewModel = remember(lllh) { RouteEleChartViewModel(lllh) }
    var routePointer by remember { mutableIntStateOf(-1) }

    ModalBottomSheet(onDismissRequest = onClose) {
        RouteAltiChartWithGridLines(
            routePointer = routePointer,
            viewModel = viewModel,
            map = { dataPoint ->
                dataPoint?.geoLocation?.let { onSelectLocation(it) }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MbsElevationChartPreview() {
    val sampleRoute = RouteEntity(
        name = "Sample Route",
        kmlString = "<kml><Document><Placemark><LineString><coordinates>1.0,1.0,100.0 1.01,1.01,150.0 1.02,1.02,120.0 1.03,1.03,200.0</coordinates></LineString></Placemark></Document></kml>"
    )
    RamaniTheme {
        MbsElevationChart(
            routeEntity = sampleRoute,
            onClose = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MbsElevationChartContentPreview() {
    val sampleLllh = arrayListOf(
        LatLngH(1.0, 1.0, 100.0),
        LatLngH(1.01, 1.01, 150.0),
        LatLngH(1.02, 1.02, 120.0),
        LatLngH(1.03, 1.03, 200.0)
    )
    val viewModel = remember { RouteEleChartViewModel(sampleLllh) }
    RamaniTheme {
        RouteAltiChartWithGridLines(
            routePointer = -1,
            viewModel = viewModel,
            map = {}
        )
    }
}
