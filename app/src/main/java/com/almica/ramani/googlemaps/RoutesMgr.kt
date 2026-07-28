package com.almica.ramani.googlemaps

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import com.almica.ramani.R
import com.almica.ramani.routes.RouteDatabaseScreen
import com.almica.ramani.routes.RouteEntity
import com.almica.ramani.routes.RouteFilesScreen
import com.almica.ramani.routes.RoutesGeojsonScreen
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.ui.theme.RamaniTheme
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber

//private const val logtag = "RoutesMgr"
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RoutesMgr(
    latLng: LatLng,
    selectRouteEntity: (RouteEntity?) -> Unit,
    selectRouteGeojson: (RouteEntity?) -> Unit
) {
    val navController = rememberNavController()
    //Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: ")
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { RoutesNavigationBar(navController) }
    ) { _ ->
        Column {
            val graph =
                navController.createGraph(startDestination = RoutesMgrScreen.RouteFiles.rout) {
                    composable(route = RoutesMgrScreen.RouteFiles.rout) {
                        RouteFilesScreen()
                        { routeEntity, routeMenu ->
                            Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: ${routeEntity?.name} $routeMenu")
                            selectRouteEntity(routeEntity)
                        }
                    }
                    composable(route = RoutesMgrScreen.RouteGeojson.rout) {
                        RoutesGeojsonScreen()
                        { routeEntity, routeMenu ->
                            Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: ${routeEntity?.name} $routeMenu")
                            selectRouteGeojson(routeEntity)
                        }
                    }
                    composable(route = RoutesMgrScreen.RouteDatabase.rout) {
                        RouteDatabaseScreen(
                            latLng
                        )
                        { routeEntity, routeDatabaseAction ->
                            Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: ${routeEntity?.name} $routeDatabaseAction")
                            //RoutesComposeContent(mapboxMap?.cameraPosition?.target) { route ->
                            if (routeEntity.isNotNull()) {
                                selectRouteEntity(routeEntity)
                            } else {
                                selectRouteEntity(null)
                                Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: routeEntity=null")
                            }
                        }
                    }
                }

            NavHost(
                navController = navController,
                graph = graph,
                modifier = Modifier.padding(0.dp)
            )
        }
    }
}
sealed class RoutesMgrScreen(val rout: String) {
    object RouteFiles: RoutesMgrScreen("Files")
    object RouteGeojson: RoutesMgrScreen("Geojson")
    object RouteDatabase: RoutesMgrScreen("Database")
}
fun Any?.isNotNull() = this != null
@Composable
private fun RoutesNavigationBar(
    navController: NavController
) {
    val selectedNavigationIndex = rememberSaveable {
        mutableIntStateOf(0)
    }

    val navigationItems = listOf(
        RoutesNavigationItem(
            title = stringResource(R.string.files),
            icon = Icons.Default.Folder,
            route = RoutesMgrScreen.RouteFiles.rout
        ),
        RoutesNavigationItem(
            title = stringResource(R.string.geojson),
            icon = Icons.Outlined.Code,
            route = RoutesMgrScreen.RouteGeojson.rout
        ),
        RoutesNavigationItem(
            title = stringResource(R.string.database),
            icon = Icons.Default.Dataset,
            route = RoutesMgrScreen.RouteDatabase.rout
        ))

    Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: density: ${LocalDensity.current.density}")
    NavigationBar(//modifier = Modifier.height(Const.DP52.times(LocalDensity.current.density)),
        containerColor = Color.White
    ) {
        navigationItems.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedNavigationIndex.intValue == index,
                onClick = {
                    selectedNavigationIndex.intValue = index
                    navController.navigate(item.route)
                },
                icon = {
                    Icon(imageVector = item.icon, contentDescription = item.title)
                },
                label = {
                    Text(
                        item.title,
                        color = if (index == selectedNavigationIndex.intValue)
                            Color.Black
                        else Color.Gray
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

private data class RoutesNavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Preview(showBackground = true)
@Composable
fun RoutesMgrPreview() {
    RamaniTheme {
        RoutesMgr(
            latLng = LatLng(0.0, 0.0),
            selectRouteEntity = {},
            selectRouteGeojson = {}
        )
    }
}