package com.strongtogether.googlemapsjetpackcompose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.android.libraries.places.api.Places
import com.strongtogether.googlemapsjetpackcompose.screens.MapScreen
import com.strongtogether.googlemapsjetpackcompose.utils.ManifestUtils
import com.strongtogether.googlemapsjetpackcompose.utils.MyDebugTree
import com.strongtogether.googlemapsjetpackcompose.viewmodel.MapViewModel
import timber.log.Timber

class PlacesActivity() : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(MyDebugTree())
        // Retrieve the API key from the manifest file
        val apiKey = ManifestUtils.getApiKeyFromManifest(this)
        // Initialize the Places API with the retrieved API key
        if (!Places.isInitialized() && apiKey != null) {
            Places.initializeWithNewPlacesApiEnabled(applicationContext, apiKey)
            //Places.createClient(this)
        }
        enableEdgeToEdge()
        setContent {
            val startLat = intent.getDoubleExtra(Const.EXTRA_LATITUDE, -1.0)
            val startLon = intent.getDoubleExtra(Const.EXTRA_LONGITUDE, -1.0)
            Timber.i("startLat: $startLat startLon: $startLon")
            MaterialTheme {
                    // Create a Surface container that uses the theme's background color
                    Surface(
                        modifier = Modifier.fillMaxSize(), // Make the surface fill the entire screen
                        color = MaterialTheme.colorScheme.background // Use the background color from the theme
                    ) {
                        // Pass the MapViewModel to the MapScreen composable
                        val mapViewModel = MapViewModel()
                        MapScreen(mapViewModel, startLat, startLon) {name, latLng ->
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

