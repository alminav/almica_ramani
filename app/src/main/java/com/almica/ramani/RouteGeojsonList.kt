package com.almica.ramani

import android.content.Context
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import com.almica.ramani.charts.theme.Black
import com.almica.ramani.utils.GeoJsonUtils.Companion.getRegionsFromRouteGeojson
import timber.log.Timber
import java.io.File


@Composable
fun RouteGeojsonList(
    context: Context,
    finished: (File?) -> Unit
) {
    val geojsonFiles = Helpers.getRouteGeojsonFiles(context)
    DropdownMenu(
        expanded = true,
        onDismissRequest = { finished(null) }
    ) {
        geojsonFiles.forEach { geojsonFile ->
            DropdownMenuItem(
                {
                    Text(text = geojsonFile.nameWithoutExtension,
                        color = Black)
                },
                onClick = { finished(geojsonFile) },
            )
        }
    }
}
