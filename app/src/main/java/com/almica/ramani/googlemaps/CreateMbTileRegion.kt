package com.almica.ramani.googlemaps

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.Const
import com.almica.ramani.R
import com.almica.ramani.tilemaker.MbtilesCreator
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.GeoJsonUtils
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun CreateMbTileRegion(regionName: String, progress_: (Int) -> Unit, finished: () -> Unit) {
    val context = LocalContext.current
    val preferences = getDefaultSharedPreferences(context)
    val tilemakerUrl = preferences.getString(context.getString(R.string.pref_tilemaker_url), Const.URL_PHONEMAPS)
    //var mapType = preferences.getString(context.getString(R.string.pref_tilemaker_maptype), Const.OUTDOOR)

    Timber.i(regionName)
    val splits = regionName.split(Const.UNDERLINE, limit = 5)
    val mapType = splits[4]
    val tile = GeoJsonUtils.Companion.Tile(
        splits[1].toInt(),
        splits[2].toInt(),
        splits[3].toInt()
    )
    val bounds = GeoJsonUtils.tileToBoundsMaplibre(tile)
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            bounds.let {
                tilemakerUrl?.let { baseUrl ->
                    MbtilesCreator(
                        context = context
                    ).createMbtiles(
                        regionName,
                        mapType = mapType,
                        baseUrl = baseUrl,
                        area = arrayOf(
                            LatLng(it.northWest.latitude, it.northWest.longitude),
                            LatLng(it.southWest.latitude, it.southWest.longitude),
                            LatLng(it.southEast.latitude, it.southEast.longitude),
                            LatLng(it.northEast.latitude, it.northEast.longitude)
                        ),
                        zooms = intArrayOf(10, 15),
                        progress = { job, p ->
                            Timber.i( "downloadActive: progress: $p")
                            progress_(p)
                            //progressAnimation = (0.01f * p)
                            //progress(progressAnimation, "$p %")
                        },
                        cancel = {
                            Timber.i("canceled: $regionName")
                            progress_(100)
                            finished()
                            //progressAnimation = (0.0f)
                            //progress(progressAnimation, "$0 %")
                        },
                        ready = {
                            Timber.i("ready: $regionName")
                            progress_(100)
                            //progressAnimation = (0.0f)
                            //progress(progressAnimation, "$0 %")
                            GeoJsonUtils.createRasterMapsBounds(context) { path ->
                                Timber.i("new grid: $path")
                            }
                            finished()
                        }
                    )
                }
                //TilemakerComposeScreen(it, regionName) { tilemakerMode = false }
                //Timber.i( "$regionName")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateMbTileRegionPreview() {
    RamaniTheme {
        CreateMbTileRegion(
            regionName = "sample_1083_673_11_Outdoor",
            progress_ = {},
            finished = {}
        )
    }
}
