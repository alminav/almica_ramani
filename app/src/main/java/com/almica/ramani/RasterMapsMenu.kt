package com.almica.ramani

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.almica.ramani.Helpers.Companion.getPrefRasterMapType
import com.almica.ramani.charts.theme.Black
import com.almica.ramani.utils.GeoJsonUtils
import org.maplibre.android.geometry.LatLng

enum class RasterMapsMenuAction {
    ToggleGrid,
    RasterMapType,
    Nothing
}
@Composable
fun RasterMapsMenu(
    context: Context,
    mapManagerPosition: LatLng?,
    finished: (RasterMapsMenuAction, String?, String?) -> Unit
) {
    val prefMapType = getPrefRasterMapType(context)
    var tileName by remember { mutableStateOf<String?>(null) }
    mapManagerPosition?.let {
        val tile10 = GeoJsonUtils.pointToTile(it.longitude, mapManagerPosition.latitude, 10.0)
        tileName = "tile_${tile10.x}_${tile10.y}_${tile10.z}_${prefMapType}"
    }

    DropdownMenu(
        expanded = true,
        onDismissRequest = { finished(RasterMapsMenuAction.Nothing, tileName, prefMapType) }
    ) {
        tileName?.let {
            DropdownMenuItem(
                { Text(text = it, color = Black) },
                onClick = { finished(RasterMapsMenuAction.Nothing, tileName, prefMapType) }
            )
        }
        DropdownMenuItem(
            { Text(text = context.getString(R.string.raster_maps_grid_toggle), color = Black) },
            onClick = { finished(RasterMapsMenuAction.ToggleGrid, tileName, prefMapType) },
            leadingIcon = { Icon(Icons.Outlined.GridView, null) }
        )
        DropdownMenuItem(
            { Text(text = context.getString(R.string.raster_map_type_setting), color = Black) },
            onClick = { finished(RasterMapsMenuAction.RasterMapType, tileName, prefMapType) },
            leadingIcon = { Icon(Icons.Outlined.Settings, null) }
        )
    }
}
