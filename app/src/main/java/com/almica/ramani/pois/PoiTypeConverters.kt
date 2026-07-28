package com.almica.ramani.pois

import androidx.room.TypeConverter
import java.util.*
import kotlin.let

class PoiTypeConverters {

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(millisSinceEpoch: Long?): Date? {
        return millisSinceEpoch?.let {
            Date(it)
        }
    }

    @TypeConverter
    fun fromUUID(uuid: UUID?): String? {
        return uuid?.toString()
    }

    @TypeConverter
    fun toUUID(uuid: String?): UUID? {
        return UUID.fromString(uuid)
    }
/*
    @TypeConverter
    fun categoryToDrawable(categoryDrawable: Int): Int {
        when (categoryDrawable) {
            0 -> return  R.drawable.mx_village
            1 -> return  R.drawable.s_street_small
            2 -> return  R.drawable.s_food_small
            3 -> return  R.drawable.ic_supermarket
            4 -> return  R.drawable.ic_bakery
            5 -> return  R.drawable.s_health_small
            6 -> return  R.drawable.s_health_small
            7 -> return  R.drawable.s_accommo_small
            8 -> return  R.drawable.s_pow_small
            9 -> return  R.drawable.mx_cemetery
            10 -> return  R.drawable.ic_fuel
            11 -> return  R.drawable.s_tourist_small
            12 -> return  R.drawable.mx_leisure_water_park
            13 -> return  R.drawable.mx_sport_stadium
            14 -> return  R.drawable.s_parking_place_small
            15 -> return  R.drawable.mx_amenity_school
            16 -> return  R.drawable.baseline_airplanemode_active_24
            17 -> return  R.drawable.mx_park
            18 -> return  R.drawable.s_tower_small
            19 -> return  R.drawable.s_peak_small
            20 -> return  R.drawable.mx_water
        }
        return -1
    }
*/

}
