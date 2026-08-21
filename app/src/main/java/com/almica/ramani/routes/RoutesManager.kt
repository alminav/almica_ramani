package com.almica.ramani.routes

import android.annotation.SuppressLint
import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.routes.RouteMenu
import timber.log.Timber

private const val logtag = "RoutesManager"
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RoutesManager(
    location: Location,
    selectRoute: (RouteEntity?, RouteMenu) -> Unit
) {
    val navController = rememberNavController()
    //Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: ")
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { RoutesBottomNavigationBar(navController) }
    ) { _ ->
        val graph =
            navController.createGraph(startDestination = RoutesManagerScreen.RouteFiles.rout) {
                composable(route = RoutesManagerScreen.RouteFiles.rout) {
                    RouteFilesScreen(selectRoute = { routeEntity, routeMenu ->
                        Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: ${routeEntity?.name} $routeMenu")
                        selectRoute(routeEntity, routeMenu)
                    })
                }
                composable(route = RoutesManagerScreen.RouteGeojson.rout) {
                    RoutesGeojsonScreen()
                    { routeEntity, routeMenu ->
                        Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: ${routeEntity?.name} $routeMenu")
                        selectRoute(routeEntity, routeMenu)
                    }
                }
                composable(route = RoutesManagerScreen.RouteDatabase.rout) {
                    RouteDatabaseScreen(
                        com.google.android.gms.maps.model.LatLng(
                            location.latitude,
                            location.longitude
                        )
                    )
                    { routeEntity, routeDatabaseAction ->
                        Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: ${routeEntity?.name} $routeDatabaseAction")
                        //RoutesComposeContent(mapboxMap?.cameraPosition?.target) { route ->
                        if (routeEntity.isNotNull()) {
                            selectRoute(routeEntity, routeDatabaseAction)
                        } else {
                            selectRoute(null, routeDatabaseAction)
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
sealed class RoutesManagerScreen(val rout: String) {
    object RouteFiles: RoutesManagerScreen("Files")
    object RouteGeojson: RoutesManagerScreen("Geojson")
    object RouteDatabase: RoutesManagerScreen("Database")
}

@Preview(showBackground = true)
@Composable
fun RoutesManagerPreview() {
    RamaniTheme {
        RoutesManager(
            location = Location("preview").apply {
                latitude = 0.0
                longitude = 0.0
            },
            selectRoute = { _, _ -> }
        )
    }
}
