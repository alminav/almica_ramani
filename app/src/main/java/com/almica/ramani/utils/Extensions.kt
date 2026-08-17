package com.almica.ramani.utils

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Xml
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.almica.ramani.Const
import com.almica.ramani.LatLngH
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.BoundingBox
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.Locale.getDefault
import kotlin.math.abs
import kotlin.math.hypot

@Throws(IOException::class)
fun String.kmlString2Lllh(): List<LatLngH> {
    val kmlReader: StringReader
    val lllh = mutableListOf<LatLngH>()
    try {
        kmlReader = StringReader(this)
        val factory = XmlPullParserFactory.newInstance()
        val xpp = factory.newPullParser()
        xpp.setInput(kmlReader)

        var inCoordinates = false
        var inLineString = false
        val sb = java.lang.StringBuilder()
        var eventType = xpp.eventType
        do {
            if (eventType == XmlPullParser.START_TAG) {
                val startTagName = xpp.name
                if (startTagName == "coordinates") {
                    inCoordinates = true
                }
                if (startTagName == "LineString") {
                    inLineString = true
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                val endTagName = xpp.name
                if (endTagName == "coordinates") {
                    inCoordinates = false
                }
                if (endTagName == "LineString") {
                    inLineString = false
                }
            } else if (eventType == XmlPullParser.TEXT) {
                if (inCoordinates && inLineString) {
                    sb.append(xpp.text.replace("\n".toRegex(), " "))
                }
            }
            eventType = xpp.next()
        } while (eventType != XmlPullParser.END_DOCUMENT)
        kmlReader.close()

        val coordinates = sb.toString()

        val coordLines = coordinates.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        for (i in coordLines.indices) {
            //Log.i(logtag,i.toString() + " " + coordLines[i])
            if (coordLines[i].length > 3) {
                val llh = coordLines[i].split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                try {
                    val nLongitude = llh[0].toDouble()
                    val nLatitude = llh[1].toDouble()
                    if (llh.size > 2) lllh.add(
                        LatLngH(nLatitude, nLongitude, llh[2].toDouble()))
                    else
                        lllh.add(LatLngH(nLatitude, nLongitude))
                } catch (e: NumberFormatException) {
                    e.message?.let { Timber.e(it) }
                    Timber.e("parse error line $i  ${coordLines[i]}")
                }
            }
        }
    } catch (xppe: XmlPullParserException) {
        xppe.printStackTrace()
        Timber.e("readKml Exception reading KML data")
        // fall through
        return emptyList()
    }
    //Timber.i("$name lllh: ${lllh.size}")
    return lllh
}

fun List<LatLngH>.getDistanceFromLllh(): Double {
    var dist = 0.0
    for (i in 1 until this.size) dist += SphericalUtil.computeDistanceBetween(
        this[i - 1].latLng,
        this[i].latLng
    )
    return dist
}

fun List<LatLngH>.getCenter(): com.google.android.gms.maps.model.LatLng {
    val llboundsBuilder: LatLngBounds.Builder = LatLngBounds.Builder()
    for (latLngH: LatLngH in this)
        llboundsBuilder.include(latLngH.latLngGms)
    val bounds = llboundsBuilder.build()
    return bounds.center
}

fun List<LatLngH>.getGmsBounds(): LatLngBounds {
    val llboundsBuilder: LatLngBounds.Builder = LatLngBounds.Builder()
    for (latLngH: LatLngH in this)
        llboundsBuilder.include(latLngH.latLngGms)
    val bounds = llboundsBuilder.build()
    return bounds
}

fun List<LatLngH>.getMaplibreBounds(): org.maplibre.android.geometry.LatLngBounds {
    val llboundsBuilder: org.maplibre.android.geometry.LatLngBounds.Builder = org.maplibre.android.geometry.LatLngBounds.Builder()
    for (latLngH: LatLngH in this)
        llboundsBuilder.include(latLngH.latLngMapLibre)
    val bounds = llboundsBuilder.build()
    return bounds
}

/**
 * Converts a list of LatLngH to a MapLibre LineLayer and GeoJsonSource.
 * MUST be called on the UI thread as it instantiates MapLibre Source and Layer objects.
 */
fun List<LatLngH>.lllhToLineLayer(
    name: String?,
    border: Double
): Triple<LineLayer, GeoJsonSource, org.maplibre.android.geometry.LatLngBounds> {
    val llboundsBuilder: org.maplibre.android.geometry.LatLngBounds.Builder =
        org.maplibre.android.geometry.LatLngBounds.Builder()
    for (latLngH: LatLngH in this)
        llboundsBuilder.include(LatLng(latLngH.latitude, latLngH.longitude))
    val bounds = llboundsBuilder.build()
    val latAdjustment = ((bounds.northEast.latitude - bounds.southWest.latitude) * border)
    val lngAdjustment =
        ((bounds.northEast.longitude - bounds.southWest.longitude) * border)
    val regionsBoundsBuilder: org.maplibre.android.geometry.LatLngBounds.Builder =
        org.maplibre.android.geometry.LatLngBounds.Builder()
    regionsBoundsBuilder.include(
        LatLng(
            (bounds.northEast.latitude + latAdjustment).coerceIn(-90.0, 90.0),
            bounds.northEast.longitude + lngAdjustment
        )
    ).include(
        LatLng(
            (bounds.southWest.latitude - latAdjustment).coerceIn(-90.0, 90.0),
            bounds.southWest.longitude - lngAdjustment
        )
    )

    //"mapbox://styles/mapbox/streets-v12"
    val jsonString = this.createFeatureString(name, null, bounds)
    //getFeatureStringFromLll(lllh, name, null, bounds)
    val geoJsonSourceName = name ?: "geoJsonSourceName"
    val source = GeoJsonSource(geoJsonSourceName, jsonString)
    val lineLayer = LineLayer(name, geoJsonSourceName)
        .withProperties(
            PropertyFactory.lineColor(Color.RED),
            //PropertyFactory.lineOpacity(0.5f),
            //PropertyFactory.lineDasharray(arrayOf(1f, 2f)),
            PropertyFactory.lineWidth(4f)
        )
    return Triple(lineLayer, source, regionsBoundsBuilder.build())
}

fun List<LatLngH>.reduceWithTolerance(
    tolerance: Double
): List<LatLngH> {
    val n: Int = this.size

    // if a shape has 2 or fewer points it cannot be reduced
    if (tolerance <= 0 || n < 3) {
        return this
    }

    val marked = BooleanArray(n) //vertex indexes to keep will be marked as "true"
    for (i in 1 until n - 1) {
        marked[i] = false
    }

    // automatically add the first and last point to the returned shape
    marked[0] = true.also { marked[n - 1] = true }


    // the first and last points in the original shape are
    // used as the entry point to the algorithm.
    douglasPeuckerReduction(
        this,  // original shape
        marked,  // reduced shape
        tolerance,  // tolerance
        0,  // index of first point
        n - 1 // index of last point
    )


    // all done, return the reduced shape
    val newShape = mutableListOf<LatLngH>() // the new shape to return
    for (i in 0 until n) {
        if (marked[i]) {
            newShape.add(this[i])
        }
    }
    return newShape
}

fun List<LatLngH>.createFeatureString(name: String?,
                                           mapType: String?,
                                           bounds: org.maplibre.android.geometry.LatLngBounds): String {
    val textLat = String.format(Locale.ENGLISH, "%.4f", bounds.center.latitude)
    val textLon = String.format(Locale.ENGLISH, "%.4f", bounds.center.longitude)
    val jsonBuilder = java.lang.StringBuilder()
    jsonBuilder.append("{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[")
    val listIterator = this.listIterator()
    var dist = 0.0
    var prevPosition: com.google.android.gms.maps.model.LatLng? = null

    while (listIterator.hasNext()) {
        val position = listIterator.next()
        if (prevPosition != null)
            dist += SphericalUtil.computeDistanceBetween(position.latLngGms, prevPosition)

        jsonBuilder.append('[')
        jsonBuilder.append(position.longitude)
        jsonBuilder.append(',')
        jsonBuilder.append(position.latitude)
        jsonBuilder.append(',')
        jsonBuilder.append(position.altitude)
        jsonBuilder.append(']')
        if (listIterator.hasNext()) {
            jsonBuilder.append(',')
        }
        prevPosition = position.latLngGms
    }
    jsonBuilder.append("]}") // end of geometry
    jsonBuilder.append("\n")
    jsonBuilder.append(",\"properties\":{\"name\":\"").append(name).append("\"")
    if (mapType != null)
        jsonBuilder.append(",\"mapType\":\"").append(mapType).append("\"")
    jsonBuilder.append(",\"distance\":\"").append(
        dist.formatDistM(true)
    ).append("\"")
        .append(",\"latitude\":\"").append(textLat).append("\"")
        .append(",\"longitude\":\"").append(textLon).append("\"")
    jsonBuilder.append("}}") // end of feature

    return jsonBuilder.toString()
}

fun List<LatLngH>.getEquidistantPoints(interval: Double): List<LatLngH> {
    val newPoints = mutableListOf<LatLngH>()
    newPoints.add(this.first()) // Add start point
    var p1 = LatLngH(this[0].latitude, this[0].longitude)
    var j = 1
    var segmentDistance = 0.0
    var remainingDistance: Double
    while (segmentDistance < interval && j < this.size - 1) {
        val p2 = LatLngH(this[j].latitude, this[j].longitude)
        val h2 = this[j].altitude
        segmentDistance += SphericalUtil.computeDistanceBetween(p1.latLngGms, p2.latLngGms) // Use SphericalUtil or similar
        remainingDistance = interval - segmentDistance
        if (remainingDistance <= 0) {
            newPoints.add(LatLngH(p2.latitude, p2.longitude, h2))
            segmentDistance = 0.0
        }
        p1 = LatLngH(this[j].latitude, this[j].longitude)
        j++
    }

    // Add the end point if not already added
    if (newPoints.last() != this.last())
        newPoints.add(this.last())
    Timber.i( "newPoints: ${newPoints.size}")
    return newPoints
}

fun List<LatLngH>.lllhToKmlString(name: String?): String {
    val df = DecimalFormat("#0.00000")
    val dfele = DecimalFormat("#0")
    val symbols = DecimalFormatSymbols()
    symbols.decimalSeparator = '.'
    df.decimalFormatSymbols = symbols

    val serializer = Xml.newSerializer()
    val ns = "http://earth.google.com/kml/2.1"
    val xmlwriter = StringWriter()
    try {
        serializer.setOutput(xmlwriter)
        // Log.i("write2Kml", "startDocument");
        serializer.startDocument("utf-8", true)

        // Log.i("write2Kml", "setPrefix");
        serializer.setPrefix("", ns)
        //serializer.text("\r\n");
        serializer.startTag(ns, "kml")
        //serializer.text("\r\n");
        serializer.startTag(ns, "Placemark")
        //serializer.text("\r\n");
        serializer.startTag(ns, "Name")
        serializer.text(name) // "GH Kml");
        serializer.endTag(ns, "Name")
        //serializer.text("\r\n");
        serializer.startTag(ns, "LineString")
        //serializer.text("\r\n");
        serializer.startTag(ns, "coordinates")

        var ws: String

        //serializer.text("\r\n");
        Timber.i("route $name has ${this.size} points")
        for (i in this.indices) {
            val lat2: Double = this[i].latitude
            val lon2: Double = this[i].longitude
            ws = (df.format(lon2) + "," + df.format(lat2) + ","
                    + dfele.format(this[i].altitude)
                    + " ")
            serializer.text(ws)
            //serializer.text("\r\n");
        }

        serializer.endTag(ns, "coordinates")
        serializer.endTag(ns, "LineString")
        serializer.endTag(ns, "Placemark")

        serializer.endTag(ns, "kml")
        serializer.endDocument()
        xmlwriter.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return xmlwriter.toString()
}

private fun douglasPeuckerReduction(
    shape: List<LatLngH>,
    marked: BooleanArray,
    tolerance: Double,
    firstIdx: Int,
    lastIdx: Int) {
    if (lastIdx <= firstIdx + 1) {
        // overlapping indexes, just return
        return
    }


    // loop over the points between the first and last points
    // and find the point that is the farthest away
    var maxDistance = 0.0
    var indexFarthest = 0

    val firstPoint = shape[firstIdx]
    val lastPoint = shape[lastIdx]

    for (idx in firstIdx + 1 until lastIdx) {
        val point = shape[idx]

        val distance: Double =
            orthogonalDistance(point, firstPoint, lastPoint)

        // keep the point with the greatest distance
        if (distance > maxDistance) {
            maxDistance = distance
            indexFarthest = idx
        }
    }

    if (maxDistance > tolerance) {
        //The farthest point is outside the tolerance: it is marked and the algorithm continues.
        marked[indexFarthest] = true

        // reduce the shape between the starting point to newly found point
        douglasPeuckerReduction(
            shape,
            marked,
            tolerance,
            firstIdx,
            indexFarthest
        )

        // reduce the shape between the newly found point and the finishing point
        douglasPeuckerReduction(
            shape,
            marked,
            tolerance,
            indexFarthest,
            lastIdx
        )
    }

}

private fun orthogonalDistance(
    point: LatLngH,
    lineStart: LatLngH,
    lineEnd: LatLngH
): Double {
    val area: Double = abs(
        (1.0 * lineStart.latitude * 1e6 * lineEnd.longitude * 1e6 + 1.0 * lineEnd.latitude * 1e6 * point.longitude * 1e6
                + 1.0 * point.latitude * 1e6 * lineStart.longitude * 1e6 - 1.0 * lineEnd.latitude * 1e6 * lineStart.longitude * 1e6 - 1.0 * point.latitude * 1e6 * lineEnd.longitude * 1e6 - 1.0 * lineStart.latitude * 1e6 * point.longitude * 1e6) / 2.0
    )

    val bottom = hypot(
        lineStart.latitude * 1e6 - lineEnd.latitude * 1e6,
        lineStart.longitude * 1e6 - lineEnd.longitude * 1e6
    )

    return (area / bottom * 2.0)
}

fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager =
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val nw = connectivityManager.activeNetwork ?: return false
    val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
    return when {
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        //for other device how are able to connect with Ethernet
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        //for check internet over Bluetooth
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
        else -> false
    }
}


fun Double.formatAlti(bMetric: Boolean): String {
    if (java.lang.Double.isNaN(this)) return ""
    return if (bMetric) java.lang.String.format(
        Locale.ENGLISH,
        "%s%.0f%s",
        Const.UC_ELE_ARROW, this, "m"
    )
    else java.lang.String.format(
        Locale.ENGLISH,
        "%s%.0f%s", Const.UC_ELE_ARROW,
        Const.M_TO_FT * this, "ft"
    )
}

fun Double.formatDistM(bMetric: Boolean): String {
    var value = this
    var sUnit = "km"
    if (!bMetric) {
        value = Const.KM_TO_MILES * this
        sUnit = "mi"
    }

    return if (abs(value) < 1000) String.format(getDefault(), "%.0f%s", this, "m")
    else if (abs(value) < 10000) String.format(
        Locale.ENGLISH,
        "%.1f%s",
        value / 1000,
        sUnit
    )
    else if (abs(value) < 100000) String.format(
        Locale.ENGLISH,
        "%.1f%s",
        value / 1000,
        sUnit
    )
    else String.format(Locale.ENGLISH, "%.0f%s", value / 1000, sUnit)
}

fun Double.formatDistValueUnit(bMetric: Boolean): FormattedDistance {
    var value = this
    var sUnit = "km"
    if (!bMetric) {
        value = Const.KM_TO_MILES * this
        sUnit = "mi"
    }

    return if (abs(value) < 1000) FormattedDistance(String.format(getDefault(), "%.0f", this), "m")
    else if (abs(value) < 10000)
        FormattedDistance(String.format(Locale.ENGLISH, "%.1f",value / 1000),sUnit)
    else if (abs(value) < 100000)
        FormattedDistance(String.format(Locale.ENGLISH, "%.1f", value / 1000),    sUnit)
    else FormattedDistance(String.format(Locale.ENGLISH, "%.0f", value / 1000), sUnit)
}
fun Double.format(digits: Int) = "%.${digits}f".format(Locale.ENGLISH, this)
fun Float.format(digits: Int) = "%.${digits}f".format(Locale.ENGLISH,this)
fun BoundingBox.contains(geoPoint: com.google.android.gms.maps.model.LatLng) = geoPoint.latitude <= north() && geoPoint.latitude >= south()
        && geoPoint.longitude <= east() && geoPoint.longitude >= west()

fun Modifier.offsetYByPercent(percentage: Float) = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(
                x = 0, //(constraints.maxWidth * percentage).toInt(),
                y = (constraints.maxHeight * percentage).toInt()
            )
        }
    }
)
/**
 * Returns true or false if the object is null
 */
fun Any?.isNotNull() = this != null