package com.almica.ramani.geojsonMaps

import android.graphics.Bitmap
import android.util.Log
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.almica.ramani.utils.GeoJsonUtils
import timber.log.Timber
import java.util.Locale
import java.util.UUID

private const val logtag = "GeojsonMapEntity"
@Entity(tableName = "geojsonMaps", indices = [Index(value = ["name"], unique = true)])
data class GeojsonMapEntity(
    @PrimaryKey var id: UUID = UUID.randomUUID(),
    var name: String = "",
    //@Ignore var geojson: String = "",
    var path: String = "", // used as region 541_335_10, 541_336_10 ...
    var x: Int = 0,
    var y: Int = 0,
    var z: Int = 0,
    var north: Double = 0.0,
    var south: Double = 0.0,
    var east: Double = 0.0,
    var west: Double = 0.0,
    var bitmap: Bitmap? = null,
    var centerLatitude: Double = 0.0,
    var centerLongitude: Double = 0.0,
    var enabled: Boolean = false,
    var data: ByteArray? = null,
    var lastModifiedTime: Long = 0L
){
    constructor(x: Int, y: Int, z: Int, path: String, enabled: Boolean, data: ByteArray?, lastModifiedTime: Long) : this() {
        this.id = UUID.randomUUID()
        Timber.i("$name id:$id")
        this.name = name
        this.x = x
        this.y = y
        this.z = z
        val geojsonBounds = GeoJsonUtils.tileToGmsBounds(GeoJsonUtils.Companion.Tile(x, y, z))
        this.north = geojsonBounds.northeast.latitude
        this.south = geojsonBounds.southwest.latitude
        this.east = geojsonBounds.northeast.longitude
        this.west = geojsonBounds.southwest.longitude
        this.centerLatitude = geojsonBounds.center.latitude
        this.centerLongitude = geojsonBounds.center.longitude
        this.name = "geojsonTile_${x}_${y}_${z}"
        this.path = path
        this.enabled = enabled
        this.data = data
        this.lastModifiedTime = lastModifiedTime
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GeojsonMapEntity

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false
        if (north != other.north) return false
        if (south != other.south) return false
        if (east != other.east) return false
        if (west != other.west) return false
        if (centerLatitude != other.centerLatitude) return false
        if (centerLongitude != other.centerLongitude) return false
        if (enabled != other.enabled) return false
        if (lastModifiedTime != other.lastModifiedTime) return false
        if (id != other.id) return false
        if (name != other.name) return false
        if (path != other.path) return false
        if (bitmap != other.bitmap) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        result = 31 * result + north.hashCode()
        result = 31 * result + south.hashCode()
        result = 31 * result + east.hashCode()
        result = 31 * result + west.hashCode()
        result = 31 * result + centerLatitude.hashCode()
        result = 31 * result + centerLongitude.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + lastModifiedTime.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + (bitmap?.hashCode() ?: 0)
        result = 31 * result + (data?.contentHashCode() ?: 0)
        return result
    }

}

