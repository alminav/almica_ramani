package com.almica.ramani.googlemaps

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Switch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.North
import androidx.compose.material.icons.outlined.TripOrigin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.almica.ramani.Const
import com.almica.ramani.R
import com.almica.ramani.charts.theme.Black
import com.almica.ramani.utils.GhHelper
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber

@Composable
internal fun GmsHairCrossMenu(context: Context, northUp: (Boolean) -> Unit, routingVehicle: () -> Unit,
                              ghFolder: () -> Unit, finished: (action: GmsHairCrossMenuAction) -> Unit) {
    DropdownMenu(context, northUp = {b -> northUp(b)}, routingVehicle = {routingVehicle()},
        ghFolder = {ghFolder()}) { action ->
        finished(action)
    }
}
enum class GmsHairCrossMenuAction {
    SetStopMarker,
    CalculateRoute,
    CalculateGmsOnlineRoute,
    CalculateGmsRoundTrip,
    Nothing
}
@Composable
private fun DropdownMenu(context: Context, northUp: (Boolean) -> Unit, routingVehicle: () -> Unit,
                         ghFolder: () -> Unit, finished: (action: GmsHairCrossMenuAction) -> Unit) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    var northup by remember { mutableStateOf(prefs.getBoolean(Const.PREF_GMS_NORTH_UP, true))}
    val currentVehicleCode = prefs.getString(context.getString(R.string.setting_locomotion), Const.DEFAULT_LOCOMOTION)
    val currentVehicleIcon = currentVehicleCode?.let { GhHelper.getVehicleIcon(context, it) }
    val ghFolderName = GhHelper.getGhFilename(context)

    GmsHairCrossMenuContent(
        northup = northup,
        currentVehicleIcon = currentVehicleIcon,
        ghFolderName = ghFolderName,
        onNorthUpChange = { isChecked ->
            northup = isChecked
            prefs.edit { putBoolean(Const.PREF_GMS_NORTH_UP, northup) }
            northUp(isChecked)
            Timber.i("northup = $northup")
        },
        onRoutingVehicle = routingVehicle,
        onGhFolder = ghFolder,
        onFinished = finished
    )
}

@Composable
private fun GmsHairCrossMenuContent(
    northup: Boolean,
    currentVehicleIcon: Int?,
    ghFolderName: String?,
    onNorthUpChange: (Boolean) -> Unit,
    onRoutingVehicle: () -> Unit,
    onGhFolder: () -> Unit,
    onFinished: (GmsHairCrossMenuAction) -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = { onFinished(GmsHairCrossMenuAction.Nothing) }
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Image(
                    painter = painterResource(R.drawable.circle_filled_red_24px),
                    modifier = Modifier.requiredSize(24.dp),
                    contentDescription = stringResource(R.string.stop_marker)
                )
            },
            text = { Text(text = stringResource(R.string.stop_marker)) },
            onClick = {
                onFinished(GmsHairCrossMenuAction.SetStopMarker)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Outlined.Navigation, null)
            },
            text = { Text(text = stringResource(R.string.route_calculation)) },
            onClick = {
                onFinished(GmsHairCrossMenuAction.CalculateRoute)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Outlined.Navigation, null)
            },
            text = { Text(text = stringResource(R.string.route_calculation_gms)) },
            onClick = {
                onFinished(GmsHairCrossMenuAction.CalculateGmsOnlineRoute)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Outlined.TripOrigin, null)
            },
            text = { Text(text = stringResource(R.string.roundtrip_gms)) },
            onClick = {
                onFinished(GmsHairCrossMenuAction.CalculateGmsRoundTrip)
            }
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.routing_vehicle), color = Black) },
            leadingIcon = {
                currentVehicleIcon?.let { if (it != -1) Icon(painterResource(it), null) }
            },
            onClick = {
                onRoutingVehicle()
            },
        )
        DropdownMenuItem(
            text = { Text(text = "${stringResource(R.string.gh_folders)}\n${ghFolderName ?: ""}", color = Black) },
            leadingIcon = {
                Icon(Icons.Outlined.Folder, null)
            },
            onClick = {
                onGhFolder()
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.map_orientation_north_up), color = Black) },
            leadingIcon = { Icon(Icons.Outlined.North, null) },
            trailingIcon = {
                Switch(
                    checked = northup,
                    onCheckedChange = onNorthUpChange
                )
            },
            onClick = {},
        )
/*
                Row(modifier = Modifier.align(alignment = Alignment.CenterVertically)) {
                    Icon(
                        Icons.Outlined.Navigation,
                        modifier = Modifier.weight(0.2f),
                        contentDescription = stringResource(R.string.gh_folders))
                    Text(
                        modifier = Modifier.weight(0.8f).align(alignment = Alignment.CenterVertically),
                        fontSize = 16.sp,
                        text = stringResource(R.string.gh_folders)
                    )
                }
 */
    }
}

@Preview(showBackground = true)
@Composable
private fun GmsHairCrossMenuPreview() {
    RamaniTheme {
        GmsHairCrossMenuContent(
            northup = true,
            currentVehicleIcon = R.drawable.ic_directions_car_black_24dp,
            ghFolderName = "sample_folder",
            onNorthUpChange = {},
            onRoutingVehicle = {},
            onGhFolder = {},
            onFinished = {}
        )
    }
}
