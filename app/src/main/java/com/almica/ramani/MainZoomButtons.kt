package com.almica.ramani

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.format
import com.almica.ramani_lib.CameraPosition
import org.maplibre.android.maps.MapLibreMap
import timber.log.Timber

@Composable
fun MainZoomButtons(
    visibility: Boolean,
    mapPositionZoom: MutableState<Double?>,
    map: MapLibreMap?,
    cameraPosition: MutableState<CameraPosition>, setZoom: (Double?) -> Unit
) {
    val zoomText by remember {
        derivedStateOf {
            (mapPositionZoom.value ?: cameraPosition.value.zoom)?.format(1) ?: ""
        }
    }
    Timber.i("zoomText: $zoomText")
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.align(alignment = Alignment.CenterEnd)) {
            AnimatedVisibility(
                visible = visibility
            ) {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    Button(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.teal_200_trans)
                        ),
                        onClick = {
                            //val cameraModeClone = cameraMode
                            //cameraMode.intValue = CameraMode.NONE
                            val newZoom: Double? = map?.cameraPosition?.zoom?.plus(1.0)
                            Timber.i("zoom: $newZoom")
                            setZoom(newZoom)
                        }) {
                        Text(
                            "+",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = zoomText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.teal_200_trans)
                        ),
                        onClick = {
                            //val cameraModeClone = cameraMode
                            //cameraMode.intValue = CameraMode.NONE
                            val newZoom = cameraPosition.value.zoom?.minus(1.0)
                            Timber.i("zoom: $newZoom")
                            setZoom(newZoom)
                        },
                    ) {
                        Text(
                            "-",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun MainZoomButtonsPreview() {
    val cameraPosition = remember {
        mutableStateOf(CameraPosition(zoom = 10.0))
    }
    RamaniTheme {
        MainZoomButtons(
            visibility = true,
            mapPositionZoom = remember { mutableStateOf(12.0) },
            map = null,
            cameraPosition = cameraPosition,
            setZoom = {}
        )
    }
}