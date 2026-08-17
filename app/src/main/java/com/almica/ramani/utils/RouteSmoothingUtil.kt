package com.almica.ramani.utils

import com.almica.ramani.Helpers
import com.almica.ramani.LatLngH
import timber.log.Timber
import java.util.PriorityQueue
import kotlin.jvm.JvmName
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.sinh
import kotlin.math.sqrt

object RouteSmoothingUtil {
    private const val DEFAULT_ALPHA = 0.2f // Lower = smoother but more lag
    private const val ALTITUDE_THRESHOLD_METERS = 3.0 // Filters out minor GPS noise

    /**
     * Smoothes a full list of coordinates using a Moving Average.
     */
    fun smoothRoute(points: List<LatLngH>): List<LatLngH> {
        if (points.isEmpty()) return emptyList()

        val smoothedList = mutableListOf<LatLngH>()
        var currentPoint = points.first()
        smoothedList.add(currentPoint)

        for (i in 1 until points.size) {
            val nextPoint = points[i]
            currentPoint = smoothPoint(currentPoint, nextPoint, DEFAULT_ALPHA)
            smoothedList.add(currentPoint)
        }
        return smoothedList
    }

    /**
     * Smoothes a real-time incoming coordinate against the last known valid point.
     */
    fun smoothPoint(last: LatLngH, next: LatLngH, alpha: Float = DEFAULT_ALPHA): LatLngH {
        val smoothLat = last.latitude + alpha * (next.latitude - last.latitude)
        val smoothLng = last.longitude + alpha * (next.longitude - last.longitude)

        // Altitude specific filtering: ignore changes below the threshold
        val altDiff = abs(next.altitude - last.altitude)
        val smoothAlt = if (altDiff > ALTITUDE_THRESHOLD_METERS) {
            last.altitude + alpha * (next.altitude - last.altitude)
        } else {
            last.altitude // Keep old altitude to prevent jitter while stationary
        }

        return LatLngH(smoothLat, smoothLng, smoothAlt, next.time)
    }

    //google search kotlin douglas peucker algorithm with number of points instead of epsilon:
    fun List<LatLngH>.toMercatorPoints(): List<Point> = map { latLngH ->
        Point(
            x = Helpers.getMercatorX(latLngH.longitude),
            y = Helpers.getMercatorY(latLngH.latitude),
            z = latLngH.altitude,
            time = latLngH.time
        )
    }

    /**
     * example targetCount = 30, 30 is the minimum, a 40 km distance route will become 40 sections
     */
    fun List<LatLngH>.simplifyToTargetCount(targetCount: Int): List<LatLngH> {
        val routeDistance = this.getDistanceFromLllh()
        val modifiedTargetCount = targetCount.coerceAtLeast((0.001 * routeDistance).toInt())
        Timber.i("modifiedTargetCount: $modifiedTargetCount")
        return this.toMercatorPoints().simplifyToTargetCount(modifiedTargetCount).toLllh()
    }

    fun List<Point>.toLllh(): List<LatLngH> = map { webMercatorToLatLng(it.x, it.y, it.z, it.time) }

    fun webMercatorToLatLng(x: Double, y: Double, z: Double, time: Long): LatLngH {
        val earthRadius = 6378137.0

        val longitude = (x / earthRadius) * (180.0 / Math.PI)
        val latitude = atan(sinh(y / earthRadius)) * (180.0 / Math.PI)

        return LatLngH(latitude, longitude, z, time)
    }

    data class Point(val x: Double, val y: Double, val z: Double, val time: Long)

    /**
     * Simplifies a polyline to a target number of coordinates using a
     * priority-queue based variant of the Ramer-Douglas-Peucker algorithm.
     */
    @JvmName("simplifyPointsToTargetCount")
    fun List<Point>.simplifyToTargetCount(targetCount: Int): List<Point> {
        // Edge cases where reduction isn't possible or necessary
        if (this.size <= targetCount) return this
        if (targetCount <= 2) return listOf(this.first(), this.last())

        // Data class to track sub-segments inside the Priority Queue
        data class Segment(val startIndex: Int, val endIndex: Int) {
            var splitIndex: Int = -1
            var maxDistance: Double = -1.0

            init {
                calculateMaxDistance()
            }

            private fun calculateMaxDistance() {
                if (endIndex - startIndex <= 1) return

                val startPoint = this@simplifyToTargetCount[startIndex]
                val endPoint = this@simplifyToTargetCount[endIndex]

                val lineLengthSq = (endPoint.x - startPoint.x) * (endPoint.x - startPoint.x) +
                        (endPoint.y - startPoint.y) * (endPoint.y - startPoint.y)

                for (i in (startIndex + 1) until endIndex) {
                    val point = this@simplifyToTargetCount[i]

                    // Calculate perpendicular distance
                    val distance = if (lineLengthSq == 0.0) {
                        // Start and end points are identical
                        val dx = point.x - startPoint.x
                        val dy = point.y - startPoint.y
                        sqrt(dx * dx + dy * dy)
                    } else {
                        val numerator = abs(
                            (endPoint.x - startPoint.x) * (startPoint.y - point.y) -
                                    (startPoint.x - point.x) * (endPoint.y - startPoint.y)
                        )
                        numerator / sqrt(lineLengthSq)
                    }

                    if (distance > maxDistance) {
                        maxDistance = distance
                        splitIndex = i
                    }
                }
            }
        }

        // Initialize Max-Heap prioritizing segments with the largest geometric error
        val maxHeap = PriorityQueue<Segment> { a, b -> b.maxDistance.compareTo(a.maxDistance) }

        // Tracks the indices of points we choose to keep. Start and End are kept automatically.
        val keptIndices = sortedSetOf(0, this.lastIndex)

        // Push the initial full polyline segment
        val initialSegment = Segment(0, this.lastIndex)
        if (initialSegment.splitIndex != -1) {
            maxHeap.add(initialSegment)
        }

        // Keep splitting segments until we reach the exact target number of coordinates
        while (keptIndices.size < targetCount && maxHeap.isNotEmpty()) {
            val segment = maxHeap.poll()

            // If the segment has no valid split point, we cannot split further
            if (segment != null) {
                if (segment.splitIndex == -1 || segment.maxDistance <= 0.0) continue

                // Add the split point to our kept points list
                keptIndices.add(segment.splitIndex)

                // Generate left and right sub-segments from the split
                val leftSegment = Segment(segment.startIndex, segment.splitIndex)
                val rightSegment = Segment(segment.splitIndex, segment.endIndex)

                if (leftSegment.splitIndex != -1) maxHeap.add(leftSegment)
                if (rightSegment.splitIndex != -1) maxHeap.add(rightSegment)
            }
        }

        // Map the sorted kept indices back to their original Point objects
        return keptIndices.map { this[it] }
    }
    /**
     * Once you've converted your latitude/longitude coordinates to Mercator coordinates,
     * they are on a Euclidean plane, where you can use the Pythagorean theorem.
     * Specifically, if your Mercator coordinates are (x1, y1) and (x2, y2), the distance is:
     * sqrt((x2-x1)^2 + (y2-y1)^2)
     * The distance between two points on a Mercator projection map can be approximated using the Pythagorean theorem
     * The Pythagorean method is suitable for short distances in a local area
     */
}