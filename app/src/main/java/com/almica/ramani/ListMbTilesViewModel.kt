package com.almica.ramani

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.edit
import androidx.core.graphics.scale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.almica.ramani.tilemaker.MbtilesDatabase
import com.almica.ramani.utils.simpleStringWithTime
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
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class ListMbTilesUiState(
    val rasterItemModels: List<RasterMapItemModel> = emptyList(),
    val itemsGrouped: Map<String, List<RasterMapItemModel>> = emptyMap(),
    val checkCount: Int = 0,
    val snackbarData: MbTilesSnackbarData? = null,
    val isLoading: Boolean = false,
    val showDriveEntries: Boolean = false,
    val showGoogleMap: String? = null,
    val clipText: String? = null,
    val restartRequired: Boolean = false
)

class ListMbTilesViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ListMbTilesUiState())
    val uiState: StateFlow<ListMbTilesUiState> = _uiState.asStateFlow()

    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)

    init {
        // refreshRasterMaps() is triggered by screen width availability in the UI
    }

    fun refreshRasterMaps(screenWidthPx: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val context = getApplication<Application>()
            val rootFolder = context.filesDir
            val mbTilesRootFolder = File(rootFolder, Const.MBTILES_FOLDER)
            if (!mbTilesRootFolder.exists()) mbTilesRootFolder.mkdirs()

            val files = getRasterMapFiles(mbTilesRootFolder)
            val models = createItemModels(context, files, screenWidthPx)
            
            val count = models.count { it.selected && File(it.path).exists() }
            val grouped = models.groupBy { it.mapType }

            delay(1000.milliseconds) // Keep original delay if intentional
            _uiState.update {
                it.copy(
                    rasterItemModels = models,
                    itemsGrouped = grouped,
                    checkCount = count,
                    isLoading = false
                )
            }
        }
    }

    private fun getRasterMapFiles(mbTilesRootFolder: File): Array<File> {
        val fileFilter = FileFilter { file: File? ->
            file?.name?.endsWith(Const.MBTILES_EXT) == true &&
                    !file.name.contains(Const.JOURNAL)
        }
        return mbTilesRootFolder.listFiles(fileFilter)?.apply { sortWith(compareBy { it.name }) } ?: emptyArray()
    }

    private fun createItemModels(
        context: Context,
        files: Array<File>,
        screenWidthPx: Int?
    ): List<RasterMapItemModel> {
        val tilesPrefSet = prefs.getStringSet(Const.PREF_MBTILES_FILEPATH_SET, setOf()) ?: setOf()
        return files.map { file ->
            val lastModifiedDate = Date(file.lastModified())
            val regionName = file.name.replace(Const.MBTILES_EXT, "")
            val splits = regionName.split(Const.UNDERLINE, limit = 5)
            val bitmap = getBitmapForRegion(context, regionName, false)
            
            var scaledBitmap: Bitmap? = null
            if (bitmap != null && screenWidthPx != null) {
                val scale = 0.75 * screenWidthPx / bitmap.width
                scaledBitmap = bitmap.scale((bitmap.width * scale).toInt(), (bitmap.height * scale).toInt())
            } else {
                scaledBitmap = bitmap
            }

            val active = tilesPrefSet.contains(file.path)
            RasterMapItemModel(
                name = file.name,
                path = file.path,
                thumbnail = scaledBitmap,
                lastModifiedDate = lastModifiedDate.simpleStringWithTime(),
                mapType = if (splits.size > 3) splits[4] else context.getString(R.string.unknown),
                selected = active
            )
        }
    }

    fun changeItemState(name: String, currentState: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val model = _uiState.value.rasterItemModels.find { it.name == name }
            model?.let {
                var tilesPrefSet = prefs.getStringSet(Const.PREF_MBTILES_FILEPATH_SET, setOf())?.toMutableSet() ?: mutableSetOf()
                if (currentState) {
                    tilesPrefSet.remove(it.path)
                } else {
                    if (File(it.path).exists()) {
                        tilesPrefSet.add(it.path)
                    }
                }
                prefs.edit { putStringSet(Const.PREF_MBTILES_FILEPATH_SET, tilesPrefSet) }
                showSnackbar(MbTilesSnackbarData(getApplication<Application>().getString(R.string.after_restart), MbTilesSnackbarAction.Nothing, null, null))
                refreshRasterMaps()
            }
        }
    }

    fun deleteSelectedMaps() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val itemsToDelete = _uiState.value.rasterItemModels.filter { it.selected }
            val result = deleteMaps(context, itemsToDelete)
            
            var tilesPrefSet = prefs.getStringSet(Const.PREF_MBTILES_FILEPATH_SET, setOf())?.toMutableSet() ?: mutableSetOf()
            result.forEach { tilesPrefSet.remove(it) }
            prefs.edit { putStringSet(Const.PREF_MBTILES_FILEPATH_SET, tilesPrefSet) }
            
            refreshRasterMaps()
        }
    }

    fun deleteSingleMap(model: RasterMapItemModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val result = deleteMaps(context, listOf(model.copy(selected = true)))
            
            var tilesPrefSet = prefs.getStringSet(Const.PREF_MBTILES_FILEPATH_SET, setOf())?.toMutableSet() ?: mutableSetOf()
            result.forEach { tilesPrefSet.remove(it) }
            prefs.edit { putStringSet(Const.PREF_MBTILES_FILEPATH_SET, tilesPrefSet) }
            
            refreshRasterMaps()
        }
    }

    private fun deleteMaps(context: Context, items: List<RasterMapItemModel>): List<String> {
        val deletedFiles = mutableListOf<String>()
        items.forEach {
            if (it.selected) {
                val dbFile = MbtilesDatabase.DatabaseContext(context).getDatabasePath(it.name)
                val b = dbFile.delete()
                val dbJournal = File("${it.path}${Const.DB_JOURNAL_SUFFIX}")
                val bj = dbJournal.delete()
                Timber.i("delete database:${it.name} $b journal:${dbJournal.name} $bj")
                deletedFiles.add(it.path)
            }
        }
        return deletedFiles
    }

    fun confirmChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            var tilesPrefSet = prefs.getStringSet(Const.PREF_MBTILES_FILEPATH_SET, setOf())?.toMutableSet() ?: mutableSetOf()
            
            // Cleanup non-existent files
            val iterator = tilesPrefSet.iterator()
            while (iterator.hasNext()) {
                if (!File(iterator.next()).exists()) iterator.remove()
            }

            _uiState.value.rasterItemModels.forEach { model ->
                if (model.selected && File(model.path).exists()) {
                    tilesPrefSet.add(model.path)
                } else {
                    tilesPrefSet.remove(model.path)
                }
            }
            
            prefs.edit { putStringSet(Const.PREF_MBTILES_FILEPATH_SET, tilesPrefSet) }
            _uiState.update { it.copy(restartRequired = true) }
            showSnackbar(MbTilesSnackbarData(context.getString(R.string.after_restart), MbTilesSnackbarAction.Nothing, null, null))
            refreshRasterMaps()
        }
    }

    fun showSnackbar(data: MbTilesSnackbarData) {
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
