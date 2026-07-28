package com.almica.ramani.routes

class RouteNearestPointResult(
    var deviation_meters: Int,
    var route_pointer: Int,
    var route_distance_meters: Double,
    var route_distpart_meters: Double
) {
    override fun toString(): String {
        return "deviation: " + deviation_meters +
                " route_distance_meters: " + route_distance_meters +
                " route_distpart_meters: " + route_distpart_meters +
                " route_point_pointer: " + route_pointer
    }
}