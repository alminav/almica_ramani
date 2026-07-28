package com.almica.ramani.googlemaps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.fragment.compose.AndroidFragment
import com.almica.ramani.R
import com.almica.ramani.utils.BackPressHandler
import timber.log.Timber

/**
 * not working with TileOverlayActivity ComponentActivity
 * ==> Solution with MaptypeMenu Dropdown Menu
 *
 * The solution was to use Theme.AppCompat as the base theme.
 * Apparently, android:Theme.Material and other SDK themes do not work with Androidx Preferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RasterMaptypePrefScreen(finished:() -> Unit) {
    BackPressHandler {
        Timber.i("Back Press intercepted")
        finished()
    }
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Timber.i("")
        MaptypePrefComposeContent(innerPadding) {
            finished()
        }
    }
}
@Composable
private fun MaptypePrefComposeContent(innerPadding: PaddingValues, finished: () -> Unit) {
    Timber.i("")
    Column (modifier = Modifier.background(Color.White).padding(innerPadding)){
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { finished() }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back home"
                )
            }
            Text(text = stringResource(R.string.offline_map_preferences))
        }

        AndroidFragment<RasterMaptypePrefFragment>(modifier = Modifier.background(Color.White)) {
            Timber.i(" ")
        }

    }
}