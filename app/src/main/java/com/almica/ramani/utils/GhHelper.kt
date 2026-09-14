package com.almica.ramani.utils

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.preference.PreferenceManager
import com.almica.ramani.Const
import com.almica.ramani.R
import com.google.android.gms.maps.model.LatLng
import com.graphhopper.GraphHopper
import com.graphhopper.util.PointList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

private const val logtag = "GhHelper"
class GhHelper {
    companion object {
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

        suspend fun unzipFile(
            context: Context,
            zipUri: Uri,
            targetFolder: File,
            createSubfolder: Boolean = false,
            extensionFilter: String? = null,
            flatten: Boolean = false
        ) = withContext(Dispatchers.IO) {
            try {
                val inputStream =
                    context.contentResolver.openInputStream(zipUri) ?: return@withContext
                ZipInputStream(inputStream).use { zipInput ->
                    val baseDir = if (createSubfolder) {
                        val zipFileName = zipUri.lastPathSegment ?: "extracted"
                        val folderName = if (zipFileName.contains(".")) {
                            zipFileName.substringBeforeLast(".")
                        } else {
                            zipFileName
                        }
                        File(targetFolder, folderName).also { it.mkdirs() }
                    } else {
                        targetFolder.also { it.mkdirs() }
                    }

                    var entry = zipInput.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && (extensionFilter == null || entry.name.lowercase()
                                .endsWith(extensionFilter))
                        ) {
                            val entryName = if (flatten) File(entry.name).name else entry.name
                            val targetFile = File(baseDir, entryName)
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { output ->
                                zipInput.copyTo(output)
                            }
                        } else if (entry.isDirectory && !flatten) {
                            File(baseDir, entry.name).mkdirs()
                        }
                        zipInput.closeEntry()
                        entry = zipInput.nextEntry
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error unzipping file $zipUri")
                throw e
            }
        }

        suspend fun unzipGhFile(context: Context, zipUri: Uri, targetFolder: File) =
            unzipFile(context, zipUri, targetFolder, createSubfolder = true, flatten = false)

    }
}