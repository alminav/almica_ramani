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
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapLibreMap
import timber.log.Timber

@Composable
fun MainZoomButtons(
    visibility: Boolean,
    mapPositionZoom: MutableState<Double?>,
    map: MapLibreMap?,
    cameraPosition: MutableState<CameraPosition>, 
    setZoom: (Double?) -> Unit
) {

    val zoomText by remember {
        derivedStateOf {
            (mapPositionZoom.value ?: cameraPosition.value.zoom)?.format(1) ?: ""
        }
    }
    
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
                            // 1. Get current zoom from map (most accurate)
                            val currentActual = map?.cameraPosition?.zoom ?: 13.0
                            
                            // 2. Calculate new zoom: Snap to nearest integer then add 1.0
                            // This ensures the "step width is 1" even if currently at 13.2
                            val newZoom = (Math.round(currentActual) + 1).toDouble()
                            
                            Timber.i("zoom +: $newZoom (from $currentActual)")
                            
                            // 3. Use moveCamera (Instant) instead of easeCamera
                            // This prevents GPS updates from cancelling the zoom mid-animation
                            map?.moveCamera(CameraUpdateFactory.zoomTo(newZoom))
                            
                            // 4. Update UI state
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
                            val currentActual = map?.cameraPosition?.zoom ?: 13.0
                            
                            // Snap to nearest integer then subtract 1.0
                            val newZoom = (Math.round(currentActual) - 1).toDouble()
                            
                            Timber.i("zoom -: $newZoom (from $currentActual)")
                            
                            map?.moveCamera(CameraUpdateFactory.zoomTo(newZoom))
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
