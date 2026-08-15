package com.almica.ramani.pois

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almica.ramani.googlemaps.MapUtils.gmsElevationService
import com.almica.ramani.utils.formatAlti
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors

enum class PoiItemAction {
    Map,
    Delete,
    ElevationRefresh,
    Stop
}

enum class SnackPoiAction { Nothing, DeleteAll }

data class SnackPoiData(
    val title: String,
    val action: SnackPoiAction = SnackPoiAction.Nothing,
    val actionText: String? = null,
    val actionData: String? = null
)

enum class PoiSortOrder {
    ByName,
    ByDistance,
    ByCategory
}

class PoiViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PoiRepository.getInstance(application, Executors.newSingleThreadExecutor())

    private val _sortOrder = MutableStateFlow(PoiSortOrder.ByName)
    val sortOrder = _sortOrder.asStateFlow()

    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _mapPosition = MutableStateFlow<LatLng?>(null)
    val mapPosition = _mapPosition.asStateFlow()

    private val _snackPoiData = MutableStateFlow<SnackPoiData?>(null)
    val snackPoiData = _snackPoiData.asStateFlow()

    val categories = repository.getAllFlow().map { list ->
        list.asSequence().map { it.category }.distinct().toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val poiEntities = combine(
        repository.getAllFlow(),
        _sortOrder,
        _searchQuery,
        _selectedCategory,
        _mapPosition
    ) { allPois, sortOrder, query, category, mapPos ->
        var filtered = allPois
        if (category != null) {
            filtered = filtered.filter { it.category == category }
        }
        if (!query.isNullOrBlank()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }

        when (sortOrder) {
            PoiSortOrder.ByName -> filtered.sortedBy { it.name }
            PoiSortOrder.ByCategory -> filtered.sortedBy { it.category + it.name }
            PoiSortOrder.ByDistance -> {
                if (mapPos != null) {
                    filtered.sortedBy {
                        SphericalUtil.computeDistanceBetween(
                            LatLng(it.latitude, it.longitude),
                            mapPos
                        )
                    }
                } else {
                    filtered.sortedBy { it.name }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSortOrder(order: PoiSortOrder) {
        _sortOrder.value = order
    }

    fun setSearchQuery(query: String?) {
        _searchQuery.value = query
        _selectedCategory.value = null
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
        _searchQuery.value = null
    }

    fun setMapPosition(pos: LatLng?) {
        _mapPosition.value = pos
    }

    fun deletePoi(id: UUID) {
        viewModelScope.launch {
            repository.removePoiSuspend(id)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            repository.removeAllSuspend()
        }
    }

    fun refreshElevation(poi: PoiEntity, messageFormat: String) {
        viewModelScope.launch {
            val lllh0 = gmsElevationService(getApplication(), "${poi.latitude},${poi.longitude}")
            val h = lllh0.firstOrNull()?.altitude ?: 0.0
            Timber.i("refreshElevation h: $h")
            repository.updatePoiAltitudeSuspend(h, poi.id)
            _snackPoiData.value = SnackPoiData(
                title = String.format(messageFormat, poi.name) + " " + h.formatAlti(true)
            )
        }
    }

    fun showDeleteAllSnack(title: String, okText: String) {
        _snackPoiData.value = SnackPoiData(
            title = title,
            action = SnackPoiAction.DeleteAll,
            actionText = okText
        )
    }

    fun clearSnack() {
        _snackPoiData.value = null
    }
}
