package com.almica.ramani.compass

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.almica.ramani.Const
import com.almica.ramani.charts.theme.Orange
import com.almica.ramani.compass.theme.CompassTheme
import com.almica.ramani.utils.format
import com.almica.ramani.utils.formatDistM
import com.almica.ramani.utils.isNotNull
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import timber.log.Timber
import kotlin.math.absoluteValue

@Composable
fun CompassScreen(
    rotation: Int,
    progress: Double
) {
    val destinationLatLng: LatLng? = CompassViewModel.destination.collectAsState().value
    val destinationAltitude: Int? = CompassViewModel.altitudeDestination.collectAsState().value
    val currentLocationLatLng: LatLng? = CompassViewModel.currentLocation.collectAsState().value
    val currentAltitude: Int? = CompassViewModel.currentAltitude.collectAsState().value
    val distance: Double? = CompassViewModel.distance.collectAsState().value
    val poiName: String? = CompassViewModel.nearestPoiName.collectAsState().value
    val poiBmp: Bitmap? = CompassViewModel.poiBmp.collectAsState().value
    //Timber.i("CompassScreen currentLocationLatLng: $currentLocationLatLng")
    //Timber.i("CompassScreen destinationLatLng: $destinationLatLng")
    CompassTheme {
        Surface {
            CompassScreenContent(
                currentLocationLatLng,
                destinationLatLng,
                destinationAltitude,
                currentAltitude,
                rotation,
                progress,
                distance,
                poiName,
                poiBmp
            )
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CompassScreenContent(
    latLngCurrentLocation: LatLng?,
    latLngDestination: LatLng?, altitudeDestination: Int?, currentAltitude: Int?,
    rotation: Int, progress: Double, distance: Double?, poiName: String?, poiBmp: Bitmap?
) {
    /*
        val directionCardinal by remember(rotation) {
            derivedStateOf { CardinalDirection.getDirectionFromAzimuth(rotation.toFloat()) }
        }
     */

    Scaffold(containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface) {paddingValues -> // MaterialTheme.colorScheme.onSurface
        Timber.i("CompassScreenContent paddingValues: $paddingValues")
        Column(
            modifier = Modifier
                .fillMaxSize()
            //.background(getGradientBrush(isSystemInDarkTheme())),
        ) {
            AnimatedVisibility(latLngDestination.isNotNull()) {
                Row {
                    val dist =
                        if (latLngCurrentLocation != null && latLngDestination != null) {
                            SphericalUtil.computeDistanceBetween(
                                latLngCurrentLocation,
                                latLngDestination
                            )
                        } else
                            0.0

                    Image(modifier = Modifier
                        .weight(0.1f)
                        .align(alignment = Alignment.CenterVertically),
                        painter = painterResource(id = com.almica.ramani.R.drawable.circle_filled_red_24px),
                        contentDescription = null
                    )
                    Text(modifier = Modifier.weight(0.4f),
                        //text = "${Const.UC_FILLED_CIRCLE} ${formatDistM(dist, true)}",
                        text = dist.formatDistM(true),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    AnimatedVisibility(visible = altitudeDestination.isNotNull(), modifier = Modifier.weight(0.5f)) {
                        if (altitudeDestination.isNotNull()) {
                            val deltaH = altitudeDestination?.minus((currentAltitude ?: 0))
                            deltaH?.let {
                                val gradient =
                                    (if (dist > 0) 100 * deltaH / dist else 0.0).coerceIn(-100.0, 100.0)

                                val isUp = deltaH >= 0
                                val arrow = if (isUp) Const.UC_UPWARDS_ARROW_FROM_BAR else Const.UC_DOWNWARDS_ARROW_FROM_BAR
                                val gradArrow = if (isUp) Const.GRADIENT_UP_UC else Const.GRADIENT_DOWN_UC

                                Text(
                                    text = if (dist > 100)
                                        "$arrow${deltaH.absoluteValue}m  $gradArrow${gradient.absoluteValue.format(0)}%"
                                    else "$arrow${deltaH.absoluteValue}m",
                                    style = MaterialTheme.typography.headlineMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(latLngDestination.isNotNull()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(1.0f)
                        .align(alignment = Alignment.CenterHorizontally)
                ) {
                    LinearProgressIndicator(
                    progress = { progress.toFloat() },
                    modifier = Modifier
                        .padding(start = 4.dp, end = 4.dp)
                        .fillMaxWidth(),
                    color = ProgressIndicatorDefaults.linearColor,
                    trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap)
                }
            }
            AnimatedVisibility(latLngDestination.isNotNull()) {
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row(modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
                Box(modifier = Modifier.weight(0.5f)) {
                    val direction : Double? =
                        if (latLngCurrentLocation != null && latLngDestination != null) {
                            SphericalUtil.computeHeading(latLngCurrentLocation, latLngDestination)
                        } else
                            null
                    //Timber.i("CompassScreen direction: $direction")
                    //Timber.i("CompassScreen altitudeDestination: $altitudeDestination")
                    Compass(
                        direction = direction?.toInt(),
                        rotation = rotation
                    )
                }

                val haircrossBitmap =
                    CompassViewModel.haircrossThumbnail.collectAsState().value?.asImageBitmap()

                if (haircrossBitmap != null) {
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = BitmapPainter(
                                haircrossBitmap,
                                IntOffset.Zero,
                                IntSize(haircrossBitmap.width, haircrossBitmap.height)
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Cardinal Direction: North
                        Text(
                            text = "N",
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .drawBehind {
                                    drawCircle(
                                        color = Orange,
                                        alpha = 0.5f,
                                        radius = this.size.maxDimension / 2 + 4.dp.toPx()
                                    )
                                },
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Cardinal Direction: South (Visible if no POI name)
                        if (poiName == null) {
                            Text(
                                text = "S",
                                modifier = Modifier.align(Alignment.BottomCenter),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Distance in Top-Right
                        distance?.let { dist ->
                            Text(
                                text = dist.formatDistM(true),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(horizontal = 4.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // West
                        Text(
                            text = "W",
                            modifier = Modifier.align(Alignment.CenterStart),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // East
                        Text(
                            text = "E",
                            modifier = Modifier.align(Alignment.CenterEnd),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // POI Info in Bottom-Right
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            poiBmp?.let { bmp ->
                                Image(
                                    painter = BitmapPainter(bmp.asImageBitmap()),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(2.dp))
                            }
                            poiName?.let { name ->
                                Text(
                                    text = name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }
                    }
                }
//                else {
//                    Spacer(modifier = Modifier.weight(0.5f))
//                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompassScreenPreview() {
    val sampleCurrentLocation = LatLng(37.7749, -122.4194) // San Francisco
    val sampleDestination = LatLng(34.0522, -118.2437) // Los Angeles
    CompassScreen(
        rotation = 45,
        progress = 0.5
    )
}

@Preview(showBackground = true)
@Composable
fun CompassScreenContentPreview() {
    val sampleCurrentLocation = LatLng(52.5200, 13.4050) // Berlin
    val sampleDestination = LatLng(48.8566, 2.3522) // Paris
    CompassTheme {
        CompassScreenContent(
            latLngCurrentLocation = sampleCurrentLocation,
            latLngDestination = sampleDestination,
            altitudeDestination = 100,
            80,
            rotation = 120,
            progress = 0.5,
            distance = 12.5,
            null,
            null
        )
    }
}
