package com.almica.ramani.utils

import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * A bundle containing a MapLibre source and its associated layers.
 */
data class GeoJsonLayerBundle(
    val source: GeoJsonSource,
    val layers: List<Layer>
)
