package com.almica.ramani.geojsonMaps

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.graphics.scale
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.preference.PreferenceManager
import com.almica.ramani.Const
import com.almica.ramani.Helpers
import com.almica.ramani.LatLngH
import com.almica.ramani.MaptypeKey
import com.almica.ramani.R
import com.almica.ramani.charts.theme.Black
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.filepicker.UnzipUtils
import com.almica.ramani.googlemaps.NewMapAction
import com.almica.ramani.tilemaker.MbtilesDatabase
import com.almica.ramani.utils.BackPressHandler
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.simpleStringWithTime
import com.almica.ramani.utils.format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.snapshotter.MapSnapshot
import timber.log.Timber
import java.io.File
import java.util.Date
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.pow


@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListGeojsonDatabaseScreen(
    innerPadding: PaddingValues,
    position: LatLng?,
    finish: (Boolean) -> Unit
) {
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
    val context = LocalContext.current
    val localConfiguration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val localDensity = LocalDensity.current
    val preferences = PreferenceManager.getDefaultSharedPreferences(LocalContext.current)
    //val liveSharedPreferences = LiveSharedPreferences(preferences)
    var snackbarData by remember { mutableStateOf<GeojsonSnackbarData?>(null) }
    var regionFilter by remember { mutableStateOf<String?>(null) }
    var restartRequired by remember { mutableStateOf(false) }
    var refreshRequired by remember { mutableIntStateOf(0) }
    //Timber.i( "zoomLevelFilter: $zoomLevelFilter")
    var geojsonDatabaseItemModels by remember { mutableStateOf<List<GeojsonDatabaseItemModel>>(listOf())}
    var mapsGrouped by remember { mutableStateOf<Map<String, List<GeojsonDatabaseItemModel>>?>(null) }
    var processState by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = snackbarData) {
        Timber.i( "LaunchedEffect(key1 = snackbarData)")
        delay(5000)
        snackbarData = null
    }
    LaunchedEffect(refreshRequired) {
        Timber.i( "LaunchedEffect refreshRequired: $refreshRequired ")
        processState = true
        getGeojsonDatabaseEntities(context, localConfiguration, localDensity, regionFilter) { items ->
            geojsonDatabaseItemModels = items
            mapsGrouped = geojsonDatabaseItemModels.groupBy { it.path }
            mapsGrouped?.let { Timber.i( "LaunchedEffect mapsGrouped: ${it.size} ") }
            processState = false
        }
    }
    var name13: String? = null
    var name12: String? = null
    var region: String? = null
    if (position != null) {
        //Timber.i( "position: $position")
        val geojsonTile10 : GeoJsonUtils.Companion.Tile = GeoJsonUtils.pointToTile(position.longitude,
            position.latitude, 10.0)
        val geojsonTile12 : GeoJsonUtils.Companion.Tile = GeoJsonUtils.pointToTile(position.longitude,
            position.latitude, 12.0)
        val geojsonTile13 : GeoJsonUtils.Companion.Tile = GeoJsonUtils.pointToTile(position.longitude,
            position.latitude, 13.0)
        region = "${geojsonTile10.x}_${geojsonTile10.y}_${geojsonTile10.z}"
        name12 = "geojsonTile_${geojsonTile12.x}_${geojsonTile12.y}_${geojsonTile12.z}"
        name13 = "geojsonTile_${geojsonTile13.x}_${geojsonTile13.y}_${geojsonTile13.z}"
        //Timber.i( "name12: $name12")
        //Timber.i( "name13: $name13")
    }

    var showRegionFilterMenu by remember { mutableStateOf(false) }
    var showImportDropdownMenu by remember { mutableStateOf(false) }
    var useGeojsonMaps by remember {
        mutableStateOf(
            preferences.getInt(Const.PREF_MAPTYPE_KEY, 0) == MaptypeKey.GeoJson.ordinal
        )
    }
    BackPressHandler {
        Timber.i( "Back Press intercepted")
        finish(restartRequired)
    }
    Scaffold(
        modifier = Modifier.padding(innerPadding),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            finish(restartRequired)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back home"
                        )
                    }
                }, title = {
                    Text(text = stringResource(R.string.geojson), fontSize = 14.sp)
                }, actions = {
                    IconButton(onClick = {
                        Timber.i("")
                        showRegionFilterMenu = true
                    }) {
                        Icon(
                            Icons.Outlined.FilterAlt,
                            tint = if (regionFilter != null) Color.Magenta else Color.Unspecified,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = {
                        showImportDropdownMenu = true
                        Timber.i("")

                    }) {
                        Icon(
                            Icons.Outlined.ImportExport,
                            contentDescription = null
                        )
                    }

                    useGeojsonMaps.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = stringResource(R.string.enable),
                                fontSize = 14.sp
                            )
                            Checkbox(
                                checked = it,
                                onCheckedChange = { checked ->
                                    snackbarData = if (checked)
                                        GeojsonSnackbarData(
                                            context.getString(R.string.after_restart_no_rastermaps),
                                            null,
                                            null
                                        )
                                    else
                                        GeojsonSnackbarData(
                                            context.getString(R.string.after_restart),
                                            null,
                                            null
                                        )
                                    Timber.i("checked: $checked")
                                    useGeojsonMaps = checked
                                    val pmk = if (checked) 1 else 0
                                    preferences.edit { putInt(Const.PREF_MAPTYPE_KEY, pmk) }
                                    Timber.i("PREF_MAPTYPE_KEY: $pmk")
                                },
                            )
                        }
                    }
                }
            )
        },
    ) { paddingValues ->
        snackbarData?.let {
            MoboSnack(snackbarData!!) { action ->
                when (action) {
                    GeojsonSnackbarSelection.Nothing -> snackbarData = null
                    GeojsonSnackbarSelection.Import -> {
                        snackbarData = null
                        FileImportActivity.launch(context, FileType.GeoJson)
                    }
                    null -> snackbarData = null
                }
            }
        }
        if (showRegionFilterMenu) {
            RegionDropdownMenu(geojsonDatabaseItemModels) {region ->
                //getGeojsonDatabaseEntities(context, localConfiguration, localDensity, region) { geojsonDatabaseItemModels = it }
                showRegionFilterMenu = false
                regionFilter = region
                refreshRequired++
            }
        }
        /**
         * 31dez2025
         * emulator pixel7, nexus5 imported geojson database was not usable
         */
        if (showImportDropdownMenu) {
            Timber.i("$region $name12 $name13")
            ImportGeojsonDropdownMenu(context, listOf(region, name12, name13)) { fileType, _ ->
                showImportDropdownMenu = false
                if (fileType != FileType.Nothing) {
                    FileImportActivity.launch(context, fileType)
                }
            }
        }

        GeojsonMapsGroupedList(Modifier.padding(paddingValues), mapsGrouped,
            updateGeojsonMaps = { region, status ->
                snackbarData = GeojsonSnackbarData(context.getString(R.string.new_mapstatus_, region, status.toString()),
                    null, null)
                Timber.i( "region: $region status: $status")
                updateMapStatusWithRegion(context, region, status) {
                    restartRequired = true
                    //regionFilter = null
                    refreshRequired++
                }
            }, shareRegion = { region ->
                Timber.i( "region: $region")
                //shareGeojsonMaps(context, region, lifecycleOwner) //multiple files
                snackbarData = GeojsonSnackbarData(context.getString(R.string.create_geojson_archive), null, null)
                UnzipUtils.shareZippedGeojsonRegion(context, region, lifecycleOwner) {entriesCount ->
                    Timber.i( "entriesCount: $entriesCount")
                    snackbarData = null
                }
            }, refreshThumbnail = { name, bounds ->
            val geojsonMapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
                takeSnapshot(context, name, bounds) { snapshot ->
                    if (snapshot != null) {
                        Timber.i("takeSnapshot" +
                                    " $name ready byteCount: ${snapshot.bitmap.byteCount}")

                        geojsonMapRepository.updateGeojsonMapByName(
                            snapshot.bitmap,
                            name
                        ) {
                            snackbarData = GeojsonSnackbarData(context.getString(R.string.map_thumbnail_ready), null, null)
                            Timber.i( "update ready $name")
                            refreshRequired++
                        }
                    } else
                        Timber.i( "snapshot = null $name")
                }
            }, updateState = { name, newState ->
                val geojsonMapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
                geojsonMapRepository.updateGeojsonMapStatus(newState, name) {
                    refreshRequired++
                    restartRequired = true
                }
            }, removeRegion = { region ->
                deleteGeojsonMaps(context, region) {
                    refreshRequired++
                    restartRequired = true
                }
            })
        AnimatedVisibility(
            visible = processState,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.initialization),
                    modifier = Modifier
                        .background(Color.White)
                        .padding(4.dp),
                    textAlign = TextAlign.Center
                )
                CircularProgressIndicator(
                    modifier = Modifier.size(50.dp, 50.dp)
                )
            }
        }
    }
}


private fun deleteGeojsonMaps(context: Context, region: String, finished: () -> Unit) {
    val geojsonMapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
    geojsonMapRepository.removeGeojsonMapsByRegion(region) {
        Timber.i( "remove ready region: $region")
        finished()
    }
    return
}

private fun updateMapStatusWithRegion(context: Context, region: String, status: Boolean, finished: () -> Unit) {
    val geojsonMapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
    Timber.i( "region: $region status: $status")
    geojsonMapRepository.getAllSimple(false) { geojsonMapEntities ->
        geojsonMapEntities.forEach { geojsonMapEntity ->
                geojsonMapRepository.updateGeojsonMapStatus(status && geojsonMapEntity.path == region,
                    geojsonMapEntity.name) {}
        }
        finished()
    }
}

@Composable
private fun GeojsonMapsGroupedList(
    modifier: Modifier,
    mapsGrouped: Map<String, List<GeojsonDatabaseItemModel>>?,
    updateGeojsonMaps: (String, Boolean) -> Unit,
    shareRegion: (String) -> Unit,
    refreshThumbnail: (String, LatLngBounds) -> Unit,
    updateState: (String, Boolean) -> Unit,
    removeRegion: (String) -> Unit
) {
//    var geojsonDatabaseItemModels by remember { mutableStateOf<List<GeojsonDatabaseItemModel>>(listOf()) }
//    geojsonDatabaseItemModels = List(items.size) {index ->
//        items[index]
//    }
    val geojsonMapRepository = GeojsonMapRepository.getInstance(LocalContext.current, Executors.newSingleThreadExecutor())
    var indexExpanded by remember { mutableIntStateOf(-1) }

    var groupExpanded by remember { mutableStateOf<String?>(null) }
    //val mapsGrouped = geojsonDatabaseItemModels.groupBy { it.path }
    Timber.i( "mapsGrouped: ${mapsGrouped?.size}")
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        mapsGrouped?.forEach { (initial, mapEntities) ->
            var activeCount = 0
            mapEntities.forEach { mapEntity ->
                if (mapEntity.selected)
                    activeCount++
            }
            stickyHeader {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(color = Color.LightGray),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BadgedBox(badge = { Badge { Text("${mapEntities.size}") } }) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                            BadgedBox(badge = { Badge { Text("$activeCount") } }) {
                                IconButton(onClick = {
                                    updateGeojsonMaps(initial, activeCount != mapEntities.size)
                                }) {
                                    Icon(
                                        Icons.Outlined.CheckCircleOutline,
                                        contentDescription = null
                                    )
                                }
                            }
                            TextButton(onClick = {
                                groupExpanded = if (groupExpanded != null && groupExpanded == initial)
                                    null else initial
                            }) {
                                Text(
                                    text = initial,
                                    //modifier = Modifier.fillMaxWidth(0.5f),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                            TextButton(onClick = {
                                groupExpanded = if (groupExpanded != null && groupExpanded == initial)
                                    null else initial
                            }) {
                                Text(
                                    text =
                                        if (groupExpanded != null && initial == groupExpanded) Const.UC_DROPUP_ARROW else Const.UC_DROPDOWN_ARROW,
                                    textAlign = TextAlign.Center, fontSize = 20.sp
                                )
                            }
                            IconButton(onClick = {
                                shareRegion(initial)
                            }) {
                                Icon(
                                    Icons.Outlined.Share,
                                    contentDescription = stringResource(R.string.export_title)
                                )
                            }
                            IconButton(onClick = {
                                removeRegion(initial)
                            }) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = stringResource(R.string.remove)
                                )
                            }
                        }
                    }
                }
            }

            items(mapEntities.size) { index ->
                if(groupExpanded != null && mapEntities[index].path == groupExpanded)
                    MapItem(index, indexExpanded, mapEntities, geojsonMapRepository, expand = {index ->
                        indexExpanded = index
                    }, refreshThumbnail = {mapName, latLngBounds ->
                        refreshThumbnail(mapName, latLngBounds)
                    }, updateState = {mapName, newState ->
                        updateState(mapName, newState)
                    })
            }
        }
    }
}

@Composable
private fun MapItem(
    index: Int,
    indexExpanded: Int,
    mapEntities: List<GeojsonDatabaseItemModel>,
    geojsonMapRepository: GeojsonMapRepository,
    expand: (Int) -> Unit,
    refreshThumbnail: (String, LatLngBounds) -> Unit,
    updateState: (String, Boolean) -> Unit
) {
    Timber.i( "index: $index indexExpanded: $indexExpanded")
    //var indexExpanded by remember { mutableIntStateOf(indexExp) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 3.dp, start = 3.dp, end = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        updateState(
                            mapEntities[index].name,
                            mapEntities[index].selected.not()
                        )
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mapEntities[index].selected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "selected",
                        tint = Color.Magenta,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                } else {
                    Spacer(modifier = Modifier.width(35.dp))
                }
                Column(modifier = Modifier.fillMaxWidth(0.80f)) {
                    Text(
                        fontSize = 13.sp,
                        text = mapEntities[index].name
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(0.60f), fontSize = 13.sp,
                        text = mapEntities[index].path
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(0.60f), fontSize = 10.sp,
                        text = "(${mapEntities[index].lastModifiedDate})"
                    )
                }
            }
            TextButton(onClick = {
                //mapEntities[index].expanded = mapEntities[index].expanded.not()
                if (indexExpanded == index)
                    expand(-1)
                //indexExpanded = -1
                else {
                    if (mapEntities[index].thumbnail == null) {
                        mapEntities[index].thumbnail =
                            geojsonMapRepository.getGeojsonThumbnail(mapEntities[index].id)
                    }
                    expand(index)
                    //indexExpanded = index
                }
                //notify(listOf(index), GeojsonDatabaseAction.ExpansionChanged)
            }) {
                Text(
                    text =
                        if (index == indexExpanded) Const.UC_DROPUP_ARROW else Const.UC_DROPDOWN_ARROW,
                    textAlign = TextAlign.Center, fontSize = 20.sp
                )
            }
        }
        AnimatedVisibility(visible = index == indexExpanded) {
            Column {
                if (mapEntities[index].bounds != null) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val bounds = mapEntities[index].bounds
                        bounds?.let {
                            val textBounds = "S ${it.southWest.latitude.format(3)}° " +
                                    "N ${it.northWest.latitude.format(3)}° " +
                                    "W ${it.northWest.longitude.format(3)}° " +
                                    "E ${it.northEast.longitude.format(3)}°"
                            Text(
                                text = textBounds,
                                textAlign = TextAlign.Center, fontSize = 11.sp
                            )
                        }
                    }
                }

                if (mapEntities[index].thumbnail != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val imageBitmap =
                        mapEntities[index].thumbnail!!.asImageBitmap()
//                            Timber.i(
//                                "${index}: thumbnail not null width: " +
//                                        "${imageBitmap.width}")

                    // Lays out and draws an image sized to the rectangular subsection of the ImageBitmap
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = BitmapPainter(
                                imageBitmap, IntOffset(0, 0),
                                IntSize(
                                    mapEntities[index].thumbnail!!.width,
                                    mapEntities[index].thumbnail!!.height
                                )
                            ),
                            contentDescription = mapEntities[index].name
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = {
                            mapEntities[index].bounds?.let {
                                refreshThumbnail(
                                    mapEntities[index].name,
                                    it
                                )
                            }
                        }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "refresh")
                        }
                    }
                    Timber.i("${mapEntities[index].name} thumbnail = null")
                }
                HorizontalDivider(modifier = Modifier.padding(4.dp))
            }
        }
    }
}

@Composable
fun ImportGeojsonDropdownMenu(context: Context, mapToImport: List<String?>, import: (FileType, String?) -> Unit) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = { import(FileType.Nothing, null) }
    ) {
        DropdownMenuItem(
            {
                when (mapToImport[0]) {
                    null -> Text(text = context.getString(R.string.geojson_zip))
                    else -> Text(text = "$mapToImport[0] Zip", color = Black)
                }
            },
            onClick = { import(FileType.GeoJsonZip, mapToImport[0]) }
        )
        HorizontalDivider()
        DropdownMenuItem(
            {
                when (mapToImport[0]) {
                    null -> Text(text = context.getString(R.string.geojson))
                    else -> Text(text = "${mapToImport[1]}\n${mapToImport[1]}", color = Black)
                }
            },
            onClick = { import(FileType.GeoJson, null) }
        )
    }
}

@Composable
private fun RegionDropdownMenu(mapEntities: List<GeojsonDatabaseItemModel>?, selectRegion: (String?) -> Unit) {
    val regions = createRegionArray(mapEntities)
    Surface(
        Modifier
            .fillMaxWidth()
            .padding(top = 250.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.align(Alignment.CenterStart)) {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { selectRegion(null) }
                ) {
                    regions.forEach { region ->
                        if (region != null) {
                            DropdownMenuItem(
                                { Text(text = region, color = Black) },
                                onClick = { selectRegion(region) }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun createRegionArray(mapEntities: List<GeojsonDatabaseItemModel>?) : Array<String?> {
    val regionList = ArrayList<String>()
    if (mapEntities != null) {
        for (mapEntity in mapEntities) {
            if (!regionList.contains(mapEntity.path))
                regionList.add(mapEntity.path)
        }
    }
    if (regionList.size == 1)
        regionList.add(0, "")
    var regionArr = arrayOfNulls<String>(regionList.size)
    regionArr = regionList.toArray(regionArr)
    return regionArr
}

private fun takeSnapshot(context: Context, name: String, bounds: LatLngBounds, finish: (MapSnapshot?) -> Unit) {
    val lllh = arrayListOf<LatLngH>()
    lllh.add(LatLngH(bounds.northWest.latitude, bounds.northWest.longitude))
    lllh.add(LatLngH(bounds.northEast.latitude, bounds.northEast.longitude))
    lllh.add(LatLngH(bounds.southEast.latitude, bounds.southEast.longitude))
    lllh.add(LatLngH(bounds.southWest.latitude, bounds.southWest.longitude))
    lllh.add(LatLngH(bounds.northWest.latitude, bounds.northWest.longitude))
    Helpers.takeSnapshot(
        context, lllh, name, Const.styleVectorUri,
        512, 0.1, false,
    ) { snapshot ->
        finish(snapshot)
        Timber.i("${System.currentTimeMillis()} bounds:$bounds snapshot ready")
    }
}

fun getGeojsonDatabaseEntities(
    context: Context,
    configuration: Configuration,
    density: Density,
    regionFilter: String?,
    finished: (List<GeojsonDatabaseItemModel>) -> Unit
) {
    Timber.i( "regionFilter: $regionFilter")
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val mapType = preferences.getString(context.getString(R.string.pref_tilemaker_maptype), Const.OUTDOOR)

    val geojsonMapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
    geojsonMapRepository.getAllSimple(true) {geojsonMapEntities ->
        Timber.i("geojsonMapEntities: ${geojsonMapEntities.size}")
        val geoJsonDatabaseItemModels = ArrayList<GeojsonDatabaseItemModel>()
        geojsonMapEntities.forEach { geojsonMapEntity ->
            //Timber.i( "geojson name: ${geojsonMapEntity.name} enabled: ${geojsonMapEntity.enabled}")
            val lastModifiedDate = Date(geojsonMapEntity.lastModifiedTime)
            val name = geojsonMapEntity.name.replace("geojsonTile", "tile")
                .replace(Const.GEOJSON_EXT, "").replace(Const.HASHTAG, "") + "${Const.UNDERLINE}$mapType"

            var scaledBitmap: Bitmap? = null
            val splits = name.split(Const.UNDERLINE, limit = 5)
            if (splits.size > 3) {
                val tile = GeoJsonUtils.Companion.Tile(splits[1].toInt(), splits[2].toInt(), splits[3].toInt())
                val tileLat = GeoJsonUtils.tile2lat(tile.y, tile.z)
                val tileLon = GeoJsonUtils.tile2lon(tile.x, tile.z)
                val tile10 = GeoJsonUtils.pointToTile(tileLon, tileLat, 10.0)
                val mbtilesName = "tile_${tile10.x}_${tile10.y}_${tile10.z}_$mapType"
                //Timber.i( "regionName: $regionName")
                val bitmap = getBitmapFromMbtiles(context, mbtilesName, tile.x, tile.y, geojsonMapEntity.z)
                val screenWidthPx = with(density) {configuration.screenWidthDp.dp.roundToPx()}
                if (bitmap != null) {
                    val scale = 0.75 * screenWidthPx / bitmap.width
                    scaledBitmap = bitmap.scale((bitmap.width * scale).toInt(), (bitmap.height * scale).toInt())
                } else if (geojsonMapEntity.bitmap != null) {
                    val scale = 0.75 * screenWidthPx / geojsonMapEntity.bitmap!!.width
                    scaledBitmap = geojsonMapEntity.bitmap!!.scale((geojsonMapEntity.bitmap!!.width * scale).toInt(),
                        (geojsonMapEntity.bitmap!!.height * scale).toInt())
                }
//                else
//                    Timber.i( "bitmap = null")
            }

            val boundsBuilder = LatLngBounds.Builder()
            boundsBuilder.include(LatLng(geojsonMapEntity.north, geojsonMapEntity.west))
            boundsBuilder.include(LatLng(geojsonMapEntity.north, geojsonMapEntity.east))
            boundsBuilder.include(LatLng(geojsonMapEntity.south, geojsonMapEntity.east))
            boundsBuilder.include(LatLng(geojsonMapEntity.south, geojsonMapEntity.west))
            boundsBuilder.include(LatLng(geojsonMapEntity.north, geojsonMapEntity.west))

            if ((regionFilter == null).or(geojsonMapEntity.path == regionFilter)) {
                geoJsonDatabaseItemModels.add(
                    GeojsonDatabaseItemModel(
                        id = geojsonMapEntity.id,
                        name = geojsonMapEntity.name,
                        geojsonMapEntity.path,
                        geojsonMapEntity.data,
                        thumbnail = scaledBitmap, //geojsonMapEntity.bitmap,
                        boundsBuilder.build(),
                        geojsonMapEntity.z,
                        lastModifiedDate.simpleStringWithTime(),
                        selected = geojsonMapEntity.enabled)
                )
            }
        }
        Timber.i("geoJsonDatabaseItemModels: ${geoJsonDatabaseItemModels.size}")
        finished(geoJsonDatabaseItemModels)
    }
}

fun getBitmapFromMbtiles(
    context: Context,
    mbtilesName: String,
    x: Int,
    y: Int,
    z: Int
): Bitmap? {
    val dbName = "${mbtilesName}${Const.MBTILES_EXT}"
    val dbFile = MbtilesDatabase.DatabaseContext(context).getDatabasePath(dbName)
    if (dbFile.exists()) { // prevent database create
        val dbHelper = MbtilesDatabase.MbtilesHelper(context.applicationContext, dbName)
        try {
            val db = dbHelper.readableDatabase
            val row =
                2.0.pow(z.toDouble()) - y - 1
            //Timber.i( "dbName: $dbName $z $x $y")
            val cursor = MbtilesDatabase.getTileBitmap(db, z, x, row.toInt())
            cursor?.use {
                if (it.moveToFirst()) {
                    val byteArray = it.getBlob(0)
                    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading MBTiles: $dbName")
        } finally {
            dbHelper.close()
        }
        return null
    } //else Timber.i( "not found: ${dbFile.path}")
    return null
}

data class GeojsonDatabaseItemModel(
    val id: UUID,
    val name: String,
    val path: String,
    val data: ByteArray?,
    var thumbnail: Bitmap?,
    val bounds: LatLngBounds?,
    val zoom: Int,
    val lastModifiedDate: String,
    var selected: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GeojsonDatabaseItemModel

        if (zoom != other.zoom) return false
        if (selected != other.selected) return false
        if (id != other.id) return false
        if (name != other.name) return false
        if (path != other.path) return false
        if (!data.contentEquals(other.data)) return false
        if (thumbnail != other.thumbnail) return false
        if (bounds != other.bounds) return false
        if (lastModifiedDate != other.lastModifiedDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = zoom
        result = 31 * result + selected.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + (thumbnail?.hashCode() ?: 0)
        result = 31 * result + (bounds?.hashCode() ?: 0)
        result = 31 * result + lastModifiedDate.hashCode()
        return result
    }
}

enum class GeojsonSnackbarSelection {
    Nothing,
    Import
}

data class GeojsonSnackbarData(val msg: String, val actionText: String?, val action: GeojsonSnackbarSelection?)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoboSnack(geojsonSnackbarData: GeojsonSnackbarData, finished: (action: GeojsonSnackbarSelection?) -> Unit) {
    ModalBottomSheet(onDismissRequest = { finished(GeojsonSnackbarSelection.Nothing) }) {
        Box(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) {
            Row(
                modifier = Modifier.border(
                    width = 2.dp,
                    color = Color.LightGray,
                    shape = RectangleShape
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = geojsonSnackbarData.msg,
                    Modifier
                        .weight(0.8f)
                        .padding(top = 8.dp, bottom = 8.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Blue
                )
                geojsonSnackbarData.actionText.let { text ->
                    TextButton(onClick = {
                        Timber.i("category: $text")
                        finished(geojsonSnackbarData.action)
                    }, modifier = Modifier.weight(0.2f)) {
                        if (text != null) {
                            Text(
                                text = text,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Blue
                            )
                        }
                    }
                }
            }
        }
    }
}

