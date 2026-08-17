package com.almica.ramani

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Base64
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.Helpers.Companion.addLineToSnapshotWithGradient
import com.almica.ramani.Helpers.Companion.compressString
import com.almica.ramani.pdfcreator.createOverviewSnapshot
import com.almica.ramani.routes.drawRouteName
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.GeoJsonUtils.Companion.pointToTile
import com.almica.ramani.utils.getCenter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.time.Duration.Companion.milliseconds

class ListRouteFoldersViewModel(application: Application) : AndroidViewModel(application) {

    private val _routeFile = MutableStateFlow<File?>(null)
    val routeFile = _routeFile.asStateFlow()

    private val _routeSnapshot = MutableStateFlow<File?>(null)
    val routeSnapshot = _routeSnapshot.asStateFlow()

    private val _snapshotFeedback = MutableStateFlow<SnapshotFeedback?>(null)
    val snapshotFeedback = _snapshotFeedback.asStateFlow()

    private val _routeInfoFeedback = MutableStateFlow<RouteInfoFeedback?>(null)
    val routeInfoFeedback = _routeInfoFeedback.asStateFlow()

    private val _popupSnackMsg = MutableStateFlow<String?>(null)
    val popupSnackMsg = _popupSnackMsg.asStateFlow()

    private val _alertProgress = MutableStateFlow<String?>(null)
    val alertProgress = _alertProgress.asStateFlow()

    fun setRouteFile(file: File?) {
        _routeFile.value = file
    }

    fun setRouteSnapshot(file: File?) {
        Timber.i("setRouteSnapshot: $file")
        _routeSnapshot.value = file
    }

    fun clearSnapshotFeedback() {
        _snapshotFeedback.value = null
    }

    fun clearRouteInfoFeedback() {
        _routeInfoFeedback.value = null
    }

    fun clearPopupSnackMsg() {
        _popupSnackMsg.value = null
    }

    fun clearAlertProgress() {
        _alertProgress.value = null
    }

    fun showRouteInfoAlert(msg: String, file: File) {
        _routeInfoFeedback.value = RouteInfoFeedback(msg, file)
        _routeFile.value = null
    }

    fun setPopupSnackMsg(msg: String?) {
        _popupSnackMsg.value = msg
        if (msg != null) {
            viewModelScope.launch {
                delay(3000.milliseconds)
                if (_popupSnackMsg.value == msg) {
                    _popupSnackMsg.value = null
                }
            }
        }
    }

    fun processSingleSnapshot(file: File) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            Timber.i(context.getString(R.string.taking_snapshot, file.nameWithoutExtension))
            _alertProgress.value = context.getString(R.string.taking_snapshot, file.nameWithoutExtension)
            delay(1000.milliseconds)

            if (file.extension == Const.GEOJSON_EXT.replace(".", "")) {
                val feedback = takeGeojsonSnapshot(context, file, override = true)
                if (feedback != null) {
                    _snapshotFeedback.value = feedback
                } else {
                    setPopupSnackMsg(context.getString(R.string.take_snapshot_ready, file.nameWithoutExtension) + " ERROR")
                }
            } else {
                val feedback = takeSnapShot(context, file)
                _snapshotFeedback.value = feedback
            }
            _alertProgress.value = null
        }
    }

    fun processFolderSnapshots(folder: File) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val folderThumbnails = File(context.filesDir, Const.THUMBNAILS)
            val files = folder.listFiles { f ->
                f.isFile && (f.extension == Const.GPX_EXT.replace(".", "") ||
                        f.extension == Const.KML_EXT.replace(".", "") ||
                        f.extension == Const.GEOJSON_EXT.replace(".", ""))
            }
            Timber.i("${folder.path} files: ${files?.size}")
            files?.forEachIndexed { index, file ->
                _alertProgress.value = context.getString(R.string.taking_snapshot, file.nameWithoutExtension).plus(" ($index / ${files.size})")

                delay(500.milliseconds)

                try {
                    if (file.extension == Const.GEOJSON_EXT.replace(".", "")) {
                        takeGeojsonSnapshot(context, file)
                    } else {
                        val snapShotFile1 = File(folderThumbnails, file.nameWithoutExtension.plus(Const.JPG_EXT))
                        val snapShotFile2 = File(file.parentFile, file.nameWithoutExtension.plus(Const.JPG_EXT))
                        if (!snapShotFile1.exists() && !snapShotFile2.exists()) {
                            Timber.i(context.getString(R.string.taking_snapshot, file.nameWithoutExtension))
                            takeSnapShot(context, file)
                        } else {
                            Timber.i("snapshot already exists: ${file.nameWithoutExtension}")
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error processing snapshot for ${file.name}")
                }
            }

            setPopupSnackMsg(context.getString(R.string.refresh_route_preview_ready, folder.name))
            _alertProgress.value = null
        }
    }

    private suspend fun takeGeojsonSnapshot(context: Context, geojsonFile: File, override: Boolean = false): SnapshotFeedback? {
        Timber.i("takeGeojsonSnapshot: ${geojsonFile.path}")
        val thumbnailsFolder = File(context.filesDir, Const.THUMBNAILS)
        if (!thumbnailsFolder.exists()) {
            val b = thumbnailsFolder.mkdirs()
            Timber.i("${thumbnailsFolder.path} mkdirs: $b")
        }
        val snapshotFile = File(thumbnailsFolder, geojsonFile.name.replace(Const.GEOJSON_EXT, Const.JPG_EXT))
        if (snapshotFile.exists() && !override) return SnapshotFeedback(context.getString(R.string.snapshot_already_exists), null, snapshotFile)
        val geojsonString = geojsonFile.inputStream().bufferedReader().use { it.readText() }
        val deferred = CompletableDeferred<SnapshotFeedback?>()
        val res = createOverviewSnapshot(context, geojsonString, geojsonFile.nameWithoutExtension, null, result = { bitmap, center ->
            if (bitmap != null) {
                val bmp: Bitmap = createBitmap(bitmap.width, bitmap.height + 32)
                val thumbCanvas = Canvas(bmp)
                thumbCanvas.drawColor(android.graphics.Color.WHITE)
                thumbCanvas.drawBitmap(bitmap, 0f, 0f, null)
                drawRouteName(context, thumbCanvas, geojsonFile.nameWithoutExtension)
                Timber.i("snapshotFile: ${snapshotFile.path}")
                try {
                    FileOutputStream(snapshotFile).use { out ->
                        bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        out.flush()
                    }
                    val exifInterface = ExifInterface(snapshotFile.path)
                    exifInterface.setLatLong(center.latitude, center.longitude)
                    exifInterface.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION,
                        "${Const.GEOJSON_ROOT_FOLDER} ${snapshotFile.name}")
                    exifInterface.setAttribute(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL.toString()
                    )

                    val processedString = compressString(geojsonString)

                    if (processedString.length < Const.EXIF_MAX_SIZE) {
                        Timber.i("geojson processedString: ${processedString.length}")
                        exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, processedString)
                    } else {
                        Timber.w("geojsonString (even compressed) too large for EXIF: ${processedString.length} > ${Const.EXIF_MAX_SIZE}")
                    }
                    exifInterface.saveAttributes()
                    deferred.complete(SnapshotFeedback(
                        message = context.getString(R.string.take_snapshot_ready, geojsonFile.nameWithoutExtension),
                        thumbnail = bmp,
                        routeFile = geojsonFile
                    ))
                } catch (e: Exception) {
                    Timber.e(e, "Failed to save snapshot or EXIF attributes")
                    deferred.complete(null)
                }
            } else {
                deferred.complete(null)
            }
        })
        if (res == null) return null
        return deferred.await()
    }

    private suspend fun takeSnapShot(context: Context, routeFile: File): SnapshotFeedback? {
        Timber.i("takeSnapShot: ${routeFile.path}")
        val lllh =
            if (routeFile.extension == Const.JPG_EXT) {
                Helpers.getCoordinatesFromExif(routeFile)
            } else
                Helpers.getLllhFromFile(routeFile)
        if (lllh.isNullOrEmpty()) {
            Timber.e("${routeFile.name} lllh isNullOrEmpty")
            return SnapshotFeedback(context.getString(R.string.no_coordinates, routeFile.name), null, routeFile)
        }

        val deferred = CompletableDeferred<SnapshotFeedback?>()
        val routeCenter = lllh.getCenter()
        val mvtTileMatch: GeoJsonUtils.Companion.Tile =
            pointToTile(routeCenter.longitude, routeCenter.latitude, 9.0)
        Timber.i("$routeCenter mvtTileMatch: $mvtTileMatch")
        val mvtMatchingMap = "${Const.MVT_PREFIX}${mvtTileMatch.x}_${mvtTileMatch.y}_${mvtTileMatch.z}"
        val mvtRootFolder = File(context.filesDir, Const.MVT_FOLDER)
        val mvtMatchingFile = File(mvtRootFolder, mvtMatchingMap.plus(Const.MBTILES_EXT))
        Timber.i("mvtMatchingFile: ${mvtMatchingFile.path}")
        val preferences = getDefaultSharedPreferences(context)
        val mvtCurrentPath = preferences.getString(Const.PREF_MVT_FILEPATH, null)
        var baseMapChange = false
        if (mvtMatchingFile.exists() && mvtMatchingFile.path != mvtCurrentPath) {
            preferences.edit { putString(Const.PREF_MVT_FILEPATH, mvtMatchingFile.path) }
            Timber.i("pref ${Const.PREF_MVT_FILEPATH} changed: ${mvtMatchingFile.path}")
            baseMapChange = true
        }
        Helpers.takeRouteSnapshot(context, lllh, routeFile.nameWithoutExtension, Const.styleVectorUri, 512, 0.1, true,
            routeFile.parentFile)
        { snapShot, _ ->
            Timber.i("takeLocationsSnapshot ready")
            if (snapShot != null) {
                addLineToSnapshotWithGradient(snapShot, lllh)

                val snackTitle = StringBuilder(context.getString(R.string.refresh_route_preview_ready, routeFile.nameWithoutExtension))
                if (baseMapChange) {
                    snackTitle.append("\n")
                        .append(context.getString(R.string.vector_map_changed_to_, mvtMatchingFile.name))
                } else if (mvtMatchingFile.exists()) {
                    snackTitle.append("\n")
                        .append(context.getString(R.string.vector_map_used_, mvtMatchingFile.name))
                } else {
                    snackTitle.append("\n")
                        .append(context.getString(R.string.vector_map_missing_, mvtMatchingFile.name))
                }
                deferred.complete(SnapshotFeedback(snackTitle.toString(), snapShot.bitmap, routeFile))
            } else {
                deferred.complete(null)
            }
        }
        return deferred.await()
    }
}
