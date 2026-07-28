package com.almica.ramani.googlemaps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberTileOverlayState

@Composable
fun UpdateCoordinateOverlay(mapType: String) {
    val context = LocalContext.current
    val state = rememberTileOverlayState()
    val tileProvider = remember {
        object : CoordinateTileProvider(context, mapType) {}
    }
    TileOverlay(tileProvider = tileProvider, state = state, fadeIn = false, visible = true)
}