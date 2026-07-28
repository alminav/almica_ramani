package com.almica.ramani.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.almica.ramani.BuildConfig
import com.almica.ramani.Const
import com.almica.ramani.FeatureProperties.Companion.AERODROME
import com.almica.ramani.FeatureProperties.Companion.AEROWAY
import com.almica.ramani.FeatureProperties.Companion.AMENITY
import com.almica.ramani.FeatureProperties.Companion.ATTRACTION
import com.almica.ramani.FeatureProperties.Companion.BAKERY
import com.almica.ramani.FeatureProperties.Companion.BARE_ROCK
import com.almica.ramani.FeatureProperties.Companion.BEACH
import com.almica.ramani.FeatureProperties.Companion.BUILDING
import com.almica.ramani.FeatureProperties.Companion.CATHEDRAL
import com.almica.ramani.FeatureProperties.Companion.CEMETERY
import com.almica.ramani.FeatureProperties.Companion.CHAPEL
import com.almica.ramani.FeatureProperties.Companion.CHURCH
import com.almica.ramani.FeatureProperties.Companion.CITY
import com.almica.ramani.FeatureProperties.Companion.COASTLINE
import com.almica.ramani.FeatureProperties.Companion.COMMERCIAL
import com.almica.ramani.FeatureProperties.Companion.CONVENIENCE
import com.almica.ramani.FeatureProperties.Companion.DESCRIPTION
import com.almica.ramani.FeatureProperties.Companion.FARMLAND
import com.almica.ramani.FeatureProperties.Companion.FARMYARD
import com.almica.ramani.FeatureProperties.Companion.FOREST
import com.almica.ramani.FeatureProperties.Companion.FUEL
import com.almica.ramani.FeatureProperties.Companion.GARDEN
import com.almica.ramani.FeatureProperties.Companion.GOVERNMENT
import com.almica.ramani.FeatureProperties.Companion.GRASS
import com.almica.ramani.FeatureProperties.Companion.GRASSLAND
import com.almica.ramani.FeatureProperties.Companion.GREEN_CIRCLE_FILLED
import com.almica.ramani.FeatureProperties.Companion.HAMLET
import com.almica.ramani.FeatureProperties.Companion.HIGHWAY
import com.almica.ramani.FeatureProperties.Companion.HOSPITAL
import com.almica.ramani.FeatureProperties.Companion.HOTEL
import com.almica.ramani.FeatureProperties.Companion.INDUSTRIAL
import com.almica.ramani.FeatureProperties.Companion.LAKE
import com.almica.ramani.FeatureProperties.Companion.LANDFILL
import com.almica.ramani.FeatureProperties.Companion.LANDUSE
import com.almica.ramani.FeatureProperties.Companion.LEISURE
import com.almica.ramani.FeatureProperties.Companion.LINE_TAG
import com.almica.ramani.FeatureProperties.Companion.LIVING_STREET
import com.almica.ramani.FeatureProperties.Companion.MAN_MADE
import com.almica.ramani.FeatureProperties.Companion.MEADOW
import com.almica.ramani.FeatureProperties.Companion.MILITARY
import com.almica.ramani.FeatureProperties.Companion.NAME
import com.almica.ramani.FeatureProperties.Companion.NATURAL
import com.almica.ramani.FeatureProperties.Companion.OFFICE
import com.almica.ramani.FeatureProperties.Companion.PARKING
import com.almica.ramani.FeatureProperties.Companion.PEAK
import com.almica.ramani.FeatureProperties.Companion.PHARMACY
import com.almica.ramani.FeatureProperties.Companion.PICNIC_TABLE
import com.almica.ramani.FeatureProperties.Companion.PLACE
import com.almica.ramani.FeatureProperties.Companion.RAIL
import com.almica.ramani.FeatureProperties.Companion.RAILWAY
import com.almica.ramani.FeatureProperties.Companion.RECREATION_GROUND
import com.almica.ramani.FeatureProperties.Companion.RED_CIRCLE_FILLED
import com.almica.ramani.FeatureProperties.Companion.RESIDENTIAL
import com.almica.ramani.FeatureProperties.Companion.RESTAURANT
import com.almica.ramani.FeatureProperties.Companion.RETAIL
import com.almica.ramani.FeatureProperties.Companion.SAND
import com.almica.ramani.FeatureProperties.Companion.SCHOOL
import com.almica.ramani.FeatureProperties.Companion.SCHOOL_OUTLINED
import com.almica.ramani.FeatureProperties.Companion.SCRUB
import com.almica.ramani.FeatureProperties.Companion.SHOP
import com.almica.ramani.FeatureProperties.Companion.SPORTS_CENTRE
import com.almica.ramani.FeatureProperties.Companion.STATE
import com.almica.ramani.FeatureProperties.Companion.SUPERMARKET
import com.almica.ramani.FeatureProperties.Companion.TOURISM
import com.almica.ramani.FeatureProperties.Companion.TOWER
import com.almica.ramani.FeatureProperties.Companion.TOWN
import com.almica.ramani.FeatureProperties.Companion.UNDERLINE
import com.almica.ramani.FeatureProperties.Companion.VILLAGE
import com.almica.ramani.FeatureProperties.Companion.VILLAGE_GREEN
import com.almica.ramani.FeatureProperties.Companion.WAREHOUSE
import com.almica.ramani.FeatureProperties.Companion.WATER
import com.almica.ramani.FeatureProperties.Companion.WATERWAY
import com.almica.ramani.FeatureProperties.Companion.WATER_PARK
import com.almica.ramani.FeatureProperties.Companion.WETLAND
import com.almica.ramani.FeatureProperties.Companion.WOOD
import com.almica.ramani.Helpers
import com.almica.ramani.Helpers.Companion.copyStreamToFile
import com.almica.ramani.Helpers.Companion.getPrefRasterMapType
import com.almica.ramani.LatLngH
import com.almica.ramani.R
import com.almica.ramani.StyleExpressionsJson
import com.almica.ramani.geojsonMaps.GeojsonMapEntity
import com.almica.ramani.geojsonMaps.GeojsonMapRepository
import com.almica.ramani.getRasterRegionNames
import com.almica.ramani.getRegionName
import com.almica.ramani.readToString
import com.almica.ramani.routes.RouteEntity
import com.almica.ramani.routes.RouteRepository
import com.almica.ramani.turf.TurfMeasurement
import com.almica.ramani.turf.TurfMeta
import com.almica.ramani.utils.RouteSmoothingUtil.simplifyToTargetCount
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.expressions.Expression.Converter
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.GeometryAdapterFactory
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiLineString
import org.maplibre.geojson.MultiPolygon
import org.maplibre.geojson.Point
import org.maplibre.geojson.gson.GeoJsonAdapterFactory
import timber.log.Timber
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStreamReader
import java.net.URI
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

private const val logtag = "GeoJsonUtils"
data class GeoJsonComponents(val geoJsonSources: ArrayList<GeoJsonSource>,
                             val geoJsonLineLayers: ArrayList<LineLayer>,
                             val geoJsonSymbolLayers: ArrayList<SymbolLayer>,
                             val geoJsonFillLayers: ArrayList<FillLayer>)
class GeoJsonUtils {
    companion object {
        fun getFeatureCenter(feature: Feature): LatLng {
            if (feature.geometry() is Point)
                return LatLng((feature.geometry() as Point).latitude(),
                    (feature.geometry() as Point).longitude())
            else {
                val bbox = TurfMeasurement.bbox(feature)
                val p1 = com.google.android.gms.maps.model.LatLng(bbox[1], bbox[0])
                val p2 = com.google.android.gms.maps.model.LatLng(bbox[3], bbox[2])
                val gmsLatLng = SphericalUtil.interpolate(p1, p2, 0.5)
                return LatLng(gmsLatLng.latitude, gmsLatLng.longitude)
            }
        }

        fun getLllhFromGeometry(geometry: org.maplibre.geojson.Geometry?): ArrayList<LatLngH> {
            val lllh = ArrayList<LatLngH>()
            when (geometry) {
                is LineString -> {
                    geometry.coordinates().forEach { point ->
                        lllh.add(LatLngH(point.latitude(), point.longitude(), point.altitude()))
                    }
                }

                is MultiLineString -> {
                    geometry.coordinates().forEach { line ->
                        line.forEach { point ->
                            lllh.add(LatLngH(point.latitude(), point.longitude(), point.altitude()))
                        }
                    }
                }
            }
            return lllh
        }

        fun createFilterFillColor(): Pair<Expression?, Expression?> {
            // Create from JSON array string
            val e = Converter.convert(StyleExpressionsJson.FILTER_COLOR_FILL_LAYER)
            val o = Converter.convert(StyleExpressionsJson.FILTER_OPACITY_FILL_LAYER)
            return Pair(e, o)
        }

        fun createFilterLineColor(): Pair<Expression?, Expression?> {
            val colorExpression = Converter.convert(StyleExpressionsJson.FILTER_COLOR_LINE_LAYER)
            val widthOuterExpression = Converter.convert(StyleExpressionsJson.FILTER_WIDTH_LINE_LAYER)
            return Pair(colorExpression, widthOuterExpression)
        }

        fun createFilterSymbolIcons(): Pair<Expression?, Expression?> {
            val imageExpression = Converter.convert(StyleExpressionsJson.FILTER_IMAGE_SYMBOL_LAYER)
            val nameExpression = Converter.convert(StyleExpressionsJson.FILTER_NAME_SYMBOL_LAYER)
            return Pair(imageExpression, nameExpression)
        }

        fun createGeojsonLineLayer(
            colorFilter: Pair<Expression?, Expression?>,
            sourceName: String,
            minZoom: Float,
        ): LineLayer {
            /**
             * Capturing from the style-spec that lineDasharray property isn't data driven:
             *
             * https://www.mapbox.com/mapbox-gl-js/style-spec/#paint-line-line-dasharray
             * You will have to work around this by using a layer for each track_type and filtering out other data in that layer.
              */
//            val defaultFilter = Expression.not(match(Expression.get(HIGHWAY),
//                Expression.literal(FOOTWAY), Expression.literal(PEDESTRIAN),
//                Expression.literal(TRACK), Expression.literal(PATH)))
            val layerId = sourceName + LINE_TAG
            val geoJsonLineLayer = LineLayer(layerId, sourceName).withProperties(
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                //PropertyFactory.lineWidth(colorFilter.second),
                PropertyFactory.lineWidth(Expression.interpolate(
                    // Set the interpolation type
                    Expression.linear(
                        //Expression.literal(1.75)
                    ),
                    // Get current zoom level
                    Expression.zoom(),
                    // If the map is at zoom level 12 or below,
                    // set circle radius to 2
                    Expression.stop (
                        Expression.literal(11),
                        Expression.literal(1)
                    ),
                    // If the map is at zoom level 22 or above,
                    // set circle radius to 180
                    Expression.stop (
                        Expression.literal(16),
                        Expression.number(colorFilter.second)
                    )
                )),
                PropertyFactory.lineColor(colorFilter.first))

//            geoJsonLineLayer.setFilter(defaultFilter)
            geoJsonLineLayer.minZoom = minZoom
            return geoJsonLineLayer
        }

        fun createGeojsonFillLayer(
            colorFilter: Pair<Expression?, Expression?>,
            sourceName: String,
            minZoom: Float,
        ): FillLayer {
            val layerId = sourceName + "_fill"
            //Timber.i( "layerId $layerId")
            val polygonFillLayer = FillLayer(layerId, sourceName)
            polygonFillLayer.setProperties(
                PropertyFactory.fillColor(colorFilter.first),
                PropertyFactory.fillOpacity(colorFilter.second)
            )
            //polygonFillLayer.setFilter(Expression.gt(Expression.literal(minZoom), Expression.zoom()))
            polygonFillLayer.minZoom = minZoom
            return polygonFillLayer
        }
/*
        fun createGeojsonFillLayer(
            color: Int,
            filterName: String,
            filterValue: String,
            sourceName: String,
            minZoom: Float,
        ): FillLayer {
            val layerId = sourceName + "_fill_" + filterName + UNDERLINE + filterValue
            //Timber.i( "layerId $layerId")
            val polygonFillLayer = FillLayer(layerId, sourceName)
            polygonFillLayer.setProperties(
                PropertyFactory.fillColor(color),
                PropertyFactory.fillOpacity(.9f)
            )
//        forestPolygonFillLayer.setFilter(eq(literal("\$type"), literal("Polygon")))
            //polygonFillLayer.setFilter(Expression.gt(Expression.literal(9), Expression.zoom()))
            polygonFillLayer.setFilter(
                Expression.eq(
                    Expression.literal(filterName),
                    Expression.literal(filterValue)
                )
            )
            polygonFillLayer.minZoom = minZoom
            return polygonFillLayer
        }

        fun createGeojsonFillLayer(
            color: Int,
            filterName: String,
            filterValue: String,
            sourceName: String,
            minZoom: Float,
        ): Pair<FillLayer, FillLayer> {
            val layerId1 = sourceName + "_fill1_" + filterName + UNDERLINE + filterValue
            //Timber.i( "layerId $layerId")
            val polygonFillLayer1 = FillLayer(layerId1, sourceName)
            polygonFillLayer1.setProperties(
                PropertyFactory.fillColor(color),
                PropertyFactory.fillOpacity(.9f)
            )
            val layerId2 = sourceName + "_fill2_" + filterName + UNDERLINE + filterValue
            //Timber.i( "layerId $layerId")
            val polygonFillLayer2 = FillLayer(layerId2, sourceName)
            polygonFillLayer2.setProperties(
                //PropertyFactory.fillColor(color),
                PropertyFactory.fillColor(Expression.toColor(Expression.get(COLOR_PROPERTY_KEY))),
                PropertyFactory.fillOpacity(.9f)
            )
            polygonFillLayer2.setFilter(
                Expression.not(Expression.has(COLOR_PROPERTY_KEY)))

            polygonFillLayer1.setFilter(
                Expression.eq(
                    Expression.literal(filterName),
                    Expression.literal(filterValue)
                )
            )
            polygonFillLayer1.minZoom = minZoom
            polygonFillLayer2.minZoom = minZoom
            return Pair(polygonFillLayer2, polygonFillLayer1)
        }
*/
        /**
         * 09Nov2025
         * It appears there are predefined images in maplibre.
         * If the name is identical, e.g., 'school', these are used.
         */
        fun createSymbolImageList(): ArrayList<Pair<String, Int>> {
            val imageList = ArrayList<Pair<String, Int>>()
            imageList.add(Pair(CHURCH, R.drawable.outline_church_24))
            imageList.add(Pair(RESTAURANT, R.drawable.ic_category_eat))
            imageList.add(Pair(HOTEL, R.drawable.mx_tourism_hotel))
            imageList.add(Pair(PARKING, R.drawable.ic_category_parking))
            imageList.add(Pair(PHARMACY, R.drawable.ic_category_pharmacy))
            imageList.add(Pair(FUEL, R.drawable.ic_category_fuel))
            imageList.add(Pair(SHOP, R.drawable.outline_shopping_bag_24))
            imageList.add(Pair(CITY, R.drawable.mx_place_city))
            imageList.add(Pair(TOWN, R.drawable.mx_place_town))
            imageList.add(Pair(VILLAGE, R.drawable.mm_village))
            imageList.add(Pair(PEAK, R.drawable.mm_natural_peak))
            imageList.add(Pair(BAKERY, R.drawable.mx_shop_bakery))
            imageList.add(Pair(SUPERMARKET, R.drawable.outline_shopping_cart_24))
            imageList.add(Pair(PICNIC_TABLE, R.drawable.mx_picnic_table))
            imageList.add(Pair(TOWER, R.drawable.mx_man_made_tower))
            imageList.add(Pair(ATTRACTION, R.drawable.mx_tourism_attraction))
            imageList.add(Pair(CEMETERY, R.drawable.mx_cemetery))
            imageList.add(Pair(LAKE, R.drawable.mx_water))
            imageList.add(Pair(HOSPITAL, R.drawable.outline_local_hospital_24))
            imageList.add(Pair(SCHOOL_OUTLINED, R.drawable.outline_school_24))
            imageList.add(Pair(SPORTS_CENTRE, R.drawable.outline_sports_and_outdoors_24))
            imageList.add(Pair(WATER_PARK, R.drawable.outline_water_24))
            imageList.add(Pair(LIVING_STREET, R.drawable.s_street_extra_small))
            imageList.add(Pair(RED_CIRCLE_FILLED, R.drawable.circle_filled_red_24px))
            imageList.add(Pair(GREEN_CIRCLE_FILLED, R.drawable.circle_filled_green_24px))
            return imageList
        }

        fun createGeojsonSymbolLayer(
            expression: Pair<Expression?, Expression?>,
            sourceName: String,
            minZoom: Float
        ): SymbolLayer {
            val layerId = sourceName+"_symbol"
            val geoJsonSymbolLayer = SymbolLayer(layerId, sourceName)
            //Expression.image()
            //val name = Expression.toString(Expression.get("name"))
            geoJsonSymbolLayer.setProperties(
                PropertyFactory.iconImage(expression.first),
                PropertyFactory.iconSize(Expression.interpolate(
                    // Set the interpolation type
                    Expression.linear(
                        //Expression.literal(1.75)
                    ),
                    // Get current zoom level
                    Expression.zoom(),
                    // If the map is at zoom level 12 or below,
                    // set circle radius to 2
                    Expression.stop (
                        Expression.literal(11),
                        Expression.literal(0.5)
                    ),
                    Expression.stop (
                        Expression.literal(14),
                        Expression.literal(1.5)
                    ),
                    // If the map is at zoom level 22 or above,
                    // set circle radius to 180
                    Expression.stop (
                        Expression.literal(16),
                        Expression.literal(3)
                    )
                )),
                PropertyFactory.textField(expression.second),
                PropertyFactory.textSize(Expression.interpolate(
                    // Set the interpolation type
                    Expression.linear(
                        //Expression.literal(1.75)
                    ),
                    // Get current zoom level
                    Expression.zoom(),
                    // If the map is at zoom level 12 or below,
                    // set circle radius to 2
                    Expression.stop (
                        Expression.literal(13),
                        Expression.literal(0)
                    ),
                    Expression.stop (
                        Expression.literal(14),
                        Expression.literal(15)
                    ),
                    // If the map is at zoom level 22 or above,
                    // set circle radius to 180
                    Expression.stop (
                        Expression.literal(16),
                        Expression.literal(20)
                    )
                )),
                PropertyFactory.textColor(Color.BLACK),
                PropertyFactory.textIgnorePlacement(true),
                PropertyFactory.textAllowOverlap(true)
            )

            geoJsonSymbolLayer.minZoom = minZoom
            return geoJsonSymbolLayer
        }

        fun createGeojsonSymbolLayer(
            filterName: String,
            filterValue: String,
            sourceName: String,
            minZoom: Float,
            iconName: String
        ): SymbolLayer {

            val layerId = sourceName+"_symbol_"+filterName+ UNDERLINE +filterValue
            val geoJsonSymbolLayer = SymbolLayer(layerId, sourceName)
            geoJsonSymbolLayer.setFilter(
                Expression.eq(
                    Expression.literal(filterName),
                    Expression.literal(filterValue)
                )
            )
            //Expression.image()
            val name = Expression.toString(Expression.get("name"))
            geoJsonSymbolLayer.setProperties(
                PropertyFactory.iconImage(iconName),
                PropertyFactory.textField(name),
                PropertyFactory.textSize(12F),
                PropertyFactory.textColor(Color.BLACK),
                PropertyFactory.textIgnorePlacement(true),
                PropertyFactory.textAllowOverlap(true)
            )

            geoJsonSymbolLayer.minZoom = minZoom
            return geoJsonSymbolLayer
        }

        /**
         * replaces Helpers.createGeojsonFromRoutesDatabase 27okt2025
         */
        fun createGeojsonFromRoutesDatabase(context: Context, finished: () -> Unit) {
            val hexColors = listOf(
                "gray",
                "maroon",
                "red",
                "purple",
                "green",
                "lime",
                "olive",
                "yellow",
                "navy",
                "blue",
                "teal",
                "aqua",
                "#FF7F27",
                "#808080",
                "#000000",
                "#FF0000",
                "#800000",
                "#FFFF00",
                "#808000",
                "#00FF00",
                "#008000",
                "#00FFFF",
                "#008080",
                "#0000FF",
                "#000080",
                "#FF00FF",
                "#800080"
            )
            var color = 0
            val features = arrayListOf<Feature>()
            val routeRepository = RouteRepository.getInstance(context, Executors.newSingleThreadExecutor())
            routeRepository.getAllSimple { routes ->
                Timber.i( "routes: ${routes.size}")
                routes.forEachIndexed { _, route ->
                    val pointList = arrayListOf<Point>()
                    val lllhRaw = route.kmlString.kmlString2Lllh()
                    if (lllhRaw.isNotEmpty()) {
                        val lllh = lllhRaw.reduceWithTolerance(200.0)
                        lllh.forEach { llh ->
                            pointList.add(Point.fromLngLat(llh.longitude, llh.latitude, llh.altitude))
                        }
                        val textDist = route.distance.formatDistM(true)
                        val linestringFeature = Feature.fromGeometry(LineString.fromLngLats(pointList))
                        linestringFeature.addStringProperty(
                            "name",
                            route.name.replace(Const.JPG_EXT, "")
                                .replace(Const.GPX_EXT, "").replace(Const.KML_EXT, "")
                        )
                        color += 1
                        if (color >= hexColors.size)
                            color = 0
                        linestringFeature.addStringProperty("color", hexColors[color])
                        linestringFeature.addStringProperty("region", route.region)
                        linestringFeature.addStringProperty("distance", textDist)
                        linestringFeature.addStringProperty("latitude", lllh[0].latitude.format(4))
                        linestringFeature.addStringProperty("longitude", lllh[0].longitude.format(4))
                        linestringFeature.addStringProperty("distance", textDist)
                        features.add(linestringFeature)
                    }
                }
                val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                val fileGeojson = File(rootRouteFolder, "routes${Const.GEOJSON_EXT}")
                val featureCollection = FeatureCollection.fromFeatures(features)
                fileGeojson.writeText(featureCollection.toJson())
                Timber.i("routes geojson ready: ${fileGeojson.path}")
                finished()
            }
        }

        /**
         * replaces Helpers.createGeojsonFromRoutes 27okt2025
         * elevation data is NOT included
         */
        fun createGeojsonFromRoutes(routeFolder: File, targetFile: File) {
            val hexColors = listOf(
                "maroon",
                "red",
                "purple",
                "green",
                "lime",
                "olive",
                "yellow",
                "navy",
                "blue",
                "teal",
                "aqua",
                "#FF7F27",
                "#FFFF00",
                "#808000",
                "#00FF00",
                "#008000",
                "#00FFFF",
                "#008080",
                "#0000FF",
                "#000080",
                "#FF00FF",
                "#800080"
            )
            //hexColors.shuffle() // only with arrayListOf
            Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}:")

            val files = ArrayList<File>()
            val fileTreeIterator = routeFolder.walkTopDown().iterator()
            while (fileTreeIterator.hasNext()) {
                val file = fileTreeIterator.next()
                if (!file.isDirectory &&
                    (file.parent != null && !file.parent!!.endsWith("tracks", true)) &&
                    (file.name.lowercase().endsWith(Const.KML_EXT) ||
                            file.name.lowercase().endsWith(Const.GPX_EXT) ||
                            file.name.lowercase().endsWith(Const.JPG_EXT))) {
                    files.add(file)
                }
            }
            var color_ = 0
            val features = arrayListOf<Feature>()
            Timber.i( "files: ${files.size}")
            files.forEachIndexed { index, file ->
                val pointList = arrayListOf<Point>()
                val lllhRaw = Helpers.getLllhFromFile(file)
                if (!lllhRaw.isNullOrEmpty()) {
                    val lllh = lllhRaw.reduceWithTolerance(200.0)
                    lllh.forEach { llh ->
                        pointList.add(Point.fromLngLat(llh.longitude, llh.latitude, llh.altitude))
                    }
                    val distM = lllh.getDistanceFromLllh()
                    val textDist = distM.formatDistM(true)
                    val linestringFeature = Feature.fromGeometry(LineString.fromLngLats(pointList))
                    linestringFeature.addStringProperty(
                        "name",
                        file.name.replace(Const.JPG_EXT, "")
                            .replace(Const.GPX_EXT, "").replace(Const.KML_EXT, "")
                    )
                    color_ += 1
                    if (color_ >= hexColors.size)
                        color_ = 0
                    linestringFeature.addStringProperty("color", hexColors[color_])
                    linestringFeature.addStringProperty("region", file.parentFile?.name)
                    linestringFeature.addStringProperty("distance", textDist)
                    linestringFeature.addStringProperty("latitude", lllh[0].latitude.format(4))
                    linestringFeature.addStringProperty("longitude", lllh[0].longitude.format(4))
                    linestringFeature.addStringProperty("distance", textDist)
                    features.add(linestringFeature)
                }
            }
            val featureCollection = FeatureCollection.fromFeatures(features)
            targetFile.writeText(featureCollection.toJson())
            Timber.i(
                "routes geojson ready: ${targetFile.path}")
        }
        fun createGeojsonFromRouteSnapshots(context: Context, routeFolder: File, targetFile: File) {
            val hexColors = listOf(
                "maroon",
                "red",
                "purple",
                "green",
                "lime",
                "olive",
                "yellow",
                "navy",
                "blue",
                "teal",
                "aqua",
                "#FF7F27",
                "#FFFF00",
                "#808000",
                "#00FF00",
                "#008000",
                "#00FFFF",
                "#008080",
                "#0000FF",
                "#000080",
                "#FF00FF",
                "#800080"
            )
            Timber.i( "routeFolder: ${routeFolder.path}")

            val files = routeFolder.listFiles()?.distinctBy { it.nameWithoutExtension }
            var color_ = 0
            val features = arrayListOf<Feature>()
            Timber.i( "files: ${files?.size}")
            files?.forEach { file ->
                val snapshotFile = File(file.parentFile, file.nameWithoutExtension + Const.JPG_EXT)
                val thumbnailFolder = File(context.filesDir, Const.THUMBNAILS)
                val thumbnailFile = File(thumbnailFolder, file.nameWithoutExtension + Const.JPG_EXT)
                if (snapshotFile.exists() || thumbnailFile.exists()) {
                    val pointList = arrayListOf<Point>()
                    val lllh = Helpers.getLllhFromFile(file)?.simplifyToTargetCount(60) as ArrayList<LatLngH>
                    // geojson string should not become too large
                    // Exif string length maximum EXIF_MAX_SIZE = 64 * 1024
                    if (lllh.isNotEmpty()) {
                        //val lllh = lllhRaw.reduceWithTolerance(200.0)
                        lllh.forEach { llh ->
                            pointList.add(
                                Point.fromLngLat(
                                    llh.longitude,
                                    llh.latitude,
                                    llh.altitude
                                )
                            )
                        }
                        val distM = lllh.getDistanceFromLllh()
                        val textDist = distM.formatDistM(true)
                        val linestringFeature =
                            Feature.fromGeometry(LineString.fromLngLats(pointList))
                        linestringFeature.addStringProperty("name", file.nameWithoutExtension)
                        color_ += 1
                        if (color_ >= hexColors.size)
                            color_ = 0
                        linestringFeature.addStringProperty("color", hexColors[color_])
                        linestringFeature.addStringProperty("region", file.parentFile?.name)
                        linestringFeature.addStringProperty("distance", textDist)
                        linestringFeature.addStringProperty("latitude", lllh[0].latitude.format(4))
                        linestringFeature.addStringProperty("longitude", lllh[0].longitude.format(4))
                        linestringFeature.addStringProperty("distance", textDist)
                        features.add(linestringFeature)
                        Timber.i( "features.add: ${file.nameWithoutExtension}")
                    }
                }
            }
            val featureCollection = FeatureCollection.fromFeatures(features)
            targetFile.writeText(featureCollection.toJson())
            Timber.i(
                "routes geojson ready: ${targetFile.path}")
        }

        fun createGeojsonOfflineRegionsBoundFeatures(regions: Array<OfflineRegion>): FeatureCollection? {
            val features = arrayListOf<Feature>()
            regions.forEachIndexed { index, region ->
                val bounds = region.definition.bounds
                if (bounds != null) {
                    val pointList = listOf<Point>(
                        Point.fromLngLat(bounds.longitudeWest, bounds.latitudeNorth),
                        Point.fromLngLat(bounds.longitudeEast, bounds.latitudeNorth),
                        Point.fromLngLat(bounds.longitudeEast, bounds.latitudeSouth),
                        Point.fromLngLat(bounds.longitudeWest, bounds.latitudeSouth),
                        Point.fromLngLat(bounds.longitudeWest, bounds.latitudeNorth)
                    )
                    val pointFeature = Feature.fromGeometry(
                        Point.fromLngLat(bounds.center.longitude, bounds.center.latitude)
                    )
                    val regionName = getRegionName(region)
                    Timber.i( "regionName: $regionName")
                    pointFeature.addStringProperty("name", regionName)
                    val linestringFeature = Feature.fromGeometry(LineString.fromLngLats(pointList))
                    linestringFeature.addStringProperty("name", "offline")
                    features.add(pointFeature)
                    features.add(linestringFeature)
                }
            }
            val featureCollection = FeatureCollection.fromFeatures(features)
            return featureCollection
        }

        fun createRasterMapBoundFeatures(context: Context, finished: (FeatureCollection) -> Unit) {
            val names = getRasterRegionNames(context)
            val features = arrayListOf<Feature>()
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val tilesPrefSet = prefs.getStringSet(Const.PREF_MBTILES_FILEPATH_SET, setOf())
            val prefMapType = getPrefRasterMapType(context)
            Timber.i( "RasterMapType $prefMapType")
            val rootFolder = context.filesDir
            val tilesRootFolder = File(rootFolder, Const.MBTILES_FOLDER)
            names.forEachIndexed { index, name ->
                val splits = name.split(Const.UNDERLINE, ".", limit = 6)
                if (splits.size > 3) {
                /*
                                0 = "tile"
                                1 = "541"
                                2 = "336"
                                3 = "10"
                                4 = "OpenTopo"
                                5 = "mbtiles"
                */
                    val mapType = splits[4]
                    if (mapType == prefMapType) {
                        val bounds = tileToGmsBounds(
                            Tile(splits[1].toInt(), splits[2].toInt(), splits[3].toInt())
                        )
                        val pointList = listOf<Point>(
                            Point.fromLngLat(bounds.southwest.longitude, bounds.northeast.latitude),
                            Point.fromLngLat(bounds.northeast.longitude, bounds.northeast.latitude),
                            Point.fromLngLat(bounds.northeast.longitude, bounds.southwest.latitude),
                            Point.fromLngLat(bounds.southwest.longitude, bounds.southwest.latitude),
                            Point.fromLngLat(bounds.southwest.longitude, bounds.northeast.latitude)
                        )
                        val pointFeature = Feature.fromGeometry(
                            Point.fromLngLat(bounds.center.longitude, bounds.center.latitude)
                        )
                        val rawName = "${splits[1]}_${splits[2]}_${splits[3]}"
                        pointFeature.addStringProperty("name", rawName)
                        pointFeature.addStringProperty(DESCRIPTION, "rasterTile")
                        val rasterFile =
                            File(tilesRootFolder, "tile_${rawName}_${mapType}${Const.MBTILES_EXT}")
                        pointFeature.addStringProperty(
                            STATE,
                            if (rasterFile.exists() && (tilesPrefSet?.contains(rasterFile.path)
                                    ?: false)
                            )
                                "enabled" else "disabled"
                        )
                        val linestringFeature = Feature.fromGeometry(LineString.fromLngLats(pointList))
                        linestringFeature.addStringProperty("name", splits[4])
                        features.add(pointFeature)
                        features.add(linestringFeature)
                    }
                }
            }
            val featureCollection = FeatureCollection.fromFeatures(features)
            finished(featureCollection)
        }

        fun createRasterMapsBounds(context: Context, finished: (String) -> Unit) {
            createGeojsonMapBoundFeatures(context, null) { featureCollection ->
                val rootMapsFolder = File(context.filesDir, Const.MBTILES_FOLDER)
                val fileGeojson = File(rootMapsFolder, "raster_boundaries${Const.GEOJSON_EXT}")
                fileGeojson.writeText(featureCollection.toJson())
                Timber.i(
                    "boundaries${Const.GEOJSON_EXT} ready"
                )
                finished(fileGeojson.path)
            }
        }
/*
        fun createMvtBounds(context: Context, finished: (String) -> Unit) {
            createMvtBoundFeatures(context) { featureCollection ->
                val rootMapsFolder = File(context.filesDir, Const.MVT_FOLDER)
                val fileGeojson = File(rootMapsFolder, "mvt_boundaries${Const.GEOJSON_EXT}")
                fileGeojson.writeText(featureCollection.toJson())
                Timber.i("${fileGeojson.name} ready")
                finished(fileGeojson.path)
            }
        }
*/
        fun createMvtBoundFeatures(context: Context,
                                   finished: (FeatureCollection, File) -> Unit) {
            val driveMap = DriveSharedLinks.Companion.MvtRegions().list
            val names = driveMap.keys // getMvtRegionNames(context)
            val features = arrayListOf<Feature>()
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val currentMvtPath = prefs.getString(Const.PREF_MVT_FILEPATH, null)
            val rootFolder = context.filesDir
            val mvtFolder = File(rootFolder, Const.MVT_FOLDER)
            names.forEachIndexed { index, name ->
                val splits = name.split(Const.UNDERLINE, ".", limit = 6)
                if (splits.size > 3) {
                    /*
                                    0 = "mvt"
                                    1 = "670"
                                    2 = "168"
                                    3 = "9"
                    */
                    val bounds = tileToGmsBounds(
                        Tile(splits[1].toInt(), splits[2].toInt(), splits[3].toInt())
                    )
                    val pointList = listOf<Point>(
                        Point.fromLngLat(bounds.southwest.longitude, bounds.northeast.latitude),
                        Point.fromLngLat(bounds.northeast.longitude, bounds.northeast.latitude),
                        Point.fromLngLat(bounds.northeast.longitude, bounds.southwest.latitude),
                        Point.fromLngLat(bounds.southwest.longitude, bounds.southwest.latitude),
                        Point.fromLngLat(bounds.southwest.longitude, bounds.northeast.latitude)
                    )
                    val pointFeature = Feature.fromGeometry(
                        Point.fromLngLat(bounds.center.longitude, bounds.center.latitude)
                    )
                    val rawName = "${splits[1]}_${splits[2]}_${splits[3]}"
                    pointFeature.addStringProperty("name", rawName)
                    pointFeature.addStringProperty(DESCRIPTION, "mvt")
                    val mvtFile = File(mvtFolder, name)
                    pointFeature.addStringProperty(
                        STATE,
                        if (mvtFile.exists() && (currentMvtPath?.equals(mvtFile.path) ?: false)) "enabled" else "disabled"
                    )
                    val linestringFeature = Feature.fromGeometry(LineString.fromLngLats(pointList))
                    linestringFeature.addStringProperty("name", rawName)
                    features.add(pointFeature)
                    features.add(linestringFeature)
                } else
                    Timber.i(context.getString(R.string.fit_pattern_failed, name))
            }

            val featureCollection = FeatureCollection.fromFeatures(features)
            val fileGeojson = File(context.filesDir, "mvt_boundaries${Const.GEOJSON_EXT}")
            fileGeojson.writeText(featureCollection.toJson())
            //Timber.i("${fileGeojson.name} ready")
            finished(featureCollection, fileGeojson)
        }

        fun getMvtRegionNames(context: Context): ArrayList<String> {
            val rootFolder = context.filesDir
            val mvtFolder = File(rootFolder, Const.MVT_FOLDER)
            mvtFolder.mkdirs()
            val fileFilter = FileFilter { file: File? -> file?.name?.endsWith(Const.MBTILES_EXT) == true &&
                    !file.name.contains(Const.JOURNAL)
            }
            val files: Array<File> = mvtFolder.listFiles(fileFilter) as Array<File>
            files.sortWith(compareBy { it.name })
            val names = arrayListOf<String>()

            files.iterator().forEach {file ->
                val name = file.name
                names.add(name)
                Timber.i( "$name")
            }
            return names
        }

        fun createGeojsonMapBoundFeatures(
            context: Context,
            map: MapLibreMap?,
            finished: (FeatureCollection) -> Unit
        ) {
            //Timber.i( "createGeojsonMapBoundFeatures")
            val mapRepository =
                GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
            val regionList = ArrayList<String>()
            //mapRepository.getAllSimple(false) { maps ->
            val maps = mapRepository.getAllSimpleSync(false)
            //Timber.i( "maps: ${maps.size}")
            val features = arrayListOf<Feature>()
            maps.forEach { mapEntity ->
                if (!regionList.contains(mapEntity.path))
                    regionList.add(mapEntity.path)
                val pointList = listOf<Point>(
                    Point.fromLngLat(mapEntity.west, mapEntity.north),
                    Point.fromLngLat(mapEntity.east, mapEntity.north),
                    Point.fromLngLat(mapEntity.east, mapEntity.south),
                    Point.fromLngLat(mapEntity.west, mapEntity.south),
                    Point.fromLngLat(mapEntity.west, mapEntity.north)
                )
                val pointFeature = Feature.fromGeometry(
                    Point.fromLngLat(
                        mapEntity.centerLongitude,
                        mapEntity.centerLatitude
                    )
                )
                pointFeature.addStringProperty(
                    "name",
                    "${mapEntity.x}_${mapEntity.y}_${mapEntity.z}"
                )
                pointFeature.addStringProperty(DESCRIPTION, "geojsonTile")
                pointFeature.addStringProperty(
                    STATE,
                    if (mapEntity.enabled) "enabled" else "disabled"
                )
                val linestringFeature = Feature.fromGeometry(LineString.fromLngLats(pointList))
                //linestringFeature.addStringProperty("name", mapEntity.path.replace("tile_", ""))
                features.add(pointFeature)
                features.add(linestringFeature)
            }
            regionList.forEach { region ->
                val splits = region.split(Const.UNDERLINE, ".", limit = 6)
                if (splits.size > 3) {
                    val bounds = tileToGmsBounds(
                        Tile(splits[1].toInt(), splits[2].toInt(), splits[3].toInt())
                    )
                    val pointList = listOf<Point>(
                        Point.fromLngLat(bounds.center.longitude, bounds.northeast.latitude),
                        Point.fromLngLat(bounds.northeast.longitude, bounds.northeast.latitude),
                        Point.fromLngLat(bounds.northeast.longitude, bounds.southwest.latitude),
                        Point.fromLngLat(bounds.southwest.longitude, bounds.southwest.latitude),
                        Point.fromLngLat(bounds.southwest.longitude, bounds.northeast.latitude),
                        Point.fromLngLat(bounds.center.longitude, bounds.northeast.latitude)
                    )
                    val rawName = "${splits[1]}_${splits[2]}_${splits[3]}"
                    val linestringFeature = Feature.fromGeometry(LineString.fromLngLats(pointList))
                    linestringFeature.addStringProperty(NAME, rawName)
                    linestringFeature.addStringProperty(DESCRIPTION, context.getString(R.string.region))
                    Timber.i( "region: $rawName")
                    features.add(linestringFeature)
                }
            }
            if (map != null) {
                val geoJsonSource = map.style!!.getSource("geojson_boundaries${Const.GEOJSON_EXT}")
                if (geoJsonSource != null) {
                    CoroutineScope(Dispatchers.Main).launch { //corresponds to runOnUiThread
                        (geoJsonSource as GeoJsonSource).setGeoJson(
                            FeatureCollection.fromFeatures(features)
                        )
                    }.invokeOnCompletion {
                        Timber.i( "createGeojsonMapBoundFeatures finished features: ${features.size}")
                        finished(FeatureCollection.fromFeatures(features))
                    }
                }
            } else {
                //Timber.i( "createGeojsonMapBoundFeatures finished features: ${features.size}")
                finished(FeatureCollection.fromFeatures(features))
            }
        }

        fun createTileBackgroundFeatureCollection(mapEntity: GeojsonMapEntity): FeatureCollection? {
            val pointList = listOf<Point>(
                Point.fromLngLat(mapEntity.west, mapEntity.north),
                Point.fromLngLat(mapEntity.east, mapEntity.north),
                Point.fromLngLat(mapEntity.east, mapEntity.south),
                Point.fromLngLat(mapEntity.west, mapEntity.south),
                Point.fromLngLat(mapEntity.west, mapEntity.north)
            )
            //val polygonList = listOf(pointList)
            val pointFeature = Feature.fromGeometry(Point.fromLngLat(mapEntity.centerLongitude, mapEntity.centerLatitude))
            pointFeature.addStringProperty("name", mapEntity.name)
            val linestringFeature = Feature.fromGeometry(LineString.fromLngLats(pointList))
            linestringFeature.addStringProperty("name", mapEntity.name)
            val features = arrayListOf(linestringFeature, pointFeature)
            val featureCollection = FeatureCollection.fromFeatures(features)
            //File(mapEntity.path.replace("geojsonTile_", "bg_")).writeText(features.toJson())
            Timber.i( "backgroundSource features: ${features.size}")
            return featureCollection
        }

        fun createTileBackgroundGeojsonSource(mapEntity: GeojsonMapEntity): GeoJsonSource {
            val pointList = listOf<Point>(
                Point.fromLngLat(mapEntity.west, mapEntity.north),
                Point.fromLngLat(mapEntity.east, mapEntity.north),
                Point.fromLngLat(mapEntity.east, mapEntity.south),
                Point.fromLngLat(mapEntity.west, mapEntity.south),
                Point.fromLngLat(mapEntity.west, mapEntity.north)
            )
            //val polygonList = listOf(pointList)
            val pointFeature = Feature.fromGeometry(Point.fromLngLat(mapEntity.centerLongitude, mapEntity.centerLatitude))
            pointFeature.addStringProperty("name", mapEntity.name)
            val linestringFeature = Feature.fromGeometry(LineString.fromLngLats(pointList))
            linestringFeature.addStringProperty("name", mapEntity.name)
            val features = arrayListOf(linestringFeature, pointFeature)
            val featureCollection = FeatureCollection.fromFeatures(features)
            val backgroundSource = GeoJsonSource("${mapEntity.name}_background", featureCollection)
            //File(mapEntity.path.replace("geojsonTile_", "bg_")).writeText(features.toJson())
            //Timber.i( "backgroundSource features: ${features.size}")
            return backgroundSource
        }

        /**
         * 21mar2026 change for use of qgis plugin Export_multilayers (Export des couches vecteur)
         * 0. plugin not compatible with newest QGis Version 4.0
         * 1. plugin creates files with coordinate precision 1e-15
         * 2. transportation layer, pmtiles_539_336_10 — transportation_name, WITHOUT pathes, bicycle ways ...
         * 3. solution: manually export transportation layer and transportation_name layer
         * 4. transportation layer pmtiles_539_336_10 (Hannover) with pathes ... leads to storage problems
         * 5. solution: transportation layer without pathes and cycleway overlay :-(
         *
         * -------------------------------------------------------------------
         *         //Setting the map style using the new edited JSON file
         *         map.setStyle(
         *             Style.Builder().fromUri(Uri.fromFile(styleFile).toString())
         */
        fun createGeojsonOfflineStyle(context: Context, geojson_folder_name: String): String {
            //Timber.i("geojson_folder_name $geojson_folder_name")

            val styleFileName = Const.GEOJSON_QGIS_STYLE_FILENAME
            val styleJsonInputStream = context.assets.open("styles/${styleFileName}")
            //Creating a new file to which to copy the json content to
            //val dir = File(filesDir.absolutePath)
            val rootFolder = context.filesDir
            val mvtRootFolder = File(rootFolder, Const.MVT_FOLDER)
            mvtRootFolder.mkdir()
            val styleFile = File(mvtRootFolder, styleFileName)
            //val styleFile = File(cacheDir, styleFileName)
            //Copying the original JSON content to new file
            copyStreamToFile(styleJsonInputStream, styleFile)

            //val bounds = getMvtBounds(mbtilesFile)
            //Timber.i("bounds: $bounds")
            //val minZoomLevel = getMinZoom(mbtilesFile).toDouble()

            //Replacing placeholder with uri of the mbtiles file
            val newValue = geojson_folder_name.replace(Const.GEOJSON_PREFIX, "")
            val newFileStr = styleFile.inputStream().readToString()
                .replace("___FOLDER_NAME___", newValue) //geojson_folder_name)
            //Timber.i("geojson_folder_name: $geojson_folder_name")
            //Writing new content to file
            val fileWriter = FileWriter(styleFile)
            val out = BufferedWriter(fileWriter)
            out.write(newFileStr)
            out.close()
            return Uri.fromFile(styleFile).toString()
            //return styleFile
        }

        fun createDefaultGeojsonOfflineStyle(context: Context) {
            val sourceStyleFileName = Const.PLANET_STYLE_FILENAME
            Timber.i("style sourceStyleFileName: $sourceStyleFileName will be used")
            val targetStyleFileName = Const.GEOJSON_QGIS_STYLE_FILENAME
            val styleJsonInputStream = context.assets.open("styles/${sourceStyleFileName}")
            val rootFolder = context.filesDir
            val mvtRootFolder = File(rootFolder, Const.MVT_FOLDER)
            mvtRootFolder.mkdir()
            val styleFile = File(mvtRootFolder, targetStyleFileName)
            copyStreamToFile(styleJsonInputStream, styleFile)
        }

        fun initGeoJsonComponents(geojsonMapEntity: GeojsonMapEntity): GeoJsonComponents {
            val jsonString = geojsonMapEntity.data?.zlibDecompress()
            //Timber.i( "jsonString: ${jsonString?.length}")
            val geoMapFile = File(geojsonMapEntity.path) // used as region 541_335_10, 541_336_10 ...
            Timber.i("geoMapFile: ${geoMapFile.name}")
            val geoMapFileUri = geoMapFile.toURI()
            val fileUri = "file://" + geoMapFileUri.path
            val geoJsonSource = if (jsonString != null)
                GeoJsonSource(geojsonMapEntity.name, jsonString) else
                GeoJsonSource(geojsonMapEntity.name, URI(fileUri))
            Timber.i( "geoMapFile: ${geoMapFile.path}")
/*
            val backgroundSource = createTileBackgroundGeojsonSource(geojsonMapEntity)
            var backgroundFillLayer: FillLayer?
            val layerId = "${geojsonMapEntity.name}_fill_background"
            backgroundFillLayer = FillLayer(layerId, backgroundSource.id)
            backgroundFillLayer.setProperties(
                PropertyFactory.fillColor(COLOR_LANDFILL),
                PropertyFactory.fillOpacity(.7f)
            )
*/
            val geoJsonSymbolLayers = java.util.ArrayList<SymbolLayer>()
            val imageExpression = createFilterSymbolIcons()
            geoJsonSymbolLayers.add(createGeojsonSymbolLayer(imageExpression, geojsonMapEntity.name, 12f))

            val geoJsonLineLayers = java.util.ArrayList<LineLayer>()
            val filterLineColor = createFilterLineColor()
            //Timber.i( "$filterLineColor")
            geoJsonLineLayers.add(createGeojsonLineLayer(filterLineColor, geojsonMapEntity.name, 11f))

            val geoJsonFillLayers = java.util.ArrayList<FillLayer>()
            //backgroundFillLayer.let { geoJsonFillLayers.add(it) }
            val filterFillColor = createFilterFillColor()
            //Timber.i( "$filterFillColor")
            geoJsonFillLayers.add(createGeojsonFillLayer(filterFillColor, geojsonMapEntity.name, 10f))

            return GeoJsonComponents(
                arrayListOf(geoJsonSource), // backgroundSource),
                    geoJsonLineLayers, //listOf(geoJsonLineLayer),
                    geoJsonSymbolLayers, geoJsonFillLayers) //listOf(geoJsonSymbolLayer))
        }

        fun getFeatureCollectionFromFile(geojsonMapFile: File): FeatureCollection? {
            if (!geojsonMapFile.exists()) return null
            Timber.i("geojsonMapFile: ${geojsonMapFile.name}")
            return try {
                val fis = FileInputStream(geojsonMapFile)
                val gson = GsonBuilder()
                gson.registerTypeAdapterFactory(GeoJsonAdapterFactory.create())
                gson.registerTypeAdapterFactory(GeometryAdapterFactory.create())
                val buffReader = BufferedReader(InputStreamReader(fis), 8192)
                val featureCollection: FeatureCollection? =
                    gson.create().fromJson(buffReader, FeatureCollection::class.java)
                fis.close()
                featureCollection
            } catch (e: Exception) {
                Timber.e(e, "Error reading FeatureCollection from file")
                null
            }
        }

        fun getFeatureCollectionFromString(jsonString: String): FeatureCollection? {
            if (jsonString.isBlank()) return null
            return try {
                val gson = GsonBuilder()
                gson.registerTypeAdapterFactory(GeoJsonAdapterFactory.create())
                gson.registerTypeAdapterFactory(GeometryAdapterFactory.create())
                val bis = ByteArrayInputStream(jsonString.toByteArray())
                val buffReader = BufferedReader(InputStreamReader(bis), 8192)
                val featureCollection: FeatureCollection? =
                    gson.create().fromJson(buffReader, FeatureCollection::class.java)
                featureCollection
            } catch (e: Exception) {
                Timber.e(e, "Error reading FeatureCollection from string")
                null
            }
        }

        fun simplifyGeojsonMap(jsonString: String): ByteArray? {
            val featureCollection: FeatureCollection? = getFeatureCollectionFromString(jsonString)
            Timber.i( "featureCollections ready")
            val features = featureCollection?.features()
            if (features != null) {
                Timber.i( "features: ${features.size}")
                val coastlineFeatures = ArrayList<Feature>()
                val forestFeatures = ArrayList<Feature>()
                val waterFeatures = ArrayList<Feature>()
                val grassFeatures = ArrayList<Feature>()
                val woodFeatures = ArrayList<Feature>()
                val cemeteryFeatures = ArrayList<Feature>()
                val recreationGroundfeatures = ArrayList<Feature>()
                val militaryFeatures = ArrayList<Feature>()
                val landfillFeatures = ArrayList<Feature>()
                val scrubFeatures = ArrayList<Feature>()
                val beachFeatures = ArrayList<Feature>()
                val sandFeatures = ArrayList<Feature>()
                val wetlandFeatures = ArrayList<Feature>()
                val grasslandFeatures = ArrayList<Feature>()
                val bareRockFeatures = ArrayList<Feature>()
                val meadowFeatures = ArrayList<Feature>()
                val villagegreenFeatures = ArrayList<Feature>()
                val residentialFeatures = ArrayList<Feature>()
                val farmlandFeatures = ArrayList<Feature>()
                val farmyardFeatures = ArrayList<Feature>()
                val commercialFeatures = ArrayList<Feature>()
                val industrialFeatures = ArrayList<Feature>()
                val railwayFeatures = ArrayList<Feature>()
                val gardenFeatures = ArrayList<Feature>()
                val waterparkFeatures = ArrayList<Feature>()
                val sportscenterFeatures = ArrayList<Feature>()
                val aerowayFeatures = ArrayList<Feature>()
                val buildingFeatures = ArrayList<Feature>()
                val wayFeatures = ArrayList<Feature>()
                val waterwayFeatures = ArrayList<Feature>()
                val thinLineFeatures = ArrayList<Feature>()
                features.forEach {
                    if (it.geometry() is MultiPolygon) {
                        if (it.getProperty(LANDUSE) != null) {
                            val landuse = it.getProperty(LANDUSE).asString
                            Timber.i( "landuse:$landuse")
                            if (landuse.contains(FOREST))
                                forestFeatures.add(it)
                            else if (landuse.contains(FARMLAND))
                                farmlandFeatures.add(it)
                            else if (landuse.contains(FARMYARD))
                                farmyardFeatures.add(it)
                            else if (landuse.contains(GRASS))
                                grassFeatures.add(it)
                            else if (landuse.contains(CEMETERY))
                                cemeteryFeatures.add(it)
                            else if (landuse.contains(RECREATION_GROUND))
                                recreationGroundfeatures.add(it)
                            else if (landuse.contains(MILITARY))
                                militaryFeatures.add(it)
                            else if (landuse.contains(LANDFILL))
                                landfillFeatures.add(it)
                            else if (landuse.contains(MEADOW))
                                meadowFeatures.add(it)
                            else if (landuse.contains(VILLAGE_GREEN))
                                villagegreenFeatures.add(it)
                            else if (landuse.contains(RESIDENTIAL))
                                residentialFeatures.add(it)
                            else if (landuse.contains(COMMERCIAL))
                                commercialFeatures.add(it)
                            else if (landuse.contains(INDUSTRIAL))
                                industrialFeatures.add(it)
                            else if (landuse.contains(RAILWAY))
                                railwayFeatures.add(it)
                        }
                        if (it.getProperty(NATURAL) != null) {
                            val natural = it.getProperty(NATURAL).asString
                            if (natural.contains(WATER))
                                waterFeatures.add((it))
                            else if (natural.contains(WOOD))
                                waterFeatures.add((it))
                            else if (natural.contains(SCRUB))
                                scrubFeatures.add(it)
                            else if (natural.contains(BEACH))
                                beachFeatures.add(it)
                            else if (natural.contains(SAND))
                                sandFeatures.add(it)
                            else if (natural.contains(WETLAND))
                                wetlandFeatures.add(it)
                            else if (natural.contains(BARE_ROCK))
                                bareRockFeatures.add(it)
                            else if (natural.contains(GRASSLAND))
                                grasslandFeatures.add(it)
                            else if (natural.contains(COASTLINE))
                                coastlineFeatures.add(it)
                        }
                        if (it.getProperty(LEISURE) != null) {
                            val leisure = it.getProperty(LEISURE).asString
                            if (leisure.contains(GARDEN))
                                gardenFeatures.add((it))
                            if (leisure.contains(WATER_PARK))
                                waterparkFeatures.add((it))
                            if (leisure.contains(SPORTS_CENTRE)){
                                sportscenterFeatures.add((it))
                                Timber.i("MultiPolygon: %s", it.properties().toString())
                            }
                        }
                        if (it.getProperty(AEROWAY) != null) {
                            if (it.getProperty(AEROWAY).toString().contains(AERODROME))
                                aerowayFeatures.add(it)
                        }
                        if (it.getProperty(BUILDING) != null) {
                            val building = it.getProperty(BUILDING)
                            Timber.i( building.toString())
                            if (building.toString().contains(SCHOOL)
                                || building.toString().contains(COMMERCIAL)
                                || building.toString().contains(INDUSTRIAL)
                                || building.toString().contains(CHURCH)
                                || building.toString().contains(CATHEDRAL)
                                || building.toString().contains(HOTEL)
                                || building.toString().contains(OFFICE)
                                || building.toString().contains(RETAIL)
                                || building.toString().contains(SUPERMARKET)
                                || building.toString().contains(WAREHOUSE)
                                || building.toString().contains(GOVERNMENT)
                                || building.toString().contains(CHAPEL)
                            ) {
                                Timber.i( "add: %s", it.properties())
                                buildingFeatures.add((it))
                                thinLineFeatures.add(it)
                            }
                        }
                        if (it.getProperty(AMENITY) != null) {
                            val amenity = it.getProperty(AMENITY)
                            if (amenity.toString().contains(HOSPITAL)) {
                                buildingFeatures.add((it))
                                thinLineFeatures.add(it)
                            }
                        }
                    } else if (it.geometry() is LineString) {
                        if (it.getProperty(RAILWAY) != null && it.getProperty(RAILWAY).toString()
                                .contains(RAIL)
                        )
                            wayFeatures.add(it)
                        else if (it.getProperty(HIGHWAY) != null) {
                            wayFeatures.add(it)
                        } else if (it.getProperty(WATERWAY) != null) {
                            waterwayFeatures.add(it)
                        } else if (it.getProperty(LEISURE) != null) {
                            val leisure = it.getProperty(LEISURE)
                            if (leisure.toString().contains(SPORTS_CENTRE) ||
                                leisure.toString().contains("playground")
                            ) {
                                Timber.i("LineString: %s", it.properties().toString())
                                thinLineFeatures.add(it)
                            }
                        }
                    }
                }
                Timber.i( "wayFeatures: %s", wayFeatures.size)
                Timber.i( "forestFeatures: %s", forestFeatures.size)
                Timber.i( "residentialFeatures: %s", residentialFeatures.size)
                Timber.i( "grassFeatures: %s", grassFeatures.size)
                Timber.i( "woodFeatures: %s", woodFeatures.size)
                Timber.i( "cemeteryFeatures: %s", cemeteryFeatures.size)
                Timber.i("recreation_groundFeatures: %s", recreationGroundfeatures.size)
                Timber.i( "militaryFeatures: %s", militaryFeatures.size)
                Timber.i( "landfillFeatures: %s", landfillFeatures.size)
                Timber.i( "scrubFeatures: %s", scrubFeatures.size)
                Timber.i( "beachFeatures: %s", beachFeatures.size)
                Timber.i( "sandFeatures: %s", sandFeatures.size)
                Timber.i( "wetlandFeatures: %s", wetlandFeatures.size)
                Timber.i( "coastlineFeatures: %s", coastlineFeatures.size)
                Timber.i( "grasslandFeatures: %s", grasslandFeatures.size)
                Timber.i( "bareRockFeatures: %s", bareRockFeatures.size)
                Timber.i( "meadowFeatures: %s", meadowFeatures.size)
                Timber.i( "villagegreenFeatures: %s", villagegreenFeatures.size)
                Timber.i( "waterFeatures: %s", waterFeatures.size)
                Timber.i( "waterwayFeatures: %s", waterwayFeatures.size)
                Timber.i( "farmlandFeatures: %s", farmlandFeatures.size)
                Timber.i( "farmyardFeatures: %s", farmyardFeatures.size)
                Timber.i( "commercialFeatures: %s", commercialFeatures.size)
                Timber.i( "industrialFeatures: %s", industrialFeatures.size)
                Timber.i( "railwayFeatures: %s", railwayFeatures.size)
                Timber.i( "gardenFeatures: %s", gardenFeatures.size)
                Timber.i( "waterparkFeatures: %s", waterparkFeatures.size)
                Timber.i( "sportscenterFeatures: %s", sportscenterFeatures.size)
                Timber.i( "aerowayFeatures: %s", aerowayFeatures.size)
                Timber.i( "buildingFeatures: %s", buildingFeatures.size)
                Timber.i( "thinLineFeatures: %s", thinLineFeatures.size)
//        val baseGeoJsonSource = GeoJsonSource(name, featureCollection)
                val combinedFeatures = ArrayList<Feature>()
                val forestFeatureCollection = FeatureCollection.fromFeatures(forestFeatures)
                combinedFeatures.addAll(forestFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(residentialFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(grassFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(woodFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(cemeteryFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(recreationGroundfeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(militaryFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(landfillFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(scrubFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(beachFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(sandFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(wetlandFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(coastlineFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(grasslandFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(bareRockFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(meadowFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(villagegreenFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(farmlandFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(farmyardFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(commercialFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(industrialFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(railwayFeatures)
                Timber.i( "railwayFeatures: %s", railwayFeatures.size)

                combinedFeatures.addAll(waterFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(waterparkFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(sportscenterFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(aerowayFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(gardenFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(buildingFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(wayFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(waterwayFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                combinedFeatures.addAll(thinLineFeatures)
                Timber.i( "combinedFeatures: %s", combinedFeatures.size)

                val combinedFeatureCollection = FeatureCollection.fromFeatures(combinedFeatures)
                combinedFeatures.addAll(
                    getGeojsonSymbolFeatures(
                        PLACE,
                        CITY,
                        featureCollection
                    )
                )
                combinedFeatures.addAll(
                    getGeojsonSymbolFeatures(
                        PLACE,
                        TOWN,
                        featureCollection
                    )
                )
                combinedFeatures.addAll(
                    getGeojsonSymbolFeatures(
                        PLACE,
                        VILLAGE,
                        featureCollection
                    )
                )
                combinedFeatures.addAll(
                    getGeojsonSymbolFeatures(
                        PLACE,
                        HAMLET,
                        featureCollection
                    )
                )
                combinedFeatures.addAll(
                    getGeojsonSymbolFeatures(
                        AMENITY,
                        RESTAURANT,
                        featureCollection
                    )
                )
                combinedFeatures.addAll(
                    getGeojsonSymbolFeatures(
                        AMENITY,
                        PARKING,
                        featureCollection
                    )
                )
                combinedFeatures.addAll(
                    getGeojsonSymbolFeatures(
                        AMENITY,
                        PHARMACY,
                        featureCollection
                    )
                )
                combinedFeatures.addAll(
                    getGeojsonSymbolFeatures(
                        AMENITY,
                        FUEL,
                        featureCollection
                    )
                )
                combinedFeatures.addAll(
                    getGeojsonSymbolFeatures(
                        SHOP,
                        BAKERY,
                        featureCollection
                    )
                )
                combinedFeatures.addAll(
                    getGeojsonSymbolFeatures(
                        SHOP,
                        CONVENIENCE,
                        featureCollection
                    )
                )
                combinedFeatures.addAll(
                    getGeojsonSymbolFeatures(
                        SHOP,
                        SUPERMARKET,
                        featureCollection
                    )
                )
                combinedFeatures.addAll(
                    getGeojsonSymbolFeatures(
                        NATURAL,
                        PEAK,
                        featureCollection
                    )
                )
                combinedFeatures.addAll(
                    getGeojsonSymbolFeatures(
                        LEISURE,
                        PICNIC_TABLE,
                        featureCollection
                    )
                )
                combinedFeatures.addAll(
                    getGeojsonSymbolFeatures(
                        MAN_MADE,
                        TOWER,
                        featureCollection
                    )
                )
                combinedFeatures.addAll(
                    getGeojsonSymbolFeatures(
                        TOURISM,
                        ATTRACTION,
                        featureCollection
                    )
                )

                //fileName.replaceFirst(UNDERLINE, "#" + UNDERLINE)
                val jsonStringSimplified = createJsonStringFromFeatureCollection(combinedFeatureCollection)
                var jsonStringCompressed : ByteArray? = null
                jsonStringSimplified?.let {
                    jsonStringCompressed = jsonStringSimplified.zlibCompress()
                    val compressRatio = ((100.0 * jsonStringCompressed.size)/jsonStringSimplified.length)
                    Timber.i( "compressRatio: ${compressRatio.format(1)}%")
                }
                return jsonStringCompressed
            }
            return null
        }

        private fun createJsonStringFromFeatureCollection(featureCollection: FeatureCollection): String? {
            val gsonBuilder = GsonBuilder()
            gsonBuilder.registerTypeAdapterFactory(GeoJsonAdapterFactory.create())
            gsonBuilder.registerTypeAdapterFactory(GeometryAdapterFactory.create())
            val gson = gsonBuilder.create()
            val jsonString = gson.toJson(featureCollection)
            return jsonString
        }

        /**
         * if name is duplicate a new file version will be created in drive
         * dropbox asks for overwrite
         */
        fun shareGeojsonMaps(context: Context, region: String, lifecycleOwner: LifecycleOwner) {
            val mapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())

            mapRepository.getAllSimple(region) { maps ->
                Timber.i("maps: ${maps.size}")
                val uris = arrayListOf<Uri>()
                lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    maps.forEachIndexed { _, geojsonMapEntity ->
                        val cacheFile = File(context.cacheDir,
                            "${geojsonMapEntity.name}${Const.HASHTAG}${Const.GEOJSON_EXT}")
                        Timber.i("cacheFile: ${cacheFile.path}")
                        val jsonString = geojsonMapEntity.data?.zlibDecompress()
                        jsonString?.let { cacheFile.writeText(it, Charsets.UTF_8) }
                        val uri = FileProvider.getUriForFile(
                            context,
                            BuildConfig.APPLICATION_ID + ".provider",
                            cacheFile
                        )
                        uris.add(uri)
                    }
                }.invokeOnCompletion {
                    val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND_MULTIPLE
                    shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    shareIntent.type = "*/*"
                    context.startActivity(Intent.createChooser(shareIntent, "Share files to.."))

                }
            }
        }
        /**
         * if name is duplicate a new file version will be created in drive
         * dropbox asks for overwrite
         */
        // share GeoJsonMap from source database by cache file
        fun shareGeojsonMap(context: Context, geojsonMapEntity: GeojsonMapEntity) {
            context.cacheDir.walkTopDown().forEach {file ->
                if (file.name.endsWith(Const.GEOJSON_EXT)) {
                    val b = file.delete()
                    Timber.i( "delete $b: ${file.name}")
                }
            }
            val cacheFile = File(context.cacheDir, "${geojsonMapEntity.name}${Const.HASHTAG}${Const.GEOJSON_EXT}")
            Timber.i(
                "cacheFile: ${cacheFile.path}")
            val jsonString = geojsonMapEntity.data?.zlibDecompress()
            jsonString?.let { cacheFile.writeText(it, Charsets.UTF_8) }
            val shareIntent = Intent()
            val uri = FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".provider",
                cacheFile
            )

            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.type = "*/*"
            context.startActivity(Intent.createChooser(shareIntent, "Share file to.."))
        }

        private fun getGeojsonSymbolFeatures(
            filterName: String,
            filterValue: String,
            data: FeatureCollection
        ): ArrayList<Feature> {
            Timber.i( "$filterName $filterValue")
            val result = ArrayList<Feature>()
            data.features()?.forEach { feature ->
                if ((feature.geometry() is Point || feature.geometry() is MultiPolygon) && feature.getProperty(
                        filterName
                    ) != null
                ) {
                    if (feature.getProperty(filterName).toString().contains(filterValue)) {
                        result.add(feature)
                    }
                }
            }
            Timber.i( "%s %s%s %s", filterName, filterValue, ":", result.size)
            return result
        }

        fun readJsonFile(context: Context): ArrayList<LatLngH> {
            val file = File(context.filesDir, "trackpointdata.json")
            Timber.i("Found File: $file")
            val fis = FileInputStream(file)
            val gson = GsonBuilder()
            gson.registerTypeAdapterFactory(GeoJsonAdapterFactory.create())
            gson.registerTypeAdapterFactory(GeometryAdapterFactory.create())
            val buffReader = BufferedReader(InputStreamReader(fis), 8192)
            val coordinates: JsonArray =
                gson.create().fromJson(buffReader, JsonArray::class.java)
            val lllh = ArrayList<LatLngH>()
            for (i in 0 until coordinates.size()) {
                val jo = coordinates.asJsonArray.get(i).asJsonObject
                Timber.i(
                    "${jo.get("id")} lat=${jo.get("lat")} lon=${jo.get("lon")} alt=${jo.get("alt")}"
                )
                lllh.add(
                    LatLngH(
                        jo.get("lat").asDouble,
                        jo.get("lon").asDouble,
                        jo.get("alt").asDouble
                    )
                )
            }
            Timber.i("coordinates: $coordinates")
            return lllh
        }

        fun getBoundsFromGeojsonMap(file: File): LatLngBounds? {
            val featureCollection: FeatureCollection? = getFeatureCollectionFromFile(file)
            val latLngBounds = featureCollection?.let { getFeatureCollectionBounds(it) }
            Timber.i(
                "%s %s %s", file.name, latLngBounds?.southWest.toString(), latLngBounds?.northEast.toString()
            )
            return latLngBounds
        }

        fun getRouteEntityFromGeojsonByName(context: Context, name: String?): RouteEntity? {
            var routeEntity : RouteEntity? = null
            val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
            val fileGeojson = File(rootRouteFolder, "routes${Const.GEOJSON_EXT}")
            val featureCollection: FeatureCollection? = getFeatureCollectionFromFile(fileGeojson)

            if (featureCollection != null && !featureCollection.features().isNullOrEmpty()) {
                val featureIterator = featureCollection.features()?.iterator()
                run forEachLoop@ {
                    featureIterator?.forEach { feature ->
                        if (feature != null) {
                            val featureName = feature.getProperty("name").asString
                            if (featureName == name) {
                                val lllh = getLllhFromGeometry(feature.geometry())
                                if (lllh.isNotEmpty()) {
                                    val region = feature.getProperty("region").asString
                                    Timber.i("feature $region $name")
                                    Timber.i("$featureName lllh ${lllh.size} ")
                                    routeEntity = RouteEntity(
                                        UUID.randomUUID(),
                                        name ?: "",
                                        region,
                                        lllh[0].latitude,
                                        lllh[0].longitude,
                                        distance = lllh.getDistanceFromLllh(),
                                        kmlString = lllh.lllhToKmlString(name)
                                    )
                                    return@forEachLoop
                                }
                            }
                        }
                    }
                }
            }
            return routeEntity
        }

        fun getRouteEntitiesFromGeojson(context: Context, file: File?): ArrayList<RouteEntity> {
            val routeEntities = ArrayList<RouteEntity>()
            val fileGeojson = if (file.isNotNull()) file else {
                val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                File(rootRouteFolder, "routes${Const.GEOJSON_EXT}")
            }
            fileGeojson?.let {
                if (it.exists()) {
                    val featureCollection: FeatureCollection? = getFeatureCollectionFromFile(fileGeojson)

                    if (featureCollection != null && !featureCollection.features().isNullOrEmpty()) {
                        val featureIterator = featureCollection.features()?.iterator()
                        featureIterator?.forEach { feature ->
                            if (feature != null) {
                                val lllh = getLllhFromGeometry(feature.geometry())
                                if (lllh.isNotEmpty()) {
                                    val region = feature.getProperty("region").asString
                                    val name = feature.getProperty("name").asString
                                    //Timber.i("feature $region $name")
                                    val routeEntity = RouteEntity(
                                        UUID.randomUUID(),
                                        name,
                                        region,
                                        lllh[0].latitude,
                                        lllh[0].longitude,
                                        distance = lllh.getDistanceFromLllh(),
                                        kmlString = lllh.lllhToKmlString(name)
                                    )
                                    routeEntities.add(routeEntity)
                                }
                            }
                        }
                    }
                }
            }
            return routeEntities
        }

        fun getRegionsFromRouteGeojson(context: Context): ArrayList<String> {
            val regions = ArrayList<String>()
            val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
            val fileGeojson = File(rootRouteFolder, "routes${Const.GEOJSON_EXT}")
            if (fileGeojson.exists()) {
                val fis = FileInputStream(fileGeojson)
                val gson = GsonBuilder()
                gson.registerTypeAdapterFactory(GeoJsonAdapterFactory.create())
                gson.registerTypeAdapterFactory(GeometryAdapterFactory.create())
                val buffReader = BufferedReader(InputStreamReader(fis), 8192)
                val featureCollection: FeatureCollection =
                    gson.create().fromJson(buffReader, FeatureCollection::class.java)
                if (!featureCollection.features().isNullOrEmpty()) {
                    val featureIterator = featureCollection.features()?.iterator()
                    featureIterator?.forEach { feature ->
                        if (feature != null && feature.geometry() is LineString) {
                            val regionProperty = feature.getProperty("region")
                            if (regionProperty != null) {
                                val regionValue = regionProperty.asString
                                val name = feature.getProperty("name")
                                Timber.i("feature $regionValue $name")
                                if (regionValue != null && !regions.contains(regionValue))
                                    regions.add(regionValue)
                            }
                        }
                    }
                }
                return regions
            }
            Timber.i("${fileGeojson.path} not found")
            return arrayListOf()
        }

        fun getFeatureCollectionBounds(featureCollection: FeatureCollection): LatLngBounds {
            val maxCount = 100000 // oom
            var featureCount = 0
            var pointList: MutableList<Point> = ArrayList()
            featureCollection.features()?.let { features ->
                for (feature: Feature in features) {
                    if (feature.geometry() is MultiPolygon || feature.geometry() is LineString) {
                        val bbox = feature.geometry()?.bbox() // always null ?
                        if (bbox != null) {
                            Timber.i("bbox: %s", bbox)
                            pointList.add(
                                Point.fromLngLat(
                                    bbox.northeast().longitude(),
                                    bbox.northeast().latitude()
                                )
                            )
                            pointList.add(
                                Point.fromLngLat(
                                    bbox.southwest().longitude(),
                                    bbox.southwest().latitude()
                                )
                            )
                        } else {
                            pointList = feature.geometry()
                                ?.let { TurfMeta.coordAllFromSingleGeometry(pointList, it, false) }
                                ?: pointList
                            //Timber.i( "pointList: " + pointList.size)
                        }

                        featureCount++
                    }
                    if (featureCount > maxCount)
                        break
                }
            }
            Timber.i("featureCount: $featureCount")
            Timber.i( "pointList: %s", pointList.size)
            val latLngBoundsBuilder = LatLngBounds.Builder()
            for (point: Point in pointList) {
                latLngBoundsBuilder.include(
                    LatLng(
                        point.latitude(),
                        point.longitude()
                    )
                ) // oom
            }
            return latLngBoundsBuilder.build()
        }

        data class Tile(var x: Int, var y: Int, val z: Int)
        const val R2D = 180 / Math.PI
        const val D2R = Math.PI / 180
        fun tile2lon(x: Int, z: Int): Double {
            return (x / (2.0).pow(z)) * 360 - 180
        }

        fun tile2lat(y: Int, z: Int): Double {
            val n = Math.PI - (2 * Math.PI * y) / (2.0).pow(z)
            return R2D * atan(0.5 * (exp(n) - exp(-n)))
        }

        /**
         * Get the tile for a point at a specified zoom level
         *
         * const tile = pointToTile(1, 1, 20)
         * //=tile
         */
        fun pointToTile(lon: Double, lat: Double, z: Double): Tile {
            val tile = pointToTileFraction(lon, lat, z)
            tile.x = floor(tile.x.toDouble()).toInt()
            tile.y = floor(tile.y.toDouble()).toInt()
            return tile
        }

        /**
         * Get the precise fractional tile location for a point at a zoom level
         *
         * const tile = pointToTileFraction(30.5, 50.5, 15)
         * //=tile
         */
        fun pointToTileFraction(lon: Double, lat: Double, z: Double): Tile {
            val sin = sin(lat * D2R)
            val z2 = (2.0).pow(z)
            var x = z2 * (lon / 360 + 0.5)
            val y = z2 * (0.5 - (0.25 * ln((1 + sin) / (1 - sin))) / Math.PI)

            // Wrap Tile X
            x %= z2
            if (x < 0) x += z2
            return Tile(x.toInt(), y.toInt(), z.toInt())
        }

        fun tileToGmsBounds(tile: Tile): com.google.android.gms.maps.model.LatLngBounds {
            val e = tile2lon(tile.x + 1, tile.z)
            val w = tile2lon(tile.x, tile.z)
            val s = tile2lat(tile.y + 1, tile.z)
            val n = tile2lat(tile.y, tile.z)
            val llBoundsBuilder = com.google.android.gms.maps.model.LatLngBounds.Builder()
            llBoundsBuilder.include(com.google.android.gms.maps.model.LatLng(s,w))
            llBoundsBuilder.include(com.google.android.gms.maps.model.LatLng(n,e))

            val latLngBounds = llBoundsBuilder.build()
            return latLngBounds
        }

        fun tileToBounds(tile: Tile): LatLngBounds {
            val e = tile2lon(tile.x + 1, tile.z)
            val w = tile2lon(tile.x, tile.z)
            val s = tile2lat(tile.y + 1, tile.z)
            val n = tile2lat(tile.y, tile.z)
            val llBoundsBuilder = LatLngBounds.Builder()
            llBoundsBuilder.include(LatLng(s,w))
            llBoundsBuilder.include(LatLng(n,e))
            val latLngBounds = llBoundsBuilder.build()
            return latLngBounds
        }

        fun tileCenter(tile: Tile): com.google.android.gms.maps.model.LatLng {
            val latLngBounds = tileToGmsBounds(tile)
            return latLngBounds.center
        }

        fun tileToBoundsMaplibre(tile: Tile): LatLngBounds {
            val e = tile2lon(tile.x + 1, tile.z)
            val w = tile2lon(tile.x, tile.z)
            val s = tile2lat(tile.y + 1, tile.z)
            val n = tile2lat(tile.y, tile.z)
            val llBoundsBuilder = LatLngBounds.Builder()
            llBoundsBuilder.include(LatLng(s,w))
            llBoundsBuilder.include(LatLng(n,e))

            val latLngBounds = llBoundsBuilder.build()
            return latLngBounds
        }

        fun getXYTile(lat : Double, lon: Double, zoom : Int) : Pair<Int, Int> {
            val latRad = Math.toRadians(lat)
            var xtile = floor( (lon + 180) / 360 * (1 shl zoom) ).toInt()
            var ytile = floor( (1.0 - asinh(tan(latRad)) / PI) / 2 * (1 shl zoom) ).toInt()

            if (xtile < 0) {
                xtile = 0
            }
            if (xtile >= (1 shl zoom)) {
                xtile= (1 shl zoom) - 1
            }
            if (ytile < 0) {
                ytile = 0
            }
            if (ytile >= (1 shl zoom)) {
                ytile = (1 shl zoom) - 1
            }

            return Pair(xtile, ytile)
        }
        fun getBbbikeUrl(region: String, latLngBounds: com.google.android.gms.maps.model.LatLngBounds,
                         format: String): Uri? {
            Timber.i( "region $region")
            val uri = Uri.Builder().scheme("http")
                .authority("extract.bbbike.org")
                .appendQueryParameter("sw_lng", (latLngBounds.southwest.longitude - 0.005).format(4)) // overlap 0.005
                .appendQueryParameter("sw_lat", (latLngBounds.southwest.latitude - 0.005).format(4))
                .appendQueryParameter("ne_lng", (latLngBounds.northeast.longitude + 0.005).format(4))
                .appendQueryParameter("ne_lat", (latLngBounds.northeast.latitude + 0.005).format(4))
                .appendQueryParameter("format", format)
                .appendQueryParameter("city", region) //.appendEncodedPath(HASHTAG)
                .appendQueryParameter("email", "alt.micha@gmail.com")
            return uri.build()
        }
    }
}

