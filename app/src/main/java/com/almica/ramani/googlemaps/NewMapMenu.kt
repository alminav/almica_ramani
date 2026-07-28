package com.almica.ramani.googlemaps

import android.content.Context
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.Const
import com.almica.ramani.R
import com.almica.ramani.charts.theme.Black
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.GeoJsonUtils
import com.google.android.gms.maps.model.LatLng

enum class NewMapAction {
    Nothing,
    Create,
    Import
}
@Composable
fun NewMapMenu(
    context: Context,
    latLng: LatLng?,
    finished: (NewMapAction?, String?) -> Unit
) {
    val preferences = getDefaultSharedPreferences(context)
    val mapType = preferences.getString(context.getString(R.string.pref_tilemaker_maptype), Const.OUTDOOR)

    DropdownMenu(
        expanded = true,
        onDismissRequest = { finished(NewMapAction.Nothing, null) }
    ) {
        DropdownMenuItem(
            { Text(text = context.getString(R.string.create_map), color = Black) },
            onClick = { finished(NewMapAction.Create, null) }
        )
        HorizontalDivider()
        DropdownMenuItem(
            { Text(text = context.getString(R.string.import_map), color = Black) },
            onClick = {
                if (latLng != null) {
                    val tile10 = GeoJsonUtils.pointToTile(latLng.longitude, latLng.latitude, 10.0)
                    val mapName = "tile_${tile10.x}_${tile10.y}_${tile10.z}_$mapType"
                    finished(NewMapAction.Import, mapName)
                } else
                    finished(NewMapAction.Import, null)
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NewMapMenuPreview() {
    RamaniTheme {
        NewMapMenu(
            context = LocalContext.current,
            latLng = LatLng(-1.286389, 36.817223),
            finished = { _, _ -> }
        )
    }
}
