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
import androidx.activity.viewModels
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.almica.ramani.RouteInfo
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


data class SnapshotFeedback(
    val message: String,
    val thumbnail: Bitmap?,
    val routeFile: File
)

data class RouteInfoFeedback(
    val message: String,
    val routeFile: File
)

class ListRouteFoldersActivity : ComponentActivity() {

    private val viewModel: ListRouteFoldersViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val resources = LocalResources.current
            val dialogModeOrdinal = intent.getIntExtra(Const.EXTRA_ROUTE_DIALOG_MODE, RouteDialogMode.Admin.ordinal)
            Timber.i("dialogModeOrdinal: $dialogModeOrdinal")

            val routeFile by viewModel.routeFile.collectAsState()
            val snapshotFeedback by viewModel.snapshotFeedback.collectAsState()
            val routeInfoFeedback by viewModel.routeInfoFeedback.collectAsState()
            val popupSnackMsg by viewModel.popupSnackMsg.collectAsState()
            val alertProgress by viewModel.alertProgress.collectAsState()

            var routeFileForSnapshot: File? by remember { mutableStateOf(null) }
            var routeFolderForSnapshots: File? by remember { mutableStateOf(null) }

            LaunchedEffect(routeFileForSnapshot) {
                routeFileForSnapshot?.let {
                    viewModel.processSingleSnapshot(it)
                    routeFileForSnapshot = null
                }
            }

            LaunchedEffect(routeFolderForSnapshots) {
                routeFolderForSnapshots?.let {
                    viewModel.processFolderSnapshots(it)
                    routeFolderForSnapshots = null
                }
            }

            // Dialogs and Overlays
            popupSnackMsg?.let { msg ->
                SnackPopup(message = msg, onDismiss = { viewModel.clearPopupSnackMsg() })
            }

            alertProgress?.let { msg ->
                ProgressDialog(message = msg, onDismiss = { viewModel.clearAlertProgress() })
            }

            snapshotFeedback?.let { feedback ->
                SnapshotFeedbackDialog(
                    feedback = feedback,
                    onShare = { shareRouteSnapshot(context, it) },
                    onDismiss = { viewModel.clearSnapshotFeedback() }
                )
            }

            routeInfoFeedback?.let { feedback ->
                RouteInfoFeedbackDialog(
                    feedback = feedback,
                    onTakeSnapshot = {
                        viewModel.clearRouteInfoFeedback()
                        routeFileForSnapshot = it
                    },
                    onDismiss = { viewModel.clearRouteInfoFeedback() }
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
                                    viewModel.setRouteFile(routeFileSelection)
                                }
                            }
                        }
                    })
            }
            routeFile?.let { selectedFile ->
                RouteDialog(filesDir, selectedFile, finish = {
                    viewModel.setRouteFile(null)
                    Timber.i("finish routeFile = null")
                }, alert = { msg ->
                    viewModel.showRouteInfoAlert(msg, selectedFile)
                }, share = {
                    shareRouteSnapshot(context, selectedFile)
                    viewModel.setRouteFile(null)
                }, refresh = {
                    Timber.i("routeFileForSnapshot: $selectedFile")
                    routeFileForSnapshot = selectedFile
                    viewModel.setRouteFile(null)
                }, select = {
                    viewModel.setRouteFile(null)
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
                onDocumentViewerResult = { resultRouteInfo ->
                    Timber.i("onDocumentViewerResult: ${resultRouteInfo.name} ${resultRouteInfo.formattedDistance} lllh:${resultRouteInfo.points.size}")
                    resultRouteInfo.name?.let { name ->
                        val prefs = getDefaultSharedPreferences(context)
                        val routeFolder = prefs.getString(Const.PREF_ROUTEFOLDER_FILEPATH, null)
                        Timber.i("routeFolder: $routeFolder")
                        routeFolder?.let {
                            val routeRootFolderFile = File(filesDir, Const.ROUTEFOLDER)
                            val routeFolderFile = File(routeRootFolderFile, routeFolder)
                            Timber.i("routeFolderFile: $routeFolderFile")
                            val foundFile = routeFolderFile.listFiles()?.find { file ->
                                file.nameWithoutExtension == name
                            }
                            viewModel.setRouteFile(foundFile)
                            Timber.i("routeFile: ${foundFile?.path}")
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
                            viewModel.setRouteFile(file)
                        else {
                            Timber.e("routeFile not found: ${file.path}")
                            viewModel.setPopupSnackMsg("routeFile not found: ${file.path}")
                        }
                    }
                }, onRouteInfoSelected = { file ->
                    Timber.i("onRouteInfoSelected: $file")
                    file.let {
                        if (file.exists())
                            viewModel.setRouteFile(file)
                        else {
                            Timber.e("routeFile not found: ${file.path}")
                            viewModel.setPopupSnackMsg("routeFile not found: ${file.path}")
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
                        Helpers.compressString(it)
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

@Composable
private fun SnapshotFeedbackDialog(
    feedback: SnapshotFeedback,
    onShare: (File) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        confirmButton = {
            TextButton(onClick = {
                onShare(feedback.routeFile)
                onDismiss()
            }) {
                Text(stringResource(R.string.share_route_snapshot))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.exit_))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(feedback.message)
                feedback.thumbnail?.let { thumbnail ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = BitmapPainter(
                                thumbnail.asImageBitmap(), IntOffset(0, 0),
                                IntSize(thumbnail.width, thumbnail.height)
                            ),
                            contentDescription = null
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun RouteInfoFeedbackDialog(
    feedback: RouteInfoFeedback,
    onTakeSnapshot: (File) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        confirmButton = {
            TextButton(onClick = { onTakeSnapshot(feedback.routeFile) }) {
                Text(stringResource(R.string.take_snapshot))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.exit_))
            }
        },
        title = { Text(feedback.routeFile.nameWithoutExtension) },
        text = { Text(feedback.message) }
    )
}

@Composable
private fun ProgressDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        confirmButton = {},
        dismissButton = {},
        title = { Text(text = message, style = MaterialTheme.typography.titleMedium) },
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

@Composable
private fun SnackPopup(
    message: String,
    onDismiss: () -> Unit
) {
    Popup(
        properties = PopupProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        alignment = Alignment.Center,
        onDismissRequest = onDismiss
    ) {
        Surface(
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 4.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
