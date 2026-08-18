package com.almica.ramani.utils

import com.almica.ramani.FeatureProperties.Companion.NAME
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.style.sources.GeometryTileProvider
import org.maplibre.geojson.Feature
import com.google.gson.JsonObject
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import timber.log.Timber
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Implementation of GeometryTileProvider that returns features representing a zoom-dependent
 * grid.
 */
internal class GridProvider : GeometryTileProvider {

    override fun getFeaturesForBounds(bounds: LatLngBounds, zoomLevel: Int): FeatureCollection {
        val features = mutableListOf<Feature>()

        val decimalPlaces = when {
            zoomLevel >= 9 -> 2
            zoomLevel >= 6 -> 1
            else -> 0
        }

        val gridSpacing = when {
            zoomLevel >= 13 -> 0.01
            zoomLevel >= 11 -> 0.05
            zoomLevel == 10 -> 0.1
            zoomLevel == 9 -> 0.25
            zoomLevel == 8 -> 0.5
            zoomLevel >= 6 -> 1.0
            zoomLevel == 5 -> 2.0
            zoomLevel >= 3 -> 5.0
            zoomLevel == 2 -> 10.0
            else -> 20.0
        }

        // Add Latitude Lines (Horizontal)
        addGridLines(
            features = features,
            start = ceil(bounds.latitudeNorth / gridSpacing),
            end = floor(bounds.latitudeSouth / gridSpacing),
            step = -1.0,
            spacing = gridSpacing,
            decimalPlaces = decimalPlaces,
            isVertical = false,
            bounds = bounds
        )

        // Add Longitude Lines (Vertical)
        addGridLines(
            features = features,
            start = floor(bounds.longitudeWest / gridSpacing),
            end = ceil(bounds.longitudeEast / gridSpacing),
            step = 1.0,
            spacing = gridSpacing,
            decimalPlaces = decimalPlaces,
            isVertical = true,
            bounds = bounds
        )

        return FeatureCollection.fromFeatures(features)
    }

    private fun addGridLines(
        features: MutableList<Feature>,
        start: Double,
        end: Double,
        step: Double,
        spacing: Double,
        decimalPlaces: Int,
        isVertical: Boolean,
        bounds: LatLngBounds
    ) {
        val diff = end - start
        // Ensure we don't divide by zero or get weird results if start/end are reversed or same
        if ((step > 0 && diff < 0) || (step < 0 && diff > 0)) return
        
        val numSteps = (diff / step).roundToInt()
        for (i in 0..numSteps) {
            val coord = (start + i * step) * spacing

            val geometry = if (isVertical) {
                LineString.fromLngLats(
                    listOf(
                        Point.fromLngLat(coord, bounds.latitudeSouth),
                        Point.fromLngLat(coord, bounds.latitudeNorth)
                    )
                )
            } else {
                LineString.fromLngLats(
                    listOf(
                        Point.fromLngLat(bounds.longitudeWest, coord),
                        Point.fromLngLat(bounds.longitudeEast, coord)
                    )
                )
            }

            //val label = "${coord.format(decimalPlaces)}°"

            // Add the line feature
            val lineProperties = JsonObject()
            //lineProperties.addProperty(NAME, label)
            features.add(Feature.fromGeometry(geometry, lineProperties))

            // Add a point feature for the text label at the center of the line segment within view
/*
            val labelPoint = if (isVertical) {
                Point.fromLngLat(coord, (bounds.latitudeNorth + bounds.latitudeSouth) / 2)
            } else {
                Point.fromLngLat((bounds.longitudeWest + bounds.longitudeEast) / 2, coord)
            }

            val pointProperties = JsonObject()
            pointProperties.addProperty(NAME, label)
            val pointFeature = Feature.fromGeometry(labelPoint, pointProperties)

            Timber.i("labelPoint: $labelPoint with label: $label")
            features.add(pointFeature)
 */
        }
    }
}
