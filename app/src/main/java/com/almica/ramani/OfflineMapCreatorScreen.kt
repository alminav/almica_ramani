package com.almica.ramani

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almica.ramani.Const.MapType
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.geojsonMaps.GoogleMapViewInColumn
import com.almica.ramani.googlemaps.MaptypeMenu
import com.almica.ramani.googlemaps.NewMapAction
import com.almica.ramani.utils.BackPressHandler
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.MoboConfirmation
import com.almica.ramani.utils.formatLatLngShort
import com.almica.ramani.utils.getRegionName
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import timber.log.Timber
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun OfflineMapCreatorScreen(
    regionNameParm: String,
    offlineTilePyramidRegionDefinition: OfflineTilePyramidRegionDefinition,
    downloadActive: (Boolean, String?) -> Unit,
    progress: (Float, String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: OfflineMapCreatorViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (viewModel.regionName.isEmpty()) {
            viewModel.regionName = regionNameParm
        }
    }

    BackPressHandler {
        Timber.i("Back Press intercepted")
        onDismiss()
    }

    LaunchedEffect(viewModel.progressAnimation, viewModel.statusText) {
        progress(viewModel.progressAnimation, viewModel.statusText)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back home"
                        )
                    }
                },
                title = {
                    Text(text = stringResource(R.string.create_maps), fontSize = 16.sp)
                },
                actions = {
                    TextButton(onClick = { viewModel.showDropDownRasterMaptype = true }) {
                        Text(text = stringResource(R.string.maptype))
                    }
                }
            )
        }
    ) { padding ->
        if (viewModel.showDropDownRasterMaptype) {
            MaptypeMenu(context) { mapType ->
                viewModel.showDropDownRasterMaptype = false
                mapType?.let { viewModel.onMapTypeChanged(it) }
            }
        }

        Column(modifier = Modifier.padding(padding)) {
            OfflineMapCreatorContent(
                viewModel = viewModel,
                regionDefinition = offlineTilePyramidRegionDefinition,
                downloadActive = { active, type ->
                    downloadActive(active, type.name)
                }
            )

            AnimatedVisibility(visible = viewModel.progressAnimation > 0f && viewModel.progressAnimation < 1f) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(text = viewModel.statusText, modifier = Modifier.padding(start = 10.dp))
                    LinearProgressIndicator(
                        progress = viewModel.progressAnimation,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                    )
                }
            }
        }

        if (viewModel.tileLimitExceeded > 0) {
            AlertDialog(
                modifier = Modifier.fillMaxWidth(0.92f),
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = true,
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true
                ),
                shape = RoundedCornerShape(20.dp),
                onDismissRequest = { viewModel.tileLimitExceeded = 0 },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { viewModel.tileLimitExceeded = 0 }) {
                        Text(text = stringResource(R.string.uc_close))
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.offline_manager),
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.limit_exceeded, viewModel.tileLimitExceeded),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    }
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun OfflineMapCreatorContent(
    viewModel: OfflineMapCreatorViewModel,
    regionDefinition: OfflineTilePyramidRegionDefinition,
    downloadActive: (Boolean, MapType) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val clipboardManager = LocalClipboard.current

    var clipText: String? by remember { mutableStateOf(null) }
    var moboDeleteConfirmation: String? by remember { mutableStateOf(null) }
    var isGmsMapLoaded by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf(regionDefinition.bounds) }

    LaunchedEffect(clipText) {
        clipText?.let {
            val clipData = ClipData.newPlainText(NewMapAction.Import.name, it)
            clipboardManager.setClipEntry(ClipEntry(clipData))
            clipText = null
        }
    }

    if (viewModel.showOfflineRegions) {
        DropdownMenuOfflineRegions(context, viewModel.offlineRegionsMap?.values) { _, name ->
            name?.let { viewModel.regionName = it }
            viewModel.showOfflineRegions = false
        }
    }

    if (viewModel.showRasterRegions) {
        ListRasterDriveEntries(
            onDismissRequest = { viewModel.showRasterRegions = false },
            import = {
                FileImportActivity.launch(context, FileType.MbTiles)
            }
        ) { rasterItemModel ->
            viewModel.regionName = rasterItemModel.name.replace(Const.MBTILES_EXT, "")
            viewModel.showRasterRegions = false
        }
    }

    moboDeleteConfirmation?.let { mode ->
        MoboConfirmation(resources.getString(R.string.delete_confirmation_question, viewModel.regionName)) { confirmed ->
            if (confirmed) {
                if (mode == "0") {
                    viewModel.deleteOfflineRegion(viewModel.regionName) { }
                } else {
                    val rootFolder = context.filesDir
                    val mbTilesRootFolder = File(rootFolder, Const.MBTILES_FOLDER)
                    val splits = viewModel.regionName.split(Const.UNDERLINE, limit = 5)
                    val checkName = if (splits.size == 5)
                        "${viewModel.regionName}${Const.MBTILES_EXT}" else "${viewModel.regionName}_${viewModel.mapType}${Const.MBTILES_EXT}"
                    File(mbTilesRootFolder, checkName).delete()
                    viewModel.refreshRasterRegionNames()
                    GeoJsonUtils.createRasterMapsBounds(context) { }
                }
            }
            moboDeleteConfirmation = null
        }
    }

    Box(modifier = Modifier.padding(start = 5.dp, top = 20.dp)) {
        Column {
            Card(
                modifier = Modifier.fillMaxWidth().padding(3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(
                            text = "Center N: ${bounds?.center?.latitude?.formatLatLngShort()}° " +
                                    "W: ${bounds?.center?.longitude?.formatLatLngShort()}°",
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Row {
                        listOf(
                            "N:${bounds?.latitudeNorth?.formatLatLngShort()}",
                            "S:${bounds?.latitudeSouth?.formatLatLngShort()}",
                            "W:${bounds?.longitudeWest?.formatLatLngShort()}",
                            "E:${bounds?.longitudeEast?.formatLatLngShort()}"
                        ).forEach {
                            Text(text = it, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = viewModel.regionName,
                    onValueChange = { viewModel.regionName = it },
                    label = { Text(stringResource(R.string.region_name)) }
                )
                TextButton(onClick = {
                    clipText = viewModel.regionName
                    FileImportActivity.launch(context, FileType.MbTiles)
                }) {
                    Text(text = stringResource(R.string.import_title))
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            // Vector Section
            Card(
                modifier = Modifier.fillMaxWidth().padding(3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(R.string.vector), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    if (viewModel.regionName.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.startVectorDownload(regionDefinition, downloadActive) },
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Text(text = stringResource(R.string.create), fontSize = 13.sp)
                        }
                    }
                    TextButton(onClick = { viewModel.showOfflineRegions = true }) {
                        Text(text = stringResource(R.string.list_offline_regions), fontSize = 13.sp)
                    }
                    val splits = viewModel.regionName.split(Const.UNDERLINE, limit = 5)
                    if (splits.size >= 4) {
                        val checkName = "${splits[0]}_${splits[1]}_${splits[2]}_${splits[3]}"
                        if (viewModel.offlineRegionsMap?.containsKey(checkName) == true) {
                            IconButton(onClick = { moboDeleteConfirmation = "0" }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete Region")
                            }
                        }
                    }
                }
            }

            // Raster Section
            Card(
                modifier = Modifier.fillMaxWidth().padding(3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${stringResource(R.string.raster)} ${viewModel.mapType}", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    if (viewModel.regionName.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                bounds?.let { b ->
                                    viewModel.startRasterDownload(b, downloadActive) {
                                        GeoJsonUtils.createRasterMapsBounds(context) { }
                                    }
                                }
                            },
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Text(text = stringResource(R.string.create), fontSize = 13.sp)
                        }
                    }
                    TextButton(onClick = { viewModel.showRasterRegions = true }) {
                        Text(text = stringResource(R.string.raster_drive_content), fontSize = 13.sp)
                    }
                    val splits = viewModel.regionName.split(Const.UNDERLINE, limit = 5)
                    val checkName = if (splits.size == 5)
                        "${viewModel.regionName}${Const.MBTILES_EXT}" else "${viewModel.regionName}_${viewModel.mapType}${Const.MBTILES_EXT}"
                    if (viewModel.rasterRegionNames.contains(checkName)) {
                        IconButton(onClick = { moboDeleteConfirmation = "1" }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete Region")
                        }
                    }
                }
            }

            if (viewModel.progressAnimation > 0 && viewModel.progressAnimation < 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material.TextButton(onClick = { viewModel.cancelDownloads(downloadActive) }) {
                        Text(Const.UC_CLOSE, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                }
            }

            // Map Section
            val splits = viewModel.regionName.split(Const.UNDERLINE, limit = 5)
            if (splits.size >= 4) {
                val tile = GeoJsonUtils.Companion.Tile(splits[1].toInt(), splits[2].toInt(), splits[3].toInt())
                var cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(LatLng(bounds?.center?.latitude ?: 0.0, bounds?.center?.longitude ?: 0.0), tile.z.toFloat())
                }
                val tileCenterLatLng = LatLng(bounds?.center?.latitude ?: 0.0, bounds?.center?.longitude ?: 0.0)
                val tileCenterLatLngState = rememberUpdatedMarkerState(position = tileCenterLatLng)

                Box(contentAlignment = Alignment.Center, modifier = Modifier.align(Alignment.CenterHorizontally).size(320.dp)) {
                    GoogleMapViewInColumn(
                        modifier = Modifier.fillMaxSize().testTag("Map").pointerInteropFilter { it.action == MotionEvent.ACTION_DOWN },
                        cameraPositionState = cameraPositionState,
                        tileCenterLatLngState = tileCenterLatLngState,
                        tile.x, tile.y, tile.z,
                        onMapLoaded = { isGmsMapLoaded = true },
                        onMapClick = { latLng ->
                            val tile_ = GeoJsonUtils.pointToTile(latLng.longitude, latLng.latitude, tile.z.toDouble())
                            bounds = GeoJsonUtils.tileToBoundsMaplibre(tile_)
                            viewModel.regionName = "${Const.TILE_PREFIX}${tile_.x}_${tile_.y}_${tile_.z}"
                            cameraPositionState = CameraPositionState(position = CameraPosition.fromLatLngZoom(LatLng(bounds?.center?.latitude ?: 0.0, bounds?.center?.longitude ?: 0.0), tile.z.toFloat()))
                        },
                        onMarkerClick = { }
                    )
                    if (!isGmsMapLoaded) {
                        CircularProgressIndicator(modifier = Modifier.background(MaterialTheme.colorScheme.background).wrapContentSize())
                    }
                }
            }
        }
    }
}

@Composable
fun DropdownMenuOfflineRegions(
    context: Context,
    offlineRegions: Collection<OfflineRegion>?,
    finish: (OfflineRegion?, String?) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        DropdownMenu(
            expanded = !offlineRegions.isNullOrEmpty(),
            onDismissRequest = { finish(null, null) }
        ) {
            val regionNames = mutableSetOf<String>()
            offlineRegions?.forEach { region ->
                val name = getRegionName(region)
                if (regionNames.add(name)) {
                    DropdownMenuItem(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        text = { Text(text = name, color = Color.Black) },
                        onClick = { finish(region, name) }
                    )
                }
            }
        }
    }
}
