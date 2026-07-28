package com.almica.ramani

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.almica.ramani.charts.theme.HoloBlueDark
import com.almica.ramani.charts.theme.Teal200
import com.almica.ramani.charts.theme.White
import com.almica.ramani.routes.RouteEntity
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.ui.theme.RamaniTheme

@Composable
fun MainBottomButtonBar(bottomBarVisibility: Boolean,
                        loadedRouteEntity: RouteEntity?,
                        logCount: Int,
                        setRouteMonitorMenu: (Boolean) -> Unit,
                        setHaircrossMenu: (Boolean) -> Unit,
                        setMapMenu: (Boolean) -> Unit,
                        setPoiDatabase: (Boolean) -> Unit,
                        setRouteFiles: (Boolean) -> Unit,
                        setLocationsMenu: (Boolean) -> Unit,
                        dimmerState: (Boolean) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.align(alignment = Alignment.BottomCenter)) {
            AnimatedVisibility(
                visible = bottomBarVisibility,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp)
            ) {
                Column {
                    Row(modifier = Modifier.align(Alignment.End)) {
                        AnimatedVisibility(visible = loadedRouteEntity.isNotNull()) {
                            IconButton(
                                onClick = {
                                    setRouteMonitorMenu(true)
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .width(32.dp)
                                    .height(32.dp)
                                    .border(1.dp, Teal200, CircleShape)
                                    .background(Teal200)
                            ) {
                                Icon(
                                    painterResource(R.drawable.baseline_route_24),
                                    null
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                dimmerState(false)
                                setHaircrossMenu(true)
                            }, modifier = Modifier
                                .clip(CircleShape)
                                .width(32.dp)
                                .height(32.dp)
                                .border(1.dp, HoloBlueDark, CircleShape)
                                .background(HoloBlueDark)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Navigation,
                                contentDescription = "navigation",
                                tint = White
                            )
                        }

                        IconButton(
                            onClick = {
                                setMapMenu(true)
                            }, modifier = Modifier
                                .clip(CircleShape)
                                .width(32.dp)
                                .height(32.dp)
                                .border(1.dp, HoloBlueDark, CircleShape)
                                .background(HoloBlueDark)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = White
                            )
                        }

                        IconButton(
                            onClick = {
                                setPoiDatabase(true)
                            }, modifier = Modifier
                                .clip(CircleShape)
                                .width(32.dp)
                                .height(32.dp)
                                .border(1.dp, HoloBlueDark, CircleShape)
                                .background(HoloBlueDark)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Place,
                                contentDescription = "Pois",
                                tint = White
                            )
                        }

                        Button(
                            onClick = { setRouteFiles(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HoloBlueDark
                            )
                        ) {
                            Text(text = stringResource(R.string.routes))
                        }

                        BadgedBox(badge = { Badge { Text("$logCount") } }) {
                            Button(
                                onClick = { setLocationsMenu(true) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HoloBlueDark
                                )
                            ) {
                                Text(text = stringResource(R.string.track))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainBottomButtonBarPreview() {
    RamaniTheme {
        MainBottomButtonBar(
            bottomBarVisibility = true,
            loadedRouteEntity = RouteEntity(name = "Sample Route", region = "Mountains"),
            logCount = 5,
            setRouteMonitorMenu = {},
            setHaircrossMenu = {},
            setMapMenu = {},
            setPoiDatabase = {},
            setRouteFiles = {},
            setLocationsMenu = {},
            dimmerState = {}
        )
    }
}