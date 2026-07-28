package com.almica.ramani.utils

import android.content.Context
import android.util.Log
import com.almica.ramani.BuildConfig
import com.almica.ramani.Helpers
import com.almica.ramani.LatLngH
import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets

private const val logtag = "OrsRouting"
class OrsRouting(
    var postLllh: ArrayList<LatLng>, iVehicle: Int, iPreference: Int, alternateRoute: Boolean) {
    private val preference: String?
    private val alternatRoute: Boolean

    //String API_KEY = "5b3ce3597851110001cf624800f86f4d35dd452a923c73be6cd20943";
    var CYCLING_REGULAR: String = "cycling-regular"
    var DRIVING_CAR: String = "driving-car"
    var FOOT_WALKING: String = "foot-walking"
    var VEHICLES: Array<String> = arrayOf(FOOT_WALKING, CYCLING_REGULAR, DRIVING_CAR)
    var PREFERENCES: Array<String> = arrayOf(SHORTEST, FASTEST)
    var vehicle: String? = VEHICLES[iVehicle]

    /**
     *
     * @param postLllh contains 2 items, start + end point
     * @param iVehicle
     * @param iPreference
     * @param infaOsrRouting
     */
    init {
        this.preference = PREFERENCES[iPreference]
        this.alternatRoute = alternateRoute
    }

    /*
    // 01aug2023 experimental
    // elevation: https://api.openrouteservice.org/elevation/point?api_key=5b3ce3597851110001cf624800f86f4d35dd452a923c73be6cd20943&geometry=10.3706,52.3254
    @Throws(MalformedURLException::class)
    fun getElevation(lat: Double, lon: Double) {
        val urlBuilder = StringBuilder("https://api.openrouteservice.org/elevation/point?")
        urlBuilder.append("api_key=").append(BuildConfig.ORS_API_KEY) //.append(API_KEY);
        urlBuilder.append("&geometry=").append(lon).append(",").append(lat)
        Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: $urlBuilder")
        val url = URL(urlBuilder.toString())
        Thread(object : Runnable {
            override fun run() {
                val urlConnection: HttpURLConnection
                try {
                    urlConnection = url.openConnection() as HttpURLConnection
                    val bufferedReader: InputStream?
                    checkNotNull(urlConnection)
                    bufferedReader = BufferedInputStream(urlConnection.getInputStream())
                    val buff = ByteArray(8000)
                    var bytesRead: Int
                    val bao = ByteArrayOutputStream()
                    while ((bufferedReader.read(buff).also { bytesRead = it }) != -1) {
                        bao.write(buff, 0, bytesRead)
                    }
                    val out = bao.toString("UTF-8")
                    val h = parseElevation(out)
                    if (infaOsrRouting != null) infaOsrRouting!!.returnElevation(h)
                } catch (e: JSONException) {
                    Timber.e( "${Thread.currentThread().stackTrace[2].lineNumber}: ${e.message}")
                    if (infaOsrRouting != null) infaOsrRouting!!.returnElevation(Int.Companion.MIN_VALUE)
                } catch (e: IOException) {
                    Timber.e( "${Thread.currentThread().stackTrace[2].lineNumber}: ${e.message}")
                    if (infaOsrRouting != null) infaOsrRouting!!.returnElevation(Int.Companion.MIN_VALUE)
                }
            }
        }).start()
    }
*/
    // response
    // {"attribution":"service by https://openrouteservice.org | data by http://srtm.csi.cgiar.org","geometry":{"coordinates":[10.3706,52.3254,71],"type":"Point"},"timestamp":1690878748,"version":"0.2.1"}
    @Throws(JSONException::class)
    private fun parseElevation(out: String): Int {
        Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: out: $out")
        val joRoot = JSONObject(out)
        val joGeometry = joRoot.get("geometry") as JSONObject
        val joCoordinates = joGeometry.get("coordinates") as JSONArray
        return joCoordinates.getInt(2)
    }


    interface InfaOsrRouting {
        fun returnElevation(h: Int)

        fun returnResult(gpxRouteResults: Array<GpxRouteResult?>?, openrouteserviceGpx: String?)
    }

    // graphhopper api, kein gutes android beispiel gefunden 28apr2024
    // https://graphhopper.com/api/1/route?point=52.34705,10.19097&point=52.32784,10.387546&vehicle=bike&debug=true&key=898a8c44-d27f-4766-b1f0-96627c8534df&type=json
    // not working, experimental
    @Throws(MalformedURLException::class)
    fun getGhRoute(context: Context) {
        val url =
            URL("https://graphhopper.com/api/1/route?key=b028927a-eb63-4d5e-b9fe-1272bf0d3338")
        //key=898a8c44-d27f-4766-b1f0-96627c8534df");
        val postJson = JSONObject()
        val coordinates = JSONArray()
        try {
            for (latLng in postLllh) coordinates.put(
                JSONArray().put(latLng.latitude).put(latLng.longitude)
            )
            postJson.put("points", coordinates)
            //postJson.put("profile", "bike");
        } catch (e: JSONException) {
            Timber.e( "${Thread.currentThread().stackTrace[2].lineNumber}: ${e.message}")
        }
        val postData = postJson.toString().toByteArray(StandardCharsets.UTF_8)
        Thread(object : Runnable {
            override fun run() {
                val connection: HttpURLConnection
                try {
                    connection = url.openConnection() as HttpURLConnection
                    checkNotNull(connection)
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setFixedLengthStreamingMode(postData.size)

                    connection.outputStream.use { os ->
                        os.write(postData)
                    }

                    val fos = context.openFileOutput(OPENROUTESERVICE_GPX, 0)
                    fos.use { fileOutput ->
                        val out = BufferedWriter(OutputStreamWriter(fileOutput))
                        out.use { bufferedWriter ->
                            connection.inputStream.use { inputStream ->
                                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                                bufferedReader.use { reader ->
                                    var inputLine: String?
                                    val content = StringBuilder()
                                    while (reader.readLine().also { inputLine = it } != null) {
                                        content.append(inputLine)
                                        bufferedWriter.write(inputLine)
                                        bufferedWriter.newLine()
                                    }
                                    bufferedWriter.flush()
                                }
                            }
                        }
                    }
                    connection.disconnect()
                } catch (e: IOException) {
                    Timber.e( "${Thread.currentThread().stackTrace[2].lineNumber}: ${e.message}")
                }
            }
        }).start()
    }

    // http post method
    // returns gpx rtept with instructions
    // {"coordinates":[[8.681495,49.41461],[8.686507,49.41943],[8.687872,49.420318]]}
    @Throws(MalformedURLException::class)
    fun getOrsRoute(context: Context, infaOsrRouting: (ArrayList<ArrayList<LatLngH>>) -> Unit) {
        val url =
            URL("https://api.openrouteservice.org/v2/directions/" + vehicle + "/gpx") //foot-walking");
        val postJson = JSONObject()
        val coordinates = JSONArray()
        val alternative_routes = JSONObject()
        try {
            for (latLng in postLllh) coordinates.put(
                JSONArray().put(latLng.longitude).put(latLng.latitude)
            )
            postJson.put("coordinates", coordinates)
            postJson.put("elevation", true)
            postJson.put("instructions", true)
            postJson.put("preference", preference)
            postJson.put("language", "de")
            //postJson.put("language", Locale.getDefault().toLanguageTag().substring(0, 2));
            if (alternatRoute) {
                alternative_routes.put("share_factor", 0.4)
                alternative_routes.put("target_count", 2)
                alternative_routes.put("weight_factor", 2.0)
                postJson.put("alternative_routes", alternative_routes)
            }
            Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: $alternative_routes")
        } catch (e: JSONException) {
            Timber.e( "${Thread.currentThread().stackTrace[2].lineNumber}: ${e.message}")
        }
        val postData = postJson.toString().toByteArray(StandardCharsets.UTF_8)
//        Thread(object : Runnable {
//            override fun run() {
        val connection: HttpURLConnection
        try {
            connection = url.openConnection() as HttpURLConnection
            checkNotNull(connection)
            connection.requestMethod = "POST"
            connection.setRequestProperty(
                "Authorization",
                BuildConfig.ORS_API_KEY
            ) //API_KEY);

            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty(
                "Accept",
                "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8"
            )
            connection.doOutput = true
            connection.setFixedLengthStreamingMode(postData.size)

            connection.outputStream.use { os ->
                os.write(postData)
            }

            val content = StringBuilder()
            val fos = context.openFileOutput(OPENROUTESERVICE_GPX, 0)
            fos.use { fileOutput ->
                val out = BufferedWriter(OutputStreamWriter(fileOutput))
                out.use { bufferedWriter ->
                    connection.inputStream.use { inputStream ->
                        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                        bufferedReader.use { reader ->
                            var inputLine: String?
                            while (reader.readLine().also { inputLine = it } != null) {
                                content.append(inputLine)
                                bufferedWriter.write(inputLine)
                                bufferedWriter.newLine()
                            }
                            bufferedWriter.flush()
                        }
                    }
                }
            }
            connection.disconnect()
            Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: $content")
            val stream: InputStream =
                ByteArrayInputStream(content.toString().toByteArray(StandardCharsets.UTF_8))
//                    val gpxRouteResults: Array<GpxRouteResult?>? =
//                        Helpers.readMultiGpxRouteExtended(context, stream)
//                    if (gpxRouteResults != null) {
//                        //infaOsrRouting(gpxRouteResults)
//                        Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: gpxRouteResults:${gpxRouteResults.size}")
//                    }
            val lllhArray = Helpers.readGpxRoute(stream)
            infaOsrRouting(lllhArray)
        } catch (e: IOException) {
            Timber.e( "${Thread.currentThread().stackTrace[2].lineNumber}: ${e.message}")
            infaOsrRouting(arrayListOf())
        }
//            }
//           }).start()
    }

    companion object {
        private const val FASTEST = "fastest"
        private const val SHORTEST = "shortest"
        const val OPENROUTESERVICE_GPX: String = "openrouteservice.gpx"
    }
}

