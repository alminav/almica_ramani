package com.almica.ramani.googlemaps

import android.content.Context
import androidx.preference.PreferenceManager
import com.almica.ramani.Const
import com.almica.ramani.R
import com.almica.ramani.tilemaker.MbtilesDatabase
import com.almica.ramani.utils.GeoJsonUtils
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import timber.log.Timber
import java.net.URL
import kotlin.math.pow

/**
 *
 */
abstract class LocalTileProvider : TileProvider {
    private val zza: Int
    private val zzb: Int
    private val ctx: Context?
    val mbtilesName: String?

    override fun getTile(x: Int, y: Int, z: Int): Tile? {
        if (ctx != null && mbtilesName != null) {
            val byteArray = getByteArrayFromMbtiles(ctx, mbtilesName, x, y, z)
            return if (byteArray != null) {
                Tile(zza, zzb, byteArray)
            } else {
                Timber.e( "$mbtilesName: byteArray = null")
                TileProvider.NO_TILE
            }
        }
        return TileProvider.NO_TILE
    }

    fun getByteArrayFromMbtiles(
        context: Context,
        mbtilesName: String,
        x: Int,
        y: Int,
        z: Int
    ): ByteArray? {
        val dbName = "${mbtilesName}${Const.MBTILES_EXT}"
        val dbFile = MbtilesDatabase.DatabaseContext(context).getDatabasePath(dbName)
        if (dbFile.exists()) { // prevent database create
            //Timber.i( "raster mapname: $dbName")
            val dbHelper = MbtilesDatabase.MbtilesHelper(context.applicationContext, dbName)
            try {
                val db = dbHelper.readableDatabase
                val row =
                    2.0.pow(z.toDouble()) - y - 1
                //Log.i(logtag, "dbName: $dbName $z $x $y")
                val cursor = MbtilesDatabase.getTileBitmap(db, z, x, row.toInt())
                cursor?.use {
                    if (it.moveToFirst()) {
                        return it.getBlob(0)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error reading MBTiles database: $dbName")
            } finally {
                dbHelper.close()
            }
            return null
        } //else Timber.i( "NOT FOUND: ${dbFile.path}")
        return null
    }

    constructor(width: Int, height: Int) {
        this.zza = width
        this.zzb = height
        this.ctx = null
        this.mbtilesName = null
    }

    constructor(context: Context, mbtilesName: String?, width: Int, height: Int) {
        this.zza = width
        this.zzb = height
        this.ctx = context
        this.mbtilesName = mbtilesName
    }

    constructor(context: Context, latLng: LatLng, width: Int, height: Int) {
        this.zza = width
        this.zzb = height
        this.ctx = context
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val mapType = preferences.getString(context.getString(R.string.pref_tilemaker_maptype), Const.OUTDOOR)
        val tile10 = GeoJsonUtils.pointToTile(latLng.longitude, latLng.latitude, 10.0)
        val mbtilesName = "tile_${tile10.x}_${tile10.y}_${tile10.z}_$mapType"
        Timber.i( "mbtilesName: $mbtilesName")
        this.mbtilesName = mbtilesName
    }
}
