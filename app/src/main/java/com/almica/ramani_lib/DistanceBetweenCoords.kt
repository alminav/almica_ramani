package com.almica.ramani_lib

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.core.graphics.minus
import org.maplibre.android.geometry.LatLng

@MapLibreComposable
@Composable
fun screenDistanceBetween(a: LatLng, b: LatLng): Float {
    val mapApplier = currentComposer.applier as MapApplier
    val projection = mapApplier.map.projection

    val pixelA = projection.toScreenLocation(a)
    val pixelB = projection.toScreenLocation(b)

    return (pixelB - pixelA).length()
}