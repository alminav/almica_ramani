package com.almica.ramani.googlemaps

import android.content.Context
import com.almica.ramani.Const
import com.almica.ramani.Helpers.Companion.getTileName
import com.almica.ramani.LatLngH
import com.almica.ramani.routes.RouteEntity
import com.almica.ramani.utils.HgtReader
import com.almica.ramani.utils.getCenter
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.utils.kmlString2Lllh
import com.almica.ramani.utils.lllhToKmlString
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import timber.log.Timber
import java.io.File
import java.util.UUID

data class RouteData(
    var lllh: List<LatLngH>,
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
                    hgtReader.refreshRouteElevationFromSrtm(this.lllh).lllh
                if (lllhRefreshed.isNotNull()) {
                    this.lllh = lllhRefreshed!!
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
            hgtReader.refreshRouteElevationFromSrtm(lllh).lllh
        if (lllhRefreshed.isNotNull()) {
            lllh = lllhRefreshed!!
        }
    }
    val routeData = RouteData(lllh, this.name, this.distance, state = true, null)
    return routeData
}
