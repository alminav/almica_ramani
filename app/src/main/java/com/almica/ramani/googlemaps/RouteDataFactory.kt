package com.almica.ramani.googlemaps

import android.content.Context
import android.widget.TextView
import com.almica.ramani.Const
import com.almica.ramani.Helpers.Companion.getTileName
import com.almica.ramani.LatLngH
import com.almica.ramani.charts.interpolateColor
import com.almica.ramani.routes.RouteEntity
import com.almica.ramani.utils.HgtReader
import com.almica.ramani.utils.getCenter
import com.almica.ramani.utils.getEquidistantPoints
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.utils.kmlString2Lllh
import com.almica.ramani.utils.lllhToKmlString
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.ui.IconGenerator
import timber.log.Timber
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

data class RouteData(
    var lllh: ArrayList<LatLngH>,
    val name: String,
    val distance: Double,
    var state: Boolean,
    var routeMarkerDataList: List<RouteMarkerData>?
)
data class RouteMarkerData(
    var latLngH: LatLngH,
    val gradient: Double,
    val distanceKm: Double,
    var icon: BitmapDescriptor?
)

fun RouteData.createRouteMarkers(context: Context, _hMax: Double): Double {
        var hMax = _hMax
        val routeMarkerDataList = mutableListOf<RouteMarkerData>()
        val center = this.lllh.getCenter()
        if (hMax < 10) {
            val tileName =
                getTileName(center.latitude, center.longitude).uppercase()
            if (!this.state) {
                val demFolder =
                    File(context.filesDir, Const.HGT_FOLDER_NAME)
                val hgtFile = File(demFolder, tileName + Const.HGT_EXT)
                if (hgtFile.exists()) {
                    val hgtReader = HgtReader(context, hgtFile)
                    val lllhRefreshed =
                        hgtReader.refreshRouteElevationFromSrtm(this.lllh)
                    if (lllhRefreshed.isNotNull() && lllhRefreshed.lllh.isNotNull()) {
                        this.lllh = lllhRefreshed.lllh!! as ArrayList<LatLngH>
                        this.state = true
                        hMax = hMax.coerceAtLeast(lllhRefreshed.hMax)
                    }
                }
            }
        } else
            this.state = true
        val interval = 750.0 // estimation 19jan2026, 1000.0
        val lllhKmSteps = this.lllh.getEquidistantPoints(interval) //1000.0)
        Timber.i( "lllhKmSteps: ${lllhKmSteps.size}")
        lllhKmSteps.forEachIndexed { i, llh ->
            if (i > 0) {
                var gradient: Double
                val deltaH: Double = lllhKmSteps[i].altitude - lllhKmSteps[i - 1].altitude
                gradient = 100 * deltaH / interval //1 km steps
                val c = interpolateColor((0.1 * abs(gradient)).toFloat())
                //Timber.i("bar value $i: " + "${llh.altitude} gradient: ${gradient.format(1)}")

                val iconGenerator = IconGenerator(context)
                val contentView = TextView(context)
                if (hMax > 10) {
                    contentView.text = String.format(
                        Locale.ENGLISH, "%s%.0f%s", Const.UC_ELE_ARROW, llh.altitude, "m")
                } else {
                    contentView.text = String.format(
                        Locale.ENGLISH, "%s%d%s", Const.UC_DISTANCE_ARROW, i, " km")
                }
                contentView.setBackgroundColor(c)
                contentView.setTextColor(android.graphics.Color.BLUE)
                contentView.textSize = 14f
                iconGenerator.setBackground(null)
                iconGenerator.setContentView(contentView)
                val bitmap = iconGenerator.makeIcon()
                var icon : BitmapDescriptor? = null
                bitmap.let { image ->
                    icon = BitmapDescriptorFactory.fromBitmap(image)
                }

                routeMarkerDataList.add(RouteMarkerData(lllhKmSteps[i], gradient, i.toDouble(), icon))
            }
        }
        this.routeMarkerDataList = routeMarkerDataList
        return hMax
    }

    fun RouteData.getRouteEntity(context: Context) : RouteEntity {
        val center = this.lllh.getCenter()
        val tileName =
            getTileName(center.latitude, center.longitude).uppercase()
        if (!this.state) {
            val demFolder =
                File(context.filesDir, Const.HGT_FOLDER_NAME)
            val hgtFile = File(demFolder, tileName + Const.HGT_EXT)
            if (hgtFile.exists()) {
                val hgtReader = HgtReader(context, hgtFile)
                val lllhRefreshed =
                    hgtReader.refreshRouteElevationFromSrtm(this.lllh).lllh as ArrayList<LatLngH>
                if (lllhRefreshed.isNotNull()) {
                    this.lllh = lllhRefreshed
                    this.state = true
                }
            }
        }
        val kmlString = this.lllh.lllhToKmlString(this.name)
        val routeEntity = RouteEntity(UUID.randomUUID(),
            this.name,
            "",
            this.lllh[0].latitude,
            this.lllh[0].longitude,
            center.latitude,
            center.longitude,
            this.lllh[lllh.lastIndex].latitude,
            this.lllh[lllh.lastIndex].longitude,

            this.distance,
            kmlString,
            null
        )
        return routeEntity
    }

fun RouteEntity.getRouteData(context: Context) : RouteData {
    var lllh = this.kmlString.kmlString2Lllh()
    val center = lllh.getCenter()
    val tileName =
        getTileName(center.latitude, center.longitude).uppercase()

    val demFolder =
        File(context.filesDir, Const.HGT_FOLDER_NAME)
    val hgtFile = File(demFolder, tileName + Const.HGT_EXT)
    if (hgtFile.exists()) {
        val hgtReader = HgtReader(context, hgtFile)
        val lllhRefreshed =
            hgtReader.refreshRouteElevationFromSrtm(lllh).lllh as ArrayList<LatLngH>
        if (lllhRefreshed.isNotNull()) {
            lllh = lllhRefreshed
        }
    }
    val routeData = RouteData(lllh, this.name, this.distance, state = true, null)
    return routeData
}
