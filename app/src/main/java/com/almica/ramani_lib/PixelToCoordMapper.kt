package com.almica.ramani_lib

import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import org.maplibre.android.geometry.LatLng

@Composable
fun PixelToCoordMapper(points: List<PointF>, onChange: (List<LatLng>) -> Unit) {
    val mapApplier = currentComposer.applier as MapApplier
    val projection = mapApplier.map.projection

    onChange(points.map {
        projection.fromScreenLocation(it)
    })
}
