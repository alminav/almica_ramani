package com.almica.ramani.routes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.room.TypeConverter
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.*

private const val logtag = "RouteTypeConverters"
class RouteTypeConverters {
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

    @TypeConverter
    fun getByteArrayFromBitmap(bitmap: Bitmap?): ByteArray {
        if (bitmap == null) {
            return ByteArray(10)
        }
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        Timber.i( "byteArray: %s", byteArray.size)
        return byteArray
    }
    @TypeConverter
    fun getBitmapFromByteArray(byteArray: ByteArray): Bitmap?{
        if (byteArray.isEmpty())
            return null
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        return bitmap
    }
}
