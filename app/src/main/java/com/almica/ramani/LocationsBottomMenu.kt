package com.almica.ramani

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.EditLocationAlt
import androidx.compose.material.icons.outlined.Monitor
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almica.ramani.Helpers.Companion.saveLocations
import com.almica.ramani.locationupdates.resetLocations
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber
import java.io.File

enum class LocationsAction {
    Close,
    Save,
    StartTime,
    Monitor,
    Reset,
    DeleteTracks,
    SnapShot,
    SaveAsRoute
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsBottomMenu(
    modifier: Modifier = Modifier,
    onAction: (String?, LocationsAction) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { onAction(null, LocationsAction.Close) },
        modifier = modifier
    ) {
        LocationsBottomMenuContent(onAction = onAction)
    }
}

@Composable
fun LocationsBottomMenuContent(
    modifier: Modifier = Modifier,
    onAction: (String?, LocationsAction) -> Unit
) {
    val context = LocalContext.current
    val saveLocationsText = stringResource(R.string.save_locations)
    val setLocationsStartTimeText = stringResource(R.string.set_locations_starttime)
    val resetLocationsText = stringResource(R.string.reset_locations)
    val removeAllTracksFeedbackText = stringResource(R.string.remove_all_tracks_feedback)
    val mapText = stringResource(R.string.map)
    val saveAsRouteText = stringResource(R.string.save_as_route)
    val locationsMonitorText = stringResource(R.string.locations_monitor)

    Column(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            MenuButton(
                icon = Icons.Outlined.SaveAlt,
                label = saveLocationsText,
                onClick = {
                    saveLocations(context, 0L) { feedback ->
                        Timber.i(feedback)
                        onAction(feedback, LocationsAction.Save)
                    }
                }
            )
            MenuButton(
                icon = Icons.Outlined.AccessTime,
                label = setLocationsStartTimeText,
                onClick = {
                    onAction(
                        setLocationsStartTimeText,
                        LocationsAction.StartTime
                    )
                }
            )
        }
        HorizontalDivider(thickness = 1.dp)
        Row(modifier = Modifier.fillMaxWidth()) {
            MenuButton(
                icon = Icons.Outlined.Restore,
                label = resetLocationsText,
                onClick = {
                    resetLocations(context) { feedback ->
                        Timber.i(feedback)
                        onAction(feedback, LocationsAction.Reset)
                    }
                }
            )
            MenuButton(
                icon = Icons.Outlined.DeleteForever,
                label = stringResource(R.string.all_tracks),
                onClick = {
                    val routesRootFolder = File(context.filesDir, Const.ROUTEFOLDER)
                    val trackFolder = File(routesRootFolder, Const.TRACKFOLDER)
                    val result = trackFolder.deleteRecursively()
                    Timber.i("Delete tracks result: $result")
                    onAction(
                        removeAllTracksFeedbackText,
                        LocationsAction.DeleteTracks
                    )
                }
            )
        }
        HorizontalDivider(thickness = 1.dp)
        Row(modifier = Modifier.fillMaxWidth()) {
            MenuButton(
                icon = Icons.Outlined.EditLocationAlt,
                label = mapText,
                onClick = {
                    onAction(mapText, LocationsAction.SnapShot)
                }
            )
            MenuButton(
                icon = Icons.Outlined.Save,
                label = saveAsRouteText,
                onClick = {
                    onAction(saveAsRouteText, LocationsAction.SaveAsRoute)
                }
            )
        }
        HorizontalDivider(thickness = 1.dp)
        Row(modifier = Modifier.fillMaxWidth()) {
            MenuButton(
                icon = Icons.Outlined.Monitor,
                label = locationsMonitorText,
                onClick = {
                    onAction(
                        locationsMonitorText,
                        LocationsAction.Monitor
                    )
                }
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun RowScope.MenuButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.weight(1f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LocationsBottomMenuPreview() {
    RamaniTheme {
        LocationsBottomMenuContent(
            onAction = { _, _ -> }
        )
    }
}
