package com.almica.ramani

import android.content.Context
import android.graphics.Bitmap
import com.almica.ramani.utils.HgtReader
import com.almica.ramani.Helpers.Companion.getTileName
import com.almica.ramani.utils.getCenter
import com.almica.ramani.utils.getEquidistantPoints
import com.almica.ramani.utils.isNotNull
import timber.log.Timber
import java.io.File

data class PolygonState(var lllh: List<LatLngH>, val name: String, val distance: Double,
                   var polygonData: PolygonData? = null)

data class PolygonData(
    var lllh: List<LatLngH>,
    val name: String,
    val distance: Double,
    var hgtState: Boolean,
    var polygonMarkerDataList: List<PolygonMarkerData>? = null,
    var lllhKmSteps: List<LatLngH>? = null
)

data class PolygonMarkerData(
    var latLngH: LatLngH,
    val gradient: Double,
    val distanceKm: Double,
    var bitmap: Bitmap? // not used, but good to know
)

fun PolygonData.createPolygonMarkers(context: Context, _hMax: Double): Double {
    Timber.i("route name: ${this.name}")
    var hMax = _hMax
    val polygonMarkerDataList = mutableListOf<PolygonMarkerData>()
    val center = this.lllh.getCenter()
    if (hMax < 10) {
        val tileName = getTileName(center.latitude, center.longitude).uppercase()
        if (!this.hgtState) {
            val demFolder =
                File(context.filesDir, Const.HGT_FOLDER_NAME)
            val hgtFile = File(demFolder, tileName + Const.HGT_EXT)
            if (hgtFile.exists()) {
                val hgtReader = HgtReader(context, hgtFile)
                val lllhRefreshed =
                    hgtReader.refreshRouteElevationFromSrtm(this.lllh)
                if (lllhRefreshed.isNotNull() && lllhRefreshed.lllh.isNotNull()) {
                    this.lllh = lllhRefreshed.lllh!!
                    this.hgtState = true
                    hMax = hMax.coerceAtLeast(lllhRefreshed.hMax)
                }
            }
        }
    } else
        this.hgtState = true

    val interval = 750.0 // estimation 19jan2026, 1000.0
    lllhKmSteps = this.lllh.getEquidistantPoints(interval)
    //Timber.i("lllhKmSteps: ${lllhKmSteps?.size}")
    lllhKmSteps?.forEachIndexed { i, llh ->
        if (i > 0) {
            var gradient: Double
            val deltaH: Double = lllhKmSteps!![i].altitude - lllhKmSteps!![i - 1].altitude
            gradient = 100 * deltaH / interval //1 km steps
            //val c = interpolateColor((0.1 * abs(gradient)).toFloat())

            polygonMarkerDataList.add(
                PolygonMarkerData(
                    lllhKmSteps!![i],
                    gradient,
                    i.toDouble(),
                    null
                )
            )
        }
    }
    this.polygonMarkerDataList = polygonMarkerDataList
    return hMax
}
