package com.almica.ramani

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationDisabled
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.almica.ramani.charts.theme.Teal200
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani_lib.CameraPosition
import me.ibrahimsn.library.LiveSharedPreferences
import org.maplibre.android.location.modes.CameraMode
import timber.log.Timber

/**
 * CameraMode switch + bottom bar visibility switch
 */
@Composable
fun MainCameraModeSwitchButton(
    toggleButtonsBottomBar: Boolean,
    cameraPosition: MutableState<CameraPosition>,
    renderMode: String?,
    setSatStatus: (Boolean) -> Unit,
    setButtonsBottomBar: (Boolean) -> Unit,
    setRenderMode: (String) -> Unit,
    liveSharedPreferences: LiveSharedPreferences
) {
    //Timber.i("toggleButtonsBottomBar: $toggleButtonsBottomBar")
    val resources = LocalResources.current
    var renderModeMap by remember { mutableStateOf(renderMode) }
    //Timber.i("renderModeValue: $renderModeMap")
    var cameraMode by remember { mutableIntStateOf(CameraMode.TRACKING_GPS) }
    when (renderModeMap) {
        Const.RENDER_MODE_COMPASS -> {cameraMode = CameraMode.TRACKING_GPS}
        Const.RENDER_MODE_TRACKING -> {cameraMode = CameraMode.TRACKING_GPS}
        Const.RENDER_MODE_FREE -> {cameraMode = CameraMode.NONE}
    }
    liveSharedPreferences.getString(resources.getString(R.string.pref_render_mode), Const.RENDER_MODE_COMPASS).observe(LocalLifecycleOwner.current) { value ->
        if (value != null) {
            //Timber.i("renderModeValue $value")
            renderModeMap = value
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.align(alignment = Alignment.BottomCenter)) {
            AnimatedVisibility(
                visible = !toggleButtonsBottomBar,
                modifier = Modifier.padding(bottom = 50.dp)
            ) {
                    Row {
                        val tint by animateColorAsState(if (renderModeMap == "0") Color.Gray
                            else colorResource(R.color.teal_200_trans))
                        IconButton(
                            onClick = {
                                Timber.i("renderModeValue: $renderModeMap cameraMode: $cameraMode")
                                // 0 ==> 2 follow north
                                // 1 ==> 0 off
                                // 2 ==> 1 follow compass
                                when (renderModeMap) {
                                    "0" -> Pair("2", CameraMode.TRACKING_GPS_NORTH)
                                    "1" -> Pair("0", CameraMode.NONE)
                                    "2" -> Pair("1", CameraMode.TRACKING_GPS)
                                    else -> Pair("0", CameraMode.NONE)
                                }.let { (nextRenderMode, nextCameraMode) ->
                                    // Update local state
                                    renderModeMap = nextRenderMode
                                    cameraMode = nextCameraMode

                                    // Apply specific side effects
                                    if (nextRenderMode == "2") {
                                        setSatStatus(false)
                                    }
                                    if (nextCameraMode == CameraMode.NONE) {
                                        cameraPosition.value = CameraPosition(cameraPosition.value).apply {
                                            this.bearing = 0.0
                                        }
                                    }

                                    // Persist changes in a single transaction
                                    liveSharedPreferences.preferences.edit {
                                        putInt(Const.PREF_CAMERA_MODE, nextCameraMode)
                                        putString(resources.getString(R.string.pref_render_mode), nextRenderMode)
                                    }

                                    // Notify parent
                                    setRenderMode(nextRenderMode)
                                }
                                Timber.i("renderModeValue: $renderModeMap cameraMode: $cameraMode")
                            }, modifier = Modifier
                                .weight(0.125f)
                                .clip(CircleShape)
                                .width(32.dp)
                                .height(32.dp)
                                .border(1.dp, Teal200, CircleShape)
                                .background(tint)
                        ) {
                            //Timber.i("renderModeValue: $renderModeValue")
                            when (renderModeMap) {
                                Const.RENDER_MODE_COMPASS -> {
                                    Icon(
                                        painterResource(R.drawable.baseline_northup_24),
                                        "NorthUp toggle"
                                    )
                                }

                                Const.RENDER_MODE_TRACKING -> {
                                    Icon(
                                        Icons.Outlined.MyLocation,
                                        "Location toggle"
                                    )
                                }

                                Const.RENDER_MODE_FREE -> {
                                    Icon(
                                        Icons.Outlined.LocationDisabled,
                                        "Location toggle"
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.weight(0.75f))
                        IconButton(
                            onClick = {
                                setButtonsBottomBar(true)
                                Timber.i("toggleButtonsBottomBar = true")
                            }, modifier = Modifier
                                .weight(0.125f)
                                .clip(RectangleShape)
                                .width(32.dp)
                                .height(32.dp)
                                .border(1.dp, Teal200, RectangleShape)
                                .background(colorResource(R.color.teal_200_trans))
                        ) {
                            Icon(
                                Icons.Outlined.Menu,
                                null,
                                tint = Color.White
                            )
                        }
                    }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainCameraModeSwitchButtonPreview() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
    val liveSharedPreferences = LiveSharedPreferences(sharedPreferences)
    val cameraPosition = remember { mutableStateOf(CameraPosition()) }
    val renderMode: String? = null

    RamaniTheme {
        MainCameraModeSwitchButton(
            toggleButtonsBottomBar = false,
            cameraPosition = cameraPosition,
            renderMode = renderMode,
            setSatStatus = {},
            setButtonsBottomBar = {},
            setRenderMode = {},
            liveSharedPreferences = liveSharedPreferences
        )
    }
}
