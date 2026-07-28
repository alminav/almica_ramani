package com.almica.ramani.googlemaps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import java.io.ByteArrayOutputStream
import androidx.core.graphics.createBitmap
import com.almica.ramani.Const
import java.io.File
import java.io.FileFilter


abstract class CoordinateTileProvider(context: Context, mapType_: String) : TileProvider {
    /* Scale factor based on density, with a 0.6 multiplier to increase tile generation
         * speed */
    private val mScaleFactor: Float = context.resources.displayMetrics.density * 0.6f
    private val ctx: Context = context
    private val mapType = mapType_

    override fun getTile(x: Int, y: Int, zoom: Int): Tile {
        val coordTile = createTile(x, y, zoom)
        val stream = ByteArrayOutputStream()
        coordTile.compress(Bitmap.CompressFormat.PNG, 0, stream)
        val bitmapData = stream.toByteArray()
        return Tile(
            (TILE_SIZE_DP * mScaleFactor).toInt(),
            (TILE_SIZE_DP * mScaleFactor).toInt(), bitmapData
        )
    }

    fun createTile(x: Int, y: Int, zoom: Int): Bitmap {
        val tile =
            createBitmap(
                (TILE_SIZE_DP * mScaleFactor).toInt(),
                (TILE_SIZE_DP * mScaleFactor).toInt()
            )
        val canvas = Canvas(tile)

        // Draw the tile borders.
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        borderPaint.style = Paint.Style.STROKE

        canvas.drawRect(
            0F, 0F, TILE_SIZE_DP * mScaleFactor,
            TILE_SIZE_DP * mScaleFactor, borderPaint
        )

        val checked =
            if (zoom == 10)
                checkRasterMapFiles(x,y)
            else
                false
        // Draw the tile position text.
        val tileCoords = if (checked) "($x, $y) ${Const.UC_CHECKMARK}" else "($x, $y)"
        val zoomLevel = "zoom = $zoom"
        val mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTextPaint.textAlign = Paint.Align.CENTER
        mTextPaint.textSize = if (zoom == 10) 18 * mScaleFactor else 14 * mScaleFactor
        if (zoom == 10) {
            val textWith = mTextPaint.measureText(tileCoords)
            borderPaint.style = Paint.Style.FILL_AND_STROKE
            borderPaint.color = Color.White.toArgb()
            canvas.drawRect(
                TILE_SIZE_DP * mScaleFactor / 2 - 0.55f * textWith,
                8 + TILE_SIZE_DP * mScaleFactor / 2,
                0.55f * textWith + TILE_SIZE_DP * mScaleFactor / 2,
                TILE_SIZE_DP * mScaleFactor / 2 - 28f,
                borderPaint
            )
        }
        canvas.drawText(
            tileCoords, TILE_SIZE_DP * mScaleFactor / 2,
            TILE_SIZE_DP * mScaleFactor / 2, mTextPaint
        )

        canvas.drawText(
            zoomLevel, TILE_SIZE_DP * mScaleFactor / 2,
            TILE_SIZE_DP * mScaleFactor * 2 / 3, mTextPaint
        )

        return tile
    }

    fun checkRasterMapFiles(x: Int, y: Int): Boolean {
        val rootFolder = ctx.filesDir
        val mbTilesRootFolder = File(rootFolder, Const.MBTILES_FOLDER)
        val filter = "tile_${x}_${y}_10_${mapType}"

        val fileFilter = FileFilter { file: File? -> file?.name?.startsWith(filter) == true &&
                !file.name.contains(Const.JOURNAL)
        }
        val files = mbTilesRootFolder.listFiles(fileFilter) as Array<File>
        return files.isNotEmpty()
    }

    companion object {
        private const val TILE_SIZE_DP = 256
    }
}