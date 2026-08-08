package com.almica.ramani

import android.app.Application
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.almica.ramani.locations.LocationRepository
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.RouteSmoothingUtil.simplifyToTargetCount
import com.almica.ramani.utils.formatDistM
import com.almica.ramani.utils.getCenter
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.snapshotter.MapSnapshot
import timber.log.Timber
import java.io.File
import java.util.concurrent.Executors

data class LocationsSnapshotUiState(
    val title: String? = null,
    val lllhLocations: List<LatLngH>? = null,
    val styleUriToUse: String? = null,
    val snapshot: MapSnapshot? = null,
    val snapshotBitmap: ImageBitmap? = null,
    val logCount: Int = 0,
    val showGradient: Boolean = false,
    val startTime: Long = 0L,
    val isLoading: Boolean = false
)

class LocationsSnapshotViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LocationsSnapshotUiState())
    val uiState: StateFlow<LocationsSnapshotUiState> = _uiState.asStateFlow()

    private val locationRepository = LocationRepository.getInstance(application, Executors.newSingleThreadExecutor())
    private val mvtFolder = File(application.filesDir, Const.MVT_FOLDER)

    init {
        Timber.i("LocationsSnapshotViewModel init")
    }

    fun loadLocations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val entities = withContext(Dispatchers.IO) {
                locationRepository.getLocationsAscFromTime(0L)
            }
            val count = withContext(Dispatchers.IO) {
                locationRepository.getLocationsCount()
            }

            val lllh = withContext(Dispatchers.IO) {
                entities.map { entity ->
                    LatLngH(
                        latitude = entity.latitude,
                        longitude = entity.longitude,
                        altitude = entity.altitude,
                        time = entity.recordedAt.time
                    )
                }.simplifyToTargetCount(30)
            }

            _uiState.update { it.copy(lllhLocations = lllh, logCount = count, isLoading = false) }
            
            if (lllh.isNotEmpty()) {
                resolveStyle(lllh) // takeSnapshot is called in resolveStyle
                Timber.i("takeSnapshot lllh: ${lllh.size}")
            } else {
                Timber.i("lllh is empty")
                _uiState.update { it.copy(snapshot = null, snapshotBitmap = null, logCount = 0, isLoading = false) }
            }
        }
    }

    private suspend fun resolveStyle(locations: List<LatLngH>) {
        withContext(Dispatchers.IO) {
            val locCenter = (locations as ArrayList<LatLngH>).getCenter()
            val mvtTileMatch = GeoJsonUtils.pointToTile(locCenter.longitude, locCenter.latitude, 9.0)
            val mvtMatchingMap = "${Const.MVT_PREFIX}${mvtTileMatch.x}_${mvtTileMatch.y}_${mvtTileMatch.z}"
            val mvtMatchingFile = File(mvtFolder, mvtMatchingMap.plus(Const.MBTILES_EXT))
            
            val preferences = PreferenceManager.getDefaultSharedPreferences(getApplication())
            val mvtCurrentPath = preferences.getString(Const.PREF_MVT_FILEPATH, null)
            
            val styleUri = when {
                mvtMatchingFile.exists() && mvtMatchingFile.path != mvtCurrentPath -> {
                    preferences.edit { putString(Const.PREF_MVT_FILEPATH, mvtMatchingFile.path) }
                    Helpers.createMvtOfflineStyle(getApplication(), File(mvtMatchingFile.path))
                }
                !mvtCurrentPath.isNullOrEmpty() -> {
                    Helpers.createMvtOfflineStyle(getApplication(), File(mvtCurrentPath))
                }
                else -> {
                    val emptyStyleFile = File(mvtFolder, Const.EMPTY_STYLE_FILENAME)
                    Uri.fromFile(emptyStyleFile).toString()
                }
            }
            
            _uiState.update { it.copy(styleUriToUse = styleUri) }
            takeSnapshot()
        }
    }

    fun setStartTime(time: Long) {
        _uiState.update { it.copy(startTime = time) }
        updateTitle()
    }

    fun setShowGradient(show: Boolean) {
        _uiState.update { it.copy(showGradient = show, snapshot = null) } // Clear snapshot to re-trigger
        takeSnapshot()
    }

    private fun updateTitle() {
        val state = _uiState.value
        val locations = state.lllhLocations ?: return
        val filtered = locations.filter { it.time >= state.startTime }
        
        if (filtered.isNotEmpty()) {
            var distM = 0.0
            if (filtered.size > 1) {
                for (i in 0 until filtered.size - 1) {
                    distM += SphericalUtil.computeDistanceBetween(filtered[i].latLngGms, filtered[i + 1].latLngGms)
                }
            }
            val seconds = 0.001 * (locations.last().time - filtered.first().time)
            val timeText = Helpers.convertSecondsToHHMMSS(seconds.toInt())
            val distText = distM.formatDistM(true)
            val title = "$distText $timeText ${Const.UC_ELE_ARROW}${filtered.first().altitude.formatDistM(true)}"
            _uiState.update { it.copy(title = title) }
        }
    }

    fun takeSnapshot() {
        Timber.i("takeSnapshot")
        val state = _uiState.value
        val lllh = state.lllhLocations ?: return
        val styleUri = state.styleUriToUse ?: return
        
        if (lllh.isEmpty()) return

        Helpers.takeLocationsSnapshot(
            getApplication(), lllh as ArrayList<LatLngH>,
            styleUri,
            512, 0.2
        ) { snap, _ ->
            snap?.let { s ->
                if (state.showGradient) {
                    Helpers.addLineToSnapshotWithGradient(s, lllh)
                } else {
                    Helpers.addLineToSnapshot(s, lllh)
                }
                _uiState.update { it.copy(snapshot = s, snapshotBitmap = s.bitmap.asImageBitmap()) }
                updateTitle()
            }
        }
    }

    fun deleteLocationsBefore(time: Long, onRefresh: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            locationRepository.removeLocationsToTime(time)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(snapshot = null, startTime = 0L) }
                loadLocations()
                onRefresh()
            }
        }
    }

    fun deleteLocationsAfter(time: Long, onRefresh: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            locationRepository.removeLocationsFromTime(time)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(snapshot = null, startTime = 0L) }
                loadLocations()
                onRefresh()
            }
        }
    }
}
