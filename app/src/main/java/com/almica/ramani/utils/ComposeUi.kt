package com.almica.ramani.utils

import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.icu.text.SimpleDateFormat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.Const
import com.almica.ramani.FeatureProperties.Companion.AERODROME
import com.almica.ramani.FeatureProperties.Companion.AEROWAY
import com.almica.ramani.FeatureProperties.Companion.AMENITY
import com.almica.ramani.FeatureProperties.Companion.BEACH
import com.almica.ramani.FeatureProperties.Companion.BUILDING
import com.almica.ramani.FeatureProperties.Companion.CATHEDRAL
import com.almica.ramani.FeatureProperties.Companion.CEMETERY
import com.almica.ramani.FeatureProperties.Companion.CHAPEL
import com.almica.ramani.FeatureProperties.Companion.CHURCH
import com.almica.ramani.FeatureProperties.Companion.CITY
import com.almica.ramani.FeatureProperties.Companion.CONVENIENCE
import com.almica.ramani.FeatureProperties.Companion.DESCRIPTION
import com.almica.ramani.FeatureProperties.Companion.FUEL
import com.almica.ramani.FeatureProperties.Companion.GREEN_CIRCLE_FILLED
import com.almica.ramani.FeatureProperties.Companion.HIGHWAY
import com.almica.ramani.FeatureProperties.Companion.HOSPITAL
import com.almica.ramani.FeatureProperties.Companion.HOTEL
import com.almica.ramani.FeatureProperties.Companion.LANDUSE
import com.almica.ramani.FeatureProperties.Companion.LEISURE
import com.almica.ramani.FeatureProperties.Companion.LINES_TAG
import com.almica.ramani.FeatureProperties.Companion.MAN_MADE
import com.almica.ramani.FeatureProperties.Companion.NAME
import com.almica.ramani.FeatureProperties.Companion.NATURAL
import com.almica.ramani.FeatureProperties.Companion.PARKING
import com.almica.ramani.FeatureProperties.Companion.PEAK
import com.almica.ramani.FeatureProperties.Companion.PHARMACY
import com.almica.ramani.FeatureProperties.Companion.PLACE
import com.almica.ramani.FeatureProperties.Companion.POINT_TAG
import com.almica.ramani.FeatureProperties.Companion.RED_CIRCLE_FILLED
import com.almica.ramani.FeatureProperties.Companion.REGION
import com.almica.ramani.FeatureProperties.Companion.RESTAURANT
import com.almica.ramani.FeatureProperties.Companion.SHOP
import com.almica.ramani.FeatureProperties.Companion.STATE
import com.almica.ramani.FeatureProperties.Companion.SUPERMARKET
import com.almica.ramani.FeatureProperties.Companion.TOWER
import com.almica.ramani.FeatureProperties.Companion.TOWN
import com.almica.ramani.FeatureProperties.Companion.VILLAGE
import com.almica.ramani.FeatureProperties.Companion.WAREHOUSE
import com.almica.ramani.FeatureProperties.Companion.WATER
import com.almica.ramani.Helpers
import com.almica.ramani.Const.Companion.LATLNG_GRID_LAYER
import com.almica.ramani.Const.Companion.LATLNG_GRID_SOURCE
import com.almica.ramani.FeatureProperties
import com.almica.ramani.LatLngH
import com.almica.ramani.MaptypeKey
import com.almica.ramani.R
import com.almica.ramani.RasterMapsItems
import com.almica.ramani.geojsonMaps.GeojsonMapEntity
import com.almica.ramani.geojsonMaps.GeojsonMapRepository
import com.almica.ramani.googlemaps.MapUtils
import com.almica.ramani.pois.PoiEntity
import com.almica.ramani.pois.PoiRepository
import com.almica.ramani.utils.GeoJsonUtils.Companion.createGeojsonMapBoundFeatures
import com.almica.ramani.utils.GeoJsonUtils.Companion.createGeojsonOfflineRegionsBoundFeatures
import com.almica.ramani.utils.GeoJsonUtils.Companion.createMvtBoundFeatures
import com.almica.ramani.utils.GeoJsonUtils.Companion.createRasterMapBoundFeatures
import com.almica.ramani.utils.GeoJsonUtils.Companion.pointToTile
import com.google.gson.annotations.SerializedName
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.ibrahimsn.library.LiveSharedPreferences
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentConstants
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.scalebar.ScaleBarOptions
import org.maplibre.android.plugins.scalebar.ScaleBarPlugin
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.CustomGeometrySource
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.Source
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import timber.log.Timber
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.net.URI
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.concurrent.Executors

private const val logtag = "ComposeUi"
@OptIn(ExperimentalMaterial3Api::class)
fun initScaleBar(mapView: MapView, map: MapLibreMap?) {
    val scaleBarPlugin = map?.let { ScaleBarPlugin(mapView, it) }
    if (scaleBarPlugin != null) {
        //val marginTopDp = TopAppBarDefaults.TopAppBarExpandedHeight.value
        //val r: Resources = context.resources
        // Set the custom styling via a ScaleBarOptions object
        val scaleBarOptions = ScaleBarOptions(mapView.context)
        scaleBarOptions
            .setTextColor(android.R.color.holo_red_dark)
            .setTextSize(24f)
            .setBarHeight(15f)
            .setBorderWidth(3f)
            .setMetricUnit(true)
            .setRefreshInterval(15)
            .setMarginTop(10f)
            .setMarginLeft(16f)
            .setTextBarMargin(15f)
        // Give the plugin the ScaleBarOptions object to style the scale bar
        scaleBarPlugin.create(scaleBarOptions)
//        Timber.i( "initScaleBar ready")
    } else
        Timber.e("initScaleBar map error")
}

fun initMapsGridRaster(
    context: Context,
    visibleProperty: String,
    idTag: String?, // make ids unique
    finished: (Triple<GeoJsonSource, LineLayer, SymbolLayer>?) -> Unit
) {
    createRasterMapBoundFeatures(context) { featureCollection ->
        val sourceId = if (idTag.isNotNull()) "raster_boundaries${Const.GEOJSON_EXT}_$idTag" else
            "raster_boundaries${Const.GEOJSON_EXT}"
        val layerIdLines = if (idTag.isNotNull()) "${context.getString(R.string.raster_maps_grid)}_${idTag}${LINES_TAG}"
                else "${context.getString(R.string.raster_maps_grid)}${LINES_TAG}"
        val layerIdPoints = if (idTag.isNotNull()) "${context.getString(R.string.raster_maps_grid)}_${idTag}${POINT_TAG}"
                else "${context.getString(R.string.raster_maps_grid)}${POINT_TAG}"

        CoroutineScope(Dispatchers.Main).launch { // runOnUiThread
            val geoJsonSource = GeoJsonSource(sourceId, featureCollection,
                GeoJsonOptions().withSynchronousUpdate(true))
//        Timber.i( "${geoJsonSource.id}")

            //Timber.i( "layerIdLines $layerIdLines")
            val geoJsonLineLayer = LineLayer(layerIdLines, geoJsonSource.id).withProperties(
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineDasharray(arrayOf(0.01f, 2f)),
                PropertyFactory.lineWidth(6.0f),
                PropertyFactory.lineOpacity(1.0f),
                lineColor("#FF7F27".toColorInt()),
                PropertyFactory.visibility(visibleProperty)
            )
            val geoJsonSymbolLayer = SymbolLayer(layerIdPoints, geoJsonSource.id)
            //Timber.i( "layerIdPoints $layerIdPoints")

            val name = Expression.toString(Expression.get("name"))
            geoJsonSymbolLayer.setProperties(
                PropertyFactory.iconImage(Expression.switchCase(
                    Expression.eq(
                        Expression.get(STATE),
                        Expression.literal("enabled")
                    ), Expression.literal(GREEN_CIRCLE_FILLED),
                    Expression.eq(
                        Expression.get(STATE),
                        Expression.literal("disabled")
                    ), Expression.literal(RED_CIRCLE_FILLED),
                    Expression.literal(""))
                ),
                PropertyFactory.iconSize(1.5f),
                PropertyFactory.textField(name),
                PropertyFactory.textSize(14F),
                PropertyFactory.textColor(Color.BLACK),
                PropertyFactory.textIgnorePlacement(true),
                PropertyFactory.textAllowOverlap(true),
                PropertyFactory.visibility(Property.NONE)
            )

            geoJsonSymbolLayer.minZoom = 6f
            geoJsonLineLayer.minZoom = 6f
            // only for test purposes
            val fileGeojson = File(context.filesDir, "raster_boundaries${Const.GEOJSON_EXT}")
            fileGeojson.writeText(featureCollection.toJson())

            finished(Triple(geoJsonSource, geoJsonLineLayer, geoJsonSymbolLayer))
        }
    }
}

fun initLatLngGrid(finished: (Pair<Source, Layer>?) -> Unit) {
    val sourceGrid = CustomGeometrySource(LATLNG_GRID_SOURCE, GridProvider())
    val layerGrid = LineLayer(LATLNG_GRID_LAYER, LATLNG_GRID_SOURCE)
    layerGrid.setProperties(
        PropertyFactory.visibility(Property.NONE),
        lineColor("#000000".toColorInt())
    )
    // symbolLayer makes everything invisible
    val symbolLayer = SymbolLayer(LATLNG_GRID_LAYER + "_symbol", LATLNG_GRID_SOURCE)
    symbolLayer.setProperties(
        PropertyFactory.visibility(Property.NONE),
        PropertyFactory.textField(Expression.get(NAME)),
        //PropertyFactory.textOffset(arrayOf(0.1f, 0.1f)),
        PropertyFactory.symbolPlacement(Property.SYMBOL_PLACEMENT_LINE)
    )
    finished(Pair(sourceGrid, symbolLayer))
    return
}
/**
 * 14jan2026
 */
fun initMvtGrid(context: Context,
                finished: (Triple<GeoJsonSource, LineLayer, SymbolLayer>?) -> Unit) {
    val preferences = getDefaultSharedPreferences(context)
    val prefMaptypeKey = preferences.getInt(Const.PREF_MAPTYPE_KEY, 0)
    if (prefMaptypeKey != MaptypeKey.Raster.ordinal) {
        createMvtBoundFeatures(context) { featureCollection, _ ->
            //Timber.i("featureCollection size: ${featureCollection.features()?.size}")
            CoroutineScope(Dispatchers.Main).launch { // runOnUiThread
                val geoJsonSource =
                    GeoJsonSource("mvt_boundaries${Const.GEOJSON_EXT}", featureCollection)
                var layerId = "${context.getString(R.string.mvt_grid)}${LINES_TAG}"
                //Timber.i("layerId $layerId")
                val geoJsonLineLayer = LineLayer(layerId, geoJsonSource.id).withProperties(
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineDasharray(arrayOf(0.01f, 2f)),
                    PropertyFactory.lineWidth(2.5f),
                    PropertyFactory.lineOpacity(1.0f),
                    lineColor(Color.MAGENTA),
                    PropertyFactory.visibility(Property.NONE)
                )

                // SymbolLayer is not displayed with mvt maps 17jan2026
                layerId = context.getString(R.string.mvt_grid) + POINT_TAG
                val geoJsonSymbolLayer = SymbolLayer(layerId, geoJsonSource.id)
                val name = Expression.toString(Expression.get("name"))
                geoJsonSymbolLayer.setProperties(
                    PropertyFactory.iconImage(
                        Expression.switchCase(
                            Expression.eq(
                                Expression.get(STATE),
                                Expression.literal("enabled")
                            ), Expression.literal(GREEN_CIRCLE_FILLED),
                            Expression.eq(
                                Expression.get(STATE),
                                Expression.literal("disabled")
                            ), Expression.literal(RED_CIRCLE_FILLED),
                            Expression.literal("")
                        )
                    ),
                    PropertyFactory.iconSize(1.2f),
                    PropertyFactory.textField(name),
                    PropertyFactory.textSize(10F),
                    PropertyFactory.textColor(Color.BLACK),
                    PropertyFactory.textIgnorePlacement(true),
                    PropertyFactory.textAllowOverlap(true),
                    PropertyFactory.visibility(Property.VISIBLE)
                )

                geoJsonSymbolLayer.minZoom = 6f
                geoJsonLineLayer.minZoom = 4f
                finished(Triple(geoJsonSource, geoJsonLineLayer, geoJsonSymbolLayer))
            }
        }
    } else
        finished(null)
}

fun initMapsGridGeojson(context: Context, finished: (Pair<GeoJsonSource, List<Layer>>?) -> Unit) {
    createGeojsonMapBoundFeatures(context, null) { featureCollection ->
        CoroutineScope(Dispatchers.Main).launch { // runOnUiThread
            val geoJsonSource =
                GeoJsonSource("geojson_boundaries${Const.GEOJSON_EXT}", featureCollection)
            var layerId = context.getString(R.string.geojson_maps_grid) + LINES_TAG
            //Timber.i( "layerId $layerId")
            val linewidthExpression = Expression.switchCase(Expression.eq(Expression.get(DESCRIPTION),
                Expression.literal(context.getString(R.string.region))),
                Expression.toNumber(Expression.literal(5)),
                Expression.toNumber(Expression.literal(3)))
            val geoJsonLineLayer = LineLayer(layerId, geoJsonSource.id).withProperties(
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineDasharray(arrayOf(0.01f, 2f)),
                PropertyFactory.lineWidth(linewidthExpression), //3.0f),
                PropertyFactory.lineOpacity(0.6f),
                lineColor(Color.BLUE),
                PropertyFactory.visibility(Property.NONE)
            )
            layerId = context.getString(R.string.geojson_maps_grid) + POINT_TAG
            val geoJsonSymbolLayerEnabled = SymbolLayer(layerId, geoJsonSource.id)
            //val geoJsonSymbolLayerDisabled = SymbolLayer("{$layerId}_disabled", geoJsonSource.id)
            //val name = Expression.toString(Expression.get("name"))
            val textsizeExpression = Expression.switchCase(Expression.eq(Expression.get(DESCRIPTION),
                Expression.literal(context.getString(R.string.region))),
                Expression.toNumber(Expression.literal(16)),
                Expression.toNumber(Expression.literal(12)))
            val halowidthExpression = Expression.switchCase(Expression.eq(Expression.get(DESCRIPTION),
                Expression.literal(context.getString(R.string.region))),
                Expression.toNumber(Expression.literal(2)),
                Expression.toNumber(Expression.literal(0)))

            geoJsonSymbolLayerEnabled.setProperties(
                PropertyFactory.iconImage(GREEN_CIRCLE_FILLED),
                // works not as expected 30okt2025
                // is ok at first time, but not after changing one map; NO other problem
                // ==> all circles are changed

                PropertyFactory.iconImage(
                    Expression.switchCase(
                        Expression.eq(
                            Expression.get(STATE),
                            Expression.literal("enabled")
                        ), Expression.literal(GREEN_CIRCLE_FILLED),
                        Expression.eq(
                            Expression.get(STATE),
                            Expression.literal("disabled")
                        ), Expression.literal(RED_CIRCLE_FILLED),
                        Expression.literal("")
                    )
                ),
                PropertyFactory.iconSize(1.5f),
                PropertyFactory.symbolZOrder("0"),
                PropertyFactory.textField(Expression.get(NAME)),
                PropertyFactory.textSize(textsizeExpression), //12F),
                PropertyFactory.textColor(Color.BLACK),
                PropertyFactory.textHaloColor(Color.MAGENTA),
                PropertyFactory.textHaloWidth(halowidthExpression), //2f),
                PropertyFactory.textIgnorePlacement(true),
                PropertyFactory.textAllowOverlap(true),
                PropertyFactory.visibility(Property.NONE)
            )

            geoJsonSymbolLayerEnabled.minZoom = 8f
            geoJsonLineLayer.minZoom = 8f
            finished(
                Pair(geoJsonSource, listOf(geoJsonLineLayer, geoJsonSymbolLayerEnabled))
            )
        }
    }
}

fun initRoutesGeojsonLayer(context: Context, file: File): Pair<GeoJsonSource, LineLayer> {
    val prefs = getDefaultSharedPreferences(context)
    val layerVisibility = //Property.VISIBLE
        prefs.getString(context.getString(R.string.pref_routes_geojson_visibility), Property.NONE)
    var geoJsonSource: GeoJsonSource?
    val b = file.exists()
    if (b) {
        val geoMapFileUri = file.toURI()
        val fileUri = "file://" + geoMapFileUri.path
        geoJsonSource = GeoJsonSource(file.name, URI(fileUri))
    } else {
        geoJsonSource = GeoJsonSource(file.name) // placeholder for /data/data/com.almica.ramani/files/routes/home/home_260623_094939.geojson ...
        Timber.i("NOT FOUND: ${file.path}")
    }
    //Timber.i( "${geoJsonSource.id}")
    val layerId = context.getString(R.string.routes) + LINES_TAG
    //Timber.i("layerId $layerId")
    val geoJsonLineLayer = LineLayer(layerId, geoJsonSource.id).withProperties(
        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        PropertyFactory.lineDasharray(arrayOf(2.0f, 3f)),
        //PropertyFactory.lineWidth(4.0f),
        PropertyFactory.lineWidth(
            Expression.interpolate(
                Expression.linear(
                ),
                Expression.zoom(),
                Expression.stop(
                    Expression.literal(8),
                    Expression.literal(1)
                ),
                Expression.stop(
                    Expression.literal(16),
                    Expression.literal(6)
                )
            )
        ),
        PropertyFactory.lineOpacity(0.5f),
        PropertyFactory.visibility(layerVisibility),
        lineColor(Expression.toColor(Expression.get("color")))
    )
    geoJsonLineLayer.minZoom = 8f // = 3f with "europe_boundary${Const.GEOJSON_EXT}"
    return Pair(geoJsonSource, geoJsonLineLayer)
}

/**
 * Initializes a 'hit layer' for route interaction, providing a wider invisible or semi-transparent
 * line area to make it easier for users to select/click on a route.
 * opacity = 0.0 does not work
 */
fun initRoutesHitLayer(context: Context, geoJsonSource: GeoJsonSource): Pair<GeoJsonSource, LineLayer> {
    val layerId = context.getString(R.string.routes) + FeatureProperties.HITLAYER_TAG
    Timber.i("layerId: $layerId")
    val hitLineLayer = LineLayer(layerId, geoJsonSource.id).withProperties(
        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        //PropertyFactory.lineWidth(4.0f),
        PropertyFactory.lineWidth(
            Expression.interpolate(
                Expression.linear(
                ),
                Expression.zoom(),
                Expression.stop(
                    Expression.literal(8),
                    Expression.literal(2)
                ),
                Expression.stop(
                    Expression.literal(16),
                    Expression.literal(20)
                )
            )
        ),
        PropertyFactory.lineOpacity(0.01f),  // 0.0 does not work
        PropertyFactory.visibility(Property.VISIBLE),
        lineColor("#ffffff".toColorInt())
    )
    hitLineLayer.minZoom = 8f // = 3f with "europe_boundary${Const.GEOJSON_EXT}"
    return Pair(geoJsonSource, hitLineLayer)
}

fun initOfflineRegionsGridGeojson(context: Context, finished: (Triple<GeoJsonSource, LineLayer, SymbolLayer>?) -> Unit) {
    getOfflineRegions(context) { regions ->
        if (regions.isNotNull())
            regions?.let {
                Timber.i("regions: ${it.size}")
                val featureCollection = createGeojsonOfflineRegionsBoundFeatures(it)
                val geoJsonSource =
                    GeoJsonSource("offline_boundaries${Const.GEOJSON_EXT}", featureCollection)
                var layerId = context.getString(R.string.offline_regions_grid) + LINES_TAG
                Timber.i("layerId $layerId")
                val geoJsonLineLayer = LineLayer(layerId, geoJsonSource.id).withProperties(
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineDasharray(arrayOf(0.01f, 2f)),
                    PropertyFactory.lineWidth(3.0f),
                    PropertyFactory.lineOpacity(0.6f),
                    lineColor(Color.DKGRAY),
                    PropertyFactory.visibility(Property.NONE)
                )

                layerId = context.getString(R.string.offline_regions_grid) + POINT_TAG
                val geoJsonSymbolLayer = SymbolLayer(layerId, geoJsonSource.id)

                val name = Expression.toString(Expression.get("name"))
                geoJsonSymbolLayer.setProperties(
                    PropertyFactory.iconImage(GREEN_CIRCLE_FILLED),
                    PropertyFactory.iconSize(1.5f),
                    PropertyFactory.textField(name),
                    PropertyFactory.textSize(14F),
                    PropertyFactory.textColor(Color.BLACK),
                    PropertyFactory.textIgnorePlacement(true),
                    PropertyFactory.textAllowOverlap(true),
                    PropertyFactory.visibility(Property.NONE)
                )

                geoJsonSymbolLayer.minZoom = 6f
                geoJsonLineLayer.minZoom = 6f
                finished(Triple(geoJsonSource, geoJsonLineLayer, geoJsonSymbolLayer))
            }
        else
            finished(null)
    }
}

data class RoutesObject(
    @SerializedName("routes") val routes: List<RouteObject>? = null
)
data class RouteObject(
    @SerializedName("bounds") val bounds: Any? = null,
    @SerializedName("copyrights") val copyrights: String? = null,
    @SerializedName("legs") val legs: List<Any>? = null,
    @SerializedName("overview_polyline") val overview_polyline: PointsObject? = null
)
data class PointsObject(
    @SerializedName("points") val points: String? = null
)

data class ElevationResultsObject(
    @SerializedName("results") val elevationResults: List<ElevationResultObject>? = null
)
data class ElevationResultObject(
    @SerializedName("elevation") val elevation: Double? = null,
    @SerializedName("location") val location: LocationObject? = null
)
data class LocationObject(
    @SerializedName("lat") val latitude: Double? = null,
    @SerializedName("lng") val longitude: Double? = null
)
fun gmsOnlineCalc(
    context: Context,
    startY: Double,
    startX: Double,
    stopY: Double,
    stopX: Double,
    alternatives: Boolean,
    finished: (lllh: ArrayList<LatLngH>, name: String, success: Boolean) -> Unit
) {
    val sharedPreferences = getDefaultSharedPreferences(context)
    val s1s2: String? = sharedPreferences.getString(context.getString(R.string.setting_locomotion), Const.DEFAULT_LOCOMOTION)
    val splits: List<String> = s1s2?.split(".") ?: listOf("1", "1")
    val iVehicle = 2.coerceAtMost(Integer.parseInt(splits[0]))
    val mode = when (iVehicle) {0 -> "walking"; 1 -> "bicycling"; 2 -> "driving"
        else -> {"bicycling"}
    }
    Timber.i("vehicle: $iVehicle $mode")
    MapUtils.gmsDirectionsService(
        context, com.google.android.gms.maps.model.LatLng(startY, startX),
        com.google.android.gms.maps.model.LatLng(stopY, stopX), mode, alternatives
    ) { lllh, name, success ->
        finished(lllh as ArrayList<LatLngH>, name, success)
    }
}

fun ghCalc(
    context: Context,
    startY: Double,
    startX: Double,
    stopY: Double,
    stopX: Double,
    finished: (lllh: ArrayList<LatLngH>, name: String, success: Boolean, ghInitError: Boolean) -> Unit
) {
    /* 25dez2025
    MapUtils.gmsDirectionsService(context, com.google.android.gms.maps.model.LatLng(startY, startX),
        com.google.android.gms.maps.model.LatLng(stopY, stopX), "bicycling") {lllh, name, success ->
        finished(lllh as ArrayList<LatLngH>, name, success, false)
    }
     */
    val ghManager = GhHelper.getGhManager(context)
    if (ghManager == null) {
        Timber.i(context.getString(R.string.gh_not_initialzed))
        finished(ArrayList<LatLngH>(), "Gh.Error", false, true)
        return
    }
    //val aldi = alertProgressIndeterminate(getString(R.string.route_calculation), GhHelper.getVehicleIcon(context))
    val lllh = java.util.ArrayList<LatLngH>()
    val timeFormat = SimpleDateFormat(Const.TIME_PATTERN_LONG, Locale.getDefault())
    val vehicle = ghManager.getVehicle(context)
    CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
        val ghResponse =
            ghManager.startRequest(context, startY, startX, stopY, stopX)

        val distM = ghResponse.distance
        val ghPoints = ghResponse.points
        //val instructions = ghResponse.instructions

        Timber.i( "distM: $distM points:${ghResponse.points.size()}")
        for (i in 0 until ghPoints.size()) {
            lllh.add(
                LatLngH(
                    ghPoints.getLatitude(i),
                    ghPoints.getLongitude(i),
                    ghPoints.getElevation(i)
                )
            )
        }
    }.invokeOnCompletion {
        //aldi.dismiss()
        try {
            if (lllh.isEmpty()) {
                lllh.add(LatLngH(startY, startX))
                lllh.add(LatLngH(stopY, stopX))
            }
            val name = vehicle?.let { it1 -> "${Const.GH_TAG}.${it1[0]}.${timeFormat.format(Date())}" }
                ?: "${Const.GH_TAG}.${timeFormat.format(Date())}"
            finished(lllh, name, true, ghManager.hasInitError())
            //val instructions = Helpers.createInstructionsFromLllh(context, lllh)
            //Timber.i( "instructions:${instructions.size}")
        } catch (e: IOException) {
            e.message?.let { it1 -> Timber.i( it1) }
        }
    }
}

fun launchOrsRouting(context: Context,
                     startY: Double,
                     startX: Double,
                     stopY: Double,
                     stopX: Double,
                     roundTrip: Boolean, finished: (ArrayList<LatLngH>, name: String, success: Boolean) -> Unit) {
    (context as LifecycleOwner).lifecycleScope.launch(Dispatchers.IO) {
        Timber.i( "roundTrip: $roundTrip")
        Helpers.onlineOrsRouting(
            context,
            startY,
            startX,
            stopY,
            stopX,
            roundTrip
        ) { lllhArray, name, success, errorMsg ->
        Timber.i("name:$name success:$success")
            if (lllhArray.isNotEmpty().and(success)) {
                Timber.i( "ors routes: ${lllhArray.size}")
                val lllh = arrayListOf<LatLngH>()
                lllh.addAll(lllhArray[0])
                if (lllhArray.size > 1)
                    lllh.addAll(lllhArray[1].reversed())
                val timeFormat =
                    SimpleDateFormat(Const.TIME_PATTERN_LONG, Locale.getDefault())
                val name = Const.ORS_TAG + "." + timeFormat.format(Date())
                finished(lllh, name, true)
            } else {
                finished(arrayListOf(), "", false)
            }
        }
    }
}

fun selectRasterMaps(map: MapLibreMap, context: Context, activeMaps: (Int, String) -> Unit) {
    val visibleRegion = map.projection.visibleRegion.latLngBounds
    val rootFolder = context.filesDir
    val mbTilesRootFolder = File(rootFolder, Const.MBTILES_FOLDER)
    mbTilesRootFolder.mkdirs()
    val prefs = getDefaultSharedPreferences(context)
    val prefMapType = prefs.getString(context.getString(R.string.pref_tilemaker_maptype), Const.OUTDOOR)
    val liveSharedPreferences = LiveSharedPreferences(prefs)
    val mbTilesPrefSet = mutableSetOf<String>()
        //liveSharedPreferences.getStringSet(Const.PREF_MBTILES_FILEPATH_SET, setOf()).value
    val fileFilter = FileFilter { file: File? -> file?.name?.endsWith(Const.MBTILES_EXT) == true &&
            !file.name.contains(Const.JOURNAL) &&
            file.name.contains(prefMapType.toString())
    }
    val files = mbTilesRootFolder.listFiles(fileFilter) as Array<File>
    files.sortWith(compareBy { it.name })
    Timber.i( "files: ${files.size}")
    files.forEachIndexed { index, file ->
        val splits = file.name.split(Const.UNDERLINE, ".", limit = 6)
        /*
                        0 = "tile"
                        1 = "1082"
                        2 = "672"
                        3 = "11"
                        4 = "OpenTopo"
                        5 = "mbtiles"
        */
        if (splits.size > 3) {
            val bounds = GeoJsonUtils.tileToGmsBounds(
                GeoJsonUtils.Companion.Tile(
                    splits[1].toInt(),
                    splits[2].toInt(),
                    splits[3].toInt()
                )
            )
            val boundsBuilder = LatLngBounds.Builder()
            boundsBuilder.include(LatLng(bounds.southwest.latitude, bounds.southwest.longitude))
            boundsBuilder.include(LatLng(bounds.northeast.latitude, bounds.southwest.longitude))
            boundsBuilder.include(LatLng(bounds.northeast.latitude, bounds.northeast.longitude))
            boundsBuilder.include(LatLng(bounds.southwest.latitude, bounds.northeast.longitude))
            boundsBuilder.include(LatLng(bounds.southwest.latitude, bounds.southwest.longitude))
            val intersectBounds = visibleRegion.intersect(boundsBuilder.build())
            val state = intersectBounds != null && intersectBounds.isEmptySpan.not()
            if (state)
                mbTilesPrefSet.add(file.path)
        } else
            Timber.i( "invalid mapname: ${file.name}")
    }
    Timber.i( "$mbTilesPrefSet")
    liveSharedPreferences.preferences.edit {
        putStringSet(
            Const.PREF_MBTILES_FILEPATH_SET,
            mbTilesPrefSet
        )
    }

    prefMapType?.let { activeMaps(mbTilesPrefSet.size, it) }
}

/*
fun selectGeojsonRegion(map: MapLibreMap, context: Context, finished: (Int) -> Unit) {
    val enabledMaps = ArrayList<GeojsonMapEntity>()
    //val visibleRegion = map.projection.visibleRegion.latLngBounds
    val pos = map.cameraPosition.target
    val tile = pos?.let { pointToTile(pos.longitude, it.latitude, 10.0) }
    if (tile != null) {
        val region = "tile_${tile.x}_${tile.y}_${tile.z}"
        val bounds = tileToBounds(tile)
        val geojsonMapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
        geojsonMapRepository.getAllSimple(false) { geojsonMapEntities ->
            Timber.i( "geojsonMapEntities: ${geojsonMapEntities.size}")
            geojsonMapEntities.forEach { geojsonMapEntity ->
                val intersectBounds = bounds.intersect(geojsonMapEntity.getBounds())
                val state = intersectBounds != null && intersectBounds.isEmptySpan.not()
                if (state) {
                    geojsonMapRepository.updateGeojsonMapStatus(true, geojsonMapEntity.name, region) {}
                    enabledMaps.add(geojsonMapEntity)
                } else
                    geojsonMapRepository.updateGeojsonMapStatus(false, geojsonMapEntity.name) {}
            }
        }
    }
}
 */

fun checkGeojsonMaps(map: MapLibreMap, context: Context) {
    val visibleRegion = map.projection.visibleRegion.latLngBounds
    val geojsonMapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
    var triggerRepaint = false
    geojsonMapRepository.getAllSimpleEnabled {geojsonMapEntities ->
        //Timber.i( "geojsonMapEntities: ${geojsonMapEntities.size}")
        geojsonMapEntities.forEach { geojsonMapEntity ->
//            val geojsonMapFile = File(geojsonMapEntity.path)
//            if (geojsonMapFile.exists()) {
            val intersectBounds = visibleRegion.intersect(geojsonMapEntity.getBounds())
            //Timber.i( "${geojsonMapEntity.name} intersectBounds: ${intersectBounds}")
            val newVisibility = if (!(intersectBounds == null || intersectBounds.isEmptySpan))
                Property.VISIBLE else Property.NONE
            if (changeLayerVisibility(map, newVisibility, geojsonMapEntity.name)) {
                //Timber.i( "visibility change $newVisibility: ${geojsonMapEntity.name}")
                triggerRepaint = true
            }
        }
    }
    if (triggerRepaint) {
        map.triggerRepaint()
        Timber.i( "triggerRepaint launched")
    }
}

fun GeojsonMapEntity.getBounds(): LatLngBounds {
    val bounds = LatLngBounds.Builder()
    bounds.include(LatLng(north, west))
    bounds.include(LatLng(south, east))
    return bounds.build()
}

fun getLayer(map: MapLibreMap, filter: String): Layer? {
    Timber.i( "filter: $filter")
    var result : Layer? = null
    map.style?.layers?.forEach { layer ->
        if (layer.id == filter) {
//            Timber.i( "toggle -> ${layer.id}:")
            result = layer
            return@forEach
        }
    }
    return result
}

fun getLayerVisibility(map: MapLibreMap, filter: String): Boolean {
    //Timber.i( "filter: $filter")
    var result = false
    map.style?.layers?.forEach { layer ->
        if (layer.id.startsWith(filter)) {
//            Timber.i( "toggle -> ${layer.id}:")
            result = layer.visibility.value != Property.NONE
            return@forEach
        }
    }
    return result
}

fun removeLayers(map: MapLibreMap, filter: String) {
    Timber.i( "filter: $filter")
    val layersToRemove = arrayListOf<Layer>()
    map.style?.layers?.forEach { layer ->
        if (layer.id.startsWith(filter)) {
            layersToRemove.add(layer)
        }
    }
    var removeCount = 0
    layersToRemove.forEach { layer ->
        val b = map.style?.removeLayer(layer)
        if (b == true) removeCount++
    }
    Timber.i("removed layers: $removeCount")
}

fun toggleLayerVisibility(map: MapLibreMap, filter: String): Layer? {
    Timber.i( "filter: $filter")
    var result : String
    var changedLayer: Layer? = null
    map.style?.layers?.forEach { layer ->
        if (layer.id.contains(filter) ) {
            result = if (layer.visibility.value == Property.NONE) Property.VISIBLE else Property.NONE
            Timber.i( "toggle -> ${layer.id} $result")
            layer.setProperties(
                PropertyFactory.visibility(if (layer.visibility.value == Property.NONE) Property.VISIBLE else Property.NONE)
            )
            changedLayer = layer
        }
    }
    return changedLayer
}

fun logLayers(map: MapLibreMap) {
    map.style?.layers?.forEach { layer ->
        Timber.i( "layer: ${layer.id} visibility: ${layer.visibility.value}")
    }
}

fun checkLayerVisibility(map: MapLibreMap?, filter: String): Boolean {
    Timber.i( "filter: $filter layers: ${map?.style?.layers?.size}")
    map?.style?.layers?.forEach { layer ->
//        Timber.i("layer: ${layer.id} visible: ${layer.visibility.value}")
        if (layer.id.contains(filter)) {
            //Timber.i( "${layer.id} visible: ${layer.visibility.value}")
            return  layer.visibility.value == Property.VISIBLE
        }
    }
    //Timber.i( "$filter layer not found")
    return false
}

fun changeLayerVisibility(map: MapLibreMap?, visibilityProperty: String, filter: String): Boolean {
    //Timber.i( "filter: $filter $visibilityProperty")
    var triggerRepaint = false
    map?.style?.layers?.forEach { layer ->
//        Timber.i( "layer id: ${layer.id}")
        if (layer.id.contains(filter) && layer.visibility.value != visibilityProperty) {
            Timber.i( "change -> $visibilityProperty ${layer.id}")
            layer.setProperties(PropertyFactory.visibility(visibilityProperty))
            triggerRepaint = true
        }
    }
    //Timber.i( "$filter $visibilityProperty triggerRepaint: $triggerRepaint")
    return triggerRepaint
}

fun initMapComponentsLocalStyle(
    context: Context,
    style: Style?,
    useCyclewayOverlays: Boolean,
    mvtBounds: LatLngBounds?,
    mvtPath: String?,
    progressMsg: (String?) -> Unit
) {
    //Timber.i( "initMapComponentsLocalStyle start")
    setPlanetVisibility(context, null, style)
    val prefs = getDefaultSharedPreferences(context)
    val prefGeojsonFolderPath = prefs.getString(Const.PREF_GEOJSON_FILEPATH, "")
    //Timber.i("prefGeojsonFolderPath: $prefGeojsonFolderPath")
    if (prefGeojsonFolderPath.isNullOrEmpty()) {  //17feb2026 qgis geojson
        // native local geojson files
        val geoJsonComponents = GeoJsonComponents(
            arrayListOf(),
            arrayListOf(), arrayListOf(), arrayListOf()
        )
        val geojsonMapRepository =
            GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
        geojsonMapRepository.getAllSimpleEnabled { geojsonMapEntities ->
            //Timber.i("geojsonMapEntities: ${geojsonMapEntities.size}")
            geojsonMapEntities.forEach { geojsonMapEntity ->
                val geoJsonComponentsSingle = GeoJsonUtils.initGeoJsonComponents(geojsonMapEntity)
                geoJsonComponents.geoJsonSources.addAll(geoJsonComponentsSingle.geoJsonSources)
                geoJsonComponents.geoJsonLineLayers.addAll(geoJsonComponentsSingle.geoJsonLineLayers)
                geoJsonComponents.geoJsonFillLayers.addAll(geoJsonComponentsSingle.geoJsonFillLayers)
                geoJsonComponents.geoJsonSymbolLayers.addAll(geoJsonComponentsSingle.geoJsonSymbolLayers)
            }
            geoJsonComponents.geoJsonSources.forEach { source -> style?.addSource(source) } //mapSources.add(source) }
            geoJsonComponents.geoJsonFillLayers.forEach { layer -> style?.addLayer(layer) } //style?.layers?.add(layer) }
            geoJsonComponents.geoJsonLineLayers.forEach { layer -> style?.addLayer(layer) }
            geoJsonComponents.geoJsonSymbolLayers.forEach { layer -> style?.addLayer(layer) }
        }
    }

    initMapsGridRaster(context, Property.NONE, "${System.currentTimeMillis()}") { mapsGridRaster ->  // async
        //Timber.i("initMapsGridRaster success: ${mapsGridRaster.isNotNull()}")
        if (mapsGridRaster != null) {
            //Timber.i("initMapsGridRaster source: ${mapsGridRaster.first.id}")
            style?.addSource(mapsGridRaster.first) //mapSources.add(mapsGridRaster.first)
            style?.addLayer(mapsGridRaster.second) // lines
            //style?.addLayer(mapsGridRaster.third) // symbols  makes all layers invisible
            //Timber.i("add layer: ${mapsGridRaster.second.id}")
        }
    }

    initMapsGridGeojson(context) { result -> // async
        //Timber.i( "initMapsGridGeojson success: ${result.isNotNull()}")
        if (result != null) {
            style?.addSource(result.first) //mapSources.add(result.first)
            style?.addLayer(result.second[0])
            //Timber.i("add source: ${result.first.id}")
        }
    }

    val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
    val file = File(rootRouteFolder, "routes${Const.GEOJSON_EXT}") // "europe_boundary${Const.GEOJSON_EXT}")
    initRoutesOverlay(context, style, file) { _ ->
        //Timber.i( "initRoutesOverlay finished routeLayerId: $routeLayerId")
        progressMsg(null)

        val sourceGrid = CustomGeometrySource(LATLNG_GRID_SOURCE, GridProvider())
        val layerGrid = LineLayer(LATLNG_GRID_LAYER + "_line", LATLNG_GRID_SOURCE)
        layerGrid.setProperties(
            PropertyFactory.visibility(Property.NONE),
            lineColor("#000000".toColorInt())
        )
        // symbolLayer makes lat lng grid invisible
//        val symbolLayer = SymbolLayer(LATLNG_GRID_LAYER + "_symbol", LATLNG_GRID_SOURCE)
//        symbolLayer.setProperties(
//            PropertyFactory.visibility(Property.NONE),
//            PropertyFactory.textField(Expression.get(NAME)),
//            //PropertyFactory.textOffset(arrayOf(0.1f, 0.1f)),
//            PropertyFactory.symbolPlacement(Property.SYMBOL_PLACEMENT_LINE)
//        )
        style?.addSource(sourceGrid)
        style?.addLayer(layerGrid)
//        style?.addLayer(symbolLayer)
        //
        style?.let {
            mvtBounds?.let { it1 ->
                showBoundsArea(it,
                    it1, Color.RED, "source${mvtPath}-bounds",
                    "layer${mvtPath}-bounds", 0.25f )
            }
        }

        initMvtGrid(context) { result ->
            result?.let { it1 ->
//                Timber.i( "map layers: ${style?.layers?.size}")
                style?.addSource(it1.first)
                style?.addLayer(it1.second)
//                Timber.i( "map layers: ${style?.layers?.size}")
            }
            initCycleOverlay(context, style, useCyclewayOverlays) {
                //Timber.i( "initCycleOverlay finished")
            }
            //Timber.i( "initMvtGrid success: ${result.isNotNull()}")
            progressMsg(null)
        }
    }
}

fun initMapComponentsMaptypeRaster(
    context: Context, style: Style?, useCyclewayOverlays: Boolean,
    mapTypeKey: Int,
    finished: () -> Unit
) {
    Timber.i( "initMapComponentsMaptypeRaster mapTypeKey: $mapTypeKey")
    val mapLayers = java.util.ArrayList<Layer>()
    val mapSources = java.util.ArrayList<Source>()
    val mergedRasterMaps = RasterMapsItems(arrayListOf(), arrayListOf())
    val rasterMaps = Helpers.createRasterMapsLayers(context)
    val cyclewayOverlayMaps = Helpers.createCyclewayMapsLayers(context)

    if (mapTypeKey == MaptypeKey.Raster.ordinal && rasterMaps.isNotNull()) {
        Timber.i( "" + "rasterMaps: ${rasterMaps.rasterSourceList.size}")

        rasterMaps.let {
            mergedRasterMaps.rasterSourceList.addAll(it.rasterSourceList)
            mergedRasterMaps.rasterLayerList.addAll(it.rasterLayerList)
        }
        if (cyclewayOverlayMaps.isNotNull()) {
            cyclewayOverlayMaps.let {
                mergedRasterMaps.rasterSourceList.addAll(it.rasterSourceList)
                mergedRasterMaps.rasterLayerList.addAll(it.rasterLayerList)
            }
        }

        mapLayers.addAll(mergedRasterMaps.rasterLayerList)
        mapSources.addAll(mergedRasterMaps.rasterSourceList)
        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
        val file = File(rootRouteFolder, "routes${Const.GEOJSON_EXT}") // "europe_boundary${Const.GEOJSON_EXT}")
        val routesGeojson = initRoutesGeojsonLayer(context, file)
        mapSources.add(routesGeojson.first)
        mapLayers.add(routesGeojson.second)
        val routesHitLayer = initRoutesHitLayer(context, routesGeojson.first)
        //mapSources.add(routesHitLayer.first)
        mapLayers.add(routesHitLayer.second)
        val sourceGrid = CustomGeometrySource(LATLNG_GRID_SOURCE, GridProvider())
        val layerGrid = LineLayer(LATLNG_GRID_LAYER, LATLNG_GRID_SOURCE)
        layerGrid.setProperties(
            PropertyFactory.visibility(Property.NONE),
            lineColor("#000000".toColorInt())
        )
        // symbolLayer makes lat lng grid invisible
//        val symbolLayer = SymbolLayer(LATLNG_GRID_LAYER + "_symbol", LATLNG_GRID_SOURCE)
//        symbolLayer.setProperties(
//            PropertyFactory.visibility(Property.NONE),
//            PropertyFactory.textField(Expression.get(NAME)),
//            //PropertyFactory.textOffset(arrayOf(0.1f, 0.1f)),
//            PropertyFactory.symbolPlacement(Property.SYMBOL_PLACEMENT_LINE)
//        )
        mapSources.add(sourceGrid)
        mapLayers.add(layerGrid)
        //mapLayers.add(symbolLayer)

        initMapsGridRaster(context, Property.NONE, "${System.currentTimeMillis()}") { mapsGridRaster ->  // async
            if (mapsGridRaster != null) {
                mapSources.add(mapsGridRaster.first) // source
                mapLayers.add(mapsGridRaster.second) // lines
                //mapLayers.add(mapsGridRaster.third) // symbols
                Timber.i("add layers: ${mapsGridRaster.second.id}") // ${mapsGridRaster.third.id}")
            } else
                Timber.i("mapsGridRaster = null")

            // add to map style
            mapSources.iterator().forEach { source ->
                Timber.i("style add source: ${source.id}")
                style?.addSource(source)
            }

            mapLayers.iterator().forEach { layer ->
                Timber.i("style add layer: ${layer.id}")
                style?.addLayerBelow(layer, LocationComponentConstants.ACCURACY_LAYER)
            }
            finished()
        }

        return
    }
}

/**
 * 13jan2026 test with local vector database
 */
fun initRoutesOverlay(context: Context, style: Style?, file: File,
                      finished: (String?) -> Unit) {
    val routesGeojson = initRoutesGeojsonLayer(context, file)
    val mapLayers = java.util.ArrayList<Layer>()
    val mapSources = java.util.ArrayList<Source>()
    mapSources.add(routesGeojson.first)
    mapLayers.add(routesGeojson.second)
    val routesHitLayer = initRoutesHitLayer(context, routesGeojson.first)
    //mapSources.add(routesHitLayer.first)
    mapLayers.add(routesHitLayer.second)

    mapSources.iterator().forEach { source ->
        style?.addSource(source)
    }
    mapLayers.iterator().forEach { layer ->
        style?.addLayerBelow(layer, LocationComponentConstants.ACCURACY_LAYER)
    }
    finished(routesGeojson.second.id)
}

fun initCycleOverlay(
    context: Context,
    style: Style?,
    useCyclewayOverlays: Boolean,
    finished: () -> Unit
) {
//    Timber.i("useCyclewayOverlays: $useCyclewayOverlays")
    val cyclewayOverlayMaps = Helpers.createCyclewayMapsLayers(context)
    //Timber.i("cyclewayOverlayMaps: ${cyclewayOverlayMaps.rasterSourceList.size}")
    if (cyclewayOverlayMaps.isNotNull()) {
        cyclewayOverlayMaps.let {
            it.rasterSourceList.forEach { source ->
                style?.addSource(source)
            }

            it.rasterLayerList.forEach { layer ->
                //Timber.i("layer: ${layer.id} ${layer.sourceId}")
                layer.setProperties(if (useCyclewayOverlays) PropertyFactory.visibility(Property.VISIBLE)
                else PropertyFactory.visibility(Property.NONE))
                //layer.setProperties(PropertyFactory.symbolZOrder(Expression.literal(2)))
                style?.addLayer(layer)
            }
        }
    }
    finished()
}

fun setCyclewayLayersVisibility(context: Context, v: String, style: Style?) {
    val sharedPreferences = getDefaultSharedPreferences(context)
    val layerIds = ArrayList<String>()
    val rasterMapFilePathSet = sharedPreferences.getStringSet(Const.PREF_CYCLEWAY_OVERLAYS_FILEPATH_SET, null)
    if (rasterMapFilePathSet.isNotNull()) {
        rasterMapFilePathSet?.forEach { path ->
            val rasterMapFile = File(path.toString())
            if (rasterMapFile.exists()) {
                val id = rasterMapFile.name.replace(Const.MBTILES_EXT, "")
                layerIds.add("${id}-layer")
            } else
                Timber.e( "Not found: ${rasterMapFile.absolutePath}")
        }
    }
    //Timber.i("cycle way layers: ${layerIds.size}")
    val layers = style?.layers?.iterator()
    layers?.forEach { layer ->
        if (layer is RasterLayer) {
            if (layerIds.contains(layer.id)) {
                layer.setProperties(PropertyFactory.visibility(v))
                Timber.i( "${layer.id} visibility: ${layer.visibility}")
            }
        }
    }
}

fun setMainLayersVisibility(context: Context, style: Style?, prefMaptypeKey: Int) {
    Timber.i("prefMaptypeKey: $prefMaptypeKey")
    when (prefMaptypeKey) {
        MaptypeKey.GeoJson.ordinal -> {
            setGeojsonMapVisibility(context, Property.VISIBLE, style)
            setRasterMapVisibility(context, Property.NONE, style)
            setMaplibreLayersVisibility(Property.NONE, style)
            setHillshadeVisibility(context, null, style)
            setPlanetVisibility(context, null, style)
        }

        MaptypeKey.Raster.ordinal -> {
            setGeojsonMapVisibility(context, Property.NONE, style)
            setRasterMapVisibility(context, Property.VISIBLE, style)
            setMaplibreLayersVisibility(Property.NONE, style)
            setHillshadeVisibility(context, false, style)
            setPlanetVisibility(context, false, style)
        }

        MaptypeKey.None.ordinal -> {
            setGeojsonMapVisibility(context, Property.NONE, style)
            setRasterMapVisibility(context, Property.NONE, style)
            setMaplibreLayersVisibility(Property.VISIBLE, style)
            setHillshadeVisibility(context, null, style)
            setPlanetVisibility(context, null, style)
        }

        MaptypeKey.Mvt.ordinal -> {
            setGeojsonMapVisibility(context, Property.NONE, style)
            setRasterMapVisibility(context, Property.NONE, style)
            setMaplibreLayersVisibility(Property.NONE, style)
            setHillshadeVisibility(context, null, style)
            setPlanetVisibility(context, null, style)
        }
    }
}

fun setGeojsonMapVisibility(context: Context, v: String, style: Style?) {
    val geojsonMapSourceIds = arrayListOf<String>()
    val geojsonMapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
    geojsonMapRepository.getAllSimpleEnabled { geojsonMapEntities ->
        Timber.i("geojsonMapEntities: ${geojsonMapEntities.size}")
        geojsonMapEntities.forEach { geojsonMapEntity ->
            geojsonMapSourceIds.add(geojsonMapEntity.name)
            geojsonMapSourceIds.add("${geojsonMapEntity.name}_background")
        }
    }
    val layers = style?.layers?.iterator()
    layers?.forEach { layer ->
        if (layer is LineLayer) {
            if (geojsonMapSourceIds.contains(layer.sourceId))
                layer.setProperties(PropertyFactory.visibility(v))
        } else if (layer is SymbolLayer) {
            if (geojsonMapSourceIds.contains(layer.sourceId))
                layer.setProperties(PropertyFactory.visibility(v))
        } else if (layer is FillLayer) {
            if (geojsonMapSourceIds.contains(layer.sourceId)) {
                layer.setProperties(PropertyFactory.visibility(v))
//                Timber.i( "${layer.id} ${layer.visibility}")
            }
        }
    }
    //map.triggerRepaint()
}

fun setRasterMapVisibility(context: Context, v: String, style: Style?) {
    Timber.i( "v: $v")
    val rasterMapSourceIds = arrayListOf<String>()
    val sharedPreferences =
        getDefaultSharedPreferences(context)
    val rasterMapFilePathSet =
        sharedPreferences.getStringSet(Const.PREF_MBTILES_FILEPATH_SET, null)
    if (rasterMapFilePathSet.isNotNull()) {
        Timber.i(
            "rasterMapFilePathSet: ${rasterMapFilePathSet?.size}")
        rasterMapFilePathSet?.forEach { path ->
            val rasterMapFile = File(path.toString())
            if (rasterMapFile.exists()) {
                val id = rasterMapFile.name.replace(Const.MBTILES_EXT, "")
                rasterMapSourceIds.add(id)
            } else
                Timber.e("NOT FOUND: ${rasterMapFile.path}")
        }
    }
    val layers = style?.layers?.iterator()
    layers?.forEach { layer ->
        if (layer is RasterLayer) {
            Timber.i( "RasterLayer: ${layer.id} ${layer.sourceId}")
            if (rasterMapSourceIds.contains(layer.sourceId)) {
                layer.setProperties(PropertyFactory.visibility(v))
                Timber.i( "${layer.visibility} ${layer.id}")
            }
        }
    }
    //map.triggerRepaint()
}

fun setHillshadeVisibility(context: Context, forceVisibility: Boolean?, style: Style?) {
    val sharedPreferences = getDefaultSharedPreferences(context)
    val vb = forceVisibility ?: sharedPreferences.getBoolean(Const.PREF_HILLSHADE_VISIBILITY, false)
    val v = if (vb) Property.VISIBLE else Property.NONE
    Timber.i("hillshadeState: $v")
    val layers = style?.layers?.iterator()
    layers?.forEach { layer ->
        if (layer.id == Const.RASTER_DEM_LAYER) {
            layer.setProperties(PropertyFactory.visibility(v))
            Timber.i( "${layer.id} visibility: ${layer.visibility}")
            return@forEach
        }
    }
}

fun setPlanetVisibility(context: Context, forceVisibility: Boolean?, style: Style?) {
    val sharedPreferences = getDefaultSharedPreferences(context)
    val vb = forceVisibility ?: sharedPreferences.getBoolean(Const.PREF_PLANET_VISIBILITY, false)
    val v = if (vb) Property.VISIBLE else Property.NONE
    //Timber.i("planetState: $v")
    val layers = style?.layers?.iterator()
    layers?.forEach { layer ->
        if (layer.id.startsWith(Const.PLANET_LAYER_TAG) ||
                layer.id.startsWith(Const.COUNTRIES_LAYER_TAG)) {
            layer.setProperties(PropertyFactory.visibility(v))
            //Timber.i( "${layer.id} visibility: ${layer.visibility}")
            return@forEach
        }
    }
}

fun checkMvtMap(cp: LatLng, context: Context): String? {
    val mvtTile: GeoJsonUtils.Companion.Tile =
        pointToTile(cp.longitude, cp.latitude, 9.0)
    //val mvtBounds = GeoJsonUtils.tileToGmsBounds(mvtTile)
    //Timber.i("mvtBounds: $mvtBounds")
    val preferences = getDefaultSharedPreferences(context)
    val currentMvtPath = preferences.getString(Const.PREF_MVT_FILEPATH, null)
    val mvtname = "mvt_${mvtTile.x}_${mvtTile.y}_${mvtTile.z}${Const.MBTILES_EXT}"
    val mvtDir = File(context.filesDir, Const.MVT_FOLDER)
    val mvtFile = File(mvtDir, mvtname)
    if (mvtFile.exists() && currentMvtPath != mvtFile.path) {
        Timber.i("Mvt map option: ${mvtFile.name}")
        return mvtFile.path
    }
    return null
}

fun showBoundsArea(
    loadedMapStyle: Style,
    bounds: LatLngBounds,
    color: Int,
    sourceId: String,
    layerId: String,
    opacity: Float
) {
    val outerPoints: MutableList<Point> = ArrayList()

    outerPoints.add(Point.fromLngLat(bounds.northWest.longitude, bounds.northWest.latitude))
    outerPoints.add(Point.fromLngLat(bounds.northEast.longitude, bounds.northEast.latitude))
    outerPoints.add(Point.fromLngLat(bounds.southEast.longitude, bounds.southEast.latitude))
    outerPoints.add(Point.fromLngLat(bounds.southWest.longitude, bounds.southWest.latitude))
    outerPoints.add(Point.fromLngLat(bounds.northWest.longitude, bounds.northWest.latitude))

    loadedMapStyle.removeLayer(layerId)
    loadedMapStyle.removeSource(sourceId)

    loadedMapStyle.addSource(
        GeoJsonSource(
            sourceId,
            Polygon.fromLngLats(mutableListOf(outerPoints.toMutableList()))
        )
    )

    loadedMapStyle.addLayer(
        FillLayer(layerId, sourceId).withProperties(
            fillColor(color),
            fillOpacity(opacity),
            PropertyFactory.visibility(Property.NONE)
        )
    )

    loadedMapStyle.addLayer(
        LineLayer("${layerId}_", sourceId).withProperties(
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            //PropertyFactory.lineDasharray(arrayOf(0.01f, 2f)),
            PropertyFactory.lineWidth(2.0f),
            PropertyFactory.lineOpacity(0.6f),
            lineColor(Color.RED),
            PropertyFactory.visibility(Property.VISIBLE)
        )
    )
}

fun checkMaplibreLayersVisibility(map: MapLibreMap?): Boolean {
    val layers = map?.style?.layers?.iterator()
    layers?.forEach { layer ->
        if (MaplibreLayers().list.containsKey(layer.id)
            && layer.visibility.value == Property.VISIBLE) {
            return true
        }
    }
    return false
}

fun setMaplibreLayersVisibility(v: String, style: Style?) {
    Timber.i( "setMaplibreLayersVisibility: $v")
    val layers = style?.layers?.iterator()
    var count = 0
    layers?.forEach { layer ->
        if (MaplibreLayers().list.containsKey(layer.id)) {
            layer.setProperties(PropertyFactory.visibility(v))
            count++
        }
    }
    Timber.i( "layers affected: $count")
}

fun getVisibleMapFeatures(
    context: Context,
    map: MapLibreMap,
    latLngClick: com.google.android.gms.maps.model.LatLng?
): List<FeatureItem> {
    val bounds = map.projection.visibleRegion.latLngBounds
    val pointNW: PointF = map.projection.toScreenLocation(bounds.northWest)
    val pointSO: PointF = map.projection.toScreenLocation(bounds.southEast)
    val rectF = RectF(pointNW.x, pointNW.y, pointSO.x, pointSO.y)
    //Timber.i( "rectF: $rectF")
    val latLngGms = com.google.android.gms.maps.model.LatLng(bounds.center.latitude, bounds.center.longitude)
    val filter = Expression.has(NAME)
    val features: List<Feature> = map.queryRenderedFeatures(rectF, filter)
    Timber.i("features: ${features.size}")
    if (features.isNotEmpty()) {
        val selectedFeatures = HashMap<String, FeatureItem>()
        for (feature in features) {
            val featureItem =
                getFeatureItem(context, map, feature, latLngGms)
            val title = feature.getStringProperty(NAME)
            if (featureItem != null && title != null && title.isNotEmpty()) {
                selectedFeatures[title] = featureItem
//            Timber.i("feature: " + title + " " + feature.geometry()!!.type())
            }
        }
        if (selectedFeatures.isNotEmpty() && latLngClick != null) {
            val latLngGms = com.google.android.gms.maps.model.LatLng(
                latLngClick.latitude,
                latLngClick.longitude
            )
            val sortedFeatureItems =
                selectedFeatures.values.sortedWith(DistComparatorCenter(latLngGms))
            return sortedFeatureItems
        }
    }
    return emptyList()
}
/**
 * FeatureItem is used for feature presentation
 */
fun createPoiIconsMap(): MutableMap<Int?, Int?> {
    val iconMap: MutableMap<Int?, Int?> = HashMap<Int?, Int?>()
    iconMap[RESTAURANT_POICAT_ID] = R.drawable.s_food_small
    iconMap[CAFE_POICAT_ID] = R.drawable.s_cafe_small
    iconMap[HOTEL_POICAT_ID] = R.drawable.s_accommo_small
    iconMap[CHURCH_POICAT_ID] = R.drawable.s_pow_small
    iconMap[VILLAGE_POICAT_ID] = R.drawable.circle_red_16px
    iconMap[SUPERMARKET_POICAT_ID] = R.drawable.ic_supermarket
    iconMap[BAKERY_POICAT_ID] = R.drawable.ic_bakery
    iconMap[HOSPITAL_POICAT_ID] = R.drawable.s_health_small
    iconMap[DOCTOR_POICAT_ID] = R.drawable.s_health_small
    iconMap[PHARMACY_POICAT_ID] = R.drawable.s_health_small
    iconMap[FUEL_POICAT_ID] = R.drawable.ic_fuel
    iconMap[ATTRACTION_POICAT_ID] = R.drawable.s_tourist_small
    iconMap[PEAKS_POICAT_ID] = R.drawable.s_peak_small
    iconMap[CITIES_POICAT_ID] = R.drawable.circle_red_20px
    iconMap[TOWN_POICAT_ID] = R.drawable.circle_red_24px
    iconMap[CONVENIENCE_POICAT_ID] = R.drawable.ic_supermarket
    iconMap[MUSEUM_POICAT_ID] = R.drawable.s_museum_small
    iconMap[INFORMATION_POICAT_ID] = R.drawable.s_information_small
    iconMap[VIEWPOINT_POICAT_ID] = R.drawable.s_viewpoint_small
    iconMap[TOWER_POICAT_ID] = R.drawable.s_tower_small
    iconMap[159] = R.drawable.s_leisure_small
    iconMap[160] = R.drawable.s_leisure_small
    iconMap[161] = R.drawable.s_leisure_small
    iconMap[162] = R.drawable.s_leisure_small
    iconMap[163] = R.drawable.s_leisure_small
    iconMap[164] = R.drawable.s_leisure_small
    iconMap[165] = R.drawable.s_leisure_small
    iconMap[166] = R.drawable.s_leisure_small
    iconMap[167] = R.drawable.s_leisure_small
    iconMap[168] = R.drawable.s_leisure_small
    iconMap[169] = R.drawable.s_leisure_small
    iconMap[170] = R.drawable.s_leisure_small
    iconMap[171] = R.drawable.s_leisure_small
    iconMap[172] = R.drawable.s_leisure_small
    iconMap[173] = R.drawable.s_leisure_small
    iconMap[174] = R.drawable.s_leisure_small
    iconMap[175] = R.drawable.s_leisure_small
    iconMap[176] = R.drawable.s_leisure_small
    iconMap[LEISURE_POICAT_ID] = R.drawable.s_leisure_small
    iconMap[PUBLIC_TRANSPORT_POICAT_ID] = R.drawable.s_public_transport_small
    iconMap[PARKING_POICAT_ID] = R.drawable.s_parking_place_small
    iconMap[AIRPORT_POICAT_ID] = R.drawable.baseline_airplanemode_active_24
    iconMap[GRAVEYARD_POICAT_ID] = R.drawable.s_grave_small
    iconMap[ROUTE_POI_CATEGORY] = R.drawable.baseline_route_24
    iconMap[STREET_POI_CATEGORY] = R.drawable.s_street_small
    iconMap[STREET_POI_CATEGORY] = R.drawable.s_street_small
    iconMap[TURN_POI_CATEGORY] = R.drawable.ic_turn_24
    iconMap[CURRENT_TURN_POI_CATEGORY] = R.drawable.circle_red_16px
    iconMap[GRADIENT_POI_CATEGORY] = R.drawable.ic_gradient_24px
    return iconMap
}
const val RESTAURANT_POICAT_ID = 0
const val CAFE_POICAT_ID = 7
const val HOTEL_POICAT_ID = 372
const val CHURCH_POICAT_ID = 58
const val VILLAGE_POICAT_ID = 214
const val SUPERMARKET_POICAT_ID = 296
const val CONVENIENCE_POICAT_ID = 243
const val BAKERY_POICAT_ID = 225
const val HOSPITAL_POICAT_ID = 35
const val DOCTOR_POICAT_ID = 38
const val PHARMACY_POICAT_ID = 34
const val FUEL_POICAT_ID = 24
const val PUBLIC_TRANSPORT_POICAT_ID = 221
const val PARKING_POICAT_ID = 25
const val AIRPORT_POICAT_ID = 71
const val ATTRACTION_POICAT_ID = 364
const val MUSEUM_POICAT_ID = 375
const val GRAVEYARD_POICAT_ID = 55
const val INFORMATION_POICAT_ID = 373
const val VIEWPOINT_POICAT_ID = 378
const val TOWER_POICAT_ID = 186
const val PEAKS_POICAT_ID = 202
const val LEISURE_POICAT_ID = 177
const val CITIES_POICAT_ID = 212
const val TOWN_POICAT_ID = 213
const val ROUTE_POI_CATEGORY = 400
const val STREET_POI_CATEGORY = 401
const val TURN_POI_CATEGORY = 402
const val CURRENT_TURN_POI_CATEGORY = 403
const val GRADIENT_POI_CATEGORY = 404
const val UNKNOWN = "unknown"

data class FeatureItem(
    var name: String?,
    var dist: Double,
    var lat: Double,
    var lon: Double,
    var heading: Double,
    var poicat: Int?,
    var poicatText: String?,
    var region: String?,
    var description: String?,
    var enabled: Boolean,
    var routeDistance: String?,
    var color: Int?
) {
    val logtag = "FeatureItem"
    val poiIcons = createPoiIconsMap()
    var drawableId = poiIcons[poicat]
    override fun toString(): String {
        //Timber.i( "$poicat drawableId $drawableId")
        val s = String.format(Locale.ENGLISH, "%s %.0fm %.3f° %.3f° %s",
            name, dist, lat, lon, poicatText)
        return s
    }
}
private fun getFeatureItem(
    context: Context,
    map: MapLibreMap,
    feature: Feature?,
    center: com.google.android.gms.maps.model.LatLng?
): FeatureItem? {
    if (feature != null && center != null) {
        val pos = com.google.android.gms.maps.model.LatLng(
            map.cameraPosition.target!!.latitude,
            map.cameraPosition.target!!.longitude
        )
        val dist = SphericalUtil.computeDistanceBetween(center, pos)
        val heading = SphericalUtil.computeHeading(pos, center)

        var poiCategory = -1
        val colorText = feature.getStringProperty("color")
        var color: Int? = null
        if (colorText != null) {
            try {
                color = colorText.toColorInt()
            } catch(e: IllegalArgumentException) {
                Timber.i( "invalid color: $colorText $e")
            }
        }
        val region = feature.getStringProperty(REGION)
        val description = feature.getStringProperty(DESCRIPTION)
        val name = feature.getStringProperty(NAME)
        val leisure = feature.getStringProperty(LEISURE)
        if (leisure != null) {
            //Timber.i( "leisure: $leisure")
            poiCategory = LEISURE_POICAT_ID
        }
        val landuse = feature.getStringProperty(LANDUSE)
        if (landuse != null) {
            //Timber.i( "landuse: $landuse")
            when (landuse) {
                CEMETERY -> poiCategory =
                    GRAVEYARD_POICAT_ID
            }
        }
        val manmade = feature.getStringProperty(MAN_MADE)
        if (manmade != null) {
            //Timber.i( "manmade: $manmade")
            when (manmade) {
                TOWER -> poiCategory =
                    TOWER_POICAT_ID
            }
        }
        val aeroway = feature.getStringProperty(AEROWAY)
        if (aeroway != null) {
            //Timber.i( "aeroway: $aeroway")
            when (aeroway) {
                AERODROME -> poiCategory =
                    AIRPORT_POICAT_ID
            }
        }
        val amenity = feature.getStringProperty(AMENITY)
        if (amenity != null) {
            //Timber.i( "amenity: $amenity $name")
            when (amenity) {
                RESTAURANT -> poiCategory =
                    RESTAURANT_POICAT_ID

                PHARMACY -> poiCategory =
                    PHARMACY_POICAT_ID

                HOSPITAL -> poiCategory =
                    HOSPITAL_POICAT_ID

                PARKING -> poiCategory =
                    PARKING_POICAT_ID

                FUEL -> poiCategory = FUEL_POICAT_ID
            }
        }
        val shop = feature.getStringProperty(SHOP)
        if (shop != null) {
            //Timber.i( "shop: $shop $name")
            when (shop) {
                SUPERMARKET -> poiCategory =
                    SUPERMARKET_POICAT_ID

                WAREHOUSE -> poiCategory =
                    SUPERMARKET_POICAT_ID

                CONVENIENCE -> poiCategory =
                    CONVENIENCE_POICAT_ID
            }
        }

        val natural = feature.getStringProperty(NATURAL)
        if (natural != null) {
            //Timber.i( "natural: $natural $name")
            when (natural) {
                PEAK -> poiCategory = PEAKS_POICAT_ID
                WATER -> poiCategory =
                    ATTRACTION_POICAT_ID // attraction
                BEACH -> poiCategory =
                    ATTRACTION_POICAT_ID // attraction
            }
        }
        val building = feature.getStringProperty(BUILDING)
        if (building != null) {
            //Timber.i( "building: $building $name")
            when (building) {
                HOTEL -> poiCategory =
                    HOTEL_POICAT_ID
                //SCHOOL
                CHURCH -> poiCategory =
                    CHURCH_POICAT_ID

                CHAPEL -> poiCategory =
                    CHURCH_POICAT_ID

                CATHEDRAL -> poiCategory =
                    CHURCH_POICAT_ID
                //INDUSTRIAL
                SUPERMARKET -> poiCategory =
                    SUPERMARKET_POICAT_ID

                WAREHOUSE -> poiCategory =
                    SUPERMARKET_POICAT_ID

                CONVENIENCE -> poiCategory =
                    CONVENIENCE_POICAT_ID
            }
        }

        val place = feature.getStringProperty(PLACE)
        if (place != null) {
            //Timber.i( "place: $place $name")
            when (place) {
                VILLAGE -> poiCategory =
                    VILLAGE_POICAT_ID

                TOWN -> poiCategory = TOWN_POICAT_ID
                CITY -> poiCategory =
                    CITIES_POICAT_ID
            }
        }
        val highway = feature.getStringProperty(HIGHWAY)
        if (highway != null) {
            //Timber.i( "highway: $highway $name")
            poiCategory = STREET_POI_CATEGORY
        }

        var poiCategoryTextDefaultValue: String
        val poiCategoryTextSb: java.lang.StringBuilder = StringBuilder()
        run properties@ {
            var keys = 0
            for (propKey in feature.properties()?.keySet()!!) {
                //val isEle = propKey == "ele"
                val propValue = feature.properties()!!.get(propKey).toString().replace("\"", "")
                if (propValue.isNotEmpty()) {
                    val codepoint = Character.codePointAt(propValue, 0)
                    val isNotChinese =
                        Character.UnicodeScript.of(codepoint) != Character.UnicodeScript.HAN
                    val isNotNumeric = !propValue.matches("-?\\d+(\\.\\d+)?".toRegex())
                    if (propValue.isNotEmpty() && !propKey.equals(NAME)) {
                        if (propValue != "yes" && propValue != "no" && isNotChinese
                            && isNotNumeric && !propValue.startsWith(Const.HASHTAG)
                        ) {
                            poiCategoryTextSb.append(propValue).append(" ")
                            keys++
                        }
                    }
                    if (keys >= 2)
                        return@properties
                }
            }
        }
        poiCategoryTextDefaultValue = poiCategoryTextSb.toString()
        if (poiCategoryTextDefaultValue.isEmpty())
            poiCategoryTextDefaultValue = UNKNOWN
        val poiCatProperties = createPoicatProperties(context)
        val poiCategoryText =
            poiCatProperties.getProperty(poiCategory.toString(), poiCategoryTextDefaultValue)

//        val poiIcons = createPoiIconsMap()
//        val drawableId = poiIcons.get(poiCategory)

        val featureDescription = feature.getStringProperty(DESCRIPTION)
        val featureName = feature.getStringProperty(NAME)
        val featureCenter = GeoJsonUtils.getFeatureCenter(feature)
        val featureItem = FeatureItem(
            featureName, dist, featureCenter.latitude, featureCenter.longitude,
            heading, poiCategory, poiCategoryText, region, description, false,
            feature.getStringProperty("distance"), color)
        //Timber.i( "cat: $poiCategory drawableId: ${featureItem.drawableId}")
        if (region.isNotNull()) {
            featureItem.drawableId = R.drawable.baseline_route_24
            featureItem.poicatText = context.getString(R.string.route)
        } else if (featureName.isNotNull() && feature.getStringProperty(NAME).startsWith("geojsonTile_")) {
            val mapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
            val mapEntity = mapRepository.getGeojsonMapSimpleByName(featureName)
            featureItem.enabled = mapEntity?.enabled == true
            featureItem.poicatText = "GeoJsonTile"
            featureItem.drawableId = R.drawable.outline_file_json_24
        } else if (featureDescription.isNotNull() && feature.getStringProperty(DESCRIPTION) == "geojsonTile") {
            val mapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
            val mapEntity = mapRepository.getGeojsonMapSimpleByName("geojsonTile_$featureName")
            featureItem.enabled = mapEntity?.enabled == true
            featureItem.poicatText = "GeoJsonTile"
            featureItem.drawableId = R.drawable.outline_file_json_24
        }
        return featureItem
    }
    return null
}

fun createPoicatProperties(context: Context): Properties {
    val poicatProperties = Properties()
    poicatProperties.setProperty(
        java.lang.String.valueOf(RESTAURANT_POICAT_ID),
        context.getString(R.string.restaurant)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(CAFE_POICAT_ID),
        context.getString(R.string.cafe)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(HOTEL_POICAT_ID),
        context.getString(R.string.hotel)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(CHURCH_POICAT_ID),
        context.getString(R.string.church)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(VILLAGE_POICAT_ID),
        context.getString(R.string.village)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(SUPERMARKET_POICAT_ID),
        context.getString(R.string.supermarket)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(BAKERY_POICAT_ID),
        context.getString(R.string.bakery)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(HOSPITAL_POICAT_ID),
        context.getString(R.string.hospital)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(DOCTOR_POICAT_ID),
        context.getString(R.string.doctor)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(PHARMACY_POICAT_ID),
        context.getString(R.string.pharmacy)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(FUEL_POICAT_ID),
        context.getString(R.string.fuel)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(ATTRACTION_POICAT_ID),
        context.getString(R.string.attraction)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(PEAKS_POICAT_ID),
        context.getString(R.string.peaks)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(CITIES_POICAT_ID),
        context.getString(R.string.city)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(TOWN_POICAT_ID),
        context.getString(R.string.locality)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(CONVENIENCE_POICAT_ID),
        context.getString(R.string.shop)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(MUSEUM_POICAT_ID),
        context.getString(R.string.museum)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(INFORMATION_POICAT_ID),
        context.getString(R.string.information)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(VIEWPOINT_POICAT_ID),
        context.getString(R.string.viewpoint)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(TOWER_POICAT_ID),
        context.getString(R.string.tower)
    )
    poicatProperties.setProperty(159.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(160.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(161.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(162.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(163.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(187.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(164.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(165.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(166.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(167.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(168.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(169.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(170.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(171.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(172.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(173.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(174.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(175.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(176.toString(), context.getString(R.string.leisure))
    poicatProperties.setProperty(
        java.lang.String.valueOf(LEISURE_POICAT_ID),
        context.getString(R.string.leisure)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(PUBLIC_TRANSPORT_POICAT_ID),
        context.getString(R.string.public_transport)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(PARKING_POICAT_ID),
        context.getString(R.string.parking_place)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(AIRPORT_POICAT_ID),
        context.getString(R.string.airport)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(GRAVEYARD_POICAT_ID),
        context.getString(R.string.grave_yard)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(ROUTE_POI_CATEGORY),
        context.getString(R.string.route)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(STREET_POI_CATEGORY),
        context.getString(R.string.street)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(TURN_POI_CATEGORY),
        context.getString(R.string.turn)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(CURRENT_TURN_POI_CATEGORY),
        context.getString(R.string.current_turn)
    )
    poicatProperties.setProperty(
        java.lang.String.valueOf(GRADIENT_POI_CATEGORY),
        context.getString(R.string.gradient)
    )
    return poicatProperties
}
class DistComparatorCenter(val center: com.google.android.gms.maps.model.LatLng?) :
    Comparator<FeatureItem> {
    override fun compare(o1: FeatureItem?, o2: FeatureItem?): Int {
        val featureCenter1 =
            o1?.let { com.google.android.gms.maps.model.LatLng(it.lat, it.lon) }
        val featureCenter2 =
            o2?.let { com.google.android.gms.maps.model.LatLng(it.lat, it.lon) }
        val dist1 = center?.let { featureCenter1?.let { it1 -> SphericalUtil.computeDistanceBetween(it, it1) } }
        val dist2 = center?.let { featureCenter2?.let { it1 -> SphericalUtil.computeDistanceBetween(it, it1) } }
        return dist2?.let { dist1?.compareTo(it) } ?: 0
    }
}
class NameComparator() :
    Comparator<FeatureItem> {
    override fun compare(o1: FeatureItem?, o2: FeatureItem?): Int {
        return o1?.name!!.compareTo(o2?.name.toString())
    }
}
class CategoryComparator() :
    Comparator<FeatureItem> {
    override fun compare(o1: FeatureItem?, o2: FeatureItem?): Int {
        return o1?.poicat!!.toInt().compareTo(o2?.poicat!!.toInt())
    }
}

fun addPoiDao(
    context: Context,
    name: String,
    latLng: com.google.android.gms.maps.model.LatLng?,
    h: Double,
    category: String,
    finished: (PoiEntity?) -> Unit
) {
    Timber.i( "$name $category latLng: $latLng")
    val hgtReader = HgtReader(context, null)
    val poiAltitude = if (h >= 0) h else latLng?.let { hgtReader.getElevationFromHgt(it) }
    val poiEntity = latLng?.let {
        if (poiAltitude != null) {
            PoiEntity(
                name,
                it.latitude,
                it.longitude,
                poiAltitude,
                category
            )
        } else
            PoiEntity(
                name,
                it.latitude,
                it.longitude,
                0.0,
                category
            )
    }

    val poiRepository =
        PoiRepository.getInstance(context, Executors.newSingleThreadExecutor())
    poiEntity?.let { poiRepository.addPoi(it) {
        finished(it)
    } }
    //val cat = PoiEntity.categoryAttributes(poiEntity.category)
}
