package com.almica.ramani.utils

import com.almica.ramani.LatLngH
import com.graphhopper.util.Instruction


class GpxRouteResult(
    var instructions: ArrayList<Instruction?>?,
    var listLatLngH: ArrayList<LatLngH?>?,
    var instructionType: Int
)