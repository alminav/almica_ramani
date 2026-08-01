package com.almica.ramani

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.almica.ramani.Const.MapType
import com.almica.ramani.utils.deleteOfflineRegion
import com.almica.ramani.utils.downloadBounds
import com.almica.ramani.utils.getOfflineRegionsMap
import com.almica.ramani.utils.getRasterRegionNames
import com.almica.ramani.utils.invalidateOfflineRegion
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.ibrahimsn.library.LiveSharedPreferences
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import timber.log.Timber

class OfflineMapCreatorViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val liveSharedPreferences = LiveSharedPreferences(preferences)

    var regionName by mutableStateOf("")
    var tileLimitExceeded by mutableLongStateOf(0L)
    var mapType by mutableStateOf(preferences.getString(context.getString(R.string.pref_tilemaker_maptype), Const.OUTDOOR) ?: Const.OUTDOOR)
    var tilemakerUrl by mutableStateOf(preferences.getString(context.getString(R.string.pref_tilemaker_url), Const.URL_PHONEMAPS))
    var tilemakerMaxZoom by mutableStateOf(preferences.getString(context.getString(R.string.pref_tilemaker_maxzoom), "14")?.toInt() ?: 14)
    var tilemakerMinZoom by mutableStateOf(preferences.getString(context.getString(R.string.pref_tilemaker_minzoom), "8")?.toInt() ?: 8)
    var tileLimit by mutableStateOf<Long?>(6000L)
    
    var statusText by mutableStateOf("")
    var progressAnimation by mutableFloatStateOf(0f)
    var showDropDownRasterMaptype by mutableStateOf(false)
    var showOfflineRegions by mutableStateOf(false)
    var showRasterRegions by mutableStateOf(false)
    
    var offlineRegionsMap by mutableStateOf<Map<String, OfflineRegion>?>(null)
    var rasterRegionNames by mutableStateOf<List<String>>(emptyList())
    
    var isDownloadActive by mutableStateOf(false)
    var currentDownloadMapType by mutableStateOf<MapType?>(null)

    private var downloadJob: Job? = null
    var tilemakerJob: Job? = null

    init {
        loadTileLimit()
        refreshRasterRegionNames()
        loadOfflineRegions()
    }

    private fun loadTileLimit() {
        if (preferences.contains(context.getString(R.string.pref_OfflineMapboxTileCountLimit))) {
            val limitString = preferences.getString(context.getString(R.string.pref_OfflineMapboxTileCountLimit), "6000")
            tileLimit = limitString?.toLong()
        }
    }

    fun refreshRasterRegionNames() {
        rasterRegionNames = getRasterRegionNames(context)
    }

    fun loadOfflineRegions() {
        getOfflineRegionsMap(context) {
            offlineRegionsMap = it
        }
    }

    fun onMapTypeChanged(newMapType: String) {
        mapType = newMapType
        tilemakerUrl = when (mapType) {
            Const.PHONEMAPS -> Const.URL_PHONEMAPS
            Const.OPENTOPO -> Const.URL_OPENTOPO
            Const.OUTDOOR -> Const.URL_OUTDOOR
            Const.THUNDERFOREST -> Const.URL_THUNDERFOREST
            else -> Const.URL_PHONEMAPS
        }
    }

    fun startVectorDownload(regionDefinition: OfflineTilePyramidRegionDefinition, onDownloadActive: (Boolean, MapType) -> Unit) {
        isDownloadActive = true
        currentDownloadMapType = MapType.Vector
        onDownloadActive(true, MapType.Vector)
        
        downloadJob = viewModelScope.launch {
            downloadBounds(
                context,
                tileLimit,
                regionName,
                regionDefinition,
                { completed, completedCount, requiredCount ->
                    statusText = "$completed completed: $completedCount required: $requiredCount"
                    progressAnimation = completedCount.toFloat() / requiredCount.toFloat()
                    if (completed) {
                        progressAnimation = 1f
                        isDownloadActive = false
                        onDownloadActive(false, MapType.Vector)
                    }
                },
                { limit ->
                    tileLimitExceeded = limit
                    progressAnimation = 0f
                    isDownloadActive = false
                    onDownloadActive(false, MapType.Vector)
                }
            )
        }
    }

    fun startRasterDownload(
        bounds: org.maplibre.android.geometry.LatLngBounds,
        onDownloadActive: (Boolean, MapType) -> Unit,
        onReady: () -> Unit
    ) {
        val baseUrl = tilemakerUrl ?: return
        isDownloadActive = true
        currentDownloadMapType = MapType.Raster
        onDownloadActive(true, MapType.Raster)

        com.almica.ramani.tilemaker.MbtilesCreator(context).createMbtiles(
            regionName,
            mapType = mapType,
            baseUrl = baseUrl,
            area = arrayOf(
                com.google.android.gms.maps.model.LatLng(bounds.northWest.latitude, bounds.northWest.longitude),
                com.google.android.gms.maps.model.LatLng(bounds.southWest.latitude, bounds.southWest.longitude),
                com.google.android.gms.maps.model.LatLng(bounds.southEast.latitude, bounds.southEast.longitude),
                com.google.android.gms.maps.model.LatLng(bounds.northEast.latitude, bounds.northEast.longitude)
            ),
            zooms = intArrayOf(
                4.coerceAtLeast(tilemakerMinZoom),
                15.coerceAtMost(tilemakerMaxZoom)
            ),
            progress = { job, p ->
                tilemakerJob = job
                progressAnimation = 0.01f * p
                statusText = "$p %"
            },
            ready = {
                progressAnimation = 0f
                isDownloadActive = false
                onDownloadActive(false, MapType.Raster)
                refreshRasterRegionNames()
                onReady()
            },
            cancel = {
                progressAnimation = 0f
                isDownloadActive = false
                onDownloadActive(false, MapType.Raster)
                refreshRasterRegionNames()
            }
        )
    }

    fun cancelDownloads(onDownloadActive: (Boolean, MapType) -> Unit) {
        downloadJob?.cancel()
        tilemakerJob?.cancel()
        progressAnimation = 0f
        statusText = ""
        isDownloadActive = false
        val type = currentDownloadMapType ?: MapType.Vector
        onDownloadActive(false, type)
        currentDownloadMapType = null
    }

    fun deleteOfflineRegion(name: String, callback: (Boolean) -> Unit) {
        val region = offlineRegionsMap?.get(name) ?: return
        invalidateOfflineRegion(region) { success ->
            if (success) {
                deleteOfflineRegion(region) { deleted ->
                    if (deleted) {
                        loadOfflineRegions()
                    }
                    callback(deleted)
                }
            } else {
                callback(false)
            }
        }
    }
}
