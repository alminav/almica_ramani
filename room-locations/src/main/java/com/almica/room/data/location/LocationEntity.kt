package com.almica.room.data.location

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.almica.room.data.Const
import java.text.DateFormat
import java.util.*
import kotlin.math.abs

@Entity(tableName = "location")
data class LocationEntity(
    @PrimaryKey var id: UUID = UUID.randomUUID(),
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,
    var speed: Float = 0.0f,
    var bearing: Float = 0.0f,
    var hasBearing: Boolean = true,
    var time: Long = 0,
    var distanceM: Double = 0.0,
    var foreground: Boolean = true,
    var recordedAt: Date = Date(),
    internal var latitudeStart: Double = 0.0, // tag ComposeInternal
    internal var longitudeStart: Double = 0.0 // tag ComposeInternal
) {
    override fun toString(): String {
        val appState = if (foreground) {
            "in app"
        } else {
            "in BG"
        }

        return "${latitude.format(4)}, ${longitude.format(4)}, ${formatAlti(altitude, true)}, " +
                "${speed.format(1)}KmH, ${Const.UC_DISTANCE_ARROW}${formatDistM(distanceM, true)}, $appState on " +
                "${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.GERMAN).format(recordedAt)}.\n"
    }
    fun shortString(): String {
        val appState = if (foreground) {
            "in app"
        } else {
            "in BG"
        }

        return "$appState on " +
                "${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.GERMAN).format(recordedAt)}.\n"
    }

    fun getRecordAt(): String {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.GERMAN).format(recordedAt)
    }
    fun getCoordinates(): String {
        return "${latitude.format(4)}, ${longitude.format(4)}, ${altitude.format(1)}m"
    }
    fun getNotificationText(): String {
        return "${latitude.format(4)}, ${longitude.format(4)} ${Const.UC_ELE_ARROW}${altitude.format(1)}m ${Const.UC_DISTANCE_ARROW}${formatDistM(distanceM, true)}"
    }

    fun formatAlti(d: Double, bMetric: Boolean): String {
        if (java.lang.Double.isNaN(d)) return ""
        return if (bMetric) java.lang.String.format(Locale.ENGLISH,
            "%s%.0f%s",
            Const.UC_ELE_ARROW, d, "m"
        )
        else java.lang.String.format(Locale.ENGLISH,
            "%s%.0f%s", Const.UC_ELE_ARROW,
            Const.M_TO_FT * d, "ft"
        )
    }

    fun formatDistM(d: Double, bMetric: Boolean): String {
        var value = d
        var sUnit = "km"
        if (!bMetric) {
            value = Const.KM_TO_MILES * d
            sUnit = "mi"
        }

        return if (abs(value) < 1000) java.lang.String.format(Locale.getDefault(), "%.0f%s", d, "m")
        else if (abs(value) < 10000) java.lang.String.format(
            Locale.ENGLISH,
            "%.1f%s",
            value / 1000,
            sUnit
        )
        else if (abs(value) < 100000) java.lang.String.format(
            Locale.ENGLISH,
            "%.1f%s",
            value / 1000,
            sUnit
        )
        else java.lang.String.format(Locale.ENGLISH, "%.0f%s", value / 1000, sUnit)
    }

}

fun Double.format(digits: Int) = "%.${digits}f".format(Locale.ENGLISH, this)
fun Float.format(digits: Int) = "%.${digits}f".format(Locale.ENGLISH, this)