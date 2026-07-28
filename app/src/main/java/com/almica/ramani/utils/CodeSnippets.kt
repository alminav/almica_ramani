package com.almica.ramani.utils

class CodeSnippets {
    /** experiment 01feb2026, online maps have contour lines, they are also indistinctly visible in mvt maps */
    /**
    style.addSource(
    VectorSource(
    "terrain-data",
    //"mapbox://mapbox.mapbox-terrain-v2"
    "mapbox://mapbox-terrain-v2"
    )
    )
    val contourLinesLayer = LineLayer(
    "terrain-layer", "terrain-data"
    ).withProperties(
    lineWidth(2.0f),
    lineColor("#FFFFFF".toColorInt()),
    lineJoin(Property.LINE_JOIN_ROUND),
    PropertyFactory.visibility(v)
    )
    style.addLayerAbove(contourLinesLayer, Const.RASTER_DEM_LAYER)
     */
}