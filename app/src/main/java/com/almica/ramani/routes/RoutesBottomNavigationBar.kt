package com.almica.ramani.routes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavController
import com.almica.ramani.R

/**
 * Author: Santosh Yadav
 * Created on: 16-11-2024 09:32
 */

@Composable
fun RoutesBottomNavigationBar(
    navController: NavController
) {
    val selectedNavigationIndex = rememberSaveable {
        mutableIntStateOf(0)
    }

    val navigationItems = listOf(
        RoutesNavigationItem(
            title = stringResource(R.string.files),
            icon = Icons.Default.Folder,
            route = RoutesManagerScreen.RouteFiles.rout
        ),
        RoutesNavigationItem(
            title = stringResource(R.string.geojson),
            icon = Icons.Outlined.Code,
            route = RoutesManagerScreen.RouteGeojson.rout
        ),
        RoutesNavigationItem(
            title = stringResource(R.string.database),
            icon = Icons.Default.Dataset,
            route = RoutesManagerScreen.RouteDatabase.rout
        ))

    NavigationBar(
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

data class RoutesNavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)