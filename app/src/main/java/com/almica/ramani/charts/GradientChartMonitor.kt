package com.almica.ramani.charts

import android.annotation.SuppressLint
import android.location.Location
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
import androidx.compose.ui.platform.LocalContext
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
private const val logtag = "GradientChartMonitor"
@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientChartMonitor(
    routeEntity: RouteEntity,
    userLocation: Location,
    offsetYByPercent: Float,
    homeIcon: ImageVector,
    result: (LatLng?) -> Unit,
    animated: Boolean
) {
//    Timber.i( "${mapComposer?.applier}")
    val context = LocalContext.current
    val routeTolerance = 500.0
    var trackingIsActive by remember { mutableStateOf(false) }
    var locationTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var routePointer by remember { mutableIntStateOf(-1) }
    //var routeDeviation by remember { mutableStateOf<Int?>(0) }
    //val lllh = Helpers.getLllhFromFile(routeFile)
    val routeName by remember { mutableStateOf(
        if (routeEntity.name.length > 20) {
            routeEntity.name.substring(0..20) + Const.UC_THREEDOTS } else routeEntity.name)
    }

    //var lllhReduced = ArrayList<LatLngH>()
    var simplifiedPoints by remember { mutableStateOf(emptyList<LatLngH>()) }
    var routeDistance by remember { mutableDoubleStateOf(-1.0) }
    var barChartDataModel by remember { mutableStateOf(GradientChartDataModel(emptyList(), -1, 0.0)) }

    LaunchedEffect(routeName) {
        val lllh = routeEntity.kmlString.kmlString2Lllh()
        routeDistance = lllh.getDistanceFromLllh()
        Timber.i("${routeEntity.name} lllh.size:${lllh.size}")
        val stepCount = (0.001 * routeDistance).toInt().coerceAtMost(42)
        if (lllh.isNotEmpty()) {
            simplifiedPoints = lllh.simplifyToTargetCount(stepCount)
        }
//        if (lllh.isNotEmpty())
//            simplifiedPoints = reducedLllhKmSteps(lllh)
        if (userLocation.isNotNull() && userLocation.provider != null) {
            val gmsLatLng = LatLng(userLocation.latitude, userLocation.longitude)
            routePointer = Helpers.locationIndexOnPath(gmsLatLng, simplifiedPoints, routeTolerance)
            Timber.i("routePointer: $routePointer $userLocation")
        } else {
            Timber.i("userLocation = null")
        }
        Timber.i("simplifiedPoints.size:${simplifiedPoints.size} routeDistance:${routeDistance.toInt()}")
        barChartDataModel = GradientChartDataModel(simplifiedPoints, routePointer, routeDistance)
        Timber.i("barChartDataModel bars: ${barChartDataModel.barChartData.first?.bars?.size}")
    }

    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            Timber.i( "onDispose")
        }
    }

    val latModel = GpsViewModel.latitude.collectAsState()
    val lonModel = GpsViewModel.longitude.collectAsState()
    latModel.value?.let { latitude ->
        lonModel.value?.let { longitude ->
            val recordedLatLng = LatLng(latitude, longitude)
            recordedLatLng.let {
                routePointer = Helpers.locationIndexOnPath(it, simplifiedPoints, routeTolerance)
            }
        }
    }
    LaunchedEffect(routePointer) {
        Timber.i("LaunchedEffect routePointer:$routePointer")
        barChartDataModel =
            GradientChartDataModel(simplifiedPoints, routePointer, routeDistance)
    }

    /*
    if (liveSharedPreferences.isNotNull()) {
        liveSharedPreferences?.getLong(Const.LAST_LOCATION_TIME, 0L)
            ?.observe(LocalLifecycleOwner.current) { locTime ->
                //Timber.i( "locTime:$locTime")

                val locationRepository =
                    LocationRepository.getInstance(context, Executors.newSingleThreadExecutor())
                if (lllhReduced.isNotEmpty().and(locTime.isNotNull()).and(locationTime != locTime)) {
                    val locations = locationRepository.getLocationForTime(locTime)
                    Timber.i(
                        "locationTime:$locationTime")
                    if (locations.isNotEmpty()) {
                        locationTime = locTime!!
                        val latitude = locations[0].latitude
                        val longitude = locations[0].longitude
                        Timber.i("lat: $latitude lon: $longitude")
                        val recordedLatLng = LatLng(latitude, longitude)
                        recordedLatLng.let {
                            routePointer = Helpers.locationIndexOnPath(it, lllhReduced, routeTolerance)
                        }
                        Timber.i(
                            "routePointer:$routePointer")
                        barChartDataModel =
                            GradientChartDataModel(lllhReduced, routePointer, routeDistance)
                        Timber.i(
                            "barChartDataModel: ${barChartDataModel.barChartData.second}")
                    } else
                        Timber.i( "locations is empty")
                }
            } ?: 0L
    } else
        Timber.i( "liveSharedPreferences = null")
    */
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
                            //ScreenRouter.navigateHome()
                            simplifiedPoints = emptyList()
                            routeDistance = -1.0
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

                AnimatedVisibility(visible = trackingIsActive.and(routePointer < 0)) {
                    Timber.i(
                        "%s",
                                stringResource(R.string.route_tolerance_exceeded, routeTolerance.toInt()))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(
                                R.string.route_tolerance_exceeded,
                                routeTolerance.toInt()
                            )
                        )
                    }
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
                Timber.i( "${barChartDataModel.labelDrawer.drawLocation}")
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

fun reducedLllhKmSteps(lllh: ArrayList<LatLngH>) : ArrayList<LatLngH> {
    if (lllh.isEmpty()) return arrayListOf()
    //val lllh = RouteSmoothingUtil.smoothRoute(lllhSource) as ArrayList<LatLngH>?
    lllh.let {
        val originalDistanceKm = (1 + (0.001 * calcDistMeter(lllh)).toInt())
        val stepWidth = max(1, lllh.size / originalDistanceKm)

        Timber.i("originalDistanceKm: $originalDistanceKm stepWidth: $stepWidth")

        val reducedListLatLngH = (lllh.filterIndexed { index, _ ->
            index % stepWidth == 0
        }.take(originalDistanceKm) ?: arrayListOf()) as ArrayList<LatLngH>

        Timber.i("reduce: ${lllh.size} --> ${reducedListLatLngH.size}")
        return reducedListLatLngH
    }
}

fun calcDistMeter(listLatLng: ArrayList<LatLngH>?): Double {
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
    val userLocation = Location("fused").apply {
        latitude = 0.0
        longitude = 0.0
        provider = "fused"
    }
    RamaniTheme {
        GradientChartMonitor(
            routeEntity = routeEntity,
            userLocation = userLocation,
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
