package com.almica.ramani

//import org.maplibre.android.geometry.LatLng
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.Const.Companion.EXTRA_ACTIVITY
import com.almica.ramani.Const.Companion.EXTRA_LATLNG
import com.almica.ramani.Const.Companion.EXTRA_MVT_MAP_PATH
import com.almica.ramani.Const.Companion.EXTRA_RESTART
import com.almica.ramani.Helpers.Companion.createMvtOfflineStyle
import com.almica.ramani.Helpers.Companion.getTileName
import com.almica.ramani.bglocationaccess.LocationService
import com.almica.ramani.bglocationaccess.hasActivityRecognitionPermission
import com.almica.ramani.bglocationaccess.hasBGLocationPermission
import com.almica.ramani.bglocationaccess.hasLocationPermission
import com.almica.ramani.bglocationaccess.hasNotificationPerm
import com.almica.ramani.compass.CompassViewModel
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.GeoJsonUtils.Companion.pointToTile
import com.almica.ramani.utils.HgtReader
import com.almica.ramani.utils.ManifestUtils
import com.almica.ramani.utils.MyDebugTree
import com.almica.ramani.utils.getCenter
import com.almica.ramani.utils.getEquidistantPoints
import com.almica.ramani.utils.isNotNull
import com.google.android.libraries.places.api.Places
import me.ibrahimsn.library.LiveSharedPreferences
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.RasterSource
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader


enum class MainSnackbarSelection {
    Nothing,
    RouteCalculation,
    //EnableDimmer,
    SetStop,
    RemoveStop,
    SaveRoute,
    MapManager,
    Drive,
    Bbbike,
    SelectMvt,
    ChangeMvt,
    RouteSideBar,
    AppRestart,
    ToggleTracking
}
enum class GpsValue {
    Elevation,
    Velocity,
    Speedometer
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : FragmentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("MissingPermission", "UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(MyDebugTree())

        startLocationService()
        initInitialState()
        checkPermissions()
        setupWindowFlags()

        enableEdgeToEdge()
        setContent {
            val uiState by mainViewModel.uiState.collectAsState()
            
            MainContent(
                localStyleUri = uiState.styleUriToUse,
                startLatLng = LatLng(
                    intent.getDoubleExtra(Const.EXTRA_LATITUDE, 0.0),
                    intent.getDoubleExtra(Const.EXTRA_LONGITUDE, 0.0)
                ),
                reComposition = { recompose, mvtMapPath, latLng ->
                    handleRecomposition(recompose, mvtMapPath, latLng)
                },
                mapViewReady = { /* Handled in Compose */ }
            )
        }
    }

    private fun startLocationService() {
        Intent(this, LocationService::class.java).apply {
            putExtra(Const.TAG_START_TIME, System.currentTimeMillis())
            action = LocationService.ACTION_SERVICE_START
            startService(this)
        }
    }

    private fun initInitialState() {
        val latitude = intent.getDoubleExtra(Const.EXTRA_LATITUDE, 0.0)
        val longitude = intent.getDoubleExtra(Const.EXTRA_LONGITUDE, 0.0)
        Timber.i("latitude: $latitude longitude: $longitude")
        
        GpsViewModel.loadDistance(0.0)
        GpsViewModel.loadLatitude(latitude)
        GpsViewModel.loadLongitude(longitude)
        CompassViewModel.setDestination(null, null)
        CompassViewModel.setRouteThumbnail(null)
        CompassViewModel.setCurrentLocation(null, null)
        
        mainViewModel.calculateStyleUri(latitude, longitude)
    }


    private fun setupWindowFlags() {
        val preferences = getDefaultSharedPreferences(this)
        val liveSharedPreferences = LiveSharedPreferences(preferences)
        liveSharedPreferences.getBoolean(Const.PREF_KEEP_SCREEN_ON, true).observe(this) { value ->
            if (value != false) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private fun handleRecomposition(recompose: Boolean, mvtMapPath: String?, latLng: LatLng?) {
        Timber.i("recompose $recompose $latLng")
        val resultIntent = Intent().apply {
            putExtra(EXTRA_RESTART, recompose)
            putExtra(EXTRA_ACTIVITY, this@MainActivity::class.java.simpleName)
            mvtMapPath?.let { putExtra(EXTRA_MVT_MAP_PATH, it) }
            latLng?.let { putExtra(EXTRA_LATLNG, doubleArrayOf(it.latitude, it.longitude)) }
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Intent(this, LocationService::class.java).apply {
            action = LocationService.ACTION_SERVICE_STOP
            startService(this)
        }
    }

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            Timber.d("${it.key} = ${it.value}")
            if (it.key == Manifest.permission.POST_NOTIFICATIONS && it.value) {
                askForBGPermission()
            }
        }
    }

    private val requestLocationPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            askForBGPermission()
        }
    }

    private val requestBGLocationPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Timber.d("Background Permission is true")
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPerm()) {
                requestMultiplePermissions.launch(
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.USE_EXACT_ALARM,
                        Manifest.permission.SCHEDULE_EXACT_ALARM,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    )
                )
            } else {
                checkLocationPerm()
            }
        } else {
            checkLocationPerm()
        }
    }

    private fun checkLocationPerm() {
        when {
            !hasLocationPermission() -> requestLocationPerm.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasActivityRecognitionPermission() ->
                requestLocationPerm.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            else -> askForBGPermission()
        }
    }

    private fun askForBGPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBGLocationPermission()) {
            requestBGLocationPerm.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }
}

data class RasterMapsItems(
    val rasterSourceList: ArrayList<RasterSource>,
    val rasterLayerList: ArrayList<RasterLayer>
)


fun MainActivity_placeholder() {}

fun InputStream.readToString(): String {
    val r = BufferedReader(InputStreamReader(this))
    val total = StringBuilder("")
    var line: String?
    while (r.readLine().also { line = it } != null) {
        total.append(line).append('\n')
    }
    return total.toString()
}
