package com.almica.ramani.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.almica.ramani.Const
import com.almica.ramani.R
import com.google.android.gms.maps.model.LatLng
import com.graphhopper.GraphHopper
import com.graphhopper.util.PointList
import timber.log.Timber
import java.io.File
private const val logtag = "GhHelper"
class GhHelper {
    companion object {
/*
        fun setSrtmValuesInGhResponse(
            context: Context?,
            pointList: PointList,
            tileName: String
        ): PointList? {
            if (context != null) {
                val demFolder =
                    File(context.filesDir, Const.HGT_FOLDER_NAME)
                Log.i(this::class.java.simpleName, "demFolder: " + demFolder.path + " " + tileName)
                val hgtFile = File(demFolder, tileName + Const.HGT_EXT)
                if (hgtFile.exists()) {
                    val pointList3d = PointList(pointList.size(), true)
                    val hgtReader = HgtReader(hgtFile)
                    for (i in 0 until pointList.size()) {
                        val latLng = LatLng(pointList.getLat(i), pointList.getLon(i))
                        val newAlti = hgtReader.getElevationFromHgt(latLng)
                        if (newAlti != HgtReader.NO_ELEVATION) pointList3d.add(
                            latLng.latitude,
                            latLng.longitude,
                            newAlti
                        )
                        else pointList3d.add(latLng.latitude, latLng.longitude, 0.0)
                    }
                    return pointList3d
                }

                return null
            }
            return null
        }
*/
        private fun getGhFolder(context: Context): String? {
            //val ghFolder = File(context.filesDir, Const.GH_TAG)
            //val ghDefaultFile = File(ghFolder, "n52e0103d")
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val ghFilePath = prefs.getString(Const.PREF_GH_FILEPATH, null)
            if (ghFilePath != null) {
                return if (File(ghFilePath).exists())
                    ghFilePath
                else {
                    Timber.e("$ghFilePath not found")
                    null
                }
            }
            return null
        }

        fun getGhFilename(context: Context): String? {
            val ghFolder = getGhFolder(context)
            if (ghFolder != null) {
                val ghFile = File(ghFolder)
                if (ghFile.exists()) return ghFile.name
            }
            Timber.i("GH folder error $ghFolder")
            return null
        }

        fun getGhManager(context: Context): GhManager? {
            val ghPath = getGhFolder(context)
            Timber.i("ghPath: $ghPath")
            if (ghPath != null) {
                return GhManager.getInstance(context, ghPath, mGhListener)
            } else {
                Timber.i("GH folder error")
                return GhManager.getInstance(context, null, mGhListener)
                //return null
            }
        }

        fun getVehicleIconFromPref(context: Context): Int {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val s1s2 = sharedPreferences.getString(
                context.getString(R.string.setting_locomotion),
                Const.DEFAULT_LOCOMOTION
            )

            val drawable = intArrayOf(
                R.drawable.ic_directions_walk_black_24dp,
                R.drawable.ic_directions_bike_black_24dp,
                R.drawable.ic_directions_bike_fast_black_24dp,
                R.drawable.ic_directions_car_black_24dp,
                R.drawable.ic_directions_car_fast_black_24dp,
                R.drawable.ic_directions_airplane_24_black
            )
            when (s1s2) {
                "0.0" -> return drawable[0]
                "0.1" -> return drawable[0]
                "1.1" -> return drawable[1]
                "1.0" -> return drawable[2]
                "2.1" -> return drawable[3]
                "2.0" -> return drawable[4]
                "3.1" -> return drawable[5]
                "3.0" -> return drawable[5]
            }
            return -1
        }

        fun getVehicleIcon(context: Context, s1s2: String): Int {
            val drawable = intArrayOf(
                R.drawable.ic_directions_walk_black_24dp,
                R.drawable.ic_directions_bike_black_24dp,
                R.drawable.ic_directions_bike_fast_black_24dp,
                R.drawable.ic_directions_car_black_24dp,
                R.drawable.ic_directions_car_fast_black_24dp,
                R.drawable.ic_directions_airplane_24_black
            )
            when (s1s2) {
                "0.0" -> return drawable[0]
                "0.1" -> return drawable[0]
                "1.1" -> return drawable[1]
                "1.0" -> return drawable[2]
                "2.1" -> return drawable[3]
                "2.0" -> return drawable[4]
                "3.1" -> return drawable[5]
                "3.0" -> return drawable[5]
            }
            return -1
        }
        fun getVehicleDescriptionFromPref(context: Context): String? {
            var sResult: String? = null
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val s1_s2 = sharedPreferences.getString(
                context.getString(R.string.setting_locomotion),
                Const.DEFAULT_LOCOMOTION
            )

            val description = arrayOf(
                context.getString(R.string.pedestrian),
                context.getString(R.string.bicycle_short),
                context.getString(R.string.bicycle_fast),
                context.getString(R.string.car_short),
                context.getString(R.string.car_fast),
                context.getString(R.string.airplane)
            )
            when (s1_s2) {
                "0.0" -> return description[0]
                "0.1" -> sResult = description[0]
                "1.1" -> sResult = description[1]
                "1.0" -> sResult = description[2]
                "2.1" -> sResult = description[3]
                "2.0" -> sResult = description[4]
                "3.1" -> sResult = description[5]
                "3.0" -> sResult = description[5]
            }
            Timber.i("sResult: $sResult")
            return sResult
        }

        fun getVehicleDescription(context: Context, s1s2: String): String? {
            var sResult: String? = null
            val description = arrayOf(
                context.getString(R.string.pedestrian),
                context.getString(R.string.bicycle_short),
                context.getString(R.string.bicycle_fast),
                context.getString(R.string.car_short),
                context.getString(R.string.car_fast),
                context.getString(R.string.airplane)
            )
            when (s1s2) {
                "0.0" -> return description[0]
                "0.1" -> sResult = description[0]
                "1.1" -> sResult = description[1]
                "1.0" -> sResult = description[2]
                "2.1" -> sResult = description[3]
                "2.0" -> sResult = description[4]
                "3.1" -> sResult = description[5]
                "3.0" -> sResult = description[5]
            }
            Timber.i("sResult: $sResult")
            return sResult
        }

        var mGhListener: GhManager.InitListener = object : GhManager.InitListener {
            override fun completeOk(tmpHopp: GraphHopper?, context: Context) {
                Timber.i(
                    context.getString(
                        R.string.gh_load_ok,
                        getGhFilename(context)
                    )
                )
            }

            override fun completeNok(msg: String?, context: Activity?) {
                if (context != null) {
                    Timber.i(
                        context.getString(
                            R.string.gh_load_error,
                            getGhFilename(context)
                        )
                    )
                }
                if (context != null) {
                    Timber.i(context.getString(R.string.gh_load_error, getGhFilename(context)))
                }

            }

            override fun progress(fileName: String?) {
                if (fileName != null) {
                    Timber.i(fileName)
                }
            }

            override fun ghInitStarted(context: Context) {
                Timber.i(
                    context.getString(
                        R.string.gh_initialization,
                        context.let { getGhFilename(it) }
                    )
                )
            }
        }

    }
}