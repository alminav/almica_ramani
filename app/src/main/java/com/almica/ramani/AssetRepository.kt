package com.almica.ramani

import android.content.Context
import com.almica.ramani.Helpers.Companion.copyAssetPlanetMbtiles
import com.almica.ramani.Helpers.Companion.copyAssetPlanetStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class AssetRepository(private val context: Context) {

    suspend fun copyAssetsIfNeeded() = withContext(Dispatchers.IO) {
        //Timber.i("copyAssetsIfNeeded")
        val mbtiles = listOf(Const.COUNTRIES_MVT_FILENAME, Const.PLANET_MVT_FILENAME)
        mbtiles.forEach { copyAssetPlanetMbtiles(context, it) }

        val styles = listOf(
            Const.MVT_OFFLINE_STYLE_FILENAME, Const.PLANET_STYLE_FILENAME,
            Const.COUNTRIES_STYLE_FILENAME, Const.GEOJSON_OFFLINE_STYLE_FILENAME,
            Const.MAPTILER_REMOTE_STYLE_FILENAME, Const.ONLY_BACKGROUND_FILENAME,
            Const.EMPTY_STYLE_FILENAME
        )
        styles.forEach { copyAssetPlanetStyle(context, it) }
    }
}
