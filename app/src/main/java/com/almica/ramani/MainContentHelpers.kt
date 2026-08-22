package com.almica.ramani

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.MutableState
import androidx.core.content.edit
import androidx.exifinterface.media.ExifInterface
import com.almica.ramani.compass.CompassViewModel
import com.almica.ramani.routes.RouteEntity
import com.almica.ramani.utils.HgtReader
import com.almica.ramani.utils.RouteSmoothingUtil.simplifyToTargetCount
import com.almica.ramani.utils.getCenter
import com.almica.ramani.utils.getDistanceFromLllh
import com.almica.ramani.Helpers.Companion.decompressString
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.utils.kmlString2Lllh
import com.almica.ramani.utils.lllhToKmlString
import com.almica.ramani.utils.reduceWithTolerance
import com.almica.ramani_lib.CameraMotionType
import com.almica.ramani_lib.CameraPosition
import me.ibrahimsn.library.LiveSharedPreferences
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.sources.GeoJsonSource
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.GZIPInputStream

fun getGeojsonFromSnapshot(
    routeFile: File,
    map: MapLibreMap?,
    liveSharedPreferences: LiveSharedPreferences,
    context: Context
): Pair<String?, String?> {
    var routesGeoJsonStringResult: String? = null
    var popupSnackMsgResult: String? = null
    val exifInterface: ExifInterface
    try {
        exifInterface = ExifInterface(routeFile.path)
        routesGeoJsonStringResult =
            exifInterface.getAttribute(ExifInterface.TAG_USER_COMMENT)
                ?.let { compressedData -> Helpers.decompressString(compressedData) }
        Timber.i("geojsonString: $routesGeoJsonStringResult")
        val routesGeojsonSource =
            map?.style?.getSource("routes${Const.GEOJSON_EXT}") as? GeoJsonSource
        routesGeoJsonStringResult?.let { geojson ->
            routesGeojsonSource?.setGeoJson(geojson)
            val routesHitGeojsonSource =
                map?.style?.getSource("routes${Const.GEOJSON_EXT}${FeatureProperties.HITLAYER_TAG}") as? GeoJsonSource
            routesHitGeojsonSource?.setGeoJson(geojson)
            liveSharedPreferences.preferences.edit {
                putString(
                    context.getString(R.string.pref_routes_geojson_visibility),
                    Property.VISIBLE
                )
            }
            popupSnackMsgResult = context.getString(R.string.geojson_loaded_, routeFile.nameWithoutExtension)
        }
    } catch (e: IOException) {
        Timber.e(e)
        e.printStackTrace()
        popupSnackMsgResult = context.getString(R.string.route_load_failed_, routeFile.name)
    }
    return Pair(popupSnackMsgResult, routesGeoJsonStringResult)
}

fun displayRouteOnMap(
    routeEntity: RouteEntity?,
    context: Context,
    cameraMode: MutableState<Int>,
    cameraPosition: MutableState<CameraPosition>,
    onLoaded: (RouteEntity, PolygonState) -> Unit
) {
    if (routeEntity == null) {
        Timber.e("routeEntity = null")
        return
    }

    val routeName = routeEntity.name
    val lllh = routeEntity.kmlString.kmlString2Lllh()

    if (lllh.isNotEmpty()) {
        val lllhReduced = lllh.reduceWithTolerance(200.0)
        if (lllhReduced.isNotEmpty()) {
            val dist = lllhReduced.getDistanceFromLllh()
            val entity = routeEntity.copy(
                latitudeStart = lllh[0].latitude,
                longitudeStart = lllh[0].longitude
            )
            val state = PolygonState(lllhReduced, routeName, dist, null).apply {
                polygonData = PolygonData(lllhReduced, routeName, dist, false, null)
                polygonData?.createPolygonMarkers(context, 0.0)
            }

            onLoaded(entity, state)

            Timber.i("lllh: ${lllhReduced.size}")
            CompassViewModel.setRouteThumbnail(null)

            val cameraModeClone = cameraMode.value
            cameraMode.value = CameraMode.NONE
            cameraPosition.value = CameraPosition(cameraPosition.value).apply {
                this.target = LatLng(
                    lllh[0].latitude,
                    lllh[0].longitude
                )
                this.bearing = 0.0
                this.animationDurationMs = 300
            }
            cameraMode.value = cameraModeClone
        }
    } else {
        Timber.e("no coordinates in ${routeEntity.name}")
    }
}

fun reverseRoute(
    context: Context,
    currentState: PolygonState,
    currentEntity: RouteEntity?,
    onSuccess: (PolygonState, RouteEntity, MainSnackbarData) -> Unit
) {
    val lllhReversed = currentState.lllh.reversed()
    val routeName = currentState.name
    val routeDist = currentState.distance

    val startLat = lllhReversed[0].latitude
    val startLon = lllhReversed[0].longitude
    val tag = currentEntity?.region ?: Const.GH_TAG
    val id = currentEntity?.id ?: UUID.randomUUID()
    val center = lllhReversed.getCenter()

    val newState = PolygonState(lllhReversed, routeName, routeDist).apply {
        polygonData = PolygonData(lllhReversed, routeName, routeDist, false, null)
        polygonData?.createPolygonMarkers(context, 0.0)
    }

    val newEntity = RouteEntity(
        id, routeName, tag, startLat, startLon,
        latitudeCenter = center.latitude,
        longitudeCenter = center.longitude,
        latitudeStop = lllhReversed[lllhReversed.lastIndex].latitude,
        longitudeStop = lllhReversed[lllhReversed.lastIndex].longitude,
        kmlString = lllhReversed.lllhToKmlString(routeName)
    )

    onSuccess(
        newState,
        newEntity,
        MainSnackbarData(
            context.getString(R.string.polygon_reversed),
            null,
            null,
            null
        )
    )
}

fun refreshRouteElevation(
    context: Context,
    pos: LatLng,
    currentState: PolygonState,
    currentEntity: RouteEntity?,
    onSuccess: (PolygonState, RouteEntity, MainSnackbarData) -> Unit,
    onFailure: (MainSnackbarData) -> Unit
) {
    val tileName = Helpers.getTileName(pos.latitude, pos.longitude).uppercase()
    val demFolder = File(context.filesDir, Const.HGT_FOLDER_NAME)
    val hgtFile = File(demFolder, tileName + Const.HGT_EXT)

    if (hgtFile.exists()) {
        val hgtReader = HgtReader(context, hgtFile)
        val lllhRefreshed = hgtReader.refreshRouteElevationFromSrtm(currentState.lllh).lllh

        if (lllhRefreshed != null) {
            val routeName = currentState.name
            val routeDist = currentState.distance
            val startLat = lllhRefreshed[0].latitude
            val startLon = lllhRefreshed[0].longitude
            val tag = currentEntity?.region ?: Const.GH_TAG
            val id = currentEntity?.id ?: UUID.randomUUID()
            val center = lllhRefreshed.getCenter()

            val newState = PolygonState(lllhRefreshed, routeName, routeDist).apply {
                polygonData = PolygonData(lllhRefreshed, routeName, routeDist, false, null)
                polygonData?.createPolygonMarkers(context, 0.0)
            }

            val newEntity = RouteEntity(
                id, routeName, tag, startLat, startLon,
                latitudeCenter = center.latitude,
                longitudeCenter = center.longitude,
                latitudeStop = lllhRefreshed[lllhRefreshed.lastIndex].latitude,
                longitudeStop = lllhRefreshed[lllhRefreshed.lastIndex].longitude,
                kmlString = lllhRefreshed.lllhToKmlString(routeName)
            )

            onSuccess(
                newState,
                newEntity,
                MainSnackbarData(
                    context.getString(R.string.srtm_refresh_done),
                    null,
                    null,
                    null
                )
            )
        } else {
            onFailure(
                MainSnackbarData(
                    context.getString(R.string.srtm_refresh_failed),
                    null,
                    null,
                    null
                )
            )
        }
    } else {
        onFailure(
            MainSnackbarData(
                context.getString(
                    R.string.file_not_found,
                    hgtFile.path
                ), null, null, null
            )
        )
    }
}

fun loadRouteFromFile(
    routeFile: File,
    context: Context,
    cameraMode: MutableState<Int>,
    cameraPosition: MutableState<CameraPosition>,
    onLoaded: (RouteEntity, PolygonState) -> Unit,
    loadFailed: () -> Unit
) {
    Timber.i("routeFile: $routeFile")
    val lllhReduced = com.almica.ramani.Helpers.getLllhFromFile(routeFile)
        ?.simplifyToTargetCount(200) as? ArrayList<LatLngH>
    Timber.i("${routeFile.name} coordinates: ${lllhReduced?.size}")
    if (lllhReduced.isNullOrEmpty()) {
        loadFailed()
        return
    }
    lllhReduced.let { lllh ->
        if (lllh.isNotEmpty()) {
            val routeCenter = lllh.getCenter()
            val dist = lllh.getDistanceFromLllh()
            val name = routeFile.nameWithoutExtension

            val entity = RouteEntity(
                UUID.randomUUID(),
                name,
                "",
                lllh[0].latitude,
                lllh[0].longitude,
                routeCenter.latitude,
                routeCenter.longitude,
                lllh[lllh.lastIndex].latitude,
                lllh[lllh.lastIndex].longitude,
                dist,
                lllh.lllhToKmlString(name),
                null
            )

            val state = PolygonState(lllh, name, dist, null).apply {
                polygonData = PolygonData(lllh, name, dist, false, null)
                polygonData?.createPolygonMarkers(context, 0.0)
            }

            onLoaded(entity, state)

            Timber.i("lllh: ${lllh.size}")
            CompassViewModel.setRouteThumbnail(null)

            val cameraModeClone = cameraMode.value
            cameraMode.value = CameraMode.NONE
            cameraPosition.value = CameraPosition(cameraPosition.value).apply {
                this.target = LatLng(
                    lllh[0].latitude,
                    lllh[0].longitude
                )
                this.bearing = 0.0
                this.animationDurationMs = 300
            }
            cameraMode.value = cameraModeClone
        }
    }
}

fun loadRouteFromLllh(
    lllh: List<LatLngH>,
    name: String,
    context: Context,
    cameraMode: MutableState<Int>,
    cameraPosition: MutableState<CameraPosition>,
    onLoaded: (RouteEntity, PolygonState) -> Unit
) {
    val lllhReduced = lllh.simplifyToTargetCount(200)

    lllhReduced.let { lllh ->
        if (lllh.isNotEmpty()) {
            val routeCenter = lllh.getCenter()
            val dist = lllh.getDistanceFromLllh()

            val entity = RouteEntity(
                UUID.randomUUID(),
                name,
                "",
                lllh[0].latitude,
                lllh[0].longitude,
                routeCenter.latitude,
                routeCenter.longitude,
                lllh[lllh.lastIndex].latitude,
                lllh[lllh.lastIndex].longitude,
                dist,
                lllh.lllhToKmlString(name),
                null
            )

            val state = PolygonState(lllh, name, dist, null).apply {
                polygonData = PolygonData(lllh, name, dist, false, null)
                polygonData?.createPolygonMarkers(context, 0.0)
            }

            onLoaded(entity, state)

            Timber.i("lllh: ${lllh.size}")
            CompassViewModel.setRouteThumbnail(null)

            val cameraModeClone = cameraMode.value
            cameraMode.value = CameraMode.NONE
            cameraPosition.value = CameraPosition(cameraPosition.value).apply {
                this.target = LatLng(
                    lllh[0].latitude,
                    lllh[0].longitude
                )
                this.bearing = 0.0
                this.animationDurationMs = 300
            }
            cameraMode.value = cameraModeClone
        }
    }
}

fun MutableState<CameraPosition>.setCameraTarget(latlng: LatLng?) {
    if (latlng == null) return
    this.value =
        CameraPosition(this.value).apply {
            this.target = LatLng(
                latlng.latitude,
                latlng.longitude
            )
            //this.zoom = 13.0
            this.bearing = 0.0
            this.motionType = CameraMotionType.INSTANT
            this.animationDurationMs = 0
        }
}
