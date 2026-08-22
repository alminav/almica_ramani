package com.almica.ramani

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.view.MotionEvent
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.googlemaps.NewMapAction
import com.almica.ramani.ui.theme.Margin
import com.almica.ramani.utils.BackPressHandler
import com.almica.ramani.utils.GeoJsonUtils
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import timber.log.Timber
import java.io.File
import androidx.compose.ui.platform.LocalResources

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListMvtScreen(
    innerPadding: PaddingValues,
    newMvtMap: (String?) -> Unit,
    finish: (Boolean) -> Unit
) {
    val viewModel: ListMvtViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current

    LaunchedEffect(uiState.clipText) {
        uiState.clipText?.let { text ->
            if (text.isNotEmpty()) {
                Timber.i("clipText: $text")
                val clipData = ClipData.newPlainText(NewMapAction.Import.name, text)
                clipboardManager.setClipEntry(ClipEntry(clipData))
                viewModel.clearClipText()
            }
        }
    }

    BackPressHandler {
        finish(uiState.restartRequired)
    }

    ListMvtScreenContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onBackClick = { finish(uiState.restartRequired) },
        onImportClick = { viewModel.setShowDriveEntries(true) },
        onMvtSelected = { viewModel.onMvtSelected(it, newMvtMap) },
        onDeleteMvt = { viewModel.deleteMvtFile(it) },
        onShareMvt = {
            viewModel.shareMvtFile(it)
            Timber.i("onShareMvt: ${it.name}") },
        onInfoMvt = { viewModel.takeSnapshotAndShowInfo(it) },
        onDriveDismiss = { viewModel.setShowDriveEntries(false) },
        onDriveImport = { _ ->
            FileImportActivity.launch(context, FileType.Mvt)
        },
        onDriveItemClick = { mvtItemModel ->
            viewModel.setShowGoogleMap(mvtItemModel.name)
        },
        onMapImport = { mvtName ->
            viewModel.setClipText(mvtName)
            FileImportActivity.launch(context, FileType.Mvt)
            viewModel.setShowGoogleMap(null)
            viewModel.setShowDriveEntries(false)
        },
        onMapActivate = { mvtName ->
            viewModel.onMvtSelected(
                MvtItemModel(
                    name = mvtName,
                    path = File(File(context.filesDir, Const.MVT_FOLDER), mvtName.plus(Const.MBTILES_EXT)).path,
                    x = 0,
                    y = 0,
                    selected = false,
                    exists = true
                ),
                newMvtMap
            )
            viewModel.setShowGoogleMap(null)
            viewModel.setShowDriveEntries(false)
        },
        onMapDismiss = { viewModel.setShowGoogleMap(null) },
        onSnackAction = { action ->
            if (action == MvtSnackbarAction.Share) {
                Timber.i("onSnackAction: Share ${uiState.snackbarData?.actionData}")
                (uiState.snackbarData?.actionData as? File)?.let { file ->
                    shareFile(context, file)
                }
            }
            viewModel.clearSnackbar()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListMvtScreenContent(
    uiState: ListMvtUiState,
    innerPadding: PaddingValues,
    onBackClick: () -> Unit,
    onImportClick: () -> Unit,
    onMvtSelected: (MvtItemModel) -> Unit,
    onDeleteMvt: (MvtItemModel) -> Unit,
    onShareMvt: (MvtItemModel) -> Unit,
    onInfoMvt: (MvtItemModel) -> Unit,
    onDriveDismiss: () -> Unit,
    onDriveImport: (String?) -> Unit,
    onDriveItemClick: (MvtItemModel) -> Unit,
    onMapImport: (String) -> Unit,
    onMapActivate: (String) -> Unit,
    onMapDismiss: () -> Unit,
    onSnackAction: (MvtSnackbarAction?) -> Unit
) {
    Scaffold(
        modifier = Modifier.padding(innerPadding),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back_home)
                        )
                    }
                },
                title = {
                    Text(text = stringResource(R.string.mvt_databases), style = MaterialTheme.typography.titleSmall)
                },
                actions = {
                    IconButton(onClick = onImportClick) {
                        Icon(Icons.Outlined.ImportExport, contentDescription = stringResource(R.string.import_mvt))
                    }
                }
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            MvtFileList(
                items = uiState.mvtItemModels,
                onSelected = onMvtSelected,
                onDelete = onDeleteMvt,
                onShare = onShareMvt,
                onInfo = onInfoMvt
            )

            if (uiState.showDriveEntries) {
                ListMvtDriveEntries(
                    currentMvtName = uiState.currentMvtName,
                    itemsGrouped = uiState.driveItemModels,
                    onDismissRequest = onDriveDismiss,
                    import = onDriveImport,
                    onItemClick = onDriveItemClick
                )
            }

            uiState.showGoogleMap?.let { name ->
                MoboMvtGoogleMap(
                    name = name,
                    import = onMapImport,
                    activate = onMapActivate,
                    onDismiss = onMapDismiss
                )
            }

            uiState.snackbarData?.let { data ->
                MoboSnack(data, onSnackAction)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MvtFileList(
    items: List<MvtItemModel>,
    onSelected: (MvtItemModel) -> Unit,
    onDelete: (MvtItemModel) -> Unit,
    onShare: (MvtItemModel) -> Unit,
    onInfo: (MvtItemModel) -> Unit
) {
    val itemsGrouped = items.groupBy { it.x }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Margin.horizontal, vertical = Margin.vertical)
    ) {
        itemsGrouped.forEach { (initial, groupedEntries) ->
            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            items(groupedEntries) { item ->
                MvtItemRow(
                    item = item,
                    onSelected = { onSelected(item) },
                    onDelete = { onDelete(item) },
                    onShare = { onShare(item) },
                    onInfo = { onInfo(item) }
                )
            }
        }
    }
}

@Composable
private fun MvtItemRow(
    item: MvtItemModel,
    onSelected: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp)
                .clickable { onSelected() },
            colors = CardDefaults.cardColors(
                containerColor = if (item.selected) MaterialTheme.colorScheme.primaryContainer 
                                 else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name.replace(Const.MBTILES_EXT, ""),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                if (item.selected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = stringResource(R.string.selected),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete))
        }

        IconButton(onClick = onShare) {
            Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share))
        }

        if (item.name.startsWith(Const.MVT_PREFIX)) {
            IconButton(onClick = onInfo) {
                Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.info))
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

private fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        type = "*/*"
    }
    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_files_to)))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoboMvtGoogleMap(
    name: String,
    import: (String) -> Unit,
    activate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val mvtFile = File(File(context.filesDir, Const.MVT_FOLDER), name.plus(Const.MBTILES_EXT))

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
                if (!mvtFile.exists()) {
                    TextButton(onClick = { import(name) }) {
                        Text(text = stringResource(R.string.import_title))
                    }
                } else {
                    TextButton(onClick = { activate(name) }) {
                        Text(text = stringResource(R.string.activate))
                    }
                }
            }
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                GoogleMapViewInColumn(
                    modifier = Modifier.fillMaxSize(),
                    mvtName = name,
                    onMapLoaded = { Timber.i("onMapLoaded") },
                    onMapClick = { onDismiss() }
                )
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun GoogleMapViewInColumn(
    modifier: Modifier,
    mvtName: String,
    onMapLoaded: () -> Unit,
    onMapClick: (LatLng) -> Unit
) {
    val uiSettings by remember { mutableStateOf(MapUiSettings(compassEnabled = false)) }
    val mapProperties by remember { mutableStateOf(MapProperties(mapType = MapType.NORMAL)) }

    val splits = mvtName.replace(Const.MBTILES_EXT, "").split(Const.UNDERLINE, limit = 5)
    if (splits.size > 3) {
        val mvtTile = GeoJsonUtils.Companion.Tile(
            splits[1].toInt(),
            splits[2].toInt(),
            splits[3].toInt()
        )
        val mvtBounds = GeoJsonUtils.tileToBounds(mvtTile)
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                LatLng(mvtBounds.center.latitude, mvtBounds.center.longitude),
                mvtTile.z.toFloat()
            )
        }
        
        GoogleMap(
            modifier = modifier
                .pointerInteropFilter {
                    when (it.action) {
                        MotionEvent.ACTION_DOWN -> { false }
                        else -> { true }
                    }
                },
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings,
            onMapLoaded = onMapLoaded,
            onMapClick = onMapClick
        ) {
            val points = listOf(
                LatLng(mvtBounds.northWest.latitude, mvtBounds.northWest.longitude),
                LatLng(mvtBounds.northEast.latitude, mvtBounds.northEast.longitude),
                LatLng(mvtBounds.southEast.latitude, mvtBounds.southEast.longitude),
                LatLng(mvtBounds.southWest.latitude, mvtBounds.southWest.longitude),
                LatLng(mvtBounds.northWest.latitude, mvtBounds.northWest.longitude)
            )
            val routePattern = listOf(Dash(20f), Gap(20f), Dash(20f))
            Polyline(
                points = points,
                color = Color.Red,
                width = 6f,
                pattern = routePattern,
                clickable = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoboSnack(
    mvtSnackbarData: MvtSnackbarData,
    onAction: (MvtSnackbarAction?) -> Unit
) {
    ModalBottomSheet(onDismissRequest = { onAction(MvtSnackbarAction.Nothing) }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = RectangleShape)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                mvtSnackbarData.title?.let {
                    Text(
                        text = it,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                mvtSnackbarData.actionText?.let { text ->
                    TextButton(onClick = { onAction(mvtSnackbarData.action) }) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            mvtSnackbarData.thumbNail?.let { thumbnail ->
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = BitmapPainter(thumbnail.asImageBitmap()),
                    contentDescription = mvtSnackbarData.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListMvtScreenPreview() {
    ListMvtScreenContent(
        uiState = ListMvtUiState(
            mvtItemModels = listOf(
                MvtItemModel("File 1", "path1", 1, 1, true, true),
                MvtItemModel("File 2", "path2", 2, 2, false, true)
            )
        ),
        innerPadding = PaddingValues(0.dp),
        onBackClick = {},
        onImportClick = {},
        onMvtSelected = {},
        onDeleteMvt = {},
        onShareMvt = {},
        onInfoMvt = {},
        onDriveDismiss = {},
        onDriveImport = {},
        onDriveItemClick = {},
        onMapImport = {},
        onMapActivate = {},
        onMapDismiss = {},
        onSnackAction = {}
    )
}
