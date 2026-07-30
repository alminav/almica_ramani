package com.almica.ramani.googlemaps

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
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
import com.almica.ramani.utils.ManifestUtils
import com.almica.ramani.utils.MyDebugTree
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import me.ibrahimsn.library.LiveSharedPreferences
import timber.log.Timber

/**
 *
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
        var routeData: RouteData? =
            RouteData(arrayListOf(), "", 0.0, false, null)
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
