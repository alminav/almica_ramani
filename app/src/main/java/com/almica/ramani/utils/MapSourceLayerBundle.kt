package com.almica.ramani.utils

import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * A bundle containing a MapLibre source and its associated line layer.
 */
data class MapSourceLayerBundle(
    val source: GeoJsonSource,
    val layer: LineLayer
)
