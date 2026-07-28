package com.almica.ramani.googlemaps

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.almica.ramani.R
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber

@Composable
fun GmsZoomButtons(locationEnabled: Boolean, zoom: Float, setZoom: (Float) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            AnimatedVisibility(visible = locationEnabled) {
                Button(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.teal_200_trans)
                    ),
                    onClick = {
                        setZoom(zoom.plus(1f))
                        Timber.i("zoom: $zoom")
                    }) {
                    Text(
                        "+",
                        color = Color.White,
                        //modifier = Modifier.background(color = colorResource(R.color.teal_200)),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
            AnimatedVisibility(visible = locationEnabled) {
                Button(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.teal_200_trans)
                    ),
                    onClick = {
                        //val cameraModeClone = cameraMode
                        //cameraMode.intValue = CameraMode.NONE
                        setZoom(zoom.minus(1f))
                        Timber.i("zoom: $zoom")
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

@Preview(showBackground = true)
@Composable
fun GmsZoomButtonsPreview() {
    RamaniTheme {
        GmsZoomButtons(
            locationEnabled = true,
            zoom = 15f,
            setZoom = {}
        )
    }
}
