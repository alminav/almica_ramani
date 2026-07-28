package com.almica.ramani.pdfcreator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import android.provider.DocumentsContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileDescriptor
import android.provider.OpenableColumns
import androidx.core.graphics.createBitmap
import java.io.IOException
import java.io.OutputStream
import androidx.core.graphics.scale
import com.almica.ramani.Const
import com.almica.ramani.FeatureProperties.Companion.LINES_TAG
import com.almica.ramani.Helpers.Companion.createMvtOfflineStyle
import com.almica.ramani.R
import com.almica.ramani.routes.drawLastPageIndicator
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.GeoJsonUtils.Companion.pointToTile
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshot
import org.maplibre.android.snapshotter.MapSnapshotter
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.sources.GeoJsonSource
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class MainViewModel : ViewModel() {

    private var _state  = MutableStateFlow(MainScreenState())
    val state = _state.asStateFlow()

    fun onImagesSelected(uris: List<Uri>,context: Context){
        Timber.i("uris: ${uris.size}")
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = state.value.copy(
                isLoading = true
            )
            val newList = _state.value.imageBitmaps.toMutableList()
            uris.forEach { uri ->
                val bitmap = uriToBitmap(uri, context)
                if (bitmap != null) {
                    newList.add(bitmap)
                }
            }
            _state.value = state.value.copy(
                imageBitmaps = newList,
                isLoading = false
            )
        }
    }

    fun onRouteFolderSelected(bitmaps: List<Bitmap>, routeFolderExtraName: String, context: Context){
        Timber.i("bitmaps: ${bitmaps.size}")
        viewModelScope.launch(Dispatchers.IO) {
            val newList = _state.value.imageBitmaps.toMutableList()
            _state.value = state.value.copy(
                isLoading = true
            )
            bitmaps.forEach { bitmap ->
                newList.add(bitmap)
            }
            val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
            val routeFolder = File(rootRouteFolder, routeFolderExtraName)
            val fileGeojson = File(context.cacheDir, "routes_${routeFolderExtraName}${Const.GEOJSON_EXT}")
            GeoJsonUtils.createGeojsonFromRouteSnapshots(context, routeFolder, fileGeojson)
            Timber.i( "fileGeojson ${fileGeojson.absolutePath} exists: ${fileGeojson.exists()}")
            _state.value = state.value.copy(
                imageBitmaps = newList,
                geojsonFile = fileGeojson,
                isLoading = false
            )
        }
    }

    fun removeImage(index:Int){
        viewModelScope.launch {
            val newList = _state.value.imageBitmaps.toMutableList()
            newList.removeAt(index)
            _state.value = state.value.copy(
                imageBitmaps = newList
            )
        }
    }

    fun writeToSelectedPath(selectedPathUri: Uri, context: Context, baseName: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Capture source geojson before createPdf clears the state
                val sourceGeojson = _state.value.geojsonFile
                val bitmaps = _state.value.imageBitmaps.toMutableList()

                if (bitmaps.isEmpty()) {
                    Timber.e("No bitmaps to write")
                    return@launch
                }

                var pdfUri: Uri?
                var geojsonUri: Uri? = null

                // Check if it's a tree URI (directory) or a single document URI
                val isTreeUri = try {
                    DocumentsContract.getTreeDocumentId(selectedPathUri) != null
                } catch (e: Exception) {
                    Timber.d("Not a tree URI: ${e.message}")
                    false
                }

                if (isTreeUri) {
                    // Handle Tree URI - Create files inside the selected directory
                    val treeId = DocumentsContract.getTreeDocumentId(selectedPathUri)
                    val parentUri = DocumentsContract.buildDocumentUriUsingTree(selectedPathUri, treeId)
                    val name = baseName?.replace(Const.PDF_EXT, "") ?: "export_${System.currentTimeMillis()}"

                    pdfUri = DocumentsContract.createDocument(context.contentResolver, parentUri, "application/pdf", "$name.pdf")
                    if (sourceGeojson != null) {
                        geojsonUri = DocumentsContract.createDocument(context.contentResolver, parentUri, "application/geo+json", "$name.geojson")
                    }
                    Timber.i("Created documents in tree: $pdfUri, $geojsonUri")
                } else {
                    // Handle Single File URI (e.g., from CreateDocument)
                    pdfUri = selectedPathUri

                    // Get the actual file name from the Uri to manage the associated geojson
                    var displayName: String? = null
                    context.contentResolver.query(selectedPathUri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex != -1) {
                            displayName = cursor.getString(nameIndex)
                        }
                    }
                    Timber.i("displayName: $displayName")

                    val geojsonName = (displayName ?: baseName ?: "export").replace(Const.PDF_EXT, "") + Const.GEOJSON_EXT

                    // Attempt to create companion geojson in the same folder
                    if (sourceGeojson != null && sourceGeojson.exists()) {
                        try {
                            val documentId = DocumentsContract.getDocumentId(selectedPathUri)
                            if (documentId.contains("/")) {
                                val parentId = documentId.substringBeforeLast("/", documentId)
                                val authority = selectedPathUri.authority!!
                                val parentUri = DocumentsContract.buildDocumentUri(authority, parentId)

                                geojsonUri = DocumentsContract.createDocument(context.contentResolver,
                                    parentUri, "application/geo+json", geojsonName)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Could not create companion geojson. Folder permission likely missing.")
                        }
                    }
                }

                // Write the GeoJSON if created
                if (geojsonUri != null && sourceGeojson != null && sourceGeojson.exists()) {
                    val geojsonString = sourceGeojson.inputStream().bufferedReader().use { it.readText() }
                    Timber.d("GeoJSON Content: $geojsonString")

                    context.contentResolver.openOutputStream(geojsonUri)?.use { outputStream ->
                        sourceGeojson.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                            createOverviewSnapshot(context, geojsonString, baseName, sourceGeojson, result = {bitmap, _ ->
                                bitmaps.add(bitmap!!)
                                val bmp: Bitmap = createBitmap(512, 512)
                                val dummyCanvas = Canvas(bmp)
                                drawLastPageIndicator(context, dummyCanvas, "LAST PAGE")
                                bitmaps.add(bmp)
                                // Write the PDF
                                pdfUri?.let { uri ->
                                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                                        createPdf(bitmaps, stream)
                                    }
                                }
                            })
                        }
                    }
                    Timber.i("Geojson copied to: $geojsonUri")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error writing to selected path")
                e.printStackTrace()
            }
        }
    }

    private suspend fun createPdf(bitmaps: List<Bitmap>, stream: OutputStream) {
        withContext(Dispatchers.IO) {
            _state.value = state.value.copy(
                isLoading = true
            )
            val document = PdfDocument()

            bitmaps.forEachIndexed { index , bitmap ->
                println("the page no is  $index")
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index+1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint()

                val scaledBm = bitmap.scale(bitmap.width, bitmap.height, false)

                canvas.drawBitmap(scaledBm, 0F, 0F, paint)
                document.finishPage(page)
            }

            try {
                document.writeTo(stream)
                _state.value = state.value.copy(
                    success = true
                )
            } catch (e: Exception) {
                _state.value = state.value.copy(
                    success = false
                )
                e.printStackTrace()
            } finally {
                // Note: android.graphics.pdf.PdfDocument does not support adding custom metadata
                // (like Title, Author) via its public API. Metadata handling typically requires
                // a library like iText or PdfBox-Android.
                document.close()
                _state.value = state.value.copy(
                    isLoading = false,
                    imageBitmaps = emptyList(),
                    geojsonFile = null
                )
            }
        }
    }

    private suspend fun uriToBitmap(uri : Uri, context: Context): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
                val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                parcelFileDescriptor.close()
                return@withContext image
            } catch (e: IOException) {
                e.printStackTrace()
            }
            null
        }
    }

}

internal fun writeSnapshotToFile(
    context: Context,
    snapshot: MapSnapshot,
    name: String?,
    snapshotFile: (File?) -> Unit
) {
    //snapshot.isShowLogo
    if (name != null) {
        Timber.i("snapshot ready withLogo ${snapshot.isShowLogo}")
        val folderThumbnails =
            File(context.filesDir, Const.THUMBNAILS)
        var b = folderThumbnails.mkdir()
        Timber.i("${folderThumbnails.path} mkdir: $b")
        var fileName = name.replace(Const.KML_EXT, Const.JPG_EXT)
            .replace(Const.GPX_EXT, Const.JPG_EXT)
            .replace(Const.GEOJSON_EXT, Const.JPG_EXT)
        if (!fileName.endsWith(Const.JPG_EXT))
            fileName += Const.JPG_EXT
        Timber.i("fileName: $fileName")
        val file = File(
            folderThumbnails,
            fileName
        )
        if (file.exists()) {
            b = file.delete()
            Timber.i("${file.path} delete $b")
        }

        val out = FileOutputStream(file)
        snapshot.bitmap.compress( //isBoundary ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG,
            Bitmap.CompressFormat.JPEG, 60, out
        )
        out.flush()
        out.close()
        Timber.i("snapShot file $name ${file.path} created")
        snapshotFile(file)
    } else
        Timber.e("name = null")
}

internal fun initGeojsonLayer(
    context: Context,
    name: String?,
    geojsonText: String
): Pair<GeoJsonSource, LineLayer> {
    val geoJsonSource = GeoJsonSource(name, geojsonText)
    //Timber.i( "${geoJsonSource.id}")
    val layerId = context.getString(R.string.routes) + LINES_TAG
    //Timber.i("layerId $layerId")
    val geoJsonLineLayer = LineLayer(layerId, geoJsonSource.id).withProperties(
        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        PropertyFactory.lineDasharray(arrayOf(0.5f, 3f)),
        //PropertyFactory.lineWidth(4.0f),

        PropertyFactory.lineWidth(
            Expression.interpolate(
                Expression.linear(
                ),
                Expression.zoom(),
                Expression.stop(
                    Expression.literal(5),
                    Expression.literal(1)
                ),
                Expression.stop(
                    Expression.literal(16),
                    Expression.literal(10)
                )
            )
        ),
        PropertyFactory.lineOpacity(0.8f),
        PropertyFactory.visibility(Property.VISIBLE),
        //PropertyFactory.lineColor(-65281))
        //PropertyFactory.lineColor(
        //rgb(literal(0.0f), literal(201.0f), literal(14.0f))))
        //rgb(Expression.get("red"), Expression.get("green"), Expression.get("blue"))))
        lineColor(Expression.toColor(Expression.get("color")))
    )
    //PropertyFactory.lineColor(android.graphics.Color.MAGENTA)) //-14065)) //android.graphics.Color.RED))
    //geoJsonLineLayer.minZoom = 7f
    return Pair(geoJsonSource, geoJsonLineLayer)
}

internal suspend fun createOverviewSnapshot(
    context: Context,
    geojsonString: String,
    baseName: String?,
    sourceGeojson: File?,
    result: suspend (Bitmap?, LatLng) -> Unit
): Unit? {
    // Prepare what we can on IO thread
    //val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    //val mvtPath = preferences.getString(Const.PREF_MVT_FILEPATH, null)
    val rootFolder = context.filesDir
    val mvtFolder = File(rootFolder, Const.MVT_FOLDER)
    val planetStyleFile = File(mvtFolder, Const.PLANET_STYLE_FILENAME)
    val planetStyleUri = Uri.fromFile(planetStyleFile).toString()
    val features = GeoJsonUtils.getFeatureCollectionFromString(geojsonString)
    Timber.i("features size: ${features?.features()?.size}")
    val boundsAll = features?.let {
        GeoJsonUtils.getFeatureCollectionBounds(it)
    }


    return boundsAll?.let { bounds ->
        val mvtTileMatch: GeoJsonUtils.Companion.Tile = pointToTile(
            boundsAll.center.longitude,
            boundsAll.center.latitude, 9.0
        )
        val mvtMatchingMap =
            "${Const.MVT_PREFIX}${mvtTileMatch.x}_${mvtTileMatch.y}_${mvtTileMatch.z}"
        val mvtMatchingFile = File(mvtFolder, mvtMatchingMap.plus(Const.MBTILES_EXT))
        var localStyleUri: String? // isNotNull ==> mvt
        if (mvtMatchingFile.exists()) {
            localStyleUri = createMvtOfflineStyle(context, mvtMatchingFile)
            Timber.i("localStyleFile: $localStyleUri")
        } else
            localStyleUri = planetStyleUri
        Timber.i("bounds: $bounds")
        withContext(Dispatchers.Main) {
            val routesGeojson =
                initGeojsonLayer(context, baseName, geojsonString)
            val builder = Style.Builder().fromUri(localStyleUri)
                .withSource(routesGeojson.first)
                .withLayer(routesGeojson.second)
            val mapSnapshotter = MapSnapshotter(
                context,
                MapSnapshotter
                    .Options(512, 512)
                    .withStyleBuilder(builder)
                    .withRegion(bounds)
                    .withLogo(false) // no effect

            )
            mapSnapshotter.start({ snapshot ->
                CoroutineScope(Dispatchers.IO).launch {
                    // We call writeSnapshotToFile to ensure the thumbnail exists for other app features
                    writeSnapshotToFile(
                        context,
                        snapshot,
                        baseName
                    ) { file ->
                        if (file == null)
                            Timber.w("thumbnail $baseName was not persisted to disk")
                    }
                    sourceGeojson?.delete()
                    result(snapshot.bitmap, boundsAll.center)
                }
            })
        }
    }
}