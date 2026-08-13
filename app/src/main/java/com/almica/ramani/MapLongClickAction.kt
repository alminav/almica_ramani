package com.almica.ramani

import androidx.compose.material3.Switch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.tooling.preview.Preview
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.charts.theme.Black
import com.almica.ramani.pois.PoiEntity
import com.almica.ramani.pois.PoiRepository
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.Const.Companion.LATLNG_GRID_LAYER
import com.almica.ramani.utils.checkLayerVisibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.maps.MapLibreMap
import timber.log.Timber
import java.util.concurrent.Executors

enum class MapLongClickAction {
    ChangeDimmerState,
    SatStatus,
    Weather,
    PdfViewer,
    ClearGpsCircles,
    RouteFolders,
    MvtBbbike09,
    Nothing
}
@Composable
fun MapLongClickMenu(
    map: MapLibreMap?,
    changeGridState: (Boolean, String) -> Unit,
    finished: (MapLongClickAction) -> Unit,
    navigateToHome: (PoiEntity) -> Unit) {
    val latlngState = remember { checkLayerVisibility(map, LATLNG_GRID_LAYER) }
    MapLongClickMenuContent(
        latlngStateInitial = latlngState,
        changeGridState = changeGridState,
        finished = finished,
        navigateToHome = {home -> navigateToHome(home)}
    )
}

@Composable
fun MapLongClickMenuContent(
    latlngStateInitial: Boolean,
    changeGridState: (Boolean, String) -> Unit,
    finished: (MapLongClickAction) -> Unit,
    navigateToHome: (PoiEntity) -> Unit,
) {
    val context = LocalContext.current
//    val prefs = getDefaultSharedPreferences(context)
//    val prefRouteFolder = prefs.getString(Const.PREF_ROUTEFOLDER_FILEPATH, null)

    // AI 09mai2026
    val poiHome by produceState<PoiEntity?>(initialValue = null, context) {
        withContext(Dispatchers.IO) {
            val poiRepository = PoiRepository.getInstance(context, Executors.newSingleThreadExecutor())
            value = poiRepository.getPoiSimpleByName(Const.HOME)
            Timber.i("poiHome: $value")
        }
    }

    val resources = LocalResources.current
    var latlngState by remember { mutableStateOf(latlngStateInitial) }
    DropdownMenu(
        expanded = true,
        onDismissRequest = { finished(MapLongClickAction.Nothing) }
    ) {
        DropdownMenuItem(
            { Text(text = resources.getString(R.string.route_folders), color = Black) },
            onClick = { finished(MapLongClickAction.RouteFolders) },
            leadingIcon = {Icon (Icons.Outlined.FolderOpen, null)}
        )
        DropdownMenuItem(
            { Text(text = resources.getString(R.string.change_dimmer_state), color = Black) },
            onClick = { finished(MapLongClickAction.ChangeDimmerState) },
            leadingIcon = {Icon (Icons.Outlined.Brightness6, null)}
        )
        DropdownMenuItem(
            { Text(text = resources.getString(R.string.sat_status), color = Black) },
            onClick = { finished(MapLongClickAction.SatStatus) },
            leadingIcon = {Icon (Icons.Outlined.GpsFixed, null)}
        )

        DropdownMenuItem(
            { Text(text = resources.getString(R.string.weather), color = Black) },
            onClick = { finished(MapLongClickAction.Weather) },
            leadingIcon = {Icon (Icons.Outlined.Cloud, null)}
        )

        DropdownMenuItem(
            { Text(text = resources.getString(R.string.clear_gps_circles), color = Black) },
            onClick = { finished(MapLongClickAction.ClearGpsCircles) },
            leadingIcon = {Icon (Icons.Outlined.ClearAll, null)}
        )
        HorizontalDivider()
        poiHome?.let { home ->
            DropdownMenuItem(
                { Text(text = resources.getString(R.string.navigate_to_home), color = Black) },
                onClick = { navigateToHome(home) },
                leadingIcon = { Icon(Icons.Outlined.Home, null) }
            )
        }
        DropdownMenuItem(
            text = { Text(text = resources.getString(R.string.latlng_grid_), color = Black) },
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
            { Text(text = resources.getString(R.string.treat_mvt), color = Black) },
            onClick = { finished(MapLongClickAction.MvtBbbike09) },
            leadingIcon = { Icon(Icons.Outlined.OpenInBrowser, null) }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MapLongClickMenuPreview() {
    RamaniTheme {
        MapLongClickMenuContent(
            latlngStateInitial = true,
            changeGridState = { _, _ -> },
            finished = {},
            navigateToHome = {}
        )
    }
}
