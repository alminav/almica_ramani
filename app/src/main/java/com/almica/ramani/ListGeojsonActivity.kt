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
            ListGeojsonScreen { selection ->
                Timber.i("resultPair: ${selection.path} ${selection.name}")
                val resultIntent = Intent()
                if (selection.path.isNotEmpty()) {
                    resultIntent.putExtra(Const.RESULT_GEOJSON_FOLDERNAME, selection.path)
                    resultIntent.putExtra(Const.RESULT_GEOJSON_FILENAME, selection.name)
                }
                setResult(RESULT_OK, resultIntent)
                if (selection.isBack)
                    finish()
            }
        }
    }
}