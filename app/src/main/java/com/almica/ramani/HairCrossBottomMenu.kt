package com.almica.ramani

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.LocationSearching
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.SpaceBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.almica.ramani.Helpers.Companion.getTileName
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.GhHelper
import com.almica.ramani.utils.HgtReader
import com.almica.ramani.utils.format
import com.almica.ramani.utils.isNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import timber.log.Timber
import java.io.File

private const val logtag = "HairCrossBottomMenu"

enum class HairCrossAction {
    Close,
    AddPoi,
    AddStop,
    RemoveStop,
    Calc,
    OrsCalc,
    OrsRoundtrip,
    RoutingVehicle,
    MapFeatures,
    GhFolder,
    GeoCoder,
    NearestPoi
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HairCrossBottomMenu(
    latLng: LatLng?,
    stopPosition: LatLng?,
    callback: (HairCrossAction) -> Unit
) {
    ModalBottomSheet(onDismissRequest = { callback(HairCrossAction.Close) }) {
        HairCrossBottomMenuContent(
            latLng = latLng,
            stopPosition = stopPosition,
            callback = callback
        )
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun HairCrossBottomMenuContent(
    latLng: LatLng?,
    stopPosition: LatLng?,
    callback: (HairCrossAction) -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    val headLine by produceState(initialValue = "", latLng, isPreview) {
        if (latLng == null) {
            value = ""
            return@produceState
        }

        var hl = "lat: ${latLng.latitude.format(4)}° lon: ${latLng.longitude.format(4)}°"
        if (!isPreview) {
            withContext(Dispatchers.IO) {
                val tileName = getTileName(latLng.latitude, latLng.longitude).uppercase()
                val demFolder = File(context.filesDir, Const.HGT_FOLDER_NAME)
                val hgtFile = File(demFolder, tileName + Const.HGT_EXT)
                if (hgtFile.exists()) {
                    try {
                        val hgtReader = HgtReader(context, hgtFile)
                        val hRefreshed = hgtReader.getElevationFromHgt(
                            com.google.android.gms.maps.model.LatLng(latLng.latitude, latLng.longitude)
                        )
                        hl += " ${Const.UC_ELE_ARROW}${hRefreshed.format(0)}m"
                    } catch (e: Exception) {
                        Timber.e(e)
                        // Fallback if elevation reading fails
                    }
                }
            }
        }
        value = hl
    }

    val currentId = remember(context, isPreview) {
        if (isPreview) {
            -1
        } else {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val currentCode = preferences.getString(
                context.getString(R.string.setting_locomotion),
                Const.DEFAULT_LOCOMOTION
            )
            currentCode?.let { GhHelper.getVehicleIcon(context, it) }
        }
    }

    val ghFolder = remember(context, isPreview) {
        if (isPreview) {
            "(preview)"
        } else {
            "(${GhHelper.getGhFilename(context)})"
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
            .fillMaxWidth()
    ) {
        if (latLng != null) {
            Text(
                text = headLine,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
        val buttonModifier = Modifier
            .weight(0.5f)
            .height(48.dp)

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            HairCrossMenuItem(
                modifier = buttonModifier,
                text = stringResource(R.string.routing_vehicle),
                iconPainter = currentId?.takeIf { it != -1 }?.let { painterResource(it) },
                onClick = { callback(HairCrossAction.RoutingVehicle) }
            )
            HairCrossMenuItem(
                modifier = buttonModifier,
                text = stringResource(R.string.route_calculation),
                iconVector = Icons.Outlined.Calculate,
                onClick = { callback(HairCrossAction.Calc) },
                enabled = stopPosition.isNotNull()
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            HairCrossMenuItem(
                modifier = buttonModifier,
                text = stringResource(R.string.stop_marker),
                iconPainter = painterResource(R.drawable.circle_filled_red_24px),
                iconTint = Color.Unspecified,
                onClick = { callback(HairCrossAction.AddStop) }
            )
            HairCrossMenuItem(
                modifier = buttonModifier,
                text = stringResource(R.string.remove_stop_marker),
                iconPainter = painterResource(R.drawable.remove_stop_marker_24),
                iconTint = Color.Unspecified,
                onClick = { callback(HairCrossAction.RemoveStop) },
                enabled = stopPosition.isNotNull()
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            HairCrossMenuItem(
                modifier = buttonModifier,
                text = stringResource(R.string.placemark),
                iconVector = Icons.Outlined.Add,
                onClick = { callback(HairCrossAction.AddPoi) }
            )
            HairCrossMenuItem(
                modifier = buttonModifier,
                text = stringResource(R.string.geocoder),
                iconVector = Icons.Outlined.LocationSearching,
                onClick = { callback(HairCrossAction.GeoCoder) }
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            HairCrossMenuItem(
                modifier = buttonModifier,
                text = stringResource(R.string.ors_roundtrip),
                iconVector = Icons.Outlined.SpaceBar,
                onClick = { callback(HairCrossAction.OrsRoundtrip) }
            )
            HairCrossMenuItem(
                modifier = buttonModifier,
                iconVector = Icons.Outlined.SpaceBar,
                text = stringResource(R.string.ors_route_calculation),
                onClick = { callback(HairCrossAction.OrsCalc) }
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            HairCrossMenuItem(
                modifier = buttonModifier,
                text = stringResource(R.string.gh_folder),
                secondaryText = ghFolder,
                iconVector = Icons.Outlined.Navigation,
                onClick = { callback(HairCrossAction.GhFolder) }
            )
            HairCrossMenuItem(
                modifier = buttonModifier,
                text = stringResource(R.string.map_features),
                iconVector = Icons.Outlined.Category,
                onClick = { callback(HairCrossAction.MapFeatures) }
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            HairCrossMenuItem(
                modifier = buttonModifier,
                text = stringResource(R.string.nearest_poi),
                iconVector = Icons.Outlined.NearMe,
                onClick = { callback(HairCrossAction.NearestPoi) }
            )
            Box(modifier = Modifier.weight(0.5f))
        }
    }
}


@Composable
private fun HairCrossMenuItem(
    modifier: Modifier = Modifier,
    text: String,
    secondaryText: String? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    iconVector: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(3.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.LightGray, // surfaceVariant might be too dark/different, keeping similar to original for now but using MaterialTheme if possible
            contentColor = Color.Blue // original content color
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            val iconModifier = Modifier.weight(0.2f)
            if (iconPainter != null) {
                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = iconTint
                )
            } else if (iconVector != null) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = iconTint
                )
            }

            Column(
                modifier = Modifier.weight(0.8f),
                horizontalAlignment = if (iconPainter == null && iconVector == null) Alignment.CenterHorizontally else Alignment.Start
            ) {
                Text(
                    text = text,
                    fontSize = 14.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                if (secondaryText != null) {
                    Text(
                        text = secondaryText,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HairCrossBottomMenuPreview() {
    RamaniTheme {
        HairCrossBottomMenuContent(
            latLng = LatLng(48.8584, 2.2945),
            stopPosition = null,
            callback = {}
        )
    }
}
