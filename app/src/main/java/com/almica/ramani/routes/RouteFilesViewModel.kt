package com.almica.ramani.routes

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almica.ramani.BuildConfig
import com.almica.ramani.Const
import com.almica.ramani.Helpers
import com.almica.ramani.LatLngH
import com.almica.ramani.R
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.filepicker.UnzipUtils
import com.almica.ramani.utils.lllhToKmlString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileFilter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

enum class RouteMenu {
    Home,
    Map,
    Chart,
    ElevationRefreshFromSrtm,
    ElevationGmsService,
    Gradient,
    RefreshPreview,
    SaveFile,
    DeleteFile,
    ShareFile,
    ShareSnapshot,
    MoveFile,
    InsertIntoDatabase,
    Placeholder
}

enum class SnackRoutesAction {
    Nothing,
    RemoveRouteFolder,
    RemoveAllRoutes,
    ShowSrtmFiles
}

data class SnackRoutesData(
    val routeMenu: RouteMenu, val title: String,
    val action: SnackRoutesAction, val actionText: String?, val actionData: Any?
)

data class RouteFilesUiState(
    val routeEntities: List<RouteEntity> = emptyList(),
    val routeEntitiesSorted: List<RouteEntity> = emptyList(),
    val isLoading: Boolean = false,
    val showRoutesImportExportMenu: Boolean = false,
    val showSingleRouteImportExportMenu: String? = null,
    val snackRoutesData: SnackRoutesData? = null,
    val newRouteFolderMode: Boolean = false,
    val newRouteFolder: String = "",
    val createNewRouteFolder: Boolean = false,
    val showRouteMoBo: RouteEntity? = null,
    val showRouteChart: File? = null,
    val showRouteGradient: RouteEntity? = null,
    val showSrtmFiles: Boolean = false,
    val srtmFile: File? = null,
    val moveFile: Boolean = false,
    val askForNameFilter: Boolean = false,
    val askForRouteName: RouteEntity? = null,
    val dataChangeCompleted: Boolean = false,
    val notifyDataChanged: Boolean = true
)

class RouteFilesViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RouteFilesUiState())
    val uiState: StateFlow<RouteFilesUiState> = _uiState.asStateFlow()

    private val routeRepository = RouteRepository.getInstance(
        application,
        Executors.newSingleThreadExecutor()
    )

    init {
        refreshRoutes()
    }

    fun refreshRoutes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, notifyDataChanged = true) }
            val routes = withContext(Dispatchers.IO) {
                getAllRoutesSimple(getApplication())
            }
            _uiState.update { state ->
                state.copy(
                    routeEntities = routes,
                    routeEntitiesSorted = routes.sortedBy { entity -> entity.region.plus(entity.name) },
                    isLoading = false,
                    notifyDataChanged = false,
                    dataChangeCompleted = true
                )
            }
        }
    }

    private fun getAllRoutesSimple(context: Context): List<RouteEntity> {
        val routeEntities = ArrayList<RouteEntity>()
        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
        if (!rootRouteFolder.exists()) return emptyList()

        rootRouteFolder.walkTopDown().forEach { routeFile ->
            if (routeFile.isFile && !routeFile.name.endsWith(Const.GEOJSON_EXT)) {
                val routeEntity =
                    RouteEntity(
                        UUID.randomUUID(), routeFile.name,
                        routeFile.parentFile?.name.toString()
                    )
                routeEntities.add(routeEntity)
            }
        }
        return routeEntities
    }

    fun setNewRouteFolderMode(mode: Boolean) {
        _uiState.update { it.copy(newRouteFolderMode = mode) }
    }

    fun setNewRouteFolder(name: String) {
        _uiState.update { it.copy(newRouteFolder = name) }
    }

    fun createNewRouteFolder() {
        val folderName = _uiState.value.newRouteFolder
        if (folderName.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val rootRouteFolder = File(getApplication<Application>().filesDir, Const.ROUTEFOLDER)
                val routeFolder = File(rootRouteFolder, folderName)
                routeFolder.mkdirs()
                refreshRoutes()
            }
        }
        _uiState.update { it.copy(newRouteFolderMode = false, newRouteFolder = "") }
    }

    fun setShowRouteMoBo(route: RouteEntity?) {
        _uiState.update { it.copy(showRouteMoBo = route) }
    }

    fun setShowRouteChart(file: File?) {
        _uiState.update { it.copy(showRouteChart = file) }
    }

    fun setShowRouteGradient(route: RouteEntity?) {
        _uiState.update { it.copy(showRouteGradient = route) }
    }

    fun setShowSrtmFiles(show: Boolean) {
        _uiState.update { it.copy(showSrtmFiles = show) }
    }

    fun setSrtmFile(file: File?) {
        _uiState.update { it.copy(srtmFile = file) }
    }

    fun setAskForNameFilter(show: Boolean) {
        _uiState.update { it.copy(askForNameFilter = show) }
    }

    fun setAskForRouteName(route: RouteEntity?, isMove: Boolean = false) {
        _uiState.update { it.copy(askForRouteName = route, moveFile = isMove) }
    }

    fun setShowRoutesImportExportMenu(show: Boolean) {
        _uiState.update { it.copy(showRoutesImportExportMenu = show) }
    }

    fun setShowSingleRouteImportExportMenu(region: String?) {
        _uiState.update { it.copy(showSingleRouteImportExportMenu = region) }
    }

    fun setSnackRoutesData(data: SnackRoutesData?) {
        _uiState.update { it.copy(snackRoutesData = data) }
    }

    fun deleteRoute(route: RouteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            routeRepository.removeRoute(route.id)
            withContext(Dispatchers.Main) {
                refreshRoutes()
            }
        }
    }

    fun deleteRouteFolder(region: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val rootRouteFolder = File(getApplication<Application>().filesDir, Const.ROUTEFOLDER)
            val routeFolder = File(rootRouteFolder, region)
            routeFolder.deleteRecursively()
            withContext(Dispatchers.Main) {
                refreshRoutes()
            }
        }
    }

    fun deleteMainRouteFolder() {
        viewModelScope.launch(Dispatchers.IO) {
            val rootRouteFolder = File(getApplication<Application>().filesDir, Const.ROUTEFOLDER)
            rootRouteFolder.deleteRecursively()
            val thumbnailFolder = File(getApplication<Application>().filesDir, Const.THUMBNAILS)
            thumbnailFolder.deleteRecursively()
            withContext(Dispatchers.Main) {
                refreshRoutes()
            }
        }
    }

    fun exportRoutes() {
        val context = getApplication<Application>()
        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
        val timeFormat = SimpleDateFormat(Const.TIME_PATTERN_LONG_YEAR, Locale.getDefault())
        val timeTag = timeFormat.format(System.currentTimeMillis())
        val zipFile = File(context.cacheDir, "${Const.ROUTEFOLDER}_${timeTag}${Const.ZIP_EXT}")

        viewModelScope.launch(Dispatchers.IO) {
            UnzipUtils.zipFolder(rootRouteFolder, zipFile)
            val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", zipFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun exportThumbnails() {
        val context = getApplication<Application>()
        val thumbnailFolder = File(context.filesDir, Const.THUMBNAILS)
        val timeFormat = SimpleDateFormat(Const.TIME_PATTERN_LONG_YEAR, Locale.getDefault())
        val timeTag = timeFormat.format(System.currentTimeMillis())
        val zipFile = File(context.cacheDir, "${Const.THUMBNAILS}_${timeTag}${Const.ZIP_EXT}")

        viewModelScope.launch(Dispatchers.IO) {
            UnzipUtils.zipFolder(thumbnailFolder, zipFile)
            val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", zipFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun shareRouteSubFolder(initial: String) {
        val context = getApplication<Application>()
        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
        val timeFormat = SimpleDateFormat(Const.TIME_PATTERN_LONG_YEAR, Locale.getDefault())
        val timeTag = timeFormat.format(System.currentTimeMillis())
        val zipFile = File(context.cacheDir, "${Const.ROUTEFOLDER}_${initial}_${timeTag}${Const.ZIP_EXT}")
        val routeFolder = File(rootRouteFolder, initial)

        viewModelScope.launch(Dispatchers.IO) {
            UnzipUtils.zipRouteSubFolder(routeFolder, zipFile)
            val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", zipFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun applyFilter(filter: String?, region: String?) {
        val routes = _uiState.value.routeEntities
        val filtered = when {
            region != null -> routes.filter { it.region == region }
            filter != null -> routes.filter { it.name.contains(filter, ignoreCase = true) }
            else -> routes
        }
        _uiState.update { it.copy(
            routeEntitiesSorted = filtered.sortedBy { entity -> entity.region.plus(entity.name) },
            askForNameFilter = false
        ) }
    }

    fun cleanUpRouteFolder(region: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
            val routeFolder = File(rootRouteFolder, region)
            
            // Re-using logic from RouteFilesScreen.kt
            var deleteCount = 0
            var renameCount = 0
            val suffixMap = listOf(
                (Const.JPG_EXT + Const.JPG_EXT) to Const.JPG_EXT,
                (Const.KML_EXT + Const.GPX_EXT) to Const.GPX_EXT,
                (Const.KML_EXT + Const.KML_EXT) to Const.KML_EXT,
                (Const.GPX_EXT + Const.KML_EXT) to Const.KML_EXT,
                (Const.GPX_EXT + Const.GPX_EXT) to Const.GPX_EXT
            )
            
            suffixMap.forEach { (badSuffix, goodSuffix) ->
                routeFolder.listFiles { f -> f.name.endsWith(badSuffix) }?.forEach { f ->
                    val newName = f.name.removeSuffix(badSuffix) + goodSuffix
                    if (File(routeFolder, newName).delete()) deleteCount++
                    if (f.renameTo(File(routeFolder, newName))) renameCount++
                }
            }

            val files = routeFolder.listFiles { file -> file.extension.equals("jpg", ignoreCase = true) }
                ?: emptyArray()
            files.forEach { f ->
                val baseName = f.nameWithoutExtension
                if (File(routeFolder, "$baseName${Const.KML_EXT}").delete()) deleteCount++
                if (File(routeFolder, "$baseName${Const.GPX_EXT}").delete()) deleteCount++
            }

            withContext(Dispatchers.Main) {
                setSnackRoutesData(
                    SnackRoutesData(
                        RouteMenu.Placeholder,
                        context.getString(R.string.changed_files_, deleteCount, renameCount),
                        action = SnackRoutesAction.Nothing, actionText = null, null
                    )
                )
                refreshRoutes()
            }
        }
    }

    fun moveGeoJsonFile(route: RouteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val rootFolder = context.filesDir
            val routesRootFolder = File(rootFolder, Const.ROUTEFOLDER)
            val routesFolder = File(routesRootFolder, route.region)
            val sourceFile = File(routesFolder, route.name)
            val targetFile = File(routesRootFolder, route.name)
            sourceFile.copyTo(targetFile, true)
            if (targetFile.exists()) sourceFile.delete()
            
            withContext(Dispatchers.Main) {
                setSnackRoutesData(
                    SnackRoutesData(
                        RouteMenu.MoveFile,
                        context.getString(R.string.moved_to, "routes folder"),
                        action = SnackRoutesAction.Nothing, actionText = null, null
                    )
                )
                refreshRoutes()
            }
        }
    }

    fun deleteGeoJsonFile(route: RouteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
            val routeFolder = File(rootRouteFolder, route.region)
            File(rootRouteFolder, route.name).delete()
            File(routeFolder, route.name).delete()
            
            withContext(Dispatchers.Main) {
                refreshRoutes()
            }
        }
    }

    fun deleteRouteWithFile(route: RouteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
            val routeFolder = File(rootRouteFolder, route.region)
            val routeFile = File(routeFolder, route.name)
            
            routeRepository.removeRoute(route.id)
            routeFile.delete()
            
            val sharedPref = context.getSharedPreferences(
                context.getString(R.string.early_annotations),
                Context.MODE_PRIVATE
            )
            sharedPref.edit { remove(routeFile.path) }
            
            withContext(Dispatchers.Main) {
                refreshRoutes()
            }
        }
    }

    fun takeSnapshot(lllh: List<LatLngH>, region: String, name: String, routeFolder: File?, finished: () -> Unit) {
        Helpers.takeRouteSnapshot(
            getApplication(),
            lllh,
            name,
            Const.styleVectorUri,
            512,
            0.1,
            true,
            routeFolder
        ) { snapshot, _ ->
            val track = Track(lllh)
            val kmlString = lllh.lllhToKmlString(name)
            snapshot?.let {
                replaceRouteDao(name, region, kmlString, it.bitmap, track) {
                    finished()
                }
            }
        }
    }

    private fun replaceRouteDao(name: String, region: String, kmlString: String,
                               bitmap: Bitmap, track: Track, finished: () -> Unit) {
        val routeEntity = track.distance?.let {
            RouteEntity(
                name, region,
                track.startLatLng!!.latitude, track.startLatLng!!.longitude,
                track.center.latitude, track.center.longitude,
                track.stopLatLng!!.latitude, track.stopLatLng!!.longitude,
                distance = it, kmlString, bitmap
            )
        }

        if (routeEntity != null) {
            routeRepository.replaceRoute(routeEntity) {
                finished()
            }
        }
    }
}
