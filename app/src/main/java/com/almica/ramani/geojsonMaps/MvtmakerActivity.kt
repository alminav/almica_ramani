package com.almica.ramani.geojsonMaps

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowLeft
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.preference.PreferenceManager
import com.almica.ramani.Const
import com.almica.ramani.MvtItemModel
import com.almica.ramani.GeoCoderLauncher
import com.almica.ramani.ListMvtDriveEntries
import com.almica.ramani.R
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.googlemaps.NewMapAction
import com.almica.ramani.googlemaps.UpdateCoordinateOverlay
import com.almica.ramani.utils.BackPressHandler
import com.almica.ramani.utils.DriveSharedLinks
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.MoboConfirmation
import com.almica.ramani.utils.formatLatLngShort
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber
import java.io.File
import java.io.FileFilter

class MvtmakerActivity : ComponentActivity() {
    // ToDo KI improve
    @SuppressLint("LocalContextGetResourceValueCall")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
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
            BackPressHandler {
                Timber.i("Back Press intercepted")
                setResult(RESULT_OK)
                finish()
            }
            val zoom = 9
            val context = LocalContext.current
            var contentRefreshRequired by remember { mutableLongStateOf(0L) }
            val lifecycleOwner = LocalLifecycleOwner.current
            val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

            LaunchedEffect(lifecycleState) {
                // Do something with your state
                // You may want to use DisposableEffect or other alternatives
                // instead of LaunchedEffect
                //Timber.i("$lifecycleState")
                when (lifecycleState) {
                    Lifecycle.State.DESTROYED -> {}
                    Lifecycle.State.INITIALIZED -> {}
                    Lifecycle.State.CREATED -> {}
                    Lifecycle.State.STARTED -> {}
                    Lifecycle.State.RESUMED -> {
                        // refresh after import
                        Timber.i("Lifecycle.State.RESUMED")
                        contentRefreshRequired = System.currentTimeMillis()
                    }
                }
            }

            var isMapLoaded by remember { mutableStateOf(false) }
            var showGeoCoder by remember { mutableStateOf(false) }
            val rootFolder = LocalContext.current.filesDir
            val mvtRootFolder = File(rootFolder, Const.MVT_FOLDER)
            val fileFilter = FileFilter { file: File? -> file?.name?.endsWith(Const.MBTILES_EXT) == true &&
                    !file.name.contains(Const.JOURNAL)
            }
            var fileNames by remember { mutableStateOf<List<String>>(listOf()) }
            LaunchedEffect(contentRefreshRequired) {
                val files = mvtRootFolder.listFiles(fileFilter) as Array<File>
                fileNames = List(files.size) { i ->
                    files[i].name.replace(Const.MBTILES_EXT, "")
                }
            }

            val prefMapPath = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Const.PREF_MVT_FILEPATH, "")
            var prefMapname by remember { mutableStateOf(prefMapPath?.let {File(it).name} ?: "") }

            var moboDeleteConfirmation: String? by remember { mutableStateOf(null) }
            val driveMap = DriveSharedLinks.Companion.MvtRegions().list
            //var driveUrl: String? by remember { mutableStateOf(null) }
            val startLat = intent.getDoubleExtra(Const.EXTRA_LATITUDE, -1.0)
            val startLon = intent.getDoubleExtra(Const.EXTRA_LONGITUDE, -1.0)
            var tile : GeoJsonUtils.Companion.Tile? = null
            if (startLat >= 0 && startLon >= 0) {
                tile = GeoJsonUtils.pointToTile(startLon, startLat, zoom.toDouble())
            }
            var x by remember { mutableIntStateOf(tile?.x ?: 0) }
            var y by remember { mutableIntStateOf(tile?.y ?: 0) }
            var listItems by remember { mutableStateOf(false) }

            var regionName by remember { mutableStateOf("mvt_${x}_${y}_${zoom}")}
            val regionNames = arrayListOf<String>()
            driveMap.keys.forEach { key -> regionNames.add(key) }
            regionNames.sort()
            var mvtRegionNames by remember { mutableStateOf(regionNames) }
            //val splits = regionName.split(Const.UNDERLINE, ".", limit = 6)
            /*
                            0 = "tile"
                            1 = "1082"
                            2 = "672"
                            3 = "11"
                            4 = "OpenTopo"
                            5 = "mbtiles"
            */
            var bounds by remember {mutableStateOf(
                GeoJsonUtils.tileToBounds(GeoJsonUtils.Companion.Tile(x, y, zoom)))
            }
            var createMvtRegion by remember { mutableStateOf<String?>(null) }
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(startLat, startLon), zoom.toFloat())
            }
            val tileCenterLatLng by remember {
                mutableStateOf(GeoJsonUtils.tileToGmsBounds(GeoJsonUtils.Companion.Tile(x, y, zoom)).center) }
            Timber.i("tileCenterLatLng $tileCenterLatLng")
            val tileCenterLatLngState = rememberUpdatedMarkerState(position = tileCenterLatLng)
            //tileCenterLatLngState.showInfoWindow()

            LaunchedEffect(key1 = x, key2 = y) {
            //LaunchedEffect(key1 = regionName) {
                Timber.i("x: $x y: $y")
                val tile = GeoJsonUtils.Companion.Tile(x, y, zoom)
                val tileCenterLatLng = GeoJsonUtils.tileCenter(tile)
                Timber.i("tileCenterLatLng: ${tileCenterLatLng.latitude} ${tileCenterLatLng.longitude}")
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                        tileCenterLatLng, zoom.toFloat())
                Timber.i("cameraPositionState: ${cameraPositionState.position.target.latitude} " +
                            "${cameraPositionState.position.target.longitude}")
                bounds = GeoJsonUtils.tileToBounds(GeoJsonUtils.Companion.Tile(x, y, zoom))
                tileCenterLatLngState.position = GeoJsonUtils.tileToGmsBounds(GeoJsonUtils.Companion.Tile(x, y, zoom)).center
                Timber.i("bounds: $bounds")
                regionName = "${Const.MVT_PREFIX}${x}_${y}_${zoom}"
            }

            Scaffold(topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                setResult(RESULT_OK)
                                finish()
                                //ScreenRouter.navigateHome()
                                Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: navigateHome")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go back home"
                            )
                        }
                    }, title = {
                        Text(text = stringResource(R.string.mvtmaker), fontSize = 14.sp)
                    }, actions = {
                        TextButton(
                            onClick = {
                                Timber.i("import")
                                clipText = regionName
                                Timber.i("import clipText: $clipText")

                                FileImportActivity.launch(context, FileType.Mvt)
                            }
                        ) {
                            Text(text = stringResource(R.string.import_title))
                        }

                        IconButton(onClick = {
                            showGeoCoder = true
                        }) {Icon(Icons.Outlined.Search, null) }
                    }
                )
            }, bottomBar = {
                BottomAppBar(//modifier = Modifier.height(56.dp).padding(bottom = 208.dp),
                    actions = {
                    AnimatedVisibility(visible = !fileNames.contains(regionName)) {
                        TextButton(
                            onClick = {
                                createMvtRegion = regionName
                            }
                        ) {
                            Text(text = stringResource(R.string.create))
                        }
                    }
                    AnimatedVisibility(visible = driveMap.keys.contains(regionName) && !fileNames.contains(regionName)) {
                        Text(text = stringResource(R.string.available_on_drive))
                    }

                    AnimatedVisibility(visible = fileNames.contains(regionName)) {
                        Text(text =
                            if (prefMapname.contains(regionName))
                                context.getString(R.string._is_active, regionName)
                            else
                                context.getString(R.string._is_available, regionName))
                    }
                    AnimatedVisibility(visible = prefMapname.contains(regionName)) {
                        TextButton(
                            onClick = {
                                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                                prefs.edit { remove(Const.PREF_MVT_FILEPATH) }
                                Timber.i("$regionName deactivated")
                                prefMapname = ""
                                contentRefreshRequired = System.currentTimeMillis()
                            }
                        ) {
                            Text(text = stringResource(R.string.deactivate))
                        }
                    }
                    AnimatedVisibility(visible = fileNames.contains(regionName)) {
                        IconButton(
                            onClick = {
                                moboDeleteConfirmation = context.getString(R.string.confirmation_question)
                            }
                        ) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = null
                            )
                        }
                    }
                    AnimatedVisibility(visible = fileNames.contains(regionName) && !prefMapname.contains(regionName)) {
                        TextButton(
                            onClick = {
                                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                                val rootFolder = context.filesDir
                                val mvtRootFolder = File(rootFolder, Const.MVT_FOLDER)
                                val f = File(mvtRootFolder, regionName.plus(Const.MBTILES_EXT))
                                if (f.exists()) {
                                    prefs.edit { putString(Const.PREF_MVT_FILEPATH, f.path) }
                                    prefMapname = f.name
                                }
                            }
                        ) {
                            Text(text = context.getString(R.string.activate))
                        }
                    }
                })
            }) {innerPadding ->
/*
                driveUrl?.let {
                    Timber.i("driveUrl: $driveUrl")
                    val browserIntent = Intent(Intent.ACTION_VIEW, driveUrl!!.toUri())
                    context.startActivity(browserIntent)
                    driveUrl = null
                }
 */
                moboDeleteConfirmation?.let {
                    MoboConfirmation(moboDeleteConfirmation!!) { result ->
                        moboDeleteConfirmation = null
                        if (result) {
                            val rootFolder = context.filesDir
                            val mvtRootFolder = File(rootFolder, Const.MVT_FOLDER)
                            val f = File(mvtRootFolder, regionName.plus(Const.MBTILES_EXT))
                            val b = f.delete()
                            Timber.i("${f.path} delete: $b")
                            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                            if (prefMapname.contains(regionName)) {
                                prefs.edit { remove(Const.PREF_MVT_FILEPATH) }
                                prefMapname = ""
                            }

                            contentRefreshRequired = System.currentTimeMillis()
                        }
                    }
                }
                if (showGeoCoder) {
                    //GeoCoderComposeScreen(cameraPosition.value.target?.let {
                    GeoCoderLauncher (tileCenterLatLng.let {
                        LatLng(
                            it.latitude,
                            it.longitude
                        )
                    }, showInMap = { geoCoderResultName: String?, _: String?, latlng: org.maplibre.android.geometry.LatLng? ->
                        latlng?.let {
                            val tileMap = GeoJsonUtils.pointToTile(
                                latlng.longitude, latlng.latitude, 9.0)
                            x = tileMap.x
                            y = tileMap.y
                        }
                        Timber.i("name $geoCoderResultName")

                        showGeoCoder = false
                    })
                }
                Column(modifier = Modifier.padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    if (listItems) {
                        val mvtFolder = File(context.filesDir, Const.MVT_FOLDER)
                        val driveItemsGrouped = remember(mvtRegionNames, prefMapname) {
                            mvtRegionNames.map { name ->
                                val f = File(mvtFolder, "$name${Const.MBTILES_EXT}")
                                val splits = name.split(Const.UNDERLINE, limit = 4)
                                MvtItemModel(
                                    name = name,
                                    path = "",
                                    x = splits.getOrNull(1)?.toIntOrNull() ?: 0,
                                    y = splits.getOrNull(2)?.toIntOrNull() ?: 0,
                                    selected = prefMapname.contains(name),
                                    exists = f.exists()
                                )
                            }.groupBy { it.x }
                        }

                        ListMvtDriveEntries(
                            currentMvtName = prefMapname.replace(Const.MBTILES_EXT, ""),
                            itemsGrouped = driveItemsGrouped,
                            onDismissRequest = { listItems = false },
                            import = {
                                listItems = false
                                FileImportActivity.launch(context, FileType.Mvt)
                            },
                            onItemClick = { mvtItemModel ->
                                listItems = false
                                mvtItemModel.let {
                                    try {
                                        val splits = it.name.split(Const.UNDERLINE, limit = 6)
                                        x = splits[1].toInt()
                                        y = splits[2].toInt()
                                        regionName = it.name
                                    } catch (e: Exception) {
                                        Timber.i("$it.name doesn't fit the pattern mvt_x_y_z.mbtiles")
                                    }
                                }
                            }
                        )
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
                                Text(
                                    text = "Center N: ${bounds.center.latitude.formatLatLngShort()}° " +
                                            "W: ${bounds.center.longitude.formatLatLngShort()}°",
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Row {
                                Text(
                                    modifier = Modifier.weight(0.25f),
                                    text = "N:${bounds.latitudeNorth.formatLatLngShort()}",
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    modifier = Modifier.weight(0.25f),
                                    text = "S:${bounds.latitudeSouth.formatLatLngShort()}",
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    modifier = Modifier.weight(0.25f),
                                    text = "W:${bounds.longitudeWest.formatLatLngShort()}",
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    modifier = Modifier.weight(0.25f),
                                    text = "E:${bounds.longitudeEast.formatLatLngShort()}",
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.padding(start = 5.dp, end = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.weight(0.8f),
                            readOnly = true,
                            value = regionName, onValueChange = { regionName = it },
                            label = { Text(stringResource(R.string.region_name)) })
                        IconButton(
                            onClick = {
                                listItems = true
                                mvtRegionNames = regionNames },
                            modifier = Modifier
                                .weight(0.20f)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.List,
                                contentDescription = null
                            )
                        }
                    }
                    Row {
                        IconButton(
                            onClick = { x += 1
                                isMapLoaded = true
                                regionName = "${Const.MVT_PREFIX}${x}_${y}_${zoom}"
                                Timber.i("regionName: $regionName")
                                bounds = GeoJsonUtils.tileToBounds(GeoJsonUtils.Companion.Tile(x, y, zoom)) },
                            modifier = Modifier
                                .weight(0.20f)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowRight,
                                contentDescription = null
                            )
                        }

                        IconButton(
                            onClick = { x -= 1
                                isMapLoaded = true
                                regionName = "${Const.MVT_PREFIX}${x}_${y}_${zoom}"
                                Timber.i("regionName: $regionName")
                                bounds = GeoJsonUtils.tileToBounds(GeoJsonUtils.Companion.Tile(x, y, zoom)) },
                            modifier = Modifier
                                .weight(0.20f)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowLeft,
                                contentDescription = null
                            )
                        }

                        IconButton(onClick = {
                            y -= 1
                            isMapLoaded = true
                            regionName = "${Const.MVT_PREFIX}${x}_${y}_${zoom}"
                            Timber.i("regionName: $regionName")
                            bounds = GeoJsonUtils.tileToBounds(GeoJsonUtils.Companion.Tile(x, y, zoom))
                        }, modifier = Modifier
                            .weight(0.20f)) {
                            Icon(Icons.Outlined.ArrowDropUp, contentDescription = null)
                        }

                        IconButton(
                            onClick = {
                                y += 1
                                isMapLoaded = true
                                regionName = "${Const.MVT_PREFIX}${x}_${y}_${zoom}"
                                Timber.i("regionName: $regionName")
                                bounds = GeoJsonUtils.tileToBounds(GeoJsonUtils.Companion.Tile(x, y, zoom))
                            },
                            modifier = Modifier
                                .weight(0.20f)
                        ) {
                            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                        }
                    }

                    if (createMvtRegion != null) {
                        val mvtBounds = GeoJsonUtils.tileToGmsBounds(
                            GeoJsonUtils.Companion.Tile(x, y, zoom))
                        val bbbikeUrl = GeoJsonUtils.getBbbikeUrl(
                            "${Const.MVT_PREFIX}${x}_${y}_${zoom}",
                            mvtBounds, "mbtiles-basic.zip"
                        )
                        Timber.i("bbbikeUrl: $bbbikeUrl")
                        val browserIntent =
                            Intent(Intent.ACTION_VIEW, bbbikeUrl)
                        context.startActivity(browserIntent)
                        createMvtRegion = null
                    }
                    Box(contentAlignment = Alignment.Center,
                        modifier = Modifier
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
                            x, y, zoom,
                            onMapLoaded = {
                                isMapLoaded = true
                                Timber.i("onMapLoaded regular feedback")
                            }, onMapClick = { latLng ->
                                isMapLoaded = true
                                val tile = GeoJsonUtils.pointToTile(latLng.longitude, latLng.latitude, zoom.toDouble())
                                Timber.i("tile $tile")
                                x = tile.x
                                y = tile.y
                            }, onMarkerClick = { region ->
                                Timber.i("region $region")
                            }
                        )
                        if (!isMapLoaded) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.background)
                                    .wrapContentSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleMapViewInColumn(
    modifier: Modifier,
    cameraPositionState: CameraPositionState,
    tileCenterLatLngState: MarkerState,
    tileX: Int,
    tileY: Int,
    zoom: Int,
    onMapLoaded: () -> Unit,
    onMapClick: (LatLng) -> Unit,
    onMarkerClick: (String) -> Unit
) {
    Timber.i("cameraPositionState: ${cameraPositionState.position.target}")
    val regionName = "${Const.MVT_PREFIX}${tileX}_${tileY}_${zoom}"
    Timber.i("tileX: $tileX tileY: $tileY regionName: $regionName")

    Timber.i("cameraPositionState: ${cameraPositionState.position.target}")
    var uiSettings by remember { mutableStateOf(MapUiSettings(compassEnabled = false)) }
    var mapProperties by remember {
        mutableStateOf(MapProperties(mapType = MapType.NORMAL))
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
        onMapLoaded = onMapLoaded,
        onMapClick = { latLng ->
            onMapClick(latLng)
        }
    ) {
        UpdateCoordinateOverlay(MapType.NORMAL.name)
        // Drawing on the map is accomplished with a child-based API
        val markerClick: (Marker) -> Boolean = {
            Timber.i("${it.title} was clicked")
            cameraPositionState.projection?.let { projection ->
                Timber.i("projection: $projection")
            }
            it.title?.let { p1 -> onMarkerClick(p1) }
            false
        }

        MarkerInfoWindowContent(
            state = tileCenterLatLngState,
            title = regionName.replace(Const.MVT_PREFIX, ""),
            onClick = markerClick,
            draggable = false
        ) {
            Text(it.title ?: "Title", color = Color.Red)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GoogleMapViewInColumnPreview() {
    val zoom = 9
    val x = 1082
    val y = 672
    val tileCenterLatLng = LatLng(-1.0, -1.0) // Sample LatLng
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(tileCenterLatLng, zoom.toFloat())
    }
    val tileCenterLatLngState = rememberUpdatedMarkerState(position = tileCenterLatLng)

    RamaniTheme {
        GoogleMapViewInColumn(
            modifier = Modifier
                .width(320.dp)
                .height(320.dp),
            cameraPositionState = cameraPositionState,
            tileCenterLatLngState = tileCenterLatLngState,
            tileX = x,
            tileY = y,
            zoom = zoom,
            onMapLoaded = {},
            onMapClick = {},
            onMarkerClick = {}
        )
    }
}

@Composable
private fun DropDownMvtRegions(mvtRegions: ArrayList<String>, prefMapname: String, select: (String?) -> Unit) {
    //val state = rememberScrollState()
    //LaunchedEffect(Unit) { state.animateScrollTo(2000) }
    DropdownMenu( //scrollState = state,
        expanded = mvtRegions.isNotEmpty(),
        onDismissRequest = { select(null) }) {
        for (mvtRegion in mvtRegions) {
            DropdownMenuItem(
                text = { Text(text = if (prefMapname.contains(mvtRegion)) "$mvtRegion ${Const.UC_CHECKMARK}" else mvtRegion,
                    color = Color.Black)},
                onClick = {
                    val name = mvtRegion.replace(Const.MBTILES_EXT, "")
                    //getBitmapForRegion(context, regionName, true)
                    Timber.i( "select: $name")
                    select(name)
                }
            )
        }
    }
}
