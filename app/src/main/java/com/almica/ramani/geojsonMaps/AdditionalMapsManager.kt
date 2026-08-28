package com.almica.ramani.geojsonMaps

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.Fitbit
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.almica.ramani.Const
import com.almica.ramani.ListCyclewayTilesScreen
import com.almica.ramani.ListMbTilesScreen
import com.almica.ramani.ListMvtScreen
import com.almica.ramani.OfflineMapCreatorScreen
import com.almica.ramani.R
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.BackPressHandler
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.MoboMessage
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import timber.log.Timber
import java.io.File

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AdditionalMapsManager(
    map: MapLibreMap?,
    initialPosition: LatLng?,
    newMvtName: (String) -> Unit,
    modifier: Modifier = Modifier,
    finish: (Boolean) -> Unit
) {
    val position = initialPosition ?: map?.cameraPosition?.target
    Timber.i("position: $position")
    var moboMessage: String? by remember { mutableStateOf(null) }
    val navController = rememberNavController()
    BackPressHandler {
        Timber.i("Back Press intercepted")
        finish(false)
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = { AdditionalBottomNavigationBar(navController) }
    ) { innerPadding ->
        moboMessage?.let { message ->
            MoboMessage(message) {
                moboMessage = null
            }
        }
        if (LocalInspectionMode.current) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Additional Maps Manager Preview")
            }
        } else {
            NavHost(
                navController = navController,
                startDestination = AdditionalManagerScreen.Mvt.rout,
                modifier = Modifier.padding(0.dp)
            ) {
                composable(route = AdditionalManagerScreen.Mvt.rout) {
                    ListMvtScreen(innerPadding, newMvtMap = { name ->
                        name?.let { p1 ->
                            newMvtName(p1)
                            Timber.i("newMvtMap: $p1")
                        }
                    }) { result -> finish(result) }
                }
                composable(route = AdditionalManagerScreen.RasterMaps.rout) {
                    ListMbTilesScreen(innerPadding) { result -> finish(result) }
                }
                composable(route = AdditionalManagerScreen.OfflineManager.rout) {
                    val context = LocalContext.current
                    val resources = LocalResources.current

                    val offlineInfo = remember(position) {
                        val dirMvtTiles = File(context.filesDir, Const.MVT_FOLDER)
                        val styleFile = File(dirMvtTiles, Const.MAPTILER_REMOTE_STYLE_FILENAME)
                        val localStyleUri = Uri.fromFile(styleFile).toString()

                        val geojsonTile: GeoJsonUtils.Companion.Tile =
                            if (position != null)
                                GeoJsonUtils.pointToTile(position.longitude, position.latitude, 10.0)
                            else GeoJsonUtils.pointToTile(0.0, 0.0, 10.0)

                        val bounds = GeoJsonUtils.tileToBoundsMaplibre(geojsonTile)
                        val regionName = "tile_${geojsonTile.x}_${geojsonTile.y}_${geojsonTile.z}"
                        val minZoom = 10.0
                        val maxZoom = 15.0
                        val pixelRatio = resources.displayMetrics.density

                        val definition = OfflineTilePyramidRegionDefinition(
                            localStyleUri,
                            bounds,
                            minZoom,
                            maxZoom,
                            pixelRatio
                        )
                        Triple(regionName, definition, bounds)
                    }

                    val (regionName, definition, bounds) = offlineInfo
                    Timber.i("$bounds")

                    var downloadActive by remember { mutableStateOf(false) }

                    OfflineMapCreatorScreen(
                        regionName,
                        definition,
                        downloadActive = { state, type ->
                            moboMessage = if (state)
                                resources.getString(
                                    R.string.create_offline_maps_started_, type, regionName
                                )
                            else
                                resources.getString(
                                    R.string.create_offline_maps_finished_, type, regionName
                                )
                            Timber.i("downloadActive: $downloadActive")
                            downloadActive = state
                        },
                        progress = { progress, text ->
                            Timber.i("$text $progress")
                        },
                        onDismiss = {
                            Timber.i("onDismiss")
                            downloadActive = false
                            finish(false)
                        })
                }

                composable(route = AdditionalManagerScreen.CycleWayOverlay.rout) {
                    ListCyclewayTilesScreen(innerPadding) { result -> finish(result) }
                }
                composable(route = AdditionalManagerScreen.GeojsonMapDatabase.rout) {
                    ListGeojsonDatabaseScreen(innerPadding, position) { result -> finish(result) }
                }
            }
        }
    }
}

@Composable
fun AdditionalBottomNavigationBar(
    navController: NavController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigationItems = listOf(
        AdditionalMapNavigationItem(
            title = stringResource(R.string.mvt),
            icon = Icons.Outlined.OfflinePin,
            rout = AdditionalManagerScreen.Mvt.rout
        ),
        AdditionalMapNavigationItem(
            title = stringResource(R.string.rastermaps),
            icon = Icons.Outlined.Fitbit,
            rout = AdditionalManagerScreen.RasterMaps.rout
        ),
        AdditionalMapNavigationItem(
            title = stringResource(R.string.create_maps),
            icon = Icons.Outlined.DownloadForOffline,
            rout = AdditionalManagerScreen.OfflineManager.rout
        ),
        AdditionalMapNavigationItem(
            title = stringResource(R.string.cycleways),
            icon = Icons.Default.PedalBike,
            rout = AdditionalManagerScreen.CycleWayOverlay.rout
        ),
        AdditionalMapNavigationItem(
            title = stringResource(R.string.geojson_maps),
            icon = Icons.Default.ShapeLine,
            rout = AdditionalManagerScreen.GeojsonMapDatabase.rout
        )
    )
    NavigationBar(
        containerColor = Color.White
    ) {
        navigationItems.forEach { item ->
            val isSelected = currentRoute == item.rout
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(item.rout) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(imageVector = item.icon, contentDescription = item.title)
                },
                label = {
                    Text(
                        item.title,
                        color = if (isSelected) Color.Black else Color.Gray,
                        textAlign = TextAlign.Center
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.surface,
                    indicatorColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

sealed class AdditionalManagerScreen(val rout: String) {
    data object Mvt : AdditionalManagerScreen(rout = "mvt")
    data object RasterMaps : AdditionalManagerScreen("rasterMaps")
    data object OfflineManager : AdditionalManagerScreen("offlineManager")
    data object CycleWayOverlay : AdditionalManagerScreen(rout = "cycleWays")
    data object GeojsonMapDatabase : AdditionalManagerScreen("geojson")
}

data class AdditionalMapNavigationItem(
    val title: String,
    val icon: ImageVector,
    val rout: String
)

@Preview(showBackground = true)
@Composable
fun AdditionalMapsManagerPreview() {
    RamaniTheme {
        AdditionalMapsManager(
            map = null,
            initialPosition = LatLng(0.0, 0.0),
            newMvtName = {},
            finish = {}
        )
    }
}
