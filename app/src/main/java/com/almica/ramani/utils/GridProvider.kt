package com.almica.ramani.utils

import com.almica.ramani.FeatureProperties.Companion.NAME
import com.almica.ramani.utils.GeoJsonUtils
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.style.sources.GeometryTileProvider
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Implementation of GeometryTileProvider that returns features representing a zoom-dependent
 * grid.
 */
internal class GridProvider : GeometryTileProvider {
    override fun getFeaturesForBounds(bounds: LatLngBounds, zoomLevel: Int): FeatureCollection {
        val features: MutableList<Feature> = ArrayList()
        val decimalPlaces = if (zoomLevel >= 13) { 2
        } else if (zoomLevel >= 11) {
            2
        } else if (zoomLevel == 10) {
            2
        } else if (zoomLevel == 9) {
            2
        } else if (zoomLevel == 8) {
            1
        } else if (zoomLevel >= 6) {
            1
        } else if (zoomLevel == 5) {
            0
        } else if (zoomLevel >= 3) {
            0
        } else if (zoomLevel == 2) {
            0
        } else {
            0
        }

        val gridSpacing = if (zoomLevel >= 13) {
            0.01
        } else if (zoomLevel >= 11) {
            0.05
        } else if (zoomLevel == 10) {
            .1
        } else if (zoomLevel == 9) {
            0.25
        } else if (zoomLevel == 8) {
            0.5
        } else if (zoomLevel >= 6) {
            1.0
        } else if (zoomLevel == 5) {
            2.0
        } else if (zoomLevel >= 3) {
            5.0
        } else if (zoomLevel == 2) {
            10.0
        } else {
            20.0
        }
        var gridLines: MutableList<List<Point>> = mutableListOf()
        var y = ceil(bounds.latitudeNorth / gridSpacing) * gridSpacing
        while (y >= floor(bounds.latitudeSouth / gridSpacing) * gridSpacing) {
            gridLines.add(
                listOf(
                    Point.fromLngLat(bounds.longitudeWest, y),
                    Point.fromLngLat(bounds.longitudeEast, y)
                )
            )
            val lineFeature = Feature.fromGeometry(
                LineString.fromLngLats(listOf(
                    Point.fromLngLat(bounds.longitudeWest, y),
                    Point.fromLngLat(bounds.longitudeEast, y))))
            lineFeature.addStringProperty(NAME, "${y.format((decimalPlaces).coerceAtLeast(0))}°")
            features.add(lineFeature)
            y -= gridSpacing
        }

        //features.add(Feature.fromGeometry(MultiLineString.fromLngLats(gridLines)))
        gridLines = mutableListOf()
        var x = floor(bounds.longitudeWest / gridSpacing) * gridSpacing
        while (x <= ceil(bounds.longitudeEast / gridSpacing) * gridSpacing) {
            gridLines.add(
                listOf(
                    Point.fromLngLat(x, bounds.latitudeSouth),
                    Point.fromLngLat(x, bounds.latitudeNorth)
                )
            )
            val lineFeature = Feature.fromGeometry(
                LineString.fromLngLats(listOf(
                    Point.fromLngLat(x, bounds.latitudeSouth),
                    Point.fromLngLat(x, bounds.latitudeNorth))))
            lineFeature.addStringProperty(NAME, "${x.format(digits = decimalPlaces)}°")
            features.add(lineFeature)
            x += gridSpacing
        }
        //features.add(Feature.fromGeometry(MultiLineString.fromLngLats(gridLines)))
        return FeatureCollection.fromFeatures(features)
    }
}
