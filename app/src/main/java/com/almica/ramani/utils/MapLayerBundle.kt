package com.almica.ramani.utils

import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * A bundle containing a MapLibre source and its associated layers.
 */
data class MapLayerBundle(
    val source: GeoJsonSource,
    val lineLayer: LineLayer,
    val symbolLayer: SymbolLayer
)
