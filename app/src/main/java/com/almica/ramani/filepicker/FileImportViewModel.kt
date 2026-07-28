package com.almica.ramani.filepicker

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almica.ramani.filepicker.Const as PickerConst
import com.almica.ramani.Const as GlobalConst
import com.almica.ramani.FeatureProperties
import com.almica.ramani.Helpers
import com.almica.ramani.R
import com.almica.ramani.geojsonMaps.GeojsonMapEntity
import com.almica.ramani.geojsonMaps.GeojsonMapRepository
import com.almica.ramani.utils.GeoJsonUtils.Companion.pointToTile
import com.almica.ramani.utils.GeoJsonUtils.Companion.simplifyGeojsonMap
import com.almica.ramani.utils.GeoJsonUtils.Companion.tile2lat
import com.almica.ramani.utils.GeoJsonUtils.Companion.tile2lon
import com.almica.ramani.utils.zlibCompress
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.Executors
import java.util.zip.ZipInputStream
import kotlin.time.Duration.Companion.milliseconds

class FileImportViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application

    var processState by mutableStateOf(false)
        private set
    var filename by mutableStateOf(" ")
        private set
    var selectedFileType by mutableStateOf(FileType.Route)
        private set
    var showImportedFile by mutableStateOf<FileImportActivity.SaveFileResult?>(null)
    var popupSnackMsg: String? by mutableStateOf(null)
        private set

    var fileDirectDownloadUrl by mutableStateOf<String?>(null)

    private var routeFolderExtraName: String? = null

    fun initializeFromIntent(intent: Intent) {
        val filetypeName = intent.getStringExtra(PickerConst.EXTRA_FILETYPE)
        selectedFileType = FileType.entries.find { it.name == filetypeName } ?: FileType.Route
        fileDirectDownloadUrl = intent.getStringExtra(PickerConst.EXTRA_DIRECT_DOWNLOAD_URL)
        routeFolderExtraName = intent.getStringExtra(GlobalConst.EXTRA_ROUTEFOLDER)
        
        Timber.i("Initialized with fileType: $selectedFileType, directDownload: $fileDirectDownloadUrl")
    }

    fun onFileSelected(uri: Uri) {
        if (uri == Uri.EMPTY) return

        viewModelScope.launch {
            val metadata = queryUriMetadata(uri)
            if (metadata != null) {
                filename = metadata.first
                processFile(uri, filename)
            } else {
                Timber.e("Could not query metadata, using URI segment as filename")
                filename = uri.lastPathSegment ?: "imported_file"
                processFile(uri, filename)
            }
        }
    }

    private fun queryUriMetadata(uri: Uri): Pair<String, Int>? {
        return try {
            appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx != -1 && sizeIdx != -1) {
                    cursor.getString(nameIdx) to cursor.getInt(sizeIdx)
                } else null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to query URI metadata")
            null
        }
    }

    private fun processFile(uri: Uri, fileName: String) {
        processState = true
        saveFile(uri, selectedFileType, fileName)
    }

    fun dismissPopup() {
        popupSnackMsg = null
    }

    fun onDirectDownload() {
        val url = fileDirectDownloadUrl ?: return
        if (selectedFileType == FileType.GeoJsonZip) {
            processState = true
            directDriveDownloadGeojsonRegion(url)
        }
    }

    private fun saveFile(uri: Uri, fileType: FileType, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (fileType) {
                    FileType.Route, FileType.RouteThumbnail -> handleRouteImport(uri, name)
                    FileType.RoutesZip -> handleZipImport(uri, File(appContext.filesDir, GlobalConst.ROUTEFOLDER))
                    FileType.ThumbnailsZip -> handleZipImport(uri, File(appContext.filesDir, GlobalConst.THUMBNAILS))
                    FileType.GhFolderZip -> handleFolderZipImport(uri, name, File(appContext.filesDir, PickerConst.GH_FOLDER), PickerConst.GHZ_EXT)
                    FileType.GeojsonQgisZip -> handleFolderZipImport(uri, name, File(appContext.filesDir, GlobalConst.GEOJSON_ROOT_FOLDER), PickerConst.ZIP_EXT)
                    FileType.MbTiles -> handleSingleFileImport(uri, name, File(appContext.filesDir, PickerConst.MBTILES_FOLDER))
                    FileType.Mvt -> handleSingleFileImport(uri, name, File(appContext.filesDir, PickerConst.MVT_FOLDER))
                    FileType.CycleWay -> handleSingleFileImport(uri, name, File(appContext.filesDir, PickerConst.CYCLEWAY_FOLDER))
                    FileType.GeoJson -> handleGeoJsonImport(uri, name)
                    FileType.Hgt -> handleHgtImport(uri, name)
                    FileType.GeoJsonZip -> handleGeoJsonZipImport(uri)
                    else -> Timber.w("Unsupported file type: $fileType")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error saving file $name")
                showImportedFile = FileImportActivity.SaveFileResult(name, 0, false, 0)
            } finally {
                processState = false
            }
        }
    }

    private fun handleRouteImport(uri: Uri, name: String) {
        val routesRootFolder = File(appContext.filesDir, GlobalConst.ROUTEFOLDER)
        var bytes = 0L
        var success = false
        val routeFolder = routeFolderExtraName?.let { File(routesRootFolder, it) }
        
        if (routeFolder != null) {
            routeFolder.mkdirs()
            val destFile = File(routeFolder, name)
            appContext.contentResolver.openInputStream(uri)?.use { ins ->
                destFile.outputStream().use { outs ->
                    bytes = ins.copyTo(outs)
                    success = true
                }
            }
        }

        if (success) {
            var finalName = name
            if (name.endsWith(PickerConst.JPG_EXT, ignoreCase = true)) {
                finalName = postProcessJpg(routeFolder!!, name)
            } else {
                showPopup(appContext.getString(R.string.file_imported_successfully_, name))
            }
            showImportedFile = FileImportActivity.SaveFileResult(finalName, bytes, true, 0)
        } else {
            showImportedFile = FileImportActivity.SaveFileResult(name, 0, false, 0)
        }
    }

    private fun postProcessJpg(routeFolder: File, name: String): String {
        val file = File(routeFolder, name)
        if (!file.exists()) return name

        return try {
            val exif = ExifInterface(file)
            val imageDescription = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)
            val splits = imageDescription?.split(" ")
            if (splits != null && splits.size == 2 && splits[0] == "geojson") {
                val originalFileName = splits[1]
                if (file.renameTo(File(routeFolder, originalFileName))) {
                    showPopup(appContext.getString(R.string.file_renamed_to).plus(" $originalFileName"))
                    return originalFileName
                }
            } else {
                // EXIF KML processing from Activity
                val kmlString = Helpers.getKmlStringFromExif(file)
                val extractedName = kmlString?.replace("Name", "name")
                    ?.substringAfter("<name>", "")
                    ?.substringBefore("</name>", "")
                    ?.takeIf { it.isNotEmpty() }

                if (!extractedName.isNullOrEmpty()) {
                    val sanitized = extractedName.replace(PickerConst.GPX_EXT, "").replace(PickerConst.KML_EXT, "")
                        .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue")
                        .replace("Ä", "Ae").replace("Ö", "Oe").replace("Ü", "Ue")
                    
                    val targetName = if (sanitized.endsWith(PickerConst.JPG_EXT, ignoreCase = true)) sanitized 
                                     else sanitized + PickerConst.JPG_EXT
                    
                    if (file.renameTo(File(routeFolder, targetName))) {
                        return targetName
                    }
                }
            }
            name
        } catch (e: Exception) {
            Timber.e(e, "Exif processing failed")
            name
        }
    }

    private fun handleZipImport(uri: Uri, destDir: File) {
        appContext.contentResolver.openInputStream(uri)?.use { ins ->
            var bCount = 0
            var fCount = 0
            UnzipUtils.unzip(destDir.path, ZipInputStream(ins)) { b, f ->
                bCount = b
                fCount = f
            }
            showImportedFile = FileImportActivity.SaveFileResult(destDir.name, bCount.toLong(), true, fCount)
        }
    }

    private fun handleFolderZipImport(uri: Uri, name: String, root: File, ext: String) {
        val folderName = name.replace(ext, "")
        val target = File(root, folderName).apply { mkdirs() }
        appContext.contentResolver.openInputStream(uri)?.use { ins ->
            val result = UnzipUtils.unzipFolder(ZipInputStream(ins), target.path)
            showImportedFile = result
        }
    }

    private fun handleSingleFileImport(uri: Uri, name: String, destDir: File) {
        destDir.mkdirs()
        val destFile = File(destDir, name)
        appContext.contentResolver.openInputStream(uri)?.use { ins ->
            destFile.outputStream().use { outs ->
                val bytes = ins.copyTo(outs)
                showImportedFile = FileImportActivity.SaveFileResult(name, bytes, true, 0)
            }
        }
    }

    private fun handleGeoJsonImport(uri: Uri, name: String) {
        val mapRepository = GeojsonMapRepository.getInstance(appContext, Executors.newSingleThreadExecutor())
        val cleanName = name.replace(GlobalConst.GEOJSON_EXT, "")
            .replace(FeatureProperties.HASHTAG, "")

        val splits = cleanName.split(FeatureProperties.UNDERLINE)
        if (splits.size < 4) return
        
        val x = splits[1].toInt()
        val y = splits[2].toInt()
        val z = splits[3].toInt()

        val lat = tile2lat(y, z)
        val lon = tile2lon(x, z)
        val tile10 = pointToTile(lon, lat, 10.0)
        val region = "tile_${tile10.x}_${tile10.y}_${tile10.z}"

        val bos = ByteArrayOutputStream()
        val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.copyTo(bos) } ?: 0L
        val jsonString = bos.toByteArray().toString(Charset.defaultCharset())

        if (bytes > 0) {
            if (!name.contains(FeatureProperties.HASHTAG)) {
                val simplifyResult = simplifyGeojsonMap(jsonString)
                if (simplifyResult != null) {
                    mapRepository.removeGeojsonMapByXYZ(x, y, z) {}
                    val entity = GeojsonMapEntity(x, y, z, region, false, simplifyResult, System.currentTimeMillis())
                    mapRepository.insertGeojsonMap(entity) { success ->
                        showImportedFile = FileImportActivity.SaveFileResult(name, bytes, success, 0)
                    }
                }
            } else {
                mapRepository.removeGeojsonMapByXYZ(x, y, z) {}
                val compressed = jsonString.zlibCompress()
                val entity = GeojsonMapEntity(x, y, z, region, false, compressed, System.currentTimeMillis())
                mapRepository.insertGeojsonMap(entity) { success ->
                    showImportedFile = FileImportActivity.SaveFileResult(name, bytes, success, 0)
                }
            }
        }
    }

    private fun handleHgtImport(uri: Uri, name: String) {
        val target = File(appContext.filesDir, PickerConst.HGT_FOLDER).apply { mkdirs() }
        appContext.contentResolver.openInputStream(uri)?.use { ins ->
            UnzipUtils.unzipHgt(target, name, ZipInputStream(ins)) { success, bytes, files ->
                showImportedFile = FileImportActivity.SaveFileResult(name, bytes.toLong(), success, files)
            }
        }
    }

    private fun handleGeoJsonZipImport(uri: Uri) {
        appContext.contentResolver.openInputStream(uri)?.use { ins ->
            UnzipUtils.unzipGeojsonArchive(appContext, ZipInputStream(ins)) { success, bytes, files ->
                showImportedFile = FileImportActivity.SaveFileResult("GeoJson Archive", bytes.toLong(), success, files)
            }
        }
    }

    private fun directDriveDownloadGeojsonRegion(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                appContext.contentResolver.openInputStream(url.toUri())?.use { ins ->
                    UnzipUtils.unzipGeojsonArchive(appContext, ZipInputStream(ins)) { success, bytes, files ->
                        showImportedFile = FileImportActivity.SaveFileResult("Direct Download", bytes.toLong(), success, files)
                        fileDirectDownloadUrl = null
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Direct download failed")
                showImportedFile = FileImportActivity.SaveFileResult("Direct Download", 0, false, 0)
            } finally {
                processState = false
            }
        }
    }

    private fun showPopup(msg: String) {
        viewModelScope.launch {
            popupSnackMsg = msg
            delay(5000.milliseconds)
            popupSnackMsg = null
        }
    }
}
