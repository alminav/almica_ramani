package com.almica.ramani.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.pdf.PdfDocument
import androidx.pdf.SandboxedPdfLoader
import androidx.pdf.compose.PdfViewer
import androidx.pdf.compose.PdfViewerState
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.Const
import com.almica.ramani.FeatureProperties.Companion.NAME
import com.almica.ramani.Helpers
import com.almica.ramani.Helpers.Companion.writeKml2Exif
import com.almica.ramani.LatLngH
import com.almica.ramani.R
import com.almica.ramani.utils.GeoJsonUtils.Companion.getFeatureCollectionFromString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.platform.LocalResources

// Source - https://stackoverflow.com/a/79247047
// Posted by BenjyTec, modified by community. See post 'Timeline' for change history
// Retrieved 2026-06-12, License - CC BY-SA 4.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewer(finish: () -> Unit,
                   routeDataTripleSelection: (Triple<String?, String?, ArrayList<LatLngH>>) -> Unit) {
    val resources = LocalResources.current
    val context = LocalContext.current
    val prefs = getDefaultSharedPreferences(context)
    val prefRouteFolder = prefs.getString(Const.PREF_ROUTEFOLDER_FILEPATH, null)
    var displayName: String? by remember { mutableStateOf(prefRouteFolder) }
    BackPressHandler {
        Timber.i(" Back Press intercepted")
        finish()
    }
    var documentUri by remember { mutableStateOf<Uri?>(null) }
    var geoJsonUriState by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        documentUri = it
    }

    var popupSnackMsg: String? by remember { mutableStateOf(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val rootUri = documentUri ?: return@rememberLauncherForActivityResult
        if (uris.isNotEmpty()) popupSnackMsg = "Importing ${uris.size} files..."

        uris.forEach { sourceUri ->
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
                }
                if (uris.indexOf(sourceUri) == uris.size - 1) {
                    popupSnackMsg = "Import completed"
                    finish()
                }
            } catch (e: Exception) {
                Timber.e(e, "Import failed")
                popupSnackMsg = "Import failed: ${e.message}"
            }
        }
    }

    var routeDataTriple by remember { mutableStateOf<Triple<String?, String?, ArrayList<LatLngH>>?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    val pdfLoader = remember { SandboxedPdfLoader(context.applicationContext) }
    val pdfViewerState = remember { PdfViewerState() }
    val coroutineScope = rememberCoroutineScope()

    val pdfDocument by produceState<PdfDocument?>(initialValue = null, documentUri) {
        val document = try {
            documentUri?.let { pdfLoader.openDocument(it) }
        } catch (_: Exception) {
            null
        }
        // Reset scroll state when a new document is loaded
        pdfViewerState.scrollToPage(0)

        value = document
        document?.use {
            awaitCancellation()
        }
    }
    //var confirmFilter: String? by remember { mutableStateOf(null) }

    fun shareFiles(context: Context, pdfUri: Uri?, geoJsonUri: Uri?) {
        val uris = arrayListOf<Uri>()
        pdfUri?.let { uris.add(it) }
        geoJsonUri?.let { uris.add(it) }

        if (uris.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Files"))
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var geojsonText by remember { mutableStateOf("") }
        var pdfUri: Uri? by remember { mutableStateOf(null) }
        var geoJsonUri: Uri? by remember { mutableStateOf(null) }
        var geojsonRoutesExportTrigger: Long by remember { mutableLongStateOf(0L) }
        var geojsonSnapshotsExportTrigger: Long by remember { mutableLongStateOf(0L) }
        CenterAlignedTopAppBar(
            title = { Text(
                text = "$displayName",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            ) },
            actions = {
                IconButton(onClick = {
                    pdfDocument?.let { doc ->
                        coroutineScope.launch {
                            pdfViewerState.scrollToPage(doc.pageCount - 1)
                        }
                    }
                }) {
                    Icon(Icons.Default.VerticalAlignBottom, contentDescription = "Scroll to bottom")
                }
                IconButton(onClick = {
                    pdfDocument?.let { doc ->
                        coroutineScope.launch {
                            pdfViewerState.scrollToPage(0)
                        }
                    }
                }) {
                    Icon(Icons.Default.VerticalAlignTop, contentDescription = "Scroll to top")
                }
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Import") },
                        onClick = {
                            menuExpanded = false
                            importLauncher.launch("*/*")
                        },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = {
                            menuExpanded = false
                            shareFiles(context, documentUri, geoJsonUriState)
                        },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Export Routes") },
                        onClick = {
                            menuExpanded = false
                            geojsonRoutesExportTrigger = System.currentTimeMillis()
                        },
                        leadingIcon = { Icon(Icons.Default.Route, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Export Snapshots") },
                        onClick = {
                            menuExpanded = false
                            geojsonSnapshotsExportTrigger = System.currentTimeMillis()
                        },
                        leadingIcon = { Icon(Icons.Default.Preview, contentDescription = null) })
                }
            }
        )
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
/*
        FilledTonalButton(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(start = 10.dp, end = 10.dp),
            onClick = {
                if (documentUri != null)
                    finish()
                else
                    launcher.launch(arrayOf("application/pdf"))
            }
        ) {
            Text(text = "Select Document")
        }
 */
        /**
         * KI 13jun2026
         * I've fixed the java.lang.IllegalStateException: Launcher has not been initialized error.
         * The issue was caused by calling ActivityResultLauncher.launch() before the launcher was fully
         * registered in Jetpack Compose. This typically happens when launch() is called:
         * 1.
         * Directly within the body of a Composable function.
         * 2.
         * Inside a LaunchedEffect(Unit) immediately upon composition without a small delay to allow the
         * ActivityResultRegistry to complete registration.
         * I have applied the following fixes:
         * •
         * FileImportActivity.kt and DocumentViewer.kt: Moved the launch() calls from the Composable body into a LaunchedEffect.
         * •
         * GeoCoderLauncher.kt, GmsMapScreen.kt, and GmsTileOverlayActivity.kt: Added a small delay(100) within the
         * LaunchedEffect to ensure the launcher is initialized before use.
         * These changes ensure that the launcher is ready when launch() is invoked, resolving the race condition.
         */
        LaunchedEffect(Unit) {
            delay(100.milliseconds)
            launcher.launch(null)
        }

        LaunchedEffect(geojsonRoutesExportTrigger) {
            if (geojsonRoutesExportTrigger > 0)
                withContext(Dispatchers.IO) {
                prefRouteFolder?.let {
                    val routeFolder =
                        File(File(context.filesDir, Const.ROUTEFOLDER), prefRouteFolder)
                    if (!routeFolder.exists()) {
                        routeFolder.mkdirs()
                    }

                    Timber.i("geojsonExportTrigger: $geojsonRoutesExportTrigger")
                    val featureCollection: FeatureCollection? =
                        getFeatureCollectionFromString(geojsonText)
                    Timber.i("featureCollections ready")
                    val features = featureCollection?.features()
                    Timber.i("features: ${features?.size}")
                    features?.forEach { feature ->
                        if (feature.geometry() is LineString) {
                            if (feature.getProperty(NAME) != null) {
                                val name = feature.getProperty(NAME).asString
                                Timber.i("feature name: $name")

                                val lllh = ArrayList<LatLngH>()
                                val geometry = feature.geometry()
                                if (geometry is LineString) {
                                    geometry.coordinates().forEach { point ->
                                        lllh.add(LatLngH(point.latitude(), point.longitude(), point.altitude()))
                                    }
                                }

                                val routeFile = File(routeFolder, "$name.kml")
                                Timber.i("Exporting ${lllh.size} points to: ${routeFile.path}")
                                Helpers.writeLllh2KmlFile(lllh, routeFile.path)
                            }
                        }
                    }
                    displayName?.let {
                        val geojsonName = it.replace(Const.PDF_EXT, Const.GEOJSON_EXT)
                        val geojsonFile = File(routeFolder, geojsonName)
                        geojsonFile.writeText(geojsonText)
                    }
                    popupSnackMsg = resources.getString(R.string.routes_exported, prefRouteFolder)
                }
            }
        }
        LaunchedEffect(geojsonSnapshotsExportTrigger) {
            if (geojsonSnapshotsExportTrigger > 0)
                withContext(Dispatchers.IO) {
                    prefRouteFolder?.let {
                        val routeMap = hashMapOf<String, ArrayList<LatLngH>>()
                        val routeFolder =
                            File(File(context.filesDir, Const.ROUTEFOLDER), prefRouteFolder)
                        if (!routeFolder.exists()) {
                            routeFolder.mkdirs()
                        }
                        Timber.i("geojsonExportTrigger: $geojsonRoutesExportTrigger")
                        val featureCollection: FeatureCollection? =
                            getFeatureCollectionFromString(geojsonText)
                        Timber.i("featureCollections ready")
                        val features = featureCollection?.features()
                        Timber.i("features: ${features?.size}")
                        features?.forEachIndexed { index, feature ->
                            if (feature.geometry() is LineString) {
                                if (feature.getProperty(NAME) != null) {
                                    val name = feature.getProperty(NAME).asString
                                    Timber.i("feature name: $name")

                                    val lllh = ArrayList<LatLngH>()
                                    val geometry = feature.geometry()
                                    if (geometry is LineString) {
                                        geometry.coordinates().forEach { point ->
                                            lllh.add(
                                                LatLngH(
                                                    point.latitude(),
                                                    point.longitude(),
                                                    point.altitude()
                                                )
                                            )
                                        }
                                    }
                                    routeMap[name] = lllh
                                }
                            }
                        }
                        // put geojson data in same sort order as pdf pages
                        val thumbnailsFolder = File(context.filesDir, Const.THUMBNAILS)
                        if (!thumbnailsFolder.exists()) {
                            val b = thumbnailsFolder.mkdirs()
                            Timber.i("${thumbnailsFolder.path} mkdirs: $b")
                        }
                        routeMap.keys.sorted().forEachIndexed { index, name ->
                            val lllh = routeMap[name]!!
                            val snapshotFile = File(thumbnailsFolder, "$name.jpg")
                            Timber.i("Exporting ${lllh.size} points to: ${snapshotFile.path}")
                            pdfDocument?.getPageBitmapSource(2 * index + 1).use { bmpSource ->
                                val size = Size(
                                    pdfDocument!!.getPageInfo(2 * index + 1).width,
                                    pdfDocument!!.getPageInfo(2 * index + 1).height
                                )
                                val bmp = bmpSource?.getBitmap(size, Rect(0, 0,
                                    pdfDocument!!.getPageInfo(2 * index + 1).width,
                                    pdfDocument!!.getPageInfo(2 * index + 1).width))
                                val out = FileOutputStream(snapshotFile)
                                bmp?.compress(Bitmap.CompressFormat.JPEG, 100, out)
                                out.flush()
                                out.close()
                                writeKml2Exif(snapshotFile, lllh, name,
                                    true, lllh.getMaplibreBounds())
                            }
                        }
                        popupSnackMsg = resources.getString(R.string.snapshots_exported, prefRouteFolder)
                    }
                }
        }

        LaunchedEffect(documentUri) {
            val rootUri = documentUri ?: return@LaunchedEffect
            try {
                // Since we used OpenDocumentTree, rootUri is the folder.
                // We need to find the PDF within this folder or let the user pick.
                // For this implementation, we look for the first PDF in that tree.
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
                            displayName = name
                            pdfUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, cursor.getString(docIdIndex))
                            pdfUri?.let {uri ->
                                if (name != null && prefRouteFolder?.let { name.startsWith(it) } == true) {
                                    displayNames.add(Pair(name, uri))
                                }
                                Timber.i("displayName: $name")
                            }
                        }
                    }
                    displayNames.sortBy { it.first }
                }
                displayName = displayNames.lastOrNull()?.first
                pdfUri = displayNames.lastOrNull()?.second

                if (pdfUri == null) {
                    Timber.w("No PDF found in the selected directory")
                    return@LaunchedEffect
                }

                // Update local documentUri for the viewer
                documentUri = pdfUri

                displayName?.let { name ->
                    val geojsonName = name.replace(Const.PDF_EXT, "") + Const.GEOJSON_EXT
                    Timber.i("geojsonName: $geojsonName")

                    // 1. Try to find GeoJSON in app's internal storage first
                    val localGeoJsonFile = File(File(context.filesDir, Const.GEOJSON_ROOT_FOLDER), geojsonName)
                    if (localGeoJsonFile.exists()) {
                        Timber.i("Found local geojson: ${localGeoJsonFile.readText()}")
                    }

                    // 2. Access sidecar file via the tree URI, 'Beiwagen Daten'
                    try {
                        val parentId = DocumentsContract.getTreeDocumentId(rootUri)
                        val parentUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, parentId)

                        // First, check if the GeoJSON already exists in the tree
                        context.contentResolver.query(childrenUri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val docIdIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                            while (cursor.moveToNext()) {
                                if (cursor.getString(nameIndex) == geojsonName) {
                                    geoJsonUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, cursor.getString(docIdIndex))
                                    Timber.i("Found existing geoJsonUri: $geoJsonUri")
                                    break
                                }
                            }
                        }

                        // If not found, create it
                        if (geoJsonUri == null) {
                            geoJsonUri = DocumentsContract.createDocument(
                                context.contentResolver,
                                parentUri, "application/geo+json", geojsonName
                            )
                            Timber.i("Created new geoJsonUri: $geoJsonUri")
                        }
                        geoJsonUriState = geoJsonUri

                        geoJsonUri?.let { inputUri ->
                            context.contentResolver.openInputStream(inputUri)?.use {
                                it.bufferedReader().use { reader ->
                                    geojsonText = reader.readText()
                                    Timber.i("geojsonText.length: ${geojsonText.length}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error accessing sidecar file")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error handling document URI")
            }
        }

        /* Old logic removed for brevity as it is replaced by the tree-aware logic above */
/*
        LaunchedEffect(documentUri) {
            val uri = documentUri ?: return@LaunchedEffect
            try {
                var displayName: String? = null
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        displayName = cursor.getString(nameIndex)
                    }
                }
                Timber.i("displayName: $displayName")

                displayName?.let { name ->
                    val geojsonName = name.replace(Const.PDF_EXT, "") + Const.GEOJSON_EXT
                    Timber.i("geojsonName: $geojsonName")

                    // 1. Try to find GeoJSON in app's internal storage first
                    val localGeoJsonFile = File(File(context.filesDir, Const.GEOJSON_ROOT_FOLDER), geojsonName)
                    if (localGeoJsonFile.exists()) {
                        Timber.i("Found local geojson: ${localGeoJsonFile.readText()}")
                    }

                    // 2. Attempting to access sidecar file via SAF (often fails with SecurityException if using OpenDocument)
                    try {
                        val documentId = DocumentsContract.getDocumentId(uri)
                        if (documentId.contains("/")) {
                            val parentId = documentId.substringBeforeLast("/", documentId)
                            val authority = uri.authority!!
                            val parentUri = DocumentsContract.buildDocumentUri(authority, parentId)

                            // Note: createDocument will throw SecurityException if parentUri is not writable
                            val geoJsonUri = DocumentsContract.createDocument(
                                context.contentResolver,
                                parentUri, "application/geo+json", geojsonName
                            )
                            Timber.i("geoJsonUri: $geoJsonUri")

                            geoJsonUri?.let { inputUri ->
                                context.contentResolver.openInputStream(inputUri)?.use {
                                    it.bufferedReader().use { reader ->
                                        Timber.i("geoJson: ${reader.readText()}")
                                    }
                                }
                            }
                        }
                    } catch (e: SecurityException) {
                        Timber.w("Permission denied to create document in parent folder: $e. " +
                                "This is expected when using OpenDocument launcher.")
                    } catch (e: Exception) {
                        Timber.e(e, "Error accessing sidecar file")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error handling document URI")
            }
        }
*/

        Row(modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)) {
            routeDataTriple?.first?.let {
                TextButton(modifier = Modifier.weight(0.5f), onClick = {
                    //routeName(it)
                    //confirmFilter = it
                    routeDataTripleSelection(routeDataTriple!!)
                    Timber.i(it)
                }) { Text(text = it, textAlign = TextAlign.Center) }
            }
            routeDataTriple?.second?.let {
                TextButton(modifier = Modifier.weight(0.5f), onClick = {
                    //routeName(it)
                    //confirmFilter = it
                    routeDataTripleSelection(routeDataTriple!!)
                    Timber.i(it)
                }) { Text(text = it, textAlign = TextAlign.Center) }
            }
        }

        //documentUri?.let {
        pdfDocument?.let {
            PdfViewerContainer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
                pdfViewerState = pdfViewerState,
                pdfDocument = pdfDocument,
                geojsonText,
                routeDataTriple = { routeTriple ->
                    //Timber.i("routeTriple: $routeTriple")
                    routeDataTriple = routeTriple
                }
            )
        }
    }
}

@Composable
fun PdfViewerContainer(
    modifier: Modifier = Modifier,
    pdfViewerState: PdfViewerState,
    pdfDocument: PdfDocument?,
    geojsonText: String,
    routeDataTriple: (Triple<String?, String?, ArrayList<LatLngH>>) -> Unit
) {
/*
    LaunchedEffect(pdfViewerState) {
        snapshotFlow { pdfViewerState.currentSelection }
            .collectLatest { selection ->
                if (selection != null) {
                    Timber.i("Selection changed: $selection")
                    if (selection is ImageSelection) {
                        Timber.i("Image selection detected: ${selection.bitmap.width}x${selection.bitmap.height}")
                        // You can handle the image here (copy, share, etc.)
                    }
                }
            }
    }
*/

    var progressMsg: String? by remember { mutableStateOf(null) }
    var routeMap: HashMap<Int, Pair<String, ArrayList<LatLngH>>> by remember { mutableStateOf(hashMapOf()) }
    var routeMapIsReady by remember { mutableLongStateOf(0L) }
    LaunchedEffect(pdfDocument) {
        val pageCount = pdfDocument?.pageCount ?: 0
        Timber.i("PDF loaded with $pageCount pages")

        Timber.i("geojsonText: $geojsonText")
        val featureCollection: FeatureCollection? = getFeatureCollectionFromString(geojsonText)
        Timber.i("featureCollections ready")
        val features = featureCollection?.features()
        Timber.i("features: ${features?.size}")
        val routePairList = arrayListOf<Pair<String, ArrayList<LatLngH>>>()
        features?.forEachIndexed { index, feature ->
            val lllh = GeoJsonUtils.getLllhFromGeometry(feature.geometry())
            if (lllh.isNotEmpty()) {
                if (feature.getProperty(NAME) != null) {
                    val name = feature.getProperty(NAME).asString
                    Timber.i("feature name: $name")
                    Timber.i("coordinates: ${lllh.size}")
                    routePairList.add(Pair(name, lllh))
                    progressMsg = "Page ${index + 1} of ${features.size}"
                    delay(20.milliseconds)
                }
            }
        }
        routePairList.sortBy { it.first }
        routePairList.forEachIndexed { index, pair ->
            routeMap[2*index] = pair
            routeMap[2*index + 1] = pair
        }
        if (routeMap.size + 2 == pageCount)
            Timber.i("OK: routeMap: ${routeMap.size} pdf pageCount: $pageCount")
        else
            Timber.e("ERROR: routeMap: ${routeMap.size} pdf pageCount: $pageCount")
        routeMapIsReady = System.currentTimeMillis()
    }

    // Scroll to the first page after the document is loaded and UI is ready
    LaunchedEffect(routeMapIsReady) {
        Timber.i("routeMapIsReady: $routeMapIsReady")
        if (routeMapIsReady == 0L) return@LaunchedEffect
        progressMsg = null
        if (pdfDocument != null) {
            // Allow time for the viewer to initialize layout
            delay(100.milliseconds)
            pdfViewerState.scrollToPage(2)
            Timber.i("Scroll to page 0")
        } else
            Timber.e("pdfDocument is null")
    }

/* replaced by geojson parsing, TextRecognition is slow
    val textLinesMap by produceState(initialValue = hashMapOf(), key1 = pdfDocument) {
        val resultMap = hashMapOf<Int, List<Text.TextBlock>> ()
        pdfDocument?.let { document ->
            val pageCount = document.pageCount
            document.getPageInfos(0 until pageCount).forEach {pageInfo ->
                Timber.i("Page pageNum: ${pageInfo.pageNum}")
                document.getPageBitmapSource(pageInfo.pageNum).use { bmpSource ->
                    val size = Size(pageInfo.width, pageInfo.height)
                    if (pageInfo.pageNum%2 == 0) {
                        Timber.i("Page ${pageInfo.pageNum + 1} of $pageCount")
                        val bmp = bmpSource.getBitmap(size)
                        Helpers.routeNameRecognition(bmp) { textBlocks ->
                            Timber.i("routeNameRecognition pageNum ${pageInfo.pageNum} textBlocks: ${textBlocks.size}")
                            resultMap[pageInfo.pageNum] = textBlocks
                            progressMsg = "Page ${pageInfo.pageNum + 1} of $pageCount"
                            if (2*resultMap.size + 1 == pageCount) {
                                value = resultMap
                                Timber.i("resultMap.size ${resultMap.size} $pageCount")
                            }
                        }
                    }
                }
            }
        }
    }
*/
    progressMsg?.let {
        //ProgressDialog()
        ProgressPopup(it)
    }


    LaunchedEffect(pdfViewerState, routeMap) {
        snapshotFlow { pdfViewerState.firstVisiblePage }
            .collectLatest { pageIndex ->
                Timber.i("Page selected: $pageIndex")
                if (routeMap[pageIndex] != null) {
                    routeDataTriple(Triple(routeMap[pageIndex]!!.first,
                        routeMap[pageIndex]!!.second.getDistanceFromLllh().formatDistM(true),
                        routeMap[pageIndex]!!.second)
                    )
                } else {
                    Timber.i("routeMap[${pageIndex}] is null")
                    /* replaced by geojson parsing
                    val textBlocks = textLinesMap[pageIndex]
                    textBlocks?.forEach { block ->
                        for (line in block.lines) {
                            val stringBuilder = StringBuilder()
                            for (element in line.elements) {
                                val elementText = element.text
                                val elementFrame = element.boundingBox
                                Timber.i(
                                    "elementConfidence: ${element.confidence} $elementText" +
                                            " ${elementFrame?.left} ${elementFrame?.top} ${elementFrame?.right} ${elementFrame?.bottom}"
                                )
                                stringBuilder.append(elementText)
                            }
                            if (stringBuilder.isNotEmpty())
                                routeNamePair(
                                    Pair(
                                        stringBuilder.toString(),
                                        block.lines.first().elements.first().text
                                    )
                                )
                            //routeNamePair(Pair(block.lines.first().elements.first().text, null))
                        }
                    }
                    */
                }
            }
    }

    pdfDocument?.let { document ->
        PdfViewer(
            pdfDocument = document,
            state = pdfViewerState,
            modifier = modifier,
//            isImageSelectionEnabled = true,
//            appendContextMenuComponents = {
//                Timber.i("Append context menu components")
//            }
        )
    }
}

@Composable
fun ProgressPopup(progressMsg: String?) {
    Popup(properties = PopupProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        alignment = Alignment.Center,
        onDismissRequest = {
        }) {
        Surface(
            color = MaterialTheme.colorScheme.onPrimary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 4.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(0.5f),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Text(text = progressMsg?: stringResource(R.string.loading), textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.inverseSurface)
                    CircularProgressIndicator()
                }
            }
        }
    }
}
@Composable
fun ProgressDialog() {
    AlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        confirmButton = {},
        title = { Text(stringResource(R.string.loading)) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    )
}

@ComposePreview(showBackground = true)
@Composable
fun DocumentViewerPreview() {
    DocumentViewer(
        finish = {},
        routeDataTripleSelection = { }
    )
}

