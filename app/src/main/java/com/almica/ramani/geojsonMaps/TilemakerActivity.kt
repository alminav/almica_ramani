package com.almica.ramani.geojsonMaps

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.almica.ramani.Const
import com.almica.ramani.GeoCoderLauncher
import com.almica.ramani.ListRasterDriveEntries
import com.almica.ramani.R
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.googlemaps.CreateMbTileRegion
import com.almica.ramani.googlemaps.MaptypeMenu
import com.almica.ramani.googlemaps.NewMapAction
import com.almica.ramani.googlemaps.UpdateCoordinateOverlay
import com.almica.ramani.ui.theme.RamaniTheme
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
import timber.log.Timber
import java.io.File

class TilemakerActivity : ComponentActivity() {
    private val viewModel: TilemakerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TilemakerScreen(viewModel = viewModel, onFinish = {
                setResult(RESULT_OK)
                finish()
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TilemakerScreen(
    viewModel: TilemakerViewModel,
    onFinish: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val importTitle = stringResource(R.string.import_title)
    val deleteConfirmation = stringResource(R.string.confirmation_question)

    LaunchedEffect(uiState.clipText) {
        uiState.clipText?.let { text ->
            Timber.i("clipText: $text")
            val clipData = ClipData.newPlainText(NewMapAction.Import.name, text)
            val clipEntry = ClipEntry(clipData)
            clipboardManager.setClipEntry(clipEntry)
            viewModel.setClipText(null)
        }
    }

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            Timber.i("Lifecycle.State.RESUMED")
            viewModel.refreshFileData()
        }
    }

    BackHandler {
        Timber.i("Back Handler intercepted")
        onFinish()
    }

    Scaffold(
        topBar = {
            TilemakerTopBar(
                onBack = onFinish,
                onMapTypeClick = { viewModel.setShowDropDownRasterMaptype(true) },
                onSearchClick = { viewModel.setShowGeoCoder(true) }
            )
        },
        bottomBar = {
            TilemakerBottomBar(
                uiState = uiState,
                onCreate = { viewModel.startCreateMbTile(uiState.regionName) },
                onImport = {
                    viewModel.setClipText(uiState.regionName)
                    context.startActivity(
                        Intent(context, FileImportActivity::class.java)
                            .setAction(importTitle)
                            .putExtra(Const.EXTRA_FILETYPE, FileType.MbTiles.name)
                    )
                },
                onActivate = { viewModel.toggleTileActivation(uiState.regionName, true) },
                onDeactivate = { viewModel.toggleTileActivation(uiState.regionName, false) },
                onDelete = { viewModel.setMoboDeleteConfirmation(deleteConfirmation) }
            )
        }
    ) { innerPadding ->
        TilemakerContent(
            innerPadding = innerPadding,
            uiState = uiState,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TilemakerTopBar(
    onBack: () -> Unit,
    onMapTypeClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "finish"
                )
            }
        },
        title = {
            Text(text = stringResource(R.string.raster_maps), fontSize = 14.sp)
        },
        actions = {
            TextButton(onClick = onMapTypeClick) {
                Text(stringResource(R.string.map_type))
            }
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Outlined.Search, null)
            }
        }
    )
}

@Composable
fun TilemakerBottomBar(
    uiState: TilemakerUiState,
    onCreate: () -> Unit,
    onImport: () -> Unit,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onDelete: () -> Unit
) {
    BottomAppBar(
        actions = {
            AnimatedVisibility(visible = !uiState.fileNames.contains(uiState.regionName)) {
                TextButton(onClick = onCreate) {
                    Text(text = stringResource(R.string.create))
                }
            }
            AnimatedVisibility(visible = uiState.canImportFromDrive) {
                TextButton(onClick = onImport) {
                    Text(text = stringResource(R.string.import_from_drive))
                }
            }
            AnimatedVisibility(visible = uiState.isTileActive) {
                Text(text = stringResource(R.string._is_active, uiState.regionName.replace(Const.MBTILES_EXT, "")))
            }
            AnimatedVisibility(
                visible = uiState.fileNames.contains(uiState.regionName) && !uiState.isTileActive
            ) {
                TextButton(onClick = onActivate) {
                    Text(text = stringResource(R.string.activate))
                }
            }
            AnimatedVisibility(
                visible = uiState.fileNames.contains(uiState.regionName) && uiState.isTileActive
            ) {
                TextButton(onClick = onDeactivate) {
                    Text(text = stringResource(R.string.deactivate))
                }
            }
            AnimatedVisibility(visible = uiState.fileNames.contains(uiState.regionName)) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                }
            }
        }
    )
}

@Composable
fun TilemakerContent(
    innerPadding: PaddingValues,
    uiState: TilemakerUiState,
    viewModel: TilemakerViewModel
) {
    val context = LocalContext.current
    val importTitle = stringResource(R.string.import_title)
    val cameraPositionState = rememberCameraPositionState {
        uiState.startLocation?.let {
            position = CameraPosition.fromLatLngZoom(it, uiState.zoom.toFloat())
        }
    }
    
    val tile = GeoJsonUtils.Companion.Tile(uiState.x, uiState.y, uiState.zoom)
    val tileCenterLatLng = GeoJsonUtils.tileToGmsBounds(tile).center
    val tileCenterLatLngState = rememberUpdatedMarkerState(position = tileCenterLatLng)

    LaunchedEffect(uiState.x, uiState.y) {
        val center = GeoJsonUtils.tileCenter(tile)
        cameraPositionState.position = CameraPosition.fromLatLngZoom(center, uiState.zoom.toFloat())
        tileCenterLatLngState.position = GeoJsonUtils.tileToGmsBounds(tile).center
    }

    uiState.moboDeleteConfirmation?.let { message ->
        MoboConfirmation(message) { result ->
            viewModel.setMoboDeleteConfirmation(null)
            if (result) {
                viewModel.deleteRegion(uiState.regionName)
            }
        }
    }

    if (uiState.showGeoCoder) {
        GeoCoderLauncher(
            latLng = LatLng(tileCenterLatLng.latitude, tileCenterLatLng.longitude),
            showInMap = { _, _, latlng ->
                latlng?.let {
                    val tileMap = GeoJsonUtils.pointToTile(it.longitude, it.latitude, uiState.zoom.toDouble())
                    viewModel.updateCoordinates(tileMap.x, tileMap.y)
                }
                viewModel.setShowGeoCoder(false)
            }
        )
    }

    Column(
        modifier = Modifier.padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.listDriveEntries) {
            ListRasterDriveEntries(
                onDismissRequest = { viewModel.setListDriveEntries(false) },
                import = {
                    context.startActivity(
                        Intent(context, FileImportActivity::class.java)
                            .setAction(importTitle)
                            .putExtra(Const.EXTRA_FILETYPE, FileType.MbTiles.name)
                    )
                }
            ) { rasterItemModel ->
                viewModel.handleDriveEntrySelection(rasterItemModel.name)
            }
        }
        
        if (uiState.showDropDownRasterMaptype) {
            MaptypeMenu(context) { maptype ->
                viewModel.setShowDropDownRasterMaptype(false)
                maptype?.let { viewModel.updateMapType(it) }
            }
        }

        TileInfoCard(bounds = uiState.bounds)

        Row(
            modifier = Modifier.padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(0.8f),
                value = uiState.regionName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.region_name)) }
            )
            IconButton(
                onClick = { viewModel.setListDriveEntries(true) },
                modifier = Modifier.weight(0.2f)
            ) {
                Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null)
            }
        }

        TileNavigationControls(
            onXChange = { delta -> viewModel.updateCoordinates(uiState.x + delta, uiState.y) },
            onYChange = { delta -> viewModel.updateCoordinates(uiState.x, uiState.y + delta) }
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            uiState.progressCreateTilename?.let {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$it ${uiState.progressCreateValue}%",
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            .background(Color.White),
                        textAlign = TextAlign.Center
                    )
                    LinearProgressIndicator(
                        progress = { uiState.progressCreateValue / 100f },
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                    )
                }
            }
        }

        if (uiState.createMbTileRegion != null) {
            CreateMbTileRegion(
                regionName = uiState.createMbTileRegion,
                progress_ = { viewModel.updateCreateProgress(it) },
                finished = { viewModel.updateCreateProgress(100) }
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(320.dp)
                .height(320.dp)
        ) {
            GoogleMapViewInColumn(
                modifier = Modifier.fillMaxSize().testTag("Map"),
                cameraPositionState = cameraPositionState,
                tileCenterLatLngState = tileCenterLatLngState,
                tileX = uiState.x,
                tileY = uiState.y,
                zoom = uiState.zoom,
                mapType = uiState.mapType,
                onMapLoaded = { viewModel.setMapLoaded(true) },
                onMapClick = { latLng ->
                    val t = GeoJsonUtils.pointToTile(latLng.longitude, latLng.latitude, uiState.zoom.toDouble())
                    viewModel.updateCoordinates(t.x, t.y)
                },
                onMarkerClick = { Timber.i("region $it") }
            )
            if (!uiState.isMapLoaded) {
                CircularProgressIndicator(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background).wrapContentSize()
                )
            }
        }
    }
}

@Composable
fun TileInfoCard(bounds: org.maplibre.android.geometry.LatLngBounds) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(
                    text = "Center N: ${bounds.center.latitude.formatLatLngShort()}° " +
                            "W: ${bounds.center.longitude.formatLatLngShort()}°",
                    fontFamily = FontFamily.Monospace
                )
            }
            Row {
                val weight = 0.25f
                Text(
                    modifier = Modifier.weight(weight),
                    text = "N:${bounds.latitudeNorth.formatLatLngShort()}",
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    modifier = Modifier.weight(weight),
                    text = "S:${bounds.latitudeSouth.formatLatLngShort()}",
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    modifier = Modifier.weight(weight),
                    text = "W:${bounds.longitudeWest.formatLatLngShort()}",
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    modifier = Modifier.weight(weight),
                    text = "E:${bounds.longitudeEast.formatLatLngShort()}",
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun TileNavigationControls(
    onXChange: (Int) -> Unit,
    onYChange: (Int) -> Unit
) {
    Row {
        val weight = 0.2f
        IconButton(onClick = { onXChange(1) }, modifier = Modifier.weight(weight)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowRight, contentDescription = null)
        }
        IconButton(onClick = { onXChange(-1) }, modifier = Modifier.weight(weight)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowLeft, contentDescription = null)
        }
        IconButton(onClick = { onYChange(-1) }, modifier = Modifier.weight(weight)) {
            Icon(Icons.Outlined.ArrowDropUp, contentDescription = null)
        }
        IconButton(onClick = { onYChange(1) }, modifier = Modifier.weight(weight)) {
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
        }
    }
}

@Composable
private fun GoogleMapViewInColumn(
    modifier: Modifier,
    cameraPositionState: CameraPositionState,
    tileCenterLatLngState: MarkerState,
    tileX: Int,
    tileY: Int,
    zoom: Int,
    mapType: String?,
    onMapLoaded: () -> Unit,
    onMapClick: (LatLng) -> Unit,
    onMarkerClick: (String) -> Unit
) {
    val regionName = "tile_${tileX}_${tileY}_${zoom}_${mapType}"
    val uiSettings by remember { mutableStateOf(MapUiSettings(compassEnabled = false)) }
    val mapProperties by remember { mutableStateOf(MapProperties(mapType = MapType.NORMAL)) }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
        onMapLoaded = onMapLoaded,
        onMapClick = onMapClick
    ) {
        UpdateCoordinateOverlay(MapType.NORMAL.name)
        MarkerInfoWindowContent(
            state = tileCenterLatLngState,
            title = regionName,
            onClick = {
                it.title?.let { title -> onMarkerClick(title) }
                false
            },
            draggable = false
        ) {
            Text(it.title ?: "Title", color = Color.Red)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TilemakerGoogleMapViewInColumnPreview() {
    val zoom = 10
    val latLng = LatLng(-1.286389, 36.817223)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(latLng, zoom.toFloat())
    }
    val tileCenterLatLngState = rememberUpdatedMarkerState(position = latLng)

    RamaniTheme {
        GoogleMapViewInColumn(
            modifier = Modifier.size(320.dp),
            cameraPositionState = cameraPositionState,
            tileCenterLatLngState = tileCenterLatLngState,
            tileX = 1082,
            tileY = 672,
            zoom = zoom,
            mapType = "Outdoor",
            onMapLoaded = {},
            onMapClick = {},
            onMarkerClick = {}
        )
    }
}
