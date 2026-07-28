package com.almica.ramani.googlemaps

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.Const
import com.almica.ramani.Helpers
import com.almica.ramani.Helpers.Companion.getTileName
import com.almica.ramani.LatLngH
import com.almica.ramani.utils.getEquidistantPoints
import com.almica.ramani.charts.interpolateColor
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.routes.RouteEntity
import com.almica.ramani.utils.HgtReader
import com.almica.ramani.utils.ManifestUtils
import com.almica.ramani.utils.MyDebugTree
import com.almica.ramani.utils.getCenter
import com.almica.ramani.utils.kmlString2Lllh
import com.almica.ramani.utils.lllhToKmlString
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.maps.android.ui.IconGenerator
import me.ibrahimsn.library.LiveSharedPreferences
import timber.log.Timber
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

/**
 * Not used
 * template for google cloud style, not really working
 */
class GoogleMapsActivity() : ComponentActivity() {
    val logtag = "GoogleMapsActivity"
    @SuppressLint("ViewModelConstructorInComposable")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Timber.plant(Timber.DebugTree())
        Timber.plant(MyDebugTree())
        // Retrieve the API key from the manifest file
        val apiKey = ManifestUtils.getApiKeyFromManifest(this)
        // Initialize the Places API with the retrieved API key
        if (!Places.isInitialized() && apiKey != null) {
            Places.initializeWithNewPlacesApiEnabled(applicationContext, apiKey)
        }
        val cloudStyle = intent.getBooleanExtra(Const.EXTRA_CLOUD_STYLE, false)
        Timber.i( "cloudeStyle: $cloudStyle")
        val latitude = intent.getDoubleExtra(Const.EXTRA_LATITUDE, 0.0)
        val longitude = intent.getDoubleExtra(Const.EXTRA_LONGITUDE, 0.0)
        val latLng = LatLng(latitude, longitude)
        val kmlString = intent.getStringExtra(Const.EXTRA_KMLSTRING)
        var routeData: RouteData? = RouteData(arrayListOf(), "", 0.0, false, null)
        if (kmlString != null) {
            routeData = Helpers.kmlString2RouteData(kmlString)
            Timber.i( "route ${routeData?.name}")
        }
        val zoom = intent.getDoubleExtra(Const.EXTRA_ZOOM, 14.0)
        val preferences = getDefaultSharedPreferences(this)
        //preferences.edit { putBoolean(Const.PREF_USE_STEPCOUNTER, true) }
        val liveSharedPreferences = LiveSharedPreferences(preferences)
        liveSharedPreferences.getBoolean(
            Const.PREF_KEEP_SCREEN_ON,
            true
        ).observe(this) { value ->
            //Log.i(logtag, "${context.getString(R.string.keep_screen_on)}: $value")
            if (value != null) {
                if (value) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            } else
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                // Create a Surface container that uses the theme's background color
                Surface(
                    modifier = Modifier.fillMaxSize(), // Make the surface fill the entire screen
                    color = MaterialTheme.colorScheme.background // Use the background color from the theme
                ) {
                    // Pass the MapViewModel to the MapScreen composable
                    val gmsMapViewModel = GmsMapViewModel(this.application)
                    routeData?.let { GmsMapScreen(gmsMapViewModel, zoom, it, latLng, cloudStyle,
                        backPressed = {fusedLocationClient ->
                            Timber.i( "backPressed")
                            gmsMapViewModel.stopUserLocation(fusedLocationClient)
                            val intent = Intent()
                            setResult(RESULT_OK, intent)
                            finish()
                        }) }
                }
            }
        }
    }
}

data class RouteData(
    var lllh: ArrayList<LatLngH>,
    val name: String,
    val distance: Double,
    var state: Boolean,
    var routeMarkerDataList: List<RouteMarkerData>?
)
data class RouteMarkerData(
    var latLngH: LatLngH,
    val gradient: Double,
    val distanceKm: Double,
    var icon: BitmapDescriptor?
)

fun RouteData.createRouteMarkers(context: Context, _hMax: Double): Double {
    var hMax = _hMax
    val routeMarkerDataList = mutableListOf<RouteMarkerData>()
    val center = this.lllh.getCenter()
    if (hMax < 10) {
        val tileName =
            getTileName(center.latitude, center.longitude).uppercase()
        if (!this.state) {
            val demFolder =
                File(context.filesDir, Const.HGT_FOLDER_NAME)
            val hgtFile = File(demFolder, tileName + Const.HGT_EXT)
            if (hgtFile.exists()) {
                val hgtReader = HgtReader(context, hgtFile)
                val lllhRefreshed =
                    hgtReader.refreshRouteElevationFromSrtm(this.lllh)
                if (lllhRefreshed.isNotNull() && lllhRefreshed.lllh.isNotNull()) {
                    this.lllh = lllhRefreshed.lllh!! as ArrayList<LatLngH>
                    this.state = true
                    hMax = hMax.coerceAtLeast(lllhRefreshed.hMax)
                }
            }
        }
    } else
        this.state = true
    val interval = 750.0 // estimation 19jan2026, 1000.0
    val lllhKmSteps = this.lllh.getEquidistantPoints(interval) //1000.0)
    Timber.i( "lllhKmSteps: ${lllhKmSteps.size}")
    lllhKmSteps.forEachIndexed { i, llh ->
        if (i > 0) {
            var gradient: Double
            val deltaH: Double = lllhKmSteps[i].altitude - lllhKmSteps[i - 1].altitude
            gradient = 100 * deltaH / interval //1 km steps
            val c = interpolateColor((0.1 * abs(gradient)).toFloat())
            //Timber.i("bar value $i: " + "${llh.altitude} gradient: ${gradient.format(1)}")

            val iconGenerator = IconGenerator(context)
            val contentView = TextView(context)
            if (hMax > 10) {
                contentView.text = String.format(
                    Locale.ENGLISH, "%s%.0f%s", Const.UC_ELE_ARROW, llh.altitude, "m")
            } else {
                contentView.text = String.format(
                    Locale.ENGLISH, "%s%d%s", Const.UC_DISTANCE_ARROW, i, " km")
            }
            contentView.setBackgroundColor(c)
            contentView.setTextColor(android.graphics.Color.BLUE)
            contentView.textSize = 14f
            iconGenerator.setBackground(null)
            iconGenerator.setContentView(contentView)
            val bitmap = iconGenerator.makeIcon()
            var icon : BitmapDescriptor? = null
            bitmap?.let { image ->
                icon = BitmapDescriptorFactory.fromBitmap(image)
            }

            routeMarkerDataList.add(RouteMarkerData(lllhKmSteps[i], gradient, i.toDouble(), icon))
        }
    }
    this.routeMarkerDataList = routeMarkerDataList
    return hMax
}

fun RouteData.getRouteEntity(context: Context) : RouteEntity {
    val center = this.lllh.getCenter()
    val tileName =
        getTileName(center.latitude, center.longitude).uppercase()
    if (!this.state) {
        val demFolder =
            File(context.filesDir, Const.HGT_FOLDER_NAME)
        val hgtFile = File(demFolder, tileName + Const.HGT_EXT)
        if (hgtFile.exists()) {
            val hgtReader = HgtReader(context, hgtFile)
            val lllhRefreshed =
                hgtReader.refreshRouteElevationFromSrtm(this.lllh).lllh as ArrayList<LatLngH>
            if (lllhRefreshed.isNotNull()) {
                this.lllh = lllhRefreshed
                this.state = true
            }
        }
    }
    val kmlString = this.lllh.lllhToKmlString(this.name)
    val routeEntity = RouteEntity(UUID.randomUUID(),
        this.name,
        "",
        this.lllh[0].latitude,
        this.lllh[0].longitude,
        center.latitude,
        center.longitude,
        this.lllh[lllh.lastIndex].latitude,
        this.lllh[lllh.lastIndex].longitude,

        this.distance,
        kmlString,
        null
    )
    return routeEntity
}

fun RouteEntity.getRouteData(context: Context) : RouteData {
    var lllh = this.kmlString.kmlString2Lllh()
    val center = lllh.getCenter()
    val tileName =
        getTileName(center.latitude, center.longitude).uppercase()

    val demFolder =
        File(context.filesDir, Const.HGT_FOLDER_NAME)
    val hgtFile = File(demFolder, tileName + Const.HGT_EXT)
    if (hgtFile.exists()) {
        val hgtReader = HgtReader(context, hgtFile)
        val lllhRefreshed =
            hgtReader.refreshRouteElevationFromSrtm(lllh).lllh as ArrayList<LatLngH>
        if (lllhRefreshed.isNotNull()) {
            lllh = lllhRefreshed
        }
    }
    val routeData = RouteData(lllh, this.name, this.distance, state = true, null)
    return routeData
}