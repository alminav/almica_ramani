package com.almica.ramani

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.MotionEvent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.preference.PreferenceManager
import com.almica.ramani.Helpers.Companion.getGeojsonFolders
import com.almica.ramani.GeojsonFolderSelection
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.ui.theme.Margin
import com.almica.ramani.utils.BackPressHandler
import com.almica.ramani.utils.DriveSharedLinks
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.GeoJsonUtils.Companion.createDefaultGeojsonOfflineStyle
import com.almica.ramani.utils.GeoJsonUtils.Companion.createGeojsonOfflineStyle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import me.ibrahimsn.library.LiveSharedPreferences
import timber.log.Timber
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.isNotNull
import java.io.File

private const val logtag = "ListGeojsonScreen"

/**
 * replaced by ListGeojsonFolder
 */
@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListGeojsonScreen(selectGeojsonFolder: (selection: GeojsonFolderSelection) -> Unit) {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(LocalContext.current)
    val liveSharedPreferences = remember { LiveSharedPreferences(prefs) }
    var prefGeojsonFolderPath = liveSharedPreferences.preferences.getString(Const.PREF_GEOJSON_FILEPATH, "")
    var prefGeojsonFolderName by remember { mutableStateOf(prefGeojsonFolderPath?.let { File(it).name })}
    var showDriveEntries by remember { mutableStateOf(false) }
    var showGoogleMap by remember { mutableStateOf<Pair<GeoJsonUtils.Companion.Tile, String>?>(null) }
    var geojsonFolders by remember { mutableStateOf(getGeojsonFolders(context)) }
    liveSharedPreferences.getString(Const.PREF_GEOJSON_FILEPATH, "").observe(LocalLifecycleOwner.current) { value ->
        if (value != null) {
            Timber.i("Const.PREF_GEOJSON_FILEPATH $value")
            prefGeojsonFolderName = File(value).name
            prefGeojsonFolderPath = File(value).path
        }
    }

    ListGeojsonScreenContent(
        prefGeojsonFolderName = prefGeojsonFolderName,
        geojsonFolders = geojsonFolders,
        showDriveEntries = showDriveEntries,
        showGoogleMap = showGoogleMap,
        liveSharedPreferences = liveSharedPreferences,
        onBack = { selectGeojsonFolder(GeojsonFolderSelection("", "", true)) },
        onClear = {
            liveSharedPreferences.preferences.edit { remove(Const.PREF_GEOJSON_FILEPATH) }
            createDefaultGeojsonOfflineStyle(context)
            geojsonFolders = getGeojsonFolders(context)
        },
        onRefresh = { geojsonFolders = getGeojsonFolders(context) },
        onImport = { showDriveEntries = true },
        onMapAction = { name ->
            when (showGoogleMap?.second) {
                context.getString(R.string.import_title) -> {
                    FileImportActivity.launch(context, FileType.GeojsonQgisZip)
                }

                context.getString(R.string.activate) -> {
                    val rootFolder = context.filesDir
                    val geojsonRootFolder = File(rootFolder, Const.GEOJSON_ROOT_FOLDER)
                    val geojsonFolder = File(geojsonRootFolder, name)

                    selectGeojsonFolder(GeojsonFolderSelection(geojsonFolder.path, name, false))
                    prefGeojsonFolderPath = geojsonFolder.path
                    prefGeojsonFolderPath?.let {
                        liveSharedPreferences.preferences.edit {
                            putString(Const.PREF_GEOJSON_FILEPATH, geojsonFolder.path)
                        }
                        val geojsonFolderName = File(it).name
                        createGeojsonOfflineStyle(context, geojsonFolderName)
                    }
                }
            }
            showGoogleMap = null
        },
        onMapFinished = { success ->
            Timber.i("success: $success")
            showGoogleMap = null
        },
        onDriveEntrySelected = { driveEntry ->
            val buttonText = context.getString(R.string.import_title)
            if (driveEntry == buttonText) {
                showDriveEntries = false
                FileImportActivity.launch(context, FileType.GeojsonQgisZip)
            } else if (driveEntry.isNotNull()) {
                val splits = driveEntry?.replace(Const.ZIP_EXT, "")?.split(Const.UNDERLINE, limit = 4)
                if (splits.isNotNull() && splits?.size!! > 2) {
                    showGoogleMap = Pair(
                        GeoJsonUtils.Companion.Tile(
                            splits[0].toInt(), splits[1].toInt(), splits[2].toInt()
                        ),
                        buttonText
                    )
                }
            } else
                showDriveEntries = false
        },
        onListFolderSelected = { resultPair ->
            Timber.i("resultPair: ${resultPair.first} ${resultPair.second}")
            val splits = resultPair.second.split(Const.UNDERLINE, limit = 4)
            if (splits.isNotNull() && splits.size > 2) {
                showGoogleMap = Pair(
                    GeoJsonUtils.Companion.Tile(
                        splits[1].toInt(), splits[2].toInt(), splits[3].toInt()
                    ),
                    context.getString(R.string.activate)
                )
            }
        },
        onListRefresh = {
            geojsonFolders = getGeojsonFolders(context)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListGeojsonScreenContent(
    prefGeojsonFolderName: String?,
    geojsonFolders: Array<File>,
    showDriveEntries: Boolean,
    showGoogleMap: Pair<GeoJsonUtils.Companion.Tile, String>?,
    liveSharedPreferences: LiveSharedPreferences,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onRefresh: () -> Unit,
    onImport: () -> Unit,
    onMapAction: (String) -> Unit,
    onMapFinished: (Boolean) -> Unit,
    onDriveEntrySelected: (String?) -> Unit,
    onListFolderSelected: (Pair<String, String>) -> Unit,
    onListRefresh: () -> Unit
) {
    val context = LocalContext.current
    BackPressHandler {
        onBack()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back home"
                        )
                    }
                }, title = {
                    Text(text = stringResource(R.string.geojson_folders))
                }, actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClear) {
                            Icon(
                                Icons.Outlined.Clear,
                                contentDescription = null
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onRefresh) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = null
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onImport) {
                            Icon(
                                Icons.Outlined.ImportExport,
                                contentDescription = null
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        showGoogleMap?.let {
            MoboGoogleMap(it, action = onMapAction, finished = onMapFinished)
        }
        if (showDriveEntries) {
            val buttonText = stringResource(R.string.import_title)
            DropdownGeojsonEntries(buttonText, onDriveEntrySelected)
        }
        ListGeojsonFolder(
            context,
            Modifier.padding(paddingValues),
            geojsonFolders,
            prefGeojsonFolderName,
            liveSharedPreferences,
            selectGeojsonFolder = onListFolderSelected,
            refreshList = onListRefresh
        )
    }
}

@Composable
fun ListGeojsonFolder(
    context: Context,
    modifier: Modifier,
    geojsonFolders: Array<File>,
    prefGeojsonFolderName: String?,
    liveSharedPreferences: LiveSharedPreferences,
    selectGeojsonFolder: (name: Pair<String, String>) -> Unit,
    refreshList: () -> Unit
) {
    //TitleViewModel.value = Screen.GraphHopper.name
    Timber.i("ghFolders: ${geojsonFolders.size}")
    val geojsonList = ArrayList<Pair<String, String>>()
    for (file in geojsonFolders) {
        geojsonList.add(Pair(file.name, file.path))
    }
    Column(
        modifier = modifier.padding(
            horizontal = Margin.horizontal,
            vertical = Margin.vertical
        )
    ) {
        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(geojsonList) { name ->
                GeojsonFolderItem(context, prefGeojsonFolderName, liveSharedPreferences, geojsonName = name.first,
                    onItemClick = { resultPair ->
                        Timber.i("resultPair: $resultPair")
                        selectGeojsonFolder(Pair(resultPair.first, resultPair.second))
                }, refreshList = { refreshList() })
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
@Composable
fun GeojsonFolderItem(context: Context,
                      prefGeojsonFolderName: String?,
                      liveSharedPreferences: LiveSharedPreferences,
                      geojsonName: String,
                      onItemClick: (Pair<String, String>) -> Unit,
                      refreshList: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(modifier = Modifier.fillMaxWidth(0.8f), onClick = {
                    val rootFolder = context.filesDir
                    val geojsonRootFolder = File(rootFolder, Const.GEOJSON_ROOT_FOLDER)
                    val geojsonFolder = File(geojsonRootFolder, geojsonName)
                    onItemClick(Pair(geojsonFolder.path, geojsonFolder.name))
                }) {
                    Text(text = geojsonName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                if (geojsonName == prefGeojsonFolderName) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(0.2f)
                    )
                } else
                    Spacer(modifier = Modifier.fillMaxWidth(0.2f))

                IconButton(onClick = {
                    val prefGeojsonFolderPath = liveSharedPreferences.preferences.getString(
                            Const.PREF_GEOJSON_FILEPATH, null)
                    Timber.i("prefGeojsonFolderPath: $prefGeojsonFolderPath")
                    val rootFolder = context.filesDir
                    val geojsonRootFolder = File(rootFolder, Const.GEOJSON_ROOT_FOLDER)
                    val geojsonFolder = File(geojsonRootFolder, geojsonName)
                    Timber.i("geojsonFolder: ${geojsonFolder.path}")
                    if (geojsonFolder.isNotNull()) {
                        geojsonFolder.deleteRecursively()
                        if (geojsonFolder.path == prefGeojsonFolderPath) {
                            liveSharedPreferences.preferences.edit { remove(Const.PREF_GEOJSON_FILEPATH) }
                            createDefaultGeojsonOfflineStyle(context)
                        }
                        refreshList()
                    }
                }) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null
                    )
                }

            }
            //Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
private fun DropdownGeojsonEntries(buttonText: String, selected: (String?) -> Unit) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = { selected(null) }
    ) {
        val driveMap = DriveSharedLinks.Companion.GeojsonQgisMapRegions().list
        if (buttonText == stringResource(R.string.import_title)) {
            DropdownMenuItem(
                trailingIcon = {
                    Icon(
                        Icons.Outlined.ImportExport,
                        null
                    )
                },
                text = { Text(text = buttonText, textDecoration = TextDecoration.Underline) },
                onClick = {
                    selected(buttonText)
                }
            )
        }
        driveMap.forEach { driveEntry ->
            Timber.i("driveEntry: ${driveEntry.key}")
            DropdownMenuItem(
                text = { Text(text = driveEntry.key) },
                onClick = {
                    selected(driveEntry.key)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoboGoogleMap(
    tile: Pair<GeoJsonUtils.Companion.Tile, String>,
    action: (String) -> Unit,
    finished: (Boolean) -> Unit
)
{
    val name = "geojsonTile_${tile.first.x}_${tile.first.y}_${tile.first.z}"
    ModalBottomSheet(onDismissRequest = { finished(true) }) {
        Column() {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = name, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                Box(modifier = Modifier
                    .align(alignment = Alignment.CenterVertically)
                    .weight(0.3f),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = { action(name) }) {
                        Text(text = tile.second)
                    }
                }
            }
            Row(modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
                Box(
                    contentAlignment = Alignment.Center,
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
                        tile.first,
                        onMapLoaded = {
                            Timber.i("onMapLoaded")
                        }, onMapClick = {
                            finished(true)
                        }
                    )
                }
            }
        }
    }
}
@Composable
private fun GoogleMapViewInColumn(
    modifier: Modifier,
    tile: GeoJsonUtils.Companion.Tile,
    onMapLoaded: () -> Unit,
    onMapClick: (LatLng) -> Unit
) {
    var uiSettings by remember { mutableStateOf(MapUiSettings(compassEnabled = false)) }
    var mapProperties by remember {
        mutableStateOf(MapProperties(mapType = MapType.NORMAL))
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(GeoJsonUtils.tileCenter(tile), tile.z.toFloat())
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
        val tileBounds = GeoJsonUtils.tileToBounds(tile)
        val points = arrayListOf<LatLng>()
        points.add(LatLng(tileBounds.northWest.latitude, tileBounds.northWest.longitude))
        points.add(LatLng(tileBounds.northEast.latitude, tileBounds.northEast.longitude))
        points.add(LatLng(tileBounds.southEast.latitude, tileBounds.southEast.longitude))
        points.add(LatLng(tileBounds.southWest.latitude, tileBounds.southWest.longitude))
        points.add(LatLng(tileBounds.northWest.latitude, tileBounds.northWest.longitude))
        val routePattern = listOf(Dash(20f), Gap(20f), Dash(20f))
        if (points.isNotEmpty()) {
            Timber.i("lllh: ${points.size}")
            com.google.maps.android.compose.Polyline(
                points,
                color = Color.Red,
                width = 6f,
                pattern = routePattern,
                clickable = true,
                onClick = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListGeojsonScreenPreview() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("preview_prefs", Context.MODE_PRIVATE)
    val liveSharedPreferences = LiveSharedPreferences(prefs)
    val geojsonFolders = arrayOf(
        File("Sample Folder 1"),
        File("Sample Folder 2")
    )

    RamaniTheme {
        ListGeojsonScreenContent(
            prefGeojsonFolderName = "Sample Folder 1",
            geojsonFolders = geojsonFolders,
            showDriveEntries = false,
            showGoogleMap = null,
            liveSharedPreferences = liveSharedPreferences,
            onBack = {},
            onClear = {},
            onRefresh = {},
            onImport = {},
            onMapAction = {},
            onMapFinished = {},
            onDriveEntrySelected = {},
            onListFolderSelected = {},
            onListRefresh = {}
        )
    }
}


