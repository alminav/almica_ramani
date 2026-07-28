package com.almica.ramani.geojsonMaps

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.Const
import org.maplibre.android.geometry.LatLng
import timber.log.Timber

class MapsManagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = getDefaultSharedPreferences(LocalContext.current)
            val startLatitudeL = prefs.getLong(Const.PREF_LATITUDE, 0L)
            val startLongitudeL = prefs.getLong(Const.PREF_LONGITUDE, 0L)
            val latitude = Double.fromBits(startLatitudeL)
            val longitude = Double.fromBits(startLongitudeL)
            val position = LatLng(latitude, longitude)
            var mvtName: String? by remember { mutableStateOf(null) }
            AdditionalMapsManager(null, position,
                newMvtName = { mvtName = it; Timber.i("mvtName: $it")
            }) { restartRequired ->
                Timber.i("restartRequired: $restartRequired $mvtName")
                val intent = Intent()
                intent.putExtra(Const.EXTRA_MVTNAME, mvtName)
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }
}