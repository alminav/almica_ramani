package com.almica.ramani

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.edit
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.almica.ramani.utils.DriveSharedLinks
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.format
import com.almica.ramani.utils.getCenter
import com.almica.ramani.utils.isNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileFilter
import kotlin.time.Duration.Companion.seconds

data class MvtItemModel(
    val name: String,
    val path: String,
    val x: Int,
    val y: Int,
    var selected: Boolean,
    val exists: Boolean,
)

data class MvtSnackbarData(
    val title: String?,
    val action: MvtSnackbarAction?,
    val actionText: String?,
    val actionData: Any?,
    val thumbNail: Bitmap?,
)

enum class MvtSnackbarAction {
    Nothing,
    Share,
}

data class ListMvtUiState(
    val mvtItemModels: List<MvtItemModel> = emptyList(),
    val driveItemModels: Map<Int, List<MvtItemModel>> = emptyMap(),
    val snackbarData: MvtSnackbarData? = null,
    val restartRequired: Boolean = false,
    val showDriveEntries: Boolean = false,
    val showGoogleMap: String? = null,
    val clipText: String? = null,
    val prefMvtDatabasePath: String? = null,
    val currentMvtName: String = "",
    val isLoading: Boolean = false,
)

class ListMvtViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ListMvtUiState())
    val uiState: StateFlow<ListMvtUiState> = _uiState.asStateFlow()

    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)

    init {
        refreshMvtFiles()
    }

    fun refreshMvtFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val context = getApplication<Application>()
            val rootFolder = context.filesDir
            val mvtFolder = File(rootFolder, Const.MVT_FOLDER)
            if (!mvtFolder.exists()) mvtFolder.mkdirs()

            val files = getMvtFiles(mvtFolder)
            val currentPrefPath = prefs.getString(Const.PREF_MVT_FILEPATH, null)
            Timber.i("refreshMvtFiles: $currentPrefPath")
            val models = createMvtItemModels(context, files, currentPrefPath)
            
            // Process Drive Entries
            val currentMvtFile = File(currentPrefPath ?: "")
            val driveMap = DriveSharedLinks.Companion.MvtRegions().list
            val driveModels = driveMap.keys.sorted().map { regionName ->
                val f = File(mvtFolder, "$regionName${Const.MBTILES_EXT}")
                val splits = regionName.split(Const.UNDERLINE, limit = 4)
                MvtItemModel(
                    name = regionName,
                    path = "", // Path is empty for drive entries until downloaded
                    x = splits.getOrNull(1)?.toIntOrNull() ?: 0,
                    y = splits.getOrNull(2)?.toIntOrNull() ?: 0,
                    selected = currentPrefPath != null && 
                            currentMvtFile.exists() && 
                            currentMvtFile.name == regionName,
                    exists = f.exists()
                )
            }
            val groupedDriveModels = driveModels.groupBy { it.x }

            _uiState.update {
                it.copy(
                    mvtItemModels = models,
                    driveItemModels = groupedDriveModels,
                    prefMvtDatabasePath = currentPrefPath,
                    currentMvtName = currentMvtFile.name.replace(Const.MBTILES_EXT, ""),
                    isLoading = false
                )
            }
        }
    }

    private fun getMvtFiles(mvtFolder: File): Array<File> {
        val fileFilter = FileFilter { file: File? ->
            (file?.name?.startsWith(Const.MVT_PREFIX) == true) &&
                    (file.name.endsWith(Const.MBTILES_EXT)) &&
                    (!file.name.contains(Const.JOURNAL))
        }
        return mvtFolder.listFiles(fileFilter)?.apply { sortWith(compareBy { it.name }) } ?: emptyArray()
    }

    private fun createMvtItemModels(
        context: Context,
        files: Array<File>,
        currentMvtPath: String?
    ): List<MvtItemModel> {
        val mvtFolder = File(context.filesDir, Const.MVT_FOLDER)
        return files.map { file ->
            val splits = file.name.replace(Const.MBTILES_EXT, "").split(Const.UNDERLINE, limit = 4)
            MvtItemModel(
                name = file.name,
                path = file.path,
                x = splits.getOrNull(1)?.toIntOrNull() ?: 0,
                y = splits.getOrNull(2)?.toIntOrNull() ?: 0,
                selected = currentMvtPath == file.path,
                exists = File(mvtFolder, file.name).exists()
            )
        }
    }

    fun onMvtSelected(model: MvtItemModel, onNewMvtMap: (String?) -> Unit) {
        val newSelected = !model.selected
        if (newSelected) {
            onNewMvtMap(model.name)
            prefs.edit { putString(Const.PREF_MVT_FILEPATH, model.path) }
        } else {
            onNewMvtMap(null)
            prefs.edit { remove(Const.PREF_MVT_FILEPATH) }
        }
        _uiState.update { it.copy(restartRequired = true) }
        refreshMvtFiles()
        val context = getApplication<Application>()
        showSnackbar(MvtSnackbarData(context.getString(R.string.after_restart), MvtSnackbarAction.Nothing, null, null, null))
    }

    fun deleteMvtFile(model: MvtItemModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(model.path)
            val deleted = file.delete()
            if (deleted && _uiState.value.prefMvtDatabasePath == model.path) {
                prefs.edit { remove(Const.PREF_MVT_FILEPATH) }
            }
            refreshMvtFiles()
            val context = getApplication<Application>()
            showSnackbar(
                MvtSnackbarData(
                    context.getString(R.string.deleted_file_, file.name, deleted.toString()),
                    MvtSnackbarAction.Nothing,
                    null,
                    null,
                    null
                )
            )
        }
    }

    fun shareMvtFile(model: MvtItemModel) {
        val file = File(model.path)
        val driveMap = DriveSharedLinks.Companion.MvtRegions().list
        val driveUrl = driveMap[file.name]
        val context = getApplication<Application>()

        if (driveUrl.isNotNull()) {
            showSnackbar(
                MvtSnackbarData(
                    context.getString(R.string.map_available_on_drive, file.name),
                    MvtSnackbarAction.Share,
                    context.getString(R.string.continue_anyway),
                    file,
                    null
                )
            )
        } else {
            Timber.i("shareMvtFile: ${file.name}")
            showSnackbar(MvtSnackbarData(file.name,
                MvtSnackbarAction.Share, context.getString(R.string.share), file, null))
        }
    }

    fun takeSnapshotAndShowInfo(model: MvtItemModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val file = File(model.path)
            takeSnapshot(context, file) { snapshot, bounds ->
                val centerLatLng = bounds.getCenter()
                showSnackbar(
                    MvtSnackbarData(
                        "${model.name.replace(Const.MBTILES_EXT, "")} ${Const.UC_POSITION} " +
                                "${centerLatLng.latitude.format(2)}° ${centerLatLng.longitude.format(2)}°",
                        MvtSnackbarAction.Nothing,
                        null,
                        null,
                        snapshot?.bitmap
                    )
                )
            }
        }
    }

    private fun takeSnapshot(context: Context, mvtFile: File, finish: (org.maplibre.android.snapshotter.MapSnapshot?, ArrayList<LatLngH>) -> Unit) {
        var stylePlanetUri: String? = null
        val styleFile = File(mvtFile.parentFile, Const.PLANET_STYLE_FILENAME)
        if (styleFile.exists())
            stylePlanetUri = Uri.fromFile(styleFile).toString()
        val lllhBounds = arrayListOf<LatLngH>()
        if (mvtFile.name.startsWith(Const.MVT_PREFIX)) {
            val splits = mvtFile.name.replace(Const.MBTILES_EXT, "").split(Const.UNDERLINE, limit = 4)
            if (splits.getOrNull(1)?.isDigitsOnly() == true && splits.getOrNull(2)?.isDigitsOnly() == true && splits.getOrNull(3)?.isDigitsOnly() == true) {
                val tile = GeoJsonUtils.Companion.Tile(
                    splits[1].toInt(),
                    splits[2].toInt(),
                    splits[3].toInt()
                )

                val tileBounds = GeoJsonUtils.tileToBounds(tile)
                lllhBounds.add(LatLngH(tileBounds.northWest.latitude, tileBounds.northWest.longitude))
                lllhBounds.add(LatLngH(tileBounds.northEast.latitude, tileBounds.northEast.longitude))
                lllhBounds.add(LatLngH(tileBounds.southEast.latitude, tileBounds.southEast.longitude))
                lllhBounds.add(LatLngH(tileBounds.southWest.latitude, tileBounds.southWest.longitude))
                lllhBounds.add(LatLngH(tileBounds.northWest.latitude, tileBounds.northWest.longitude))
            }
        }
        if (lllhBounds.isEmpty()) {
            val bounds = Helpers.getMvtBoundsFromMeta(mvtFile)
            lllhBounds.add(LatLngH(bounds.northWest.latitude, bounds.northWest.longitude))
            lllhBounds.add(LatLngH(bounds.northEast.latitude, bounds.northEast.longitude))
            lllhBounds.add(LatLngH(bounds.southEast.latitude, bounds.southEast.longitude))
            lllhBounds.add(LatLngH(bounds.southWest.latitude, bounds.southWest.longitude))
            lllhBounds.add(LatLngH(bounds.northWest.latitude, bounds.northWest.longitude))
        }
        Helpers.takeSnapshot(
            context = context,
            lllhBounds = lllhBounds,
            name = mvtFile.name.replace(Const.MBTILES_EXT, Const.JPG_EXT),
            styleUri = if (stylePlanetUri.isNotNull()) stylePlanetUri else Const.styleVectorUri,
            size = 512,
            border = 0.25,
            writeFile = false,
        ) { snapshot ->
            finish(snapshot, lllhBounds)
        }
    }

    fun showSnackbar(data: MvtSnackbarData) {
        _uiState.update { it.copy(snackbarData = data) }
        viewModelScope.launch {
            delay(5.seconds)
            if (_uiState.value.snackbarData == data) {
                _uiState.update { it.copy(snackbarData = null) }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarData = null) }
    }

    fun setShowDriveEntries(show: Boolean) {
        _uiState.update { it.copy(showDriveEntries = show) }
    }

    fun setShowGoogleMap(name: String?) {
        _uiState.update { it.copy(showGoogleMap = name) }
    }

    fun setClipText(text: String?) {
        _uiState.update { it.copy(clipText = text) }
    }

    fun clearClipText() {
        _uiState.update { it.copy(clipText = null) }
    }
}
