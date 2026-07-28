package com.almica.ramani_lib

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun rememberCameraPosition(init: CameraPosition.() -> Unit): CameraPosition {
    return remember {
        CameraPosition().apply(init)
    }
}

@Composable
fun rememberMapProperties(init: MapProperties.() -> Unit): MapProperties {
    return remember {
        MapProperties().apply(init)
    }
}
