package com.almica.ramani

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.almica.ramani.charts.theme.Black

@Composable
fun BbbikeFunctionsMenu(context: Context, finished: (LayersControlAction) -> Unit) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = { finished(LayersControlAction.Nothing) }
    ) {
        DropdownMenuItem(
            { Text(text = context.getString(R.string.bbbike_geojson), color = Black) },
            onClick = { finished(LayersControlAction.GeojsonBbbike12) },
            leadingIcon = { Icon(Icons.Outlined.OpenInBrowser, null) }
        )
        DropdownMenuItem(
            { Text(text = context.getString(R.string.bbbike_geojson13), color = Black) },
            onClick = { finished(LayersControlAction.GeojsonBbbike13) },
            leadingIcon = { Icon(Icons.Outlined.OpenInBrowser, null) }
        )
        DropdownMenuItem(
            { Text(text = context.getString(R.string.bbbike_pmtiles11), color = Black) },
            onClick = { finished(LayersControlAction.PmtilesBbbike11) },
            leadingIcon = { Icon(Icons.Outlined.OpenInBrowser, null) }
        )
        DropdownMenuItem(
            { Text(text = context.getString(R.string.bbbike_pmtiles10), color = Black) },
            onClick = { finished(LayersControlAction.PmtilesBbbike10) },
            leadingIcon = { Icon(Icons.Outlined.OpenInBrowser, null) }
        )
        DropdownMenuItem(
            { Text(text = context.getString(R.string.bbbike_mvt09), color = Black) },
            onClick = { finished(LayersControlAction.MvtBbbike09) },
            leadingIcon = { Icon(Icons.Outlined.OpenInBrowser, null) }
        )
    }
}
