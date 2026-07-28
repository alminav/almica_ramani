package com.almica.ramani_lib

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableTargetMarker
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.gestures.MoveGestureDetector
import org.maplibre.android.gestures.RotateGestureDetector
import org.maplibre.android.gestures.ShoveGestureDetector
import org.maplibre.android.gestures.StandardScaleGestureDetector
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.OnCameraTrackingChangedListener
import org.maplibre.android.location.engine.LocationEngine
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineDefault
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.sources.Source
import org.maplibre.android.utils.BitmapUtils
import timber.log.Timber

//lateinit var locationCallback: LocationEngineCallback<LocationEngineResult>
private const val logtag = "MapLibre"
@Retention(AnnotationRetention.BINARY)
@ComposableTargetMarker(description = "Maplibre Composable")
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class MapLibreComposable

/**
 * A composable representing a MapLibre map.
 *
 * @param modifier The modifier applied to the map.
 * @param styleBuilder The style builder to access the tile provider. Defaults to a demo tile provider.
 * @param cameraPosition The position of the map camera.
 * @param uiSettings Settings related to the map UI.
 * @param properties Properties being applied to the map.
 * @param locationRequestProperties Properties related to the location marker. If null (which is
 *        the default), then the location will not be enabled on the map. Enabling the location
 *        requires setting this field and getting the location permission in your app.
 * @param locationEngine The location engine to use for the location marker. If null (which is
 *        the default), then the default location engine will be used.
 * @param locationStyling Styling related to the location marker (color, pulse, etc).
 * @param userLocation If set and if the location is enabled (by setting [locationRequestProperties]),
 *        it will be updated to contain the latest user location as known by the map.
 * @param sources External (user-defined) sources for the map.
 * @param layers External (user-defined) layers for the map.
 * @param images Images to be added to the map and used by external layers (pairs of <id, drawable code>).
 * @param renderMode Ways the user location can be rendered on the map.
 * @param cameraMode Set specific camera tracking modes as the device location changes.
 * @param onMapLongClick Callback that is invoked when the map is long clicked
 * @param content The content of the map.
 */
@Composable
fun MapLibre(
    modifier: Modifier,
    styleBuilder: Style.Builder = Style.Builder()
        .fromUri("https://demotiles.maplibre.org/style.json"),
    cameraPosition: CameraPosition = rememberSaveable { CameraPosition() },
    uiSettings: UiSettings = UiSettings(),
    properties: MapProperties = MapProperties(),
    locationRequestProperties: LocationRequestProperties = LocationRequestProperties(),
    locationEngine: LocationEngine? = null,
    locationStyling: LocationStyling = LocationStyling(),
    userLocation: MutableState<Location>? = null,
    sources: List<Source>? = null,
    layers: List<Layer>? = null,
    images: List<Pair<String, Int>>? = null,
    mapView: MapView = rememberMapViewWithLifecycle(),
    renderMode: MutableIntState = mutableIntStateOf(RenderMode.NORMAL), //Int = RenderMode.NORMAL,
    cameraMode: MutableIntState = mutableIntStateOf(CameraMode.NONE),
    onMapClick: (LatLng) -> Unit = {},
    onMapLongClick: (LatLng) -> Unit = {},
    onStyleLoaded: (Style) -> Unit = {},
    onMapReady: (MapLibreMap) -> Unit = {},
    content: (@Composable @MapLibreComposable () -> Unit)? = null,
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier)
        return
    }
    val context = LocalContext.current
    val currentStyleBuilder by rememberUpdatedState(styleBuilder)
    val currentCameraPosition by rememberUpdatedState(cameraPosition)
    val currentUiSettings by rememberUpdatedState(uiSettings)
    val currentMapProperties by rememberUpdatedState(properties)
    val currentLocationRequestProperties by rememberUpdatedState(locationRequestProperties)
    val currentLocationEngine by rememberUpdatedState(locationEngine)
    val currentLocationStyling by rememberUpdatedState(locationStyling)
    val currentSources by rememberUpdatedState(sources)
    val currentLayers by rememberUpdatedState(layers)
    val currentImages by rememberUpdatedState(images)
    val currentRenderMode by rememberUpdatedState(renderMode)
    val currentContent by rememberUpdatedState(content)
    val parentComposition = rememberCompositionContext()

    val currentStyle = remember { mutableStateOf<Style?>(null) }
    val currentMap = remember { mutableStateOf<MapLibreMap?>(null) }

    /**
     * ramani lib 0.10.0 05feb2026
     */
    LaunchedEffect(currentStyleBuilder) {
        currentLayers?.forEach { currentStyle.value?.removeLayer(it) }
        currentSources?.forEach { currentStyle.value?.removeSource(it) }
        currentStyle.value = mapView.awaitMap().awaitStyle(currentStyleBuilder)
        currentStyle.value?.let {
            onStyleLoaded(it)
            Timber.i("onStyleLoaded ${it.uri}")
        }
    }

    AndroidView(modifier = modifier, factory = { mapView })

    LaunchedEffect(null) {
        Timber.i("awaitMap")
        val maplibreMap = mapView.awaitMap()
        val style = maplibreMap.awaitStyle(currentStyleBuilder)
        onStyleLoaded(style)

        currentMap.value = maplibreMap
        onMapReady(maplibreMap)
        currentStyle.value = style
        //Timber.i("addImages: ${currentImages?.size}")
        maplibreMap.addImages(context, currentImages)

        mapView.addOnDidFinishLoadingStyleListener {
            Timber.i("FinishLoadingStyle")
            maplibreMap.addSources(currentSources)
            maplibreMap.addLayers(currentLayers)
        }
        maplibreMap.addSources(currentSources)
        maplibreMap.addLayers(currentLayers)

        maplibreMap.addOnMapClickListener { latLng ->
            onMapClick(latLng)
            false
        }

        maplibreMap.addOnMapLongClickListener { latLng ->
            onMapLongClick(latLng)
            false
        }

        mapView.newComposition(parentComposition, maplibreMap, currentStyle) {
            //Timber.i("cameraMode: ${cameraMode.intValue}")
            //Timber.i("renderMode: ${renderMode.intValue}")
            CompositionLocalProvider {
                MapUpdater(
                    map = checkNotNull(currentMap.value),
                    style = currentStyle,
                    cameraPosition = currentCameraPosition,
                    uiSettings = currentUiSettings,
                    properties = currentMapProperties,
                    locationRequestProperties = currentLocationRequestProperties,
                    locationEngine = currentLocationEngine,
                    locationStyling = currentLocationStyling,
                    userLocation = userLocation,
                    cameraMode = cameraMode,
                    renderMode = renderMode // currentRenderMode.value,
                )
                currentContent?.invoke()
            }
        }
    }
}

private fun MapLibreMap.applyUiSettings(uiSettings: UiSettings) {
    this.uiSettings.apply {
        setAttributionMargins(
            uiSettings.attributionsMargins.left,
            uiSettings.attributionsMargins.top,
            uiSettings.attributionsMargins.right,
            uiSettings.attributionsMargins.bottom
        )

        setCompassMargins(
            uiSettings.compassMargins.left,
            uiSettings.compassMargins.top,
            uiSettings.compassMargins.right,
            uiSettings.compassMargins.bottom
        )

        setLogoMargins(
            uiSettings.logoMargins.left,
            uiSettings.logoMargins.top,
            uiSettings.logoMargins.right,
            uiSettings.logoMargins.bottom
        )

        flingAnimationBaseTime = uiSettings.flingAnimationBaseTime
        flingThreshold = uiSettings.flingThreshold
        isAttributionEnabled = uiSettings.isAttributionEnabled
        isDeselectMarkersOnTap = uiSettings.deselectMarkersOnTap
        isDisableRotateWhenScaling = uiSettings.disableRotateWhenScaling
        isDoubleTapGesturesEnabled = uiSettings.doubleTapGesturesEnabled
        isFlingVelocityAnimationEnabled = uiSettings.flingVelocityAnimationEnabled
        isHorizontalScrollGesturesEnabled = uiSettings.horizontalScrollGesturesEnabled
        isIncreaseScaleThresholdWhenRotating = uiSettings.increaseScaleThresholdWhenRotating
        isLogoEnabled = uiSettings.isLogoEnabled
        isQuickZoomGesturesEnabled = uiSettings.quickZoomGesturesEnabled
        isRotateGesturesEnabled = uiSettings.rotateGesturesEnabled
        isRotateVelocityAnimationEnabled = uiSettings.rotateVelocityAnimationEnabled
        isScaleVelocityAnimationEnabled = uiSettings.scaleVelocityAnimationEnabled
        isScrollGesturesEnabled = uiSettings.scrollGesturesEnabled
        isTiltGesturesEnabled = uiSettings.tiltGesturesEnabled
        isZoomGesturesEnabled = uiSettings.zoomGesturesEnabled
        zoomRate = uiSettings.zoomRate

        uiSettings.compassGravity?.let { compassGravity = it }
        uiSettings.logoGravity?.let { logoGravity = it }
    }
}

private fun MapLibreMap.applyProperties(properties: MapProperties) {
    properties.maxZoom?.let { this.setMaxZoomPreference(it) }
    properties.minZoom?.let { this.setMinZoomPreference(it) }
    properties.maxPitch?.let { this.setMaxPitchPreference(it) }
    properties.minPitch?.let { this.setMinPitchPreference(it) }
    properties.latLngBounds?.let { this.setLatLngBoundsForCameraTarget(it) }
}

/*private fun MapLibreMap.stopLocation(locationEngine: LocationEngine?) {
    Timber.i("removeLocationUpdates")
    locationEngine?.removeLocationUpdates(locationCallback)
}*/

private fun MapLibreMap.setupLocation(
    context: Context,
    style: Style,
    locationRequestProperties: LocationRequestProperties,
    locationEngine: LocationEngine? = null,
    locationStyling: LocationStyling,
    userLocation: MutableState<Location>?,
    renderMode: MutableIntState,
    cameraMode: MutableIntState,
) {
    //Timber.i("setupLocation")
    val locationEngineRequest = locationRequestProperties.toMapLibre()

    val baseEngine = locationEngine ?: LocationEngineDefault.getDefaultLocationEngine(context)
    val safeEngine = SafeLocationEngine(baseEngine)

    val activationBuilder = LocationComponentActivationOptions
        .builder(context, style)
        .locationComponentOptions(locationStyling.toMapLibre(context))
        .locationEngineRequest(locationEngineRequest)
        .locationEngine(safeEngine)

    val locationActivationOptions = activationBuilder.build()
    try {
        this.locationComponent.activateLocationComponent(locationActivationOptions)

        if (isFineLocationGranted(context) || isCoarseLocationGranted(context)) {
            @SuppressLint("MissingPermission")
            this.locationComponent.isLocationComponentEnabled = true
            userLocation?.let {
                trackLocation(
                    safeEngine,
                    locationEngineRequest,
                    userLocation,
                )
            }
        }
    } catch (e: Exception) {
        Timber.e(e, "Failed to activate location component")
    }

    this.locationComponent.renderMode = renderMode.intValue

    this.locationComponent.addOnRenderModeChangedListener { currentMode ->
        if (renderMode.intValue != currentMode) {
            locationComponent.renderMode = renderMode.intValue
            Timber.i("renderMode: $renderMode")
        }
    }

    this.locationComponent.addOnCameraTrackingChangedListener(
        object : OnCameraTrackingChangedListener {
            override fun onCameraTrackingDismissed() {
                cameraMode.intValue = locationComponent.cameraMode
            }

            override fun onCameraTrackingChanged(currentMode: Int) {
                if (cameraMode.intValue != currentMode) {
                    locationComponent.cameraMode = cameraMode.intValue
//                    Timber.i("cameraMode: $cameraMode")
                }
            }
        })

    this.locationComponent.cameraMode = cameraMode.intValue
}

private fun isFineLocationGranted(context: Context): Boolean {
    return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

private fun isCoarseLocationGranted(context: Context): Boolean {
    return context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
private fun trackLocation(
    locationEngine: LocationEngine,
    locationEngineRequest: LocationEngineRequest,
    userLocation: MutableState<Location>
) {
    try {
        locationEngine.requestLocationUpdates(
            locationEngineRequest,
            object : LocationEngineCallback<LocationEngineResult> {
                override fun onSuccess(result: LocationEngineResult?) {
                    result?.lastLocation?.let {
                        userLocation.value = it
                    }
                }

                override fun onFailure(exception: Exception) {
                    Timber.e("trackLocation failure: ${exception.message}")
                }
            },
            null
        )
    } catch (e: Exception) {
        Timber.e(e, "Failed to request location updates")
    }
}

private fun LocationStyling.toMapLibre(context: Context): LocationComponentOptions {
    val builder = LocationComponentOptions.builder(context)
    this.accuracyAlpha?.let { builder.accuracyAlpha(it) }
    this.accuracyColor?.let { builder.accuracyColor(it) }
    this.enablePulse?.let { builder.pulseEnabled(it) }
    this.enablePulseFade?.let { builder.pulseFadeEnabled(it) }
    this.pulseColor?.let { builder.pulseColor(it) }
    this.bearingTintColor?.let { builder.bearingTintColor(it) }
    this.foregroundTintColor?.let { builder.foregroundTintColor(it) }
    this.backgroundTintColor?.let { builder.backgroundTintColor(it) }
    this.foregroundStaleTintColor?.let { builder.foregroundStaleTintColor(it) }
    this.backgroundStaleTintColor?.let { builder.backgroundStaleTintColor(it) }
    return builder.build()
}

private fun LocationRequestProperties.toMapLibre(): LocationEngineRequest {
    return LocationEngineRequest.Builder(this.interval)
        .setPriority(this.priority.value)
        .setFastestInterval(this.fastestInterval)
        .setDisplacement(this.displacement)
        .setMaxWaitTime(this.maxWaitTime)
        .build()
}

private fun MapLibreMap.addImages(context: Context, images: List<Pair<String, Int>>?) {
    //Timber.i("addImage: images: ${images?.size} ")
    images?.let {
        images.mapNotNull { image ->
            val drawable = AppCompatResources.getDrawable(context,image.second)
            val bitmap = BitmapUtils.getBitmapFromDrawable(drawable)
            bitmap?.let { Pair(image.first, bitmap) }
        }.forEach {
            style!!.addImage(it.first, it.second)
            //Timber.i("addImage: ${it.first} ${it.second} ")
        }
    }
}

fun MapLibreMap.addSources(sources: List<Source>?) {
    try {
        sources?.forEach {
            style!!.addSource(it)
        }
    } catch (exception: Exception) {
        Timber.e("$exception")
    }
}

fun MapLibreMap.addLayers(layers: List<Layer>?) {
    try {
        layers?.forEach {
            style!!.addLayer(it)
        }
    } catch (exception: Exception) {
        Timber.i("$exception")
    }
}

@Composable
internal fun MapUpdater(
    map: MapLibreMap,
    style: MutableState<Style?>,
    cameraPosition: CameraPosition,
    uiSettings: UiSettings,
    properties: MapProperties,
    locationRequestProperties: LocationRequestProperties,
    locationEngine: LocationEngine?,
    locationStyling: LocationStyling,
    userLocation: MutableState<Location>?,
    renderMode: MutableIntState, //Int,
    cameraMode: MutableIntState,
) {
    val context = LocalContext.current
    val currentCameraMode by rememberUpdatedState(cameraMode.intValue)
    val currentRenderMode by rememberUpdatedState(renderMode.intValue)

    fun observeZoom(cameraPosition: CameraPosition) {
        map.addOnScaleListener(object : MapLibreMap.OnScaleListener {
            override fun onScaleBegin(detector: StandardScaleGestureDetector) {}

            override fun onScale(detector: StandardScaleGestureDetector) {
                cameraPosition.zoom = map.cameraPosition.zoom
            }

            override fun onScaleEnd(detector: StandardScaleGestureDetector) {}
        })
    }

    fun observeCameraPosition(cameraPosition: CameraPosition) {
        map.addOnMoveListener(object : MapLibreMap.OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) {}

            override fun onMove(detector: MoveGestureDetector) {
                cameraPosition.target = map.cameraPosition.target
            }

            override fun onMoveEnd(detector: MoveGestureDetector) {}
        })
    }

    fun observeBearing(cameraPosition: CameraPosition) {
        map.addOnRotateListener(object : MapLibreMap.OnRotateListener {
            override fun onRotateBegin(detector: RotateGestureDetector) {}

            override fun onRotate(detector: RotateGestureDetector) {
                cameraPosition.bearing = map.cameraPosition.bearing
            }

            override fun onRotateEnd(detector: RotateGestureDetector) {}
        })
    }

    fun observeTilt(cameraPosition: CameraPosition) {
        map.addOnShoveListener(object : MapLibreMap.OnShoveListener {
            override fun onShoveBegin(detector: ShoveGestureDetector) {}

            override fun onShove(detector: ShoveGestureDetector) {
                cameraPosition.tilt = map.cameraPosition.tilt
            }

            override fun onShoveEnd(detector: ShoveGestureDetector) {}
        })
    }

    fun observeIdle(cameraPosition: CameraPosition) {
        map.addOnCameraIdleListener {
            cameraPosition.zoom = map.cameraPosition.zoom
            cameraPosition.target = map.cameraPosition.target
            cameraPosition.bearing = map.cameraPosition.bearing
            cameraPosition.tilt = map.cameraPosition.tilt
        }
    }

    ComposeNode<MapPropertiesNode, MapApplier>(factory = {
        MapPropertiesNode(
            context = context,
            map = map,
            style = style,
            uiSettings = uiSettings,
            properties = properties,
            cameraPosition = cameraPosition,
            locationRequestProperties = locationRequestProperties,
            locationEngine = locationEngine,
            locationStyling = locationStyling,
            userLocation = userLocation,
            renderMode = renderMode,
            cameraMode = cameraMode,
        )
    }, update = {
        observeZoom(cameraPosition)
        observeCameraPosition(cameraPosition)
        observeBearing(cameraPosition)
        observeTilt(cameraPosition)
        observeIdle(cameraPosition)

        update(uiSettings) {
            map.applyUiSettings(uiSettings)
        }

        update(properties) {
            map.applyProperties(properties)
        }

        update(locationRequestProperties) {
            try {
                map.locationComponent.locationEngineRequest = locationRequestProperties.toMapLibre()
            } catch (e: Exception) {
                Timber.e(e, "Failed to update location engine request")
            }
        }

        update(locationEngine) {
            try {
                val baseEngine = locationEngine ?: LocationEngineDefault.getDefaultLocationEngine(context)
                map.locationComponent.locationEngine = SafeLocationEngine(baseEngine)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update location engine")
            }
        }

        update(locationStyling) {
            map.locationComponent.applyStyle(locationStyling.toMapLibre(context))
        }

        update(currentCameraMode) {
            map.locationComponent.cameraMode = cameraMode.intValue
        }
        update(currentRenderMode) {
            map.locationComponent.renderMode = renderMode.intValue
        }

        update(renderMode) {
            map.locationComponent.renderMode = renderMode.intValue
        }

        update(cameraPosition) {
            this.cameraPosition = it
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition.toMapLibre())

            when (cameraPosition.motionType) {
                CameraMotionType.INSTANT -> map.moveCamera(cameraUpdate)

                CameraMotionType.EASE -> map.easeCamera(
                    cameraUpdate,
                    cameraPosition.animationDurationMs
                )

                CameraMotionType.FLY -> map.animateCamera(
                    cameraUpdate,
                    cameraPosition.animationDurationMs
                )
            }
        }
    })
}

internal class MapPropertiesNode(
    val context: Context,
    val map: MapLibreMap,
    val style: MutableState<Style?>,
    val uiSettings: UiSettings,
    val properties: MapProperties,
    var cameraPosition: CameraPosition,
    val locationRequestProperties: LocationRequestProperties,
    val locationEngine: LocationEngine?,
    val locationStyling: LocationStyling,
    val userLocation: MutableState<Location>?,
    val renderMode: MutableIntState,
    val cameraMode: MutableIntState,
) : MapNode {
    override fun onCleared() {
        Timber.i("MapPropertiesNode onCleared")
        //locationEngine?.removeLocationUpdates(locationCallback)
    }

    override fun onRemoved() {
        Timber.i("MapPropertiesNode onRemoved")
        //locationEngine?.removeLocationUpdates(locationCallback)
    }
    override fun onAttached() {
        Timber.i("MapPropertiesNode onAttached")
        map.applyUiSettings(uiSettings)
        map.applyProperties(properties)
        map.cameraPosition = cameraPosition.toMapLibre()

        map.setupLocation(
            context = context,
            style = style.value!!,
            locationRequestProperties = locationRequestProperties,
            locationEngine = locationEngine,
            locationStyling = locationStyling,
            userLocation = userLocation,
            renderMode = renderMode,
            cameraMode = cameraMode,
        )
    }
}

internal fun CameraPosition.toMapLibre(): org.maplibre.android.camera.CameraPosition {
    val builder = org.maplibre.android.camera.CameraPosition.Builder()

    target?.let { builder.target(it) }
    zoom?.let { builder.zoom(it) }
    tilt?.let { builder.tilt(it) }
    bearing?.let { builder.bearing(it) }

    return builder.build()
}

internal class SafeLocationEngine(private val engine: LocationEngine) : LocationEngine {
    @SuppressLint("MissingPermission")
    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) {
        try {
            engine.getLastLocation(callback)
        } catch (e: Exception) {
            Timber.e(e, "SafeLocationEngine: Failed to get last location")
            // Not calling callback.onFailure(e) here as LocationComponent might not expect it
            // and we want to avoid further bubbling if possible.
        }
    }

    @SuppressLint("MissingPermission")
    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        callback: LocationEngineCallback<LocationEngineResult>,
        looper: Looper?
    ) {
        try {
            engine.requestLocationUpdates(request, callback, looper)
        } catch (e: Exception) {
            Timber.e(e, "SafeLocationEngine: Failed to request location updates")
        }
    }

    @SuppressLint("MissingPermission")
    override fun requestLocationUpdates(request: LocationEngineRequest, pendingIntent: PendingIntent?) {
        try {
            engine.requestLocationUpdates(request, pendingIntent)
        } catch (e: Exception) {
            Timber.e(e, "SafeLocationEngine: Failed to request location updates with PendingIntent")
        }
    }

    override fun removeLocationUpdates(callback: LocationEngineCallback<LocationEngineResult>) {
        try {
            engine.removeLocationUpdates(callback)
        } catch (e: Exception) {
            Timber.e(e, "SafeLocationEngine: Failed to remove location updates")
        }
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent?) {
        try {
            engine.removeLocationUpdates(pendingIntent)
        } catch (e: Exception) {
            Timber.e(e, "SafeLocationEngine: Failed to remove location updates with PendingIntent")
        }
    }
}