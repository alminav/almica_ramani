package com.almica.ramani.googlemaps

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.net.Uri
import com.almica.ramani.Const
import com.almica.ramani.LatLngH
import com.almica.ramani.R
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.utils.ElevationResultsObject
import com.almica.ramani.utils.ManifestUtils
import com.almica.ramani.utils.RoutesObject
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.text.Charsets.UTF_8

object MapUtils {
    /**
     * Linearly interpolates (lerps) between two colors based on a given [fraction].
     * Used to create smooth gradients between a start and end color.
     *
     * @param startColor The ARGB color to start from (Int representation).
     * @param endColor The ARGB color to interpolate to (Int representation).
     * @param fraction A value between 0.0 and 1.0 representing interpolation progress.
     *                 0.0 = startColor, 1.0 = endColor, 0.5 = halfway blend.
     *
     * @return The interpolated color as a packed ARGB Int.
     */
    fun lerpColor(startColor: Int, endColor: Int, fraction: Float): Int {
        // Decompose startColor into alpha, red, green, blue (8-bit components)
        val startA = (startColor shr 24) and 0xff // alpha
        val startR = (startColor shr 16) and 0xff // red
        val startG = (startColor shr 8) and 0xff  // green
        val startB = startColor and 0xff          // blue

        // Decompose endColor into alpha, red, green, blue (8-bit components)
        val endA = (endColor shr 24) and 0xff
        val endR = (endColor shr 16) and 0xff
        val endG = (endColor shr 8) and 0xff
        val endB = endColor and 0xff

        // Linearly interpolate each channel using the formula:
        // result = start + ((end - start) * fraction)
        val a = (startA + ((endA - startA) * fraction)).toInt()
        val r = (startR + ((endR - startR) * fraction)).toInt()
        val g = (startG + ((endG - startG) * fraction)).toInt()
        val b = (startB + ((endB - startB) * fraction)).toInt()

        // Recombine the channels into a single ARGB Int:
        // (alpha << 24) | (red << 16) | (green << 8) | blue
        return (a and 0xff shl 24) or
                (r and 0xff shl 16) or
                (g and 0xff shl 8) or
                (b and 0xff)
    }

    private const val EARTH_RADIUS_KM = 6371.0  // Average radius of the Earth

    /**
     * Calculates the shortest distance between two points on Earth
     * using the Haversine formula.
     *
     * Haversine accounts for Earth's curvature.
     *
     * @param start starting coordinate (latitude, longitude)
     * @param end ending coordinate
     * @return distance in kilometers (Double)
     */
    fun calculateHaversineDistance(start: LatLng, end: LatLng): Double {
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLon = Math.toRadians(end.longitude - start.longitude)

        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)

        val a = sin(dLat / 2).pow(2.0) +
                sin(dLon / 2).pow(2.0) * cos(lat1) * cos(lat2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    fun getBearing(start: LatLng, end: LatLng): Float {
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon2 = Math.toRadians(end.longitude)

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        return Math.toDegrees(atan2(y, x)).toFloat()
    }

    fun lerpAngle(start: Float, end: Float, fraction: Float): Float {
        var delta = (end - start + 360) % 360
        if (delta > 180) delta -= 360
        return (start + delta * fraction + 360) % 360
    }

    /**
     * Cubic easing function for smooth animation transitions.
     *
     * Provides a gradual start (ease-in), fast middle, and gradual stop (ease-out),
     * which looks more natural than linear motion.
     *
     * @param t the normalized time or progress (range 0.0 to 1.0)
     * @return eased value also in range [0, 1]
     */
    fun easeInOutCubic(t: Float): Float {
        return if (t < 0.5f) {
            4 * t * t * t
        } else {
            1 - (-2 * t + 2).let { it * it * it } / 2
        }
    }

    // does not run on doogee 24dez2025
    fun fetchPlaceRequest(context: Context, placeId: String, result: (Place?) -> Unit) {
        Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: placeId: $placeId")
    // Specify the fields to return.
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
            Place.Field.GOOGLE_MAPS_URI,
        )
        // Construct a request object, passing the place ID and fields array.
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)
        val placesClient = Places.createClient(context)
        placesClient.fetchPlace(request)
            .addOnSuccessListener { response: FetchPlaceResponse ->
                val place = response.place

                val name = place.displayName
                val address = place.formattedAddress
                val location = place.location
                val gmsUri = place.googleMapsUri
                //val placeTypes = place.placeTypes
                //val iconMaskUrl = place.iconMaskUrl
                //val websiteUri = place.websiteUri

                Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: name: $name")
                Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: address: $address")
                Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: GOOGLE_MAPS_URI: $gmsUri")
                //Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: placeTypes: $placeTypes")
                //Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: iconMaskUrl: $iconMaskUrl")
                //Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: websiteUri: $websiteUri")
                if (location != null) {
                    Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: " +
                            "location: ${location.latitude} ${location.longitude}")
                    result(place)
                } else {
                    Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: " +
                            "location: = null")
                }
            }.addOnFailureListener { exception: Exception ->
                if (exception is ApiException) {
                    val message = context.getString(R.string.place_not_found, exception.message)
                    Timber.e( "${Thread.currentThread().stackTrace[2].lineNumber}: $message" +
                            " statusCode: ${exception.statusCode}")
                }
            }
    }

    //maps.googleapis.com/maps/api/elevation/json?locations=enc:gfo}EtohhUxD@bAxJmGF&key=AIzaSyD3i7HcguIVj2fWH9H_syOc2zjQM6n2zLc
    // locations=40.714728,-73.998672
    fun gmsElevationService(context: Context, locations: String,
                            finished: (lllh: ArrayList<LatLngH>) -> Unit) {
        val apiKey = ManifestUtils.getApiKeyFromManifest(context)

        val uri = Uri.Builder().scheme("https")
            .authority("maps.googleapis.com")
            .appendPath("maps")
            .appendPath("api")
            .appendPath("elevation")
            .appendPath("json")
            .appendQueryParameter("locations", locations)
            .appendQueryParameter("key", apiKey)
        val elevationUrl = uri.build()
        Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: elevationUrl: $elevationUrl")
        val lllh = mutableListOf<LatLngH>()
        val job = CoroutineScope(Dispatchers.IO).launch {
            try {
                //shows something in the UI - progressBar
                Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: CoroutineScope")
                withContext(Dispatchers.IO) {
                    Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: withContext")
/*
                    val tempFile = File(context.cacheDir, "${name}_elevation${Const.TXT_EXT}")
                    Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: tempFile: ${tempFile.path}")
                    val bytesCount = downloadFile(elevationUrl.toString(), tempFile)
                    Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: ${tempFile.path} bytesCount: $bytesCount")
                    val inputStream = tempFile.inputStream()
                    val size = inputStream.available()
                    val buffer = ByteArray(size)
                    inputStream.read(buffer)
                    inputStream.close()
 */
                    val jsonBuffer = getUrlContent(elevationUrl.toString())
                    Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: jsonBuffer: ${jsonBuffer.size}")
                    val json = String(jsonBuffer, charset = UTF_8)
                    val gson = Gson()
                    val eleData = gson.fromJson(json, ElevationResultsObject::class.java)
                    if (eleData.isNotNull()) {
                        val elevationResults = eleData.elevationResults
                        Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: elevationResults: ${elevationResults?.size} ")
                        elevationResults?.forEach { elevationResult ->
                            val location = elevationResult.location
                            if (location != null) {
                                lllh.add(
                                    LatLngH(
                                        location.latitude ?: 0.0,
                                        location.longitude ?: 0.0,
                                        elevationResult.elevation ?: 0.0
                                    )
                                )
                            }
                        }
                    }
                }
                finished(lllh as ArrayList)
            } catch (e: IOException) {
                finished(arrayListOf())
                e.message?.let {
                    Timber.e("${Thread.currentThread().stackTrace[2].lineNumber} $it")
                }
            }
        }
    }
    /*
        https://maps.googleapis.com/maps/api/directions/json?origin=52.32531,10.37146&destination=52.33621,10.32585&mode=bicycling&key=AIzaSyD3i7HcguIVj2fWH9H_syOc2zjQM6n2zLc
        driving (default) indicates standard driving directions or distance using the road network.
        walking requests walking directions or distance via pedestrian paths & sidewalks (where available).
        bicycling requests bicycling directions or distance via bicycle paths & preferred streets (where available).
     */

    fun gmsDirectionsService(context: Context, start: LatLng, stop: LatLng, mode: String, alternatives: Boolean,
                             finished: (lllh: List<LatLngH>, name: String, success: Boolean) -> Unit) {
        val apiKey = ManifestUtils.getApiKeyFromManifest(context)
        val timeFormat =
            SimpleDateFormat(Const.TIME_PATTERN_LONG, Locale.getDefault())
        val name = Const.GMS_TAG + "." + timeFormat.format(Date())
        val uri = Uri.Builder().scheme("https")
            .authority("maps.googleapis.com")
            .appendPath("maps")
            .appendPath("api")
            .appendPath("directions")
            .appendPath("json")
            .appendQueryParameter("origin", "${start.latitude},${start.longitude}")
            .appendQueryParameter("destination", "${stop.latitude},${stop.longitude}")
            .appendQueryParameter("mode", mode)
            .appendQueryParameter("alternatives", alternatives.toString())
            .appendQueryParameter("key", apiKey)
        val directionsUrl = uri.build()
        Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: directionsUrl: $directionsUrl")
        val job = CoroutineScope(Dispatchers.IO).launch {
            try {
                //shows something in the UI - progressBar
                Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: CoroutineScope")
                withContext(Dispatchers.IO) {
                    Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: withContext")
                    val tempFile = File(context.cacheDir, "$name${Const.TXT_EXT}")
                    Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: tempFile: ${tempFile.path}")
                    val bytesCount = downloadFile(directionsUrl.toString(), tempFile)
                    //tempFile.writeBytes(bytes)
                    Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: ${tempFile.path} bytesCount: $bytesCount")
                    val inputStream = tempFile.inputStream()
                    val size = inputStream.available()
                    val buffer = ByteArray(size)
                    inputStream.read(buffer)
                    inputStream.close()
                    val json = String(buffer, charset = UTF_8)
                    val gson = Gson()

                    val routeData = gson.fromJson(json, RoutesObject::class.java)
                    val routes = routeData.routes
                    Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: routeData.routes size: ${routes?.size}")
                    if (!routes.isNullOrEmpty()) {
                        if (routes.size > 1) {
                            val encodedPolyline0 = routes[0].overview_polyline?.points
                            var lllhResult0: List<LatLngH>
                            gmsElevationService(context, "enc:${encodedPolyline0}") { lllh0 ->
                                lllhResult0 = lllh0
                                val encodedPolyline1 = routes[1].overview_polyline?.points
                                var lllhResult1: List<LatLngH>
                                gmsElevationService(context, "enc:${encodedPolyline1}") { lllh1 ->
                                    lllhResult1 = lllh1.reversed()
                                    val lllhResult = List(lllhResult0.size + lllhResult1.size) { i ->
                                        if (i < lllhResult0.size)
                                            lllhResult0[i] else lllhResult1[i - lllhResult0.size]
                                    }
                                    Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: lllhResult size: ${lllhResult.size}")
                                    finished(lllhResult, name, true)
                                }
                            }
                        } else {
                            val encodedPolyline0 = routes[0].overview_polyline?.points
                            gmsElevationService(context, "enc:${encodedPolyline0}") { lllh ->
                                Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: lllh size: ${lllh.size}")
                                finished(lllh, name, true)
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                finished(listOf(), "", false)
                e.message?.let {
                    Timber.e("${Thread.currentThread().stackTrace[2].lineNumber} $it")
                }
            }
        }
    }

    /*
    https://developers.google.com/maps/documentation/places/web-service/place-details?hl=de#GetPlaceRequest
        addressComponents
        addressDescriptor*
        adrFormatAddress
        formattedAddress
        location
        plusCode
        postalAddress
        shortFormattedAddress
        types
        viewport
     */
    // alternative web-service method for fetchPlaceRequest Api
    // works on doogee
    fun downloadPoiInfo(
        context: Context,
        name: String,
        latLng: LatLng,
        placeId: String,
        result: (PoiInfo?) -> Unit
    ) {
        val apiKey = ManifestUtils.getApiKeyFromManifest(context)
        //val placesUrl = "https://places.googleapis.com/v1/places/${poi.placeId}?fields=id,displayName,addressDescriptor&${apiKey}"
        val uri = Uri.Builder().scheme("https")
            .authority("places.googleapis.com")
            .appendPath("v1")
            .appendPath("places")
            .appendPath(placeId)
            .appendQueryParameter("fields", "id,displayName,location,formattedAddress,photos")
            .appendQueryParameter("key", apiKey)
        val placesUrl = uri.build()
        Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: placesUrl: $placesUrl")
        val job = CoroutineScope(Dispatchers.IO).launch {
            try {
                //shows something in the UI - progressBar
                Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: CoroutineScope")
                withContext(Dispatchers.IO) {
                    Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: withContext")
                    val tempFile = File(context.cacheDir, "$placeId${Const.TXT_EXT}")
                    val bytesCount = downloadFile(placesUrl.toString(), tempFile)
                    //tempFile.writeBytes(bytes)
                    Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: ${tempFile.path} bytesCount: $bytesCount")
                    val inputStream = tempFile.inputStream()
                    val size = inputStream.available()
                    val buffer = ByteArray(size)
                    inputStream.read(buffer)
                    inputStream.close()
                    val json = String(buffer, charset = UTF_8)
                    val gson = Gson()
                    val data = gson.fromJson(json, PlaceObject::class.java)
                    if (data != null) {
                        Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: formattedAddress: ${data.formattedAddress}")
                        // To get requestData
                        val photos = data.photos
                        photos?.forEach { photo ->
                            Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: ${photo.googleMapsUri}")
                        }
                        result(
                            PoiInfo(
                                name,
                                latLng,
                                data.formattedAddress ?: "",
                                data.photos?.getOrNull(0)?.googleMapsUri ?: ""
                            )
                        )
                    } else {
                        result(null)
                    }
                }
            } catch (e: IOException) {
                e.message?.let {
                    Timber.e("${Thread.currentThread().stackTrace[2].lineNumber} $it")
                }
            }
        }
    }
    data class PlaceObject(
        @SerializedName("googleMapsUri") val googleMapsUri: String? = null,
        @SerializedName("formattedAddress") val formattedAddress: String? = null,
        @SerializedName("photos") val photos: List<PhotoObject>? = null
    )

    data class PhotoObject(
        @SerializedName("googleMapsUri") val googleMapsUri: String? = null,
    )

    data class PoiInfo(val name: String, val latLng: LatLng, val formattedAddress: String, val googleMapsUri: String)

    // // works on doogee WITH connection.connectTimeout = 700 24sez2024
    private fun downloadFile(urlString: String, tempFile: File): Long {
        Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: tempFile: ${tempFile.path}")
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 700 // important for doogee
        Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: connection.connectTimeout: ${connection.connectTimeout}")
        Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: launched connection.connect")
        connection.connect()
        Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: finished connection.connect")

        var total: Long = 0
        try {
            connection.inputStream.use { input ->
                val bufferedInput = BufferedInputStream(input, 8192)
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(1024)
                    var read: Int = bufferedInput.read(buffer)
                    while (read != -1) {
                        output.write(buffer, 0, read)
                        total += read
                        read = bufferedInput.read(buffer)
                    }
                    output.flush()
                }
            }
        } finally {
            connection.disconnect()
        }
        Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: bytes: $total")
        return total
    }

    private fun getUrlContent(urlString: String): ByteArray {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 700 // important for doogee
        connection.connect()

        try {
            connection.inputStream.use { input ->
                val bufferedInput = BufferedInputStream(input, 8192)
                ByteArrayOutputStream().use { output ->
                    val data = ByteArray(1024)
                    var count: Int
                    while (bufferedInput.read(data).also { count = it } != -1) {
                        output.write(data, 0, count)
                    }
                    output.flush()
                    return output.toByteArray()
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}