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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
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
            val viewModel: MvtmakerViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            val clipboardManager = LocalClipboard.current
            LaunchedEffect(uiState.clipText) {
                uiState.clipText?.let {
                    Timber.i("clipText: $it")
                    val clipData = ClipData.newPlainText(NewMapAction.Import.name, it)
                    val clipEntry = ClipEntry(clipData)
                    clipboardManager.setClipEntry(clipEntry)
                    viewModel.setClipText(null)
                }
            }

            BackPressHandler {
                Timber.i("Back Press intercepted")
                setResult(RESULT_OK)
                finish()
            }

            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                Timber.i("Lifecycle.Event.ON_RESUME")
                viewModel.refreshFileData()
            }

            val context = LocalContext.current
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(
                    uiState.startLocation ?: LatLng(0.0, 0.0),
                    uiState.zoom.toFloat()
                )
            }
            val tileCenterLatLngState = rememberUpdatedMarkerState(position = uiState.tileCenterLatLng)
            val confirmationQuestion = stringResource(R.string.confirmation_question)

            LaunchedEffect(uiState.x, uiState.y) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    uiState.tileCenterLatLng, uiState.zoom.toFloat()
                )
                tileCenterLatLngState.position = uiState.tileCenterLatLng
            }

            LaunchedEffect(cameraPositionState.isMoving) {
                if (!cameraPositionState.isMoving) {
                    viewModel.setMapLoaded(true)
                }
            }

            MvtmakerScreen(
                uiState = uiState,
                cameraPositionState = cameraPositionState,
                tileCenterLatLngState = tileCenterLatLngState,
                onBack = {
                    setResult(RESULT_OK)
                    finish()
                },
                onImport = {
                    viewModel.setClipText(uiState.regionName)
                    FileImportActivity.launch(context, FileType.Mvt)
                },
                onShowGeoCoder = { viewModel.setShowGeoCoder(it) },
                onCreateMvt = { viewModel.setCreateMvtRegion(it) },
                onDeactivate = { viewModel.deactivateMvt() },
                onDeleteRequest = { viewModel.setMoboDeleteConfirmation(confirmationQuestion) },
                onActivate = { viewModel.activateMvt(it) },
                onConfirmDelete = { result ->
                    viewModel.setMoboDeleteConfirmation(null)
                    if (result) {
                        viewModel.deleteMvt(uiState.regionName)
                    }
                },
                onGeoCoderResult = { latLng -> viewModel.handleGeoCoderResult(latLng) },
                onShowListItems = { viewModel.setListItems(it) },
                onDriveEntrySelected = { viewModel.handleDriveEntrySelection(it) },
                onCoordinateChange = { x, y -> viewModel.updateCoordinates(x, y) },
                onMapLoaded = { viewModel.setMapLoaded(true); Timber.i("onMapLoaded true") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MvtmakerScreen(
    uiState: MvtmakerUiState,
    cameraPositionState: CameraPositionState,
    tileCenterLatLngState: MarkerState,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onShowGeoCoder: (Boolean) -> Unit,
    onCreateMvt: (String?) -> Unit,
    onDeactivate: () -> Unit,
    onDeleteRequest: () -> Unit,
    onActivate: (String) -> Unit,
    onConfirmDelete: (Boolean) -> Unit,
    onGeoCoderResult: (LatLng) -> Unit,
    onShowListItems: (Boolean) -> Unit,
    onDriveEntrySelected: (String) -> Unit,
    onCoordinateChange: (Int, Int) -> Unit,
    onMapLoaded: () -> Unit
) {
    val context = LocalContext.current
    val driveMap = DriveSharedLinks.Companion.MvtRegions().list

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back home"
                        )
                    }
                },
                title = { Text(text = stringResource(R.string.mvtmaker), fontSize = 14.sp) },
                actions = {
                    TextButton(onClick = onImport) {
                        Text(text = stringResource(R.string.import_title))
                    }
                    IconButton(onClick = { onShowGeoCoder(true) }) {
                        Icon(Icons.Outlined.Search, null)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(actions = {
                AnimatedVisibility(visible = !uiState.fileNames.contains(uiState.regionName)) {
                    TextButton(onClick = { onCreateMvt(uiState.regionName) }) {
                        Text(text = stringResource(R.string.create))
                    }
                }
                AnimatedVisibility(visible = driveMap.keys.contains(uiState.regionName) && !uiState.fileNames.contains(uiState.regionName)) {
                    Text(text = stringResource(R.string.available_on_drive))
                }
                AnimatedVisibility(visible = uiState.fileNames.contains(uiState.regionName)) {
                    Text(
                        text = if (uiState.prefMapname.contains(uiState.regionName))
                            stringResource(R.string._is_active, uiState.regionName)
                        else
                            stringResource(R.string._is_available, uiState.regionName)
                    )
                }
                AnimatedVisibility(visible = uiState.prefMapname.contains(uiState.regionName)) {
                    TextButton(onClick = onDeactivate) {
                        Text(text = stringResource(R.string.deactivate))
                    }
                }
                AnimatedVisibility(visible = uiState.fileNames.contains(uiState.regionName)) {
                    IconButton(onClick = onDeleteRequest) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                    }
                }
                AnimatedVisibility(visible = uiState.fileNames.contains(uiState.regionName) && !uiState.prefMapname.contains(uiState.regionName)) {
                    TextButton(onClick = { onActivate(uiState.regionName) }) {
                        Text(text = stringResource(R.string.activate))
                    }
                }
            })
        }
    ) { innerPadding ->
        uiState.moboDeleteConfirmation?.let {
            MoboConfirmation(it) { result -> onConfirmDelete(result) }
        }

        if (uiState.showGeoCoder) {
            GeoCoderLauncher(
                uiState.tileCenterLatLng,
                showInMap = { geoCoderResultName: String?, _: String?, latlng: org.maplibre.android.geometry.LatLng? ->
                    latlng?.let {
                        onGeoCoderResult(LatLng(it.latitude, it.longitude))
                    }
                    Timber.i("name $geoCoderResultName")
                }
            )
        }

        Column(
            modifier = Modifier.padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.listItems) {
                val mvtFolder = File(context.filesDir, Const.MVT_FOLDER)
                val driveItemsGrouped = remember(uiState.mvtRegionNames, uiState.prefMapname) {
                    uiState.mvtRegionNames.map { name ->
                        val f = File(mvtFolder, "$name${Const.MBTILES_EXT}")
                        val splits = name.split(Const.UNDERLINE, limit = 4)
                        MvtItemModel(
                            name = name,
                            path = "",
                            x = splits.getOrNull(1)?.toIntOrNull() ?: 0,
                            y = splits.getOrNull(2)?.toIntOrNull() ?: 0,
                            selected = uiState.prefMapname.contains(name),
                            exists = f.exists()
                        )
                    }.groupBy { it.x }
                }

                ListMvtDriveEntries(
                    currentMvtName = uiState.prefMapname.replace(Const.MBTILES_EXT, ""),
                    itemsGrouped = driveItemsGrouped,
                    onDismissRequest = { onShowListItems(false) },
                    import = {
                        onShowListItems(false)
                        FileImportActivity.launch(context, FileType.Mvt)
                    },
                    onItemClick = { mvtItemModel ->
                        onDriveEntrySelected(mvtItemModel.name)
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
                            text = "Center N: ${uiState.bounds.center.latitude.formatLatLngShort()}° " +
                                    "W: ${uiState.bounds.center.longitude.formatLatLngShort()}°",
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Row {
                        Text(modifier = Modifier.weight(0.25f), text = "N:${uiState.bounds.latitudeNorth.formatLatLngShort()}", fontFamily = FontFamily.Monospace)
                        Text(modifier = Modifier.weight(0.25f), text = "S:${uiState.bounds.latitudeSouth.formatLatLngShort()}", fontFamily = FontFamily.Monospace)
                        Text(modifier = Modifier.weight(0.25f), text = "W:${uiState.bounds.longitudeWest.formatLatLngShort()}", fontFamily = FontFamily.Monospace)
                        Text(modifier = Modifier.weight(0.25f), text = "E:${uiState.bounds.longitudeEast.formatLatLngShort()}", fontFamily = FontFamily.Monospace)
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
                    value = uiState.regionName,
                    onValueChange = { },
                    label = { Text(stringResource(R.string.region_name)) }
                )
                IconButton(
                    onClick = { onShowListItems(true) },
                    modifier = Modifier.weight(0.20f)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null)
                }
            }

            Row {
                val x = uiState.x
                val y = uiState.y
                IconButton(onClick = { onCoordinateChange(x + 1, y) }, modifier = Modifier.weight(0.20f)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowRight, contentDescription = null)
                }
                IconButton(onClick = { onCoordinateChange(x - 1, y) }, modifier = Modifier.weight(0.20f)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowLeft, contentDescription = null)
                }
                IconButton(onClick = { onCoordinateChange(x, y - 1) }, modifier = Modifier.weight(0.20f)) {
                    Icon(Icons.Outlined.ArrowDropUp, contentDescription = null)
                }
                IconButton(onClick = { onCoordinateChange(x, y + 1) }, modifier = Modifier.weight(0.20f)) {
                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                }
            }

            if (uiState.createMvtRegion != null) {
                val mvtBounds = GeoJsonUtils.tileToGmsBounds(
                    GeoJsonUtils.Companion.Tile(uiState.x, uiState.y, uiState.zoom)
                )
                val bbbikeUrl = GeoJsonUtils.getBbbikeUrl(
                    uiState.regionName, mvtBounds, "mbtiles-basic.zip"
                )
                Timber.i("bbbikeUrl: $bbbikeUrl")
                context.startActivity(Intent(Intent.ACTION_VIEW, bbbikeUrl))
                onCreateMvt(null)
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.width(320.dp).height(320.dp)
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
                                    else -> true
                                }
                            }
                        ),
                    cameraPositionState = cameraPositionState,
                    tileCenterLatLngState = tileCenterLatLngState,
                    tileX = uiState.x,
                    tileY = uiState.y,
                    zoom = uiState.zoom,
                    onMapLoaded = onMapLoaded,
                    onMapClick = { latLng ->
                        val tile = GeoJsonUtils.pointToTile(latLng.longitude, latLng.latitude, uiState.zoom.toDouble())
                        onCoordinateChange(tile.x, tile.y)
                    },
                    onMarkerClick = { region -> Timber.i("region $region") }
                )
                if (!uiState.isMapLoaded) {
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
