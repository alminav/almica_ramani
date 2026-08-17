package com.almica.ramani.routes

import com.almica.ramani.Helpers.Companion.latitudeToY
import com.almica.ramani.Helpers.Companion.longitudeToX
import com.almica.ramani.LatLngH
import com.almica.ramani.utils.getDistanceFromLllh
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.geometry.Point

class Track(val listLlh: List<LatLngH>?) {
    private val points = ArrayList<Point>()

    init {
        if (listLlh != null) {
            for (coordinate in listLlh) {
                points.add(
                    Point(
                        longitudeToX(
                            coordinate.longitude
                        ), latitudeToY(coordinate.latitude)
                    )
                )
            }
        }
    }

    val distance: Double?
        get() = listLlh?.getDistanceFromLllh()

    val startLatLng: LatLng?
        get() = listLlh?.first()?.latLng

    val stopLatLng: LatLng?
        get() = listLlh?.last()?.latLng

    val center: LatLng
        get() = LatLng(
            0.5 * (latitudeMax + latitudeMin),
            0.5 * (longitudeMax + longitudeMin)
        )
    val xYCenter: Point
        get() = Point(0.5 * (xMax + xMin), 0.5 * (yMax + yMin))
    val yMax: Double
        get() {
            var yMax = Double.MIN_VALUE
            for (p in points) if (p.y > yMax) yMax = p.y
            return yMax
        }
    val yMin: Double
        get() {
            var yMin = Double.MAX_VALUE
            for (p in points) if (p.y < yMin) yMin = p.y
            return yMin
        }
    val xMax: Double
        get() {
            var xMax = Double.MIN_VALUE
            for (p in points) if (p.x > xMax) xMax = p.x
            return xMax
        }
    val xMin: Double
        get() {
            var xMin = Double.MAX_VALUE
            for (p in points) if (p.x < xMin) xMin = p.x
            return xMin
        }

    val latitudeMax: Double
        get() {
            var latMax = Double.MIN_VALUE
            if (listLlh != null) {
                for (llh in listLlh) if (llh.latitude > latMax) latMax = llh.latitude
            }
            return latMax
        }
    val longitudeMax: Double
        get() {
            var lonMax = Double.MIN_VALUE
            if (listLlh != null) {
                for (llh in listLlh) if (llh.longitude > lonMax) lonMax = llh.longitude
            }
            return lonMax
        }
    val latitudeMin: Double
        get() {
            var latMin = Double.MAX_VALUE
            if (listLlh != null) {
                for (llh in listLlh) if (llh.latitude < latMin) latMin = llh.latitude
            }
            return latMin
        }
    val longitudeMin: Double
        get() {
            var lonMin = Double.MAX_VALUE
            if (listLlh != null) {
                for (llh in listLlh) if (llh.longitude < lonMin) lonMin = llh.longitude
            }
            return lonMin
        }

    fun getPoints(): List<Point> {
        return points
    }
}
