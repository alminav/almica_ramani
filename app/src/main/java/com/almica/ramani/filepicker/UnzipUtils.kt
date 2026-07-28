package com.almica.ramani.filepicker

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.almica.ramani.BuildConfig
import com.almica.ramani.Const
import com.almica.ramani.FeatureProperties
import com.almica.ramani.geojsonMaps.GeojsonMapEntity
import com.almica.ramani.geojsonMaps.GeojsonMapRepository
import com.almica.ramani.utils.GeoJsonUtils.Companion.pointToTile
import com.almica.ramani.utils.GeoJsonUtils.Companion.simplifyGeojsonMap
import com.almica.ramani.utils.GeoJsonUtils.Companion.tile2lat
import com.almica.ramani.utils.GeoJsonUtils.Companion.tile2lon
import com.almica.ramani.utils.zlibDecompress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Deque
import java.util.LinkedList
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import com.almica.ramani.utils.format

object UnzipUtils {

    @Throws(IOException::class)
    fun unzip(destDirectory: String, zipInputStream: ZipInputStream, finished: (Int, Int) -> Unit) {
        val destDir = File(destDirectory)
        if (!destDir.exists()) destDir.mkdirs()

        var bytesCount = 0L
        var filesCount = 0
        
        zipInputStream.use { zis ->
            var ze: ZipEntry?
            while (zis.nextEntry.also { ze = it } != null) {
                ze?.let { entry ->
                    val file = File(destDirectory, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        file.outputStream().use { outs ->
                            bytesCount += zis.copyTo(outs)
                        }
                        filesCount++
                    }
                    zis.closeEntry()
                }
            }
        }
        finished(bytesCount.toInt(), filesCount)
    }

    @Throws(IOException::class)
    fun unzipFolder(zipIns: ZipInputStream, destDirectory: String): FileImportActivity.SaveFileResult {
        val destDir = File(destDirectory)
        if (!destDir.exists()) destDir.mkdirs()

        var countBytes = 0L
        var countFiles = 0
        zipIns.use { zis ->
            var ze: ZipEntry?
            while (zis.nextEntry.also { ze = it } != null) {
                ze?.let { entry ->
                    val targetFile = File(destDirectory, entry.name)
                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        targetFile.outputStream().use { outs ->
                            countBytes += zis.copyTo(outs)
                        }
                        countFiles++
                    }
                    zis.closeEntry()
                }
            }
        }
        return FileImportActivity.SaveFileResult(destDirectory, countBytes, true, countFiles)
    }

    fun unzipHgt(targetFolder: File, fileName: String, zipIns: ZipInputStream, finished: (Boolean, Int, Int) -> Unit) {
        Timber.i("unzipHgt: $fileName")
        zipIns.use { zis ->
            var ze: ZipEntry?
            while (zis.nextEntry.also { ze = it } != null) {
                ze?.let { entry ->
                    val hgtFile = File(targetFolder, entry.name)
                    hgtFile.outputStream().use { outs ->
                        val bytes = zis.copyTo(outs)
                        finished(true, bytes.toInt(), 1)
                    }
                    zis.closeEntry()
                }
            }
        }
    }

    fun unzipGeojsonArchive(context: Context, zipIns: ZipInputStream, finished: (Boolean, Int, Int) -> Unit) {
        val mapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
        var success = true
        var countBytes = 0L
        var countFiles = 0
        
        zipIns.use { zis ->
            var ze: ZipEntry?
            while (zis.nextEntry.also { ze = it } != null) {
                ze?.let { entry ->
                    val filename = entry.name.substringAfterLast("/")
                    val bos = ByteArrayOutputStream()
                    zis.copyTo(bos)
                    val bytes = bos.toByteArray()
                    
                    val cleanName = filename.replace(Const.GEOJSON_EXT, "").replace(FeatureProperties.HASHTAG, "")
                    val splits = cleanName.split(FeatureProperties.UNDERLINE)
                    
                    try {
                        if (splits.size >= 4) {
                            val x = splits[1].toInt()
                            val y = splits[2].toInt()
                            val z = splits[3].toInt()

                            val lat = tile2lat(y, z)
                            val lon = tile2lon(x, z)
                            val tile10 = pointToTile(lon, lat, 10.0)
                            val region = "tile_${tile10.x}_${tile10.y}_${tile10.z}"

                            if (!filename.contains(FeatureProperties.HASHTAG)) {
                                val jsonString = bytes.zlibDecompress()
                                countBytes += bytes.size
                                val simplifyResult = simplifyGeojsonMap(jsonString)

                                if (simplifyResult != null) {
                                    mapRepository.removeGeojsonMapByXYZ(x, y, z) {}
                                    val entity = GeojsonMapEntity(x, y, z, region, false, simplifyResult, System.currentTimeMillis())
                                    mapRepository.insertGeojsonMap(entity) {}
                                } else {
                                    success = false
                                }
                            } else {
                                mapRepository.removeGeojsonMapByXYZ(x, y, z) {}
                                countBytes += bytes.size
                                val entity = GeojsonMapEntity(x, y, z, region, false, bytes, System.currentTimeMillis())
                                mapRepository.insertGeojsonMap(entity) {}
                            }
                            countFiles++
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error processing geojson entry: ${entry.name}")
                    }
                    zis.closeEntry()
                }
            }
        }
        finished(success, countBytes.toInt(), countFiles)
    }

    fun shareZippedGeojsonRegion(context: Context, region: String?, lifecycleOwner: LifecycleOwner, finished: (Int) -> Unit) {
        val archiveFile = File(context.cacheDir, 
            if (region == null) "${Const.GEOJSON_MAP_FOLDER}${Const.ZIP_EXT}" 
            else "${region.replace(Const.TILE_PREFIX, "")}${Const.ZIP_EXT}")
        
        val mapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
        mapRepository.getAllSimple(region) { maps ->
            lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                var entriesCount = 0
                ZipOutputStream(FileOutputStream(archiveFile)).use { zos ->
                    maps.forEach { entity ->
                        val ze = ZipEntry("${entity.path}/${entity.name}${Const.HASHTAG}${Const.GEOJSON_EXT}")
                        zos.putNextEntry(ze)
                        ByteArrayInputStream(entity.data).use { ins ->
                            ins.copyTo(zos)
                        }
                        zos.closeEntry()
                        entriesCount++
                    }
                }
                
                launch(Dispatchers.Main) {
                    finished(entriesCount)
                    val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", archiveFile)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        type = "*/*"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share files to.."))
                }
            }
        }
    }

    @Throws(IOException::class)
    fun zipFolder(directory: File, zipfile: File) {
        val baseUri = directory.toURI()
        val queue: Deque<File> = LinkedList<File>()
        queue.push(directory)
        
        ZipOutputStream(FileOutputStream(zipfile)).use { zout ->
            while (queue.isNotEmpty()) {
                val currentDir = queue.pop()
                currentDir.listFiles()?.forEach { kid ->
                    var name = baseUri.relativize(kid.toURI()).path
                    if (kid.isDirectory) {
                        queue.push(kid)
                        name = if (name.endsWith("/")) name else "$name/"
                        zout.putNextEntry(ZipEntry(name))
                    } else {
                        zout.putNextEntry(ZipEntry(name))
                        kid.inputStream().use { it.copyTo(zout) }
                        zout.closeEntry()
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    fun zipRouteSubFolder(directory: File, zipfile: File) {
        val baseUri = directory.toURI()
        val queue: Deque<File> = LinkedList<File>()
        queue.push(directory)
        
        ZipOutputStream(FileOutputStream(zipfile)).use { zout ->
            zout.putNextEntry(ZipEntry("${directory.name}/"))
            while (queue.isNotEmpty()) {
                val currentDir = queue.pop()
                currentDir.listFiles()?.forEach { kid ->
                    var name = baseUri.relativize(kid.toURI()).path
                    if (kid.isDirectory) {
                        queue.push(kid)
                        name = if (name.endsWith("/")) name else "$name/"
                        zout.putNextEntry(ZipEntry("${directory.name}/$name"))
                    } else {
                        zout.putNextEntry(ZipEntry("${directory.name}/$name"))
                        kid.inputStream().use { it.copyTo(zout) }
                        zout.closeEntry()
                    }
                }
            }
        }
    }


}
