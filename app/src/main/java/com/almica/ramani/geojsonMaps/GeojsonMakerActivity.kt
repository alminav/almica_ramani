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
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.preference.PreferenceManager
import com.almica.ramani.Const
import com.almica.ramani.GeoCoderLauncher
import com.almica.ramani.Helpers.Companion.getGeojsonFolders
import com.almica.ramani.R
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.googlemaps.NewMapAction
import com.almica.ramani.googlemaps.UpdateCoordinateOverlay
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.utils.BackPressHandler
import com.almica.ramani.utils.DriveSharedLinks
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.GeoJsonUtils.Companion.createDefaultGeojsonOfflineStyle
import com.almica.ramani.utils.GeoJsonUtils.Companion.createGeojsonOfflineStyle
import com.almica.ramani.utils.MoboConfirmation
import com.almica.ramani.utils.MoboMessage
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
import timber.log.Timber
import java.io.File

/**
 * 14mar2026 changed to QGis geojson
 */
class GeojsonMakerActivity : ComponentActivity() {
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
                        // called after import
                        Timber.i("Lifecycle.State.RESUMED")
                        contentRefreshRequired = System.currentTimeMillis()
                    }
                }
            }
            val geojsonTilePrefix = "geojsonTile"
            val context = LocalContext.current
            //val localConfiguration = LocalConfiguration.current
            //val lifecycleOwner = LocalLifecycleOwner.current
            //val localDensity = LocalDensity.current

            var showGeoCoder by remember { mutableStateOf(false) }
            var moboMessage: String? by remember { mutableStateOf(null) }
            var moboDeleteConfirmation: String? by remember { mutableStateOf(null) }
            //var geojsonDatabaseItemModels by remember { mutableStateOf<List<GeojsonDatabaseItemModel>>(listOf())}
            var geojsonRegionNames by remember { mutableStateOf<ArrayList<String>>(arrayListOf()) }
            var geojsonMapNames by remember { mutableStateOf<ArrayList<String>>(arrayListOf()) }

            LaunchedEffect(key1 = contentRefreshRequired) {
                geojsonRegionNames = arrayListOf() // clear does not work
                geojsonMapNames = arrayListOf() // clear does not work
                val geojsonFolders = getGeojsonFolders(context)
                geojsonFolders.forEach { geojsonFolder ->
                    geojsonRegionNames.add(geojsonFolder.name.replace(Const.GEOJSON_PREFIX, ""))
                    geojsonMapNames.add(geojsonFolder.name)
                    Timber.i( "contentRefreshRequired geojsonRegionNames: ${geojsonRegionNames.size}")
                }
/*
                getGeojsonDatabaseEntities(context, localConfiguration, localDensity, null) { items ->
                    geojsonDatabaseItemModels = items
                    geojsonDatabaseItemModels.forEach { geojsonDatabaseItemModel ->
                        geojsonMapNames.add(geojsonDatabaseItemModel.name)
                    }
                    Timber.i( "geojsonNames: ${geojsonMapNames.size}")
                    val regions = createRegionArray(geojsonDatabaseItemModels)
                    regions.forEach { region ->
                        region?.let { geojsonRegionNames.add(it) }
                    }
                    Timber.i( "geojsonRegionNames: ${geojsonRegionNames.size}")
                }
*/
            }
            val zoom = 10 //12
            var isMapLoaded by remember { mutableStateOf(false) }

            val driveMap = DriveSharedLinks.Companion.GeojsonQgisMapRegions().list
                //DriveSharedLinks.Companion.GeojsonMapRegions().list
            var driveUrl: String? by remember { mutableStateOf(null) }
            val startLat = intent.getDoubleExtra(Const.EXTRA_LATITUDE, -1.0)
            val startLon = intent.getDoubleExtra(Const.EXTRA_LONGITUDE, -1.0)
            var tile : GeoJsonUtils.Companion.Tile? = null
            if (startLat >= 0 && startLon >= 0) {
                tile = GeoJsonUtils.pointToTile(startLon, startLat, zoom.toDouble())
            }
            var x by remember { mutableIntStateOf(tile?.x ?: 0) }
            var y by remember { mutableIntStateOf(tile?.y ?: 0) }
            var listItems by remember { mutableStateOf(false) }

            var geojsonMapName by remember { mutableStateOf("${geojsonTilePrefix}_${x}_${y}_${zoom}")}
            var bounds by remember {mutableStateOf(
                GeoJsonUtils.tileToBounds(GeoJsonUtils.Companion.Tile(x, y, zoom)))
            }
            var createGeojsonMap by remember { mutableStateOf<String?>(null) }
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(startLat, startLon), zoom.toFloat())
            }
            val tileCenterLatLng by remember {
                mutableStateOf(GeoJsonUtils.tileToGmsBounds(GeoJsonUtils.Companion.Tile(x, y, zoom)).center) }
            Timber.i("tileCenterLatLng $tileCenterLatLng")
            val tileCenterLatLngState = rememberUpdatedMarkerState(position = tileCenterLatLng)
            var tileRegion by remember { mutableStateOf(GeoJsonUtils.pointToTile(tileCenterLatLng.longitude,
                tileCenterLatLng.latitude, 10.0)) }
            var regionName by remember { mutableStateOf("${tileRegion.x}_${tileRegion.y}_${10}") }
            //val geojsonMapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
            //var presetMapName by remember { mutableStateOf(context.getString(R.string._is_available, geojsonMapName, "")) }
            val prefMapPath = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(Const.PREF_GEOJSON_FILEPATH, "")
            var prefMapname by remember { mutableStateOf(prefMapPath?.let {File(it).name} ?: "") }
            LaunchedEffect(key1 = x, key2 = y) {
                //Timber.i("x: $x y: $y")
                val tile = GeoJsonUtils.Companion.Tile(x, y, zoom)
                val tileCenterLatLng = GeoJsonUtils.tileCenter(tile)
                //Timber.i("tileCenterLatLng: ${tileCenterLatLng.latitude} ${tileCenterLatLng.longitude}")
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                        tileCenterLatLng, zoom.toFloat())
//                Timber.i("cameraPositionState: ${cameraPositionState.position.target.latitude} " +
//                            "${cameraPositionState.position.target.longitude}")
                bounds = GeoJsonUtils.tileToBounds(GeoJsonUtils.Companion.Tile(x, y, zoom))
                tileCenterLatLngState.position = GeoJsonUtils.tileToGmsBounds(GeoJsonUtils.Companion.Tile(x, y, zoom)).center
//                Timber.i("bounds: $bounds")
                geojsonMapName = "${geojsonTilePrefix}_${x}_${y}_${zoom}"
                tileRegion = GeoJsonUtils.pointToTile(tileCenterLatLng.longitude, tileCenterLatLng.latitude, 10.0)
                regionName = "${tileRegion.x}_${tileRegion.y}_${10}"
                //presetMapName = context.getString(R.string._is_available, geojsonMapName, "")
            }
            Timber.i("prefMapname: $prefMapname")
            Scaffold(topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                setResult(RESULT_OK)
                                finish()
                                //ScreenRouter.navigateHome()
                                Timber.i("navigateHome")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go back home"
                            )
                        }
                    }, title = {
                        Text(text = stringResource(R.string.geojson_qgis), fontSize = 14.sp)
                    }, actions = {
                        TextButton(
                            onClick = {
                                clipText = regionName
                                Timber.i("import clipText: $clipText")
                                FileImportActivity.launch(context, FileType.GeojsonQgisZip)
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
                    AnimatedVisibility(visible = !geojsonMapNames.contains(geojsonMapName)) {
                        TextButton(
                            onClick = { createGeojsonMap = geojsonMapName }
                        ) {
                            Text(text = stringResource(R.string.bbbike))
                        }
                    }
                    AnimatedVisibility(visible = driveMap.keys.contains(regionName) && !geojsonMapNames.contains(geojsonMapName)) {
                        TextButton(
                            onClick = {
                                Timber.i("download from drive")
                                driveUrl = driveMap[regionName]
                            }
                        ) {
                            Text(text = stringResource(R.string.download_from_drive))
                        }
                    }

                    AnimatedVisibility(visible = geojsonMapNames.contains(geojsonMapName)) {
                        Timber.i("prefMapname: $prefMapname geojsonMapName: $geojsonMapName" )
                        Text(text = if (prefMapname == geojsonMapName)
                            context.getString(R.string._is_active, geojsonMapName.replace(Const.GEOJSON_PREFIX, ""))
                        else
                            context.getString(R.string._is_available, geojsonMapName.replace(Const.GEOJSON_PREFIX, "")))
                    }
                    AnimatedVisibility(visible = prefMapname.contains(geojsonMapName)) {
                        TextButton(
                            onClick = {
                                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                                prefs.edit { remove(Const.PREF_GEOJSON_FILEPATH) }
                                createDefaultGeojsonOfflineStyle(context)
                                Timber.i("$geojsonMapName deactivated")
                                prefMapname = ""
                                contentRefreshRequired = System.currentTimeMillis()
                            }
                        ) {
                            Text(text = stringResource(R.string.deactivate))
                        }
                    }
                    AnimatedVisibility(visible = geojsonMapNames.contains(geojsonMapName)) {
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
                    AnimatedVisibility(visible = geojsonMapNames.contains(geojsonMapName) &&
                            !prefMapname.contains(geojsonMapName)) {
                        TextButton(
                            onClick = {
                                val rootFolder = context.filesDir
                                val geojsonRootFolder = File(rootFolder, Const.GEOJSON_ROOT_FOLDER)
                                val geojsonFolder = File(geojsonRootFolder, geojsonMapName)
                                val prefGeojsonFolderPath = geojsonFolder.path
                                val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                                prefGeojsonFolderPath.let {
                                    preferences.edit {
                                        putString(Const.PREF_GEOJSON_FILEPATH, geojsonFolder.path)
                                    }
                                    val geojsonFolderName = File(it).name
                                    createGeojsonOfflineStyle(context, geojsonFolderName)
                                }
                                prefMapname = geojsonMapName
                                Timber.i("prefMapname: $prefMapname")
                                moboMessage = context.getString(R.string._activated, geojsonMapName)
                            }
                        ) {
                            Text(text = context.getString(R.string.activate))
                        }
                    }
                })
            }) {innerPadding ->
                /**
                 * google drive deep links
                 * 22mar2026 works only with zip files, but how long? ;-(
                 */
                driveUrl?.let {
                    Timber.i("driveUrl: $driveUrl")
                    val browserIntent = Intent(Intent.ACTION_VIEW, driveUrl!!.toUri())
                    context.startActivity(browserIntent)
                    driveUrl = null
                }
                moboMessage?.let {
                    MoboMessage(moboMessage!!) {
                        moboMessage = null
                    }
                }
                moboDeleteConfirmation?.let {
                    MoboConfirmation(moboDeleteConfirmation!!) {result ->
                        moboDeleteConfirmation = null
                        if (result) {
                            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                            val prefGeojsonFolderPath = preferences.getString(
                                Const.PREF_GEOJSON_FILEPATH, null)
                            Timber.i("prefGeojsonFolderPath: $prefGeojsonFolderPath")
                            val rootFolder = context.filesDir
                            val geojsonRootFolder = File(rootFolder, Const.GEOJSON_ROOT_FOLDER)
                            val geojsonFolder = File(geojsonRootFolder, geojsonMapName)
                            Timber.i("geojsonFolder: ${geojsonFolder.path}")
                            if (geojsonFolder.isNotNull()) {
                                geojsonRegionNames.remove(geojsonMapName)
                                geojsonFolder.deleteRecursively()
                                if (geojsonFolder.path == prefGeojsonFolderPath) {
                                    preferences.edit { remove(Const.PREF_GEOJSON_FILEPATH) }
                                    createDefaultGeojsonOfflineStyle(context)
                                }
                                if (prefMapname == geojsonMapName)
                                    prefMapname = ""
                                contentRefreshRequired = System.currentTimeMillis()
                            }
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
                                latlng.longitude, latlng.latitude, 12.0)
                            x = tileMap.x
                            y = tileMap.y
                        }
                        Timber.i("name $geoCoderResultName")

                        showGeoCoder = false
                    })
                }

                Column(modifier = Modifier.padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    val regionNames = arrayListOf<String>()
                    driveMap.keys.forEach { key ->
                        regionNames.add(key)
                    }
                    if (listItems) {
                        DropDownGeojsonRegions(regionNames, prefMapname) { regionName_ ->
                            listItems = false
                            Timber.i("regionName: $regionName_")
                            regionName_?.apply {
                                val splits = this.split(Const.UNDERLINE, limit = 3)
                                val tile = GeoJsonUtils.Companion.Tile(
                                    splits[0].toInt(),
                                    splits[1].toInt(),
                                    splits[2].toInt()
                                )
                                val bounds = GeoJsonUtils.tileToBounds(tile)
                                val tileMap = GeoJsonUtils.pointToTile(bounds.center.longitude, bounds.center.latitude, zoom.toDouble())
                                x = tileMap.x
                                y = tileMap.y
                            }
                        }
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
                            value = regionName, onValueChange = {},
                            label = { Text(stringResource(R.string.region_name)) })
                        IconButton(
                            onClick = {
                                listItems = true
                            },
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
                                geojsonMapName = "${geojsonTilePrefix}_${x}_${y}_${zoom}"
                                Timber.i("regionName: $geojsonMapName")
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
                                geojsonMapName = "${geojsonTilePrefix}_${x}_${y}_${zoom}"
                                Timber.i("regionName: $geojsonMapName")
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
                            geojsonMapName = "${geojsonTilePrefix}_${x}_${y}_${zoom}"
                            Timber.i("regionName: $geojsonMapName")
                            bounds = GeoJsonUtils.tileToBounds(GeoJsonUtils.Companion.Tile(x, y, zoom))
                        }, modifier = Modifier
                            .weight(0.20f)) {
                            Icon(Icons.Outlined.ArrowDropUp, contentDescription = null)
                        }

                        IconButton(
                            onClick = {
                                y += 1
                                isMapLoaded = true
                                geojsonMapName = "${geojsonTilePrefix}_${x}_${y}_${zoom}"
                                Timber.i("regionName: $geojsonMapName")
                                bounds = GeoJsonUtils.tileToBounds(GeoJsonUtils.Companion.Tile(x, y, zoom))
                            },
                            modifier = Modifier
                                .weight(0.20f)
                        ) {
                            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                        }
                    }

                    if (createGeojsonMap != null) {
                        val bounds = GeoJsonUtils.tileToGmsBounds(
                            GeoJsonUtils.Companion.Tile(x, y, zoom))
                        val bbbikeUrl = GeoJsonUtils.getBbbikeUrl(
                            "pmtiles_${x}_${y}_${zoom}",
                            bounds, "pmtiles-basic.zip"
                        )
//                        val bbbikeUrl = GeoJsonUtils.getBbbikeUrl(
//                            "geojsonTile_${x}_${y}_${zoom}",
//                            bounds, "geojson.xz")
                        Timber.i("bbbikeUrl: $bbbikeUrl")
                        val browserIntent =
                            Intent(Intent.ACTION_VIEW, bbbikeUrl)
                        context.startActivity(browserIntent)
                        createGeojsonMap = null
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
                            tileCenterLatLngState = tileCenterLatLngState, geojsonTilePrefix,
                            driveMap.keys.contains(regionName),
                            x, y, zoom,
                            onMapLoaded = {
                                isMapLoaded = true
                                Timber.i("onMapLoaded")
                            }, onMapClick = { latLng ->
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
private fun GoogleMapViewInColumn(
    modifier: Modifier,
    cameraPositionState: CameraPositionState,
    tileCenterLatLngState: MarkerState,
    prefix: String,
    state: Boolean,
    tileX: Int,
    tileY: Int,
    zoom: Int,
    onMapLoaded: () -> Unit,
    onMapClick: (LatLng) -> Unit,
    onMarkerClick: (String) -> Unit
) {
//    val context = LocalContext.current
//    Timber.i("cameraPositionState: ${cameraPositionState.position.target}")
    val regionName = "${prefix}_${tileX}_${tileY}_${zoom}"
    Timber.i("tileX: $tileX tileY: $tileY regionName: $regionName state: $state")
    // val tileCenterLatLngState = rememberUpdatedMarkerState(position = tileCenterLatLng)
//    val defaultCameraPosition = CameraPosition.fromLatLngZoom(tileCenterLatLng, zoom.toFloat())
//    Timber.i("defaultCameraPosition: ${defaultCameraPosition.target}")
//    val cameraPositionState = rememberCameraPositionState()
//    cameraPositionState.position = CameraPosition.fromLatLngZoom(tileCenterLatLng, zoom.toFloat())
//    val cameraPositionState = rememberCameraPositionState {
//        position = CameraPosition.fromLatLngZoom(tileCenterLatLng, zoom.toFloat())//defaultCameraPosition
//        Timber.i("position: ${position.target.latitude} ${position.target.longitude}")
//    }
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
            title = regionName,
            onClick = markerClick,
            draggable = false
        ) {
            Text(it.title ?: "Title", color = if (state) Color.Green else Color.Red)
        }
    }
}
@Composable
private fun DropDownGeojsonRegions(
    geojsonRegions: ArrayList<String>,
    prefMapname: String,
    select: (String?) -> Unit
) {
    DropdownMenu(
        expanded = geojsonRegions.isNotEmpty(),
        onDismissRequest = { select(null) }) {
        for (geojsonRegion in geojsonRegions) {
            if (geojsonRegion.isNotEmpty())
                DropdownMenuItem(
                    text = { Text(text = if (prefMapname.contains(geojsonRegion)) "$geojsonRegion ${Const.UC_CHECKMARK}" else geojsonRegion,
                        color = Color.Black) },
                    onClick = {
                        val name = geojsonRegion.replace(Const.MBTILES_EXT, "")
                        //getBitmapForRegion(context, regionName, true)
                        Timber.i( "select: $name")
                        select(name)
                    }
                )
        }
    }
}