package com.almica.ramani.routes

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber
import java.io.InputStream

data class DataPoint(
    val distanceKm: Float,
    val elevationMeters: Float
)

object KmlParser {
    fun parseInputStream(inputStream: InputStream): List<DataPoint> {
        val points = mutableListOf<DataPoint>()
        val parser = Xml.newPullParser()

        try {
            parser.setInput(inputStream, null)
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "coordinates") {
                    val coordString = parser.nextText()
                    points.addAll(processCoordinates(coordString))
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream.close()
        }
        return points
    }

    private fun processCoordinates(coordString: String): List<DataPoint> {
        val points = mutableListOf<DataPoint>()
        // KML Koordinaten sind oft durch Leerzeichen oder Zeilenumbrüche getrennt
        val lines = coordString.trim().split("\\s+".toRegex())

        var totalDistance = 0f
        var lastLat: Double? = null
        var lastLon: Double? = null

        for (line in lines) {
            val parts = line.split(",")
            if (parts.size >= 3) {
                val lon = parts[0].toDoubleOrNull() ?: continue
                val lat = parts[1].toDoubleOrNull() ?: continue
                val elev = parts[2].toFloatOrNull() ?: continue

                if (lastLat != null && lastLon != null) {
                    totalDistance += calculateDistance(lastLat, lastLon, lat, lon)
                }

                points.add(DataPoint(totalDistance, elev))
                lastLat = lat
                lastLon = lon
            }
        }
        Timber.i("points.size: ${points.size} totalDistance: $totalDistance")
        return points
    }

    // Haversine-Formel zur Distanzberechnung zwischen zwei GPS-Punkten
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val r = 6371 // Erdradius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (r * c).toFloat()
    }
}
