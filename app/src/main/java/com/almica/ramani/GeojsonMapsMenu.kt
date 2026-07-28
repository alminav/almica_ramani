package com.almica.ramani

import android.content.Context
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.GridView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.charts.theme.Black
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber

private const val logtag = "GeojsonMapsMenu"
enum class GeojsonMapsMenuAction {
    ToggleGrid,
    ToggleVisibility,
    Remove,
    Share,
    Nothing
}
@Composable
fun GeojsonMapsMenu(
    context: Context,
    mapEntityName: String?,
    finished: (GeojsonMapsMenuAction, String?) -> Unit
) {
    Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: mapEntityName: $mapEntityName")
    var tileName by remember { mutableStateOf(mapEntityName) }
    Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: tileName: $tileName")
    tileName.let {
        DropdownMenu(
            expanded = true,
            onDismissRequest = { finished(GeojsonMapsMenuAction.Nothing, tileName) }
        ) {
            DropdownMenuItem(
                { Text(text = it!!, color = Black) },
                onClick = { finished(GeojsonMapsMenuAction.Nothing, tileName) }
            )

            DropdownMenuItem(
                { Text(text = context.getString(R.string.geojson_maps_grid_toggle), color = Black) },
                onClick = { finished(GeojsonMapsMenuAction.ToggleGrid, tileName) },
                leadingIcon = { Icon(Icons.Outlined.GridView, null) }
            )
            DropdownMenuItem(
                { Text(text = context.getString(R.string.toggle_visibility), color = Black) },
                onClick = { finished(GeojsonMapsMenuAction.ToggleVisibility, tileName) },
                leadingIcon = { Icon(Icons.Outlined.Visibility, null) }
            )
            DropdownMenuItem(
                { Text(text = context.getString(R.string.remove), color = Black) },
                onClick = { finished(GeojsonMapsMenuAction.Remove, tileName) },
                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) }
            )
            DropdownMenuItem(
                { Text(text = context.getString(R.string.share), color = Black) },
                onClick = { finished(GeojsonMapsMenuAction.Share, tileName) },
                leadingIcon = { Icon(Icons.Outlined.Share, null) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GeojsonMapsMenuPreview() {
    RamaniTheme {
        GeojsonMapsMenu(
            context = LocalContext.current,
            mapEntityName = "Sample Map",
            finished = { _, _ -> }
        )
    }
}