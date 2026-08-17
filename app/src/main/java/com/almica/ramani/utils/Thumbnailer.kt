package com.almica.ramani.utils
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.text.TextPaint
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.text.isDigitsOnly
import com.almica.ramani.Const
import com.almica.ramani.Helpers
import com.almica.ramani.Helpers.Companion.latitudeToY
import com.almica.ramani.Helpers.Companion.longitudeToX
import com.almica.ramani.LatLngH
import com.almica.ramani.R
import com.almica.ramani.pois.PoiEntity
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.geometry.Point
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max

/**
 * The Thread that generates the Thumbnail of the Track with the given id.
 */
class Thumbnailer(
    private var context: Context,
    var name: String,
    val sites: java.util.ArrayList<LatLngH>?,
    val poiEntities: List<PoiEntity>,
    ready: (String?, Bitmap?) -> Unit
) {
    private var isBoundary = false
    private var area_w_h: IntArray? = null
    private var yOffset = 0.0
    private var xOffset = 0.0
    private var minY = 0.0
    private var minX = 0.0
    private var track: Track
    var numberOfLocations: Long = 0
    var trackBounds: LatLngBounds? = null
    var distance = 0.0
    private val drawPaint = Paint()
    private val bgPaint = Paint()
    private val hairCrossPaint = Paint()
    private val endDotdrawPaint = Paint()
    private val endDotBGPaint = Paint()
    private val centerDotdrawPaint = Paint()
    private val bgTextPaint = Paint()
    private var size: Int
    //private var textSizeMarker = 14f
    private var textSizeArea = 12f
    private var margin = 0
    private var sizeMinusMargins = 0

    var distanceProportion: Double = 0.0
    var drawScale: Double = 0.0

    var northWest: LatLng
    var southEast: LatLng
    private var textSizeDefault = 20f

    init {
        name = name.lowercase().replace(Const.KML_EXT, "").replace(Const.GPX_EXT, "").replace(
            Const.JPG_EXT, ""
        ) + Const.JPG_EXT
        track = Track(sites)
        size = context.resources.getDimension(R.dimen.thumbSize).toInt()
        val llboundsBuilder: LatLngBounds.Builder =
            LatLngBounds.Builder()
        if (sites != null) {
            distance = sites.getDistanceFromLllh()
            for (latLngH : LatLngH in sites)
                llboundsBuilder.include(latLngH.latLng)
        }
        val llbounds = llboundsBuilder.build()
        northWest = LatLng(llbounds.northeast.latitude, llbounds.southwest.longitude)
        southEast = LatLng(llbounds.southwest.latitude, llbounds.northeast.longitude)
        doInit()
        startAsyncThumbnailer {path, bmp ->
            ready(path, bmp)
        }
    }

    private fun doInit() {
        margin = ceil(context.resources.getDimension(R.dimen.thumbnailMargin).toDouble()).toInt()
        //(int) Math.ceil(context.getResources().getDimension(R.dimen.thumbLineWidth) * 3);
        sizeMinusMargins = size - 2 * margin

        //Log.w("myApp", "[#] GPSApplication.java - Bitmap Size = " + Size);
        if (sites != null && (sites.size > 2) && (distance >= 15)) {
            numberOfLocations = sites.size.toLong()

            // Setup Paints
            bgTextPaint.color = ContextCompat.getColor(context, R.color.white_transparent_)
            bgTextPaint.isAntiAlias = true
            bgTextPaint.strokeWidth = context.resources.getDimension(R.dimen.thumbLineWidth)
            //bgTextPaint.setStrokeWidth(2);
            bgTextPaint.style = Paint.Style.FILL
            bgTextPaint.strokeJoin = Paint.Join.ROUND
            bgTextPaint.strokeCap = Paint.Cap.ROUND

            drawPaint.color = ContextCompat.getColor(context, R.color.colorThumbnailLineColor)
            drawPaint.isAntiAlias = true
            drawPaint.strokeWidth = context.resources.getDimension(R.dimen.thumbLineWidth)
            //drawPaint.setStrokeWidth(2);
            drawPaint.style = Paint.Style.STROKE
            drawPaint.strokeJoin = Paint.Join.ROUND
            drawPaint.strokeCap = Paint.Cap.ROUND

            bgPaint.color = Color.BLACK
            bgPaint.isAntiAlias = true
            bgPaint.strokeWidth = context.resources.getDimension(R.dimen.thumbLineWidth) * 1.5f
            //BGPaint.setStrokeWidth(6);
            bgPaint.style = Paint.Style.STROKE
            bgPaint.strokeJoin = Paint.Join.ROUND
            bgPaint.strokeCap = Paint.Cap.ROUND

            hairCrossPaint.strokeWidth = 2.5f
            hairCrossPaint.color = Color.RED
            hairCrossPaint.style = Paint.Style.STROKE

            endDotdrawPaint.color = ContextCompat.getColor(
                context,
                R.color.design_default_color_primary
            ) //R.color.colorThumbnailLineColor));
            endDotdrawPaint.isAntiAlias = true
            endDotdrawPaint.strokeWidth =
                context.resources.getDimension(R.dimen.thumbLineWidth) * 2.5f
            endDotdrawPaint.style = Paint.Style.STROKE
            endDotdrawPaint.strokeJoin = Paint.Join.ROUND
            endDotdrawPaint.strokeCap = Paint.Cap.ROUND

            centerDotdrawPaint.color = ContextCompat.getColor(context, android.R.color.holo_red_dark)
            centerDotdrawPaint.isAntiAlias = true
            centerDotdrawPaint.strokeWidth = context.resources.getDimension(R.dimen.thumbLineWidth) * 6.0f
            centerDotdrawPaint.style = Paint.Style.STROKE
            centerDotdrawPaint.strokeJoin = Paint.Join.ROUND
            centerDotdrawPaint.strokeCap = Paint.Cap.ROUND

            endDotBGPaint.color = Color.BLACK
            endDotBGPaint.isAntiAlias = true
            endDotBGPaint.strokeWidth = context.resources.getDimension(R.dimen.thumbLineWidth) * 4.5f
            endDotBGPaint.style = Paint.Style.STROKE
            endDotBGPaint.strokeJoin = Paint.Join.ROUND
            endDotBGPaint.strokeCap = Paint.Cap.ROUND

            // Calculate the drawing scale
            val midLatitude: Double = (track.latitudeMax + track.latitudeMin) / 2
            val angleFromEquator = abs(midLatitude)
            var poiMinX = Double.MAX_VALUE
            var poiMinY = Double.MAX_VALUE
            var poiMaxX = Double.MIN_VALUE
            var poiMaxY = Double.MIN_VALUE

            //Rect trackBounds = new Rect()
            val llboundsBuilder: LatLngBounds.Builder =
                LatLngBounds.Builder()
            //Timber.i("poiEntities  ${poiEntities.size}")
            if (!poiEntities.isEmpty()) {
                if (isBoundary) { // show 3 cities
                    for (poiEntity in poiEntities) {
                        if (poiEntity.mercatorXY[0] < poiMinX) poiMinX = poiEntity.mercatorXY[0]
                        if (poiEntity.mercatorXY[1] < poiMinY) poiMinY = poiEntity.mercatorXY[1]
                        if (poiEntity.mercatorXY[0] > poiMaxX) poiMaxX = poiEntity.mercatorXY[0]
                        if (poiEntity.mercatorXY[1] > poiMaxY) poiMaxY = poiEntity.mercatorXY[1]
                        llboundsBuilder.include(LatLng(poiEntity.latitude, poiEntity.longitude))
                    }
                    llboundsBuilder
                        .include(LatLng(track.latitudeMax, track.longitudeMax))
                        .include(LatLng(track.latitudeMin, track.longitudeMin)
                        ).build()
                } else {
                    trackBounds = llboundsBuilder
                        .include(LatLng(poiEntities[0].latitude, poiEntities[0].longitude))
                        .include(LatLng(track.latitudeMax, track.longitudeMax))
                        .include(LatLng(track.latitudeMin, track.longitudeMin)
                        ).build()
                    poiMaxX = poiEntities[0].mercatorXY[0]
                    poiMaxY = poiEntities[0].mercatorXY[1]
                    poiMinX = poiMaxX
                    poiMinY = poiMaxY
                }
            } else trackBounds = llboundsBuilder
                .include(LatLng(track.latitudeMax, track.longitudeMax))
                .include(LatLng(track.latitudeMin, track.longitudeMin)
                ).build()

            distanceProportion = cos(Math.toRadians(angleFromEquator))

            minY =
                track.yMin.coerceAtMost(if ((!poiEntities.isEmpty())) poiMinY else Double.MAX_VALUE)
            minX =
                track.xMin.coerceAtMost(if ((!poiEntities.isEmpty())) poiMinX else Double.MAX_VALUE)
            val maxY =
                track.yMax.coerceAtLeast(if ((!poiEntities.isEmpty())) poiMaxY else Double.MIN_VALUE)
            val maxX =
                track.xMax.coerceAtLeast(if ((!poiEntities.isEmpty())) poiMaxX else Double.MIN_VALUE)

            drawScale = max(maxY - minY, maxX - minX)
            yOffset = sizeMinusMargins * (1 - (maxY - minY) / drawScale) / 2
            xOffset = sizeMinusMargins * (1 - (maxX - minX) / drawScale) / 2
        }
    }

    private fun startAsyncThumbnailer(ready: (String?, Bitmap?) -> Unit) {
        var thumbBitmap : Bitmap? = null
        //val picFile : File? = null
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            if (drawScale > 0) {
                thumbBitmap = drawBitmap()
            }
        }.invokeOnCompletion {
            //ready(picFile?.path, thumbBitmap)
            ready(null, thumbBitmap)
        }
    }

    @Throws(IOException::class)
    private fun writeThumbnail2File(thumbBitmap: Bitmap): File {
        val folderThumbnails = File(context.getExternalFilesDir(null), Const.THUMBNAILS)
        var b = folderThumbnails.mkdir()
        Timber.i("${folderThumbnails.path} mkdir $b")
        val file = File(folderThumbnails, name)
        if (file.exists()) {
            b = file.delete()
            Timber.i("${file.path} delete $b")
        } else {
            b = file.createNewFile()
            Timber.i("${file.path} create $b")
        }

        val out = FileOutputStream(file)
        thumbBitmap.compress( //isBoundary ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG,
            Bitmap.CompressFormat.JPEG, 60, out
        )
        out.flush()
        out.close()
        return file
    }

    fun drawBitmap(): Bitmap? {
        val path = Path()
        val pointList: List<Point> = track.getPoints() //new ArrayList<>();
        //val altitudeList: List<Double> = track.altitudes
        if (!pointList.isEmpty()) {
            val thumbBitmap = createBitmap(size, size)
            val thumbCanvas = Canvas(thumbBitmap)
            thumbCanvas.drawColor(ContextCompat.getColor(context, R.color.white))
            thumbCanvas.scale(1f, -1f, 0.5f * size, 0.5f * size) // flip
            for (i in pointList.indices) {
                if (i == 0) path.moveTo(
                    (xOffset + margin + sizeMinusMargins * ((pointList[i].x - minX) / drawScale)).toFloat(),
                    (-yOffset + size - (margin + sizeMinusMargins * ((pointList[i].y - minY) / drawScale))).toFloat()
                )
                else path.lineTo(
                    (xOffset + margin + sizeMinusMargins * ((pointList[i].x - minX) / drawScale)).toFloat(),
                    (-yOffset + size - (margin + sizeMinusMargins * ((pointList[i].y - minY) / drawScale))).toFloat()
                )
                thumbCanvas.drawPoint(
                    (xOffset + margin + sizeMinusMargins * ((pointList[i].x - minX) / drawScale)).toFloat(),
                    (-yOffset + size - (margin + sizeMinusMargins * ((pointList[i].y - minY) / drawScale))).toFloat(),
                    endDotBGPaint
                )
            }
            thumbCanvas.drawPath(path, bgPaint)
            val endPoint: Point = pointList[pointList.size - 1]
            thumbCanvas.drawPoint(
                (xOffset + margin + sizeMinusMargins * ((endPoint.x - minX) / drawScale)).toFloat(),
                (-yOffset + size - (margin + sizeMinusMargins * ((endPoint.y - minY) / drawScale))).toFloat(),
                endDotBGPaint
            )
            thumbCanvas.drawPath(path, drawPaint)
            thumbCanvas.drawPoint(
                (xOffset + margin + sizeMinusMargins * ((endPoint.x - minX) / drawScale)).toFloat(),
                (-yOffset + size - (margin + sizeMinusMargins * ((endPoint.y - minY) / drawScale))).toFloat(),
                endDotdrawPaint
            )

            val textPaint = TextPaint()
            textPaint.color = ContextCompat.getColor(context, R.color.design_default_color_primary)
            textPaint.isAntiAlias = true
            textPaint.strokeWidth = 2f
            textPaint.textSize = textSizeDefault

            for (poiEntity in poiEntities) {
                drawPoiMarker(
                    thumbCanvas,
                    poiEntity,
                    centerDotdrawPaint
                )
            }

            thumbCanvas.scale(1f, -1f, 0.5f * size, 0.5f * size) // flip reset
            if (!isBoundary) {
                drawRouteDist(thumbCanvas)
            }
            return thumbBitmap
        }
        return null
    }

    private fun drawRouteDist(thumbCanvas: Canvas) {
        val textPaint = TextPaint()
        textPaint.color = ContextCompat.getColor(context, R.color.design_default_color_primary)
        textPaint.isAntiAlias = true
        textPaint.strokeWidth = 2f
        textPaint.textSize = 32f
        val sDist: String = distance.formatDistM(true)
        val textBounds = Rect()
        textPaint.getTextBounds(sDist, 0, sDist.length, textBounds)
        thumbCanvas.drawRect(textBounds, bgTextPaint)
        thumbCanvas.drawText(sDist, (thumbCanvas.width - textBounds.width()).toFloat(),
            (thumbCanvas.height).toFloat(), textPaint)
    }

    /**
     * ■ (U+25A0): Black Square
     * □ (U+25A1): White Square
     * ▢ (U+25A2): White Square with Rounded Corners
     * ▣ (U+25A3): White Square Containing Black Small Square
     */
    private fun drawAreaDimensions(textPaint: Paint, thumbCanvas: Canvas) {
        if (area_w_h == null) return
        val sArea = String.format(
            Locale.getDefault(), "%s%dx%dkm", context.getString(R.string.uc_square),
            area_w_h!![0], area_w_h!![1]
        )
        val textBounds = Rect()
        textPaint.getTextBounds(sArea, 0, sArea.length, textBounds)
        //                    textPaint.setShader(new LinearGradient(0, textPaint.getTextSize() + textBounds.top, 0, textPaint.getTextSize(),
//                            ContextCompat.getColor(context, R.color.design_default_color_primary), Color.YELLOW, Shader.TileMode.MIRROR));
        //textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        val boundsW = textBounds.width()
        textBounds.offset((0.6f * (size - boundsW)).toInt(), 20)
        thumbCanvas.drawRect(textBounds, bgTextPaint)
        thumbCanvas.drawText(sArea, textBounds.left.toFloat(), 20f, textPaint)
        val endPoint: Point =
            track.getPoints()[track.getPoints().size - 1]
        val sNWlat = String.format(Locale.getDefault(), "%.3f°", northWest.latitude)
        val sNWlon = String.format(Locale.getDefault(), "%.3f°", northWest.longitude)
        textPaint.textSize = textSizeArea
        textPaint.getTextBounds(sNWlat, 0, sNWlat.length, textBounds)
        val xValue: Float =
            (10 + xOffset + margin + sizeMinusMargins * ((endPoint.x - minX) / drawScale)).toFloat()
        var yValue: Float =
            (-yOffset + 1.5 * textBounds.height() + (margin + sizeMinusMargins * ((endPoint.y - minY) / drawScale))).toFloat()
        yValue = max(yValue.toDouble(), textBounds.height().toDouble()).toFloat()
        val rect = Rect(
            xValue.toInt(),
            (yValue - 1.1 * textBounds.height()).toInt(),
            (xValue + textBounds.width()).toInt(),
            (yValue + 1.1 * textBounds.height()).toInt()
        )
        thumbCanvas.drawRect(rect, bgTextPaint)
        thumbCanvas.drawText(sNWlat, xValue, yValue, textPaint)
        thumbCanvas.drawText(sNWlon, xValue, yValue + textBounds.height(), textPaint)
    }

    private fun drawTrackCenter(thumbCanvas: Canvas) {
        thumbCanvas.drawPoint(
            (xOffset + margin + sizeMinusMargins * ((track.xYCenter.x - minX) / drawScale)).toFloat(),
            (-yOffset + size - (margin + sizeMinusMargins * ((track.xYCenter.y - minY) / drawScale))).toFloat(),
            centerDotdrawPaint
        )
    }

    private fun getCardinalDirection(bearing: Double): String {
        var bearing = bearing
        bearing = (bearing + 360) % 360
        val directions = context.resources.getStringArray(R.array.arrows_directions)
        val index = floor(((bearing - 22.5) % 360) / 45)
        val cardDirect = directions[(index + 1).toInt()]
        Timber.i("$bearing $cardDirect")
        return cardDirect
    }

    private fun drawPoiMarker(
        thumbCanvas: Canvas,
        poiEntity: PoiEntity,
        centerDotdrawPaint: Paint
    ) {
        val drawX = (xOffset + margin + sizeMinusMargins * ((poiEntity.mercatorXY[0] - minX) / drawScale)).toFloat()
        val drawY = (-yOffset + size - (margin + sizeMinusMargins * ((poiEntity.mercatorXY[1] - minY) / drawScale))).toFloat()
        thumbCanvas.drawPoint(drawX, drawY, centerDotdrawPaint)
    }

    private fun checkDefaultTileName(tileName: String): Boolean {
        if (!tileName.lowercase(Locale.getDefault()).startsWith("n")) return false
        if (!tileName.substring(3, 4).equals("e", ignoreCase = true) &&
            !tileName.substring(3, 4).equals("w", ignoreCase = true)
        ) return false
        if (!tileName.substring(1, 3).isDigitsOnly()) return false
        return tileName.substring(4, 7).isDigitsOnly()
    }
}
class Track(val arrayLlh: List<LatLngH>?) {
    private val points = ArrayList<Point>()
    val altitudes = ArrayList<Double>()

    init {
        if (arrayLlh != null) {
            for (coordinate in arrayLlh) {
                points.add(
                    Point(
                        longitudeToX(
                            coordinate.longitude
                        ), latitudeToY(coordinate.latitude)
                    )
                )
                altitudes.add(coordinate.altitude)
            }
        }
    }

    val distance: Double?
        get() = arrayLlh?.getDistanceFromLllh()

    val startLatLng: LatLng?
        get() = arrayLlh?.first()?.latLng

    val center: LatLng
        get() = LatLng(
            0.5 * (latitudeMax + latitudeMin),
            0.5 * (longitudeMax + longitudeMin)
        )
    val xYCenter: Point
        get() = Point(0.5 * (xMax + xMin), 0.5 * (yMax + yMin))
    val yMax: Double
        get() {
            var yMax = Double.MIN_VALUE
            for (p in points) if (p.y > yMax) yMax = p.y
            return yMax
        }
    val yMin: Double
        get() {
            var yMin = Double.MAX_VALUE
            for (p in points) if (p.y < yMin) yMin = p.y
            return yMin
        }
    val xMax: Double
        get() {
            var xMax = Double.MIN_VALUE
            for (p in points) if (p.x > xMax) xMax = p.x
            return xMax
        }
    val xMin: Double
        get() {
            var xMin = Double.MAX_VALUE
            for (p in points) if (p.x < xMin) xMin = p.x
            return xMin
        }

    val latitudeMax: Double
        get() {
            var latMax = Double.MIN_VALUE
            if (arrayLlh != null) {
                for (llh in arrayLlh) if (llh.latitude > latMax) latMax = llh.latitude
            }
            return latMax
        }
    val longitudeMax: Double
        get() {
            var lonMax = Double.MIN_VALUE
            if (arrayLlh != null) {
                for (llh in arrayLlh) if (llh.longitude > lonMax) lonMax = llh.longitude
            }
            return lonMax
        }
    val latitudeMin: Double
        get() {
            var latMin = Double.MAX_VALUE
            if (arrayLlh != null) {
                for (llh in arrayLlh) if (llh.latitude < latMin) latMin = llh.latitude
            }
            return latMin
        }
    val longitudeMin: Double
        get() {
            var lonMin = Double.MAX_VALUE
            if (arrayLlh != null) {
                for (llh in arrayLlh) if (llh.longitude < lonMin) lonMin = llh.longitude
            }
            return lonMin
        }

    fun getPoints(): List<Point> {
        return points
    }
}
