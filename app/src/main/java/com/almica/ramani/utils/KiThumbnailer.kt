package com.almica.ramani.utils
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.withRotation
import com.almica.ramani.charts.interpolateColor
import com.almica.ramani.pois.PoiEntity
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import timber.log.Timber
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * 27mai2026 KI generated route Thumbnailer
 */
object KiThumbnailer {
    data class GPSPoint(val lat: Double, val lng: Double, val altitude: Double)
    data class PixelPoint(val x: Float, val y: Float)

    // Converts GPS to a normalized 0.0 to 1.0 system
    fun projectMercator(point: GPSPoint): PixelPoint {
        val x = (point.lng + 180.0) / 360.0
        val radLat = Math.toRadians(point.lat)
        val mercatorY = ln(tan(PI / 4.0 + radLat / 2.0))
        val y = (1.0 - (mercatorY / PI)) / 2.0
        return PixelPoint(x.toFloat(), y.toFloat())
    }

    fun transformPointsToCanvas(
        gpsRoute: List<GPSPoint>,
        canvasWidth: Int,
        canvasHeight: Int,
        padding: Float = 40f
    ): List<PixelPoint> {
        if (gpsRoute.isEmpty()) return emptyList()

        // 1. Project all points
        val projectedPoints = gpsRoute.map { projectMercator(it) }

        // 2. Determine extreme values (Bounding Box)
        val minX = projectedPoints.minOf { it.x }
        val maxX = projectedPoints.maxOf { it.x }
        val minY = projectedPoints.minOf { it.y }
        val maxY = projectedPoints.maxOf { it.y }

        val routeWidth = maxX - minX
        val routeHeight = maxY - minY

        // 3. Calculate scaling factor (maintain aspect ratio)
        val usableWidth = canvasWidth - (padding * 2)
        val usableHeight = canvasHeight - (padding * 2)

        val scaleX = if (routeWidth > 0) usableWidth / routeWidth else 1f
        val scaleY = if (routeHeight > 0) usableHeight / routeHeight else 1f
        val scale = minOf(scaleX, scaleY)

        // 4. Calculate centering offsets
        val offsetX = padding + (usableWidth - (routeWidth * scale)) / 2f
        val offsetY = padding + (usableHeight - (routeHeight * scale)) / 2f

        // 5. Finally transform points to canvas size
        return projectedPoints.map { point ->
            PixelPoint(
                x = (point.x - minX) * scale + offsetX,
                y = (point.y - minY) * scale + offsetY
            )
        }
    }

    fun haversineDistance(p1: GPSPoint, p2: GPSPoint): Double {
        val r = 6371000.0 // Earth radius in meters
        val lat1Rad = p1.lat * PI / 180.0
        val lat2Rad = p2.lat * PI / 180.0
        val dLat = (p2.lat - p1.lat) * PI / 180.0
        val dLng = (p2.lng - p1.lng) * PI / 180.0

        val a = sin(dLat / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun drawScalebar(
        canvas: Canvas,
        width: Int,
        height: Int,
        gpsRoute: List<GPSPoint>,
        padding: Float = 40f
    ) {
        if (gpsRoute.isEmpty()) return

        // 1. Get bounding box to find scaling
        val projectedPoints = gpsRoute.map { projectMercator(it) }
        val minX = projectedPoints.minOf { it.x }
        val maxX = projectedPoints.maxOf { it.x }
        val minY = projectedPoints.minOf { it.y }
        val maxY = projectedPoints.maxOf { it.y }

        val routeWidth = maxX - minX
        val routeHeight = maxY - minY
        val usableWidth = width - (padding * 2)
        val usableHeight = height - (padding * 2)

        val scaleX = if (routeWidth > 0) usableWidth / routeWidth else 1f
        val scaleY = if (routeHeight > 0) usableHeight / routeHeight else 1f
        val scale = minOf(scaleX, scaleY)

        // 2. Calculate pixels per meter at the center latitude
        val minLat = gpsRoute.minOf { it.lat }
        val maxLat = gpsRoute.maxOf { it.lat }
        val centerLat = (minLat + maxLat) / 2.0
        val centerLng = gpsRoute.map { it.lng }.average()

        val p1 = GPSPoint(centerLat, centerLng, 0.0)
        val p2 = GPSPoint(centerLat, centerLng + 0.001, 0.0) // Small offset in longitude
        val distMeters = haversineDistance(p1, p2)

        val proj1 = projectMercator(p1)
        val proj2 = projectMercator(p2)
        val distPx = (proj2.x - proj1.x) * scale

        val pixelsPerMeter = distPx / distMeters

        // 3. Choose a nice distance for the scale bar
        // We want the bar to be roughly 1/4 to 1/3 of the width
        val targetBarWidthPx = width / 4f
        val rawDistance = targetBarWidthPx / pixelsPerMeter

        // Round rawDistance to something "nice" (1, 2, 5 * 10^n)
        val magnitude = 10.0.pow(floor(log10(rawDistance)))
        val firstDigit = rawDistance / magnitude
        val niceDistance = when {
            firstDigit < 1.5 -> 1.0 * magnitude
            firstDigit < 3.5 -> 2.0 * magnitude
            firstDigit < 7.5 -> 5.0 * magnitude
            else -> 10.0 * magnitude
        }

        val finalBarWidthPx = (niceDistance * pixelsPerMeter).toFloat()

        // 4. Draw the bar
        val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 4f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val startX: Float = 0.25f * padding // width - padding - finalBarWidthPx
        val endX: Float = 0.25f * padding + finalBarWidthPx //width - padding
        val y = padding //height - padding / 2f

        canvas.drawLine(startX, y, endX, y, paint)
        canvas.drawLine(startX, y - 10f, startX, y + 10f, paint)
        canvas.drawLine(endX, y - 10f, endX, y + 10f, paint)

        val label = if (niceDistance >= 1000) {
            "${(niceDistance / 1000).toInt()} km"
        } else {
            "${niceDistance.toInt()} m"
        }
        canvas.drawText(label, (startX + endX) / 2, y - 15f, textPaint)
    }

    fun drawRouteThumbnail(
        canvas: Canvas,
        width: Int,
        height: Int,
        gpsRoute: List<GPSPoint>,
        currentGPSPoint: GPSPoint,
        posBmp: Bitmap,
        routePoint: Int,
        currentBearing: Float,
        poiEntity: PoiEntity?,
        poiBmp: Bitmap?
    ) {
        if (gpsRoute.size < 2) return
        //Timber.i("currentGPSPoint $currentGPSPoint")

        // Combine current point and route to ensure the transformation accounts for all points
        val helperList = mutableListOf(currentGPSPoint)
        helperList.addAll(gpsRoute)
        poiEntity?.let { helperList.add(GPSPoint(it.latitude, poiEntity.longitude, poiEntity.altitude)) }
        //Timber.i("helperList: ${helperList.size}")
        val transformedPoints = transformPointsToCanvas(helperList, width, height)

        val hasPoi = poiEntity != null
        val currentCanvasPoint = transformedPoints[0]

        // Sub-list indices: index 0 is currentCanvasPoint.
        // The route follows, and if POI exists, it is the last element.
        val canvasPoints = transformedPoints.subList(1, if (hasPoi) transformedPoints.size - 1 else transformedPoints.size)
        val poiCanvasPoint = if (hasPoi) transformedPoints.last() else null

        val startMarkerPaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val endMarkerPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        addLineWithGradient(canvas, canvasPoints, gpsRoute)
        // 1. Draw route as path
        /*
        // Define Pens/Paints
        val routePaint = Paint().apply {
            color = Color.BLUE
            strokeWidth = 5f
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        val path = Path().apply {
            val start = canvasPoints.first()
            moveTo(start.x, start.y)
            for (i in 1 until canvasPoints.size) {
                lineTo(canvasPoints[i].x, canvasPoints[i].y)
            }
        }
        canvas.drawPath(path, routePaint)
        */

        // 2. Draw start marker (Green circle)
        val startPoint = canvasPoints.first()
        canvas.drawCircle(startPoint.x, startPoint.y, 16f, startMarkerPaint)
        canvas.drawCircle(startPoint.x, startPoint.y, 6f, Paint().apply { color = Color.WHITE })

        currentCanvasPoint.let {
            canvas.withRotation(degrees = currentBearing, pivotX = it.x, pivotY = it.y) {
                drawBitmap(
                    posBmp,
                    it.x - 0.5f * posBmp.width,
                    it.y - 0.5f * posBmp.height,
                    null // Filter/Paint
                )
            }
        }
        if (hasPoi && poiBmp != null) {
            poiCanvasPoint?.let {
                //Timber.i("draw poi ${poiEntity.name}")
                canvas.drawBitmap(
                    poiBmp,
                    it.x - 0.5f * poiBmp.width,
                    it.y - 0.5f * poiBmp.height,
                    null // Filter/Paint
                )
            }
        }

        // 3. Draw end marker (Red circle)
        val endPoint = canvasPoints[routePoint]
        canvas.drawCircle(endPoint.x, endPoint.y, 16f, endMarkerPaint)
        canvas.drawCircle(endPoint.x, endPoint.y, 6f, Paint().apply { color = Color.WHITE })
        drawScalebar(canvas, width, height, helperList)
    }

    private fun addLineWithGradient(
        canvas: Canvas,
        canvasPoints: List<PixelPoint>,
        gpsRoute: List<GPSPoint>
    ) {
        //Timber.i("canvasPoints: ${canvasPoints.size}")
        val linePaintBorder = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 9f
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
//        strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        // Draw Path Line
        if (canvasPoints.size > 1) {
            canvasPoints.forEachIndexed { index, _ ->
                if (index > 0) {
                    val dist = //sqrt((pointB.x - pointA.x).toDouble().pow(2) + (pointB.y - pointA.y).toDouble().pow(2))
                        SphericalUtil.computeDistanceBetween(LatLng(gpsRoute[index].lat, gpsRoute[index].lng),
                            LatLng(gpsRoute[index-1].lat, gpsRoute[index-1].lng))
                    var gradient = 0.0
                    val deltaH: Double =
                        gpsRoute[index].altitude - gpsRoute[index-1].altitude
                    if (dist > 0) gradient = 100 * deltaH / dist
                    val c = interpolateColor((0.1 * abs(gradient)).toFloat())

                    val linePaint = Paint().apply {
                        color = c
                        strokeWidth = 5f
                        style = Paint.Style.STROKE
                        strokeJoin = Paint.Join.ROUND
                        strokeCap = Paint.Cap.ROUND
                        isAntiAlias = true
                    }
                    val pointA = canvasPoints[index-1]
                    val pointB = canvasPoints[index]
                    canvas.drawLine(
                        pointA.x, pointA.y,
                        pointB.x, pointB.y,
                        linePaintBorder
                    )
                    canvas.drawLine(
                        pointA.x, pointA.y,
                        pointB.x, pointB.y,
                        linePaint
                    )
                }
            }
        }
    }

}