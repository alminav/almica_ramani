package com.almica.ramani_lib

import android.os.Parcelable
import androidx.annotation.FloatRange
import org.maplibre.android.constants.MapLibreConstants
import org.maplibre.android.geometry.LatLngBounds
import kotlinx.parcelize.Parcelize

@Parcelize
class MapProperties(
    var styleUrl: String? = null,
    @FloatRange(
        from = MapLibreConstants.MINIMUM_ZOOM.toDouble(),
        to = MapLibreConstants.MAXIMUM_ZOOM.toDouble()
    ) var maxZoom: Double? = null,
    @FloatRange(
        from = MapLibreConstants.MINIMUM_ZOOM.toDouble(),
        to = MapLibreConstants.MAXIMUM_ZOOM.toDouble()
    ) var minZoom: Double? = null,
    @FloatRange(
        from = MapLibreConstants.MINIMUM_PITCH.toDouble(),
        to = MapLibreConstants.MAXIMUM_PITCH.toDouble()
    ) var maxPitch: Double? = null,
    @FloatRange(
        from = MapLibreConstants.MINIMUM_PITCH.toDouble(),
        to = MapLibreConstants.MAXIMUM_PITCH.toDouble()
    ) var minPitch: Double? = null,
    var latLngBounds: LatLngBounds? = null
) : Parcelable {
    constructor(mapProperties: MapProperties) : this(
        styleUrl = mapProperties.styleUrl,
        maxZoom = mapProperties.maxZoom,
        minZoom = mapProperties.minZoom,
        maxPitch = mapProperties.maxPitch,
        minPitch = mapProperties.minPitch,
        latLngBounds = mapProperties.latLngBounds
    )

    fun copy(
        styleUrl: String? = this.styleUrl,
        @FloatRange(
            from = MapLibreConstants.MINIMUM_ZOOM.toDouble(),
            to = MapLibreConstants.MAXIMUM_ZOOM.toDouble()
        ) maxZoom: Double? = this.maxZoom,
        @FloatRange(
            from = MapLibreConstants.MINIMUM_ZOOM.toDouble(),
            to = MapLibreConstants.MAXIMUM_ZOOM.toDouble()
        ) minZoom: Double? = this.minZoom,
        @FloatRange(
            from = MapLibreConstants.MINIMUM_PITCH.toDouble(),
            to = MapLibreConstants.MAXIMUM_PITCH.toDouble()
        ) maxPitch: Double? = this.maxPitch,
        @FloatRange(
            from = MapLibreConstants.MINIMUM_PITCH.toDouble(),
            to = MapLibreConstants.MAXIMUM_PITCH.toDouble()
        ) minPitch: Double? = this.minPitch,
        latLngBounds: LatLngBounds? = this.latLngBounds
    ): MapProperties {
        return MapProperties(
            styleUrl = styleUrl,
            maxZoom = maxZoom,
            minZoom = minZoom,
            maxPitch = maxPitch,
            minPitch = minPitch,
            latLngBounds = latLngBounds
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapProperties

        return styleUrl == other.styleUrl &&
                maxZoom == other.maxZoom &&
                minZoom == other.minZoom &&
                maxPitch == other.maxPitch &&
                minPitch == other.minPitch &&
                latLngBounds == other.latLngBounds
    }

    override fun hashCode(): Int {
        var result = styleUrl?.hashCode() ?: 0
        result = 31 * result + (maxZoom?.hashCode() ?: 0)
        result = 31 * result + (minZoom?.hashCode() ?: 0)
        result = 31 * result + (maxPitch?.hashCode() ?: 0)
        result = 31 * result + (minPitch?.hashCode() ?: 0)
        result = 31 * result + (latLngBounds?.hashCode() ?: 0)
        return result
    }
}
