package com.almica.ramani.googlemaps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AreaChart
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almica.ramani.Const
import com.almica.ramani.R
import com.almica.ramani.charts.theme.Teal200
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber

import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.LatLngH
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.RouteSmoothingUtil.simplifyToTargetCount
import com.almica.ramani.utils.formatDistM

data class GmsRouteDataPair (var selectedRoute: RouteData?, var highlightedRoute: RouteData?)
@Composable
fun GmsRouteSelection(route: RouteData?, selectedRoute: RouteData?,
                      highlightedRoute: RouteData?, selectRoute: (GmsRouteDataPair) -> Unit,
                      simulationLatLngList: List<LatLng>?,
                      gradientRouteData: (RouteData?) -> Unit,
                      elevationRouteData: (RouteData?) -> Unit,
                      showRouteSavingScreen: (Boolean) -> Unit,
                      simulation: (List<LatLng>?) -> Unit
) {
    val textDist = route?.distance?.formatDistM(true)?: "0.0"
    Column(
        modifier = Modifier
            .background(colorResource(R.color.white))
            .padding(start = 6.dp, end = 6.dp)
    ) {
        TextButton(onClick = {
            Timber.i("route name: ${route?.name}")
            selectRoute(GmsRouteDataPair(
                if (selectedRoute?.name == route?.name) null else route,
                if (highlightedRoute?.name == route?.name) null else route))
        }) {
            if (route != null) {
                Text(
                    text = if (selectedRoute?.name == route.name) "${route.name} ${Const.UC_CHECKMARK}" else
                        if (highlightedRoute?.name == route.name) "${route.name} ${Const.UC_CHECKMARK}" else route.name ,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            IconButton(
                onClick = {
                    gradientRouteData(route)
                    Timber.i("gradientRouteData")
                }, modifier = Modifier
                    .weight(0.25f)
                    .clip(RectangleShape)
                    .width(32.dp)
                    .height(32.dp)
                    .border(1.dp, Teal200, RectangleShape)
                    .background(colorResource(R.color.teal_200_trans))
            ) {
                Icon(Icons.Outlined.BarChart, null, tint = Color.White)
            }
            IconButton(
                onClick = {
                    elevationRouteData(route)
                    Timber.i("elevationRouteData")
                }, modifier = Modifier
                    .weight(0.25f)
                    .clip(RectangleShape)
                    .width(32.dp)
                    .height(32.dp)
                    .border(1.dp, Teal200, RectangleShape)
                    .background(colorResource(R.color.teal_200_trans))
            ) {
                Icon(Icons.Outlined.AreaChart, null, tint = Color.White)
            }

            Column(modifier = Modifier
                .fillMaxWidth()
                .weight(0.25f)) {
                Text(modifier = Modifier.fillMaxWidth(),
                    text = textDist,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Box(modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
                    IconButton(onClick = {
                        if (simulationLatLngList == null) {
                            if (route != null) {
                                val lllh100 = route.lllh.simplifyToTargetCount(100)
                                simulation(List(lllh100.size) { i ->
                                    lllh100[i].latLngGms
                                })
                            }
                        } else
                            simulation(null)
                    }) {
                        Image(
                            painter = painterResource(R.drawable.ic_marker_auto),
                            modifier = Modifier
                                .requiredSize(24.dp)
                                .rotate(90f),
                            contentDescription = null
                        )
                    }
                }
            }
            IconButton(
                onClick = {
                    showRouteSavingScreen(true)
                    Timber.i("showRouteSavingScreen")
                }, modifier = Modifier
                    .weight(0.25f)
                    .clip(RectangleShape)
                    .width(32.dp)
                    .height(32.dp)
                    .border(1.dp, Teal200, RectangleShape)
                    .background(colorResource(R.color.teal_200_trans))
            ) {
                Icon(Icons.Outlined.Save, null, tint = Color.White)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GmsRouteSelectionPreview() {
    val sampleRoute = RouteData(
        lllh = arrayListOf(
            LatLngH(LatLng(1.23, 4.56), 100.0),
            LatLngH(LatLng(1.24, 4.57), 110.0)
        ),
        name = "Sample Route",
        distance = 1500.0,
        state = true,
        routeMarkerDataList = null
    )
    RamaniTheme {
        GmsRouteSelection(
            route = sampleRoute,
            selectedRoute = null,
            highlightedRoute = sampleRoute,
            selectRoute = {},
            simulationLatLngList = null,
            gradientRouteData = {},
            elevationRouteData = {},
            showRouteSavingScreen = {},
            simulation = {}
        )
    }
}