package com.almica.ramani.geojsonMaps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.almica.ramani.Const
import com.almica.ramani.googlemaps.NewMapAction
import com.almica.ramani.utils.DriveSharedLinks
import com.almica.ramani.utils.GeoJsonUtils
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileFilter
import androidx.core.content.edit

data class TilemakerUiState(
    val x: Int = 0,
    val y: Int = 0,
    val zoom: Int = 10,
    val mapType: String = Const.OUTDOOR,
    val regionName: String = "",
    val bounds: org.maplibre.android.geometry.LatLngBounds = GeoJsonUtils.tileToBounds(GeoJsonUtils.Companion.Tile(0, 0, 10)),
    val fileNames: List<String> = emptyList(),
    val tilesPrefSet: Set<String> = emptySet(),
    val rasterRegionNames: List<String> = emptyList(),
    val isMapLoaded: Boolean = false,
    val showGeoCoder: Boolean = false,
    val showDropDownRasterMaptype: Boolean = false,
    val listDriveEntries: Boolean = false,
    val moboDeleteConfirmation: String? = null,
    val createMbTileRegion: String? = null,
    val progressCreateTilename: String? = null,
    val progressCreateValue: Int = 0,
    val clipText: String? = null,
    val startLocation: LatLng? = null,
    val isTileActive: Boolean = false,
    val canImportFromDrive: Boolean = false,
    val checkRegionPath: String = ""
)

class TilemakerViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TilemakerUiState())
    val uiState: StateFlow<TilemakerUiState> = _uiState.asStateFlow()

    private val preferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val rootFolder = application.filesDir
    private val mbTilesRootFolder = File(rootFolder, Const.MBTILES_FOLDER)
    private val driveMap = DriveSharedLinks.Companion.RasterMaps().list

    init {
        val lat = savedStateHandle.get<Double>(Const.EXTRA_LATITUDE) ?: -1.0
        val lon = savedStateHandle.get<Double>(Const.EXTRA_LONGITUDE) ?: -1.0
        if (lat >= 0 && lon >= 0) {
            val tile = GeoJsonUtils.pointToTile(lon, lat, _uiState.value.zoom.toDouble())
            _uiState.update {
                it.copy(
                    x = tile.x,
                    y = tile.y,
                    startLocation = LatLng(lat, lon)
                )
            }
        }
        updateRegionNameAndBounds()
        refreshFileData()
    }

    fun refreshFileData() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!mbTilesRootFolder.exists()) mbTilesRootFolder.mkdirs()
            
            val fileFilter = FileFilter { file: File? ->
                file?.name?.endsWith(Const.MBTILES_EXT) == true &&
                        !file.name.contains(Const.JOURNAL)
            }
            
            val files = mbTilesRootFolder.listFiles(fileFilter) ?: emptyArray()
            val fileNames = files.map { it.name.replace(Const.MBTILES_EXT, "") }
            
            val rasterRegionNames = driveMap.keys.toList()
            val tilesPrefSet = preferences.getStringSet(Const.PREF_MBTILES_FILEPATH_SET, emptySet()) ?: emptySet()
            
            _uiState.update {
                it.copy(
                    fileNames = fileNames,
                    rasterRegionNames = rasterRegionNames,
                    tilesPrefSet = tilesPrefSet
                )
            }
            updateComputedState()
        }
    }

    private fun updateComputedState() {
        val state = _uiState.value
        val checkRegionPath = "${mbTilesRootFolder.path}/${state.regionName}${Const.MBTILES_EXT}"
        val isTileActive = state.tilesPrefSet.contains(checkRegionPath)
        val canImportFromDrive = driveMap.keys.contains(state.regionName + Const.MBTILES_EXT) &&
                !state.fileNames.contains(state.regionName)

        _uiState.update {
            it.copy(
                checkRegionPath = checkRegionPath,
                isTileActive = isTileActive,
                canImportFromDrive = canImportFromDrive
            )
        }
    }

    fun updateCoordinates(x: Int, y: Int) {
        _uiState.update { it.copy(x = x, y = y) }
        updateRegionNameAndBounds()
    }

    fun updateMapType(mapType: String) {
        _uiState.update { it.copy(mapType = mapType) }
        updateRegionNameAndBounds()
    }

    private fun updateRegionNameAndBounds() {
        val state = _uiState.value
        val tile = GeoJsonUtils.Companion.Tile(state.x, state.y, state.zoom)
        val regionName = "tile_${state.x}_${state.y}_${state.zoom}_${state.mapType}"
        val bounds = GeoJsonUtils.tileToBounds(tile)
        _uiState.update { it.copy(regionName = regionName, bounds = bounds) }
        updateComputedState()
    }

    fun setMapLoaded(loaded: Boolean) {
        _uiState.update { it.copy(isMapLoaded = loaded) }
    }

    fun setShowGeoCoder(show: Boolean) {
        _uiState.update { it.copy(showGeoCoder = show) }
    }

    fun setShowDropDownRasterMaptype(show: Boolean) {
        _uiState.update { it.copy(showDropDownRasterMaptype = show) }
    }

    fun setListDriveEntries(show: Boolean) {
        _uiState.update { it.copy(listDriveEntries = show) }
    }

    fun setMoboDeleteConfirmation(message: String?) {
        _uiState.update { it.copy(moboDeleteConfirmation = message) }
    }

    fun setClipText(text: String?) {
        _uiState.update { it.copy(clipText = text) }
    }

    fun startCreateMbTile(regionName: String) {
        _uiState.update { 
            it.copy(
                createMbTileRegion = regionName,
                progressCreateTilename = regionName
            ) 
        }
    }

    fun updateCreateProgress(progress: Int) {
        _uiState.update { it.copy(progressCreateValue = progress) }
        if (progress >= 100) {
            _uiState.update { 
                it.copy(
                    createMbTileRegion = null,
                    progressCreateTilename = null,
                    progressCreateValue = 0
                ) 
            }
            refreshFileData()
        }
    }

    fun deleteRegion(regionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val f = File(mbTilesRootFolder, regionName.plus(Const.MBTILES_EXT))
            val path = f.path
            val b = f.delete()
            Timber.i("$path delete: $b")
            if (b) {
                val currentSet = preferences.getStringSet(Const.PREF_MBTILES_FILEPATH_SET, emptySet())?.toMutableSet() ?: mutableSetOf()
                if (currentSet.contains(path)) {
                    currentSet.remove(path)
                    preferences.edit { putStringSet(Const.PREF_MBTILES_FILEPATH_SET, currentSet) }
                }
                refreshFileData()
            }
        }
    }

    fun toggleTileActivation(regionName: String, activate: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = "${mbTilesRootFolder.path}/$regionName${Const.MBTILES_EXT}"
            val currentSet = preferences.getStringSet(Const.PREF_MBTILES_FILEPATH_SET, emptySet())?.toMutableSet() ?: mutableSetOf()
            
            if (activate) {
                currentSet.add(path)
            } else {
                currentSet.remove(path)
                currentSet.remove(path.replace(Const.MBTILES_EXT, ""))
            }
            
            preferences.edit { putStringSet(Const.PREF_MBTILES_FILEPATH_SET, currentSet) }
            refreshFileData()
        }
    }

    fun handleDriveEntrySelection(name: String) {
        val splits = name.split(Const.UNDERLINE, ".", limit = 6)
        if (splits.size >= 5) {
            _uiState.update {
                it.copy(
                    mapType = splits[4],
                    x = splits[1].toInt(),
                    y = splits[2].toInt(),
                    listDriveEntries = false
                )
            }
            updateRegionNameAndBounds()
        }
    }
}
