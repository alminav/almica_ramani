package com.almica.ramani

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.isNotNull
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.snapshotter.MapSnapshot
import timber.log.Timber
import java.io.File

/**
 * 12mar2026
 * Possible replacement for MoboGoogleMap in ListGeojsonScreen
 * but MoboGoogleMap works better
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MbsGeojsonMapSnapshot(tile: GeoJsonUtils.Companion.Tile, actionText: String, action: (String) -> Unit, finished: (Boolean) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var snapshotBitmap: ImageBitmap? by remember { mutableStateOf(null) }
    var title: String? by remember { mutableStateOf(null) }
    val mapName = "geojsonTile_${tile.x}_${tile.y}_${tile.z}"
    Timber.i("mapName: $mapName")
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycleScope.launch {
            takeMapSnapshot(context, tile, finish = { mapSnapShot, bounds ->
                snapshotBitmap = mapSnapShot?.bitmap?.asImageBitmap()
                if (snapshotBitmap == null) {
                    Timber.i("snapshotBitmap = null")
                    finished(false)
                }
            })
        }.invokeOnCompletion {
            Timber.i("invokeOnCompletion")
        }
    }

    snapshotBitmap?.let {
        ModalBottomSheet(onDismissRequest = { finished(true) }) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text(
                        text = mapName,
                        modifier = Modifier.weight(0.7f),
                        textAlign = TextAlign.Center
                    )
                    Box(modifier = Modifier
                        .align(alignment = Alignment.CenterVertically)
                        .weight(0.3f),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { action(actionText) }) { Text(text = actionText)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                title?.let { text -> Text(text = text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = BitmapPainter(
                            snapshotBitmap!!, IntOffset(0, 0),
                            IntSize(snapshotBitmap!!.width, snapshotBitmap!!.height)
                        ),
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
private fun takeMapSnapshot(context: Context, tile: GeoJsonUtils.Companion.Tile, finish: (MapSnapshot?, LatLngBounds) -> Unit) {
    val mapName = "geojsonTile_${tile.x}_${tile.y}_${tile.z}"
    Timber.i("mapName: $mapName")
    var stylePlanetUri : String? = null
    val rootFolder = context.filesDir
    val mvtRootFolder = File(rootFolder, Const.MVT_FOLDER)
    val styleFile = File(mvtRootFolder, Const.PLANET_STYLE_FILENAME)
    if (styleFile.exists())
        stylePlanetUri = Uri.fromFile(styleFile).toString()
    val bounds = GeoJsonUtils.tileToBounds(tile)
    Timber.i("bounds: $bounds")
    val lllh = arrayListOf<LatLngH>()
    lllh.add(LatLngH(bounds.northWest.latitude, bounds.northWest.longitude))
    lllh.add(LatLngH(bounds.northEast.latitude, bounds.northEast.longitude))
    lllh.add(LatLngH(bounds.southEast.latitude, bounds.southEast.longitude))
    lllh.add(LatLngH(bounds.southWest.latitude, bounds.southWest.longitude))
    lllh.add(LatLngH(bounds.northWest.latitude, bounds.northWest.longitude))
    Helpers.takeSnapshot(
        context, lllh, mapName,
        //Const.styleVectorUri,
        if (stylePlanetUri.isNotNull()) stylePlanetUri else Const.styleVectorUri,
        640, 0.1, false,
    ) { snapshot ->
        finish(snapshot, bounds)
        Timber.i("${System.currentTimeMillis()} bounds:$bounds snapshot ready")
    }
}
