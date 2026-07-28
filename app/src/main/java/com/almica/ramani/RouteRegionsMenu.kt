package com.almica.ramani

import android.content.Context
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import com.almica.ramani.charts.theme.Black
import com.almica.ramani.utils.GeoJsonUtils.Companion.getRegionsFromRouteGeojson
import timber.log.Timber

/**
 * 28jun2026 not used anymore
 * replaced by region related geojson files
 * in Google Maps: RouteGeojsonList
 */
enum class RouteRegionsMenuAction {
    All,
    None,
    Region,
    Nothing
}
@Composable
fun RouteRegionsMenu(
    context: Context,
    routesRegionFilter: String,
    finished: (RouteRegionsMenuAction, String?) -> Unit
) {
    val regions = getRegionsFromRouteGeojson(context)
    Timber.i("routesRegionFilter: $routesRegionFilter")
    DropdownMenu(
        expanded = true,
        onDismissRequest = { finished(RouteRegionsMenuAction.Nothing, null) }
    ) {
        DropdownMenuItem(
            {
                Text(
                    text = if (routesRegionFilter == stringResource(R.string.all))
                        "${stringResource(R.string.all)} ${Const.UC_CHECKMARK}" else stringResource(R.string.all),
                    textDecoration = TextDecoration.Underline,
                    color = Black
                )
            },
            onClick = { finished(RouteRegionsMenuAction.All, null) }
        )
        DropdownMenuItem(
            {
                Text(
                    text = if (routesRegionFilter == stringResource(R.string.none))
                        "${stringResource(R.string.none)} ${Const.UC_CHECKMARK}" else stringResource(R.string.none),
                    textDecoration = TextDecoration.Underline,
                    color = Black
                )
            },
            onClick = { finished(RouteRegionsMenuAction.None, null) }
        )
        regions.forEach { region ->
            DropdownMenuItem(
                {
                    Text(text = if (routesRegionFilter == region)
                            "$region ${Const.UC_CHECKMARK}" else region,
                        color = Black)
                },
                onClick = { finished(RouteRegionsMenuAction.Region, region) },
            )
        }
    }
}
