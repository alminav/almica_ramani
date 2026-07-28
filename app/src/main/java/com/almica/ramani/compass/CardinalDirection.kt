// SPDX-License-Identifier: GPL-3.0-or-later

package com.almica.ramani.compass

import android.hardware.GeomagneticField
import android.location.Location
import androidx.annotation.StringRes
import com.almica.ramani.R

object CardinalDirection {

    fun getDirectionFromAzimuth(azimuth: Float) = when (azimuth) {
        in 22.5f..<67.5f -> DIRECTION.NORTHEAST
        in 67.5f..<112.5f -> DIRECTION.EAST
        in 112.5f..<157.5f -> DIRECTION.SOUTHEAST
        in 157.5f..<202.5f -> DIRECTION.SOUTH
        in 202.5f..<247.5f -> DIRECTION.SOUTHWEST
        in 247.5f..<292.5f -> DIRECTION.WEST
        in 292.5f..<337.5f -> DIRECTION.NORTHWEST
        else -> DIRECTION.NORTH
    }

    fun getDirectionFromAzimuthShort(azimuth: Float) = when (azimuth) {
        in 22.5f..<67.5f -> DIRECTION_SHORT.NORTHEAST
        in 67.5f..<112.5f -> DIRECTION_SHORT.EAST
        in 112.5f..<157.5f -> DIRECTION_SHORT.SOUTHEAST
        in 157.5f..<202.5f -> DIRECTION_SHORT.SOUTH
        in 202.5f..<247.5f -> DIRECTION_SHORT.SOUTHWEST
        in 247.5f..<292.5f -> DIRECTION_SHORT.WEST
        in 292.5f..<337.5f -> DIRECTION_SHORT.NORTHWEST
        else -> DIRECTION_SHORT.NORTH
    }
}

fun getMagneticDeclination(location: Location): Float {
    val latitude = location.latitude.toFloat()
    val longitude = location.longitude.toFloat()
    val altitude = location.altitude.toFloat()
    val time = location.time
    // Based on WGS84 geodetic coordinates
    val geomagneticField = GeomagneticField(latitude, longitude, altitude, time)
    return geomagneticField.declination
}


enum class DIRECTION(@param:StringRes val dirName: Int) {
    NORTH(R.string.north),
    NORTHEAST(R.string.northeast),
    EAST(R.string.east),
    SOUTHEAST(R.string.southeast),
    SOUTH(R.string.south),
    SOUTHWEST(R.string.southwest),
    WEST(R.string.west),
    NORTHWEST(R.string.northwest),
}

@Suppress("ClassName")
enum class DIRECTION_SHORT(@param:StringRes val dirName: Int) {
    NORTH(R.string.north_short),
    NORTHEAST(R.string.northeast_short),
    EAST(R.string.east_short),
    SOUTHEAST(R.string.southeast_short),
    SOUTH(R.string.south_short),
    SOUTHWEST(R.string.southwest_short),
    WEST(R.string.west_short),
    NORTHWEST(R.string.northwest_short),
}