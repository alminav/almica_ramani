package com.almica.ramani.utils

import com.almica.ramani.LatLngH

/**
 * A route represented by a name and a list of points.
 */
data class NamedRoute(
    val name: String,
    val points: ArrayList<LatLngH>
)
