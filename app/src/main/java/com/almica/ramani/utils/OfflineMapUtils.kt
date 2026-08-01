package com.almica.ramani.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.almica.ramani.Const
import com.almica.ramani.tilemaker.MbtilesDatabase
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegion.OfflineRegionDeleteCallback
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import timber.log.Timber
import java.io.File
import java.io.FileFilter
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.coroutines.resume

fun invalidateOfflineRegion(region: OfflineRegion?, callback: (Boolean) -> Unit) {
    // region tiles will be set to expired
    region?.invalidate(object : OfflineRegion.OfflineRegionInvalidateCallback {
        override fun onInvalidate() {
            Timber.i("invalidate OK")
            callback(true)
        }

        override fun onError(error: String) {
            Timber.i("invalidate error:$error")
            callback(false)
        }
    })
}

fun deleteOfflineRegion(region: OfflineRegion?, callback: (Boolean) -> Unit) {
    // changes region tables but not tiles
    region?.delete(object : OfflineRegionDeleteCallback {
        override fun onDelete() {
            Timber.i("delete OK")
            callback(true)
        }

        override fun onError(error: String) {
            Timber.i("delete error:$error")
            callback(false)
        }
    })
}

fun getBitmapForRegion(context: Context, regionName: String, createThumbnail: Boolean): Bitmap? {
    val dbName = "${regionName}${Const.MBTILES_EXT}"
    val splits = dbName.split(Const.UNDERLINE, limit = 5)
    if (splits.size > 3) {
        Timber.i("dbName: $dbName")
        val dbFile = MbtilesDatabase.DatabaseContext(context).getDatabasePath(dbName)
        if (dbFile.exists()) { // prevent database create
            val dbHelper = MbtilesDatabase.MbtilesHelper(context.applicationContext, dbName)
            try {
                val db = dbHelper.writableDatabase
                val row = 1023 - splits[2].toInt()
                val cursor = MbtilesDatabase.getTileBitmap(
                    db, splits[3].replace(Const.MBTILES_EXT, "").toInt(),
                    splits[1].toInt(), row
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val byteArray = it.getBlob(0)
                        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                        if (createThumbnail) {
                            val folderThumbnails = File(
                                context.filesDir,
                                Const.THUMBNAILS
                            )
                            val thumbnailFilename = "$regionName${Const.PNG_EXT}"
                            val thumbnailFile = File(folderThumbnails, thumbnailFilename)
                            val out = FileOutputStream(thumbnailFile)
                            bitmap.compress(
                                Bitmap.CompressFormat.PNG, 100, out
                            )
                            out.flush()
                            out.close()
                        }
                        return bitmap
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error reading MBTiles: $dbName")
            } finally {
                dbHelper.close()
            }
        } else {
            Timber.i("not found: ${dbFile.path}")
        }
        return null
    } else {
        Timber.i("invalid name: $dbName")
    }
    return null
}

suspend fun downloadBounds(
    context: Context,
    tileLimit: Long?,
    regionName: String,
    regionDefinition: OfflineTilePyramidRegionDefinition,
    onStatusChanged: (Boolean, Long, Long) -> Unit,
    mapboxTileCountLimitExceeded: (Long) -> Unit
): Result<Unit> {
    return suspendCancellableCoroutine { continuation ->
        Timber.i("regionName: $regionName tileLimit: $tileLimit")
        val offlineManager = OfflineManager.getInstance(context)
        tileLimit?.let { offlineManager.setOfflineMapboxTileCountLimit(it) }
        val metadata = "{name: $regionName}"
        val encodedMetadata = metadata.toByteArray()
        offlineManager.createOfflineRegion(
            definition = regionDefinition,
            metadata = encodedMetadata,
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) {
                    offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                    offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
                        override fun mapboxTileCountLimitExceeded(limit: Long) {
                            if (!continuation.isCompleted) {
                                continuation.resume(Result.failure(Throwable("mapboxTileCountLimitExceeded $limit")))
                            }
                            Timber.e("mapboxTileCountLimitExceeded $limit")
                            mapboxTileCountLimitExceeded(limit)
                        }

                        override fun onError(error: OfflineRegionError) {
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(Throwable(error.message)))
                            } else {
                                Timber.e("Continuation was completed already $error")
                            }
                        }

                        override fun onStatusChanged(status: OfflineRegionStatus) {
                            onStatusChanged(status.isComplete, status.completedTileCount, status.requiredResourceCount)
                            Timber.i(
                                "onStatusChanged downloadState= ${status.downloadState} isComplete= ${status.isComplete}," +
                                        "completedTileCount= ${status.completedTileCount} required= ${status.requiredResourceCount}"
                            )
                            if (status.isComplete) {
                                if (continuation.isActive) {
                                    continuation.resume(Result.success(Unit))
                                } else {
                                    Timber.e("Continuation was completed already")
                                }
                            }
                        }
                    })
                }

                override fun onError(error: String) {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Throwable(message = error)))
                    }
                }
            }
        )
    }
}

fun getRasterRegionNames(context: Context): ArrayList<String> {
    val rootFolder = context.filesDir
    val mbTilesRootFolder = File(rootFolder, Const.MBTILES_FOLDER)
    mbTilesRootFolder.mkdirs()
    val fileFilter = FileFilter { file: File? -> file?.name?.endsWith(Const.MBTILES_EXT) == true &&
            !file.name.contains(Const.JOURNAL)
    }
    val files: Array<File>? = mbTilesRootFolder.listFiles(fileFilter)
    val names = arrayListOf<String>()

    files?.let {
        it.sortWith(compareBy { it.name })
        it.forEach { file ->
            names.add(file.name)
        }
    }
    return names
}

fun getOfflineRegionsMap(context: Context, completed: (HashMap<String, OfflineRegion>?) -> Unit) {
    val regionsMap = hashMapOf<String, OfflineRegion>()
    val offlineManager = OfflineManager.getInstance(context)
    offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
        override fun onError(error: String) {
            Timber.e("Error: $error")
            completed(null)
        }

        override fun onList(offlineRegions: Array<OfflineRegion>?) {
            if (!offlineRegions.isNullOrEmpty()) {
                Timber.i("OfflineRegions: %s", offlineRegions.size)
                offlineRegions.forEach {
                    val name = getRegionName(it)
                    regionsMap[name] = it
                }
                completed(regionsMap)
            } else {
                Timber.e("offlineRegions.isNullOrEmpty")
                completed(null)
            }
        }
    })
}

fun getOfflineRegions(context: Context, completed: (Array<OfflineRegion>?) -> Unit) {
    Timber.i("getOfflineRegions")
    val offlineManager = OfflineManager.getInstance(context)
    offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
        override fun onError(error: String) {
            Timber.i("Error: $error")
            completed(null)
        }

        override fun onList(offlineRegions: Array<OfflineRegion>?) {
            if (!offlineRegions.isNullOrEmpty()) {
                Timber.i("OfflineRegions: %s", offlineRegions.size)
                completed(offlineRegions)
            } else {
                Timber.i("offlineRegions.isNullOrEmpty")
                completed(null)
            }
        }
    })
}

fun getRegionName(offlineRegion: OfflineRegion): String {
    return try {
        val metadata: ByteArray = offlineRegion.metadata
        val jsonMetadata = String(metadata, Charsets.UTF_8)
        val jsonObjectMetadata = JSONObject(jsonMetadata)
        jsonObjectMetadata.getString(Const.MBGL_METADATA_REGION_NAME)
    } catch (exception: Exception) {
        Timber.e("Failed to decode metadata:%s", exception.message)
        ""
    }
}

fun Double.formatLatLngShort(): String {
    val symbols = DecimalFormatSymbols(Locale.US)
    val decimalFormatter = DecimalFormat("#.00", symbols)
    return decimalFormatter.format(this)
}

fun Double.formatLatLng(): String {
    val symbols = DecimalFormatSymbols(Locale.US)
    val decimalFormatter = DecimalFormat("00.0000", symbols)
    return decimalFormatter.format(this)
}
