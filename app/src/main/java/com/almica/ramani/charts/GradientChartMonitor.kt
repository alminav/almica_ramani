package com.almica.ramani.charts

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.collectAsState
import com.almica.ramani.ui.theme.RamaniTheme
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.almica.composecharts.charts.bar.BarChartAdjustableAnimation
import com.almica.ramani.Const
import com.almica.ramani.GpsViewModel
import com.almica.ramani.Helpers
import com.almica.ramani.LatLngH
import com.almica.ramani.R
import com.almica.ramani.googlemaps.MapUtils
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.routes.RouteEntity
import com.almica.ramani.utils.RouteSmoothingUtil
import com.almica.ramani.utils.RouteSmoothingUtil.simplifyToTargetCount
import com.almica.ramani.utils.getDistanceFromLllh
import com.almica.ramani.utils.kmlString2Lllh
import com.almica.ramani.utils.offsetYByPercent
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import timber.log.Timber
import java.util.Locale
import kotlin.math.max

/**
 * Created by bytebeats on 2021/9/30 : 19:53
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
/**
 * 17apr2026
 * liveSharedPreferences replaced by GpsViewModel Observer for time
 */
@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientChartMonitor(
    routeEntity: RouteEntity,
    offsetYByPercent: Float,
    homeIcon: ImageVector,
    result: (LatLng?) -> Unit,
//    routePointer: (Int) -> Unit = {},
    animated: Boolean
) {

    val latitude by GpsViewModel.latitude.collectAsState()
    val longitude by GpsViewModel.longitude.collectAsState()
    val locationTime by GpsViewModel.time.collectAsState()

    val routeName = remember(routeEntity.name) {
        if (routeEntity.name.length > 20) {
            routeEntity.name.substring(0..20) + Const.UC_THREEDOTS
        } else routeEntity.name
    }

    val (simplifiedPoints, routeDistance) = remember(routeEntity.kmlString) {
        val lllh = routeEntity.kmlString.kmlString2Lllh()
        val dist = lllh.getDistanceFromLllh()
        val stepCount = (0.001 * dist).toInt().coerceAtMost(42)
        val points = if (lllh.isNotEmpty()) lllh.simplifyToTargetCount(stepCount) else emptyList()
        points to dist
    }

    val closestIndex = remember(latitude, longitude, simplifiedPoints) {
        if (simplifiedPoints.isEmpty()) return@remember -1
        val currentLatLng = LatLng(latitude, longitude)
        simplifiedPoints.indices.minByOrNull { index ->
            MapUtils.calculateHaversineDistance(currentLatLng, simplifiedPoints[index].latLng)
        } ?: -1
    }

//    LaunchedEffect(closestIndex) {
//        routePointer(closestIndex)
//    }

    val barChartDataModel = remember(simplifiedPoints, closestIndex, routeDistance) {
        GradientChartDataModel(simplifiedPoints, closestIndex, routeDistance)
    }

    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            Timber.i("onDispose")
        }
    }


    Surface(modifier = Modifier.offsetYByPercent(offsetYByPercent)
        //.padding(top = 500.dp)
        ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            result(null)
                        }
                    ) {
                        Icon(
                            imageVector = homeIcon,
                            contentDescription = "Go back home"
                        )
                    }

                    if (barChartDataModel.barChartData.second != null)
                        Text(
                            text = "${barChartDataModel.barChartData.second}  $routeName",
                            fontSize = 14.sp
                        )
                    else
                        Text(text = routeName, fontSize = 14.sp)
                }
                //Timber.i("lllhReduced.size:${lllhReduced.size}")

                AnimatedVisibility(
                    visible = simplifiedPoints.isEmpty() || barChartDataModel.barChartData.first == null
                ) {
                    Timber.i("%s", stringResource(R.string.no_relevant_chart_data))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.no_relevant_chart_data),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                AnimatedVisibility(
                    visible = simplifiedPoints.isNotEmpty()
                        && barChartDataModel.barChartData.first.isNotNull()
                ) {
                    Timber.i("locationTime $locationTime")
                    //barChartDataModel = GradientChartDataModel(lllhReduced, routePointer, routeDistance)
                    if (barChartDataModel.barChartData.first != null)
                        GradientChartContent(
                            barChartDataModel,
                            locationTime,
                            animated
                        )
                }


                //Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

fun Int.format(digits: Int) = "%0${digits}d".format(Locale.ENGLISH,this)

@Composable
private fun GradientChartContent(barChartDataModel: GradientChartDataModel, locationTime: Long, animated: Boolean) {
    Column(
//        modifier = modifier.padding(
//            horizontal = Margin.horizontal,
//            vertical = Margin.verticalMedium
//        )
    ) {
        BarChartRow(barChartDataModel = barChartDataModel, locationTime, animated)
    }
}

@Composable
fun BarChartRow(barChartDataModel: GradientChartDataModel, locationTime: Long, animated: Boolean) {
    Column {
//        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
//            Text(text = barChartDataModel.barChartData.second)
//        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            barChartDataModel.barChartData.first?.let {
                //Timber.i( "${barChartDataModel.labelDrawer.drawLocation}")
                BarChartAdjustableAnimation(
                    barChartData = it,
                    labelDrawer = barChartDataModel.labelDrawer,
                    routePointer = barChartDataModel.routePointer,
                    locationTime = locationTime,
                    animated = animated
                )
            }
        }
    }
}

fun reducedLllhKmSteps(lllh: List<LatLngH>): List<LatLngH> {
    if (lllh.isEmpty()) return emptyList()
    val originalDistanceKm = (1 + (0.001 * calcDistMeter(lllh)).toInt())
    val stepWidth = max(1, lllh.size / originalDistanceKm)

    Timber.i("originalDistanceKm: $originalDistanceKm stepWidth: $stepWidth")

    val reducedListLatLngH = lllh.filterIndexed { index, _ ->
        index % stepWidth == 0
    }.take(originalDistanceKm)

    Timber.i("reduce: ${lllh.size} --> ${reducedListLatLngH.size}")
    return reducedListLatLngH
}

fun calcDistMeter(listLatLng: List<LatLngH>?): Double {
    return listLatLng?.zipWithNext { a, b ->
        SphericalUtil.computeDistanceBetween(a.latLng, b.latLng)
    }?.sum() ?: 0.0
}

@Preview(showBackground = true)
@Composable
fun GradientChartMonitorPreview() {
    val sampleKml = """
        <LineString>
            <coordinates>
                0.0,0.0,100.0 0.01,0.01,150.0 0.02,0.02,120.0 0.03,0.03,180.0 0.04,0.04,160.0
            </coordinates>
        </LineString>
    """.trimIndent()
    val routeEntity = RouteEntity(
        name = "Sample Route",
        kmlString = sampleKml
    )
    RamaniTheme {
        GradientChartMonitor(
            routeEntity = routeEntity,
            offsetYByPercent = 0f,
            homeIcon = Icons.Default.Home,
            result = {},
            animated = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BarChartRowPreview() {
    val sampleLllh = arrayListOf(
        LatLngH(0.0, 0.0, 100.0),
        LatLngH(0.01, 0.01, 150.0),
        LatLngH(0.02, 0.02, 120.0),
        LatLngH(0.03, 0.03, 180.0),
        LatLngH(0.04, 0.04, 160.0)
    )
    val model = remember { GradientChartDataModel(sampleLllh, 2, 5000.0) }
    RamaniTheme {
        BarChartRow(
            barChartDataModel = model,
            locationTime = System.currentTimeMillis(),
            animated = false
        )
    }
}
