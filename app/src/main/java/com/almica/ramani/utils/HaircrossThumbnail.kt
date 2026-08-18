package com.almica.ramani.utils
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.almica.ramani.GpsViewModel
import com.almica.ramani.Helpers.Companion.latitudeToY
import com.almica.ramani.Helpers.Companion.longitudeToX
import com.almica.ramani.LatLngH
import com.almica.ramani.R
import com.almica.ramani.pois.PoiEntity
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max

/**
 * The Thread that generates the Haircross Thumbnail of the Track with the given id.
 */
class HaircrossThumbnail(
    private var context: Context,
    val sites: ArrayList<LatLngH>?,
    val routePointer: Int,
    ready: (Bitmap?) -> Unit
) {
    private var yOffset = 0.0
    private var xOffset = 0.0
    private var minY = 0.0
    private var minX = 0.0
    private var track: Track = Track(sites)
    var numberOfLocations: Long = 0
    var trackBounds: LatLngBounds? = null
    var distance = 0.0
    private var size: Int = context.resources.getDimension(R.dimen.thumbSize).toInt()
    private var margin = 0
    private var sizeMinusMargins = 0
    var drawScale: Double = 0.0

    var northWest: LatLng
    var southEast: LatLng
    private var textSizeDefault = 20f

    init {
        val llboundsBuilder: LatLngBounds.Builder =
            LatLngBounds.Builder()
        if (!sites.isNullOrEmpty()) {
            distance = sites.getDistanceFromLllh()
            for (latLngH: LatLngH in sites)
                llboundsBuilder.include(latLngH.latLng)
            val llbounds = llboundsBuilder.build()
            northWest = LatLng(llbounds.northeast.latitude, llbounds.southwest.longitude)
            southEast = LatLng(llbounds.southwest.latitude, llbounds.northeast.longitude)
        } else {
            northWest = LatLng(0.0, 0.0)
            southEast = LatLng(0.0, 0.0)
        }
        val point = sites?.let { lllh ->
            if (routePointer >= 0 && routePointer < lllh.size) {
                PoiEntity(
                    UUID.randomUUID(),
                    "", lllh[routePointer].latitude, lllh[routePointer].longitude, 0.0
                )
            } else null
        }
        doInit()
        startAsyncThumbnailer(point) { bmp ->
            ready(bmp)
        }
    }

    private fun doInit() {
        margin = ceil(context.resources.getDimension(R.dimen.thumbnailMargin).toDouble()).toInt()
        //(int) Math.ceil(context.getResources().getDimension(R.dimen.thumbLineWidth) * 3);
        sizeMinusMargins = size - 2 * margin

        //Log.w("myApp", "[#] GPSApplication.java - Bitmap Size = " + Size);
        if (sites != null && (sites.size > 2) && (distance >= 15)) {
            numberOfLocations = sites.size.toLong()
            //Rect trackBounds = new Rect()
            val llboundsBuilder: LatLngBounds.Builder =
                LatLngBounds.Builder()
            trackBounds = llboundsBuilder
                .include(LatLng(track.latitudeMax, track.longitudeMax))
                .include(
                    LatLng(track.latitudeMin, track.longitudeMin)
                ).build()


            minY = track.yMin
            minX = track.xMin
            val maxY = track.yMax
            val maxX = track.xMax

            drawScale = max(maxY - minY, maxX - minX)
            yOffset = sizeMinusMargins * (1 - (maxY - minY) / drawScale) / 2
            xOffset = sizeMinusMargins * (1 - (maxX - minX) / drawScale) / 2
        }
    }

    private fun startAsyncThumbnailer(point: PoiEntity?, ready: (Bitmap?) -> Unit) {
        var thumbBitmap: Bitmap? = null
        //val picFile : File? = null
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            if (drawScale > 0) {
                thumbBitmap = drawBitmap(point)
            }
        }.invokeOnCompletion {
            //ready(picFile?.path, thumbBitmap)
            ready(thumbBitmap)
        }

    }

    fun drawBitmap(point: PoiEntity?): Bitmap {
        val hairCrossPaint = Paint()
        hairCrossPaint.strokeWidth = 2.5f
        hairCrossPaint.color = Color.RED
        hairCrossPaint.style = Paint.Style.STROKE

        val thumbBitmap = createBitmap(size, size)
        val thumbCanvas = Canvas(thumbBitmap)
        thumbCanvas.drawColor(ContextCompat.getColor(context, android.R.color.transparent))
        thumbCanvas.scale(1f, -1f, 0.5f * size, 0.5f * size) // flip
        GpsViewModel.longitude.value.let { modelLongitude ->
            GpsViewModel.latitude.value.let { modelLatitude ->
                val hairCrossX =
                    (xOffset + margin + sizeMinusMargins * ((longitudeToX(modelLongitude) - minX) / drawScale)).toFloat()
                val hairCrossY =
                    (-yOffset + size - (margin + sizeMinusMargins * ((latitudeToY(modelLatitude) - minY) / drawScale))).toFloat()
                //Timber.i("endPoint.x: ${endPoint.x} endPoint.y: ${endPoint.y}")
                Timber.i("hairCrossX: $hairCrossX hairCrossY: $hairCrossY")
                thumbCanvas.drawLine(
                    size.toFloat(), hairCrossY,
                    0f, hairCrossY,
                    hairCrossPaint
                )
                thumbCanvas.drawLine(
                    hairCrossX, 0f,
                    hairCrossX, size.toFloat(),
                    hairCrossPaint
                )
            }
        }

        val textPaint = TextPaint()
        textPaint.color = ContextCompat.getColor(context, R.color.design_default_color_primary)
        textPaint.isAntiAlias = true
        textPaint.strokeWidth = 2f
        textPaint.textSize = textSizeDefault

        val routePointerPaint = Paint()
        routePointerPaint.color = ContextCompat.getColor(context, android.R.color.holo_red_dark)
        routePointerPaint.isAntiAlias = true
        routePointerPaint.strokeWidth = context.resources.getDimension(R.dimen.thumbLineWidth) * 6.0f
        routePointerPaint.style = Paint.Style.STROKE
        routePointerPaint.strokeJoin = Paint.Join.ROUND
        routePointerPaint.strokeCap = Paint.Cap.ROUND
        point?.let {llh ->
            val drawX =
                (xOffset + margin + sizeMinusMargins * ((llh.mercatorXY[0] - minX) / drawScale)).toFloat()
            val drawY =
                (-yOffset + size - (margin + sizeMinusMargins * ((llh.mercatorXY[1] - minY) / drawScale))).toFloat()
            thumbCanvas.drawPoint(drawX, drawY, routePointerPaint)
        }

        thumbCanvas.scale(1f, -1f, 0.5f * size, 0.5f * size) // flip reset
        return thumbBitmap
    }

}

