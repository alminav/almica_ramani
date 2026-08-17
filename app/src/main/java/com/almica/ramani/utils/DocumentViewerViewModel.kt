package com.almica.ramani.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.pdf.PdfDocument
import androidx.pdf.SandboxedPdfLoader
import com.almica.ramani.Const
import com.almica.ramani.FeatureProperties.Companion.NAME
import com.almica.ramani.Helpers
import com.almica.ramani.Helpers.Companion.writeKml2Exif
import com.almica.ramani.LatLngH
import com.almica.ramani.R
import com.almica.ramani.RouteInfo
import com.almica.ramani.utils.GeoJsonUtils.Companion.getFeatureCollectionFromString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Duration.Companion.milliseconds

class DocumentViewerViewModel : ViewModel() {

    var displayName by mutableStateOf<String?>(null)
    var documentUri by mutableStateOf<Uri?>(null)
    var geoJsonUriState by mutableStateOf<Uri?>(null)
    var popupSnackMsg by mutableStateOf<String?>(null)
    var routeInfo by mutableStateOf<RouteInfo?>(null)
    var menuExpanded by mutableStateOf(false)
    var progressMsg by mutableStateOf<String?>(null)
    var routeMap = mutableStateMapOf<Int, NamedRoute>()
    var routeMapIsReady by mutableLongStateOf(0L)
    var geojsonText by mutableStateOf("")
    var pdfDocument by mutableStateOf<PdfDocument?>(null)

    private var pdfLoader: SandboxedPdfLoader? = null
    private var pdfLoadingJob: Job? = null

    fun initialize(context: Context, prefRouteFolder: String?) {
        if (displayName == null) {
            displayName = prefRouteFolder
        }
        if (pdfLoader == null) {
            pdfLoader = SandboxedPdfLoader(context.applicationContext)
        }
    }

    fun setDocumentUri(uri: Uri?, context: Context) {
        documentUri = uri
        if (uri != null) {
            loadPdfDocument(uri)
        }
    }

    private fun loadPdfDocument(uri: Uri) {
        pdfLoadingJob?.cancel()
        pdfLoadingJob = viewModelScope.launch {
            try {
                val document = withContext(Dispatchers.IO) {
                    pdfLoader?.openDocument(uri)
                }
                pdfDocument = document
            } catch (e: Exception) {
                Timber.e(e, "Failed to open PDF document")
                pdfDocument = null
            }
        }
    }

    fun shareFiles(context: Context) {
        val uris = arrayListOf<Uri>()
        documentUri?.let { uris.add(it) }
        geoJsonUriState?.let { uris.add(it) }

        if (uris.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Files"))
        }
    }

    fun importFiles(context: Context, uris: List<Uri>, rootUri: Uri, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uris.isNotEmpty()) popupSnackMsg = "Importing ${uris.size} files..."

            uris.forEachIndexed { index, sourceUri ->
                try {
                    val contentResolver = context.contentResolver
                    var fileName: String? = null
                    var mimeType: String? = null

                    contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                        if (cursor.moveToFirst()) {
                            fileName = cursor.getString(nameIndex)
                            mimeType = cursor.getString(mimeIndex)
                        }
                    }

                    if (fileName != null && mimeType != null) {
                        val targetUri = DocumentsContract.createDocument(
                            contentResolver,
                            DocumentsContract.buildDocumentUriUsingTree(rootUri, DocumentsContract.getTreeDocumentId(rootUri)),
                            mimeType,
                            fileName
                        )
                        targetUri?.let { dest ->
                            contentResolver.openInputStream(sourceUri)?.use { input ->
                                contentResolver.openOutputStream(dest)?.use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        if (fileName.endsWith(Const.GEOJSON_EXT)) {
                            val splitFileName = fileName.split(Const.UNDERLINE)
                            val region = splitFileName.take((splitFileName.size - 2).coerceIn(1, 4))
                                .joinToString(Const.UNDERLINE)
                            
                            val routeFolder = File(File(context.filesDir, Const.ROUTEFOLDER), region)
                            if (!routeFolder.exists()) routeFolder.mkdirs()
                            
                            val routeFile = File(routeFolder, fileName)
                            try {
                                contentResolver.openInputStream(sourceUri)?.use { input ->
                                    routeFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to copy GeoJSON to internal storage")
                            }
                        }
                    }
                    if (index == uris.size - 1) {
                        popupSnackMsg = "Import completed"
                        withContext(Dispatchers.Main) {
                            onComplete()
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Import failed")
                    popupSnackMsg = "Import failed: ${e.message}"
                }
            }
        }
    }

    fun exportRoutes(context: Context, prefRouteFolder: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            prefRouteFolder?.let { folderName ->
                val routeFolder = File(File(context.filesDir, Const.ROUTEFOLDER), folderName)
                if (!routeFolder.exists()) routeFolder.mkdirs()

                val featureCollection = getFeatureCollectionFromString(geojsonText)
                featureCollection?.features()?.forEach { feature ->
                    if (feature.geometry() is LineString) {
                        val name = feature.getProperty(NAME)?.asString
                        if (name != null) {
                            val lllh = ArrayList<LatLngH>()
                            (feature.geometry() as LineString).coordinates().forEach { point ->
                                lllh.add(LatLngH(point.latitude(), point.longitude(), point.altitude()))
                            }
                            val routeFile = File(routeFolder, "$name.kml")
                            Helpers.writeLllh2KmlFile(lllh, routeFile.path)
                        }
                    }
                }
                displayName?.let { name ->
                    val geojsonName = name.replace(Const.PDF_EXT, Const.GEOJSON_EXT)
                    val geojsonFile = File(routeFolder, geojsonName)
                    geojsonFile.writeText(geojsonText)
                }
                popupSnackMsg = context.getString(R.string.routes_exported, folderName)
            }
        }
    }

    fun exportSnapshots(context: Context, prefRouteFolder: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            prefRouteFolder?.let { folderName ->
                val routeMap = hashMapOf<String, ArrayList<LatLngH>>()
                val featureCollection = getFeatureCollectionFromString(geojsonText)
                featureCollection?.features()?.forEach { feature ->
                    if (feature.geometry() is LineString) {
                        val name = feature.getProperty(NAME)?.asString
                        if (name != null) {
                            val lllh = ArrayList<LatLngH>()
                            (feature.geometry() as LineString).coordinates().forEach { point ->
                                lllh.add(LatLngH(point.latitude(), point.longitude(), point.altitude()))
                            }
                            routeMap[name] = lllh
                        }
                    }
                }

                val thumbnailsFolder = File(context.filesDir, Const.THUMBNAILS)
                if (!thumbnailsFolder.exists()) thumbnailsFolder.mkdirs()

                routeMap.keys.sorted().forEachIndexed { index, name ->
                    val lllh = routeMap[name]!!
                    val snapshotFile = File(thumbnailsFolder, "$name.jpg")
                    pdfDocument?.getPageBitmapSource(2 * index + 1)?.use { bmpSource ->
                        val pageInfo = pdfDocument!!.getPageInfo(2 * index + 1)
                        val size = Size(pageInfo.width, pageInfo.height)
                        val bmp = bmpSource.getBitmap(size, Rect(0, 0, pageInfo.width, pageInfo.width))
                        val out = FileOutputStream(snapshotFile)
                        bmp?.compress(Bitmap.CompressFormat.JPEG, 100, out)
                        out.flush()
                        out.close()
                        writeKml2Exif(snapshotFile, lllh, name, true, lllh.getMaplibreBounds())
                    }
                }
                popupSnackMsg = context.getString(R.string.snapshots_exported, folderName)
            }
        }
    }

    fun handleSelectedTree(context: Context, rootUri: Uri, prefRouteFolder: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    rootUri,
                    DocumentsContract.getTreeDocumentId(rootUri)
                )

                val displayNames = mutableListOf<Pair<String, Uri>>()
                contentResolver.query(childrenUri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val docIdIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    while (cursor.moveToNext()) {
                        val mime = cursor.getString(mimeIndex)
                        if (mime == "application/pdf") {
                            val name = cursor.getString(nameIndex)
                            val uri = DocumentsContract.buildDocumentUriUsingTree(rootUri, cursor.getString(docIdIndex))
                            if (name != null && prefRouteFolder?.let { name.startsWith(it) } == true) {
                                displayNames.add(Pair(name, uri))
                            }
                        }
                    }
                    displayNames.sortBy { it.first }
                }

                val selectedPdf = displayNames.lastOrNull()
                if (selectedPdf == null) {
                    Timber.w("No PDF found in the selected directory")
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    displayName = selectedPdf.first
                    setDocumentUri(selectedPdf.second, context)
                }

                val geojsonName = selectedPdf.first.replace(Const.PDF_EXT, "") + Const.GEOJSON_EXT
                
                // Try to find GeoJSON in tree
                var geoJsonUri: Uri? = null
                contentResolver.query(childrenUri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val docIdIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    while (cursor.moveToNext()) {
                        if (cursor.getString(nameIndex) == geojsonName) {
                            geoJsonUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, cursor.getString(docIdIndex))
                            break
                        }
                    }
                }

                if (geoJsonUri == null) {
                    val parentId = DocumentsContract.getTreeDocumentId(rootUri)
                    val parentUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, parentId)
                    geoJsonUri = DocumentsContract.createDocument(contentResolver, parentUri, "application/geo+json", geojsonName)
                }

                withContext(Dispatchers.Main) {
                    geoJsonUriState = geoJsonUri
                }

                geoJsonUri?.let { uri ->
                    contentResolver.openInputStream(uri)?.use { input ->
                        val text = input.bufferedReader().use { it.readText() }
                        withContext(Dispatchers.Main) {
                            geojsonText = text
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error handling document URI")
            }
        }
    }

    fun parseGeoJsonAndBuildRouteMap() {
        viewModelScope.launch(Dispatchers.Default) {
            val featureCollection = getFeatureCollectionFromString(geojsonText)
            val features = featureCollection?.features() ?: return@launch
            val routeList = arrayListOf<NamedRoute>()

            features.forEachIndexed { index, feature ->
                val lllh = GeoJsonUtils.getLllhFromGeometry(feature.geometry())
                if (lllh.isNotEmpty()) {
                    val name = feature.getProperty(NAME)?.asString
                    if (name != null) {
                        routeList.add(NamedRoute(name, lllh))
                        progressMsg = "Page ${index + 1} of ${features.size}"
                        delay(20.milliseconds)
                    }
                }
            }
            routeList.sortBy { it.name }
            routeMap.clear()
            routeList.forEachIndexed { index, namedRoute ->
                routeMap[2 * index] = namedRoute
                routeMap[2 * index + 1] = namedRoute
            }
            
            withContext(Dispatchers.Main) {
                routeMapIsReady = System.currentTimeMillis()
                progressMsg = null
            }
        }
    }

    override fun onCleared() {
        pdfDocument?.close()
        pdfLoadingJob?.cancel()
    }
}
