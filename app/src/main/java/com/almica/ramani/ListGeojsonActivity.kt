package com.almica.ramani

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import timber.log.Timber

class ListGeojsonActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ListGeojsonScreen { resultTriple ->
                Timber.i("resultPair: ${resultTriple.first} ${resultTriple.second}")
                val resultIntent = Intent()
                if (resultTriple.first.isNotEmpty()) {
                    resultIntent.putExtra(Const.RESULT_GEOJSON_FOLDERNAME, resultTriple.first)
                    resultIntent.putExtra(Const.RESULT_GEOJSON_FILENAME, resultTriple.second)
                }
                setResult(RESULT_OK, resultIntent)
                if (resultTriple.third)
                    finish()
            }
        }
    }
}