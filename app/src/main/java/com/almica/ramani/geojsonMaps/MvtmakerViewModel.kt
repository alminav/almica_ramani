package com.almica.ramani.geojsonMaps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.almica.ramani.Const
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

data class MvtmakerUiState(
    val x: Int = 0,
    val y: Int = 0,
    val zoom: Int = 9,
    val regionName: String = "",
    val bounds: org.maplibre.android.geometry.LatLngBounds = GeoJsonUtils.tileToBounds(GeoJsonUtils.Companion.Tile(0, 0, 9)),
    val fileNames: List<String> = emptyList(),
    val prefMapname: String = "",
    val isMapLoaded: Boolean = false,
    val showGeoCoder: Boolean = false,
    val listItems: Boolean = false,
    val moboDeleteConfirmation: String? = null,
    val createMvtRegion: String? = null,
    val clipText: String? = null,
    val startLocation: LatLng? = null,
    val tileCenterLatLng: LatLng = LatLng(0.0, 0.0),
    val mvtRegionNames: List<String> = emptyList()
)

private fun MvtmakerUiState.updatedWithTile(newX: Int, newY: Int): MvtmakerUiState {
    if (x == newX && y == newY) return this
    val tile = GeoJsonUtils.Companion.Tile(newX, newY, zoom)
    return copy(
        x = newX,
        y = newY,
        regionName = "${Const.MVT_PREFIX}${newX}_${newY}_$zoom",
        bounds = GeoJsonUtils.tileToBounds(tile),
        tileCenterLatLng = GeoJsonUtils.tileToGmsBounds(tile).center
    )
}

class MvtmakerViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MvtmakerUiState())
    val uiState: StateFlow<MvtmakerUiState> = _uiState.asStateFlow()

    private val preferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val rootFolder = application.filesDir
    private val mvtRootFolder = File(rootFolder, Const.MVT_FOLDER)
    private val driveMap = DriveSharedLinks.Companion.MvtRegions().list

    init {
        val lat = savedStateHandle.get<Double>(Const.EXTRA_LATITUDE) ?: -1.0
        val lon = savedStateHandle.get<Double>(Const.EXTRA_LONGITUDE) ?: -1.0
        
        val zoom = _uiState.value.zoom
        val initialTile = if (lat >= 0 && lon >= 0) {
            GeoJsonUtils.pointToTile(lon, lat, zoom.toDouble())
        } else {
            GeoJsonUtils.Companion.Tile(0, 0, zoom)
        }

        _uiState.update {
            it.updatedWithTile(initialTile.x, initialTile.y).copy(
                startLocation = if (lat >= 0 && lon >= 0) LatLng(lat, lon) else null,
                mvtRegionNames = driveMap.keys.sorted(),
                isMapLoaded = false
            )
        }
        refreshFileData()
    }

    fun refreshFileData() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!mvtRootFolder.exists()) mvtRootFolder.mkdirs()

            val fileFilter = FileFilter { file: File? ->
                file?.name?.endsWith(Const.MBTILES_EXT) == true &&
                        !file.name.contains(Const.JOURNAL)
            }

            val files = mvtRootFolder.listFiles(fileFilter) ?: emptyArray()
            val fileNames = files.map { it.name.replace(Const.MBTILES_EXT, "") }

            val prefMapPath = preferences.getString(Const.PREF_MVT_FILEPATH, "")
            val prefMapname = prefMapPath?.let { File(it).name } ?: ""

            _uiState.update {
                it.copy(
                    fileNames = fileNames,
                    prefMapname = prefMapname
                )
            }
        }
    }

    fun updateCoordinates(x: Int, y: Int) {
        _uiState.update { currentState ->
            val newState = currentState.updatedWithTile(x, y)
            if (newState !== currentState) {
                // Coordinates changed: Map will move, so we wait for onMapLoaded
                newState.copy(isMapLoaded = false)
            } else {
                // Coordinates are the same: Map won't move/reload, so ensure loader is closed
                currentState.copy(isMapLoaded = true)
            }
        }
        Timber.i("updateCoordinates: $x $y")
    }

    fun setMapLoaded(loaded: Boolean) {
        _uiState.update { it.copy(isMapLoaded = loaded) }
        Timber.i("isMapLoaded: $loaded")
    }

    fun setShowGeoCoder(show: Boolean) {
        _uiState.update { it.copy(showGeoCoder = show) }
    }

    fun setListItems(show: Boolean) {
        _uiState.update { it.copy(listItems = show) }
    }

    fun setMoboDeleteConfirmation(message: String?) {
        _uiState.update { it.copy(moboDeleteConfirmation = message) }
    }

    fun setClipText(text: String?) {
        _uiState.update { it.copy(clipText = text) }
    }

    fun setCreateMvtRegion(regionName: String?) {
        _uiState.update { it.copy(createMvtRegion = regionName) }
    }

    fun activateMvt(regionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val f = File(mvtRootFolder, regionName.plus(Const.MBTILES_EXT))
            if (f.exists()) {
                preferences.edit { putString(Const.PREF_MVT_FILEPATH, f.path) }
                refreshFileData()
            }
        }
    }

    fun deactivateMvt() {
        viewModelScope.launch(Dispatchers.IO) {
            preferences.edit { remove(Const.PREF_MVT_FILEPATH) }
            refreshFileData()
        }
    }

    fun deleteMvt(regionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val f = File(mvtRootFolder, regionName.plus(Const.MBTILES_EXT))
            val deleted = f.delete()
            Timber.i("${f.path} delete: $deleted")
            
            val prefMapPath = preferences.getString(Const.PREF_MVT_FILEPATH, "")
            if (prefMapPath != null && File(prefMapPath).name.contains(regionName)) {
                preferences.edit { remove(Const.PREF_MVT_FILEPATH) }
            }
            refreshFileData()
        }
    }

    fun handleGeoCoderResult(latLng: LatLng) {
        val zoom = _uiState.value.zoom
        val tileMap = GeoJsonUtils.pointToTile(latLng.longitude, latLng.latitude, zoom.toDouble())
        updateCoordinates(tileMap.x, tileMap.y)
        setShowGeoCoder(false)
    }

    fun handleDriveEntrySelection(name: String) {
        try {
            val splits = name.split(Const.UNDERLINE, limit = 6)
            val x = splits[1].toInt()
            val y = splits[2].toInt()
            updateCoordinates(x, y)
            setListItems(false)
        } catch (e: Exception) {
            Timber.e(e, "$name doesn't fit the pattern mvt_x_y_z.mbtiles")
        }
    }
}
