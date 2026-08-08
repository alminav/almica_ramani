package com.almica.ramani

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ChangeHistory
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.geojsonMaps.GeojsonMakerActivity
import com.almica.ramani.geojsonMaps.MapsManagerActivity
import com.almica.ramani.geojsonMaps.MvtmakerActivity
import com.almica.ramani.geojsonMaps.TilemakerActivity
import com.almica.ramani.googlemaps.GoogleMapsActivity
import com.almica.ramani.googlemaps.GmsTileOverlayActivity
import com.almica.ramani.routes.RouteActivity
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.currentBackStackEntryAsState
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.format
import timber.log.Timber
import java.util.ArrayList
import kotlin.reflect.KClass

data class Activity(
    @StringRes val title: Int,
    @StringRes val description: Int,
    val kClass: KClass<out ComponentActivity>,
    val maptypeKey: MaptypeKey,
    val stringExtras: ArrayList<Pair<String, String>>,
    val doubleExtras: ArrayList<Pair<String, Double>>
)
sealed class ActivityGroup(
    @StringRes val title: Int,
    val icon: ImageVector,
    val activities: List<Activity>
) {
    object MapProvider : ActivityGroup(
        R.string.map_providers,
        Icons.Outlined.Map,
        listOf(
            Activity(
                R.string.map_provider_mablibre_mvt,
                R.string.map_provider_mablibre_mvt_description,
                MainActivity::class,
                MaptypeKey.Mvt,
                arrayListOf(),
                arrayListOf()
            ), Activity(
                R.string.map_provider_mablibre_raster,
                R.string.map_provider_mablibre_raster_description,
                MainActivity::class,
                MaptypeKey.Raster,
                arrayListOf(),
                arrayListOf()
            ), Activity(
                R.string.maplibre_geojson_description,
                R.string.map_provider_mablibre_geojson_description,
                MainActivity::class,
                MaptypeKey.GeoJson,
                arrayListOf(),
                arrayListOf()
            ), Activity(
                R.string.map_provider_mablibre,
                R.string.map_provider_mablibre_description,
                MainActivity::class,
                MaptypeKey.None,
                arrayListOf(),
                arrayListOf()
            ), Activity(
                R.string.map_provider_google_raster,
                R.string.map_provider_google_raster_description,
                GmsTileOverlayActivity::class,
                MaptypeKey.Raster,
                arrayListOf(),
                arrayListOf()
            ), Activity(
                R.string.map_provider_google,
                R.string.map_provider_google_description,
                GoogleMapsActivity::class,
                MaptypeKey.None,
                arrayListOf(),
                arrayListOf()
            )
        )
    )
    object MapAdministration : ActivityGroup(
        R.string.map_administration,
        Icons.Outlined.ChangeHistory,
        listOf(
            Activity(
                R.string.additional_maps,
                R.string.additional_maps_description,
                MapsManagerActivity::class,
                MaptypeKey.None,
                arrayListOf(),
                arrayListOf()
            ), Activity(
                R.string.routes,
                R.string.empty_string,
                RouteActivity::class,
                MaptypeKey.None,
                arrayListOf(),
                arrayListOf()
            ),
//            Activity(
//                R.string.places_search,
//                R.string.empty_string,
//                PlacesActivity::class,
//                MaptypeKey.None,
//                arrayListOf(),
//                arrayListOf()
//            ),
            Activity(
                R.string.gh_folders,
                R.string.empty_string,
                ListGhActivity::class,
                MaptypeKey.None,
                arrayListOf(),
                arrayListOf()
            ), Activity(
                R.string.route_folders,
                R.string.empty_string,
                ListRouteFoldersActivity::class,
                MaptypeKey.None,
                arrayListOf(),
                arrayListOf()
            ), Activity(
                R.string.srtm_files,
                R.string.empty_string,
                ListHgtActivity::class,
                MaptypeKey.None,
                arrayListOf(),
                arrayListOf()
            ), Activity(
                R.string.geojson_folders,
                R.string.empty_string,
                ListGeojsonActivity::class,
                MaptypeKey.None,
                arrayListOf(),
                arrayListOf()
            )
        )
    )

    object MapCreation : ActivityGroup(
        R.string.create_maps,
        Icons.Outlined.Create,
        listOf(
            Activity(
                R.string.tilemaker,
                R.string.empty_string,
                TilemakerActivity::class,
                MaptypeKey.None,
                arrayListOf(),
                arrayListOf()
            ), Activity(
                R.string.mvtmaker,
                R.string.empty_string,
                MvtmakerActivity::class,
                MaptypeKey.None,
                arrayListOf(),
                arrayListOf()
            ), Activity(
                R.string.geojson_maker_qgis,
                R.string.empty_string,
                GeojsonMakerActivity::class,
                MaptypeKey.None,
                arrayListOf(),
                arrayListOf()
            ), Activity(
                R.string.tilemaker_preferences_activity,
                R.string.empty_string,
                TilemakerPreferenceActivity::class,
                MaptypeKey.None,
                arrayListOf(),
                arrayListOf()
            )
        )
    )

    object MapImport : ActivityGroup(
        R.string.import_title,
        Icons.Outlined.ImportExport,
        listOf(
            Activity(
                R.string.import_title,
                R.string.mvt_mbtiles,
                FileImportActivity::class,
                MaptypeKey.None,
                arrayListOf(Pair(Const.EXTRA_FILETYPE, FileType.Mvt.name)),
                arrayListOf()
            ), Activity(
                R.string.import_title,
                R.string.raster_maps_mbtiles,
                FileImportActivity::class,
                MaptypeKey.None,
                arrayListOf(Pair(Const.EXTRA_FILETYPE, FileType.MbTiles.name)),
                arrayListOf()
            ), Activity(
                R.string.import_geojson_maps,
                R.string.empty_string,
                ImportGeojsonActivity::class,
                MaptypeKey.None,
                arrayListOf(),
                arrayListOf()
            ), Activity(
                R.string.import_title,
                R.string.gh_folder_archive,
                FileImportActivity::class,
                MaptypeKey.None,
                arrayListOf(Pair(Const.EXTRA_FILETYPE, FileType.GhFolderZip.name)),
                arrayListOf()
            ), Activity(
                R.string.import_title,
                R.string.srtm_file,
                FileImportActivity::class,
                MaptypeKey.None,
                arrayListOf(Pair(Const.EXTRA_FILETYPE, FileType.Hgt.name)),
                arrayListOf()
            ),  Activity(
                R.string.import_title,
                R.string.cycleway_overlays_mbtiles,
                FileImportActivity::class,
                MaptypeKey.None,
                arrayListOf(Pair(Const.EXTRA_FILETYPE, FileType.CycleWay.name)),
                arrayListOf()
            )
        )
    )
}
val allActivityGroups = listOf(
    ActivityGroup.MapProvider,
    ActivityGroup.MapAdministration,
    ActivityGroup.MapCreation,
    ActivityGroup.MapImport
)

@Composable
fun RamaniNavHost(
    mvtName: String?,
    rasterDescription: String?,
    geojsonDescription: String?,
    ghDescription: String?,
    geojsonFolderDescription: String?,
    routeFolderDescription: String?,
    firstLocationDate: String?,
    lastLocationDate: String?,
    lastLocationCoords: String?,
    logCount: Int,
    isTrackingEnabled: Boolean,
    onActivityClick: (
        KClass<out ComponentActivity>,
        kotlin.collections.ArrayList<Pair<String, String>>,
        kotlin.collections.ArrayList<Pair<String, Double>>,
        MaptypeKey
    ) -> Unit,
    showLocationsMenu: () -> Unit,
    onToggleTracking: (Boolean) -> Unit
) {
    val navController: NavHostController = rememberNavController()
    val items = listOf(
        ActivityGroup.MapProvider,
        ActivityGroup.MapAdministration,
        ActivityGroup.MapCreation,
        ActivityGroup.MapImport
    )
    val routes = listOf(
        "map_provider",
        "map_administration",
        "map_creation",
        "map_import"
    )

    val density = LocalDensity.current
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEachIndexed { index, group ->
                    val route = routes[index]
                    NavigationBarItem(
                        icon = { Icon(group.icon, contentDescription = stringResource(group.title)) },
                        label = {
                            Text(
                                text = stringResource(group.title),
                                maxLines = 1,
                                fontSize = with(density) { (MaterialTheme.typography.labelSmall.fontSize.value / fontScale).sp }
                            )
                        },
                        selected = currentRoute == route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            navController.navigate(route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                navController.graph.startDestinationRoute?.let { startRoute ->
                                    popUpTo(startRoute) {
                                        saveState = true
                                    }
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
    NavHost(
        navController = navController,
        startDestination = "map_provider",
        modifier = Modifier.padding(innerPadding)
    ) {
        composable("map_provider") {
            ActivityGroup.MapProvider.let { group ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Create a card for each activity in the group.
                    group.activities.forEach { activity ->
                        RamaniActivityItem(
                            onActivityClick, activity,
                            mvtName,
                            rasterDescription,
                            geojsonDescription,
                            ghDescription,
                            geojsonFolderDescription,
                            routeFolderDescription,
                            activity.maptypeKey
                        )
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                            lastLocationCoords?.let {
                                Text(
                                    text = "${stringResource(R.string.last_location)} $it",
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(bottom = 3.dp)
                                )
                            }

                            OutlinedButton(
                                onClick = { onToggleTracking(!isTrackingEnabled) },
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 8.dp),
                                colors = if (isTrackingEnabled) {
                                    ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                } else {
                                    ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                }
                            ) {
                                Icon(
                                    if (isTrackingEnabled) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(text = if (isTrackingEnabled) "Disable Logging" else "Enable Logging")
                            }

                            BadgedBox(modifier = Modifier.align(Alignment.CenterHorizontally), badge = { Badge { Text("$logCount") } }) {
                                Button(
                                    onClick = { showLocationsMenu() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(text = stringResource(R.string.tracking))
                                }
                            }
                            firstLocationDate?.let {
                                Text(
                                    text = "First: $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                            lastLocationDate?.let {
                                Text(
                                    text = "Last: $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }

                        }

                }
            }
        }
        composable("map_administration") {
            ActivityGroup.MapAdministration.let { group ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Create a card for each activity in the group.
                    group.activities.forEach { activity ->
                        RamaniActivityItem(
                            onActivityClick, activity,
                            mvtName,
                            rasterDescription,
                            geojsonDescription,
                            ghDescription,
                            geojsonFolderDescription,
                            routeFolderDescription,
                            activity.maptypeKey
                        )
                    }
                }
            }
        }
        composable("map_creation") {
            ActivityGroup.MapCreation.let { group ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Create a card for each activity in the group.
                    group.activities.forEach { activity ->
                        RamaniActivityItem(
                            onActivityClick, activity,
                            mvtName,
                            rasterDescription,
                            geojsonDescription,
                            ghDescription,
                            geojsonFolderDescription,
                            routeFolderDescription,
                            activity.maptypeKey
                        )
                    }
                }
            }
        }
        composable("map_import") {
            ActivityGroup.MapImport.let { group ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Create a card for each activity in the group.
                    group.activities.forEach { activity ->
                        RamaniActivityItem(
                            onActivityClick, activity,
                            mvtName,
                            rasterDescription,
                            geojsonDescription,
                            ghDescription,
                            geojsonFolderDescription,
                            routeFolderDescription,
                            activity.maptypeKey
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun RamaniActivityItem(
    onActivityClick: (
        KClass<out ComponentActivity>,
        ArrayList<Pair<String, String>>,
        ArrayList<Pair<String, Double>>,
        MaptypeKey
    ) -> Unit,
    activity: Activity,
    mvtName: String?,
    rasterDescription: String?,
    geojsonDescription: String?,
    ghDescription: String?,
    geojsonFolderDescription: String?,
    routeFolderDescription: String?,
    maptypeKey: MaptypeKey
) {
    if (!routeFolderDescription.isNullOrEmpty())
        Timber.i("routeFolderDescription: $routeFolderDescription")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                onActivityClick(
                    activity.kClass,
                    activity.stringExtras,
                    activity.doubleExtras,
                    maptypeKey
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(activity.title), textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            val descriptionText = when (maptypeKey) {
                MaptypeKey.GeoJson -> geojsonFolderDescription.takeIf { !it.isNullOrEmpty() } ?: geojsonDescription
                MaptypeKey.Raster -> rasterDescription
                MaptypeKey.Mvt -> mvtName
                else -> {
                    when (activity.kClass) {
                        ListGhActivity::class -> ghDescription
                        ListRouteFoldersActivity::class -> routeFolderDescription
                        ListGeojsonActivity::class -> geojsonFolderDescription
                        else -> stringResource(activity.description).takeIf { it.isNotEmpty() }
                    }
                }
            }

            descriptionText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RamaniListPreview() {
    RamaniTheme {
        RamaniNavHost(
            mvtName = "Sample MVT",
            rasterDescription = "Sample Raster Description",
            geojsonDescription = "Sample GeoJSON Description",
            ghDescription = "Sample GH Description",
            geojsonFolderDescription = "Sample GeoJSON Folder Description",
            routeFolderDescription = "home",
            firstLocationDate = "02/02/2026",
            lastLocationDate = "03/02/2026",
            lastLocationCoords = "lat: -1.2833° lon: 36.8167°",
            logCount = 999,
            isTrackingEnabled = true,
            onActivityClick = { _, _, _, _ -> },
            showLocationsMenu = {  },
            onToggleTracking = { }
        )
    }
}
