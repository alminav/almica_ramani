package com.almica.ramani.routes

import android.graphics.Color
import androidx.collection.forEach
import androidx.compose.runtime.Composer
import com.almica.ramani.Const
import com.almica.ramani.Const.Companion.PROPERTY_CATEGORY
import com.almica.ramani.Const.Companion.PROPERTY_COLOR_RGBA
import com.almica.ramani.Const.Companion.PROPERTY_DISTANCE_M
import com.almica.ramani.Const.Companion.PROPERTY_KMLSTRING
import com.almica.ramani.Const.Companion.PROPERTY_LAT
import com.almica.ramani.Const.Companion.PROPERTY_LON
import com.almica.ramani.Const.Companion.PROPERTY_NAME
import com.almica.ramani.Const.Companion.PROPERTY_REGION
import com.almica.ramani.Const.Companion.PROPERTY_TYPE
import com.almica.ramani.Const.Companion.PROPERTY_UUID
import com.almica.ramani.Const.Companion.ROUTE_COLORS
import com.almica.ramani.LatLngH
import com.almica.ramani.utils.getDistanceFromLllh
import com.almica.ramani.utils.kmlString2Lllh
import com.almica.ramani.utils.lllhToKmlString
import com.almica.ramani_lib.MapApplier
import com.google.gson.JsonObject
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.plugins.annotation.LineManager
import org.maplibre.android.plugins.annotation.LineOptions
import org.maplibre.android.utils.ColorUtils
import timber.log.Timber

class ComposeHelpers {
    companion object {
        val logtag: String = this::class.java.name
        fun showDbRouteEntityInMap(
            currentComposer: Composer,
            map: MapLibreMap?,
            routeEntity: RouteEntity?,
            mapView: MapView? = null
        ): String {
            val mapApplier = currentComposer.applier as? MapApplier
            if (mapApplier == null && mapView == null) {
                Timber.e("showDbRouteEntityInMap: currentComposer.applier is not MapApplier and mapView is null")
                return ""
            }
            val uuidString = routeEntity?.id.toString() //route.getStringExtra(Const.RESULT_UUID)
            val name = routeEntity?.name //route.getStringExtra(Const.RESULT_NAME)
            val region = routeEntity?.region //getStringExtra(Const.RESULT_REGION)
            val kmlString = routeEntity?.kmlString //.getStringExtra(Const.RESULT_KMLSTRING)
            Timber.i("$name $region")
            val lllh = kmlString?.kmlString2Lllh()
            moveCamera(
                map,
                routeEntity?.let {
                    com.google.android.gms.maps.model.LatLng(
                        it.latitudeStart,
                        it.longitudeStart
                    )
                },
                13.0
            )
            val routeResult: JsonObject = addRouteLine(
                currentComposer,
                lllh,
                name.toString(),
                region,
                Const.ROUTE_TYPE_DAO,
                uuidString,
                null,
                false,
                map,
                mapView
            )
            return routeResult.toString()
        }

        fun moveCamera(
            mapboxMap: MapLibreMap?,
            latLng: com.google.android.gms.maps.model.LatLng?,
            zoom: Double
        ) {
            val position = CameraPosition.Builder()
                .target(latLng?.let { LatLng(it.latitude, it.longitude) }
                ) // Sets the new camera position
                .zoom(zoom) // Sets the zoom
                .tilt(0.0) // Set the camera tilt
                .build() // Creates a CameraPosition from the builder
            Timber.i("map.animateCamera position: %s", position.target.toString())
            mapboxMap?.animateCamera(CameraUpdateFactory.newCameraPosition(position))
        }

        fun addRouteLine(
            currentComposer: Composer,
            lllh: List<LatLngH>?,
            name: String,
            region: String?,
            type: Int,
            uuidString: String?,
            center: LatLng?,
            withPattern: Boolean,
            map: MapLibreMap? = null,
            mapView: MapView? = null
        ): JsonObject {
            Timber.i("addRouteLine $name type:$type")
            val mapApplier = currentComposer.applier as? MapApplier
            val lineManager = if (mapApplier != null) {
                mapApplier.getOrCreateLineManagerForZIndex(0)
            } else if (map != null && mapView != null && map.style != null) {
                LineManager(mapView, map, map.style!!)
            } else {
                Timber.e("addRouteLine: Could not get LineManager")
                return JsonObject()
            }

            val joData = JsonObject()
            joData.addProperty(PROPERTY_NAME, name)
            joData.addProperty(PROPERTY_TYPE, type)
            if (region != null)
                joData.addProperty(PROPERTY_REGION, region)
            if (uuidString != null)
                joData.addProperty(PROPERTY_UUID, uuidString)
            val kmlString = lllh?.lllhToKmlString(name)
            val distM = lllh?.getDistanceFromLllh()
            joData.addProperty(PROPERTY_DISTANCE_M, distM)
            Timber.i(kmlString?.let { "kmlString ${it.length}" })
            joData.addProperty(
                PROPERTY_KMLSTRING,
                kmlString
            ) // line has only LatLng without altitude
            val latLngs: MutableList<LatLng> = ArrayList()
            lllh?.forEach { llh ->
                latLngs.add(LatLng(llh.latitude, llh.longitude, llh.altitude))
            }
            if (center == null) {
                joData.addProperty(PROPERTY_LAT, lllh?.get(0)?.latitude)
                joData.addProperty(PROPERTY_LON, lllh?.get(0)?.longitude)
            } else {
                joData.addProperty(PROPERTY_LAT, center.latitude)
                joData.addProperty(PROPERTY_LON, center.longitude)
            }
            val i = (Math.random() * (ROUTE_COLORS.size - 1)).toInt()
            var routeColor = ColorUtils.colorToRgbaString(ROUTE_COLORS[i])
            var linePattern: String?
            var opacity = 0.7f
            var lineWidth = 4.0f
            when (type) {
                Const.ROUTE_TYPE_GH -> {
                    opacity = 1.0f
                    lineWidth = 8.0f
                    joData.addProperty(PROPERTY_COLOR_RGBA, ROUTE_COLORS[i])
                    linePattern = Const.LINEPATTERN_ARROW + routeColor
                }
                Const.ROUTE_TYPE_GEOJSON_BORDER, Const.ROUTE_TYPE_OFFREGION_BORDER -> {
                    Timber.i("type:$type")
                    routeColor = ColorUtils.colorToRgbaString(Color.DKGRAY)
                    linePattern = Const.LINEPATTERN_CIRCLE + routeColor
                    opacity = 0.5f
                    lineWidth = 6.0f
                    joData.addProperty(PROPERTY_COLOR_RGBA, Color.DKGRAY)
                } else -> {
                    joData.addProperty(PROPERTY_COLOR_RGBA, ROUTE_COLORS[i])
                    linePattern = if (withPattern) Const.LINEPATTERN_DASH + routeColor
                    else Const.LINEPATTERN_DEFAULT + routeColor
                }
            }
            val lineOptions = LineOptions()
                //.withLinePattern(if (withPattern) Const.LINEPATTERN_DASH else Const.LINEPATTERN)
                .withLinePattern(linePattern)
                .withLatLngs(latLngs)
                .withData(joData)
                .withLineColor(routeColor)
                .withLineOpacity(opacity)
                .withLineWidth(lineWidth)
            //val lineOptionsArray = listOf(lineOptions, lineOptionsPattern)
            //mLineManager?.create(if (withPattern) lineOptionsPattern else lineOptions)
            lineManager.create(lineOptions)
            return joData
        }

        internal fun removeRouteLine(currentComposer: Composer, routeName: String, map: MapLibreMap? = null, mapView: MapView? = null) {
            val mapApplier = currentComposer.applier as? MapApplier
            val lineManager = if (mapApplier != null) {
                mapApplier.getOrCreateLineManagerForZIndex(0)
            } else if (map != null && mapView != null && map.style != null) {
                LineManager(mapView, map, map.style!!)
            } else {
                Timber.e("removeRouteLine: Could not get LineManager")
                return
            }
            Timber.i("routeName: $routeName")
            val keys = ArrayList<Long>()
            lineManager.annotations?.forEach { key, value ->
                Timber.i("$key + ${value.data}")
                val jo: JsonObject = value.data as JsonObject
                val name = jo.get(PROPERTY_NAME).asString
                if (name.contains(routeName))
                    keys.add(key)
            }
            Timber.i("removeRouteLine keys:${keys.size}")
            for (key in keys)
                lineManager.annotations?.remove(key)
            lineManager.updateSource()
        }

        fun removeSymbolsByCategory(currentComposer: Composer?, categoryFilter: String) {
            val mapApplier = currentComposer?.applier as? MapApplier
            if (mapApplier != null) {
                val symbolManager = mapApplier.getOrCreateSymbolManagerForZIndex(0)
                val keys = ArrayList<Long>()
                symbolManager.annotations?.forEach { key, value ->
                    val jo: JsonObject = value.data as JsonObject
                    val category = jo.get(PROPERTY_CATEGORY).asString
                    //if (category != TAG_START_NAVIGATION && category != TAG_STOP_NAVIGATION)
                    if (category.contains(categoryFilter, true))
                        keys.add(key)
                }
//    Timber.i("remove ${keys.size} symbols")
                for (key in keys)
                    symbolManager.annotations?.remove(key)
                symbolManager.updateSource()
            } else
                Timber.i("currentComposer = null")
        }

    }
}