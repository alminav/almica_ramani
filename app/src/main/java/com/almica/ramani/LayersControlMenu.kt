package com.almica.ramani

import android.content.Context
import androidx.compose.material.Switch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.almica.ramani.Const.Companion.COUNTRIES_LAYER_TAG
import com.almica.ramani.Const.Companion.PLANET_LAYER_TAG
import com.almica.ramani.FeatureProperties.Companion.LINES_TAG
import com.almica.ramani.Helpers.Companion.getPrefRasterMapType
import com.almica.ramani.charts.theme.Black
import com.almica.ramani.utils.checkLayerVisibility
import com.almica.ramani.Const.Companion.LATLNG_GRID_LAYER
import org.maplibre.android.maps.MapLibreMap

@Composable
fun LayersControlMenu(
    context: Context,
    map: MapLibreMap?,
    maptypeKey: Int,
    changeGridState: (Boolean, String) -> Unit,
    changePlanetState: (Boolean) -> Unit,
    changeRoutesLayerState: (Boolean) -> Unit,
    finished: (LayersControlAction) -> Unit
) {
    var geojsonState by remember { mutableStateOf(checkLayerVisibility(map,
        context.getString(R.string.geojson_maps_grid))) }
    var rasterState by remember { mutableStateOf(checkLayerVisibility(map,
        context.getString(R.string.raster_maps_grid))) }
    var mvtGridState by remember { mutableStateOf(checkLayerVisibility(map,
        context.getString(R.string.mvt_grid))) }
    var latlngState by remember { mutableStateOf(checkLayerVisibility(map,
        LATLNG_GRID_LAYER)) }
    var planetState by remember { mutableStateOf(checkLayerVisibility(map,
        PLANET_LAYER_TAG).or(checkLayerVisibility(map, COUNTRIES_LAYER_TAG))) }
    val routesLayerId = context.getString(R.string.routes) + LINES_TAG
    var routesLayerState by remember { mutableStateOf(checkLayerVisibility(map, routesLayerId)) }

    val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    val prefMapType = getPrefRasterMapType(context)
    DropdownMenu(
        expanded = true,
        onDismissRequest = { finished(LayersControlAction.Nothing) }
    ) {
        if (maptypeKey != MaptypeKey.Raster.ordinal)
            DropdownMenuItem(
                text = { Text(text = context.getString(R.string.geojson_maps_grid_), color = Black) },
                leadingIcon = { Icon(Icons.Outlined.GridView, null) },
                trailingIcon = {
                    Switch(
                        checked = geojsonState,
                        onCheckedChange = { isChecked ->
                            geojsonState = isChecked
                            changeGridState(isChecked, context.getString(R.string.geojson_maps_grid))
                        }
                    )
                },
                onClick = {},
            )
        DropdownMenuItem(
            text = { Text(text = "${context.getString(R.string.raster_maps_grid_)} (${prefMapType})", color = Black) },
            leadingIcon = { Icon(Icons.Outlined.GridView, null) },
            trailingIcon = {
                Switch(
                    checked = rasterState,
                    onCheckedChange = { isChecked ->
                        rasterState = isChecked
                        changeGridState(isChecked, context.getString(R.string.raster_maps_grid))
                    }
                )
            },
            onClick = {},
        )
        if (maptypeKey != MaptypeKey.Raster.ordinal)
            DropdownMenuItem(
                text = { Text(text = context.getString(R.string.mvt_grid_title), color = Black) },
                leadingIcon = { Icon(Icons.Outlined.GridView, null) },
                trailingIcon = {
                    Switch(
                        checked = mvtGridState,
                        onCheckedChange = { isChecked ->
                            mvtGridState = isChecked
                            changeGridState(isChecked, "${context.getString(R.string.mvt_grid)}${LINES_TAG}")
                        }
                    )
                },
                onClick = {},
            )
        if (maptypeKey != MaptypeKey.None.ordinal)
            DropdownMenuItem(
                text = { Text(text = context.getString(R.string.planet_layer), color = Black) },
                leadingIcon = { Icon(Icons.Outlined.Language, null) },
                trailingIcon = {
                    Switch(
                        checked = planetState,
                        onCheckedChange = { isChecked ->
                            planetState = isChecked
                            changePlanetState(isChecked)
                            preferences.edit { putBoolean(Const.PREF_PLANET_VISIBILITY, planetState) }
                        }
                    )
                },
                onClick = {},
            )

        DropdownMenuItem(
            text = { Text(text = context.getString(R.string.latlng_grid_), color = Black) },
            leadingIcon = { Icon(Icons.Outlined.Language, null) },
            trailingIcon = {
                Switch(
                    checked = latlngState,
                    onCheckedChange = { isChecked ->
                        latlngState = isChecked
                        changeGridState(isChecked, LATLNG_GRID_LAYER)
                    }
                )
            },
            onClick = {},
        )

        DropdownMenuItem(
            text = { Text(text = context.getString(R.string.routes_layer), color = Black) },
            leadingIcon = { Icon(Icons.Outlined.Route, null) },
            trailingIcon = {
                Switch(
                    checked = routesLayerState,
                    onCheckedChange = { isChecked ->
                        routesLayerState = isChecked
                        changeRoutesLayerState(isChecked)
                    }
                )
            },
            onClick = {},
        )

/*
        DropdownMenuItem(
            { Text(text = context.getString(R.string.routes_layer), color = Black) },
            onClick = { finished(LayersControlAction.ToggleRoutesGeojson) },
            leadingIcon = { Icon(Icons.Outlined.Route, null) }
        )
*/
    }
}
enum class LayersControlAction {
    ToggleRoutesGeojson,
    MvtBbbike09,
    PmtilesBbbike10,
    PmtilesBbbike11,
    GeojsonBbbike12,
    GeojsonBbbike13,
    Nothing
}