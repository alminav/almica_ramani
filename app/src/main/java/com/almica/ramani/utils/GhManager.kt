package com.almica.ramani.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.almica.ramani.Const
import com.almica.ramani.Helpers
import com.almica.ramani.Helpers.Companion.getTileName
import com.almica.ramani.R
import com.almica.ramani.utils.isNotNull
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.graphhopper.GHRequest
import com.graphhopper.GHResponse
import com.graphhopper.GraphHopper
import com.graphhopper.routing.util.BikeFlagEncoder
import com.graphhopper.routing.util.EdgeFilter
import com.graphhopper.routing.util.EncodingManager
import com.graphhopper.storage.DataAccess
import com.graphhopper.storage.Directory
import com.graphhopper.storage.RAMDirectory
import com.graphhopper.util.Helper
import com.graphhopper.util.PointList
import com.graphhopper.util.shapes.GHPoint
import com.graphhopper.util.shapes.GHPoint3D
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.util.Locale

class GhManager internal constructor(context: Context, private var mInitListener: InitListener?) {
    private val mGh: GraphHopper
    private var mbGh3d = false

    fun setInitListener(mInitListener: InitListener?) {
        this.mInitListener = mInitListener
    }

    var errorMsg: String? = null
        private set
    private var mInitError = false

    init {
        mGh = initGraphhopper(context)
    }

    private fun initGraphhopper(context: Context): GraphHopper {
        val tmpHopp = GraphHopper(context).forMobile()
        var graphExists = false
        if (mGhFolderPath != null) {
            Timber.i(mGhFolderPath!!)
            graphExists = mGhFolderPath?.let { File(it).exists() } == true
        }
        if (graphExists == true) {
            val bGhShortcut = checkGHShortcut(RAMDirectory(mGhFolderPath, true))
            val acceptWay = checkGHVehicle(RAMDirectory(mGhFolderPath, true))
            Timber.i("acceptWay: $acceptWay")
            // 30aug2024
            mbGh3d = checkGH3d(RAMDirectory(mGhFolderPath, true))
            Timber.i("gh shortcut $bGhShortcut")
            Timber.i("gh 3d $mbGh3d")
            if (!bGhShortcut) tmpHopp.disableCHShortcuts()
            tmpHopp.setElevation(mbGh3d)

            Timber.i("tmpHopp.load %s", mGhFolderPath)
            // no shortcut
            //String acceptWay = "bike:com.graphhopper.routing.util.BikeFlagEncoder,car:com.graphhopper.routing.util.CarFlagEncoder,foot:com.graphhopper.routing.util.FootFlagEncoder";
            tmpHopp.setEncodingManager(EncodingManager(acceptWay, 4))
            val bResult = tmpHopp.load(mGhFolderPath) // 03mai2015
            if (!bResult) {
                errorMsg = context.getString(R.string.gh_load_error, mGhFolderPath)
                mInitError = true
            } else {
                Timber.i(context.getString(R.string.gh_load_ok, mGhFolderPath))
                if (mInitListener != null) mInitListener!!.completeOk(tmpHopp, context)
            }
        } else {
            Timber.i(context.getString(R.string.gh_folder_not_found, mGhFolderPath))
            errorMsg = context.getString(R.string.gh_folder_not_found, mGhFolderPath)
            mInitError = true
        }
        return tmpHopp
    }

    fun destroy() {
        Timber.i("")
        ghManager = null
    }

    fun getClosestEdge(lat: Double, lon: Double): String? {
        val ghLocationIndex = mGh.locationIndex
        val queryResult = ghLocationIndex.findClosest(lat, lon, EdgeFilter.ALL_EDGES)
        //Log.i(logtag, "${Thread.currentThread().getStackTrace()[2].lineNumber}: ${queryResult.closestEdge.name}")
        var closestEdge = queryResult.closestEdge
        return if (closestEdge != null) queryResult.closestEdge.name else ""
    }

    fun startRequest(
        context: Context,
        startY: Double,
        startX: Double,
        stopY: Double,
        stopX: Double
    ): GHResponse {
        Timber.i("start $startY $startX")
        Timber.i("stop $stopY $stopX")

        val vehicle_weighting = getVehicle(context)
        if (mGhFolderPath != null  && vehicle_weighting != null && vehicle_weighting[0] != EncodingManager.AIRPLANE) {
            val ghRequest = GHRequest(
                startY, startX,
                stopY, stopX
            ).setAlgorithm("dijkstrabi")
                .putHint("douglas.minprecision", 1)
                .putHint("instructions", true)
                .setLocale(Locale.getDefault())
                .setWeighting(vehicle_weighting[1])
                .setVehicle(vehicle_weighting[0])
            val ghResponse = mGh.route(ghRequest)
            // 31aug2024
/*
            if (!mbGh3d) {
                val routeCenter =
                    SphericalUtil.interpolate(LatLng(startY, startX), LatLng(stopY, stopX), 0.5)
                val tileName = Helpers.getTileName(org.maplibre.android.geometry.LatLng(routeCenter.latitude, routeCenter.longitude))
                val pointList3d: PointList? =
                    GhHelper.setSrtmValuesInGhResponse(context, ghResponse.points, tileName)
                if (pointList3d != null) ghResponse.setPoints(pointList3d)
            }
 */
            return ghResponse
        } else
            return getBeeLine(context, startY, startX, stopY, stopX)
    }

    /**
     * AI 05aug2025
     * I have created the startRoundTripRequest function in GhManager.kt.
     * This function calculates a route starting from a specified point,
     * going through points perpendicular to the center of the connecting line and the "stop" point,
     * and returning to the start point.
     * ToDo make factor 0.2 in offsetPoints variable
     */
    fun startRoundTripRequest(
        context: Context,
        startY: Double,
        startX: Double,
        stopY: Double,
        stopX: Double
    ): GHResponse {
        Timber.i("round trip: start $startY $startX stop $stopY $stopX")

        val vehicle_weighting = getVehicle(context)
        if (mGhFolderPath != null && vehicle_weighting != null && vehicle_weighting[0] != EncodingManager.AIRPLANE) {
            val startLatLng = LatLng(startY, startX)
            val stopLatLng = LatLng(stopY, stopX)
            val distanceStopStart = SphericalUtil.computeDistanceBetween(startLatLng, stopLatLng)
            // Calculate a point perpendicular to the center of the line
            val heading = SphericalUtil.computeHeading(startLatLng, stopLatLng)
            val midPoint = SphericalUtil.interpolate(startLatLng, stopLatLng, 0.5)
            val offsetPoint1 = SphericalUtil.computeOffset(midPoint, 0.2 * distanceStopStart, heading + 90.0)
            val offsetPoint2 = SphericalUtil.computeOffset(midPoint, 0.2 * distanceStopStart, heading - 90.0)

            val ghRequest = GHRequest(5)
                .addPoint(GHPoint(startY, startX))
                .addPoint(GHPoint(offsetPoint1.latitude, offsetPoint1.longitude))
                .addPoint(GHPoint(stopY, stopX))
                .addPoint(GHPoint(offsetPoint2.latitude, offsetPoint2.longitude))
                .addPoint(GHPoint(startY, startX))
                .setAlgorithm("dijkstrabi")
                .putHint("douglas.minprecision", 1)
                .putHint("instructions", true)
                .setLocale(Locale.getDefault())
                .setWeighting(vehicle_weighting[1])
                .setVehicle(vehicle_weighting[0])
            return mGh.route(ghRequest)
        } else {
            val res1 = getBeeLine(context, startY, startX, stopY, stopX)
            val res2 = getBeeLine(context, stopY, stopX, startY, startX)

            val combinedPoints = PointList(res1.points.size() + res2.points.size(), mbGh3d)
            combinedPoints.add(res1.points)
            for (i in 1 until res2.points.size()) {
                if (mbGh3d)
                    combinedPoints.add(res2.points.getLatitude(i), res2.points.getLongitude(i), res2.points.getElevation(i))
                else
                    combinedPoints.add(res2.points.getLatitude(i), res2.points.getLongitude(i))
            }

            val ghResponse = GHResponse()
            ghResponse.setPoints(combinedPoints)
            ghResponse.setDistance(res1.distance + res2.distance)
            ghResponse.setFound(true)
            return ghResponse
        }
    }
    fun getBeeLine(context: Context,
        startY: Double,
        startX: Double,
        stopY: Double,
        stopX: Double
    ): GHResponse {
        Timber.i("startX: $startX startY: $startY")
        val tileName = getTileName(stopY, stopX).uppercase()
        val demFolder = File(context.filesDir, Const.HGT_FOLDER_NAME)
        val hgtFile = File(demFolder, tileName + Const.HGT_EXT)
        var hgtReader: HgtReader? = null
        if (hgtFile.exists())
            hgtReader = HgtReader(context, hgtFile)
        val llStart = LatLng(startY, startX)
        val llStop = LatLng(stopY, stopX)
        val pointList = PointList(11, mbGh3d)
        for (i in 0..10) {
            val llInterPol = SphericalUtil.interpolate(llStart, llStop, 0.1 * i)
            pointList.add(GHPoint3D(llInterPol.latitude, llInterPol.longitude,
                hgtReader?.getElevationFromHgt(LatLng(llInterPol.latitude, llInterPol.longitude)) ?: 0.0)
            )
        }
        val ghResponse = GHResponse()
        ghResponse.setPoints(pointList)
        val dist = SphericalUtil.computeDistanceBetween(llStart, llStop)
        ghResponse.setDistance(dist)
        return ghResponse
    }

    fun getVehicle(context: Context): Array<String>? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val s1_s2 = sharedPreferences.getString(
            context.getString(R.string.setting_locomotion),
            Const.DEFAULT_LOCOMOTION
        )
        Timber.i(s1_s2!!)
        if (mGhFolderPath != null &&
            mGhFolderPath!!.contains(Const.GERMANY)
        ) {
            when (s1_s2) {
                "0.1", "0.0", "1.1", "1.0", "2.1" -> return arrayOf(
                    Const.Companion.VehicleEncoding.CAR_ENCODING,
                    Const.Companion.WeightingEncoding.SHORT_ENCODING
                )

                "2.0" -> return arrayOf(
                    Const.Companion.VehicleEncoding.CAR_ENCODING,
                    Const.Companion.WeightingEncoding.FAST_ENCODING
                )

                else -> return arrayOf(
                    Const.Companion.VehicleEncoding.AIRPLANE_ENCODING,
                    Const.Companion.WeightingEncoding.SHORT_ENCODING
                )
            }
        }
        when (s1_s2) {
            "0.1", "0.0" -> return arrayOf(
                Const.Companion.VehicleEncoding.FOOT_ENCODING,
                Const.Companion.WeightingEncoding.SHORT_ENCODING
            )

            "1.1" -> return arrayOf(
                Const.Companion.VehicleEncoding.BIKE_ENCODING,
                Const.Companion.WeightingEncoding.SHORT_ENCODING
            )

            "1.0" -> return arrayOf(
                Const.Companion.VehicleEncoding.BIKE_ENCODING,
                Const.Companion.WeightingEncoding.FAST_ENCODING
            )

            "2.1" -> return arrayOf(
                Const.Companion.VehicleEncoding.CAR_ENCODING,
                Const.Companion.WeightingEncoding.SHORT_ENCODING
            )

            "2.0" -> return arrayOf(
                Const.Companion.VehicleEncoding.CAR_ENCODING,
                Const.Companion.WeightingEncoding.FAST_ENCODING
            )

            "3.0", "3.1" -> return arrayOf(
                Const.Companion.VehicleEncoding.AIRPLANE_ENCODING,
                Const.Companion.WeightingEncoding.SHORT_ENCODING
            )
            else -> return arrayOf(
                Const.Companion.VehicleEncoding.BIKE_ENCODING,
                Const.Companion.WeightingEncoding.SHORT_ENCODING
            )
        }
    }

    fun hasInitError(): Boolean {
        Timber.i("mInitError: $mInitError")
        return mInitError
    }

    interface InitListener {
        fun completeOk(tmpHopp: GraphHopper?, context: Context)
        fun completeNok(msg: String?, context: Activity?)
        fun progress(fileName: String?)

        fun ghInitStarted(context: Context)
    }

    companion object {
        private const val logtag = "GhManager"
        val GH_FILES_COUNT: Int = 6
        private var ghManager: GhManager? = null
        private val myLog = MyLog(false, logtag)
        private var mGhFolderPath: String? = null

        fun getInstance(
            context: Context,
            ghFolderPath: String?,
            initListener: InitListener?
        ): GhManager? {
            if (ghManager == null || mGhFolderPath != ghFolderPath) {
                mGhFolderPath = ghFolderPath
                ghManager = GhManager(context, initListener)
                initListener?.ghInitStarted(context)
            } else {
                initListener?.completeOk(ghManager!!.mGh, context)
            }
            return ghManager
        }

        private fun checkGH3d(dir: Directory): Boolean {
            Timber.i("")
            val map: Map<String, String> = LinkedHashMap()
            val da: DataAccess
            da = dir.find("properties")
            // reduce size
            da.setSegmentSize(1 shl 15)
            if (!da.loadExisting()) {
                da.close()
                return false
            }

            val len = da.capacity.toInt()
            Timber.i("len $len")
            val bytes = ByteArray(len)
            da.getBytes(0, bytes, len)
            try {
                Helper.loadProperties(map, StringReader(String(bytes, Helper.UTF_CS)))
            } catch (ex: IOException) {
                throw IllegalStateException(ex)
            }

            val ret = map["graph.dimension"]
            if (ret != null && ret.equals("3", ignoreCase = true)) {
                Timber.i("graph.dimension: 3")
                da.close()
                return true
            } else {
                Timber.i("graph.dimension: 2")
                da.close()
                return false
            }
        }

        private fun checkGHShortcut(dir: Directory): Boolean {
            Timber.i("")
            val map: Map<String, String> = LinkedHashMap()
            val da: DataAccess
            da = dir.find("properties")
            // reduce size
            da.setSegmentSize(1 shl 15)
            if (!da.loadExisting()) {
                da.close()
                return false
            }

            val len = da.capacity.toInt()
            Timber.i("len $len")
            val bytes = ByteArray(len)
            da.getBytes(0, bytes, len)
            try {
                Helper.loadProperties(map, StringReader(String(bytes, Helper.UTF_CS)))
            } catch (ex: IOException) {
                throw IllegalStateException(ex)
            }

            val ret = map["prepare.done"]
            if (ret != null && ret.equals("true", ignoreCase = true)) {
                Timber.i("prepare.done true")
                da.close()
                return true
            } else {
                Timber.i("prepare.done false")
                da.close()
                return false
            }
        }

        private fun checkGHVehicle(dir: Directory): String? {
            Timber.i("checkGHVehicle")
            val map: MutableMap<String, String> = LinkedHashMap()
            val da: DataAccess = dir.find("properties")
            // reduce size
            da.setSegmentSize(1 shl 15)
            if (!da.loadExisting()) {
                da.close()
                return null
            }

            val len = da.capacity.toInt()
            Timber.i("len $len")
            val bytes = ByteArray(len)
            da.getBytes(0, bytes, len)
            try {
                Helpers.loadProperties(map, StringReader(String(bytes, Helper.UTF_CS)))
            } catch (ex: IOException) {
                throw IllegalStateException(ex)
            }
            val dimension = map["graph.dimension"]
            if (dimension != null) Timber.i("graph.dimension: $dimension")
            val ret = map["osmreader.acceptWay"]
            if (ret != null) Timber.i("osmreader.acceptWay: $ret")
            return ret
        }
    }
}
