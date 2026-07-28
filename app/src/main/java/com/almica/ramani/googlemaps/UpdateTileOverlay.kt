package com.almica.ramani.googlemaps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.format
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileProvider
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberTileOverlayState
import timber.log.Timber

/**
 * This composable demonstrates how to use a [TileOverlay] with a [TileProvider] that
 * updates its content periodically.
 */
@Composable
fun UpdateTileOverlay(latLng: LatLng, tileProviderMbTiles: String) {
    Timber.i(" ${latLng.latitude.format(4)} ${latLng.longitude.format(4)}")
    val context = LocalContext.current
//    var tileProviderIndex by remember { mutableIntStateOf(0) }
//    val tileProviderMbTiles = getMbTileName(context, latLng)
    Timber.i( "tileProviderMbTiles: $tileProviderMbTiles")
//    var renderedIndex by remember { mutableIntStateOf(0) }
    val state = rememberTileOverlayState()
//    val TILE_URL = "https://tile0.maps.2gis.com/tiles?x=%d&y=%d&z=%d&v=1"
//    val opoTopoUrl = "https://a.tile.opentopomap.org/%d/%d/%d.png"
//    val phoneMapsUrl = "https://webtiles.timepress.cz/open/hike_256/%d/%d/%d.png"
//    val MIN_ZOOM = 2
//    val MAX_ZOOM = 18
//    val size = with(LocalDensity.current) { 256.dp.toPx() }.toInt()

    val tileProvider = remember(tileProviderMbTiles) { //remember(tileProviderIndex) {
        object : LocalTileProvider(context, latLng, 256, 256) {
//            override fun getTile(x: Int, y: Int, z: Int): Tile? {
//                renderTiles(renderedIndex, size)
//                return super.getTile(x, y, z)
//            }
        }
        /*
                TileProvider { _, _, _ ->
                    Tile(size, size, renderTiles(renderedIndex, size))
                }
         */

        /*
                object : UrlTileProvider(256, 256) {
                    override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {
                        //val s = String.format(java.util.Locale.ENGLISH, TILE_URL, x, y, zoom)
                        //val s = String.format(java.util.Locale.ENGLISH, optopUrl, zoom, x, y)
                        val s = String.format(java.util.Locale.ENGLISH, phoneMapsUrl, zoom, x, y)
                        Timber.i( "$s")
                        if (zoom !in MIN_ZOOM..MAX_ZOOM) {
                            return null
                        }

                        try {
                            return URL(s)
                        } catch (e: Exception) {
                            throw AssertionError(e)
                        }
                    }
                }
        */
    }
    Timber.i( "${tileProvider.mbtilesName}")
    TileOverlay(tileProvider = tileProvider, state = state, fadeIn = false, visible = true)
    //finished(tileProvider.mbtilesName)
    /*
        LaunchedEffect(Unit) {
            // This LaunchedEffect demonstrates two ways to update a tile overlay.

            // 1. Invalidate the cache to redraw tiles with new data.
            // Here, we're calling `state.clearTileCache()` every second for 5 seconds.
            // This tells the map to request new tiles from the *existing* TileProvider,
            // which will then re-render them using the latest `renderedIndex`.
            repeat(5) {
                delay(1000)
                renderedIndex += 1
                state.clearTileCache()
            }

            // 2. Update the TileProvider instance itself.
            // After 5 seconds, we update `tileProviderIndex`. Because this is a key
            // to the `remember` block for our TileProvider, Compose will discard the
            // old provider and create a new one.
            tileProviderIndex += 1

            // Now, we continue invalidating the cache to demonstrate that the *new*
            // TileProvider is the one responding to the `clearTileCache` calls.
            while (true) {
                delay(1000)
                renderedIndex += 1
                state.clearTileCache()
            }
        }
     */
}