package com.almica.ramani

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.preference.PreferenceManager
import com.almica.ramani.Const.MapType
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.geojsonMaps.GoogleMapViewInColumn
import com.almica.ramani.googlemaps.MaptypeMenu
import com.almica.ramani.googlemaps.NewMapAction
import com.almica.ramani.tilemaker.MbtilesCreator
import com.almica.ramani.tilemaker.MbtilesDatabase
import com.almica.ramani.utils.BackPressHandler
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.MoboConfirmation
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import me.ibrahimsn.library.LiveSharedPreferences
import org.json.JSONObject
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegion.OfflineRegionDeleteCallback
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import timber.log.Timber
import java.io.File
import java.io.FileFilter
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "LocalContextGetResourceValueCall")
@Composable
fun OfflineMapCreatorScreen(regionNameParm: String, offlineTilePyramidRegionDefinition: OfflineTilePyramidRegionDefinition,
                            downloadActive: (Boolean, String?) -> Unit,
                            progress: (Float, String) -> Unit,
                            onDismiss: () -> Unit) {
    //val marginTopDp = TopAppBarDefaults.TopAppBarExpandedHeight.value
    var regionName by remember { mutableStateOf(regionNameParm) }
    var tileLimitExceeded by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val liveSharedPreferences = LiveSharedPreferences(preferences)
    var tilemakerUrl by remember { mutableStateOf(preferences.getString(context.getString(R.string.pref_tilemaker_url),
        Const.URL_PHONEMAPS))}
    var tilemakerMaxZoom = 14
    if (preferences.contains(context.getString(R.string.pref_tilemaker_maxzoom)))
        tilemakerMaxZoom = preferences.getString(context.getString(R.string.pref_tilemaker_maxzoom), "14")?.toInt() ?: 14
    var tilemakerMinZoom = 8
    if (preferences.contains(context.getString(R.string.pref_tilemaker_minzoom)))
        tilemakerMinZoom = preferences.getString(context.getString(R.string.pref_tilemaker_minzoom), "14")?.toInt() ?: 14
    var mapType by remember { mutableStateOf(preferences.
        getString(context.getString(R.string.pref_tilemaker_maptype), Const.OUTDOOR))}
    liveSharedPreferences.getString(context.getString(R.string.pref_tilemaker_maptype), Const.OUTDOOR)
        .observe(LocalLifecycleOwner.current) { value ->
        if (value != null)
            mapType = value
        Timber.i( "mapType: $value")
    }
    var tileLimit: Long? = 6000L
    if (preferences.contains(context.getString(R.string.pref_OfflineMapboxTileCountLimit))) {
        val limitString = preferences.getString(context.getString(R.string.pref_OfflineMapboxTileCountLimit), "6000")
        tileLimit = limitString?.toLong()
    }
    var statusText by remember { mutableStateOf("") }
    var progressAnimation by remember { mutableFloatStateOf(0f) }
    var showDropDownRasterMaptype by remember { mutableStateOf(false) }

//    Timber.i( "" +
//            "tileLimit: $tileLimit tilemakerMaxZoom: $tilemakerMaxZoom tilemakerUrl: $tilemakerUrl" )
    BackPressHandler {
        Timber.i( "Back Press intercepted")
        onDismiss()
    }
    Scaffold(modifier = Modifier.padding(top = 0.dp),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { onDismiss() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back home"
                        )
                    }
                }, title = {
                    Text(text = stringResource(R.string.create_maps), fontSize = 16.sp)
                }, actions = {
                        TextButton(
                            onClick = {
                                showDropDownRasterMaptype = true
                            }
                        ) {
                            Text(text = stringResource(R.string.maptype))
                        }
                }
            )
        }
    ) { _ ->
        if (showDropDownRasterMaptype) {
            MaptypeMenu(context) {maptype_ ->
                showDropDownRasterMaptype = false
                maptype_?.let {
                    mapType = maptype_
                    tilemakerUrl =
                        when (mapType) {
                            Const.PHONEMAPS -> Const.URL_PHONEMAPS
                            Const.OPENTOPO -> Const.URL_OPENTOPO
                            Const.OUTDOOR -> Const.URL_OUTDOOR
                            Const.THUNDERFOREST -> Const.URL_THUNDERFOREST
                            else -> Const.URL_PHONEMAPS
                        } as String?
                }
            }
        }
        Column {
            OfflineMapCreatorContent(regionName,
                tilemakerUrl, mapType, tilemakerMaxZoom,
                tilemakerMinZoom, tileLimit,
                Modifier.padding(0.dp), offlineTilePyramidRegionDefinition,
                mapboxTileCountLimitExceeded = { limit ->
                    tileLimitExceeded = limit
                    progressAnimation = 0f
                }, progress = { progressPercent, progressText ->
                    tilemakerUrl = null
                    progressAnimation = progressPercent
                    statusText = progressText
                    progress(progressPercent, progressText)
                }, downloadActive = {state, downloadMapType ->
                    Timber.i( "downloadActive: $downloadActive ${downloadMapType.name}")
                    downloadActive(state, downloadMapType.name)
                }
            )
            AnimatedVisibility(visible = progressAnimation > 0f && progressAnimation < 1f) {
                Column {
                    Text(text = statusText, modifier = Modifier.padding(start = 10.dp))
                    LinearProgressIndicator(
                        progress = progressAnimation,
                        modifier = Modifier
                            .padding(start = 10.dp, end = 10.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                    )
                }
            }
        }

        if (tileLimitExceeded > 0) {
            progressAnimation = 0f
            AlertDialog(
                modifier = Modifier.fillMaxWidth(0.92f),
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = true,
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true
                ),
                shape = RoundedCornerShape(20.dp),
                onDismissRequest = {
                }, confirmButton = {
                }, dismissButton = {
                    TextButton(onClick = {tileLimitExceeded = 0 }) {
                        Text(text = stringResource(R.string.uc_close))
                    }
                }, title = {
                    Text(
                    text = stringResource(R.string.offline_manager),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())},
                text = {Text(
                    text = stringResource(R.string.limit_exceeded, tileLimitExceeded),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()) },

            )
        }
    }
}
@SuppressLint("MutableCollectionMutableState")
@Composable
fun OfflineMapCreatorContent(
    name: String,
    tilemakerUrl_: String?,
    mapType: String?,
    tilemakerMaxZoom: Int,
    tilemakerMinZoom: Int,
    tileLimit: Long?,
    modifier: Modifier,
    regionDefinition: OfflineTilePyramidRegionDefinition,
    mapboxTileCountLimitExceeded: (Long) -> Unit,
    progress: (Float, String) -> Unit,
    downloadActive: (Boolean, MapType) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current

    var tilemakerUrl = tilemakerUrl_
    Timber.i("tilemakerUrl: $tilemakerUrl")
    var statusText by remember { mutableStateOf("") }
    var progressAnimation by remember { mutableFloatStateOf(0f) }
    var showOfflineRegions by remember { mutableStateOf(false) }
    var showRasterRegions by remember { mutableStateOf(false) }
    var offlineRegionsMap by remember { mutableStateOf<HashMap<String, OfflineRegion>?>(null) }
    //    val coroutineScope = rememberCoroutineScope()
    var regionName by remember { mutableStateOf(name) }
    var downloadMode by remember { mutableLongStateOf(0L) }
    var tilemakerMode by remember { mutableStateOf(false) }
    var rasterRegionNames by remember { mutableStateOf(getRasterRegionNames(context)) }
    if (showOfflineRegions)
        DropdownMenuOfflineRegions(context, offlineRegionsMap?.values) { offlineRegion, name ->
            if (name != null && offlineRegion != null) {
                regionName = name
            }
            showOfflineRegions = false
    }
    val clipboardManager = LocalClipboard.current
    var clipText: String? by remember { mutableStateOf(null) }
    LaunchedEffect(clipText) {
        if (!clipText.isNullOrEmpty()) {
            Timber.i("clipText: $clipText")
            val clipData = ClipData.newPlainText( NewMapAction.Import.name, clipText)
            val clipEntry = ClipEntry(clipData)
            clipboardManager.setClipEntry(clipEntry)
            clipText = null
        }
    }

    var moboDeleteConfirmation: String? by remember { mutableStateOf(null) }
    moboDeleteConfirmation?.let {
        MoboConfirmation(LocalResources.current.getString(R.string.delete_confirmation_question, regionName)) { result ->
            if (result) {
                if (moboDeleteConfirmation == "0") {
                    moboDeleteConfirmation = null
                    Timber.i("delete regionName: $regionName")
                    val selectedRegion = offlineRegionsMap?.getValue(regionName)
                    invalidateOfflineRegion(selectedRegion) { success ->
                        if (success)
                            deleteOfflineRegion(selectedRegion) { success ->
                                val folderThumbnails = File(
                                    context.filesDir,
                                    Const.THUMBNAILS
                                )
                                val thumbnailFilename =
                                    Const.MBGL_REGION_ + regionName + Const.JPG_EXT
                                val thumbnailFile =
                                    File(folderThumbnails, thumbnailFilename)
                                val result = thumbnailFile.delete()
                                Timber.i("delete ${thumbnailFile.path} success:$result")
                                Timber.i("delete $regionName success:$success")
                            }
                        else {
                            Timber.i("invalidate $regionName success:$success")
                        }
                    }
                } else {
                    moboDeleteConfirmation = null
                    val rootFolder = context.filesDir
                    val mbTilesRootFolder = File(
                        rootFolder,
                        Const.MBTILES_FOLDER
                    )
                    val splits = regionName.split(Const.UNDERLINE, limit = 5)
                    val splitsCount = splits.size
                    val checkName = if (splitsCount == 5)
                        "${regionName}${Const.MBTILES_EXT}" else "${regionName}_${mapType}${Const.MBTILES_EXT}"
                    val rasterFile = File(mbTilesRootFolder, checkName)
                    val result = rasterFile.delete()
                    rasterRegionNames = getRasterRegionNames(context)
                    GeoJsonUtils.createRasterMapsBounds(context) {
                        Timber.i("createGeojsonRasterMapsBounds done")
                    }
                    Timber.i("delete $result: ${rasterFile.path} ")
                }
            } else
                moboDeleteConfirmation = null
        }
    }
    LaunchedEffect(Unit) {
        getOfflineRegionsMap(context) {
            offlineRegionsMap = it
            offlineRegionsMap?.let { it1 -> Timber.i( "${it1.size}") }
        }
    }

    LaunchedEffect(downloadMode) {
        if (downloadMode > 0) {
            Timber.i("downloadMode:$downloadMode")
            downloadBounds(
                context,
                tileLimit,
                regionName,
                regionDefinition,
                { completed, completedCount, requiredCount ->
                    Timber.i("downloadBounds $completed completed: $completedCount required: $requiredCount")
                    statusText = "$completed completed: $completedCount required: $requiredCount"
                    progressAnimation = (completedCount.toFloat() / requiredCount.toFloat())
                    if (completed) {
                        progressAnimation = 1f
                        statusText = "$completed completed: $requiredCount required: $requiredCount"
                        progress(progressAnimation, statusText)
                    } else {
                        statusText =
                            "$completed completed: $completedCount required: $requiredCount"
                        progressAnimation = (completedCount.toFloat() / requiredCount.toFloat())
                        progress(progressAnimation, statusText)
                    }
                },
                { limit ->
                    Timber.e("mapboxTileCountLimitExceeded")
                    downloadMode = 0L
                    downloadActive(false, MapType.Vector)
                    Timber.i("downloadActive: false")
                    progressAnimation = 0f
                    progress(progressAnimation, "mapboxTileCountLimitExceeded")
                    mapboxTileCountLimitExceeded(limit)
                })
        }
    }
    if (showRasterRegions) {
        //rasterRegions = getRasterRegionFiles(context)
        //showRasterRegions = false
        ListRasterDriveEntries(
            onDismissRequest = { showRasterRegions = false },
            import = {
                context.startActivity(
                    Intent(context, FileImportActivity::class.java)
                        .setAction(resources.getString(R.string.import_title))
                        .putExtra(Const.EXTRA_FILETYPE, FileType.MbTiles.name)
                )
            }
        ) { rasterItemModel ->
            rasterItemModel.let {
                Timber.i("rasterItemModel: ${rasterItemModel.name}")
                showRasterRegions = false
                regionName = rasterItemModel.name.replace(Const.MBTILES_EXT, "")
                //getBitmapForRegion(context, regionName, true)
                Timber.i( "regionName $regionName")
            }
        }
    }
    var bounds by remember { mutableStateOf(regionDefinition.bounds) }
    var jobTilemaker by remember { mutableStateOf<Job?>(null) }
    var isGmsMapLoaded by remember { mutableStateOf(false) }
    Box (modifier = modifier.padding(start = 5.dp, top = 100.dp)){
        Column {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
                        Text(
                            text = "Center N: ${bounds?.center?.latitude?.formatLatLngShort()}° " +
                                    "W: ${bounds?.center?.longitude?.formatLatLngShort()}°",
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Row {
                        Text(
                            modifier = Modifier.weight(0.25f),
                            text = "N:${bounds?.latitudeNorth?.formatLatLngShort()}",
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            modifier = Modifier.weight(0.25f),
                            text = "S:${bounds?.latitudeSouth?.formatLatLngShort()}",
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            modifier = Modifier.weight(0.25f),
                            text = "W:${bounds?.longitudeWest?.formatLatLngShort()}",
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            modifier = Modifier.weight(0.25f),
                            text = "E:${bounds?.longitudeEast?.formatLatLngShort()}",
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(modifier = Modifier.weight(0.7f),
                    value = regionName, onValueChange = { regionName = it },
                    label = { Text(stringResource(R.string.region_name)) })
                TextButton(
                    modifier = Modifier.fillMaxWidth(0.25f),
                    onClick = {
                        clipText = regionName
                        Timber.i("import clipText: $clipText")
                        context.startActivity(
                            Intent(context, FileImportActivity::class.java)
                                .setAction(resources.getString(R.string.import_title))
                                .putExtra(Const.EXTRA_FILETYPE, FileType.MbTiles.name)
                        )
                    }
                ) {
                    Text(
                        text = stringResource(R.string.import_title),
                        modifier = Modifier.align(alignment = Alignment.CenterVertically)
                    )
                }
             }
            Spacer(modifier = Modifier.height(15.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.vector), fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth(0.26f)
                            .align(alignment = Alignment.CenterVertically)
                    )
                    AnimatedVisibility(visible = regionName.isNotEmpty()) {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(0.25f),
                            border = BorderStroke(1.dp, Color.LightGray),
                            onClick = {
                                downloadMode = System.currentTimeMillis()
                                downloadActive(true, MapType.Vector)
                                Timber.i( "downloadActive: true ${MapType.Vector}")
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.create), fontSize = 13.sp,
                                modifier = Modifier.align(alignment = Alignment.CenterVertically)
                            )
                        }
                    }
                    TextButton(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        onClick = {
                            showOfflineRegions = true
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.list_offline_regions), fontSize = 13.sp,
                            modifier = Modifier.align(alignment = Alignment.CenterVertically)
                        )
                    }
                    val splits = regionName.split(Const.UNDERLINE, limit = 5)
                    val checkName = "${splits[0]}_${splits[1]}_${splits[2]}_${splits[3]}"
                    AnimatedVisibility(
                        visible = !(offlineRegionsMap?.keys?.contains(checkName) ?: false)
                    ) {
                        Spacer(modifier = Modifier.width(24.dp))
                    }
                    AnimatedVisibility(
                        visible = offlineRegionsMap?.keys?.contains(checkName) ?: false
                    ) {
                        IconButton(modifier = Modifier.size(24.dp, 24.dp),
                            onClick = {
                                moboDeleteConfirmation = "0"
                            }
                        ) {
                            Icon(//modifier = Modifier.align(alignment = Alignment.CenterVertically),
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete Region"
                            )
                        }

                    }
                }
            }
            //HorizontalDivider()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${stringResource(R.string.raster)} $mapType", fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth(0.26f)
                            .align(alignment = Alignment.CenterVertically)
                    )
                    AnimatedVisibility(visible = regionName.isNotEmpty()) {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(0.25f),
                            border = BorderStroke(1.dp, Color.LightGray),
                            onClick = {
                                tilemakerMode = true
                                Timber.i( "downloadActive: true ${MapType.Raster}")
                                downloadActive(true, MapType.Raster)
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.create), fontSize = 13.sp,
                                modifier = Modifier.align(alignment = Alignment.CenterVertically)
                            )
                        }
                    }
                    TextButton(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        onClick = {
                            showRasterRegions = true
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.raster_drive_content), fontSize = 13.sp,
                            modifier = Modifier.align(alignment = Alignment.CenterVertically)
                        )
                    }
                    /*
                AnimatedVisibility(visible = regionName.isEmpty()) {
                    Spacer(modifier = Modifier.fillMaxWidth(0.5f))
                }
 */
                    Timber.i(regionName)
                    val splits = regionName.split(Const.UNDERLINE, limit = 5)
                    val tile = GeoJsonUtils.Companion.Tile(
                        splits[1].toInt(),
                        splits[2].toInt(),
                        splits[3].toInt()
                    )
                    bounds = GeoJsonUtils.tileToBoundsMaplibre(tile)
                    val splitsCount = splits.size
                    Timber.i("$regionName splits: $splitsCount")
                    val checkName = if (splitsCount == 5)
                        "${regionName}${Const.MBTILES_EXT}" else "${regionName}_${mapType}${Const.MBTILES_EXT}"
                    Timber.i("checkName: $checkName $rasterRegionNames")
                    AnimatedVisibility(
                        visible = !(regionName.isNotEmpty() && rasterRegionNames.contains(
                            checkName
                        ))
                    ) {
                        Spacer(modifier = Modifier.width(24.dp))
                    }
                    AnimatedVisibility(
                        visible = (regionName.isNotEmpty() && rasterRegionNames.contains(
                            checkName
                        ))
                    ) {
                        IconButton(modifier = Modifier.size(24.dp, 24.dp),
                            onClick = {
                                moboDeleteConfirmation = "1"
                            }
                        ) {
                            Icon(//modifier = Modifier.align(alignment = Alignment.CenterVertically),
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete Region"
                            )
                        }

                    }
                }
            }
            AnimatedVisibility(visible = progressAnimation > 0 && progressAnimation < 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material.TextButton(onClick = { jobTilemaker?.cancel(null) }) {
                        Text(Const.UC_CLOSE, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                }
            }
            val splits = regionName.split(Const.UNDERLINE, limit = 5)
            val tile = GeoJsonUtils.Companion.Tile(
                splits[1].toInt(),
                splits[2].toInt(),
                splits[3].toInt()
            )

            var cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(
                    bounds?.center?.latitude ?: 0.0,
                    bounds?.center?.longitude ?: 0.0
                ), tile.z.toFloat())
            }
            val tileCenterLatLng = LatLng(bounds?.center?.latitude ?: 0.0, bounds?.center?.longitude ?: 0.0)
            Timber.i("tileCenterLatLng $tileCenterLatLng")
            val tileCenterLatLngState = rememberUpdatedMarkerState(position = tileCenterLatLng)
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                    .width(320.dp)
                    .height(320.dp)
            ) {
                GoogleMapViewInColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("Map")
                        .pointerInteropFilter(
                            onTouchEvent = {
                                when (it.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        Timber.i("onMapTouched")
                                        false
                                    }

                                    else -> {
                                        Timber.i("MotionEvent ${it.action} - this never triggers.")
                                        true
                                    }
                                }
                            }
                        ),
                    cameraPositionState = cameraPositionState,
                    tileCenterLatLngState = tileCenterLatLngState,
                    tile.x, tile.y, tile.z,
                    onMapLoaded = {
                        isGmsMapLoaded = true
                        Timber.i("onMapLoaded")
                    }, onMapClick = { latLng ->
                        val tile_ = GeoJsonUtils.pointToTile(
                            latLng.longitude,
                            latLng.latitude,
                            tile.z.toDouble()
                        )
                        Timber.i("tile_ $tile_")
                        bounds = GeoJsonUtils.tileToBoundsMaplibre(tile_)
                        regionName = "${Const.TILE_PREFIX}${tile_.x}_${tile_.y}_${tile_.z}"
                        Timber.i("regionName $regionName")
                        cameraPositionState = CameraPositionState(position = CameraPosition.fromLatLngZoom(
                                LatLng(
                                    bounds?.center?.latitude ?: 0.0,
                                    bounds?.center?.longitude ?: 0.0
                                ), tile.z.toFloat()))
                    }, onMarkerClick = { region ->
                        Timber.i("region $region")
                    }
                )
                if (!isGmsMapLoaded) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .wrapContentSize()
                    )
                }
            }
        }

        if (tilemakerMode) {
            Timber.i( "tilemakerMode:$tilemakerMode")
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    bounds?.let {
                        tilemakerUrl?.let { baseUrl ->
                            MbtilesCreator(
                                context = context
                            ).createMbtiles(
                                regionName,
                                mapType = mapType.toString(),
                                baseUrl = baseUrl,
                                area = arrayOf(
                                    com.google.android.gms.maps.model.LatLng(
                                        it.northWest.latitude,
                                        it.northWest.longitude
                                    ),
                                    com.google.android.gms.maps.model.LatLng(
                                        it.southWest.latitude,
                                        it.southWest.longitude
                                    ),
                                    com.google.android.gms.maps.model.LatLng(
                                        it.southEast.latitude,
                                        it.southEast.longitude
                                    ),
                                    com.google.android.gms.maps.model.LatLng(
                                        it.northEast.latitude,
                                        it.northEast.longitude
                                    )
                                ),
                                zooms = intArrayOf(
                                    4.coerceAtLeast(tilemakerMinZoom),
                                    15.coerceAtMost(tilemakerMaxZoom)
                                ),
                                progress = { job, p ->
                                    tilemakerUrl = null
                                    progressAnimation = (0.01f * p)
                                    progress(progressAnimation, "$p %")
                                    jobTilemaker = job
                                },
                                cancel = {
                                    Timber.i("canceled: $regionName")
                                    progressAnimation = (0.0f)
                                    progress(progressAnimation, "$0 %")
                                    rasterRegionNames = getRasterRegionNames(context)
                                    Timber.i( "downloadActive: false")
                                    downloadActive(false, MapType.Raster)
                                },
                                ready = {
                                    Timber.i("ready: $regionName")
                                    progressAnimation = (0.0f)
                                    progress(progressAnimation, "$0 %")
                                    rasterRegionNames = getRasterRegionNames(context)
                                    GeoJsonUtils.createRasterMapsBounds(context) { path ->
                                        Timber.i("new grid: $path")
                                    }
                                    Timber.i( "downloadActive: false")
                                    downloadActive(false, MapType.Raster)
                                }
                            )
                        }
                        //TilemakerComposeScreen(it, regionName) { tilemakerMode = false }
                        //Timber.i( "$regionName")
                    }
                }
            }
        }
    }
}

@Composable
fun DropdownMenuOfflineRegions(
    context: Context, offlineRegions: MutableCollection<OfflineRegion>?,
    finish: (OfflineRegion?, String?) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        DropdownMenu(
            expanded = !offlineRegions.isNullOrEmpty(),
            onDismissRequest = { if (offlineRegions != null) {finish(null, null)} }) {
            val regionNames = ArrayList<String>()
            if (offlineRegions != null) {
                for (offlineRegion in offlineRegions) {
                    if (!regionNames.contains(getRegionName(offlineRegion))) {
                        val itemRegionName = getRegionName(offlineRegion)
                        Timber.i(itemRegionName)
                        DropdownMenuItem( //modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(3.dp, 6.dp, 3.dp, 0.dp),
                            text = { Text(text = itemRegionName, color = Color.Black) },
                            onClick = {
                                finish(offlineRegion, itemRegionName)
                                Timber.i("$itemRegionName id:${offlineRegion.id}")
                                //offlineRegions = null
                                finish(null, null) //offlineRegions = null
                            }
                        )
                        regionNames.add(itemRegionName)
                    }
                }
            }
        }
    }

}
private fun invalidateOfflineRegion(region: OfflineRegion?, callback: (Boolean) -> Unit) {
    // region tiles will be set to expired
    region!!.invalidate(object : OfflineRegion.OfflineRegionInvalidateCallback {
        override fun onInvalidate() {
            Timber.i(
                "invalidate OK")
            callback(true)
        }

        override fun onError(error: String) {
            Timber.i(
                "invalidate error:$error")
            callback(false)
        }
    })
}

private fun deleteOfflineRegion(region: OfflineRegion?, callback: (Boolean) -> Unit) {
    // changes region tables but not tiles
    region!!.delete(object : OfflineRegionDeleteCallback {
        override fun onDelete() {
            Timber.i(
                "delete OK")
            callback(true)
        }

        override fun onError(error: String) {
            Timber.i(
                "delete error:$error")
            callback(false)
        }
    })
}

fun getBitmapForRegion(context: Context, regionName: String, createThumbnail: Boolean): Bitmap? {
    val dbName = "${regionName}${Const.MBTILES_EXT}"
    val splits = dbName.split(Const.UNDERLINE, limit = 5)
    if (splits.size > 3) {
        Timber.i( "dbName: $dbName")
        val dbFile = MbtilesDatabase.DatabaseContext(context).getDatabasePath(dbName)
        if (dbFile.exists()) { // prevent database create
            val dbHelper = MbtilesDatabase.MbtilesHelper(context.applicationContext, dbName)
            try {
                val db = dbHelper.writableDatabase
                val row = 1023 - splits[2].toInt()
                val cursor = MbtilesDatabase.getTileBitmap(
                    db, splits[3].replace(Const.MBTILES_EXT, "").toInt(),
                    splits[1].toInt(), row
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val byteArray = it.getBlob(0)
                        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                        if (createThumbnail) {
                            val folderThumbnails = File(
                                context.filesDir,
                                Const.THUMBNAILS
                            )
                            val thumbnailFilename = "$regionName${Const.PNG_EXT}"
                            val thumbnailFile = File(folderThumbnails, thumbnailFilename)
                            val out = FileOutputStream(thumbnailFile)
                            bitmap.compress(
                                Bitmap.CompressFormat.PNG, 100, out
                            )
                            out.flush()
                            out.close()
                        }
                        return bitmap
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error reading MBTiles: $dbName")
            } finally {
                dbHelper.close()
            }
        } else {
            Timber.i("not found: ${dbFile.path}")
        }
        return null
    } else {
        Timber.i("invalid name: $dbName")
    }
    return null
}

suspend fun downloadBounds(
    context: Context,
    tileLimit: Long?,
    regionName: String,
    regionDefinition: OfflineTilePyramidRegionDefinition,
    onStatusChanged: (Boolean, Long, Long) -> Unit,
    mapboxTileCountLimitExceeded: (Long) -> Unit
): Result<Unit> {
    return suspendCancellableCoroutine { continuation ->
        Timber.i( "regionName: $regionName tileLimit: $tileLimit")
        val offlineManager = OfflineManager.getInstance(context)
        tileLimit?.let { offlineManager.setOfflineMapboxTileCountLimit(it) } // 18mai2025
        val metadata = "{name: $regionName}"
        val encodedMetadata = metadata.toByteArray()
        offlineManager.createOfflineRegion(
            definition = regionDefinition,
            metadata = encodedMetadata,
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) {
                    offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                    offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
                        override fun mapboxTileCountLimitExceeded(limit: Long) {
                            continuation.resume(Result.failure(Throwable("mapboxTileCountLimitExceeded $limit")))
                            Timber.e( "mapboxTileCountLimitExceeded $limit")
                            mapboxTileCountLimitExceeded(limit)
                        }

                        override fun onError(error: OfflineRegionError) {
                            if (continuation.isCompleted.not()) {
                                continuation.resume(Result.failure(Throwable(error.message)))
                            } else {
                                Timber.e( "Continuation was completed already $error")
                            }
                        }

                        override fun onStatusChanged(status: OfflineRegionStatus) {
                            onStatusChanged(status.isComplete, status.completedTileCount, status.requiredResourceCount)
                            Timber.i(
                                "onStatusChanged downloadState= ${status.downloadState} isComplete= ${status.isComplete}," +
                                        "completedTileCount= ${status.completedTileCount} required= ${status.requiredResourceCount}"
                            )
                            if (status.isComplete) {
                                if (continuation.isCompleted.not()) {
                                    continuation.resume(Result.success(Unit))
                                } else {
                                    Timber.e( "Continuation was completed already")
                                }
                            }
                        }
                    })
                }

                override fun onError(error: String) {
                    continuation.resume(Result.failure(Throwable(message = error)))
                }
            }
        )
    }
}

fun getRasterRegionNames(context: Context): ArrayList<String> {
    val rootFolder = context.filesDir
    val mbTilesRootFolder = File(rootFolder, Const.MBTILES_FOLDER)
    mbTilesRootFolder.mkdirs()
    val fileFilter = FileFilter { file: File? -> file?.name?.endsWith(Const.MBTILES_EXT) == true &&
            !file.name.contains(Const.JOURNAL)
    }
    val files: Array<File> = mbTilesRootFolder.listFiles(fileFilter) as Array<File>
    files.sortWith(compareBy { it.name })
    val names = arrayListOf<String>()

    files.iterator().forEach {file ->
        val name = file.name
        names.add(name)
        //Timber.i( "$name")
    }
    return names
}

fun getOfflineRegionsMap(context: Context, completed: (HashMap<String, OfflineRegion>?) -> Unit) {
    val regionsMap = hashMapOf<String, OfflineRegion>()
    //Timber.i( "getOfflineRegionsMap")
    val offlineManager = OfflineManager.getInstance(context)
    offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
        override fun onError(error: String) {
            Timber.e( "Error: $error")
            completed(null)
        }

        override fun onList(offlineRegions: Array<OfflineRegion>?) {
            // Check result. If no regions have been
            // downloaded yet, notify user and return
            if (!offlineRegions.isNullOrEmpty()) {
                // Add all of the region names to a list
                Timber.i( "OfflineRegions: %s", offlineRegions.size)
                offlineRegions.iterator().forEach {
                    val name = getRegionName(it)
                    regionsMap.put(name, it)
                }
                completed(regionsMap)
            } else
                Timber.e( "offlineRegions.isNullOrEmpty")
        }
    })
}

fun getOfflineRegions(context: Context, completed: (Array<OfflineRegion>?) -> Unit) {
    Timber.i( "getOfflineRegions")
    val offlineManager = OfflineManager.getInstance(context)
    offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
        override fun onError(error: String) {
            Timber.i("Error: $error")
            completed(null)
        }

        override fun onList(offlineRegions: Array<OfflineRegion>?) {
            // Check result. If no regions have been
            // downloaded yet, notify user and return
            if (!offlineRegions.isNullOrEmpty()) {
                // Add all of the region names to a list
                Timber.i( "OfflineRegions: %s", offlineRegions.size)
                completed(offlineRegions)
                offlineRegions.iterator().forEach { offlineRegion ->
                    Timber.i( "${offlineRegion.definition.bounds}")
                }
            } else {
                Timber.i("offlineRegions.isNullOrEmpty")
                completed(null)
            }
        }
    })
}

fun getRegionSnapshot(context: Context, offlineRegion: OfflineRegion, regionName: String) : Bitmap? {
    val folderThumbnails = File(context.filesDir, Const.THUMBNAILS)
    val thumbnailFilename = Const.MBGL_REGION_ + regionName + Const.JPG_EXT
    val thumbnailFile = File(folderThumbnails, thumbnailFilename)
    if (!thumbnailFile.exists()) {
        val definition = offlineRegion.definition
        val bounds = definition.bounds
        Timber.i( "bounds $bounds")
        if (bounds != null) {
            val lllh = ArrayList<LatLngH>()
            lllh.add(LatLngH(bounds.southWest.latitude, bounds.southWest.longitude))
            lllh.add(LatLngH(bounds.northWest.latitude, bounds.northWest.longitude))
            lllh.add(LatLngH(bounds.northEast.latitude, bounds.northEast.longitude))
            lllh.add(LatLngH(bounds.southEast.latitude, bounds.southEast.longitude))
            lllh.add(LatLngH(bounds.southWest.latitude, bounds.southWest.longitude))
            Helpers.takeSnapshot(context, lllh, thumbnailFilename, Const.styleVectorUri,384, 0.1, false,
            ) { snapShot ->
                Timber.i("snapshot ready $regionName")
            }
            return null
        }
    } else {
        return BitmapFactory.decodeFile(thumbnailFile.path)
    }
    return null
}

fun getRegionName(offlineRegion: OfflineRegion): String {
    // Get the region name from the offline region metadata
    val regionName: String = try {
        val metadata: ByteArray = offlineRegion.metadata
        val jsonMetadata = String(metadata, Charsets.UTF_8)
        val jsonObjectMetadata = JSONObject(jsonMetadata)
        jsonObjectMetadata.getString(Const.MBGL_METADATA_REGION_NAME)
    } catch (exception: Exception) {
        Timber.e("Failed to decode metadata:%s", exception.message)
        return ""
    }
    //Timber.i( "getRegionName $regionName")
    return regionName
}

fun Double.formatLatLng(): String {
    val symbols = DecimalFormatSymbols(Locale.US)
    val decimalFormatter = DecimalFormat("00.0000", symbols)
    return decimalFormatter.format(this)
}
fun Double.formatLatLngShort(): String {
    val symbols = DecimalFormatSymbols(Locale.US)
    val decimalFormatter = DecimalFormat("#.00", symbols)
    return decimalFormatter.format(this)
}