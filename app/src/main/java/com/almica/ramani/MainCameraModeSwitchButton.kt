package com.almica.ramani

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationDisabled
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.almica.ramani.charts.theme.Teal200
import com.almica.ramani.ui.theme.RamaniTheme

/**
 * CameraMode switch + bottom bar visibility switch.
 * This is a stateless UI component.
 */
@Composable
fun MainCameraModeSwitchButton(
    renderModeMap: String,
    toggleButtonsBottomBar: Boolean,
    onToggleCameraMode: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Camera Mode Toggle Button
        val tint by animateColorAsState(
            if (renderModeMap == Const.RENDER_MODE_FREE) Color.Gray
            else colorResource(R.color.teal_200_trans),
            label = "CameraModeTint"
        )
        IconButton(
            onClick = onToggleCameraMode,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 100.dp, start = 16.dp)
                .size(48.dp)
                .background(tint, CircleShape)
                .border(1.dp, Teal200, CircleShape)
        ) {
            when (renderModeMap) {
                Const.RENDER_MODE_COMPASS -> {
                    Icon(
                        painter = painterResource(R.drawable.baseline_northup_24),
                        contentDescription = "Switch to North-Up"
                    )
                }
                Const.RENDER_MODE_TRACKING -> {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = "Enable Location Tracking"
                    )
                }
                Const.RENDER_MODE_FREE -> {
                    Icon(
                        imageVector = Icons.Outlined.LocationDisabled,
                        contentDescription = "Disable Location Tracking"
                    )
                }
            }
        }

        // Bottom Menu Button
        AnimatedVisibility(
            visible = !toggleButtonsBottomBar,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.dp, end = 16.dp)
        ) {
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier
                    .size(48.dp)
                    .background(colorResource(R.color.teal_200_trans), CircleShape)
                    .border(1.dp, Teal200, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = "Open Menu",
                    tint = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainCameraModeSwitchButtonPreview() {
    RamaniTheme {
        MainCameraModeSwitchButton(
            renderModeMap = Const.RENDER_MODE_COMPASS,
            toggleButtonsBottomBar = false,
            onToggleCameraMode = {},
            onOpenMenu = {}
        )
    }
}
