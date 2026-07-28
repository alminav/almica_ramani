package com.almica.ramani.tilemaker

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log
import com.almica.ramani.Const
import timber.log.Timber
import java.io.File
import java.util.Locale
import kotlin.math.pow
private const val logtag = "MbtilesDatabase"
open class MbtilesDatabase {
    object MetadataEntry : BaseColumns {
        const val TABLE_NAME = "metadata"
        const val COLUMN_NAME = "name"
        const val COLUMN_VALUE = "value"
    }

    object TilesEntry : BaseColumns {
        const val TABLE_NAME = "tiles"
        const val COLUMN_ZOOM_LEVEL = "zoom_level"
        const val COLUMN_TILE_COLUMN = "tile_column"
        const val COLUMN_TILE_ROW = "tile_row"
        const val COLUMN_TILE_DATA = "tile_data"
    }

    internal class DatabaseContext(base: Context?) : ContextWrapper(base) {
        override fun getDatabasePath(name: String): File {
            val sdcard = baseContext.filesDir
            //val extDatabases = File(sdcard, Const.MBTILES_PREFIX + mapType)
            val extDatabases = File(sdcard, Const.MBTILES)
            val resultMkdir = extDatabases.mkdir()
            if (resultMkdir)
                Timber.i("${extDatabases.path} result mkdir: true")
            val result = File(extDatabases, name)
            return result
        }
    }

    class MbtilesHelper(
        context: Context,
        dbName: String
    ) :
        SQLiteOpenHelper(DatabaseContext(context), dbName, null, DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(SQL_CREATE_METADATA)
            db.execSQL(SQL_CREATE_TILES)
            Timber.i("${db.path}")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Timber.i(" ")
            // Nothing
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Timber.i(" ")
            // Nothing
        }

        companion object {
            const val DATABASE_VERSION = 1
        }
    }

    companion object {
        fun insertMetadata(db: SQLiteDatabase?, name: String, value: String) {
            val values = ContentValues().apply {
                put(MetadataEntry.COLUMN_NAME, name)
                put(MetadataEntry.COLUMN_VALUE, value)
            }

            db?.insert(MetadataEntry.TABLE_NAME, null, values)
        }

        /**
         * OpenLayers Bounds format - left, bottom, right, top
         */
        fun insertMetadata(
            db: SQLiteDatabase?,
            name: String,
            northWest: DoubleArray,
            southEast: DoubleArray,
            minZoom: Int,
            maxZoom: Int
        ) {
            insertMetadata(db, "name", name)
            insertMetadata(db, "type", "baselayer")
            insertMetadata(db, "version", "1.1")
            insertMetadata(db, "description", "$name created by TileMaker")
            insertMetadata(db, "format", "png")
            insertMetadata(db, "minZoom", minZoom.toString())
            insertMetadata(db, "maxZoom", maxZoom.toString())
            val bounds = String.format(Locale.ENGLISH, "%.4f,%.4f,%.4f,%.4f",
                northWest[0], southEast[1], southEast[0], northWest[1])
            insertMetadata(db, "bounds", bounds)
        }

        fun insertTiles(db: SQLiteDatabase?, z: Int, x: Int, y: Int, data: ByteArray) {
            val values = ContentValues().apply {
                put(TilesEntry.COLUMN_ZOOM_LEVEL, z)
                put(TilesEntry.COLUMN_TILE_COLUMN, x)
                put(TilesEntry.COLUMN_TILE_ROW, 2.toDouble().pow(z) - 1 - y)
                put(TilesEntry.COLUMN_TILE_DATA, data)
            }

            val result = db?.insert(TilesEntry.TABLE_NAME, null, values)
            // result = -1: error
            Timber.i(" result: $result bytes: ${data.size}")
        }

        fun removeTiles(db: SQLiteDatabase?, z: Int, col: Int, row: Int): Int? {
            val selection = TilesEntry.COLUMN_ZOOM_LEVEL + " = ? AND " +
                    TilesEntry.COLUMN_TILE_COLUMN + " = ? AND " +
                    TilesEntry.COLUMN_TILE_ROW + " = ?"
            val selectionArgs =
                arrayOf(z.toString(), col.toString(), row.toString())

            return db?.delete(TilesEntry.TABLE_NAME, selection, selectionArgs)
        }

        fun getTileBitmap(db: SQLiteDatabase?, z: Int, col: Int, row: Int): Cursor? {
            val selection = TilesEntry.COLUMN_ZOOM_LEVEL + " = ? AND " +
                    TilesEntry.COLUMN_TILE_COLUMN + " = ? AND " +
                    TilesEntry.COLUMN_TILE_ROW + " = ?"
            val columns = arrayOf(TilesEntry.COLUMN_TILE_DATA)
            val selectionArgs = arrayOf(z.toString(), col.toString(), row.toString())
            return db?.query(TilesEntry.TABLE_NAME, columns, selection, selectionArgs, null, null, null)
        }

        const val SQL_CREATE_METADATA =
            "CREATE TABLE ${MetadataEntry.TABLE_NAME} (" +
                    "${MetadataEntry.COLUMN_NAME} text," +
                    "${MetadataEntry.COLUMN_VALUE} test)"
        const val SQL_CREATE_TILES =
            "CREATE TABLE ${TilesEntry.TABLE_NAME} (" +
                    "${TilesEntry.COLUMN_ZOOM_LEVEL} integer," +
                    "${TilesEntry.COLUMN_TILE_COLUMN} integer," +
                    "${TilesEntry.COLUMN_TILE_ROW} integer," +
                    "${TilesEntry.COLUMN_TILE_DATA} blob, " +
                    "PRIMARY KEY (${TilesEntry.COLUMN_ZOOM_LEVEL}, ${TilesEntry.COLUMN_TILE_COLUMN}, ${TilesEntry.COLUMN_TILE_ROW}))"
    }

}
