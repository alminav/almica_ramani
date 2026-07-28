package com.almica.ramani

import com.google.android.gms.maps.model.LatLng

class LatLngH {
    var instructionText: String? = null
    var instructionName: String? = null
    constructor(latitude: Double, longitude: Double) : this(LatLng(latitude, longitude), 0.0)
    var latLng: LatLng
    var latLngGms: LatLng
    var latLngMapLibre: org.maplibre.android.geometry.LatLng
    var altitude: Double
    var time: Long
    var instructionSign: Int = 0
    var distRatio: Float = 0f
    var legDistance = 0.0
    var isTurn: Boolean = false

    constructor(latLng: LatLng, altitude: Double) {
        this.latLngGms = LatLng(latLng.latitude, latLng.longitude)
        this.latLngMapLibre = org.maplibre.android.geometry.LatLng(latLng.latitude, latLng.longitude)
        this.latLng = latLng
        this.altitude = altitude
        this.time = 0
    }

    constructor(latLng: LatLng, altitude: Double, time: Long) {
        this.latLngGms = LatLng(latLng.latitude, latLng.longitude)
        this.latLngMapLibre = org.maplibre.android.geometry.LatLng(latLng.latitude, latLng.longitude)
        this.latLng = latLng
        this.altitude = altitude
        this.time = time
    }

    constructor(latitude: Double, longitude: Double, altitude: Double) : this(
        LatLng(
            latitude,
            longitude
        ), altitude
    )

    constructor(latitude: Double, longitude: Double, altitude: Double, time: Long) : this(
        LatLng(
            latitude,
            longitude
        ), altitude, time
    )


    val longitude: Double
        get() = latLng.longitude

    val latitude: Double
        get() = latLng.latitude
}
