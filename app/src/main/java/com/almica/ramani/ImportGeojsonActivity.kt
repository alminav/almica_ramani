package com.almica.ramani

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.almica.ramani.geojsonMaps.ImportGeojsonDropdownMenu
import com.almica.ramani.routes.DropdownSrtmFiles

class ImportGeojsonActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            Column() {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.import_geojson_maps), textAlign = TextAlign.Center)
                }
                Box(
                    modifier = Modifier.fillMaxSize(0.4f).padding(start = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ImportGeojsonDropdownMenu(context, listOf(null)) { fileType, _ ->
                        val resultIntent = Intent()
                        resultIntent.putExtra(Const.SETRESULT_IMPORT_GEOJSON, fileType.name)
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                }
            }
        }
    }
}