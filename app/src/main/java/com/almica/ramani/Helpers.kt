package com.almica.ramani

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Xml
import androidx.core.content.edit
import androidx.core.text.isDigitsOnly
import androidx.exifinterface.media.ExifInterface
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.FeatureProperties.Companion.NAME
import androidx.core.content.ContextCompat
import com.almica.ramani.charts.interpolateColor
import com.almica.ramani.googlemaps.RouteData
import com.almica.ramani.gpx.GPXParser
import com.almica.ramani.locations.LocationRepository
import com.almica.ramani.routes.AlminavInstruction
import com.almica.ramani.routes.RouteNearestPointResult
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.OrsRouting
import com.almica.ramani.utils.getRasterRegionNames
import com.almica.ramani.utils.createFeatureString
import com.almica.ramani.utils.formatDistM
import com.almica.ramani.utils.getDistanceFromLllh
import com.almica.ramani.utils.isNetworkAvailable
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.utils.kmlString2Lllh
import com.almica.ramani.utils.lllhToKmlString
import com.almica.ramani.utils.lllhToLineLayer
import com.almica.ramani.utils.reduceWithTolerance
import com.almica.room.data.location.LocationEntity
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.graphhopper.util.Helper
import com.graphhopper.util.Instruction
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshot
import org.maplibre.android.snapshotter.MapSnapshotter
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.android.style.sources.VectorSource
import org.maplibre.geojson.Feature
import org.w3c.dom.NodeList
import org.xml.sax.SAXException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.Reader
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Locale.getDefault
import java.util.concurrent.Executors
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.core.graphics.createBitmap
import com.almica.ramani.utils.RouteSmoothingUtil.simplifyToTargetCount
import com.almica.ramani.utils.format
import com.google.mlkit.vision.text.Text

//const val EN = "en"
private const val logtag = "com.almica.ramani.Helpers"
class Helpers {

    companion object {
        fun getMvtMinZoom(file: File): Int {
            Timber.d("absolutePath: ${file.absoluteFile}")
            val openDatabase =
                SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = openDatabase.query(
                "metadata",
                arrayOf("name", "value"),
                "name=?",
                arrayOf("minzoom"),
                null,
                null,
                null,
            )
            cursor.moveToFirst()
            val minZoomLevel = cursor.getString(1)

            Timber.d("showMBTilesFile minZoomLevel = $minZoomLevel")

            cursor.close()
            openDatabase.close()

            return minZoomLevel.toInt()
        }

        fun getMvtBoundsFromMeta(file: File): LatLngBounds {
            //Timber.d("absolutePath: ${file.absoluteFile}")

            val openDatabase =
                SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = openDatabase.query(
                "metadata",
                arrayOf("name", "value"),
                "name=?",
                arrayOf("bounds"),
                null,
                null,
                null,
            )
            cursor.moveToFirst()
            val boundsStr = cursor.getString(1).split(",")
            cursor.close()
            openDatabase.close()

            return org.maplibre.android.geometry.LatLngBounds
                .Builder()
                .include(LatLng(boundsStr[1].toDouble(), boundsStr[0].toDouble()))
                .include(LatLng(boundsStr[3].toDouble(), boundsStr[2].toDouble()))
                .build()
        }

        // Example conversion from EPSG:3857 to EPSG:4326 (Latitude/Longitude)
        // bbbike geojson qgis (from pmtile) have projection EPSG:3857
        fun webMercatorToWgs84(x: Double, y: Double): DoubleArray? {
            val lon = (x / 20037508.34) * 180
            var lat = (y / 20037508.34) * 180
            lat = 180 / Math.PI * (2 * atan(exp(lat * Math.PI / 180)) - Math.PI / 2)
            return doubleArrayOf(lat, lon) // Returns {latitude, longitude}
        }

        fun analyzeMapStyle(style: Style) {
            // experimental 08feb2026
            // sources -> layers -> features
            Timber.i("sources size: ${style.sources.size}")
            style.sources.forEach { source ->
                if (source.isNotNull() && source is VectorSource) {
                    //Timber.i("${source.id} layers size: ${style.layers.size}")
                    val filter = Expression.has(NAME)
                    style.layers.forEach { layer ->
                        //Timber.i("${source.id} layer: ${layer.id}")
                        val features: List<Feature> =
                            source.querySourceFeatures(arrayOf(layer.id), null) //filter)
                        //Timber.i("${source.id} ${layer.id} features size: ${features.size}")
                        if (features.isNotEmpty()) {
                            features.forEach { feature ->
                                Timber.i("${source.id} ${layer.id} ${feature.id()} name: ${feature.getStringProperty("name")}")
                                //Log.i(logtag, "${source.id} ${layer.id} ${feature.id()} name: ${feature.getStringProperty("name")}")
                            }
                        }
                    }
                }
            }
            Timber.i("analyzeMapStyle finished")
        }

        fun copyCountriesGeojson(context: Context, fileName: String) {
            val countriesInputStream = context.assets.open("geojson/${fileName}")

            //Creating a new file to which to copy the mvt content to
            val rootFolder = context.filesDir
            val geojsonRootFolder = File(rootFolder, Const.GEOJSON_ROOT_FOLDER)
            geojsonRootFolder.mkdir()
            val geojsonFile = File(geojsonRootFolder, fileName)
            if (!geojsonFile.exists()) {
                //Copying the original Mvt content to new file
                copyStreamToFile(countriesInputStream, geojsonFile)
                Timber.i("created: ${geojsonFile.path}")
            } else
                Timber.i("found: ${geojsonFile.path}")
        }

        fun copyAssetPlanetMbtiles(context: Context, fileName: String) {
            val styleJsonInputStream = context.assets.open("mvt_mbtiles/${fileName}")

            //Creating a new file to which to copy the mvt content to
            val rootFolder = context.filesDir
            val mvtRootFolder = File(rootFolder, Const.MVT_FOLDER)
            mvtRootFolder.mkdir()
            val mvtFile = File(mvtRootFolder, fileName)
            if (!mvtFile.exists()) {
                //Copying the original Mvt content to new file
                copyStreamToFile(styleJsonInputStream, mvtFile)
                Timber.i("created: ${mvtFile.path}")
            } else
                Timber.i("found: ${mvtFile.path}")
        }

        fun copyAssetPlanetStyle(context: Context, fileName : String) {
            val styleJsonInputStream = context.assets.open("styles/${fileName}")

            //Creating a new file to which to copy the mvt content to
            val rootFolder = context.filesDir
            val mvtRootFolder = File(rootFolder, Const.MVT_FOLDER)
            mvtRootFolder.mkdir()
            val styleFile = File(mvtRootFolder, fileName)
            copyStreamToFile(styleJsonInputStream, styleFile)
            //Timber.i("created: ${styleFile.path}")
/*
            if (!styleFile.exists()) {
                //Copying the original Mvt content to new file
                copyStreamToFile(styleJsonInputStream, styleFile)
                Timber.i("created: ${styleFile.path}")
            } else
                Timber.i("found: ${styleFile.path}")
 */
        }

        fun getGeojsonFolders(context: Context): Array<File> {
            val rootFolder = context.filesDir
            val geojsonRootFolder = File(rootFolder, Const.GEOJSON_ROOT_FOLDER)
            geojsonRootFolder.mkdirs()
            //Timber.i("geojsonRootFolder: ${geojsonRootFolder.path}")
            val fileFilter = FileFilter { file: File? -> file?.isDirectory == true }
            val files: Array<File> = geojsonRootFolder.listFiles(fileFilter) as Array<File>
            files.sortWith(compareBy { it.name })
            Timber.i("geojsonRootFolder: ${files.size}")
            return files
        }

        fun copyStreamToFile(inputStream: InputStream, outputFile: File) {
            inputStream.use { input ->
                val outputStream = FileOutputStream(outputFile)
                outputStream.use { output ->
                    val buffer = ByteArray(4 * 1024) // buffer size
                    while (true) {
                        val byteCount = input.read(buffer)
                        if (byteCount < 0) break
                        output.write(buffer, 0, byteCount)
                    }
                    output.flush()
                }
            }
        }

        fun readGpxRoute(inputStream: InputStream?): ArrayList<ArrayList<LatLngH>> {
            // only for test
            val gpxParser = GPXParser()
            val gpx = gpxParser.parseGPX(inputStream)
            val lllhArray = ArrayList<ArrayList<LatLngH>>()
            gpx.routes.forEachIndexed { index, route ->
                val lllhRoute = ArrayList<LatLngH>()
                route.routePoints.forEach { routePoint ->
                    lllhRoute.add(
                        LatLngH(
                            routePoint.latitude,
                            routePoint.longitude,
                            routePoint.elevation
                        )
                    )
                }
                lllhArray.add(lllhRoute)
            }
            return lllhArray
        }

        @Throws(IOException::class)
        fun loadProperties(map: MutableMap<String, String>, tmpReader: Reader) {
            val reader = BufferedReader(tmpReader)
            var line: String?
            reader.use { reader ->
                while ((reader.readLine().also { line = it }) != null) {
                    if (line!!.startsWith("//") || line.startsWith("#")) {
                        continue
                    }

                    if (Helper.isEmpty(line)) {
                        continue
                    }

                    val index = line.indexOf("=")
                    if (index < 0) {
                        Timber.i("Skipping configuration at line: $line")
                        continue
                    }

                    val field = line.substring(0, index)
                    val value = line.substring(index + 1)
                    map[field] = value
                }
            }
        }

        fun nearestPointOnPath(
            latLong: com.google.android.gms.maps.model.LatLng,
            listLatLng: java.util.ArrayList<LatLngH>?
        ): RouteNearestPointResult {
            var deviation = Int.MAX_VALUE
            var route_point_pointer = 0
            var dist_sum = 0.0
            var dist_part = 0.0
            if (listLatLng != null) {
                for (i in 1..<listLatLng.size) {
                    val np = nearestPointOnLine(
                        listLatLng.let { it[i - 1] }.latLng,
                        listLatLng.let { it[i] }.latLng,
                        latLong
                    )
                    val newdist = (SphericalUtil.computeDistanceBetween(np, latLong)).toInt()
                    dist_sum += (SphericalUtil.computeDistanceBetween(
                        listLatLng[i].latLng,
                        listLatLng[i - 1].latLng
                    ))
                    if (newdist < deviation) {
                        deviation = newdist
                        route_point_pointer = i
                        dist_part = dist_sum
                    }
                }
            }

            return RouteNearestPointResult(
                deviation,
                route_point_pointer,
                dist_sum,
                dist_part
            )
        }

        fun nearestPointOnLine(
            a: com.google.android.gms.maps.model.LatLng,
            b: com.google.android.gms.maps.model.LatLng,
            p: com.google.android.gms.maps.model.LatLng
        ): com.google.android.gms.maps.model.LatLng {
            val apx = getMercatorX(p.longitude) - getMercatorX(a.longitude)
            val apy = getMercatorY(p.latitude) - getMercatorY(a.latitude)
            val abx = getMercatorX(b.longitude) - getMercatorX(a.longitude)
            val aby = getMercatorY(b.latitude) - getMercatorY(a.latitude)

            val ab2 = abx * abx + aby * aby
            //		myLog.i("ab2 " + String.valueOf(ab2));
            if (ab2 == 0.0)  // a = b
                return a
            else {
                val ap_ab = apx * abx + apy * aby
                val t: Double
                if (ap_ab <= 0) t = 0.0
                else t = min(ap_ab, 1.0)
                return SphericalUtil.interpolate(a, b, t)
                //      double lon = mercatorX2GeoLong(getMercatorX(a.longitude) + abx * t);
//      double lat = mercatorY2GeoLat(getMercatorY(a.latitude) + aby * t);
//      return new LatLng(lat, lon);
            }
        }

        const val EARTH_RADIUS_M: Double = 6378137.0
        fun getMercatorY(lat: Double): Double {
            val sinLat = sin(Math.toRadians(lat))
            return (EARTH_RADIUS_M / 2
                    * ln((1 + sinLat) / (1 - sinLat)))
        }

        fun getMercatorX(lon: Double): Double {
            return EARTH_RADIUS_M * Math.toRadians(lon)
        }

        fun locationIndexOnPath(
            latLong: com.google.android.gms.maps.model.LatLng,
            listLatLng: java.util.ArrayList<LatLngH>?, tolerance: Double
        ): Int {
            val gmsListLatLng = java.util.ArrayList<com.google.android.gms.maps.model.LatLng>()
            listLatLng?.forEach {
                gmsListLatLng.add(
                    com.google.android.gms.maps.model.LatLng(
                        it.latitude,
                        it.longitude
                    )
                )
            }
            return PolyUtil.locationIndexOnPath(latLong, gmsListLatLng, true, tolerance)
        }

        fun getArrowDirection(bearing_: Double, context: Context): String {
            var bearing = bearing_
            if (bearing < 0) bearing += 360.0
            if (bearing > 360) bearing -= 360.0
            val arrows = context.resources.getStringArray(R.array.arrows_directions)
            val index = floor(((bearing - 22.5) % 360) / 45)
            return arrows[(index + 1).toInt()]
        }

        fun latitudeToY(latitude: Double): Double {
            val sinLatitude = sin(latitude * 0.017453292519943295)
            return FastMath.clamp(
                0.5 - ln((1.0 + sinLatitude) / (1.0 - sinLatitude)) / 12.566370614359172,
                0.0,
                1.0
            )
        }

        fun longitudeToX(longitude: Double): Double {
            return (longitude + 180.0) / 360.0
        }

        fun getLllhFromFile(routeFile: File): java.util.ArrayList<LatLngH>? {
            val listLatLng = java.util.ArrayList<LatLngH>()
            if (routeFile.name.lowercase(getDefault()).endsWith(Const.JPG_EXT)) {
                try {
                    val coordinates: java.util.ArrayList<LatLngH> =
                        getCoordinatesFromExif(routeFile)
                    listLatLng.addAll(coordinates)
                    Timber.i("coordinates created from exif data: ${routeFile.name}")
                } catch (e: IOException) {
                    e.message?.let {
                        Timber.e(it)
                    }
                    return null
                }
            } else if (routeFile.name.lowercase(getDefault()).endsWith(Const.GPX_EXT)) {
                try {
                    listLatLng.addAll(readGpxFile(routeFile.path))
                } catch (e: ParserConfigurationException) {
                    e.message?.let {
                        Timber.e(it)
                    }
                    return null
                } catch (e: IOException) {
                    e.message?.let {
                        Timber.e(it)
                    }
                    return null
                } catch (e: SAXException) {
                    e.message?.let {
                        Timber.e(it)
                    }
                    return null
                }
            } else {
                try {
                    val lllh = readKmlFile(routeFile)
                    if (lllh != null) {
                        listLatLng.addAll(lllh)
                    } else
                        Timber.i("no coordinates in ${routeFile.path}")
                } catch (e: IOException) {
                    e.message?.let { Timber.e(it) }
                    return null
                }
            }
            return listLatLng
        }

        @Throws(IOException::class)
        fun getKmlStringFromExif(jpgFile: File): String? {
            Timber.i(jpgFile.path)
            var kmlString: String? = null
            val exifInterface: ExifInterface
            try {
                exifInterface = ExifInterface(jpgFile.path)
                kmlString = exifInterface.getAttribute(ExifInterface.TAG_USER_COMMENT)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return kmlString
        }

        @Throws(IOException::class)
        fun getImageDimensionsFromExif(jpgFile: File): Pair<String?, String?> {
            Timber.i("getImageDescriptionFromExif: ${jpgFile.path}")
            var imageWidthString: String? = null
            var imageHeightString: String? = null
            val exifInterface: ExifInterface
            try {
                exifInterface = ExifInterface(jpgFile.path)
                imageWidthString = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
                imageHeightString = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
            } catch (e: IOException) {
                e.printStackTrace()
                Timber.e("exif could not get TAG_IMAGE_DESCRIPTION")
            }

            return Pair(imageWidthString, imageHeightString)
        }

        @Throws(IOException::class)
        fun getImageDescriptionFromExif(jpgFile: File): String? {
            Timber.i("getImageDescriptionFromExif: ${jpgFile.path}")
            var imageDescriptionString: String? = null
            val exifInterface: ExifInterface
            try {
                exifInterface = ExifInterface(jpgFile.path)
                imageDescriptionString = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)
            } catch (e: IOException) {
                e.printStackTrace()
                Timber.e("exif could not get TAG_IMAGE_DESCRIPTION")
            }

            return imageDescriptionString
        }

        @Throws(IOException::class)
        fun getCoordinatesFromExif(jpgFile: File): java.util.ArrayList<LatLngH> {
            Timber.i(jpgFile.path)
            var kmlString: String? = null
            val exifInterface: ExifInterface
            try {
                exifInterface = ExifInterface(jpgFile.path)
                kmlString = exifInterface.getAttribute(ExifInterface.TAG_USER_COMMENT)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (kmlString != null)
                return kmlString.kmlString2Lllh()
            else {
                Timber.e("exif could not get valid coordinates")
                return ArrayList()
            }
        }

        fun kmlString2RouteData(kmlString: String?): RouteData? {
            val inputStream = kmlString?.byteInputStream()
            val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val document = docBuilder.parse(inputStream) ?: return RouteData(arrayListOf(), "", 0.0, false, null)
            val nameNodeList: NodeList = document.getElementsByTagName("Name")
            if (nameNodeList.item(0).firstChild.isNotNull()) {
                val name = nameNodeList.item(0).firstChild.nodeValue

                val coordList: NodeList = document.getElementsByTagName("coordinates")
                val positions = arrayListOf<LatLngH>()

                for (i in 0 until coordList.length) {
                    val coordinatePairs =
                        coordList.item(i).firstChild.nodeValue.trim().split(" ")

                    coordinatePairs.forEach { coord ->
                        val coordSplits = coord.split(",")
                        positions.add(
                            LatLngH(
                                coordSplits[1].toDouble(),
                                coordSplits[0].toDouble(),
                                if (coordSplits.size > 2) coordSplits[2].toDouble() else 0.0
                            )
                        )
                    }
                }
                var dist = 0.0
                for (i in 1 until positions.size)
                    dist += SphericalUtil.computeDistanceBetween(
                        positions[i - 1].latLng,
                        positions[i].latLng
                    )
                return RouteData(positions, name, dist, false, null)
            }
            return null
        }

        fun convertSecondsToHHMMSS(totalSeconds: Int): String {
            val hours = totalSeconds / 3600
            val minutes: Int = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            return String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds)
        }

        fun getRouteGeojsonFiles(context: Context): ArrayList<File> {
            val resultFiles = arrayListOf<File>()
            val rootMapsFolder = File(context.filesDir, Const.ROUTEFOLDER)
            rootMapsFolder.walkTopDown().forEach {file ->
                if (file.name.endsWith(Const.GEOJSON_EXT)
                    && file.name.startsWith(file.parentFile?.name.plus(Const.UNDERLINE))) {
                    Timber.i( "${file.name}")
                    resultFiles.add(file)
                }
            }
            return resultFiles
        }

        fun writeGpxFileFromLocationEntities(
            locationEntities: List<LocationEntity>?,
            gpxFile: File
        ): Boolean {
            if (!locationEntities.isNullOrEmpty()) {
                val xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                val tagGpx =
                    "<gpx" + " version=\"1.1\"" + " creator=\"Alminav 3d\"" +
                            " xmlns=\"http://www.topografix.com/GPX/1/1\"" +
                            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                            " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx" +
                            ".xsd \">"

                Timber.i("gpxFile: %s", gpxFile.path)
                val gpxMetadata = ("  <metadata>\n" +
                        "    <time>" + Const.DF.format(System.currentTimeMillis()) + "</time>\n" +
                        "  </metadata>")

                val fw = FileWriter(gpxFile)
                fw.write(xmlHeader + "\n")
                fw.write(tagGpx + "\n")
                fw.write(gpxMetadata + "\n")
                fw.write("\t" + "<trk>" + "\n")
                fw.write("\t\t" + "<name>" + gpxFile.name + "</name>" + "\n")
                fw.write("\t\t" + "<trkseg>" + "\n")

                for (locationEntity in locationEntities) {
                    val out = StringBuilder()
                    out.append("\t\t\t" + "<trkpt lat=\"").append(locationEntity.latitude)
                        .append("\" ")
                        .append("lon=\"").append(locationEntity.longitude).append("\">")
                    out.append("<ele>").append(locationEntity.altitude).append("</ele>")
                    out.append("<time>").append(Const.DF.format(locationEntity.time))
                        .append("</time>")
                    out.append("</trkpt>" + "\n")
                    fw.write(out.toString())
                }
                fw.write("\t\t" + "</trkseg>" + "\n")
                fw.write("\t" + "</trk>" + "\n")
                fw.write("</gpx>")
                fw.close()
                Timber.i("%s ready", gpxFile.path)
                return true
            } else {
                Timber.i("%s ready", gpxFile.path)
                return false
            }
        }

        fun readGpxFile(path: String?): java.util.ArrayList<LatLngH> {
            val listLatLng = java.util.ArrayList<LatLngH>()
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val gpxFile = path?.let { File(it) }
            if (gpxFile == null || !gpxFile.exists()) {
                Timber.e("$path not found")
                return listLatLng
            }
            val inputStream: InputStream = FileInputStream(gpxFile)
            val dom = builder.parse(inputStream)
            val root = dom.documentElement ?: // 10jan2020
            return listLatLng
            val items = root.getElementsByTagName("trkpt") //
            var ele = 0.0
            for (j in 0 until items.length) {
                val item = items.item(j)
                val attrs = item.attributes
                if (attrs != null) {
                    if (item.hasChildNodes()) {
                        val props = item.childNodes
                        for (y in 0 until props.length) {
                            val item3 = props.item(y)
                            val name = item3.nodeName
                            if (item3.hasChildNodes()) {
                                //Log.i(logtag,name + " " + item3.getFirstChild().getNodeValue());
                                if (name.equals("ele", ignoreCase = true)) ele =
                                    item3.firstChild.nodeValue.toDouble()
                            }
                        }
                    }

                    listLatLng.add(
                        LatLngH(
                            attrs.getNamedItem("lat").textContent.toDouble(),
                            attrs.getNamedItem("lon").textContent.toDouble(), ele
                        )
                    )
                }
            }

            inputStream.close()
//            Log.i(logtag,
//                "listLatLng.size ${listLatLng.size}")
            return listLatLng
        }

        @Throws(IOException::class)
        fun readKmlFile(file: File?): java.util.ArrayList<LatLngH>? {
//            Log.i(logtag,
//                "readKmlFile ${file?.path}")
            val inputStream = FileInputStream(file)
            val listLatLng = java.util.ArrayList<LatLngH>()

            try {
                val factory = XmlPullParserFactory.newInstance()
                val xpp = factory.newPullParser()
                xpp.setInput(inputStream, null) //"UTF-8");

                var inCoordinates = false
                var inLineString = false
                var inDescription = false
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
                        if (startTagName.equals("description", ignoreCase = true)) {
                            inDescription = true
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        val endTagName = xpp.name
                        if (endTagName == "coordinates") {
                            inCoordinates = false
                        }
                        if (endTagName == "LineString") {
                            inLineString = false
                        }
                        if (endTagName.equals("description", ignoreCase = true)) {
                            inDescription = false
                        }
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (inCoordinates && inLineString) {
                            sb.append(xpp.text.replace("\n".toRegex(), " "))
                        }
//                        else if (inDescription) {
//                            Timber.i("""description${xpp.text}""".trimIndent())
//                        }
                    }
                    eventType = xpp.next()
                } while (eventType != XmlPullParser.END_DOCUMENT)
                inputStream.close()

                val coordinates = sb.toString()

                val coordLines = coordinates.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                for (i in coordLines.indices) {
                    if (coordLines[i].length > 3) {
                        val llh = coordLines[i].split(",".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                        try {
                            val nLongitude = llh[0].toDouble()
                            val nLatitude = llh[1].toDouble()
                            if (llh.size == 3) {
                                try {
                                    val iAlt = llh[2].toInt()
                                    listLatLng.add(LatLngH(nLatitude, nLongitude, iAlt.toDouble()))
                                } catch (e: java.lang.NumberFormatException) {
                                    e.message?.let { Timber.e(it) }
                                    val dAlt = llh[2].toDouble()
                                    listLatLng.add(LatLngH(nLatitude, nLongitude, dAlt))
                                }
                            } else listLatLng.add(LatLngH(nLatitude, nLongitude, 0.0))
                        } catch (e: java.lang.NumberFormatException) {
                            e.message?.let { Timber.e(it) }
                            Timber.e("parse error line $i ${coordLines[i]}")
                        }
                    }
                }
            } catch (xppe: XmlPullParserException) {
                xppe.printStackTrace()
                Timber.i(" readKml Exception $xppe")
                // fall through
                return null
            }
            Timber.i(listLatLng.size.toString())
            return listLatLng
        }

        fun onlineOrsRouting(
            context: Context,
            startY: Double,
            startX: Double,
            stopY: Double,
            stopX: Double,
            alternateRoute: Boolean,
            finished: (ArrayList<ArrayList<LatLngH>>, name: String, success: Boolean, errorMsg: String) -> Unit
        ) {
            if (!context.isNetworkAvailable()) {
                Timber.i(context.getString(R.string.no_network_available))
                finished(
                    arrayListOf(),
                    "Error",
                    false,
                    context.getString(R.string.no_network_available)
                )
                return
            }
            Timber.i("alternateRoute: $alternateRoute")
            val postLll = java.util.ArrayList<com.google.android.gms.maps.model.LatLng>()
            postLll.add(com.google.android.gms.maps.model.LatLng(startY, startX))
            postLll.add(com.google.android.gms.maps.model.LatLng(stopY, stopX))
            val sharedPreferences = getDefaultSharedPreferences(context)
            val s1_s2: String = sharedPreferences.getString(
                context.getString(R.string.setting_locomotion),
                Const.DEFAULT_LOCOMOTION
            )!!
            val splits: List<String> = s1_s2.split(".")
            val iVehicle = 2.coerceAtMost(Integer.parseInt(splits[0]))
            val iPreference = 1 - Integer.parseInt(splits[1]) // 0=shortest, 1=fastest
            //val sVehicle: String? = Helpers.getVehicleDescription(context)
            OrsRouting(
                postLll,
                iVehicle,
                iPreference,
                alternateRoute
            ).getOrsRoute(context) { lllhArray ->
                Timber.i(" lllhArray ${lllhArray.size} ")
                val time_format =
                    SimpleDateFormat(Const.TIME_PATTERN_LONG, getDefault())
                val name = Const.ORS_TAG + "." + time_format.format(Date())
                finished(lllhArray, name, true, "")
            }
        }

        fun takeLocationsSnapshot(
            context: Context, lllhAfter: ArrayList<LatLngH>,
            styleUri: String?,
            size: Int,
            border: Double,
            finished: (mapSnapshot: MapSnapshot?, mapBounds: LatLngBounds) -> Unit
        ) {
            val localStyleUri = styleUri // isNotNull ==> mvt
            if (lllhAfter.isNotEmpty()) {
                localStyleUri?.let {
                    Handler(Looper.getMainLooper()).post {
                        val tripleLayerSourceBounds = lllhAfter.lllhToLineLayer(Const.LOCATIONS, border)
                        val builder = Style.Builder().fromUri(localStyleUri)
                        //builder.withSource(tripleLayerSourceBounds.second)
                        //builder.withLayer(tripleLayerSourceBounds.first)
                        val mapSnapshotter = MapSnapshotter(
                            context,
                            MapSnapshotter
                                .Options(size, size)
                                .withStyleBuilder(builder) //Style.Builder().fromUri(Const.styleVectorUri))
                                .withRegion(tripleLayerSourceBounds.third)
                                .withLogo(false) // no effect
                        )
                        //Timber.i("region ${tripleLayerSourceBounds.third}")
                        mapSnapshotter.start({ snapshot ->
                            //Timber.i("snapshot $name bitmap.byteCount: ${snapshot.bitmap.byteCount}")
                            finished(snapshot, tripleLayerSourceBounds.third)
                        }) { error ->
                            Timber.e("$styleUri: $error")
                            finished(null, tripleLayerSourceBounds.third)
                        }
                    }
                }?: Timber.i("styleUri parameter = null")
            } else
                Timber.i("route has no coordinates")
        }

        fun addLineToSnapshotWithGradient(
            snapshot: MapSnapshot,
            lllh: List<LatLngH>
        ) {
            val canvas = Canvas(snapshot.bitmap)
            val linePaintBorder = Paint().apply {
                color = android.graphics.Color.DKGRAY
                strokeWidth = 9f
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }
            if (lllh.size > 1) {
                lllh.forEachIndexed { index, _ ->
                    if (index > 0) {
                        val dist = SphericalUtil.computeDistanceBetween(
                            lllh[index].latLng,
                            lllh[index - 1].latLng
                        )
                        var gradient = 0.0
                        val deltaH: Double =
                            lllh[index].altitude - lllh[index - 1].altitude
                        if (dist > 0) gradient = 100 * deltaH / dist
                        val c = interpolateColor((0.1 * abs(gradient)).toFloat())
                        val pointA = snapshot.pixelForLatLng(lllh[index - 1].latLngMapLibre)
                        val pointB = snapshot.pixelForLatLng(lllh[index].latLngMapLibre)
                        val linePaint = Paint().apply {
                            color = c
                            strokeWidth = 5f
                            style = Paint.Style.STROKE
                            strokeJoin = Paint.Join.ROUND
                            strokeCap = Paint.Cap.ROUND
                            isAntiAlias = true
                        }
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

        fun addLineToSnapshot(
            snapshot: MapSnapshot,
            lllh: List<LatLngH>
        ) {
            val canvas = Canvas(snapshot.bitmap)
            if (lllh.size > 1) {
                val linePaint = Paint().apply {
                    color = android.graphics.Color.BLUE
                    strokeWidth = 5f
                    style = Paint.Style.STROKE
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                    isAntiAlias = true
                }

                val path = Path()
                lllh.forEachIndexed { index, llh ->
                    val point = snapshot.pixelForLatLng(llh.latLngMapLibre)
                    if (index == 0) {
                        path.moveTo(point.x, point.y)
                    } else {
                        path.lineTo(point.x, point.y)
                    }
                }
                canvas.drawPath(path, linePaint)
            }
        }

        /**
         * example rad-fernwege
         *   element: 0.8740809 Berlin-Kopenhagen [Landroid.graphics.Point;@b8693c7 Rect(49, 469 - 240, 493)
         */
        fun routeNameRecognition(bitmap: Bitmap, textBlocks: (List<Text.TextBlock>) -> Unit) {
            // When using Latin script library
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = bitmap.let { bitmap -> InputImage.fromBitmap(bitmap, 0) }
            image.let { p0 ->
                recognizer.process(p0)
                    .addOnSuccessListener { visionText ->
                        textBlocks(visionText.textBlocks)
                    }
                    .addOnFailureListener { e ->
                        Timber.i("Task failed with an exception $e")
                        // ...
                    }
            }
        }

        /**
         * example radfernwege
         *   element: 0.8740809 Berlin-Kopenhagen [Landroid.graphics.Point;@b8693c7 Rect(49, 469 - 240, 493)
         */
        fun textRecognition(bitmap: Bitmap) {
            // When using Latin script library
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = bitmap.let { bitmap -> InputImage.fromBitmap(bitmap, 0) }
            val result = image.let { p0 ->
                recognizer.process(p0)
                    .addOnSuccessListener { visionText ->
                        //Timber.i("visionText: ${visionText.text}")
                        //val resultText = visionText.text
                        for (block in visionText.textBlocks) {
                            val blockText = block.text
                            val blockCornerPoints = block.cornerPoints
                            val blockFrame = block.boundingBox
                            for (line in block.lines) {
                                val lineText = line.text
                                //Timber.i("lineText: $lineText line.boundingBox ${line.boundingBox}")
                                line.boundingBox?.bottom?.let {
                                    if (it > 490)
                                        Timber.i("name: $lineText")
                                }
                                val lineCornerPoints = line.cornerPoints
                                val lineFrame = line.boundingBox
                                for (element in line.elements) {
                                    element.confidence
                                    val elementText = element.text
                                    val elementCornerPoints = element.cornerPoints
                                    val elementFrame = element.boundingBox

                                    if (element.confidence > 0.80 && element.text.first().isUpperCase()) {
                                        Timber.i("elementConfidence: ${element.confidence} $elementText " +
                                                "${elementCornerPoints.contentToString()} elementFrame: $elementFrame")
                                        elementFrame?.let {
                                            if (it.bottom > 490)
                                                Timber.i("name $elementText")
                                        }
                                    }
                                }
                            }
                        }
                        // Task completed successfully
                        // ...
                    }
                    .addOnFailureListener { e ->
                        Timber.i("Task failed with an exception $e")
                        // ...
                    }
            }
        }

        fun takeSnapshot(
            context: Context, lllhBounds: ArrayList<LatLngH>,
            name: String?,
            styleUri: String?,
            size: Int,
            border: Double,
            writeFile: Boolean,
            finished: (mapSnapshot: MapSnapshot?) -> Unit
        ) {
            val preferences = getDefaultSharedPreferences(context)
            val mvtPath = preferences.getString(Const.PREF_MVT_FILEPATH, null)
            var localStyleUri: String? // isNotNull ==> mvt
            if (mvtPath != null) {
                val fileMbTile = File(mvtPath) //File(dirMbTiles, MBTILES_NAME)
                localStyleUri = createMvtOfflineStyle(context, fileMbTile)
                Timber.i("localStyleFile: $localStyleUri")
            } else
                localStyleUri = styleUri
            Timber.i("name: $name writeFile: $writeFile lllh.size: ${lllhBounds.size}")
            if (lllhBounds.isNotEmpty()) {
                localStyleUri?.let { uri ->
                    Handler(Looper.getMainLooper()).post {
                        val tripleLayerSourceBounds = lllhBounds.lllhToLineLayer(name, border)
                        val builder = Style.Builder().fromUri(uri)//Const.styleVectorUri)
                            .withSource(tripleLayerSourceBounds.second)
                            .withLayer(tripleLayerSourceBounds.first)

                        val mapSnapshotter = MapSnapshotter(
                            context,
                            MapSnapshotter
                                .Options(size, size)
                                .withStyleBuilder(builder)
                                .withRegion(tripleLayerSourceBounds.third)
                                .withLogo(false) // no effect
                        )
                        Timber.i("region ${tripleLayerSourceBounds.third}")
                        mapSnapshotter.start({ snapshot ->
                            if (writeFile) {
                                writeSnapshotToFile(context, snapshot, name, lllhBounds, tripleLayerSourceBounds.third) { file ->
                                    file?.let { f ->
                                        Timber.i("write snapshot file: ${f.path}")
                                    } ?: Timber.i("write snapshot file failed")
                                    finished(snapshot)
                                }
                            } else {
                                Timber.i("snapshot $name bitmap.byteCount: ${snapshot.bitmap.byteCount}")
                                finished(snapshot)
                            }
                        }) { error ->
                            Timber.e("$styleUri: $error")
                            finished(null)
                        }
                    }
                }?: Timber.i("styleUri parameter = null")
            } else
                Timber.i("route has no coordinates")
        }


        fun takeRouteSnapshot(
            context: Context, lllh: ArrayList<LatLngH>,
            name: String?,
            styleUri: String?,
            size: Int,
            border: Double,
            writeFile: Boolean,
            routeFolder: File?,
            finished: (mapSnapshot: MapSnapshot?, mapBounds: LatLngBounds) -> Unit
        ) {
            val preferences = getDefaultSharedPreferences(context)
            val mvtPath = preferences.getString(Const.PREF_MVT_FILEPATH, null)
            var localStyleUri: String? // isNotNull ==> mvt
            if (mvtPath != null) {
                val fileMbTile = File(mvtPath) //File(dirMbTiles, MBTILES_NAME)
                localStyleUri = createMvtOfflineStyle(context, fileMbTile)
                Timber.i("localStyleFile: $localStyleUri")
            } else
                localStyleUri = styleUri
            Timber.i("name: $name writeFile: $writeFile lllh.size: ${lllh.size}")
            if (lllh.isNotEmpty()) {
                localStyleUri?.let { uri ->
                    Handler(Looper.getMainLooper()).post {
                        val tripleLayerSourceBounds = lllh.lllhToLineLayer(name, border)
                        val builder = Style.Builder().fromUri(uri)//Const.styleVectorUri)
                            //.withSource(tripleLayerSourceBounds.second)
                            //.withLayer(tripleLayerSourceBounds.first)

                        val mapSnapshotter = MapSnapshotter(
                            context,
                            MapSnapshotter
                                .Options(size, size)
                                .withStyleBuilder(builder)
                                .withRegion(tripleLayerSourceBounds.third)
                                .withLogo(false) // no effect
                        )

                        val handler = Handler(Looper.getMainLooper())
                        val timeoutRunnable = Runnable {
                            Timber.e("MapSnapshotter timeout for route snapshot: $name")
                            mapSnapshotter.cancel()
                            finished(null, tripleLayerSourceBounds.third)
                        }

                        mapSnapshotter.start({ snapshot ->
                            handler.removeCallbacks(timeoutRunnable)
                            addLineToSnapshotWithGradient(snapshot, lllh)
                            if (writeFile) {
                                writeSnapshotToFile(
                                    context,
                                    snapshot,
                                    name,
                                    lllh,
                                    tripleLayerSourceBounds.third
                                ) { file ->
                                    file?.let { f ->
                                        Timber.i("write snapshot file ready: ${f.path}")
//                                        if (routeFolder.isNotNull()) {
//                                            val routeThumbnail = File(routeFolder, f.name)
//                                            if (routeThumbnail.exists())
//                                                f.copyTo(routeThumbnail, true)
//                                        }
                                    } ?: Timber.i("write snapshot file failed")
                                    finished(snapshot, tripleLayerSourceBounds.third)
                                }
                            } else {
                                Timber.i("snapshot $name bitmap.byteCount: ${snapshot.bitmap.byteCount}")
                                finished(snapshot, tripleLayerSourceBounds.third)
                            }
                        }) { error ->
                            handler.removeCallbacks(timeoutRunnable)
                            Timber.e("$styleUri: $error")
                            finished(null, tripleLayerSourceBounds.third)
                        }

                        handler.postDelayed(timeoutRunnable, 5000) // Slightly longer for routes
                    }
                }?: Timber.i("styleUri parameter = null")
            } else
                Timber.i("route has no coordinates")
        }

        private fun writeSnapshotToFile(
            context: Context,
            snapshot: MapSnapshot,
            name: String?,
            lllh: ArrayList<LatLngH>,
            latLngBounds: LatLngBounds,
            snapshotFile: (File?) -> Unit
        ) {
            //snapshot.isShowLogo
            if (name != null) {
                Timber.i("snapshot ready withLogo ${snapshot.isShowLogo}")
                val folderThumbnails =
                    File(context.filesDir, Const.THUMBNAILS)
                var b = folderThumbnails.mkdir()
                Timber.i("${folderThumbnails.path} mkdir: $b")
                var fileName = name.replace(Const.KML_EXT, Const.JPG_EXT)
                    .replace(Const.GPX_EXT, Const.JPG_EXT)
                    .replace(Const.GEOJSON_EXT, Const.JPG_EXT)
                if (!fileName.endsWith(Const.JPG_EXT))
                    fileName += Const.JPG_EXT
                Timber.i("fileName: $fileName")
                val file = File(
                    folderThumbnails,
                    fileName
                )
                if (file.exists()) {
                    b = file.delete()
                    Timber.i("${file.path} delete $b")
                }

                val out = FileOutputStream(file)
                snapshot.bitmap.compress( //isBoundary ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG,
                    Bitmap.CompressFormat.JPEG, 60, out
                )
                out.flush()
                out.close()
                writeKml2Exif(file, lllh, name, true, latLngBounds)
                Timber.i("snapShot file $name ${file.path} created")
                snapshotFile(file)
            } else
                Timber.e("name = null")
        }

        /**
         *         Setting the mvt map style
         */
        fun createMvtOfflineStyle(context: Context, mbtilesFile: File): String {
            Timber.i("mbtilesFile ${mbtilesFile.name}")
            val styleFileName = when (mbtilesFile.name) {
                Const.COUNTRIES_MVT_FILENAME -> Const.COUNTRIES_STYLE_FILENAME
                Const.PLANET_MVT_FILENAME -> Const.PLANET_STYLE_FILENAME
                else -> Const.MVT_OFFLINE_STYLE_FILENAME
            }
            val styleJsonInputStream = context.assets.open("styles/${styleFileName}")
            //Creating a new file to which to copy the JSON content to
            //val dir = File(filesDir.absolutePath)
            val rootFolder = context.filesDir
            val mvtRootFolder = File(rootFolder, Const.MVT_FOLDER)
            mvtRootFolder.mkdir()

            val styleFile = File(mvtRootFolder, styleFileName)
            //Copying the original JSON content to new file
            copyStreamToFile(styleJsonInputStream, styleFile)

            //val bounds = getMvtBounds(mbtilesFile)
            //Timber.i("bounds: $bounds")
            //val minZoomLevel = getMinZoom(mbtilesFile).toDouble()

            //Replacing placeholder with uri of the mbtiles file
            val newFileStr = styleFile.inputStream().readToString()
                .replace("___FILE_URI___", "mbtiles:///${mbtilesFile.absolutePath}")
            //Timber.i("newFileStr: $newFileStr")
            //Writing new content to file
            val fileWriter = FileWriter(styleFile)
            val out = BufferedWriter(fileWriter)
            out.write(newFileStr)
            out.close()
            return Uri.fromFile(styleFile).toString()
            //return styleFile
        }

        fun writeKml2Exif(
            picFile: File,
            coordinates: java.util.ArrayList<LatLngH>?,
            name: String,
            isNotBoundary: Boolean,
            latLngBounds: LatLngBounds
        ) {
            if (coordinates != null) {
                val dist = coordinates.getDistanceFromLllh()
                val sDist: String = dist.formatDistM(true)
                val exifInterface = ExifInterface(picFile.path)
                val imgDesc = "$sDist ${latLngBounds.northWest.latitude.format(4)} ${latLngBounds.northWest.longitude.format(4)} " +
                        "${latLngBounds.southEast.latitude.format(4)} ${latLngBounds.southEast.longitude.format(4)}"
                Timber.i("imgDesc $imgDesc")
                exifInterface.setAttribute(
                    ExifInterface.TAG_IMAGE_DESCRIPTION,
                    imgDesc
                )
                exifInterface.setAttribute(
                    ExifInterface.TAG_ORIENTATION,  // 28jan2022
                    ExifInterface.ORIENTATION_NORMAL.toString()
                )

                Timber.i("route snapshot created: %s", picFile.path)
                var kmlString: String?
                if (isNotBoundary) { // reduce
                    val reduceWithTolerance = coordinates.simplifyToTargetCount(500)
/*                    var tolerance = 30.0
                    var reduceWithTolerance: java.util.ArrayList<LatLngH> =
                        coordinates.reduceWithTolerance(tolerance)
                    //Timber.i(tolerance + ": " + reduceWithTolerance.size());
                    while (reduceWithTolerance.size > 1000 && tolerance < 2000) {
                        tolerance *= 2.0
                        reduceWithTolerance =
                            coordinates.reduceWithTolerance(tolerance)
                        //Timber.i(tolerance + ": " + reduceWithTolerance.size());
                    }*/
                    Timber.i("reduceWithTolerance.size: ${reduceWithTolerance.size}")
                    kmlString = (reduceWithTolerance as ArrayList<LatLngH>).lllhToKmlString(name.replace("ä", "ae")
                        .replace("ö", "oe")
                        .replace("ü", "ue")
                        .replace("Ä", "Ae")
                        .replace("Ö", "Oe")
                        .replace("Ü", "Ue"))
                } else
                    kmlString = coordinates.lllhToKmlString(name.replace("ä", "ae")
                        .replace("ö", "oe")
                        .replace("ü", "ue")
                        .replace("Ä", "Ae")
                        .replace("Ö", "Oe")
                        .replace("Ü", "Ue"))
                Timber.i("kmlString.length: ${kmlString.length}")
                if (kmlString.length < Const.EXIF_MAX_SIZE) {
                    exifInterface.setAttribute(
                        ExifInterface.TAG_USER_COMMENT, kmlString
                    )
                    exifInterface.setLatLong(coordinates[0].latitude, coordinates[0].longitude)
                    exifInterface.saveAttributes()
                } else
                    Timber.i("too large kmlString.length: ${kmlString.length}")
            }
        }

        /**
         * MUST be called on the UI thread as it instantiates MapLibre Source and Layer objects.
         */
        fun lllhLocationsToLineLayer(
            lllhAfter: ArrayList<LatLngH>,
            lllhBefore: ArrayList<LatLngH>,
            border: Double,
            mvtPath: String?
        ): Triple<List<LineLayer>, List<GeoJsonSource>, LatLngBounds> {
            var tileBounds: LatLngBounds? = null
            if (mvtPath != null) {
                val mvtFile = File(mvtPath) //File(dirMbTiles, MBTILES_NAME)
                if (mvtFile.name.startsWith(Const.MVT_PREFIX)) {
                    val splits = mvtFile.name.replace(Const.MBTILES_EXT, "").split(Const.UNDERLINE, limit = 4)
                    if (splits[1].isDigitsOnly() && splits[2].isDigitsOnly() && splits[3].isDigitsOnly()) {
                        val tile = GeoJsonUtils.Companion.Tile(
                            splits[1].toInt(),
                            splits[2].toInt(),
                            splits[3].toInt()
                        )

                        tileBounds = GeoJsonUtils.tileToBounds(tile)
                    }
                }
            }

            val geoJsonSourceNameHaircrossVertical = "geoJsonSourceNameHaircrossVertical"
            val geoJsonSourceNameHaircrossHorizontal = "geoJsonSourceNameHaircrossHorizontal"
            val boundsPair = getLocationsBoundsPair(lllhAfter, lllhBefore, border) // first normal, second with border
            val currentLatLng = lllhAfter[0]
            val haircrossVerticalLllh = arrayListOf(LatLngH(tileBounds?.latitudeSouth ?: 0.0, currentLatLng.longitude),
                LatLngH(tileBounds?.latitudeNorth ?: 80.0, currentLatLng.longitude))
            val haircrossHorizontalLllh = arrayListOf(LatLngH(currentLatLng.latitude, tileBounds?.longitudeWest ?: -40.0),
                LatLngH(currentLatLng.latitude, tileBounds?.longitudeEast ?: 40.0))
            val haircrossVerticalJsonString = haircrossVerticalLllh.createFeatureString(geoJsonSourceNameHaircrossVertical, null, boundsPair.first)
            val haircrossHorizontalJsonString = haircrossHorizontalLllh.createFeatureString(geoJsonSourceNameHaircrossHorizontal, null, boundsPair.first)
            val lineLayerHaircrossVertical = LineLayer("${geoJsonSourceNameHaircrossVertical}_line", geoJsonSourceNameHaircrossVertical)
                .withProperties(
                    PropertyFactory.lineColor(Color.BLACK),
                    PropertyFactory.lineDasharray(arrayOf(1f, 2f)),
                    PropertyFactory.lineWidth(4f)
                )
            val lineLayerHaircrossHorizontal = LineLayer("${geoJsonSourceNameHaircrossHorizontal}_line", geoJsonSourceNameHaircrossHorizontal)
                .withProperties(
                    PropertyFactory.lineColor(Color.BLACK),
                    PropertyFactory.lineDasharray(arrayOf(1f, 2f)),
                    PropertyFactory.lineWidth(4f)
                )
            val sourceHaircrossVertical = GeoJsonSource(geoJsonSourceNameHaircrossVertical, haircrossVerticalJsonString)
            val sourceHaircrossHorizontal = GeoJsonSource(geoJsonSourceNameHaircrossHorizontal, haircrossHorizontalJsonString)
            //"mapbox://styles/mapbox/streets-v12"
            val geoJsonSourceNameAfter = "geoJsonSourceNameAfter"
            val jsonStringAfter = lllhAfter.createFeatureString(geoJsonSourceNameAfter, null, boundsPair.first)
                //getFeatureStringFromLll(lllhAfter, geoJsonSourceNameAfter, null, bounds)
            val sourceAfter = GeoJsonSource(geoJsonSourceNameAfter, jsonStringAfter)
            val lineLayerAfter = LineLayer("${geoJsonSourceNameAfter}_line_after", geoJsonSourceNameAfter)
                .withProperties(
                    PropertyFactory.lineColor(Color.RED),
                    //PropertyFactory.lineDasharray(arrayOf(1f, 2f)),
                    PropertyFactory.lineWidth(4f)
                )
            val geoJsonSourceNameBefore = "geoJsonSourceNameBefore"
            val jsonStringBefore = lllhBefore.createFeatureString(geoJsonSourceNameBefore, null, boundsPair.first)
                //getFeatureStringFromLll(lllhBefore, geoJsonSourceNameBefore, null, bounds)
            val sourceBefore = GeoJsonSource(geoJsonSourceNameBefore, jsonStringBefore)
            val lineLayerBefore = LineLayer("${geoJsonSourceNameBefore}_line_before", geoJsonSourceNameBefore)
                .withProperties(
                    PropertyFactory.lineColor(Color.BLUE),
                    //PropertyFactory.lineDasharray(arrayOf(1f, 2f)),
                    PropertyFactory.lineWidth(4f)
                )
            return Triple(listOf(lineLayerAfter, lineLayerBefore, lineLayerHaircrossVertical, lineLayerHaircrossHorizontal),
                listOf(sourceAfter, sourceBefore, sourceHaircrossVertical, sourceHaircrossHorizontal), boundsPair.second)
        }

        /**
         * return pair of LatLngBounds first: normal seconds: with border
         */
        fun getLocationsBoundsPair(
            lllhAfter: ArrayList<LatLngH>,
            lllhBefore: ArrayList<LatLngH>,
            border: Double,
        ) : Pair<LatLngBounds, LatLngBounds> {
            val llboundsBuilder: LatLngBounds.Builder = LatLngBounds.Builder()
            for (latLngH: LatLngH in lllhAfter)
                llboundsBuilder.include(LatLng(latLngH.latitude, latLngH.longitude))
            for (latLngH: LatLngH in lllhBefore)
                llboundsBuilder.include(LatLng(latLngH.latitude, latLngH.longitude))
            val bounds = llboundsBuilder.build()
            val latAdjustment = ((bounds.northEast.latitude - bounds.southWest.latitude) * border)
            val lngAdjustment = ((bounds.northEast.longitude - bounds.southWest.longitude) * border)
            val regionBoundsBuilder: LatLngBounds.Builder = LatLngBounds.Builder()
            regionBoundsBuilder.include(
                LatLng(
                    bounds.northEast.latitude + latAdjustment,
                    bounds.northEast.longitude + lngAdjustment
                )
            ).include(
                LatLng(
                    bounds.southWest.latitude - latAdjustment,
                    bounds.southWest.longitude - lngAdjustment
                )
            )
            return Pair(bounds, regionBoundsBuilder.build())
        }

        fun createInstructionsFromLllh(
            context: Context?,
            lllhOriginal: ArrayList<LatLngH>,
            name: String?
        ): java.util.ArrayList<AlminavInstruction?> {
            val lllh = lllhOriginal.reduceWithTolerance(200.0)
            if (lllh.isEmpty()) return arrayListOf()
            if (context != null) {
                val routeDistance = lllh.getDistanceFromLllh().toInt()

                var distM = 0
                val alminavInstructionList = java.util.ArrayList<AlminavInstruction?>()
                val startInstruction = AlminavInstruction(
                    context,
                    if (name != null) Instruction.START else Instruction.CONTINUE_ON_STREET,
                    name ?: Const.TAG_START,
                    "",
                    if (name != null) routeDistance else 0,
                    0,
                    lllh[0],
                    0f
                )
                alminavInstructionList.add(startInstruction)
                lllh.forEachIndexed { i, llh ->
                    distM += llh.legDistance.toInt()
                    if (llh.instructionText != null) {
                        if (llh.isTurn) {
                            val alminavInstruction = AlminavInstruction(
                                context,
                                llh.instructionSign,
                                llh.instructionName,
                                if (i < lllh.size - 1) lllh[i + 1].instructionName else Const.TAG_FINISH,
                                llh.legDistance.toInt(),
                                distM,
                                llh,
                                llh.distRatio
                            )
                            alminavInstructionList.add(alminavInstruction)
//                            Log.i(logtag,
//                                "${Thread.currentThread().getStackTrace()[2].lineNumber}: $i $alminavInstruction")
                        }
                    }
                }
                val finishInstruction = AlminavInstruction(
                    context, Instruction.FINISH, Const.TAG_FINISH, Const.TAG_FINISH,
                    0, 0, lllh[lllh.size - 1], 0f
                )
                alminavInstructionList.add(finishInstruction)
                return alminavInstructionList
            }
            return ArrayList()
        }

        fun getTileName(flat: Double, flon: Double): String {
            val ilatitude = (flat - 0.5f).roundToInt()
            val ilongitude = (flon - 0.5f).roundToInt()
            val slat: String = if (ilatitude < 0) String.format(
                    getDefault(),
                    "s%02d",
                    -ilatitude
                ) else String.format(
                    getDefault(), "n%02d", ilatitude
                )
            val slon: String = if (ilongitude < 0) String.format(
                    getDefault(),
                    "w%03d",
                    -ilongitude
                ) else String.format(
                    getDefault(), "e%03d", ilongitude
                )
            return String.format("%s%s", slat, slon)
        }

        /*
                @SuppressLint("DefaultLocale")
                fun createSimpleInstructions(context: Context, pointListH: ArrayList<LatLngH>): Int {
                    val ghManager = GhHelper.getGhManager(context)
                    var distRatio = 0.1
                    val distRoute = getDistanceFromLllh(pointListH)
                    var dist = 0.0
                    pointListH[0].distRatio = 0f
                    for (i in 1..<pointListH.size) {
                        val legDistance = SphericalUtil.computeDistanceBetween(
                            com.google.android.gms.maps.model.LatLng(pointListH[i - 1].latitude, pointListH[i - 1].longitude),
                            com.google.android.gms.maps.model.LatLng(pointListH[i].latitude, pointListH[i].longitude)
                        )
                        dist += legDistance
                        var sInstruction : String = context.getString(R.string.continue_on_street)
                        var instructionSign = 0
                        var angle = 0.0
                        if (i < pointListH.size - 1) {
                            var angle0 = SphericalUtil.computeHeading(
                                com.google.android.gms.maps.model.LatLng(
                                    pointListH[i-1].latitude,
                                    pointListH[i-1].longitude
                                ),
                                com.google.android.gms.maps.model.LatLng(
                                    pointListH[i].latitude,
                                    pointListH[i].longitude
                                )
                            )

                            var angle1 = SphericalUtil.computeHeading(
                                com.google.android.gms.maps.model.LatLng(
                                    pointListH[i].latitude,
                                    pointListH[i].longitude
                                ),
                                com.google.android.gms.maps.model.LatLng(
                                    pointListH[i+1].latitude,
                                    pointListH[i+1].longitude
                                )
                            )
                            angle = angle1 - angle0
                        }
                        if (angle < -180)
                            angle += 360
                        if (angle > 180)
                            angle -= 360
                        var isTurn = true
                        val pointDistRatio = min(1.0, dist / distRoute).toFloat()
                        //Timber.i(" $i angle:$angle")
                        when(angle) {
                            in -30.0..0.0 -> {sInstruction =
                                context.getString(R.string.continue_on_street); isTurn=true; instructionSign = Instruction.CONTINUE_ON_STREET}
                            in -60.0..-30.0 -> {sInstruction =
                                context.getString(R.string.turn_slight_left); isTurn=true; instructionSign = Instruction.TURN_SLIGHT_LEFT}
                            in -120.0..-60.0 -> {sInstruction =
                                context.getString(R.string.turn_left); isTurn=true; instructionSign = Instruction.TURN_LEFT}
                            in -120.0..-180.0 -> {sInstruction =
                                context.getString(R.string.turn_sharp_left); isTurn=true; instructionSign = Instruction.TURN_SHARP_LEFT}
                            in 0.0..30.0 -> {sInstruction =
                                context.getString(R.string.continue_on_street); isTurn=true; instructionSign = Instruction.CONTINUE_ON_STREET}
                            in 30.0..60.0 -> {sInstruction =
                                context.getString(R.string.turn_slight_right); isTurn=true; instructionSign = Instruction.TURN_SLIGHT_RIGHT}
                            in 60.0..120.0 -> {sInstruction =
                                context.getString(R.string.turn_right); isTurn=true; instructionSign = Instruction.TURN_RIGHT}
                            in 120.0..180.0 -> {sInstruction =
                                context.getString(R.string.turn_sharp_right); isTurn=true; instructionSign = Instruction.TURN_SHARP_RIGHT}
                        }

                        var instructionName = ghManager?.getClosestEdge(pointListH[i-1].latitude, pointListH[i-1].longitude)

        //                val streetName0 = ghManager?.getClosestEdge(pointListH[i-1].latitude, pointListH[i-1].longitude)
        //                val streetName1 = ghManager?.getClosestEdge(pointListH[i].latitude, pointListH[i].longitude)
        //                var streetName = streetName0
        //                if (streetName0.equals(streetName1).not())
        //                    streetName = "[${streetName0}/${streetName1}]"
        //                if (streetName.isNullOrEmpty().not()) {
        //                    instructionName =
        //                        streetName // [$text ${formatDistM(distRoute - dist, true)} ${(pointDistRatio*100).format(1)}%]"
        //                }

                        pointListH[i].distRatio = pointDistRatio
                        pointListH[i].legDistance = legDistance
        //                sInstruction = java.lang.String.format(
        //                    "%s %s", text,
        //                    textDist
        //                )
                        pointListH[i].instructionText = sInstruction
                        pointListH[i].instructionName = instructionName
                        pointListH[i].instructionSign = instructionSign
                        pointListH[i].isTurn = isTurn
                    }
        //            pointListH[pointListH.size - 1].distRatio = 1.0
        //            pointListH[pointListH.size - 1].instructionSign = Instruction.FINISH
        //            pointListH[pointListH.size - 1].instructionText = navigationEnd
                    return dist.toInt()
                }
         */
        fun getPoiDrawableMap(context: Context): Map<String, Pair<Int, Int>> {
            return mapOf(
                getEnString(context, R.string.locality) to Pair(
                    R.drawable.mx_village,
                    R.string.locality
                ),
                getEnString(context, R.string.street) to Pair(
                    R.drawable.s_street_small,
                    R.string.street
                ),
                getEnString(context, R.string.restaurant) to Pair(
                    R.drawable.s_food_small,
                    R.string.restaurant
                ),
                getEnString(context, R.string.cafe) to Pair(
                    R.drawable.s_cafe_small,
                    R.string.cafe
                ),
                getEnString(context, R.string.hotel) to Pair(
                    R.drawable.s_accommo_small,
                    R.string.hotel
                ),
                getEnString(context, R.string.supermarket) to Pair(
                    R.drawable.ic_supermarket,
                    R.string.supermarket
                ),
                getEnString(context, R.string.bakery) to Pair(
                    R.drawable.ic_bakery,
                    R.string.bakery
                ),
                getEnString(context, R.string.hospital) to Pair(
                    R.drawable.s_health_small,
                    R.string.hospital
                ),
                getEnString(context, R.string.pharmacy) to Pair(
                    R.drawable.s_health_small,
                    R.string.pharmacy
                ),
                getEnString(context, R.string.church) to Pair(
                    R.drawable.s_pow_small,
                    R.string.church
                ),
                getEnString(context, R.string.cemetery) to Pair(
                    R.drawable.mx_cemetery,
                    R.string.cemetery
                ),
                getEnString(context, R.string.fuel) to Pair(
                    R.drawable.ic_fuel,
                    R.string.fuel
                ),
                getEnString(context, R.string.attraction) to Pair(
                    R.drawable.s_tourist_small,
                    R.string.attraction
                ),
                getEnString(
                    context,
                    R.string.waterpark
                ) to Pair(R.drawable.mx_leisure_water_park, R.string.waterpark),
                getEnString(
                    context,
                    R.string.sportscentre
                ) to Pair(R.drawable.mx_sport_stadium, R.string.sportscentre),
                getEnString(
                    context,
                    R.string.parking_place
                ) to Pair(R.drawable.s_parking_place_small, R.string.parking_place),
                getEnString(context, R.string.school) to Pair(
                    R.drawable.mx_amenity_school,
                    R.string.school
                ),
                getEnString(
                    context,
                    R.string.airport
                ) to Pair(R.drawable.baseline_airplanemode_active_24, R.string.airport),
                getEnString(context, R.string.park) to Pair(
                    R.drawable.mx_park,
                    R.string.park
                ),
                getEnString(context, R.string.tower) to Pair(
                    R.drawable.s_tower_small,
                    R.string.tower
                ),
                getEnString(context, R.string.peaks) to Pair(
                    R.drawable.s_peak_small,
                    R.string.peaks
                ),
                getEnString(context, R.string.natural_lake) to Pair(
                    R.drawable.mx_water,
                    R.string.natural_lake
                ),
                getEnString(context, R.string.doctor) to Pair(
                    R.drawable.s_health_small,
                    R.string.doctor
                ),
                getEnString(context, R.string.address) to Pair(
                    R.drawable.s_address_small,
                    R.string.address
                ),
                getEnString(context, R.string.city) to Pair(
                    R.drawable.location_city_24px,
                    R.string.city
                ),
                getEnString(context, R.string.city10000) to Pair(
                    R.drawable.circle_red_20px,
                    R.string.city10000
                ),
                getEnString(context, R.string.city50000) to Pair(
                    R.drawable.circle_red_24px,
                    R.string.city50000
                ),
                getEnString(context, R.string.city100000) to Pair(
                    R.drawable.circle_red_24px,
                    R.string.city100000
                ),
                getEnString(context, R.string.city500000) to Pair(
                    R.drawable.circle_red_28px,
                    R.string.city500000
                ),
                getEnString(context, R.string.city1mio) to Pair(
                    R.drawable.circle_red_28px,
                    R.string.city1mio
                ),
                Const.TAG_START_NAVIGATION to Pair(
                    R.drawable.start_marker_24,
                    R.string.start_marker
                ),
                Const.TAG_STOP_NAVIGATION to Pair(R.drawable.circle_filled_red_24px, R.string.stop_marker),
                getEnString(context, R.string.turn) to Pair(R.drawable.ic_turn_24, R.string.turn),
                getEnString(
                    context,
                    R.string.current_turn
                ) to Pair(R.drawable.circle_filled_red_24px, R.string.current_turn)
            )
        }

        fun getPoiDrawableMapWithoutSpecials(context: Context): Map<String, Pair<Int, Int>> {
            return mapOf(
                getEnString(context, R.string.locality) to Pair(
                    R.drawable.mx_village,
                    R.string.locality
                ),
                getEnString(context, R.string.street) to Pair(
                    R.drawable.s_street_small,
                    R.string.street
                ),
                getEnString(context, R.string.restaurant) to Pair(
                    R.drawable.s_food_small,
                    R.string.restaurant
                ),
                getEnString(context, R.string.cafe) to Pair(
                    R.drawable.s_cafe_small,
                    R.string.cafe
                ),
                getEnString(context, R.string.hotel) to Pair(
                    R.drawable.s_accommo_small,
                    R.string.hotel
                ),
                getEnString(context, R.string.supermarket) to Pair(
                    R.drawable.ic_supermarket,
                    R.string.supermarket
                ),
                getEnString(context, R.string.bakery) to Pair(
                    R.drawable.ic_bakery,
                    R.string.bakery
                ),
                getEnString(context, R.string.hospital) to Pair(
                    R.drawable.s_health_small,
                    R.string.hospital
                ),
                getEnString(context, R.string.pharmacy) to Pair(
                    R.drawable.s_health_small,
                    R.string.pharmacy
                ),
                getEnString(context, R.string.church) to Pair(
                    R.drawable.s_pow_small,
                    R.string.church
                ),
                getEnString(context, R.string.cemetery) to Pair(
                    R.drawable.mx_cemetery,
                    R.string.cemetery
                ),
                getEnString(context, R.string.fuel) to Pair(
                    R.drawable.ic_fuel,
                    R.string.fuel
                ),
                getEnString(context, R.string.attraction) to Pair(
                    R.drawable.s_tourist_small,
                    R.string.attraction
                ),
                getEnString(
                    context,
                    R.string.waterpark
                ) to Pair(R.drawable.mx_leisure_water_park, R.string.waterpark),
                getEnString(
                    context,
                    R.string.sportscentre
                ) to Pair(R.drawable.mx_sport_stadium, R.string.sportscentre),
                getEnString(
                    context,
                    R.string.parking_place
                ) to Pair(R.drawable.s_parking_place_small, R.string.parking_place),
                getEnString(context, R.string.school) to Pair(
                    R.drawable.mx_amenity_school,
                    R.string.school
                ),
                getEnString(
                    context,
                    R.string.airport
                ) to Pair(R.drawable.baseline_airplanemode_active_24, R.string.airport),
                getEnString(context, R.string.park) to Pair(
                    R.drawable.mx_park,
                    R.string.park
                ),
                getEnString(context, R.string.tower) to Pair(
                    R.drawable.s_tower_small,
                    R.string.tower
                ),
                getEnString(context, R.string.peaks) to Pair(
                    R.drawable.s_peak_small,
                    R.string.peaks
                ),
                getEnString(context, R.string.natural_lake) to Pair(
                    R.drawable.mx_water,
                    R.string.natural_lake
                ),
                getEnString(context, R.string.doctor) to Pair(
                    R.drawable.s_health_small,
                    R.string.doctor
                ),
                getEnString(context, R.string.address) to Pair(
                    R.drawable.s_address_small,
                    R.string.address
                ),
                getEnString(context, R.string.city) to Pair(
                    R.drawable.location_city_24px,
                    R.string.city
                ),
                getEnString(context, R.string.city10000) to Pair(
                    R.drawable.circle_red_20px,
                    R.string.city10000
                ),
                getEnString(context, R.string.city50000) to Pair(
                    R.drawable.circle_red_20px,
                    R.string.city50000
                ),
                getEnString(context, R.string.city100000) to Pair(
                    R.drawable.circle_red_20px,
                    R.string.city100000
                ),
                getEnString(context, R.string.city500000) to Pair(
                    R.drawable.circle_red_20px,
                    R.string.city500000
                ),
                getEnString(context, R.string.city1mio) to Pair(
                    R.drawable.circle_red_20px,
                    R.string.city1mio
                )
            )
        }

        @SuppressLint("UseKtx")
        fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap? {
            val drawable: Drawable = ContextCompat.getDrawable(context, drawableId) ?: return null

            var width = drawable.intrinsicWidth
            var height = drawable.intrinsicHeight

            if (width <= 0 || height <= 0) {
                width = 512
                height = 512
            }

            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            return bitmap
        }

        fun getEnString(context: Context, id: Int): String {
            //val defaultString = context.getString(id)
            val newConfiguration: Configuration =
                with(Configuration(context.resources.configuration)) {
                    setLocale(Locale.ENGLISH)
                    this
                }

            val localizedContext = context.createConfigurationContext(newConfiguration)
            val lr = localizedContext.resources
            val enString = lr.getString(id)
            //Timber.i("$defaultString $enString")
            return enString
        }

        fun getTileName(latLng: LatLng): String {
            return getTileName(
                latLng.latitude,
                latLng.longitude
            )
        }

        fun writeLllh2KmlFile(
            listPoints: List<LatLngH>?,
            routeFilePath: String,
        ): Boolean {
            listPoints.let {
                val f: FileOutputStream
                val kmlfile = File(routeFilePath)
                try {
                    f = FileOutputStream(kmlfile)
                } catch (e1: FileNotFoundException) {
                    e1.message?.let { Timber.i(it) }
                    return false
                }
                val df = DecimalFormat("#0.00000")
                val dfele = DecimalFormat("#0")
                val symbols = DecimalFormatSymbols()
                symbols.decimalSeparator = '.'
                df.decimalFormatSymbols = symbols
                val xmlwriter = OutputStreamWriter(f)
                val serializer = Xml.newSerializer()
                val NS = "http://earth.google.com/kml/2.1"

                try {
                    serializer.setOutput(xmlwriter)
                    // Log.i("write2Kml", "startDocument");
                    serializer.startDocument("utf-8", true)

                    // Log.i("write2Kml", "setPrefix");
                    serializer.setPrefix("", NS)
                    serializer.text("\r\n")
                    serializer.startTag(NS, "kml")
                    serializer.text("\r\n")
                    serializer.startTag(NS, "Placemark")
                    serializer.text("\r\n")
                    serializer.startTag(NS, "Name")
                    serializer.text(kmlfile.name) // "GH Kml");
                    serializer.endTag(NS, "Name")
                    //
                    serializer.text("\r\n")
                    serializer.startTag(NS, "LineString")
                    serializer.text("\r\n")
                    serializer.startTag(NS, "coordinates")

                    var ws: String
                    serializer.text("\r\n")
                    Timber.i("route ${kmlfile.name} has ${it?.size} points")
                    for (i in listPoints?.indices!!) {
                        val lat2 = it?.get(i)?.latitude
                        val lon2 = it?.get(i)?.longitude
                        ws = (df.format(lon2) + "," + df.format(lat2) + ","
                                + dfele.format(it?.get(i)?.altitude ?: 0))
                        serializer.text(ws)
                        serializer.text("\r\n")
                    }

                    serializer.endTag(NS, "coordinates")
                    serializer.endTag(NS, "LineString")
                    serializer.endTag(NS, "Placemark")

                    serializer.endTag(NS, "kml")
                    serializer.endDocument()
                    xmlwriter.close()
                    Timber.i("route saved to %s", kmlfile.path)
                    return true
                } catch (e: IllegalArgumentException) {
                    e.message?.let { Timber.e(it) }
                } catch (e: IOException) {
                    e.message?.let { Timber.e(it) }
                } catch (e: IllegalStateException) {
                    e.message?.let { Timber.e(it) }
                }
                return false
            }
        }

        fun createRasterMapsLayers(context: Context): RasterMapsItems {
            val sharedPreferences =
                getDefaultSharedPreferences(context)
            val layerList = ArrayList<RasterLayer>()
            val sourceList = ArrayList<RasterSource>()
            var rasterMapFilePathSet =
                sharedPreferences.getStringSet(Const.PREF_MBTILES_FILEPATH_SET, null)
            val missingList = arrayListOf<String>()
            if (rasterMapFilePathSet.isNotNull()) {
                rasterMapFilePathSet?.forEach { path ->
                    val rasterMapFile = File(path.toString())
                    if (rasterMapFile.exists()) {
                        val id = rasterMapFile.name.replace(Const.MBTILES_EXT, "")
                        val rasterSource = RasterSource(id,
                            TileSet("tileset", "mbtiles://${rasterMapFile.absolutePath}"), 256)
                        val rasterLayer = RasterLayer("${id}-layer", id)
                        //if (rasterMapFilePathSet.size > 1)
                        rasterSource.maxOverscaleFactorForParentTiles = 0 // important for multiple raster layers
                        sourceList.add(rasterSource)
                        val result = layerList.add(rasterLayer)
                        Timber.i( "add RasterLayer ${rasterLayer.id}: $result ")
                    } else {
                        Timber.e("Not found: ${rasterMapFile.absolutePath}")
                        missingList.add(rasterMapFile.absolutePath)
                    }
                }
            }
            if (missingList.isNotEmpty()) {
                Timber.i(" missingList: ${missingList.size}")
                missingList.forEach { path ->
                    rasterMapFilePathSet = rasterMapFilePathSet?.minus(path)
                }
                sharedPreferences.edit {
                    putStringSet(
                        Const.PREF_MBTILES_FILEPATH_SET,
                        rasterMapFilePathSet
                    )
                }
            }
            val result = RasterMapsItems(sourceList, layerList)
            Timber.i( "createRasterMapsLayers result: ${result.rasterLayerList.size}")
            return RasterMapsItems(sourceList, layerList)
        }

        fun createCyclewayMapsLayers(context: Context): RasterMapsItems {
            val sharedPreferences =
                getDefaultSharedPreferences(context)
            val layerList = ArrayList<RasterLayer>()
            val sourceList = ArrayList<RasterSource>()
            val rasterMapFilePathSet =
                sharedPreferences.getStringSet(Const.PREF_CYCLEWAY_OVERLAYS_FILEPATH_SET, null)
            if (rasterMapFilePathSet.isNotNull()) {
                rasterMapFilePathSet?.forEach { path ->
                    val rasterMapFile = File(path.toString())
                    if (rasterMapFile.exists()) {
                        val id = rasterMapFile.name.replace(Const.MBTILES_EXT, "")
                        val rasterSource = RasterSource(
                            id,
                            TileSet("tileset", "mbtiles://${rasterMapFile.absolutePath}"), 256
                        )
                        val rasterLayer = RasterLayer("${id}-layer", id)
                        //if (rasterMapFilePathSet.size > 1)
                        rasterSource.maxOverscaleFactorForParentTiles =
                            0 // important for multiple raster layers
                        sourceList.add(rasterSource)
                        layerList.add(rasterLayer)
                    } else
                        Timber.e("Not found: ${rasterMapFile.absolutePath}")
                }
            }
            return RasterMapsItems(sourceList, layerList)
        }

        /**
         * 27okt2025 replaced by GeoJsonUtils.createRasterMapsBounds
         */
        fun createRasterMapsBounds(context: Context, finished: (String) -> Unit) {
            val jsonBuilder = java.lang.StringBuilder()
            jsonBuilder.append("{\"type\":\"FeatureCollection\",\"features\":[")
            jsonBuilder.append("\n")
            val names = getRasterRegionNames(context)
            names.forEachIndexed { index, name ->
                val splits = name.split(Const.UNDERLINE, ".", limit = 6)
/*
                0 = "tile"
                1 = "1082"
                2 = "672"
                3 = "11"
                4 = "OpenTopo"
                5 = "mbtiles"
*/
                if (splits.size > 3) {
                    val mapType: String = splits[4]
                    val bounds = GeoJsonUtils.tileToGmsBounds(
                        GeoJsonUtils.Companion.Tile(
                            splits[1].toInt(),
                            splits[2].toInt(),
                            splits[3].toInt()
                        )
                    )
                    val lllh: ArrayList<LatLngH> = arrayListOf()
                    val boundsBuilder = org.maplibre.android.geometry.LatLngBounds.Builder()
                    lllh.add(LatLngH(bounds.southwest.latitude, bounds.southwest.longitude))
                    boundsBuilder.include(
                        LatLng(
                            bounds.southwest.latitude,
                            bounds.southwest.longitude
                        )
                    )
                    lllh.add(LatLngH(bounds.northeast.latitude, bounds.southwest.longitude))
                    boundsBuilder.include(
                        LatLng(
                            bounds.northeast.latitude,
                            bounds.southwest.longitude
                        )
                    )
                    lllh.add(LatLngH(bounds.northeast.latitude, bounds.northeast.longitude))
                    boundsBuilder.include(
                        LatLng(
                            bounds.northeast.latitude,
                            bounds.northeast.longitude
                        )
                    )
                    lllh.add(LatLngH(bounds.southwest.latitude, bounds.northeast.longitude))
                    boundsBuilder.include(
                        LatLng(
                            bounds.southwest.latitude,
                            bounds.northeast.longitude
                        )
                    )
                    lllh.add(LatLngH(bounds.southwest.latitude, bounds.southwest.longitude))
                    boundsBuilder.include(
                        LatLng(
                            bounds.southwest.latitude,
                            bounds.southwest.longitude
                        )
                    )
                    val featureString = lllh.createFeatureString(name, mapType, boundsBuilder.build())
                        //getFeatureStringFromLll(lllh, name, mapType, boundsBuilder.build())
                    Timber.i("featureString: $featureString")
                    jsonBuilder.append(featureString)
                    if (index < names.size - 1)
                        jsonBuilder.append(',')
                    jsonBuilder.append("\n")
                }
            }
            jsonBuilder.append("]}")
            val rootMapsFolder = File(context.filesDir, Const.ROUTEFOLDER)
            rootMapsFolder.mkdirs()
            val fileGeojson = File(rootMapsFolder, "raster_boundaries${Const.GEOJSON_EXT}")
            val fos = FileOutputStream(fileGeojson)
            val writer = OutputStreamWriter(fos, StandardCharsets.UTF_8)
            writer.write(jsonBuilder.toString())
            writer.flush()
            writer.close()
            Timber.i("geojson ready: ${fileGeojson.path}")
            finished(fileGeojson.path)
        }

        fun getPrefRasterMapType(context: Context): String? {
            val prefs = getDefaultSharedPreferences(context)
            return prefs.getString(context.getString(R.string.pref_tilemaker_maptype), Const.OUTDOOR)
        }

        fun saveLocations(context: Context, fromTime: Long, invokeOnCompletion: (resultMessage: String) -> Unit) {
            @SuppressLint("SimpleDateFormat") val timeFormat =
                SimpleDateFormat(Const.TIME_PATTERN_LONG_YEAR)
            val trackNameTemplate = java.lang.String.format(
                getDefault(), "%s_%s%s", "track",
                timeFormat.format(System.currentTimeMillis()), Const.GPX_EXT
            )
            Timber.i( "from time %s", timeFormat.format(fromTime))
            val routesRootFolder = File(context.filesDir, Const.ROUTEFOLDER)
            val trackFolder = File(routesRootFolder, Const.TRACKFOLDER)
            val b = trackFolder.mkdirs()
            Timber.i( "mkdirs: $b")
            val gpxFile = File(trackFolder.path, trackNameTemplate)
            var resultMessage = context.getString(R.string.gpslog_saved_to, gpxFile.name)
            trackFolder.mkdir()
            val locationRepository =
                LocationRepository.getInstance(context, Executors.newSingleThreadExecutor())
            var locationEntities: List<LocationEntity>? = null
            CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
                locationEntities = locationRepository.getLocationsAscFromTime(fromTime) //.getLocationsAsc()
                if (locationEntities.isNotEmpty()) {
                    writeGpxFileFromLocationEntities(
                        locationEntities,
                        gpxFile
                    )
                }
            }.invokeOnCompletion {
                resultMessage = if (!locationEntities.isNullOrEmpty()) {
                    resultMessage.plus(" [" + locationEntities.size + "]")
                } else
                    "no locations"
                invokeOnCompletion(resultMessage)
            }
        }

        fun convertTimeToLong(hour: Int, minute: Int, selectedDate: LocalDate?): Long? {
            selectedDate?.let {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.DATE, selectedDate.dayOfMonth)
                    set(Calendar.MONTH, selectedDate.monthValue - 1)
                    set(Calendar.YEAR, selectedDate.year)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return calendar.timeInMillis
            }
            return null
        }

        /**
         * Deletes all files in a folder that were modified before a certain date.
         *
         * @param folder The directory to clean.
         * @param timestamp The time in milliseconds. Files modified before this time will be deleted.
         * @param recursive Whether to also clean sub-folders.
         * @return The number of files deleted.
         */
        fun deleteFilesOlderThan(folder: File, timestamp: Long, recursive: Boolean = false): Int {
            var count = 0
            if (folder.exists() && folder.isDirectory) {
                folder.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        if (recursive) {
                            count += deleteFilesOlderThan(file, timestamp, recursive)
                            // Optionally delete empty directory if it's older than timestamp too?
                            // Or just delete it if it's empty after cleaning.
                            if (file.listFiles()?.isEmpty() == true) {
                                file.delete()
                            }
                        }
                    } else if (file.lastModified() < timestamp) {
                        if (file.delete()) {
                            count++
                        }
                    }
                }
            }
            return count
        }
    }
}