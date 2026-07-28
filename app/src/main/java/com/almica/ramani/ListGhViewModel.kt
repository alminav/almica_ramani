package com.almica.ramani

import android.app.Application
import android.content.Context
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

data class ListGhUiState(
    val ghFolders: List<File> = emptyList(),
    val prefGhFolderName: String? = null,
    val prefGhFolderPath: String? = null,
)

class ListGhViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)
    
    private val _uiState = MutableStateFlow(ListGhUiState())
    val uiState: StateFlow<ListGhUiState> = _uiState.asStateFlow()

    init {
        val initialPath = prefs.getString(Const.PREF_GH_FILEPATH, "") ?: ""
        _uiState.value = _uiState.value.copy(
            prefGhFolderPath = initialPath,
            prefGhFolderName = if (initialPath.isNotEmpty()) File(initialPath).name else null
        )
        refreshFolders()
    }

    fun refreshFolders() {
        viewModelScope.launch(Dispatchers.IO) {
            val folders = getGhFoldersInternal(getApplication())
            _uiState.value = _uiState.value.copy(ghFolders = folders)
        }
    }

    fun deleteSelectedFolder() {
        val path = _uiState.value.prefGhFolderPath
        if (path.isNullOrEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val ghFolder = File(path)
            if (ghFolder.exists()) {
                Timber.i("Deleting folder: $path")
                ghFolder.deleteRecursively()
                refreshFolders()
            }
        }
    }

    fun selectFolder(path: String, name: String) {
        prefs.edit {
            putString(Const.PREF_GH_FILEPATH, path)
        }
        _uiState.value = _uiState.value.copy(
            prefGhFolderPath = path,
            prefGhFolderName = name
        )
    }

    private fun getGhFoldersInternal(context: Context): List<File> {
        val rootFolder = context.filesDir
        val ghRootFolder = File(rootFolder, Const.GH_FOLDER)
        if (!ghRootFolder.exists()) {
            return emptyList()
        }
        val fileFilter = FileFilter { file -> file.isDirectory }
        val files = ghRootFolder.listFiles(fileFilter) ?: emptyArray()
        return files.sortedBy { it.name }
    }
}
