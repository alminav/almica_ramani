package com.almica.ramani.utils

import android.content.Context
import com.almica.ramani.Const
import com.almica.ramani.LatLngH
import com.almica.ramani.utils.isNotNull
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.nio.channels.FileChannel
import kotlin.math.roundToInt

private const val logtag = "HgtReader"
/**
 * Class HgtReader reads data from SRTM HGT files. Currently this class is restricted to a resolution of 3 arc seconds.
 *
 *
 * SRTM data files are available at the [NASA SRTM site](http://dds.cr.usgs.gov/srtm/version2_1/SRTM3)
 *
 * @author Oliver Wieland &lt;oliver.wieland@online.de&gt;
 */
class HgtReader(private val context: Context, private var hgtFile: File?) {
    private val cache = HashMap<String, ShortBuffer?>()
    init {
        hgtFile?.let {
            //Timber.i( "${it.path} ${it.exists()}")
            bounds = getTileRect(it.name.replace(Const.HGT_EXT, ""))
        }
        val hgtFolder = File(context.filesDir, Const.HGT_FOLDER_NAME)
        var hgtFiles: Array<File>? = hgtFolder.listFiles()
        hgtNames.clear()
        hgtFiles?.forEach { file ->
            val hgtName = file.name.replace(Const.HGT_EXT, "")
            hgtNames.add(hgtName)
        }
    }

    fun getElevationFromHgt(position: LatLng): Double {
        val hgtFolder = File(context.filesDir, Const.HGT_FOLDER_NAME)
        var specificHgtFile: File? = if (hgtFile.isNotNull()) hgtFile else null
        if (hgtFile == null) {
            hgtNames.forEach { hgtName ->
                if (getTileRect(hgtName)?.contains(position) ?: false) {
                    specificHgtFile = File(hgtFolder, "$hgtName${Const.HGT_EXT}")
                }
            }
        }
        if (specificHgtFile.isNotNull()) {
            //Timber.i( "localHgtFile: ${specificHgtFile!!.name}")
            try {
                // given area in cache?
                specificHgtFile?.let {
                    if (!cache.containsKey(it.path)) {
                        // fill initial cache value. If no file is found, then
                        // we use it as a marker to indicate 'file has been searched
                        // but is not there'
                        cache[specificHgtFile.path] = null

                        if (specificHgtFile.exists()) {
                            // found something: read HGT file...
                            val data = readHgtFile(specificHgtFile.path)
                            // ... and store result in cache
                            cache[specificHgtFile.path] = data
                            //Timber.i( "cache put ${specificHgtFile.path}")
                        } else
                            Timber.i( "not found: ${specificHgtFile.path}")
                    }
                }
                val newH = readElevation(position, specificHgtFile)
                return newH
            } catch (e: FileNotFoundException) {
                Timber.e("Get elevation from HGT $position + failed: => + ${e.message}")
                //System.err.println("Get elevation from HGT " + coor + " failed: => " + e.getMessage());
                // no problem... file not there
                return 0.0
            } catch (ioe: Exception) {
                // oops...
                Timber.e( ioe.message!!)
                // fallback
                return 0.0
            }
        }
        return 0.0
    }

    @Throws(Exception::class)
    private fun readHgtFile(file: String): ShortBuffer? {
        var fc: FileChannel? = null
        var sb: ShortBuffer?
        try {
            // Eclipse complains here about resource leak on 'fc' - even with 'finally' clause???
            fc = FileInputStream(file).channel
            // choose the right endianness
            val bb = ByteBuffer.allocateDirect(fc.size().toInt())
            while (bb.remaining() > 0) {
                if (fc.read(bb) == -1) break
            }
            bb.flip()
            sb = bb.order(ByteOrder.BIG_ENDIAN).asShortBuffer()
        } finally {
            fc?.close()
        }
        return sb
    }

    /**
     * Reads the elevation value for the given coordinate.
     *
     *
     * See also [stackexchange.com](http://gis.stackexchange.com/questions/43743/how-to-extract-elevation-from-hgt-file)
     *
     * @param position the coordinate to get the elevation data for
     * @return the elevation value or `Double.NaN`, if no value is present
     */
    private fun readElevation(position: LatLng, localHgtFile: File?): Double {
        val tag = localHgtFile?.path
        val sb = cache[tag]

        if (sb == null) {
            Timber.i("cache.get($tag) = null")
            return NO_ELEVATION
        }

        // see http://gis.stackexchange.com/questions/43743/how-to-extract-elevation-from-hgt-file
        val fLat = frac(position.latitude) * SECONDS_PER_MINUTE
        val fLon = frac(position.longitude) * SECONDS_PER_MINUTE

        // compute offset within HGT file
        var row = (fLat * SECONDS_PER_MINUTE / HGT_RES).roundToInt()
        val col = (fLon * SECONDS_PER_MINUTE / HGT_RES).roundToInt()

        row = HGT_ROW_LENGTH - row
        val cell = (HGT_ROW_LENGTH * (row - 1)) + col

        // valid position in buffer?
        if (cell < sb.limit()) {
            val ele = sb[cell]
            // check for data voids
            return if (ele.toInt() == HGT_VOID) {
                0.0
            } else {
                ele.toDouble()
            }
        } else {
            return 0.0
        }
    }

    fun contains(latLng: LatLng) : Boolean {
        return bounds.isNotNull() && bounds?.contains(latLng) == true
    }

    data class SrtmRefresh(val hMax: Double, val lllh: List<LatLngH>?)
    fun refreshRouteElevationFromSrtm(lllh: List<LatLngH>?) : SrtmRefresh {
        var hMax = 0.0
        val resultLllh = lllh?.let {
            //Timber.i( "lllh: ${lllh.size}")
            List(it.size) {index ->
                val latLng = LatLng(lllh[index].latitude, lllh[index].longitude)
                if (contains(latLng)) {
                    val srtmAltitude = getElevationFromHgt(latLng)
                    hMax = hMax.coerceAtLeast(srtmAltitude)
                    LatLngH(latLng.latitude, latLng.longitude, getElevationFromHgt(latLng))
                } else
                    LatLngH(latLng.latitude, latLng.longitude,
                        0.0.coerceAtLeast(lllh[index].altitude)
                    )
            }
        }
        return SrtmRefresh(hMax, resultLllh)
    }

    companion object {
        private const val SECONDS_PER_MINUTE = 60

        // alter these values for different SRTM resolutions
        private const val HGT_RES = 3 // resolution in arc seconds
        private const val HGT_ROW_LENGTH = 1201 // number of elevation values per line
        private const val HGT_VOID = -32768 // magic number which indicates 'void data' in HGT file

        var NO_ELEVATION: Double = Int.MIN_VALUE.toDouble()

        private fun frac(d: Double): Double {
            val fPart: Double

            // Get user input
            val iPart = d.toLong()
            fPart = d - iPart
            return fPart
        }
        var bounds : LatLngBounds? = null
        var hgtNames: ArrayList<String> = arrayListOf()
    }
}

fun getTileRect(tileName: String): LatLngBounds? {
    try {
        val lat = tileName.substring(1, 3).toDouble()
        var lon = tileName.substring(4, 7).toDouble()
        val sZone = tileName[3].toString()
        if (sZone.equals("w", ignoreCase = true)) lon *= -1
        val latLngBounds = LatLngBounds.Builder()
        latLngBounds.include(LatLng(lat.coerceIn(-90.0, 90.0), lon))
        latLngBounds.include(LatLng((lat + 1).coerceIn(-90.0, 90.0), lon + 1))
        return latLngBounds.build()
    } catch (e: java.lang.NumberFormatException) {
        Timber.i( "$tileName NumberFormatException + ${e.message}")
        return null
    }
}
