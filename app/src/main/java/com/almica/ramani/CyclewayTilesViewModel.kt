package com.almica.ramani

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileFilter

data class OverlayItemModel(
    val name: String,
    val path: String,
    val selected: Boolean
)

data class CycleWaySnackbarData(
    val title: String?,
    val action: CycleWaySnackbarAction?,
    val actionText: String?,
    val actionData: String?
)

enum class CycleWaySnackbarAction {
    Nothing
}

class CyclewayTilesViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(application)

    private val _useCyclewayOverlay = MutableStateFlow(
        preferences.getBoolean(Const.PREF_USE_CYCLEWAYS_OVERLAY, false)
    )
    val useCyclewayOverlay: StateFlow<Boolean> = _useCyclewayOverlay.asStateFlow()

    private val _overlayItemModels = MutableStateFlow<List<OverlayItemModel>>(emptyList())
    val overlayItemModels: StateFlow<List<OverlayItemModel>> = _overlayItemModels.asStateFlow()

    private val _snackbarData = MutableStateFlow<CycleWaySnackbarData?>(null)
    val snackbarData: StateFlow<CycleWaySnackbarData?> = _snackbarData.asStateFlow()

    private val _checkCount = MutableStateFlow(0)
    val checkCount: StateFlow<Int> = _checkCount.asStateFlow()

    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    private val mbTilesRootFolder = File(application.filesDir, Const.CYCLEWAY_FOLDER)

    init {
        loadOverlayFiles()
    }

    fun loadOverlayFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!mbTilesRootFolder.exists()) {
                mbTilesRootFolder.mkdirs()
            }

            val fileFilter = FileFilter { file: File? ->
                file?.name?.endsWith(Const.MBTILES_EXT) == true && !file.name.contains(Const.JOURNAL)
            }
            val files = mbTilesRootFolder.listFiles(fileFilter) ?: emptyArray()
            files.sortWith(compareBy { it.name })

            val tilesPrefSet = preferences.getStringSet(Const.PREF_CYCLEWAY_OVERLAYS_FILEPATH_SET, emptySet())

            val models = files.map { file ->
                OverlayItemModel(
                    name = file.name,
                    path = file.path,
                    selected = tilesPrefSet?.contains(file.path) ?: false
                )
            }

            _overlayItemModels.value = models
            _checkCount.value = models.count { it.selected }
            _hasChanges.value = false
        }
    }

    fun toggleUseCyclewayOverlay(enabled: Boolean) {
        _useCyclewayOverlay.value = enabled
        preferences.edit {
            putBoolean(Const.PREF_USE_CYCLEWAYS_OVERLAY, enabled)
        }
        showRestartSnackbar()
    }

    fun toggleOverlaySelection(index: Int) {
        val currentList = _overlayItemModels.value.toMutableList()
        val item = currentList[index]
        currentList[index] = item.copy(selected = !item.selected)
        _overlayItemModels.value = currentList
        _checkCount.value = currentList.count { it.selected }
        _hasChanges.value = true
        showRestartSnackbar()
    }

    fun confirmChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            val selectedPaths = _overlayItemModels.value
                .filter { it.selected && File(it.path).exists() }
                .map { it.path }
                .toSet()

            preferences.edit {
                putStringSet(Const.PREF_CYCLEWAY_OVERLAYS_FILEPATH_SET, selectedPaths)
            }
            _hasChanges.value = false
            showRestartSnackbar()
        }
    }

    fun deleteSelected() {
        viewModelScope.launch(Dispatchers.IO) {
            val itemsToDelete = _overlayItemModels.value.filter { it.selected }
            itemsToDelete.forEach {
                val deleted = File(it.path).delete()
                Timber.i("Deleted ${it.path}: $deleted")
            }
            loadOverlayFiles()
            
            // Update preferences after deletion
            val remainingPaths = _overlayItemModels.value
                .filter { it.selected && File(it.path).exists() }
                .map { it.path }
                .toSet()
            preferences.edit {
                putStringSet(Const.PREF_CYCLEWAY_OVERLAYS_FILEPATH_SET, remainingPaths)
            }
        }
    }

    fun shareSelected(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val selectedPaths = _overlayItemModels.value
                .filter { it.selected }
                .map { it.path }

            if (selectedPaths.isEmpty()) return@launch

            val uris = ArrayList<Uri>()
            selectedPaths.forEach { path ->
                val uri = FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".provider",
                    File(path)
                )
                uris.add(uri)
            }

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                type = "*/*"
            }

            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(shareIntent, "Share files to.."))
            }
        }
    }

    private fun showRestartSnackbar() {
        _snackbarData.value = CycleWaySnackbarData(
            getApplication<Application>().getString(R.string.after_restart),
            CycleWaySnackbarAction.Nothing,
            null,
            null
        )
    }

    fun clearSnackbar() {
        _snackbarData.value = null
    }
}
