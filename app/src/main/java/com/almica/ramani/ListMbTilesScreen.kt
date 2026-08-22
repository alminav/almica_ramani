package com.almica.ramani

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.googlemaps.NewMapAction
import com.almica.ramani.tilemaker.MbtilesDatabase
import com.almica.ramani.ui.theme.Margin
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber
import java.io.File


private const val logtag = "ListMbTilesScreen"
@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListMbTilesScreen(
    innerPadding: PaddingValues,
    viewModel: ListMbTilesViewModel = viewModel(),
    finish: (restartRequired: Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val clipboardManager = LocalClipboard.current

    val screenWidthPx = remember(context) { resources.displayMetrics.widthPixels }

    LaunchedEffect(screenWidthPx) {
        viewModel.refreshRasterMaps(screenWidthPx)
    }

    LaunchedEffect(uiState.clipText) {
        uiState.clipText?.let { text ->
            if (text.isNotEmpty()) {
                val clipData = ClipData.newPlainText(NewMapAction.Import.name, text)
                clipboardManager.setClipEntry(ClipEntry(clipData))
                viewModel.clearClipText()
            }
        }
    }

    BackHandler {
        finish(uiState.restartRequired)
    }

    Scaffold(
        modifier = Modifier.padding(innerPadding),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { finish(uiState.restartRequired) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back home"
                        )
                    }
                },
                title = {
                    Text(text = stringResource(R.string.rastermaps), fontSize = 14.sp)
                },
                actions = {
                    IconButton(onClick = { viewModel.setShowDriveEntries(true) }) {
                        Icon(Icons.Outlined.ImportExport, contentDescription = null)
                    }
                }
            )
        },
    ) { paddingValues ->
        uiState.showGoogleMap?.let { mapName ->
            MoboRasterGoogleMap(
                name = mapName,
                import = { rasterName ->
                    viewModel.setClipText(rasterName)
                    FileImportActivity.launch(context, FileType.MbTiles)
                    viewModel.setShowGoogleMap(null)
                    viewModel.setShowDriveEntries(false)
                },
                activate = { rasterName ->
                    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                    val tilesPrefSet = prefs.getStringSet(Const.PREF_MBTILES_FILEPATH_SET, setOf()) ?: setOf()
                    val rasterRootFolder = File(context.filesDir, Const.MBTILES_FOLDER)
                    val f = File(rasterRootFolder, rasterName.plus(Const.MBTILES_EXT))
                    if (f.exists()) {
                        prefs.edit { putStringSet(Const.PREF_MBTILES_FILEPATH_SET, tilesPrefSet + f.path) }
                        viewModel.refreshRasterMaps(screenWidthPx)
                    }
                    viewModel.setShowGoogleMap(null)
                    viewModel.setShowDriveEntries(false)
                },
                finished = { viewModel.setShowGoogleMap(null) }
            )
        }

        uiState.snackbarData?.let { data ->
            MoboSnack(data) { viewModel.clearSnackbar() }
        }

        if (uiState.showDriveEntries) {
            ListRasterDriveEntries(
                onDismissRequest = { viewModel.setShowDriveEntries(false) },
                import = {
                    FileImportActivity.launch(context, FileType.MbTiles)
                },
                onItemClick = { model -> viewModel.setShowGoogleMap(model.name) }
            )
        }

        MultiSelectList(
            modifier = Modifier.padding(paddingValues),
            itemsGrouped = uiState.itemsGrouped,
            checkCount = uiState.checkCount,
            onConfirm = { viewModel.confirmChanges() },
            onRefresh = { viewModel.refreshRasterMaps(screenWidthPx) },
            onItemStateChanged = { name, state -> viewModel.changeItemState(name, state) },
            onDeleteSelected = { viewModel.deleteSelectedMaps() },
            onDeleteSingle = { model -> viewModel.deleteSingleMap(model) },
            onShare = { path -> exportMaps(context, setOf(path)) },
            onShareMultiple = { paths -> exportMaps(context, paths) }
        )

        AnimatedVisibility(
            visible = uiState.isLoading,
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.initialization),
                    modifier = Modifier.background(Color.White).padding(4.dp),
                    textAlign = TextAlign.Center
                )
                CircularProgressIndicator(modifier = Modifier.size(50.dp))
            }
        }
    }
}


private fun exportMaps(context: Context, mbTilesPrefSet: Set<String>?) {
    val shareIntent = Intent()
    val uris = arrayListOf<Uri>()
    mbTilesPrefSet?.forEach {
        val fileToExport = File(it)
        val uri = FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID + ".provider",
            fileToExport
        )
        uris.add(uri)
        Timber.i("exportMaps ${fileToExport.name}")
    }
    shareIntent.action = Intent.ACTION_SEND_MULTIPLE
    shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    shareIntent.type = "*/*"
    context.startActivity(Intent.createChooser(shareIntent, "Share images to.."))
}

@Composable
private fun MultiSelectList(
    modifier: Modifier,
    itemsGrouped: Map<String, List<RasterMapItemModel>>,
    checkCount: Int,
    onConfirm: () -> Unit,
    onRefresh: () -> Unit,
    onItemStateChanged: (String, Boolean) -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteSingle: (RasterMapItemModel) -> Unit,
    onShare: (String) -> Unit,
    onShareMultiple: (Set<String>) -> Unit
) {
    RasterMapHeadLine(
        modifier = modifier,
        itemsGrouped = itemsGrouped,
        checkCount = checkCount,
        onRefresh = onRefresh,
        onShareMultiple = onShareMultiple,
        onDeleteSelected = onDeleteSelected,
        onConfirm = onConfirm,
        onItemStateChanged = onItemStateChanged,
        onDeleteSingle = onDeleteSingle,
        onShareSingle = onShare
    )
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
private fun RasterMapHeadLine(
    modifier: Modifier,
    itemsGrouped: Map<String, List<RasterMapItemModel>>,
    checkCount: Int,
    onRefresh: () -> Unit,
    onShareMultiple: (Set<String>) -> Unit,
    onDeleteSelected: () -> Unit,
    onConfirm: () -> Unit,
    onItemStateChanged: (String, Boolean) -> Unit,
    onDeleteSingle: (RasterMapItemModel) -> Unit,
    onShareSingle: (String) -> Unit
) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(LocalContext.current)
    val tilesPrefSet = prefs.getStringSet(Const.PREF_MBTILES_FILEPATH_SET, setOf()) ?: setOf()
    var indexExpanded by remember { mutableIntStateOf(-1) }

    Column(
        modifier = modifier.padding(
            horizontal = Margin.horizontal,
            vertical = Margin.vertical
        )
    ) {
        Row(modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(10.dp))
            AnimatedVisibility(visible = checkCount > 0, modifier = Modifier.align(alignment = Alignment.CenterVertically)) {
                BadgedBox(badge = { Badge { Text("$checkCount") } }) {
                    Icon(Icons.Outlined.Map, contentDescription = null)
                }
            }
            AnimatedVisibility(visible = checkCount > 0) {
                IconButton(onClick = { onShareMultiple(tilesPrefSet) }) {
                    Icon(
                        Icons.Outlined.Share, modifier = Modifier.padding(10.dp),
                        contentDescription = stringResource(R.string.export_title)
                    )
                }
            }
            AnimatedVisibility(visible = checkCount > 0) {
                IconButton(onClick = onDeleteSelected) {
                    Icon(
                        Icons.Outlined.Delete, modifier = Modifier.padding(10.dp),
                        contentDescription = stringResource(R.string.remove)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.confirm))
            }
        }
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            itemsGrouped.forEach { (initial, groupedEntries) ->
                stickyHeader {
                    Column(
                        modifier = Modifier
                            .height(24.dp)
                            .fillMaxWidth()
                            .background(Color.LightGray)
                    ) {
                        Text(
                            text = initial,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                items(groupedEntries.size) { index ->
                    RasterMapItem(
                        itemModels = groupedEntries,
                        index = index,
                        indexExpanded = indexExpanded,
                        onItemStateChanged = onItemStateChanged,
                        onDelete = { onDeleteSingle(groupedEntries[index]) },
                        onShare = { onShareSingle(groupedEntries[index].path) },
                        onExpand = { ixExp -> indexExpanded = ixExp }
                    )
                }
            }
        }
    }
}

@Composable
private fun RasterMapItem(
    itemModels: List<RasterMapItemModel>,
    index: Int,
    indexExpanded: Int,
    onItemStateChanged: (String, Boolean) -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onExpand: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 3.dp, top = if (index == 0) 3.dp else 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemStateChanged(itemModels[index].name, itemModels[index].selected) }
                    .padding(start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (itemModels[index].selected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "selected",
                        tint = Color.Magenta,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                } else
                    Spacer(Modifier.width(35.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(0.66f), fontSize = 12.sp,
                    text = "${itemModels[index].name.replace(Const.MBTILES_EXT, "")} (${itemModels[index].lastModifiedDate})"
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "delete",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "share",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (itemModels[index].thumbnail != null)
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (indexExpanded == index) onExpand(-1)
                        else onExpand(index)
                    }) {
                    Text(
                        text = if (index == indexExpanded) Const.UC_DROPUP_ARROW else Const.UC_DROPDOWN_ARROW,
                        textAlign = TextAlign.Center, fontSize = 20.sp
                    )
                }
            AnimatedVisibility(visible = index == indexExpanded) {
                itemModels[index].thumbnail?.let { thumbnail ->
                    val imageBitmap = thumbnail.asImageBitmap()
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Image(
                            painter = BitmapPainter(
                                imageBitmap, IntOffset(0, 0),
                                IntSize(thumbnail.width, thumbnail.height)
                            ),
                            contentDescription = itemModels[index].name
                        )
                    }
                    HorizontalDivider(
                        color = Color.LightGray,
                        modifier = Modifier.height(2.dp).padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoboSnack(mbTilesSnackbarData: MbTilesSnackbarData, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.padding(horizontal = 10.dp)) {
            Row(
                modifier = Modifier.border(width = 2.dp, color = Color.LightGray, shape = RectangleShape),
                verticalAlignment = Alignment.CenterVertically
            ) {
                mbTilesSnackbarData.title?.let {
                    Text(
                        text = it,
                        modifier = Modifier.weight(0.8f).padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Blue
                    )
                }
                mbTilesSnackbarData.actionText?.let { text ->
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(0.2f)) {
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoboRasterGoogleMap(
    name: String,
    import: (String) -> Unit,
    activate: (String) -> Unit,
    finished: () -> Unit)
{
    val context = LocalContext.current
    val rootFolder = context.filesDir
    val rasterRootFolder = File(rootFolder, Const.MBTILES_FOLDER)
    val f = File(rasterRootFolder, name.plus(Const.MBTILES_EXT))
    ModalBottomSheet(onDismissRequest = { finished() }) {
        Column() {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = name, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                if (!f.exists()) {
                    Box(
                        modifier = Modifier
                            .align(alignment = Alignment.CenterVertically)
                            .weight(0.25f),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { import(name) }) {
                            Text(text = stringResource(R.string.import_title))
                        }
                    }
                }
                if (f.exists()) {
                    Box(
                        modifier = Modifier
                            .align(alignment = Alignment.CenterVertically)
                            .weight(0.25f),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { activate(name) }) {
                            Text(text = stringResource(R.string.activate))
                        }
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
                        name,
                        onMapLoaded = {
                            Timber.i("onMapLoaded")
                        }, onMapClick = {
                            finished()
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RasterMapItemPreview() {
    val sampleItemModels = listOf(
        RasterMapItemModel(
            name = "Sample Map${Const.MBTILES_EXT}",
            path = "/storage/emulated/0/mbtiles/sample.mbtiles",
            thumbnail = null,
            lastModifiedDate = "2023-10-27",
            mapType = "Raster",
            selected = false
        ),
        RasterMapItemModel(
            name = "Selected Map${Const.MBTILES_EXT}",
            path = "/storage/emulated/0/mbtiles/selected.mbtiles",
            thumbnail = null,
            lastModifiedDate = "2023-10-28",
            mapType = "Raster",
            selected = true
        )
    )

    RamaniTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            RasterMapItem(
                itemModels = sampleItemModels,
                index = 0,
                indexExpanded = -1,
                onItemStateChanged = { _, _ -> },
                onDelete = {},
                onShare = {},
                onExpand = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            RasterMapItem(
                itemModels = sampleItemModels,
                index = 1,
                indexExpanded = -1,
                onItemStateChanged = { _, _ -> },
                onDelete = {},
                onShare = {},
                onExpand = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RasterMapHeadLinePreview() {
    val sampleItemModels = listOf(
        RasterMapItemModel(
            name = "Sample Map 1${Const.MBTILES_EXT}",
            path = "/path/1",
            thumbnail = null,
            lastModifiedDate = "2023-10-27 10:00",
            mapType = "Type A",
            selected = false
        ),
        RasterMapItemModel(
            name = "Sample Map 2${Const.MBTILES_EXT}",
            path = "/path/2",
            thumbnail = null,
            lastModifiedDate = "2023-10-28 11:00",
            mapType = "Type A",
            selected = true
        ),
        RasterMapItemModel(
            name = "Sample Map 3${Const.MBTILES_EXT}",
            path = "/path/3",
            thumbnail = null,
            lastModifiedDate = "2023-10-29 12:00",
            mapType = "Type B",
            selected = false
        )
    )
    val itemsGrouped = sampleItemModels.groupBy { it.mapType }

    RamaniTheme {
        RasterMapHeadLine(
            modifier = Modifier.fillMaxSize(),
            itemsGrouped = itemsGrouped,
            checkCount = 1,
            onRefresh = {},
            onShareMultiple = {},
            onDeleteSelected = {},
            onConfirm = {},
            onItemStateChanged = { _, _ -> },
            onDeleteSingle = {},
            onShareSingle = {}
        )
    }
}
