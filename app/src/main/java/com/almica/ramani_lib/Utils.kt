package com.almica.ramani_lib

import android.content.ContextWrapper
import android.os.Bundle
import android.util.Log
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val inspectionMode = LocalInspectionMode.current
    if (!inspectionMode) {
        MapLibre.getInstance(context)
    }

    val mapView = remember {
        if (inspectionMode) {
            // In LayoutLib (Preview), context.getFilesDir() can return null, which causes MapLibre to crash.
            val wrappedContext = object : ContextWrapper(context) {
                override fun getFilesDir(): File = File(".")
                override fun getCacheDir(): File = File(".")
            }
            MapView(wrappedContext)
        } else {
            MapView(context)
        }
    }

    if (inspectionMode) return mapView

    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle, mapView) {
        val lifecycleObserver = getMapLifecycleObserver(mapView)
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return mapView
}


fun getMapLifecycleObserver(mapView: MapView): LifecycleEventObserver {
    return LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
            Lifecycle.Event.ON_START -> mapView.onStart()
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            Lifecycle.Event.ON_STOP -> mapView.onStop()
            Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
            else -> throw IllegalStateException()
        }
    }
}

suspend inline fun MapView.awaitMap(): MapLibreMap =
    suspendCoroutine { continuation ->
        getMapAsync {
            Timber.i("$continuation")
            continuation.resume(it)
        }
    }
