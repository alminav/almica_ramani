package com.almica.ramani

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.almica.ramani.pois.PoiEntity
import com.almica.ramani.pois.PoiRepository
import com.almica.ramani_lib.CameraPosition
import com.almica.ramani_lib.SymbolVector
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.modes.CameraMode
import timber.log.Timber
import kotlin.collections.forEach
private const val logtag = "InitSymbols"

/**
 * 15mai2026 deactivated does not work
 * instead show pois with circles
 */
@Composable
fun InitSymbols(
    context: Context,
    dbPois: List<PoiEntity>,
    poiRepository: PoiRepository,
    cameraPosition: MutableState<CameraPosition>,
    cameraMode: MutableIntState,
    snack: (MainSnackbarData) -> Unit,
    onClick: (PoiEntity) -> Unit,
    onLongClick: (PoiEntity) -> Unit,
    onDragFinished: (Boolean) -> Unit,
    finished: (Int) -> Unit
) {
    Timber.i("dbPois: ${dbPois.size}")
    val catMap by remember { mutableStateOf(Helpers.getPoiDrawableMap(context)) }
    dbPois.forEach { poiEntity ->
        SymbolVector(
            context,
            center = LatLng(poiEntity.latitude, poiEntity.longitude),
            imageId = catMap[poiEntity.category]?.first,
            text = if (cameraPosition.value.zoom != null && cameraPosition.value.zoom!! > 11.0) poiEntity.name else null,
            //imageOffset = arrayOf(100f, 100f),
            //textOffset = arrayOf(0f, -1f),
            //textJustify = org.maplibre.android.style.layers.Property.TEXT_ANCHOR_TOP,
            isDraggable = true, //cameraPosition.value.zoom != null && cameraPosition.value.zoom!! > 11.0,
            onDragFinished = { pos ->
                Timber.i("zoom: ${cameraPosition.value.zoom}")
                if (cameraPosition.value.zoom != null && cameraPosition.value.zoom!! > 11.0) {
                    poiEntity.latitude = pos.latitude
                    poiEntity.longitude = pos.longitude
                    poiRepository.updatePoi(
                        pos.latitude,
                        pos.longitude,
                        poiEntity.id
                    ) {
                        //Timber.i("dragged: ${poiEntity.name}")
                        onDragFinished(true)
                    }
                } else
                    onDragFinished(false)
            },
            onSymbolDragged = { center ->
                //Timber.i("dragged: ${poiEntity.name}")
            },
            onClick = {
                if (cameraMode.intValue == CameraMode.NONE) {
                    onClick(poiEntity)
                    snack(
                        MainSnackbarData(
                            "${poiEntity.name} - ${poiEntity.category}",
                            context.getString(R.string.set_stop_marker),
                            MainSnackbarSelection.SetStop, null
                        )
                    )
                } else
                    snack(
                        MainSnackbarData(
                            "${poiEntity.name} - ${poiEntity.category}",
                            null,
                            null, null
                        )
                    )
            },
            onLongClick = {
                onLongClick(poiEntity)
            }
        )
    }
    finished(dbPois.size)
}