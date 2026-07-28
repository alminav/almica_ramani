package com.almica.ramani.googlemaps

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Traffic
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.almica.ramani.charts.theme.Black
import com.almica.ramani.ui.theme.RamaniTheme
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.almica.ramani.R

@Composable
fun DropdownMapMenu(mapProperties: MapProperties, tileOverlayVisibility: Boolean,
                            coordinatesOverlayVisibility: Boolean,
                            tileOverlayVisibilityChanged: (Boolean) -> Unit,
                            coordinatesOverlayVisibilityChanged: (Boolean) -> Unit,
                            trafficChanged: (Boolean) -> Unit,
                            gmsMapTypeChanged: (Boolean) -> Unit,
                            finished: (action: MenuAction) -> Unit) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = { finished(MenuAction.Nothing) }
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Outlined.Route, null)
            },
            text = { Text(text = stringResource(R.string.routes)) },
            onClick = {
                finished(MenuAction.ShowRouteMgr)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Outlined.AccountTree, null)
            },
            text = { Text(text = stringResource(R.string.route_region_list)) },
            onClick = {
                finished(MenuAction.ShowRegionList)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Outlined.LocationOn, null)
            },
            text = { Text(text = stringResource(R.string.poi_database)) },
            onClick = {
                finished(MenuAction.ShowPoiDatabase)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.AutoMirrored.Outlined.List, stringResource(R.string.rastermaps))
            },
            text = { Text(text = stringResource(R.string.rastermaps)) },
            onClick = {
                finished(MenuAction.ShowRasterMapsList)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Outlined.Settings, null)
            },
            text = { Text(text = stringResource(R.string.raster_map_type)) },
            onClick = {
                finished(MenuAction.ShowRasterMaptypePref)
            }
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.traffic), color = Black) },
            leadingIcon = { Icon(Icons.Outlined.Traffic, null) },
            trailingIcon = {
                Switch(
                    checked = mapProperties.isTrafficEnabled,
                    onCheckedChange = {
                        //uiSettings = uiSettings.copy(zoomControlsEnabled = it)
                        trafficChanged(it)
                    }
                )
            },
            onClick = {},
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.rastermaps_visibility), color = Black) },
            leadingIcon = { Icon(Icons.Outlined.Visibility, null) },
            trailingIcon = {
                Switch(
                    checked = tileOverlayVisibility,
                    onCheckedChange = {
                        //uiSettings = uiSettings.copy(zoomControlsEnabled = it)
                        tileOverlayVisibilityChanged(it)
                    }
                )
            },
            onClick = {},
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.coordinates_visibility), color = Black) },
            leadingIcon = { Icon(Icons.Outlined.Visibility, null) },
            trailingIcon = {
                Switch(
                    checked = coordinatesOverlayVisibility,
                    onCheckedChange = {
                        //uiSettings = uiSettings.copy(zoomControlsEnabled = it)
                        coordinatesOverlayVisibilityChanged(it)
                    }
                )
            },
            onClick = {},
        )
        if (!tileOverlayVisibility)
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.terrain_satellite), color = Black) },
                leadingIcon = { Icon(Icons.Outlined.Terrain, null) },
                trailingIcon = {
                    Switch(
                        checked = mapProperties.mapType == MapType.SATELLITE,
                        onCheckedChange = {
                            //uiSettings = uiSettings.copy(zoomControlsEnabled = it)
                            gmsMapTypeChanged(it)
                        }
                    )
                },
                onClick = {},
            )
    }
}

@Preview(showBackground = true, name = "Light Theme")
@Composable
fun DropdownMapMenuPreview() {
    RamaniTheme(darkTheme = false) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.LightGray)
                .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            DropdownMapMenu(
                mapProperties = MapProperties(isTrafficEnabled = true),
                tileOverlayVisibility = true,
                coordinatesOverlayVisibility = false,
                tileOverlayVisibilityChanged = {},
                coordinatesOverlayVisibilityChanged = {},
                trafficChanged = {},
                gmsMapTypeChanged = {},
                finished = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Dark Theme")
@Composable
fun DropdownMapMenuDarkPreview() {
    RamaniTheme(darkTheme = true) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.DarkGray)
                .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            DropdownMapMenu(
                mapProperties = MapProperties(),
                tileOverlayVisibility = true,
                coordinatesOverlayVisibility = true,
                tileOverlayVisibilityChanged = {},
                coordinatesOverlayVisibilityChanged = {},
                trafficChanged = {},
                gmsMapTypeChanged = {},
                finished = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Satellite Option Visible")
@Composable
fun DropdownMapMenuSatellitePreview() {
    RamaniTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.LightGray)
                .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            DropdownMapMenu(
                mapProperties = MapProperties(mapType = MapType.SATELLITE),
                tileOverlayVisibility = false,
                coordinatesOverlayVisibility = true,
                tileOverlayVisibilityChanged = {},
                coordinatesOverlayVisibilityChanged = {},
                trafficChanged = {},
                gmsMapTypeChanged = {},
                finished = {}
            )
        }
    }
}
