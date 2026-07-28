package com.almica.ramani.pois

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.almica.ramani.Helpers.Companion.latitudeToY
import com.almica.ramani.Helpers.Companion.longitudeToX
import com.almica.ramani.utils.format
import timber.log.Timber
import java.util.Locale
import java.util.UUID

private const val logtag = "PoiEntity"
@Entity(tableName = "poi")
data class PoiEntity(
    @PrimaryKey var id: UUID = UUID.randomUUID(),
    var name: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,
    var category: String = ""
){
    constructor(name: String, latitude: Double, longitude: Double, altitude: Double, category: String) : this() {
        this.id = UUID.randomUUID()
        Timber.i("$name id:$id")
        this.name = name
        this.latitude = latitude
        this.longitude = longitude
        this.altitude = altitude
        this.category = category
    }
    override fun toString(): String {
        return "$name ${latitude.format(4)}, ${longitude.format(4)}, ${altitude.format(1)}m,  $category\n"
    }
    fun getString(): String {
        return "$name ${latitude.format(4)}, ${longitude.format(4)}, ${altitude.format(1)}m, " + category + "\n"
    }
    fun getStringNoCategory(): String {
        return "$name ${latitude.format(4)}, ${longitude.format(4)}, ${altitude.format(1)}m" + "\n"
    }

    fun getCoordinates(): String {
        return "${com.almica.ramani.Const.UC_POSITION}${latitude.format(4)} ${longitude.format(4)} ${com.almica.ramani.Const.UC_ELE_ARROW}${altitude.format(0)}m"
    }

    val mercatorXY: DoubleArray
        get() = doubleArrayOf(mercatorX, mercatorY)
    val mercatorX: Double
        get() = longitudeToX(longitude)
    val mercatorY: Double
        get() = latitudeToY(latitude)

}

