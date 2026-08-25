package com.almica.ramani

import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.almica.ramani.charts.GraphDataPoints
import com.almica.ramani.charts.PlotResult
import com.almica.ramani.charts.createPlotDataResult
import com.almica.ramani.locations.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

data class LauncherUiState(
    val rasterTilesPrefSet: Set<String?>? = null,
    val geojsonFolderDescription: String? = null,
    val geojsonDescription: String? = null,
    val mvtName: String? = null,
    val ghFileName: String? = null,
    val resultRouteFolderName: String? = null,
    val resultLatLng: Pair<Double, Double> = Pair(0.0, 0.0),
    val logCount: Int = 0,
    val lastLocationDate: String? = null,
    val firstLocationDate: String? = null,
    val lastLocationCoords: String? = null,
    val isTrackingEnabled: Boolean = true,
    val showLocationStatistic: Boolean = false,
    val showRouteSavingScreen: Boolean = false,
    val plotResult: PlotResult = PlotResult(GraphDataPoints(arrayListOf(), arrayListOf(), arrayListOf(), arrayListOf()), 0f),
    val locationsLllh: List<LatLngH>? = null,
    val isLoading: Boolean = false
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(getApplication())
    private val locationRepository = LocationRepository.getInstance(getApplication(), Executors.newSingleThreadExecutor())
    private val assetRepository = AssetRepository(application)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
        viewModelScope.launch {
            assetRepository.copyAssetsIfNeeded()
        }
    }

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val useOpenGL = BuildConfig.USE_OPEN_GL // true: not vulkan (Nokia 1), false: vulkan
            val rasterTiles = prefs.getStringSet(Const.PREF_MBTILES_FILEPATH_SET, null)
            val mvtPath = prefs.getString(Const.PREF_MVT_FILEPATH, null)
            val ghPath = prefs.getString(Const.PREF_GH_FILEPATH, null)
            val routeFolderPath = prefs.getString(Const.PREF_ROUTEFOLDER_FILEPATH, Const.HOME)
            
            val lat = prefs.getDouble(Const.PREF_LATITUDE, 0.0)
            val lon = prefs.getDouble(Const.PREF_LONGITUDE, 0.0)
            Timber.i("prefs lat lon: $lat, $lon")
            val count = locationRepository.getLocationsCount()
            val firstLoc = locationRepository.getFirstLocation().firstOrNull()
            val lastLoc = locationRepository.getLastLocation().firstOrNull()
            Timber.i("locationRepository lat lon: ${lastLoc?.latitude} ${lastLoc?.longitude}")

            val firstDateStr = firstLoc?.let { dateFormat.format(it.time) }
            val lastDateStr = lastLoc?.let { dateFormat.format(it.time) }
            val lastCoordsStr = when {
                lastLoc != null -> { // sync prefs with locationRepository
                    prefs.edit { putDouble(Const.PREF_LATITUDE, lastLoc.latitude) }
                    prefs.edit { putDouble(Const.PREF_LONGITUDE, lastLoc.longitude) }
                    formatLocation(lastLoc.latitude, lastLoc.longitude)
                }
                lat != 0.0 && lon != 0.0 -> formatLocation(lat, lon)
                else -> null
            }
            Timber.i("lastCoordsStr: $lastCoordsStr")
            val geojsonFolder = prefs.getString(Const.PREF_GEOJSON_FILEPATH, null)
            val geojsonFile = prefs.getString(Const.RESULT_GEOJSON_FILENAME, null)
            //val isTracking = Helpers.isServiceRunning(getApplication(), LocationService::class.java)
            val isTracking = GpsRepository.getInstance().isTrackingEnabled.value
            Timber.i("isTracking: $isTracking")
            //Timber.i("lat: $lat lon: $lon")
            _uiState.update {
                it.copy(
                    rasterTilesPrefSet = rasterTiles,
                    mvtName = mvtPath?.let { path -> File(path).name.plus(if (useOpenGL) " (OpenGL)" else " (Vulkan)") },
                    ghFileName = ghPath?.let { path -> File(path).name },
                    resultRouteFolderName = routeFolderPath,
                    // resultLatLng is updated here by reading from SharedPreferences
                    // resultLatLng updated from Prefs: lat=$lat, lon=$lon
                    resultLatLng = Pair(lat, lon),
                    logCount = count,
                    firstLocationDate = firstDateStr,
                    lastLocationDate = lastDateStr,
                    lastLocationCoords = lastCoordsStr,
                    geojsonFolderDescription = geojsonFolder?.let { File(it).name },
                    geojsonDescription = geojsonFile,
                    isTrackingEnabled = isTracking
                )
            }
        }
    }

    fun updatePrefCoordinates(lat: Double, lon: Double) {
        Timber.i("updatePrefCoordinates: lat=$lat lon=$lon")
        // This is where the preference is written.
        // It uses toRawBits() to store the Double as a Long in SharedPreferences.
        prefs.edit {
            putDouble(Const.PREF_LATITUDE, lat)
            putDouble(Const.PREF_LONGITUDE, lon)
        }
        refreshAll()
    }

    fun onMonitorClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = getPlotData()
            _uiState.update { it.copy(plotResult = result, showLocationStatistic = true, isLoading = false) }
        }
    }

    fun onSaveAsRouteClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val locationsEntities = withContext(Dispatchers.IO) {
                locationRepository.getLocationsAscFromTime(0L)
            }
            val lllh = locationsEntities.map { LatLngH(it.latitude, it.longitude, it.altitude, time = it.time) }
            val plotData = getPlotData()
            
            _uiState.update { 
                it.copy(
                    locationsLllh = lllh, 
                    plotResult = plotData, 
                    showRouteSavingScreen = true, 
                    isLoading = false
                ) 
            }
        }
    }

    fun dismissLocationStatistic() {
        _uiState.update { it.copy(showLocationStatistic = false) }
    }

    fun dismissRouteSavingScreen() {
        _uiState.update { it.copy(showRouteSavingScreen = false) }
    }

    fun setIsLoading(value: Boolean) {
        _uiState.update { it.copy(isLoading = value) }
    }

    suspend fun getPlotData(): PlotResult = withContext(Dispatchers.IO) {
        createPlotDataResult(locationRepository, 0L)
    }

    private fun formatLocation(lat: Double, lon: Double): String {
        return String.format(Locale.US, getApplication<Application>().getString(R.string.location_coords_format), lat, lon)
    }
}
