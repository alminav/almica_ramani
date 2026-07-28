package com.almica.ramani.geojsonMaps

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.UUID

private const val logtag = "GeojsonMapTypeConverters"
class GeojsonMapTypeConverters {
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
        Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: byteArray: ${byteArray.size}")
        return byteArray
    }

    @TypeConverter
    fun getBitmapFromByteArray(byteArray: ByteArray): Bitmap?{
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
/*
        if (bitmap != null)
            Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: bitmap: ${byteArray.size} ${bitmap.width} x ${bitmap.height}")
        else
            Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: bitmap = null")
 */
        return bitmap
    }
}
