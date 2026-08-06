package com.almica.ramani.navigation

import android.content.SharedPreferences
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.Const
import com.almica.ramani.RouteInfo
import com.almica.ramani.routes.ListRouteFoldersScreen
import com.almica.ramani.utils.DocumentViewer
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.routes.RouteDialogMode
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber
import java.io.File

sealed class NavItem(val route: String, var label: String, val icon: ImageVector) {
    object RouteFolders : NavItem(NavRoutes.ROUTE_FOLDERS_SCREEN, "Folders", Icons.Default.Folder)
    object DocumentView : NavItem(NavRoutes.DOCUMENT_VIEW, "PDF", Icons.Default.PictureAsPdf)
}

object NavRoutes {
    const val DOCUMENT_VIEW = "document_view"
    const val ROUTE_FOLDERS_SCREEN = "route_folders_screen"
}

@Composable
fun RamaniNavigationBar(navController: NavHostController) {
    val context = LocalContext.current
    var prefRouteFolder by remember {
        mutableStateOf(getDefaultSharedPreferences(context).getString(Const.PREF_ROUTEFOLDER_FILEPATH, null))
    }

    DisposableEffect(context) {
        val prefs = getDefaultSharedPreferences(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == Const.PREF_ROUTEFOLDER_FILEPATH) {
                prefRouteFolder = p.getString(key, null)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    RamaniNavigationBarContent(
        prefRouteFolder = prefRouteFolder,
        isSelected = { item -> currentDestination?.hierarchy?.any { it.route == item.route } == true },
        onItemClick = { item ->
            navController.navigate(item.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    )
}

@Composable
fun RamaniNavigationBarContent(
    prefRouteFolder: String?,
    isSelected: (NavItem) -> Boolean,
    onItemClick: (NavItem) -> Unit
) {
    val items = listOf(
        NavItem.RouteFolders,
        NavItem.DocumentView
    )
    prefRouteFolder?.let {
        items[1].label = "PDF: $it"
    }

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = isSelected(item),
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
fun RamaniApp(
    onDocumentViewerFinish: () -> Unit,
    onDocumentViewerResult: (RouteInfo) -> Unit,
    onRouteFolderSelected: (Triple<String, String, Int>) -> Unit,
    onRouteFolderFinished: (String?) -> Unit,
    onRouteSelected: (File) -> Unit,
    onRouteInfoSelected: (File) -> Unit,
    createSnapshots: (String?) -> Unit,
    dialogMode: Int
) {
    Timber.i("RamaniApp dialogMode: $dialogMode")
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { RamaniNavigationBar(navController) }
    ) { innerPadding ->
        RamaniNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            onDocumentViewerFinish = {
                navController.navigate(NavRoutes.ROUTE_FOLDERS_SCREEN) },
            onDocumentViewerResult = onDocumentViewerResult,
            onRouteFolderSelected = onRouteFolderSelected,
            onRouteFolderFinished = onRouteFolderFinished,
            onRouteSelected = onRouteSelected,
            onRouteInfoSelected = onRouteInfoSelected,
            createSnapshots = createSnapshots,
            dialogMode = dialogMode
        )
    }
}

@Composable
fun RamaniNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onDocumentViewerFinish: () -> Unit,
    onDocumentViewerResult: (RouteInfo) -> Unit,
    onRouteFolderSelected: (Triple<String, String, Int>) -> Unit,
    onRouteFolderFinished: (String?) -> Unit,
    onRouteSelected: (File) -> Unit,
    onRouteInfoSelected: (File) -> Unit,
    createSnapshots: (String?) -> Unit,
    dialogMode: Int
) {
    // Set up the result listener for the ROUTE_FOLDERS activity or any other destination
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            Timber.i("backStackEntry: ${backStackEntry.destination.route}")
            val savedStateHandle = backStackEntry.savedStateHandle
            // Check if the expected key "route_selection_result" exists in the SavedStateHandle
            val result = savedStateHandle.get<RouteInfo>("route_selection_result")
            if (result != null) {
                onDocumentViewerResult(result)
                // Clear the result after consuming it to prevent re-triggering on configuration changes
                savedStateHandle.remove<RouteInfo>("route_selection_result")
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.ROUTE_FOLDERS_SCREEN,
        modifier = modifier
    ) {
        composable(NavRoutes.ROUTE_FOLDERS_SCREEN) {
            ListRouteFoldersScreen(
                marginTopDp = 0f,
                selectRouteFolder = onRouteFolderSelected,
                finished = onRouteFolderFinished,
                route = onRouteSelected,
                routeInfo = onRouteInfoSelected,
                createSnapshots = createSnapshots,
                dialogMode = dialogMode
            )
        }
        composable(NavRoutes.DOCUMENT_VIEW) {
            DocumentViewer(
                finish = onDocumentViewerFinish,
                routeDataSelection = onDocumentViewerResult
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RamaniNavigationBarPreview() {
    RamaniTheme {
        RamaniNavigationBarContent(
            prefRouteFolder = "sample_folder",
            isSelected = { it == NavItem.RouteFolders },
            onItemClick = {}
        )
    }
}
