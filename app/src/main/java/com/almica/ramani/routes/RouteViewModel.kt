package com.almica.ramani.routes

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.almica.ramani.Const
import com.almica.ramani.Helpers
import com.almica.ramani.LatLngH
import com.almica.ramani.googlemaps.MapUtils
import com.almica.ramani.utils.HgtReader
import com.almica.ramani.utils.RouteSmoothingUtil.simplifyToTargetCount
import com.almica.ramani.utils.kmlString2Lllh
import com.almica.ramani.utils.lllhToKmlString
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.ArrayList

const val ROUTE_MAX_ELEVATION_POINTS = 512

enum class RouteSortOrder {
    ByName,
    ByDistance
}

enum class SnackDbRoutesAction {
    Nothing,
    RemoveRegion,
    ShowSrtmFiles
}

data class SnackDbRoutesData(
    val title: String,
    val action: SnackDbRoutesAction,
    val actionText: String? = null,
    val actionData: String? = null
)

enum class RouteDatabaseMenu {
    Home,
    Map,
    Chart,
    ElevationRefreshFromSrtm,
    Gradient,
    RefreshPreview,
    DeleteEntry,
    ElevationGmsService
}

data class RouteUiState(
    val selectedRoute: RouteEntity? = null,
    val showRouteMoBo: RouteEntity? = null,
    val showRouteChart: RouteEntity? = null,
    val showRouteGradient: RouteEntity? = null,
    val showSrtmFiles: Boolean = false,
    val srtmFile: File? = null,
    val askForNameFilter: Boolean = false,
    val sortOrder: RouteSortOrder = RouteSortOrder.ByName,
    val filterString: String? = null,
    val filterRegion: String? = null,
    val snackData: SnackDbRoutesData? = null,
    val isLoading: Boolean = false
)

class RouteViewModelFactory(
    private val repository: RouteRepository,
    private val mapPos: LatLng?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RouteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RouteViewModel(repository, mapPos) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class RouteViewModel(
    private val repository: RouteRepository,
    private val mapPos: LatLng?
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteUiState())
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    val routes: StateFlow<List<RouteEntity>> = repository.getAll().asFlow()
        .combine(_uiState) { allRoutes, state ->
            filterAndSortRoutes(allRoutes, state)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun filterAndSortRoutes(routes: List<RouteEntity>, state: RouteUiState): List<RouteEntity> {
        var filtered = routes
        if (!state.filterString.isNullOrBlank()) {
            filtered = filtered.filter { it.name.contains(state.filterString, ignoreCase = true) }
        }
        if (!state.filterRegion.isNullOrBlank()) {
            filtered = filtered.filter { it.region == state.filterRegion }
        }

        return when (state.sortOrder) {
            RouteSortOrder.ByName -> filtered.sortedBy { it.region + it.name }
            RouteSortOrder.ByDistance -> {
                if (mapPos != null) {
                    filtered.sortedBy {
                        SphericalUtil.computeDistanceBetween(
                            LatLng(it.latitudeStart, it.longitudeStart),
                            mapPos
                        )
                    }
                } else {
                    filtered.sortedBy { it.region + it.name }
                }
            }
        }
    }

    fun onFilterNameChanged(name: String?) {
        _uiState.update { it.copy(filterString = name, filterRegion = null, askForNameFilter = false) }
    }

    fun onFilterRegionChanged(region: String?) {
        _uiState.update { it.copy(filterRegion = region, filterString = null, askForNameFilter = false) }
    }

    fun clearFilter() {
        _uiState.update { it.copy(filterString = null, filterRegion = null, askForNameFilter = false) }
    }

    fun toggleSortOrder() {
        _uiState.update {
            it.copy(
                sortOrder = if (it.sortOrder == RouteSortOrder.ByName)
                    RouteSortOrder.ByDistance else RouteSortOrder.ByName
            )
        }
    }

    fun selectRoute(route: RouteEntity?) {
        _uiState.update { it.copy(selectedRoute = route, showRouteMoBo = route, showRouteChart = null) }
    }

    fun deleteRoute(route: RouteEntity) {
        viewModelScope.launch {
            repository.removeRoute(route.id)
            _uiState.update { it.copy(showRouteMoBo = null) }
        }
    }

    fun deleteRegion(region: String) {
        _uiState.update {
            it.copy(
                snackData = SnackDbRoutesData(
                    "Remove region: $region?",
                    SnackDbRoutesAction.RemoveRegion,
                    "OK",
                    region
                )
            )
        }
    }

    fun confirmDeleteRegion(region: String) {
        viewModelScope.launch {
            repository.removeRoutes(region) {
                _uiState.update { it.copy(snackData = null) }
            }
        }
    }

    fun showSrtmFiles(show: Boolean) {
        _uiState.update { it.copy(showSrtmFiles = show) }
    }

    fun setSrtmFile(file: File?) {
        _uiState.update { it.copy(srtmFile = file) }
    }

    fun showAskForFilter(show: Boolean) {
        _uiState.update { it.copy(askForNameFilter = show) }
    }

    fun showChart(route: RouteEntity?) {
        _uiState.update { it.copy(showRouteChart = route, showRouteMoBo = null) }
    }

    fun showGradient(route: RouteEntity?) {
        _uiState.update { it.copy(showRouteGradient = route, showRouteMoBo = null) }
    }

    fun dismissSnack() {
        _uiState.update { it.copy(snackData = null) }
    }

    fun exportDatabase(context: Context) {
        _uiState.update {
            it.copy(
                snackData = SnackDbRoutesData(
                    "Export started",
                    SnackDbRoutesAction.Nothing
                )
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
            val allRoutes = repository.getAllSimple()
            var filesCount = 0
            allRoutes.forEach { route ->
                val routeDir = File(rootRouteFolder, route.region)
                routeDir.mkdir()
                val routeFile = File(routeDir, route.name + Const.KML_EXT)
                val lllhRaw = route.kmlString.kmlString2Lllh()
                if (lllhRaw.isNotEmpty()) {
                    Helpers.writeLllh2KmlFile(lllhRaw, routeFile.path)
                    filesCount++
                }
            }
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(snackData = null) }
                Timber.i("Exported $filesCount routes")
            }
        }
    }

    fun refreshElevationFromSrtm(context: Context, route: RouteEntity) {
        val srtmFile = uiState.value.srtmFile
        if (srtmFile == null) {
            _uiState.update {
                it.copy(
                    snackData = SnackDbRoutesData(
                        "Select HGT file",
                        SnackDbRoutesAction.ShowSrtmFiles,
                        "OK"
                    ),
                    showRouteMoBo = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val lllh = route.kmlString.kmlString2Lllh()
            if (srtmFile.exists()) {
                val hgtReader = HgtReader(context, srtmFile)
                val refreshedLllh = withContext(Dispatchers.IO) {
                    hgtReader.refreshRouteElevationFromSrtm(lllh).lllh
                } as ArrayList<LatLngH>

                val kmlStringUpdated = refreshedLllh.lllhToKmlString(route.name)
                withContext(Dispatchers.IO) {
                    repository.updateRoute(kmlStringUpdated, route.id)
                }

                _uiState.update {
                    it.copy(
                        snackData = SnackDbRoutesData(
                            "Route Database Update: ${route.name.removeSuffix(".gpx").removeSuffix(".jpg").removeSuffix(".kml")}",
                            SnackDbRoutesAction.Nothing
                        ),
                        showRouteMoBo = null
                    )
                }
            } else {
                 _uiState.update {
                    it.copy(
                        snackData = SnackDbRoutesData(
                            "File not found: ${srtmFile.path}",
                            SnackDbRoutesAction.Nothing
                        ),
                        showRouteMoBo = null
                    )
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refreshElevationFromGms(context: Context, route: RouteEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val lllh = route.kmlString.kmlString2Lllh()
            val gmsLatLng = if (lllh.size > ROUTE_MAX_ELEVATION_POINTS) {
                lllh.simplifyToTargetCount(ROUTE_MAX_ELEVATION_POINTS)
            } else {
                lllh
            }.map { it.latLngGms }

            val encodedPolyline = PolyUtil.encode(gmsLatLng)
            val refreshedLllh = MapUtils.gmsElevationService(context, "enc:${encodedPolyline}")

            if (refreshedLllh.isNotEmpty()) {
                val kmlStringUpdated = ArrayList(refreshedLllh).lllhToKmlString(route.name)
                withContext(Dispatchers.IO) {
                    repository.updateRoute(kmlStringUpdated, route.id)
                }
                _uiState.update {
                    it.copy(
                        snackData = SnackDbRoutesData(
                            "Route Database Update: ${route.name.removeSuffix(".gpx").removeSuffix(".jpg").removeSuffix(".kml")}",
                            SnackDbRoutesAction.Nothing
                        ),
                        showRouteMoBo = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        snackData = SnackDbRoutesData(
                            "GMS Elevation Service FAILED",
                            SnackDbRoutesAction.Nothing
                        ),
                        showRouteMoBo = null
                    )
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun replaceRoute(name: String, region: String, kmlString: String, bitmap: Bitmap, track: Track) {
        val routeEntity = track.distance?.let {
            RouteEntity(
                name,
                region,
                track.startLatLng!!.latitude,
                track.startLatLng!!.longitude,
                track.center.latitude,
                track.center.longitude,
                track.stopLatLng!!.latitude,
                track.stopLatLng!!.longitude,
                distance = it,
                kmlString,
                bitmap
            )
        }
        if (routeEntity != null) {
            repository.replaceRoute(routeEntity) {
                _uiState.update { it.copy(showRouteMoBo = null) }
            }
        }
    }
}
