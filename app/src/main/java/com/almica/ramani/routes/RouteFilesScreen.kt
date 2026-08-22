package com.almica.ramani.routes

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Height
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almica.ramani.BuildConfig
import com.almica.ramani.Const
import com.almica.ramani.Helpers
import com.almica.ramani.LatLngH
import com.almica.ramani.R
import com.almica.ramani.Helpers.Companion.addLineToSnapshotWithGradient
import com.almica.ramani.charts.GradientChartMonitor
import com.almica.ramani.charts.LineYGraphLllh
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.filepicker.UnzipUtils
import com.almica.ramani.googlemaps.MapUtils
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.HgtReader
import com.almica.ramani.utils.formatDistM
import com.almica.ramani.utils.getDistanceFromLllh
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.utils.kmlString2Lllh
import com.almica.ramani.utils.lllhToKmlString
import com.almica.ramani.utils.reduceWithTolerance
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileFilter
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import androidx.core.graphics.createBitmap
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.utils.GeoJsonUtils.Companion.pointToTile
import com.almica.ramani.utils.RouteSmoothingUtil.simplifyToTargetCount
import com.almica.ramani.utils.getCenter

enum class RouteEntityItemAction{
    Select,
    Map,
    Hide,
    Delete,
    Database
}

const val MAX_ELEVATION_POINTS = 512

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteFilesTopBar(
    uiState: RouteFilesUiState,
    onBackClick: () -> Unit,
    onImportExportClick: () -> Unit,
    onFilterClick: () -> Unit,
    onAddFolderClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSrtmClick: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back home"
                )
            }
        },
        title = {
            Text(text = stringResource(R.string.route_files), fontSize = 14.sp, maxLines = 1)
        },
        actions = {
            IconButton(onClick = onImportExportClick) {
                Icon(Icons.Outlined.ImportExport, contentDescription = null)
            }
            IconButton(onClick = onFilterClick) {
                Icon(
                    painterResource(R.drawable.outline_filter_alt_24),
                    "filter",
                    modifier = Modifier.padding(horizontal = 10.dp).size(60.dp)
                )
            }
            IconButton(onClick = onAddFolderClick) {
                Icon(
                    Icons.Outlined.Add,
                    "newFolder",
                    modifier = Modifier.padding(horizontal = 10.dp).size(60.dp)
                )
            }
            IconButton(onClick = onRefreshClick) {
                Icon(
                    Icons.Outlined.Refresh,
                    "notifyDataChanged",
                    modifier = Modifier.padding(horizontal = 10.dp).size(60.dp)
                )
            }
            BadgedBox(badge = {
                if (uiState.srtmFile.isNotNull()) Badge { Text(text = Const.UC_CHECKMARK) }
            }) {
                TextButton(onClick = onSrtmClick) {
                    Text(text = stringResource(R.string.srtm))
                }
            }
        }
    )
}

@SuppressLint("UnrememberedMutableState", "UnusedMaterial3ScaffoldPaddingParameter",
    "LocalContextGetResourceValueCall", "MutableCollectionMutableState"
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteFilesScreen(
    selectRoute: (RouteEntity?, RouteMenu) -> Unit,
    viewModel: RouteFilesViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    BackHandler {
        scope.launch {
            if (uiState.showRouteGradient.isNotNull())
                viewModel.setShowRouteGradient(null)
            else if (uiState.showRouteChart.isNotNull())
                viewModel.setShowRouteChart(null)
            else
                selectRoute(null, RouteMenu.Home)
        }
    }
    Scaffold(
        topBar = {
            RouteFilesTopBar(
                uiState = uiState,
                onBackClick = { selectRoute(null, RouteMenu.Home) },
                onImportExportClick = { viewModel.setShowRoutesImportExportMenu(true) },
                onFilterClick = { viewModel.setAskForNameFilter(true) },
                onAddFolderClick = { viewModel.setNewRouteFolderMode(true) },
                onRefreshClick = { viewModel.refreshRoutes() },
                onSrtmClick = { viewModel.setShowSrtmFiles(true) }
            )
            
            if (uiState.createNewRouteFolder && uiState.newRouteFolder.isNotEmpty()) {
                NewRouteFolder(
                    uiState.newRouteFolder,
                    newFolder = { _ ->
                        viewModel.createNewRouteFolder()
                    }
                )
            }
            AnimatedVisibility(visible = uiState.newRouteFolderMode) {
                Row(
                    modifier = Modifier
                        .padding(top = 60.dp)
                        .background(Color.White)
                ) {
                    OutlinedTextField(
                        value = uiState.newRouteFolder,
                        onValueChange = { viewModel.setNewRouteFolder(it) },
                        label = { Text(stringResource(R.string.routefolder_name)) },
                        modifier = Modifier
                            .padding(start = 6.dp, end = 6.dp)
                            .fillMaxWidth(0.8f)
                    )
                    IconButton(
                        modifier = Modifier.align(alignment = Alignment.CenterVertically),
                        onClick = {
                            viewModel.createNewRouteFolder()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Done,
                            contentDescription = "Done"
                        )
                    }
                }
            }
        }) { _ ->
        uiState.snackRoutesData?.let {
            MoboSnack(it) {responseAction ->
                when(responseAction) {
                    SnackRoutesAction.Nothing -> viewModel.setSnackRoutesData(null)
                    SnackRoutesAction.RemoveRouteFolder -> {
                        val region = it.actionData as? String
                        region?.let { it1 ->
                            viewModel.deleteRouteFolder(it1)
                            viewModel.setSnackRoutesData(null)
                        }
                    }

                    SnackRoutesAction.ShowSrtmFiles -> {
                        viewModel.setSnackRoutesData(null)
                        viewModel.setShowSrtmFiles(true)
                    }

                    SnackRoutesAction.RemoveAllRoutes -> {
                        viewModel.setSnackRoutesData(null)
                        viewModel.deleteMainRouteFolder()
                    }
                }
            }
        }
        if (uiState.showSrtmFiles) {
            DropdownSrtmFiles(context, uiState.srtmFile) { file, import ->
                viewModel.setShowSrtmFiles(false)
                viewModel.setSrtmFile(file)
                if (import) {
                    FileImportActivity.launch(context, FileType.Hgt)
                }
            }
        }
        uiState.showSingleRouteImportExportMenu?.let { region ->
            DropdownSingleRouteImportExport { action ->
                when(action) {
                    SingleRouteAction.Nothing -> { viewModel.setShowSingleRouteImportExportMenu(null) }
                    SingleRouteAction.Text -> {
                        FileImportActivity.launch(context, FileType.Route, routeFolder = region)
                        viewModel.setShowSingleRouteImportExportMenu(null)
                    }
                    SingleRouteAction.Image -> {
                        FileImportActivity.launch(context, FileType.RouteThumbnail, routeFolder = region)
                        viewModel.setShowSingleRouteImportExportMenu(null)
                    }

                    SingleRouteAction.Cleanup -> {
                        viewModel.cleanUpRouteFolder(region)
                        viewModel.setShowSingleRouteImportExportMenu(null)
                    }
                }
            }
        }

        if (uiState.showRoutesImportExportMenu) {
            DropdownRoutesImportExport { action ->
                when(action) {
                    RoutesAction.Import -> {
                        viewModel.setShowRoutesImportExportMenu(false)
                        FileImportActivity.launch(context, FileType.RoutesZip)
                    }
                    RoutesAction.Export -> {
                        viewModel.setShowRoutesImportExportMenu(false)
                        viewModel.exportRoutes()
                    }
                    RoutesAction.Nothing -> viewModel.setShowRoutesImportExportMenu(false)
                    RoutesAction.Delete -> {
                        viewModel.setShowRoutesImportExportMenu(false)
                        viewModel.setSnackRoutesData(
                            SnackRoutesData(
                                RouteMenu.Placeholder,
                                context.getString(R.string.delete_all_routes),
                                action = SnackRoutesAction.RemoveAllRoutes,
                                actionText = context.getString(R.string.ok),
                                actionData = null
                            )
                        )
                    }

                    RoutesAction.ExportThumbnails -> {
                        viewModel.setShowRoutesImportExportMenu(false)
                        viewModel.exportThumbnails()
                    }
                    RoutesAction.ImportThumbnails -> {
                        viewModel.setShowRoutesImportExportMenu(false)
                        FileImportActivity.launch(context, FileType.ThumbnailsZip)
                    }
                }
            }
        }
        if (uiState.isLoading)
            ProgressDialog()

        if (uiState.dataChangeCompleted) {
            RouteFilesGroupedList(
                uiState.routeEntitiesSorted,
                selectRoute = { route, action ->
                    if (route != null) {
                        when (action) {
                            RouteEntityItemAction.Select -> {
                                viewModel.setShowRouteMoBo(route)
                            }

                            RouteEntityItemAction.Delete -> {
                                viewModel.deleteRoute(route)
                            }

                            RouteEntityItemAction.Map -> {
                                val rootFolder = File(context.filesDir, Const.ROUTEFOLDER)
                                val routeFolder = File(rootFolder, route.region)
                                val routeFile = File(routeFolder, route.name)
                                val lllh = if (route.name.endsWith(Const.JPG_EXT)) {
                                    Helpers.getCoordinatesFromExif(routeFile)
                                } else Helpers.getLllhFromFile(routeFile)
                                
                                val kmlString = lllh?.lllhToKmlString(route.name)
                                if (kmlString != null) {
                                    route.kmlString = kmlString
                                }
                                selectRoute(route, RouteMenu.Map)
                            }

                            RouteEntityItemAction.Hide -> {
                                Timber.i("Hide ${route.name}")
                            }

                            RouteEntityItemAction.Database -> {
                                viewModel.setSnackRoutesData(
                                    SnackRoutesData(
                                        RouteMenu.Placeholder,
                                        context.getString(R.string.added_to_database, route.name),
                                        action = SnackRoutesAction.Nothing, actionText = null, null
                                    )
                                )
                            }
                        }
                    }
                },
                deleteRouteFolder = { region ->
                    region?.let {
                        viewModel.setSnackRoutesData(
                            SnackRoutesData(
                                RouteMenu.Placeholder,
                                context.getString(R.string.remove_route_folder, it),
                                action = SnackRoutesAction.RemoveRouteFolder,
                                actionText = context.getString(R.string.ok),
                                actionData = it
                            )
                        )
                    }
                }, singleRouteMenu = {routeFolder ->
                    viewModel.setShowSingleRouteImportExportMenu(routeFolder)
                }
            )
        }

        uiState.showRouteMoBo?.let { route ->
            RouteFileMoBoSheet(route) { action ->
                when (action) {
                    RouteMenu.Home -> viewModel.setShowRouteMoBo(null)
                    RouteMenu.RefreshPreview -> {
                        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                        val routeFolder = File(rootRouteFolder, route.region)
                        val routeFile = File(routeFolder, route.name)
                        val lllh = if (route.name.endsWith(Const.JPG_EXT)) {
                            Helpers.getCoordinatesFromExif(routeFile)
                        } else Helpers.getLllhFromFile(routeFile)

                        if (lllh != null) {
                            val routeCenter = lllh.getCenter()
                            val mvtTileMatch = pointToTile(routeCenter.longitude, routeCenter.latitude, 9.0)
                            val mvtMatchingMap = "${Const.MVT_PREFIX}${mvtTileMatch.x}_${mvtTileMatch.y}_${mvtTileMatch.z}"
                            val mvtRootFolder = File(context.filesDir, Const.MVT_FOLDER)
                            val mvtMatchingFile = File(mvtRootFolder, mvtMatchingMap.plus(Const.MBTILES_EXT))
                            val preferences = getDefaultSharedPreferences(context)
                            val mvtCurrentPath = preferences.getString(Const.PREF_MVT_FILEPATH, null)
                            var baseMapChange = false
                            if (mvtMatchingFile.exists() && mvtMatchingFile.path != mvtCurrentPath) {
                                preferences.edit { putString(Const.PREF_MVT_FILEPATH, mvtMatchingFile.path) }
                                baseMapChange = true
                            }
                            Helpers.takeRouteSnapshot(context, lllh, route.name, Const.styleVectorUri, 512, 0.1, true, routeFolder)
                            { snapShot, _ ->
                                snapShot?.let { snapshot ->
                                    addLineToSnapshotWithGradient(snapshot, lllh)
                                    route.bitmap = snapShot.bitmap
                                    viewModel.setShowRouteMoBo(null)
                                    
                                    val routeDisplayName = route.name.removeSuffix(Const.GPX_EXT).removeSuffix(Const.KML_EXT).removeSuffix(Const.JPG_EXT)
                                    val snackTitle = StringBuilder(context.getString(R.string.refresh_route_preview_ready, routeDisplayName))
                                    if (baseMapChange) {
                                        snackTitle.append("\n").append(context.getString(R.string.vector_map_changed_to_, mvtMatchingFile.name))
                                    } else if (mvtMatchingFile.exists()) {
                                        snackTitle.append("\n").append(context.getString(R.string.vector_map_used_, mvtMatchingFile.name))
                                    } else {
                                        snackTitle.append("\n").append(context.getString(R.string.vector_map_missing_, mvtMatchingFile.name))
                                    }

                                    viewModel.setSnackRoutesData(
                                        SnackRoutesData(RouteMenu.RefreshPreview, snackTitle.toString(), action = SnackRoutesAction.Nothing, actionText = null, snapShot.bitmap)
                                    )
                                }
                            }
                        }
                    }
                    RouteMenu.Map -> {
                        val rootFolder = File(context.filesDir, Const.ROUTEFOLDER)
                        val routeFolder = File(rootFolder, route.region)
                        val routeFile = File(routeFolder, route.name)
                        val lllh = if (route.name.endsWith(Const.JPG_EXT)) {
                            Helpers.getCoordinatesFromExif(routeFile)
                        } else Helpers.getLllhFromFile(routeFile)
                        val kmlString = lllh?.lllhToKmlString(route.name)
                        if (kmlString != null) {
                            route.kmlString = kmlString
                        }
                        selectRoute(route, RouteMenu.Map)
                        viewModel.setShowRouteMoBo(null)
                    }
                    RouteMenu.MoveFile -> {
                        if (route.name == "routes.geojson") {
                            viewModel.moveGeoJsonFile(route)
                        } else {
                            viewModel.setAskForRouteName(route.copy(), isMove = true)
                        }
                        viewModel.setShowRouteMoBo(null)
                    }
                    RouteMenu.DeleteFile -> {
                        if (route.name == "routes.geojson") {
                            viewModel.deleteGeoJsonFile(route)
                        } else {
                            viewModel.deleteRouteWithFile(route)
                        }
                        viewModel.setShowRouteMoBo(null)
                    }
                    RouteMenu.ShareFile -> {
                        shareRouteFile(context, route)
                        viewModel.setShowRouteMoBo(null)
                    }
                    RouteMenu.ShareSnapshot -> {
                        shareRouteSnapshot(context, route)
                        viewModel.setShowRouteMoBo(null)
                    }
                    RouteMenu.SaveFile -> {
                        viewModel.setAskForRouteName(route.copy(), isMove = false)
                        viewModel.setShowRouteMoBo(null)
                    }
                    RouteMenu.InsertIntoDatabase -> {
                        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                        val routeFolder = File(rootRouteFolder, route.region)
                        val routeFile = File(routeFolder, route.name)

                        if (routeFile.exists()) {
                            val lllh = if (route.name.endsWith(Const.JPG_EXT)) {
                                Helpers.getCoordinatesFromExif(routeFile)
                            } else Helpers.getLllhFromFile(routeFile)
                            val region = route.region
                            val name = route.name
                            lllh?.let {
                                viewModel.takeSnapshot(lllh, region, name, routeFolder) {
                                    viewModel.setSnackRoutesData(
                                        SnackRoutesData(action, context.getString(R.string.save_to_database_ready, name), action = SnackRoutesAction.Nothing, actionText = null, null)
                                    )
                                }
                            }
                        }
                        viewModel.setShowRouteMoBo(null)
                    }
                    RouteMenu.Chart -> {
                        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                        val routeFolder = File(rootRouteFolder, route.region)
                        val routeFile = File(routeFolder, route.name)

                        if (routeFile.exists()) {
                            viewModel.setShowRouteChart(routeFile)
                        }
                        viewModel.setShowRouteMoBo(null)
                    }
                    RouteMenu.Gradient -> {
                        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                        val routeFolder = File(rootRouteFolder, route.region)
                        val routeFile = File(routeFolder, route.name)
                        if (routeFile.exists()) {
                            val lllh = if (route.name.endsWith(Const.JPG_EXT)) {
                                Helpers.getCoordinatesFromExif(routeFile)
                            } else Helpers.getLllhFromFile(routeFile)
                            if (lllh.isNullOrEmpty().not()) {
                                val kmlString = lllh!!.lllhToKmlString(route.name)
                                val gradientRoute = route.copy(kmlString = kmlString)
                                viewModel.setShowRouteGradient(gradientRoute)
                            }
                        }
                        viewModel.setShowRouteMoBo(null)
                    }
                    RouteMenu.ElevationRefreshFromSrtm -> {
                        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                        val routeFolder = File(rootRouteFolder, route.region)
                        val routeFile = File(routeFolder, route.name)
                        if (routeFile.exists()) {
                            val lllh = if (route.name.endsWith(Const.JPG_EXT)) {
                                Helpers.getCoordinatesFromExif(routeFile)
                            } else Helpers.getLllhFromFile(routeFile)
                            val hgtFile = uiState.srtmFile
                            if (hgtFile != null && hgtFile.exists()) {
                                scope.launch {
                                    viewModel.setIsLoading(true)
                                    val hgtReader = HgtReader(context, hgtFile)
                                    val refreshedLllh = withContext(Dispatchers.IO) {
                                        hgtReader.refreshRouteElevationFromSrtm(lllh).lllh
                                    }
                                    val path = routeFile.path.replace(Const.GPX_EXT, Const.KML_EXT).replace(Const.JPG_EXT, Const.KML_EXT)
                                    val result = withContext(Dispatchers.IO) {
                                        Helpers.writeLllh2KmlFile(refreshedLllh, path)
                                    }
                                    if (result) {
                                        viewModel.setSnackRoutesData(
                                            SnackRoutesData(action, context.getString(R.string.saved_to_, path), action = SnackRoutesAction.Nothing, actionText = null, null)
                                        )
                                    }
                                    viewModel.setIsLoading(false)
                                }
                            } else {
                                viewModel.setSnackRoutesData(
                                    SnackRoutesData(action, context.getString(R.string.select_hgt_file), action = SnackRoutesAction.ShowSrtmFiles, actionText = context.getString(R.string.ok), null)
                                )
                            }
                        }
                        viewModel.setShowRouteMoBo(null)
                    }
                    RouteMenu.ElevationGmsService -> {
                        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                        val routeFolder = File(rootRouteFolder, route.region)
                        val routeFile = File(routeFolder, route.name)
                        if (routeFile.exists()) {
                            val lllh = if (route.name.endsWith(Const.JPG_EXT)) {
                                Helpers.getCoordinatesFromExif(routeFile)
                            } else Helpers.getLllhFromFile(routeFile)
                            if (lllh != null) {
                                val gmsLatLng = if (lllh.size > MAX_ELEVATION_POINTS) {
                                    lllh.simplifyToTargetCount(MAX_ELEVATION_POINTS)
                                } else {
                                    lllh
                                }.map { it.latLngGms }
                                val encodedPolyline = PolyUtil.encode(gmsLatLng)
                                scope.launch {
                                    viewModel.setIsLoading(true)
                                    val refreshedLllh = MapUtils.gmsElevationService(context, "enc:${encodedPolyline}")
                                    if (refreshedLllh.isNotEmpty()) {
                                        val path = routeFile.path.replace(Const.GPX_EXT, Const.KML_EXT).replace(Const.JPG_EXT, Const.KML_EXT)
                                        val result = Helpers.writeLllh2KmlFile(refreshedLllh, path)
                                        if (result)
                                            viewModel.setSnackRoutesData(
                                                SnackRoutesData(action, context.getString(R.string.saved_to_, path), action = SnackRoutesAction.Nothing, actionText = null, null)
                                            )
                                    }
                                    viewModel.setIsLoading(false)
                                }
                            }
                        }
                        viewModel.setShowRouteMoBo(null)
                    }
                    RouteMenu.Placeholder -> {}
                }
            }
        }
        uiState.showRouteChart?.let { file ->
            val lllh = if (file.name.endsWith(Const.JPG_EXT)) {
                Helpers.getCoordinatesFromExif(file)
            } else Helpers.getLllhFromFile(file)
            val lllhReduced = lllh?.reduceWithTolerance(200.0)
            ModalBottomSheet(onDismissRequest = { viewModel.setShowRouteChart(null) }) {
                LineYGraphLllh(lllhReduced, file.name, 0F, { viewModel.setShowRouteChart(null) }, {}, Icons.AutoMirrored.Filled.ArrowBack)
            }
        }
        uiState.showRouteGradient?.let { route ->
            val lllh = route.kmlString.kmlString2Lllh()
            val distRoute = lllh.getDistanceFromLllh()
            ModalBottomSheet(onDismissRequest = { viewModel.setShowRouteGradient(null) }) {
                GradientChartMonitor(
                    route,
                    0.0f, Icons.AutoMirrored.Filled.ArrowBack,
                    result = {
                        viewModel.setShowRouteGradient(null)
                    }, animated = true
                )
                Text(
                    text = distRoute.formatDistM(true),
                    Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(96.dp))
            }
        }
        uiState.askForRouteName?.let { routeEntity ->
            val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
            val routeFolder = File(rootRouteFolder, routeEntity.region)
            var routeFile = File(routeFolder, routeEntity.name)
            if (routeFile.name.endsWith(Const.JPG_EXT)) {
                val lllh = Helpers.getCoordinatesFromExif(routeFile)
                val newFileName = routeFile.name.replace(Const.JPG_EXT, Const.KML_EXT)
                val newRouteFile = File(routeFolder, newFileName)
                Helpers.writeLllh2KmlFile(lllh, newRouteFile.path)
                viewModel.refreshRoutes()
                viewModel.setAskForRouteName(null)
                viewModel.setSnackRoutesData(
                    SnackRoutesData(RouteMenu.Placeholder, context.getString(R.string.route_file_created_, newRouteFile.path), action = SnackRoutesAction.Nothing, actionText = null, null)
                )
            } else {
                RouteFileSaveMoBoSheet(routeEntity.name) { targetFileName, targetRouteFolder ->
                    if (targetRouteFolder != null) {
                        if (routeEntity.region == Const.ROUTEFOLDER)
                            routeFile = File(rootRouteFolder, routeEntity.name)
                        var newFileName = targetFileName
                        if (routeFile.exists()) {
                            if (!targetFileName.contains("."))
                                newFileName = targetFileName.plus((Const.KML_EXT))
                            val newRouteFile = File(targetRouteFolder.second, newFileName)
                            routeFile.copyTo(newRouteFile, overwrite = true)
                            
                            if (uiState.moveFile) {
                                routeFile.delete()
                            }
                            viewModel.setSnackRoutesData(
                                SnackRoutesData(RouteMenu.Placeholder, context.getString(R.string.route_file_created_, newRouteFile.path), action = SnackRoutesAction.Nothing, actionText = null, null)
                            )
                        }
                    }
                    viewModel.setAskForRouteName(null)
                    viewModel.refreshRoutes()
                }
            }
        }

        if (uiState.askForNameFilter) {
            AskForRouteNameFilter(uiState.routeEntities, filter = { filter, region ->
                viewModel.applyFilter(filter, region)
            })
        }
    }
}

@ComposePreview(showBackground = true)
@Composable
fun RouteFilesGroupedListPreview() {
    val sampleRouteEntities = listOf(
        RouteEntity(name = "Hiking Trail.kml", region = "Mountains"),
        RouteEntity(name = "City Walk.gpx", region = "City"),
        RouteEntity(name = "Forest Path.jpg", region = "Mountains")
    )
    RamaniTheme {
        RouteFilesGroupedList(
            routeEntities = sampleRouteEntities,
            selectRoute = { _, _ -> },
            deleteRouteFolder = {},
            singleRouteMenu = {_ ->}
        )
    }
}

fun filterByRegion(region: String?, routeEntities: List<RouteEntity>): List<RouteEntity> {
    val filteredRouteEntities: ArrayList<RouteEntity> = ArrayList()
    routeEntities.forEach { routeEntity ->
        if (routeEntity.region == region)
            filteredRouteEntities.add(routeEntity)
    }
    return filteredRouteEntities
}

fun filterByName(name: String?, routeEntities: List<RouteEntity>): List<RouteEntity> {
    val filteredRouteEntities: ArrayList<RouteEntity> = ArrayList()
    name?.let {
        routeEntities.forEach { routeEntity ->
            if (routeEntity.name.contains(it).or(routeEntity.name.lowercase().contains(it)))
                filteredRouteEntities.add(routeEntity)
        }
    }
    return filteredRouteEntities
}

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RouteFilesGroupedList(
    routeEntities: List<RouteEntity>,
    selectRoute: (RouteEntity?, RouteEntityItemAction) -> Unit,
    deleteRouteFolder: (String?) -> Unit,
    singleRouteMenu: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val marginTopDp = TopAppBarDefaults.TopAppBarExpandedHeight.value
    var groupExpanded by remember { mutableStateOf<String?>(null) }
    val routesGrouped = routeEntities.groupBy { it.region }
    Scaffold(modifier = Modifier.padding(top = marginTopDp.dp, bottom = (marginTopDp * 1.4).dp))
        { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            routesGrouped.forEach { (initial, routeEntities) ->
                stickyHeader {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(color = Color.LightGray),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BadgedBox(badge = { Badge { Text("${routeEntities.size}") } }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    singleRouteMenu(initial)
                                }) {
                                    Icon(
                                        Icons.Outlined.ImportExport,
                                        contentDescription = null
                                    )
                                }
                                IconButton(onClick = {
                                    val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                                    @SuppressLint("SimpleDateFormat") val timeFormat =
                                        SimpleDateFormat(Const.TIME_PATTERN_LONG_YEAR)
                                    val timeTag = timeFormat.format(System.currentTimeMillis())
                                    val zipFile = File(context.cacheDir, "${Const.ROUTEFOLDER}_${initial}_${timeTag}${Const.ZIP_EXT}")
                                    val routeFolder = File(rootRouteFolder, initial)
                                    lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                        UnzipUtils.zipRouteSubFolder(routeFolder, zipFile)
                                    }.invokeOnCompletion {
                                        val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", zipFile)
                                        val intent = Intent(Intent.ACTION_SEND)
                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        intent.type = "*/*"
                                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(intent)
                                    }
                                }) {
                                    Icon(
                                        Icons.Outlined.Share,
                                        contentDescription = null
                                    )
                                }

                                TextButton(onClick = {
                                    groupExpanded = if (groupExpanded != null && groupExpanded == initial)
                                        null else initial
                                }) {
                                    Text(
                                        text = initial,
                                        modifier = Modifier.fillMaxWidth(0.5f),
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
                                    deleteRouteFolder(initial)
                                }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }
                }

                items(routeEntities) { routeItem ->
                    if(groupExpanded != null && routeItem.region == groupExpanded) {
                        RouteFilesItem(context, routeItem, onItemClick = { item, action ->
                            selectRoute(item, action)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun RouteFilesItem(
    context: Context,
    routeItem: RouteEntity,
    onItemClick: (RouteEntity, RouteEntityItemAction) -> Unit
) {
    var routeDbState by remember { mutableIntStateOf(0) }
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val isPreview = LocalInspectionMode.current
    LaunchedEffect(routeItem.name, routeItem.region) {
        if (!isPreview) {
            withContext(Dispatchers.IO) {
                val routeRepository = RouteRepository.getInstance(
                    context.applicationContext,
                    Executors.newSingleThreadExecutor()
                )
                val matchName = routeItem.name.replace(Const.GPX_EXT, "")
                    .replace(Const.KML_EXT, "").replace(Const.JPG_EXT, "")
                routeRepository.findRoute(matchName, routeItem.region) { result ->
                    routeDbState = result.size
                }

                // Load thumbnail asynchronously
                val folderThumbnails = File(context.filesDir, Const.THUMBNAILS)
                val picFileName = routeItem.name.replace(Const.GPX_EXT, Const.JPG_EXT)
                    .replace(Const.KML_EXT, Const.JPG_EXT)
                val picFile = if (routeItem.name.endsWith(Const.JPG_EXT)) {
                    val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                    File(File(rootRouteFolder, routeItem.region), routeItem.name)
                } else {
                    File(folderThumbnails, picFileName)
                }

                if (picFile.exists()) {
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2 // Optimization: Load smaller version
                    }
                    loadedBitmap = BitmapFactory.decodeFile(picFile.path, options)
                }
            }
        }
    }

    val updatedItem = if (loadedBitmap != null) routeItem.copy(bitmap = loadedBitmap) else routeItem
    RouteFilesItemUI(
        routeItem = updatedItem,
        routeDbState = routeDbState,
        onItemClick = { onItemClick(updatedItem, RouteEntityItemAction.Select) },
        onMapClick = { onItemClick(updatedItem, RouteEntityItemAction.Map) },
        onDatabaseClick = {
            val routeRepository = RouteRepository.getInstance(
                context.applicationContext,
                Executors.newSingleThreadExecutor()
            )
            val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
            val routeFolder = File(rootRouteFolder, updatedItem.region)
            val routeFile = File(routeFolder, updatedItem.name)
            if (routeFile.exists()) {
                val lllh = Helpers.getLllhFromFile(routeFile)
                if (!lllh.isNullOrEmpty()) {
                    val routeEntityDatabase = lllh.let {
                        RouteEntity(
                            UUID.randomUUID(), routeFile.name,
                            routeFile.parentFile?.name.toString(),
                            kmlString = lllh.lllhToKmlString(routeFile.name),
                            latitudeStart = it[0].latitude,
                            longitudeStart = it[0].longitude,
                            bitmap = updatedItem.bitmap
                        )
                    }
                    routeRepository.addRoute(routeEntityDatabase) {
                        routeDbState = 1
                        onItemClick(updatedItem, RouteEntityItemAction.Database)
                    }
                }
            }
        }
    )
}

@Composable
fun ProgressDialog() {
    AlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        confirmButton = {},
        title = { Text(stringResource(R.string.loading)) }
    )
}

@Composable
fun RouteFilesItemUI(
    routeItem: RouteEntity,
    routeDbState: Int,
    onItemClick: () -> Unit,
    onMapClick: () -> Unit,
    onDatabaseClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable { onItemClick() }
            .fillMaxWidth()
            .padding(bottom = 3.dp, start = 3.dp, end = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 5.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    modifier = Modifier
                        .align(alignment = Alignment.CenterVertically)
                        .weight(0.75f),
                    text = routeItem.name.replace(Const.GPX_EXT, "")
                        .replace(Const.KML_EXT, "")
                        .replace(Const.JPG_EXT, ""),
                    fontSize = 14.sp
                )
                IconButton(
                    onClick = onMapClick, modifier = Modifier
                        .align(alignment = Alignment.CenterVertically)
                        .weight(0.125f)
                ) {
                    Icon(Icons.Outlined.Map, null)
                }

                if (routeDbState == 0) {
                    IconButton(modifier = Modifier
                        .align(alignment = Alignment.CenterVertically)
                        .weight(0.125f), onClick = onDatabaseClick) {
                        Icon(painterResource(R.drawable.database_24), null)
                    }
                } else {
                    Spacer(modifier = Modifier.weight(0.125f))
                }

            }
            Spacer(modifier = Modifier.height(4.dp))
            if (routeItem.bitmap != null) {
                val imageBitmap = routeItem.bitmap!!.asImageBitmap()
                Image(
                    painter = BitmapPainter(
                        imageBitmap,
                        IntOffset(0, 0),
                        IntSize(routeItem.bitmap!!.width, routeItem.bitmap!!.height)
                    ),
                    contentDescription = routeItem.name
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

enum class SingleRouteAction{
    Nothing,
    Text,
    Image,
    Cleanup
}

@Composable
fun DropdownSingleRouteImportExport(action: (SingleRouteAction) -> Unit) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = { action(SingleRouteAction.Nothing) }
    ) {
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Outlined.FileDownload, null) },
            text = { Text(text = stringResource(R.string.import_file)) },
            onClick = { action(SingleRouteAction.Text) }
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Outlined.Image, null) },
            text = { Text(text = stringResource(R.string.import_photo)) },
            onClick = { action(SingleRouteAction.Image) }
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Outlined.ClearAll, null) },
            text = { Text(text = stringResource(R.string.cleanup)) },
            onClick = { action(SingleRouteAction.Cleanup) }
        )
    }
}

enum class RoutesAction {
    Import,
    Export,
    Delete,
    ExportThumbnails,
    ImportThumbnails,
    Nothing
}

@Composable
fun DropdownRoutesImportExport(action: (RoutesAction) -> Unit) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = { action(RoutesAction.Nothing) }
    ) {
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Outlined.ImportExport, null) },
            text = { Text(text = stringResource(R.string.import_title)) },
            onClick = { action(RoutesAction.Import) }
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Outlined.Share, null) },
            text = { Text(text = stringResource(R.string.share)) },
            onClick = { action(RoutesAction.Export) }
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Outlined.Delete, null) },
            text = { Text(text = stringResource(R.string.delete_all_routes)) },
            onClick = { action(RoutesAction.Delete) }
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Outlined.ImportExport, null) },
            text = { Text(text = stringResource(R.string.import_thumbnails)) },
            onClick = { action(RoutesAction.ImportThumbnails) }
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Outlined.Share, null) },
            text = { Text(text = stringResource(R.string.export_thumbnails)) },
            onClick = { action(RoutesAction.ExportThumbnails) }
        )
    }
}

@Composable
fun DropdownSrtmFiles(context: Context, srtmFile: File?, selected: (File?, Boolean) -> Unit) {
    val hgtFolder = File(context.filesDir, Const.HGT_FOLDER_NAME)
    val hgtFiles = hgtFolder.listFiles()
    hgtFiles?.sort()
    Surface(Modifier.fillMaxWidth().padding(top = 100.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.align(Alignment.TopEnd)) {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { selected(null, false) }
                ) {
                    DropdownMenuItem(
                        trailingIcon = { Icon(Icons.Outlined.ImportExport, null) },
                        text = { Text(text = stringResource(R.string.import_title), textDecoration = TextDecoration.Underline) },
                        onClick = { selected(null, true) }
                    )
                    hgtFiles?.forEach { file ->
                        DropdownMenuItem(
                            trailingIcon = {
                                if (srtmFile?.name == file.name) Icon(Icons.Outlined.Check, null)
                            },
                            text = { Text(text = file.name) },
                            onClick = { selected(file, false) }
                        )
                    }
                }
            }
        }
    }
}

fun deleteMainRouteFolder(context: Context, finished: () -> Unit) {
    val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
    rootRouteFolder.deleteRecursively()
    val thumbnailFolder = File(context.filesDir, Const.THUMBNAILS)
    thumbnailFolder.deleteRecursively()
    finished()
}

fun deleteFilesOlderThan(folder: File, days: Int) {
    val twoDaysAgo = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
    Helpers.deleteFilesOlderThan(folder, twoDaysAgo, recursive = false)
}

fun cleanUpRouteFolder(context: Context, routeFolder: File?, finished: (Int, Int) -> Unit) {
    var renameCount = 0
    val suffixMap = listOf(
        (Const.JPG_EXT + Const.JPG_EXT) to Const.JPG_EXT,
        (Const.KML_EXT + Const.GPX_EXT) to Const.GPX_EXT,
        (Const.KML_EXT + Const.KML_EXT) to Const.KML_EXT,
        (Const.GPX_EXT + Const.KML_EXT) to Const.KML_EXT,
        (Const.GPX_EXT + Const.GPX_EXT) to Const.GPX_EXT
    )
    var deleteCount = 0
    suffixMap.forEach { (badSuffix, goodSuffix) ->
        routeFolder?.listFiles { f -> f.name.endsWith(badSuffix) }?.forEach { f ->
            val newName = f.name.removeSuffix(badSuffix) + goodSuffix
            if (File(routeFolder, newName).delete()) deleteCount++
            if (f.renameTo(File(routeFolder, newName))) renameCount++
        }
    }

    val files = routeFolder?.listFiles { file -> file.extension.equals("jpg", ignoreCase = true) }
        ?: emptyArray()
    files.forEach { f ->
        val baseName = f.nameWithoutExtension
        if (File(routeFolder, "$baseName${Const.KML_EXT}").delete()) deleteCount++
        if (File(routeFolder, "$baseName${Const.GPX_EXT}").delete()) deleteCount++
    }

    finished(deleteCount, renameCount)
}

fun deleteRouteFolder(context: Context, region: String, routeEntities: List<RouteEntity>,
                      finished: (List<RouteEntity>, List<RouteEntity>) -> Unit) {
    val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
    val routeFolder = File(rootRouteFolder, region)
    routeFolder.deleteRecursively()
    val newRouteEntities = routeEntities.filter { it.region != region }
    val routeEntitiesSorted = newRouteEntities.sortedBy { it.region + it.name }.toMutableList()
    finished(newRouteEntities, routeEntitiesSorted)
}

fun getAllRoutesSimple(context: Context): List<RouteEntity> {
    val routeEntities = ArrayList<RouteEntity>()
    val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
    if (!rootRouteFolder.exists()) return emptyList()

    rootRouteFolder.walkTopDown().forEach { routeFile ->
        if (routeFile.isFile && !routeFile.name.endsWith(Const.GEOJSON_EXT)) {
            val routeEntity =
                RouteEntity(
                    UUID.randomUUID(), routeFile.name,
                    routeFile.parentFile?.name.toString()
                )
            routeEntities.add(routeEntity)
        }
    }
    return routeEntities
}

fun shareRouteSnapshot(context: Context, routeEntity: RouteEntity) {
    val rootFolder = context.filesDir
    val routesRootFolder = File(rootFolder, Const.ROUTEFOLDER)
    val routesFolder = File(routesRootFolder, routeEntity.region)
    val folderThumbnails = File(context.filesDir, Const.THUMBNAILS)
    var routeSnapshotFile = File(
        folderThumbnails,
        routeEntity.name.replace(Const.KML_EXT, Const.JPG_EXT).replace(Const.GPX_EXT, Const.JPG_EXT)
            .replace(Const.GEOJSON_EXT, Const.JPG_EXT))
    if (!routeSnapshotFile.exists())
        routeSnapshotFile = File(
            routesFolder,
            routeEntity.name.replace(Const.KML_EXT, Const.JPG_EXT).replace(Const.GPX_EXT, Const.JPG_EXT)
                .replace(Const.GEOJSON_EXT, Const.JPG_EXT))
    try {
        if (routeSnapshotFile.exists()) {
            val exifInterfaceSource = ExifInterface(routeSnapshotFile.path)
            val kmlString = exifInterfaceSource.getAttribute(ExifInterface.TAG_USER_COMMENT)
            val distString = exifInterfaceSource.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)
            val latLngArray = exifInterfaceSource.latLong

            val options = BitmapFactory.Options().apply { inMutable = true }
            val thumbnail = BitmapFactory.decodeFile(routeSnapshotFile.path, options) ?: return
            val bmp: Bitmap = createBitmap(thumbnail.width, thumbnail.height + 30)
            bmp.let {
                val thumbCanvas = Canvas(it)
                thumbCanvas.drawColor(android.graphics.Color.WHITE)
                thumbCanvas.drawBitmap(thumbnail, 0f, 0f, null)
                val name = routeSnapshotFile.name.replace(Const.JPG_EXT, "")
                drawRouteName(context, thumbCanvas, name)
                val file = File(context.cacheDir, routeSnapshotFile.name)
                file.createNewFile()

                val out = FileOutputStream(file)
                it.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()
                val exifInterface = ExifInterface(file.path)
                exifInterface.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, distString)
                exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())

                kmlString?.let { kml ->
                    if (kml.length < Const.EXIF_MAX_SIZE) {
                        exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, kml)
                    }
                }
                latLngArray?.let { array -> exifInterface.setLatLong(array[0], array[1]) }
                exifInterface.saveAttributes()

                val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    type = "*/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    } catch (e: Exception) {
        Timber.e(e)
    }
}

fun drawRouteName(context: Context, thumbCanvas: Canvas, name: String, textSize: Float = 32f) {
    val bgTextPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.white_transparent_)
        isAntiAlias = true
        strokeWidth = context.resources.getDimension(R.dimen.thumbLineWidth)
        style = Paint.Style.FILL
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    val textPaint = TextPaint().apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        color = ContextCompat.getColor(context, R.color.design_default_color_primary)
        isAntiAlias = true
        strokeWidth = 2f
        this.textSize = textSize
    }
    val textBounds = Rect()
    textPaint.getTextBounds(name, 0, name.length, textBounds)
    thumbCanvas.drawRect(textBounds, bgTextPaint)
    thumbCanvas.drawText(name, 5F, (thumbCanvas.height - 0.25f * textBounds.height()), textPaint)
}

fun drawLastPageIndicator(context: Context, thumbCanvas: Canvas, name: String, textSize: Float = 64f) {
    val bgTextPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.white_transparent_)
        isAntiAlias = true
        strokeWidth = context.resources.getDimension(R.dimen.thumbLineWidth) * 2
        style = Paint.Style.FILL
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    val textPaint = TextPaint().apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        color = ContextCompat.getColor(context, R.color.design_default_color_primary)
        isAntiAlias = true
        strokeWidth = 2f
        this.textSize = textSize
    }
    val textBounds = Rect()
    textPaint.getTextBounds(name, 0, name.length, textBounds)
    thumbCanvas.drawRect(textBounds, bgTextPaint)
    thumbCanvas.drawText(name, 0.5f * (thumbCanvas.width - textBounds.width()), (0.5f * thumbCanvas.height - 0.25f * textBounds.height()), textPaint)
}

fun shareRouteFile(context: Context, routeEntity: RouteEntity) {
    val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
    var routeFile : File
    var routeSnapshotFile : File? = null
    if (routeEntity.name == "routes.geojson")
        routeFile = File(rootRouteFolder, routeEntity.name)
    else {
        val routeFolder = File(rootRouteFolder, routeEntity.region)
        routeFile = File(routeFolder, routeEntity.name)
        val folderThumbnails = File(context.filesDir, Const.THUMBNAILS)
        routeSnapshotFile = File(
            folderThumbnails,
            routeEntity.name.replace(Const.KML_EXT, Const.JPG_EXT)
                .replace(Const.GPX_EXT, Const.JPG_EXT)
                .replace(Const.GEOJSON_EXT, Const.JPG_EXT)
        )
    }
    try {
        if(routeFile.exists()) {
            val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", routeFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
        if (routeSnapshotFile != null && routeSnapshotFile.exists()) {
            val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", routeSnapshotFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        Timber.e(e)
    }
}

@Composable
fun RouteFilesRegionList(
    paddingValues: PaddingValues,
    routeEntities: List<RouteEntity>?,
    selectRegion: (String?, Boolean) -> Unit
) {
    val regions = createRegionArray(routeEntities)
    LazyColumn(
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(regions) { region ->
            Box(
                modifier = Modifier
                    .background(color = Color.White)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { selectRegion(region, false) },
                            onLongPress = { selectRegion(region, true) }
                        )
                    }
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    region?.let {
                        Text(
                            text = it,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

fun createRegionArray(routeEntities: List<RouteEntity>?) : Array<String?> {
    val regionList = ArrayList<String>()
    if (routeEntities != null) {
        for (routeEntity in routeEntities) {
            if (!regionList.contains(routeEntity.region))
                regionList.add(routeEntity.region)
        }
    }
    if (regionList.size == 1)
        regionList.add(0, "")
    var regionArr = arrayOfNulls<String>(regionList.size)
    regionArr = regionList.toArray(regionArr)
    return regionArr
}

@Composable
fun AskForRouteNameFilter(routeEntities: List<RouteEntity>?, filter: (String?, String?) -> Unit) {
    var nameFilter by remember { mutableStateOf("") }
    Surface {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { filter(null, null) }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back home")
                }
                Text(text = stringResource(R.string.regions_), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(0.8f))
            }
            Spacer(modifier = Modifier.height(4.dp))
            RouteFilesRegionList(PaddingValues(0.dp), routeEntities) { region, _ ->
                filter(null, region)
            }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(modifier = Modifier.fillMaxWidth(0.7f),
                    value = nameFilter, onValueChange = { nameFilter = it },
                    label = { Text(stringResource(R.string.name_filter)) })
                TextButton(onClick = { filter(nameFilter, null) }) {
                    Text(Const.UC_CHECKMARK)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteFileMoBoSheet(routeEntity: RouteEntity, routeMenu: (action: RouteMenu) -> Unit) {
    ModalBottomSheet(onDismissRequest = { routeMenu(RouteMenu.Home) }) {
        Column {
            Text(text = routeEntity.region + " " + routeEntity.name, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Blue),
                    onClick = { routeMenu(RouteMenu.Map) }) {
                    Row {
                        Icon(painterResource(R.drawable.outline_map_24), contentDescription = stringResource(R.string.map))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(0.8f), fontSize = 14.sp, text = stringResource(R.string.map), color = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                Button(
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Blue),
                    onClick = { routeMenu(RouteMenu.RefreshPreview) }) {
                    Row {
                        Icon(Icons.Outlined.Preview, contentDescription = stringResource(R.string.refresh_route_preview))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(0.8f), fontSize = 14.sp, text = stringResource(R.string.refresh_route_preview), color = Color.Black)
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Blue),
                    onClick = { routeMenu(RouteMenu.Chart) }) {
                    Row {
                        Icon(painterResource(R.drawable.monitoring_24px), contentDescription = stringResource(R.string.elevation_chart))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(0.8f), fontSize = 14.sp, text = stringResource(R.string.elevation_chart), color = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                Button(
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Blue),
                    onClick = { routeMenu(RouteMenu.Gradient) }) {
                    Row {
                        Icon(painterResource(R.drawable.gradient_24px), contentDescription = stringResource(R.string.gradient))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(0.8f), fontSize = 14.sp, text = stringResource(R.string.gradient), color = Color.Black)
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Blue),
                    onClick = { routeMenu(RouteMenu.ElevationGmsService) }) {
                    Row {
                        Icon(Icons.Outlined.Height, contentDescription = stringResource(R.string.gms_elevation_service))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(0.8f), fontSize = 14.sp, text = stringResource(R.string.gms_elevation_service), color = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                Button(
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Blue),
                    onClick = { routeMenu(RouteMenu.ElevationRefreshFromSrtm) }) {
                    Row {
                        Icon(Icons.Outlined.Height, contentDescription = stringResource(R.string.elevation_refresh))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(0.8f), fontSize = 14.sp, text = stringResource(R.string.elevation_refresh), color = Color.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Blue),
                    onClick = { routeMenu(RouteMenu.SaveFile) }) {
                    Row {
                        Icon(painterResource(R.drawable.file_save_24px), contentDescription = stringResource(R.string.save_route))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(0.8f), fontSize = 14.sp, text = stringResource(R.string.save_route), color = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                Button(
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Blue),
                    onClick = { routeMenu(RouteMenu.DeleteFile) }) {
                    Row {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete_file))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(0.8f), fontSize = 14.sp, text = stringResource(R.string.delete_file), color = Color.Black)
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Blue),
                    onClick = { routeMenu(RouteMenu.ShareFile) }) {
                    Row {
                        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share_file))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(0.8f), fontSize = 14.sp, text = stringResource(R.string.share_file), color = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                Button(
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Blue),
                    onClick = { routeMenu(RouteMenu.ShareSnapshot) }) {
                    Row {
                        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share_route_snapshot))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(0.8f), fontSize = 14.sp, text = stringResource(R.string.share_route_snapshot), color = Color.Black)
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Blue),
                    onClick = { routeMenu(RouteMenu.MoveFile) }) {
                    Row {
                        Icon(painterResource(R.drawable.file_move_24px), contentDescription = stringResource(R.string.move_file))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(0.8f), fontSize = 14.sp, text = stringResource(R.string.move_file), color = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                Button(
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Blue),
                    onClick = { routeMenu(RouteMenu.InsertIntoDatabase) }) {
                    Row {
                        Icon(painterResource(R.drawable.database_24), contentDescription = stringResource(R.string.save_to_database))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(0.8f), fontSize = 14.sp, text = stringResource(R.string.save_to_database), color = Color.Black)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteFileSaveMoBoSheet(
    name: String,
    callback: (String, Pair<String, String>?) -> Unit
) {
    var routeName by remember { mutableStateOf(name) }
    val rootFolder = LocalContext.current.filesDir
    val routesRootFolder = File(rootFolder, Const.ROUTEFOLDER)
    if (!LocalInspectionMode.current) {
        routesRootFolder.mkdirs()
    }
    val fileFilter = FileFilter { file: File? -> file?.isDirectory == true }
    val files: Array<File> = routesRootFolder.listFiles(fileFilter) ?: emptyArray()
    files.sortWith(compareBy { it.name })
    val routeFolderList = ArrayList<Pair<String, String>>()
    for (file in files) {
        routeFolderList.add(Pair(file.name, file.path))
    }
    ModalBottomSheet(
        onDismissRequest = { callback("", null) },
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .nestedScroll(rememberNestedScrollInteropConnection())
    ) {
        Column(modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
            Box(modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
                Text(fontSize = 16.sp, text = stringResource(R.string.save_route), color = Color.Black)
            }
            OutlinedTextField(
                value = routeName, readOnly = false,
                onValueChange = { routeName = it },
                label = { Text(stringResource(R.string.route_name)) },
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(routeFolderList) { routeFolder ->
                    RouteFolderItem(routeFolderName = routeFolder.first, onItemClick = {
                        callback(routeName, routeFolder)
                    })
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun RouteFolderItem(routeFolderName: String, onItemClick: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        TextButton(
            onClick = { onItemClick(routeFolderName) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            contentPadding = PaddingValues(10.dp)
        ) {
            Text(
                text = routeFolderName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoboSnack(snackRoutesData: SnackRoutesData, finished: (action: SnackRoutesAction) -> Unit) {
    ModalBottomSheet(onDismissRequest = { finished(SnackRoutesAction.Nothing) }) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp, start = 3.dp, end = 3.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = snackRoutesData.title,
                    Modifier.weight(0.8f).padding(top = 8.dp, bottom = 8.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Blue
                )
                snackRoutesData.actionText?.let { text ->
                    TextButton(onClick = { finished(snackRoutesData.action) }, modifier = Modifier.weight(0.2f)) {
                        Text(text = text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color.Blue)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (snackRoutesData.routeMenu == RouteMenu.RefreshPreview && snackRoutesData.actionData != null) {
                val imageBitmap = (snackRoutesData.actionData as Bitmap).asImageBitmap()
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = BitmapPainter(imageBitmap, IntOffset(0, 0), IntSize(imageBitmap.width, imageBitmap.height)),
                        contentDescription = snackRoutesData.action.name
                    )
                }
            }
        }
    }
}

@Composable
fun NewRouteFolder(folderName: String, newFolder: (Pair<String, String>) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(folderName) {
        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
        val routeFolder = File(rootRouteFolder, folderName)
        routeFolder.mkdirs()
        newFolder(Pair(routeFolder.name, routeFolder.path))
    }
}

fun takeSnapshot(
    context: Context, lllh: List<LatLngH>,
    region: String,
    name: String,
    routeFolder: File?,
    finished: () -> Unit
) {
    Helpers.takeRouteSnapshot(
        context,
        lllh,
        name,
        Const.styleVectorUri,
        512,
        0.1,
        true,
        routeFolder
    ) { snapshot, _ ->
        val track = Track(lllh)
        val kmlString = lllh.lllhToKmlString(name)
        snapshot?.let {
            Timber.i("name: $name")
            replaceRouteDao(context, name, region, kmlString, it.bitmap, track) {
                finished()
            }
        }
    }
}

internal fun replaceRouteDao(context: Context, name: String, region: String, kmlString: String,
                             bitmap: Bitmap, track: Track, finished: () -> Unit) {
    Timber.i( "$name $region")
    val routeEntity = track.distance?.let {
        RouteEntity(
            name,
            region,
            track.startLatLng!!.latitude,
            track.startLatLng!!.longitude,
            track.center.latitude,
            track.center.longitude,
            track.stopLatLng!!.latitude,
            track.stopLatLng!!.longitude,
            distance = it,
            kmlString,
            bitmap
        )
    }

    val routeRepository =
        RouteRepository.getInstance(context, Executors.newSingleThreadExecutor())
    if (routeEntity != null) {
        routeRepository.replaceRoute(routeEntity) {
            Timber.i(name)
            finished()
        }
    }
}
