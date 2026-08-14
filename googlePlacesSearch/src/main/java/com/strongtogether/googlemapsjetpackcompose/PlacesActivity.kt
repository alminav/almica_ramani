package com.strongtogether.googlemapsjetpackcompose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.android.libraries.places.api.Places
import com.strongtogether.googlemapsjetpackcompose.screens.GoogleMapSearchScreen
import com.strongtogether.googlemapsjetpackcompose.utils.ManifestUtils
import com.strongtogether.googlemapsjetpackcompose.utils.MyDebugTree
import com.strongtogether.googlemapsjetpackcompose.viewmodel.MapViewModel
import timber.log.Timber

/**
 * 14aug2026 obsolete, MapScreen is called directly from GeoCoderLauncher
 */
class PlacesActivity : ComponentActivity() {

    private val viewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Timber.treeCount == 0) {
            Timber.plant(MyDebugTree())
        }

        val startLat = intent.getDoubleExtra(Const.EXTRA_LATITUDE, -1.0)
        val startLon = intent.getDoubleExtra(Const.EXTRA_LONGITUDE, -1.0)
        Timber.i("startLat: $startLat startLon: $startLon")

        val apiKey = ManifestUtils.getApiKeyFromManifest(this)
        if (!Places.isInitialized() && (apiKey != null)) {
            Places.initializeWithNewPlacesApiEnabled(applicationContext, apiKey)
        }
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    GoogleMapSearchScreen(viewModel, startLat, startLon) { name, latLng ->
                        Timber.i("name: $name latLng: $latLng")
                        val resultIntent = Intent()
                        latLng?.let {
                            resultIntent.putExtra(Const.PLACE_NAME, name)
                            resultIntent.putExtra(Const.PLACE_LATITUDE, it.latitude)
                            resultIntent.putExtra(Const.PLACE_LONGITUDE, it.longitude)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                }
            }
        }
    }
}

