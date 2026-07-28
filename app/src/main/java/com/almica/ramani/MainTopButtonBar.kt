package com.almica.ramani

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Height
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.ui.theme.RamaniTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.utils.format
import com.almica.ramani.utils.formatDistValueUnit
import timber.log.Timber

@Composable
fun MainTopButtonBar(setMapLongClickMenu: (Boolean) -> Unit,
                     setLocationStatistic: (Boolean) -> Unit,
                     setGpsValueState: (GpsValue) -> Unit,
                     showLocationStatistic: (Boolean),
                     gpsValueState: GpsValue,
                     logCount: Int) {
    val context = LocalContext.current
    val preferences = getDefaultSharedPreferences(context)
    val stepCountInUse = preferences.getBoolean(Const.PREF_USE_STEPCOUNTER, false)

    val speed by GpsViewModel.speed.collectAsState()
    val distance by GpsViewModel.distance.collectAsState()
    val travelledTime by GpsViewModel.travelledTime.collectAsState()
    val altitude by GpsViewModel.altitude.collectAsState()
    val stepCounter by GpsViewModel.stepCounterFlow.collectAsState()

    MainTopButtonBarContent(
        setMapLongClickMenu,
        setLocationStatistic,
        setGpsValueState,
        showLocationStatistic,
        gpsValueState,
        logCount = logCount,
        stepCountInUse = stepCountInUse,
        distance = distance,
        travelledTime = travelledTime,
        altitude = altitude,
        speed = speed.toDouble(),
        stepCounter = stepCounter
    )
}

@Composable
fun MainTopButtonBarContent(
    setMapLongClickMenu: (Boolean) -> Unit,
    setLocationStatistic: (Boolean) -> Unit,
    setGpsValueState: (GpsValue) -> Unit,
    showLocationStatistic: Boolean,
    gpsValueState: GpsValue,
    logCount: Int,
    stepCountInUse: Boolean,
    distance: Double,
    travelledTime: Long,
    altitude: Double,
    speed: Double,
    stepCounter: Int
) {
    // Top button bar: distance, time, altitude
    val timeFormatted = Helpers.convertSecondsToHHMMSS((travelledTime / 1000).toInt())
    val density = LocalDensity.current.density
    //Timber.i("density: $density")
    val responsiveTitleStyle = if (density < 2.0f) { // Nokia
        MaterialTheme.typography.bodyLarge
    } else {
        MaterialTheme.typography.titleLarge
    }

    val responsiveLabelStyle = MaterialTheme.typography.labelMedium

    val (gpsValueText, gpsUnitText) = remember(gpsValueState, altitude, speed, stepCounter, stepCountInUse) {
        when (gpsValueState) {
            GpsValue.Elevation -> altitude.format(0) to "m"
            GpsValue.Velocity, GpsValue.Speedometer -> {
                if (stepCountInUse) stepCounter.toString() to ""
                else (3.6f * speed).format(0) to "KmH"
            }
        }
    }

    val gpsValueStyle = when (gpsValueState) {
        GpsValue.Velocity, GpsValue.Speedometer -> responsiveTitleStyle
        GpsValue.Elevation -> if (altitude > 9999) responsiveLabelStyle else responsiveTitleStyle
    }

    Box {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
        ) {
            Row {
                Box(
                    modifier = Modifier
                        .padding(end = 3.dp, start = 3.dp)
                        .weight(0.33f)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(corner = CornerSize(6.dp))
                        )
                ) {
                    val textValueUnit = distance.formatDistValueUnit(true)
                    TextButton(onClick = {
                        setMapLongClickMenu(true)
                    }) {
                        Text(
                            text = textValueUnit.first,
                            textDecoration = TextDecoration.Underline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(0.7f)
                                .fillMaxWidth()
                                .padding(start = 1.dp, end = 1.dp),
                            style = responsiveTitleStyle
                        )
                        Text(
                            text = textValueUnit.second,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.4f),
                            textAlign = TextAlign.Center,
                            style = responsiveLabelStyle
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(end = 3.dp)
                        .weight(0.33f)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(corner = CornerSize(6.dp))
                        )
                ) {
                    TextButton(onClick = {
                        setLocationStatistic(showLocationStatistic.not())
                    }) {
                        Text(
                            text = timeFormatted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(corner = CornerSize(6.dp))
                                )
                                .padding(start = 1.dp, end = 1.dp),
                            textDecoration = if (logCount > 0) TextDecoration.Underline else null,
                            textAlign = TextAlign.Center,
                            style = responsiveTitleStyle
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(end = 3.dp)
                        .weight(0.33f)
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(corner = CornerSize(6.dp))
                        )
                ) {
                    TextButton(
                        onClick = {
                            setGpsValueState (
                                when (gpsValueState) {
                                    GpsValue.Elevation -> GpsValue.Velocity
                                    GpsValue.Velocity -> GpsValue.Speedometer
                                    GpsValue.Speedometer -> GpsValue.Elevation
                                }
                            )
                        },
                    ) {
                        when (gpsValueState) {
                            GpsValue.Elevation -> {
                                Icon(painterResource(id = R.drawable.s_peak_small), null)
                            }

                            GpsValue.Velocity, GpsValue.Speedometer -> {
                                if (stepCountInUse)
                                    Icon(Icons.AutoMirrored.Outlined.DirectionsWalk, null)
                                else
                                    Icon(Icons.Outlined.Speed, null)
                            }
                        }

                        Text(
                            text = gpsValueText,
                            modifier = Modifier
                                .weight(0.6f)
                                .padding(start = 1.dp, end = 1.dp),
                            textDecoration = TextDecoration.Underline,
                            textAlign = TextAlign.Center,
                            style = gpsValueStyle
                        )
                        Text(
                            text = gpsUnitText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.4f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainTopButtonBarPreview() {
    RamaniTheme {
        MainTopButtonBarContent(
            setMapLongClickMenu = {},
            setLocationStatistic = {},
            setGpsValueState = {},
            showLocationStatistic = false,
            gpsValueState = GpsValue.Velocity,
            logCount = 5,
            stepCountInUse = false,
            distance = 1234.5,
            travelledTime = 3661000L, // 01:01:01
            altitude = 567.0,
            speed = 5.5, // ~20 km/h
            stepCounter = 1000
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainTopButtonBarElevationPreview() {
    RamaniTheme {
        MainTopButtonBarContent(
            setMapLongClickMenu = {},
            setLocationStatistic = {},
            setGpsValueState = {},
            showLocationStatistic = true,
            gpsValueState = GpsValue.Elevation,
            logCount = 0,
            stepCountInUse = false,
            distance = 500.0,
            travelledTime = 120000L,
            altitude = 1234.0,
            speed = 0.0,
            stepCounter = 0
        )
    }
}
