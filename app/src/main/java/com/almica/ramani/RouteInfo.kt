package com.almica.ramani

import java.util.ArrayList

data class RouteInfo(
    val name: String?,
    val formattedDistance: String?,
    val points: ArrayList<LatLngH>
)
