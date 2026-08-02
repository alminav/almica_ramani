package com.almica.ramani

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.Helpers.Companion.addLineToSnapshotWithGradient
import com.almica.ramani.navigation.RamaniApp
import com.almica.ramani.pdfcreator.createOverviewSnapshot
import com.almica.ramani.routes.RouteDialogMode
import com.almica.ramani.routes.RouteEntity
import com.almica.ramani.routes.drawRouteName
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.GeoJsonUtils.Companion.pointToTile
import com.almica.ramani.utils.getCenter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.time.Duration.Companion.milliseconds


class ListRouteFoldersActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val resources = LocalResources.current
            val dialogModeOrdinal = intent.getIntExtra(Const.EXTRA_ROUTE_DIALOG_MODE, RouteDialogMode.Admin.ordinal)
            Timber.i("dialogModeOrdinal: $dialogModeOrdinal")
            var routeFile: File? by remember { mutableStateOf(null) }
            var routeFileForSnapshot: File? by remember { mutableStateOf(null) }
            var routeFolderForSnapshots: File? by remember { mutableStateOf(null) }
            var alertSnapshotFeedback: Triple<String, Bitmap?, File>? by remember {
                mutableStateOf(
                    null
                )
            }
            var popupSnackMsg: String? by remember { mutableStateOf(null) }
            LaunchedEffect(key1 = popupSnackMsg) {
                Timber.i( "LaunchedEffect $popupSnackMsg")
                delay(3000.milliseconds)
                popupSnackMsg = null
            }
            popupSnackMsg?.let { msg ->
                Popup(properties = PopupProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
                    alignment = Alignment.Center,
                    onDismissRequest = {
                        popupSnackMsg = null
                    }) {
                    Surface(
                        color = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 4.dp,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            var alertProgress: String? by remember { mutableStateOf(null) }
            LaunchedEffect(key1 = routeFileForSnapshot) {
                routeFileForSnapshot?.let {
                    Timber.i(resources.getString(R.string.taking_snapshot, it.nameWithoutExtension))
                    alertProgress =
                        resources.getString(R.string.taking_snapshot, it.nameWithoutExtension)
                    delay(1000.milliseconds)
                    // with Dispatchers.IO Error -->
                    //      org.maplibre.android.exceptions.CalledFromWorkerThreadException:
                    //      Mbgl-Source interactions should happen on the UI thread.
                    if (it.extension == Const.GEOJSON_EXT.replace(".", "")) {
                        val success = takeGeojsonSnapshot(context, it, override = true)
                        popupSnackMsg = resources.getString(R.string.take_snapshot_ready,
                            it.nameWithoutExtension) +
                                if (success) " OK" else " ERROR"
                    } else {
                        val feedback = takeSnapShot(context = context, routeFile = it)
                        alertSnapshotFeedback = feedback
                    }
                    routeFileForSnapshot = null
                    alertProgress = null
                } ?: Timber.i("routeFileForSnapshot = null")
            }

            LaunchedEffect(key1 = routeFolderForSnapshots) {
                routeFolderForSnapshots?.let { folder ->
                    val folderThumbnails = File(context.filesDir, Const.THUMBNAILS)
                    val files = folder.listFiles { f ->
                        f.isFile && (f.extension == Const.GPX_EXT.replace(".", "") ||
                                f.extension == Const.KML_EXT.replace(".", "") ||
                                f.extension == Const.GEOJSON_EXT.replace(".", ""))
                    }
                    Timber.i("${folder.path} files: ${files?.size}")
                    files?.forEachIndexed { index, file ->
                        alertProgress = resources.getString(R.string.taking_snapshot, file.nameWithoutExtension).plus(" ($index / ${files.size})")

                        // Allow UI to update and provide small breath between heavy operations
                        delay(500.milliseconds)

                        val success = try {
                            var feedback: Triple<String, Bitmap?, File>? = null
                            if (file.extension == Const.GEOJSON_EXT.replace(".", "")) {
                                takeGeojsonSnapshot(context, file)
                            } else {
                                val snapShotFile1 = File(folderThumbnails, file.nameWithoutExtension.plus(Const.JPG_EXT))
                                val snapShotFile2 = File(file.parentFile, file.nameWithoutExtension.plus(Const.JPG_EXT))
                                feedback = if (!snapShotFile1.exists() && !snapShotFile2.exists()) {
                                    Timber.i(resources.getString(R.string.taking_snapshot, file.nameWithoutExtension))
                                    takeSnapShot(context = context, routeFile = file)
                                } else {
                                    Timber.i("snapshot already exists: ${file.nameWithoutExtension}")
                                    Triple(resources.getString(R.string.snapshot_already_exists), null, snapShotFile1)
                                }
                                feedback != null
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error processing snapshot for ${file.name}")
                            false
                        }

                        if (!success) {
                            Timber.e("Failed to create snapshot for: ${file.name}")
                        }
                    }

                    popupSnackMsg = resources.getString(R.string.refresh_route_preview_ready, folder.name)
                    routeFolderForSnapshots = null
                    alertProgress = null
                }
            }

            alertProgress?.let { msg ->
                AlertDialog(
                    onDismissRequest = { alertProgress = null },
                    properties = DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    ),
                    confirmButton = {},
                    dismissButton = {},
                    title = { Text(text = msg, style = MaterialTheme.typography.titleMedium) },
                    text = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(40.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                )
            }
            alertSnapshotFeedback?.let { feedback ->
                AlertDialog(
                    onDismissRequest = { alertSnapshotFeedback = null },
                    properties = DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    ),
                    confirmButton = {
                        TextButton(onClick = {
                            shareRouteSnapshot(context, feedback.third)
                            alertSnapshotFeedback = null
                        }) {
                            Text(stringResource(R.string.share_route_snapshot))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { alertSnapshotFeedback = null }) {
                            Text(stringResource(R.string.exit_))
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .background(Color.White)
                                .fillMaxWidth()
                                //.aspectRatio(1.0f)
                                .padding(16.dp),
                            //shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(feedback.first)
                            feedback.second?.let { thumbnail ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(//modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                                        painter = BitmapPainter(
                                            thumbnail.asImageBitmap(), IntOffset(0, 0),
                                            IntSize(thumbnail.width, thumbnail.height)
                                        ),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    })
            }
            var alertExifMsgPair: Pair<String, File>? by remember { mutableStateOf(null) }
            alertExifMsgPair?.let { exifMsgPair ->
                AlertDialog(
                    onDismissRequest = { alertExifMsgPair = null },
                    properties = DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    ),
                    confirmButton = {
                        TextButton(onClick = {
                            alertExifMsgPair = null
                            routeFileForSnapshot = exifMsgPair.second
                        }) {
                            Text(stringResource(R.string.take_snapshot))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { alertExifMsgPair = null }) {
                            Text(stringResource(R.string.exit_))
                        }
                    },
                    title = { Text(exifMsgPair.second.nameWithoutExtension) },
                    text = { Text(exifMsgPair.first) }
                )
            }

            var alertRouteSearch: List<RouteEntity>? by remember { mutableStateOf(null) }
            alertRouteSearch?.let {
                PdfRoutesDropdownMenu(alertRouteSearch, finish = { alertRouteSearch = null },
                    {name, region ->
                        Timber.i("region: $region, name: $name")
                        alertRouteSearch = null
                        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                        region?.let { region ->
                            val routeFolder = File(rootRouteFolder, region)
                            name?.let { name ->
                                val routeFileSelection = File(routeFolder, name)
                                if (routeFileSelection.exists()) {
                                    routeFile = routeFileSelection
                                }
                            }
                        }
                    })
            }
            routeFile?.let { selectedFile ->
                RouteDialog(filesDir, selectedFile, finish = {
                    routeFile = null
                    Timber.i("finish routeFile = null")
                }, alert = { msg ->
                    Timber.i("alert: ${selectedFile.name}")
                    if (selectedFile.extension != Const.GEOJSON_EXT.replace(".", ""))
                        alertExifMsgPair = Pair(msg, selectedFile)
                    routeFile = null
                }, share = {
                    shareRouteSnapshot(context, selectedFile)
                    routeFile = null
                }, refresh = {
                    Timber.i("routeFileForSnapshot: $selectedFile")
                    routeFileForSnapshot = selectedFile
                    routeFile = null
                }, select = {
                    routeFile = null
                }, dialogModeOrdinal = dialogModeOrdinal)
            }
            val prefs = remember { getDefaultSharedPreferences(context) }
            val currentRouteFolderPath = prefs.getString(Const.PREF_ROUTEFOLDER_FILEPATH, null)
            if (currentRouteFolderPath != null && routeFolderForSnapshots == null && routeFileForSnapshot == null && dialogModeOrdinal == RouteDialogMode.Admin.ordinal) {
                // This is a placeholder for where you might trigger the bulk snapshot,
                // e.g., from a menu item or a specific button in the UI.
            }
            RamaniApp(
                onDocumentViewerFinish = {
                    Timber.i("onDocumentViewerFinish")
                    finish()
                },
                onDocumentViewerResult = { resultRouteTriple ->
                    Timber.i("onDocumentViewerResult: ${resultRouteTriple.first} ${resultRouteTriple.second} lllh:${resultRouteTriple.third.size}")
                    resultRouteTriple.first?.let { name ->
                        val prefs = getDefaultSharedPreferences(context)
                        val routeFolder = prefs.getString(Const.PREF_ROUTEFOLDER_FILEPATH, null)
                        Timber.i("routeFolder: $routeFolder")
                        routeFolder?.let {
                            val routeRootFolderFile = File(filesDir, Const.ROUTEFOLDER)
                            val routeFolderFile = File(routeRootFolderFile, routeFolder)
                            Timber.i("routeFolderFile: $routeFolderFile")
                            routeFile = routeFolderFile.listFiles()?.find { file ->
                                file.nameWithoutExtension == name
                            }
                            Timber.i("routeFile: ${routeFile?.path}")
                        }
                    }
                },
                onRouteFolderSelected = {folderTriple -> // Triple(routeFolder.name, routeFolder.path, routeFiles?.size ?: 0))
                    Timber.i("onRouteFolderSelected: $folderTriple")
                },
                onRouteFolderFinished = { prefRouteFolderName ->
                    prefRouteFolderName?.let {
                        routeFolderForSnapshots = File(File(filesDir, Const.ROUTEFOLDER), it)
                    }
                    val resultIntent = Intent()
                    resultIntent.putExtra(Const.EXTRA_ROUTEFOLDER, prefRouteFolderName)
                    Timber.i("EXTRA_ROUTEFOLDER: $prefRouteFolderName")
                    setResult(RESULT_OK, resultIntent)
                    finish()
                },
                onRouteSelected = { file ->
                    Timber.i("onRouteSelected routeFile: ${file.path}")
                    file.let {
                        if (file.exists())
                            routeFile = file
                        else {
                            Timber.e("routeFile not found: ${file.path}")
                            popupSnackMsg = "routeFile not found: ${file.path}"
                        }
                    }
                }, onRouteInfoSelected = { file ->
                    Timber.i("onRouteInfoSelected: $file")
                    file.let {
                        if (file.exists())
                            routeFile = file
                        else {
                            Timber.e("routeFile not found: ${file.path}")
                            popupSnackMsg = "routeFile not found: ${file.path}"
                        }
                        //popupSnackMsg = resources.getString(R.string.does_nothing_here)
                        //viewModel.closeOverlay()
                    }
                }, createSnapshots = {folderName ->
                    folderName?.let {
                        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                        val routeFolder = File(rootRouteFolder, folderName)
                        Timber.i("routeFolderForSnapshots: ${routeFolder.path}")
                        routeFolderForSnapshots = routeFolder
                    }
                }, dialogMode = dialogModeOrdinal)
        }
    }
}

private suspend fun takeGeojsonSnapshot(context: Context, geojsonFile: File, override: Boolean = false): Boolean {
    val thumbnailsFolder = File(context.filesDir, Const.THUMBNAILS)
    val snapshotFile = File(thumbnailsFolder, geojsonFile.name.replace(Const.GEOJSON_EXT, Const.JPG_EXT))
    if (snapshotFile.exists() && !override) return true
    val geojsonString = geojsonFile.inputStream().bufferedReader().use { it.readText() }
    val deferred = CompletableDeferred<Boolean>()
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
                deferred.complete(true)
            } catch (e: Exception) {
                Timber.e(e, "Failed to save snapshot or EXIF attributes")
                deferred.complete(false)
            }
        } else {
            deferred.complete(false)
        }
    })
    if (res == null) return false
    return deferred.await()
}

private fun compressString(data: String): String {
    val bos = ByteArrayOutputStream(data.length)
    GZIPOutputStream(bos).use { it.write(data.toByteArray(StandardCharsets.UTF_8)) }
    return "GZIP:" + Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
}

fun decompressString(compressedData: String): String {
    if (!compressedData.startsWith("GZIP:")) return compressedData
    val base64Data = compressedData.substring(5)
    val compressedBytes = Base64.decode(base64Data, Base64.NO_WRAP)
    return GZIPInputStream(compressedBytes.inputStream()).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
}

private suspend fun takeSnapShot(context: Context, routeFile: File): Triple<String, Bitmap?, File>? {
    Timber.i("takeSnapShot: ${routeFile.path}")
    val lllh =
        if (routeFile.extension == Const.JPG_EXT) {
            Helpers.getCoordinatesFromExif(routeFile)
        } else
            Helpers.getLllhFromFile(routeFile)
    if (lllh.isNullOrEmpty()) {
        Timber.e("${routeFile.name} lllh isNullOrEmpty")
        return Triple(context.getString(R.string.no_coordinates, routeFile.name), null, routeFile)
    }

    val deferred = CompletableDeferred<Triple<String, Bitmap?, File>?>()
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
            deferred.complete(Triple(snackTitle.toString(), snapShot.bitmap, routeFile))
        } else {
            deferred.complete(null)
        }
    }
    return deferred.await()
}

private fun shareRouteSnapshot(context: Context, routeFile: File) {
    val routesFolder = routeFile.parentFile
    val folderThumbnails = File(context.filesDir, Const.THUMBNAILS)
    var routeSnapshotFile = File(
        folderThumbnails,
        routeFile.name.replace(Const.KML_EXT, Const.JPG_EXT).replace(Const.GPX_EXT, Const.JPG_EXT)
            .replace(Const.GEOJSON_EXT, Const.JPG_EXT))
    if (!routeSnapshotFile.exists())
        routeSnapshotFile = File(
            routesFolder,
            routeFile.name.replace(Const.KML_EXT, Const.JPG_EXT).replace(Const.GPX_EXT, Const.JPG_EXT)
                .replace(Const.GEOJSON_EXT, Const.JPG_EXT))
    try {
        if (routeSnapshotFile.exists()) {
            val exifInterfaceSource = ExifInterface(routeSnapshotFile.path)
            val kmlString = exifInterfaceSource.getAttribute(ExifInterface.TAG_USER_COMMENT)
            val distString = exifInterfaceSource.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)
            val latLngArray = exifInterfaceSource.latLong

            val options = BitmapFactory.Options().apply { inMutable = true }
            // AI fix for: Immutable bitmap passed to Canvas constructor
            val thumbnail = BitmapFactory.decodeFile(routeSnapshotFile.path, options) ?: return
            val bmp: Bitmap = createBitmap(thumbnail.width, thumbnail.height + 30)
            bmp.let {
                val thumbCanvas = Canvas(it)
                thumbCanvas.drawColor(android.graphics.Color.WHITE)
                thumbCanvas.drawBitmap(thumbnail, 0f, 0f, null)
                val name = routeSnapshotFile.name.replace(Const.JPG_EXT, "")
                drawRouteName(context, thumbCanvas, name)
                val file = File(context.cacheDir, routeSnapshotFile.name)
                val b = file.createNewFile()
                Timber.i("${file.path} create $b")

                val out = FileOutputStream(file)
                it.compress( //isBoundary ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG,
                    Bitmap.CompressFormat.JPEG, 90, out
                )
                out.flush()
                out.close()
                val exifInterface = ExifInterface(file.path)
                exifInterface.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, distString)
                exifInterface.setAttribute(
                    ExifInterface.TAG_ORIENTATION,  // 28jan2022
                    ExifInterface.ORIENTATION_NORMAL.toString()
                )

                kmlString?.let {
                    val processedString = if (it.length > Const.EXIF_MAX_SIZE) {
                        compressString(it)
                    } else {
                        it
                    }

                    if (processedString.length < Const.EXIF_MAX_SIZE) {
                        exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, processedString)
                    } else {
                        Timber.w("kmlString (even compressed) too large for EXIF: ${processedString.length}")
                    }
                }
                latLngArray?.let { exifInterface.setLatLong(it[0], it[1]) }
                exifInterface.saveAttributes()
                Timber.i("${file.name} write exif OK")

                val uri = FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".provider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.type = "*/*"
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        } else
            Timber.i(context.getString(R.string.file_not_found, routeSnapshotFile.path))
    } catch (e: Exception) {
        Timber.i("${e.message}")
    }
}
