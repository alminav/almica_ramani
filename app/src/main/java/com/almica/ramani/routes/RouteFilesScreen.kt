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
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
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
import com.almica.ramani.routes.SnackRoutesAction.Nothing
import com.almica.ramani.routes.SnackRoutesAction.RemoveRouteFolder
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
import kotlinx.coroutines.delay
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
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.GeoJsonUtils.Companion.pointToTile
import com.almica.ramani.utils.getCenter
import com.google.android.gms.maps.model.LatLng
import kotlin.time.Duration.Companion.milliseconds

enum class RouteEntityItemAction{
    Select,
    Map,
    Hide,
    Delete,
    Database
}

private const val logtag = "RouteFilesScreen"
@SuppressLint("UnrememberedMutableState", "UnusedMaterial3ScaffoldPaddingParameter",
    "LocalContextGetResourceValueCall", "MutableCollectionMutableState"
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteFilesScreen(selectRoute: (RouteEntity?, RouteMenu) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    //var routeEntity by remember { mutableStateOf<RouteEntity?>(null) }
    var newRouteFolderMode by remember { mutableStateOf(false) }
    var newRouteFolder by remember { mutableStateOf("") }
    var createNewRouteFolder by remember { mutableStateOf(false) }
    var showRouteMoBo by remember { mutableStateOf<RouteEntity?>(null) }
    var showRouteChart by remember { mutableStateOf<File?>(null) }
    var showRouteGradient by remember { mutableStateOf<RouteEntity?>(null) }
    var showSrtmFiles by remember { mutableStateOf(false) }
    var srtmFile by remember { mutableStateOf<File?>(null) }
    var moveFile by remember { mutableStateOf(false) }
    var askForNameFilter by remember { mutableStateOf(false) }
    var askForRouteName by remember { mutableStateOf<RouteEntity?>(null) }
    var routeEntities by remember { mutableStateOf< MutableList<RouteEntity>>(mutableListOf())}
    var routeEntitiesSorted by remember { mutableStateOf<MutableList<RouteEntity>> (arrayListOf()) }
    var showRoutesImportExportMenu by remember { mutableStateOf(false) }
    var showSingleRouteImportExportMenu: String? by remember { mutableStateOf(null) }
    //var routeEntitiesSorted : MutableList<RouteEntity> = mutableListOf()
    var notifyDataChanged by remember { mutableStateOf(true) }
    var dataChangeCompleted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    //var snackbarTriple by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    val scope = rememberCoroutineScope()
    var snackRoutesData by remember { mutableStateOf<SnackRoutesData?>(null) }
    LaunchedEffect(key1 = snackRoutesData) {
        Timber.i( "LaunchedEffect(key1 = snackRoutesData)")
        delay(5000.milliseconds)
        if (snackRoutesData?.routeMenu != RouteMenu.RefreshPreview)
            snackRoutesData = null
    }
    BackHandler {
        scope.launch {
            Timber.i("")
            //(context as Activity).finish()
            if (showRouteGradient.isNotNull())
                showRouteGradient = null
            else if (showRouteChart.isNotNull())
                showRouteChart = null
            else
                selectRoute(null, RouteMenu.Home)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            selectRoute(null, RouteMenu.Home)
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
                    Text(text = stringResource(R.string.route_files), fontSize = 14.sp, maxLines = 1)
                }, actions = {
                    IconButton(onClick = {
                        showRoutesImportExportMenu = true
                    }) {
                        Icon(
                            Icons.Outlined.ImportExport,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = { askForNameFilter = true })
                    {
                        Icon(
                            painterResource(R.drawable.outline_filter_alt_24),
                            "filter",
                            modifier = Modifier
                                .padding(end = 10.dp, start = 10.dp)
                                .width(60.dp)
                                .height(60.dp)
                        )
                    }
                    IconButton(onClick = { newRouteFolderMode = true })
                    {
                        Icon(
                            Icons.Outlined.Add,
                            "newFolder",
                            modifier = Modifier
                                .padding(end = 10.dp, start = 10.dp)
                                .width(60.dp)
                                .height(60.dp)
                        )
                    }
                    IconButton(onClick = {
                        dataChangeCompleted = false
                        notifyDataChanged = true
                        Timber.i( "notifyDataChanged: $notifyDataChanged") })
                    {
                        Icon(
                            Icons.Outlined.Refresh,
                            "notifyDataChanged",
                            modifier = Modifier
                                .padding(end = 10.dp, start = 10.dp)
                                .width(60.dp)
                                .height(60.dp)
                        )
                    }
                    BadgedBox(badge = {
                        if (srtmFile.isNotNull()) Badge { Text(text = Const.UC_CHECKMARK) }
                    }) {
                        TextButton(onClick = {
                            showSrtmFiles = true
                            Timber.i("")
                        }) {
                            Text(text = stringResource(R.string.srtm))
                        }
                    }
                })

            if (createNewRouteFolder && newRouteFolder.isNotEmpty()) {
                NewRouteFolder(
                    newRouteFolder,
                    newFolder = { name ->
                        newRouteFolder = name.first
                        createNewRouteFolder = false
                    }
                )
            }
            AnimatedVisibility(visible = newRouteFolderMode) {
                Row(
                    modifier = Modifier
                        .padding(top = 60.dp)
                        .background(Color.White)
                ) {
                    OutlinedTextField(
                        value = newRouteFolder,
                        onValueChange = { newRouteFolder = it },
                        label = { Text(stringResource(R.string.routefolder_name)) },
                        modifier = Modifier
                            .padding(start = 6.dp, end = 6.dp)
                            .fillMaxWidth(0.8f)
                    )
                    IconButton(
                        modifier = Modifier.align(alignment = Alignment.CenterVertically),
                        //.border(border = BorderStroke(2.dp, Color.LightGray)),
                        onClick = {
                            newRouteFolderMode = false
                            createNewRouteFolder = true
                        }
                    ) {
                        Icon(//modifier = Modifier.align(alignment = Alignment.CenterVertically),
                            imageVector = Icons.Outlined.Done,
                            contentDescription = "Localized description"
                        )
                    }
                }
            }
        }) { _ ->
        snackRoutesData?.let {
            MoboSnack(it) {responseAction ->
                when(responseAction) {
                    Nothing -> snackRoutesData = null
                    RemoveRouteFolder -> {
                        val region = it.actionData
                        region?.let { it1 ->
                            deleteRouteFolder(
                                context,
                                it1 as String,
                                routeEntities
                            ) { result, resultSorted ->
                                snackRoutesData = null
                                routeEntities.clear()
                                routeEntities.addAll(result)
                                routeEntitiesSorted.clear()
                                routeEntitiesSorted.addAll(resultSorted)
                                dataChangeCompleted = false
                                notifyDataChanged = true
                                snackRoutesData = null
                            }
                        }
                    }

                    SnackRoutesAction.ShowSrtmFiles -> {
                        snackRoutesData = null
                        showSrtmFiles = true
                    }

                    SnackRoutesAction.RemoveAllRoutes -> {
                        snackRoutesData = null
                        deleteMainRouteFolder(context) {
                            routeEntities.clear()
                            routeEntitiesSorted.clear()
                            dataChangeCompleted = false
                            notifyDataChanged = true
                        }
                    }
                }
            }
        }
        if (showSrtmFiles) {
            DropdownSrtmFiles(context, srtmFile) { file, import ->
                showSrtmFiles = false
                srtmFile = file
                if (import) {
                    context.startActivity(
                        Intent(context, FileImportActivity::class.java)
                            .setAction(context.getString(R.string.import_title))
                            .putExtra(Const.EXTRA_FILETYPE, FileType.Hgt.name)
                    )
                }
            }
        }
        showSingleRouteImportExportMenu?.let {
            DropdownSingleRouteImportExport { action ->
                when(action) {
                    SingleRouteAction.Nothing -> { showSingleRouteImportExportMenu = null }
                    SingleRouteAction.Text -> {
                        context.startActivity(
                            Intent(context, FileImportActivity::class.java)
                                .setAction(context.getString(R.string.import_title))
                                .putExtra(Const.EXTRA_FILETYPE, FileType.Route.name)
                                .putExtra(Const.EXTRA_ROUTEFOLDER, showSingleRouteImportExportMenu)
                        )
                        showSingleRouteImportExportMenu = null
                    }
                    SingleRouteAction.Image -> {
                        context.startActivity(
                            Intent(context, FileImportActivity::class.java)
                                .setAction(context.getString(R.string.import_title))
                                .putExtra(Const.EXTRA_FILETYPE, FileType.RouteThumbnail.name)
                                .putExtra(Const.EXTRA_ROUTEFOLDER, showSingleRouteImportExportMenu)
                        )
                        showSingleRouteImportExportMenu = null
                    }

                    SingleRouteAction.Cleanup -> {
                        showSingleRouteImportExportMenu?.let { region ->
                            val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                            val routeFolder = File(rootRouteFolder, region)
                            cleanUpRouteFolder(context, File(context.filesDir, Const.THUMBNAILS)) {deleteCount, renameCount ->
                                Timber.i("cleanUp ${Const.THUMBNAILS} deleteCount $deleteCount renameCount $renameCount")
                            }
                            cleanUpRouteFolder(context, routeFolder) {deleteCount, renameCount ->
                                snackRoutesData =
                                    SnackRoutesData(
                                        RouteMenu.Placeholder,
                                        context.getString(R.string.changed_files_, deleteCount, renameCount),
                                        action = Nothing, actionText = null, null
                                    )
                            }
                            showSingleRouteImportExportMenu = null
                        }
                    }
                }
            }
        }

        if (showRoutesImportExportMenu) {
            DropdownRoutesImportExport { action ->
                when(action) {
                    RoutesAction.Import -> {
                        showRoutesImportExportMenu = false
                        context.startActivity(
                            Intent(context, FileImportActivity::class.java)
                                .setAction(context.getString(R.string.import_title))
                                .putExtra(Const.EXTRA_FILETYPE, FileType.RoutesZip.name)
                        )
                    }
                    RoutesAction.Export -> {
                        showRoutesImportExportMenu = false
                        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                        @SuppressLint("SimpleDateFormat") val timeFormat =
                            SimpleDateFormat(Const.TIME_PATTERN_LONG_YEAR)
                        val timeTag = java.lang.String.format(
                            Locale.getDefault(), "%s", timeFormat.format(System.currentTimeMillis())
                        )
                        val zipFile = File(context.cacheDir, "${Const.ROUTEFOLDER}_${timeTag}${Const.ZIP_EXT}")
                        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            UnzipUtils.zipFolder(rootRouteFolder, zipFile)
                        }.invokeOnCompletion {
                            val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", zipFile)
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            intent.type = "*/*"
                            intent.putExtra(Intent.EXTRA_STREAM, uri)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        }
                    }
                    RoutesAction.Nothing -> showRoutesImportExportMenu = false
                    RoutesAction.Delete -> {
                        showRoutesImportExportMenu = false
                        snackRoutesData =
                            SnackRoutesData(
                                RouteMenu.Placeholder,
                                context.getString(R.string.delete_all_routes),
                                action = SnackRoutesAction.RemoveAllRoutes,
                                actionText = context.getString(R.string.ok),
                                actionData = null
                            )
                    }

                    RoutesAction.ExportThumbnails -> {
                        showRoutesImportExportMenu = false
                        val thumbnailFolder = File(context.filesDir, Const.THUMBNAILS)
                        @SuppressLint("SimpleDateFormat") val timeFormat =
                            SimpleDateFormat(Const.TIME_PATTERN_LONG_YEAR)
                        val timeTag = java.lang.String.format(
                            Locale.getDefault(), "%s", timeFormat.format(System.currentTimeMillis())
                        )
                        val zipFile = File(context.cacheDir, "${Const.THUMBNAILS}_${timeTag}${Const.ZIP_EXT}")
                        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            UnzipUtils.zipFolder(thumbnailFolder, zipFile)
                        }.invokeOnCompletion {
                            val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", zipFile)
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            intent.type = "*/*"
                            intent.putExtra(Intent.EXTRA_STREAM, uri)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        }
                    }
                    RoutesAction.ImportThumbnails -> {
                        showRoutesImportExportMenu = false
                        context.startActivity(
                            Intent(context, FileImportActivity::class.java)
                                .setAction(context.getString(R.string.import_title))
                                .putExtra(Const.EXTRA_FILETYPE, FileType.ThumbnailsZip.name)
                        )
                    }
                }
            }
        }
        if (isLoading)
            ProgressDialog()
        if (notifyDataChanged) {
            RouteFilesContent(
                context, lifecycleOwner,
                initialize = { routes ->
                    if (!routes.isNullOrEmpty()) {
                        routeEntities = routes.toMutableList()
                        //routeEntitiesSorted = routes.toMutableList()
                        routeEntitiesSorted =
                            routeEntities.sortedBy { entity -> entity.region.plus(entity.name) }
                                .toMutableList()
                        notifyDataChanged = false
                        Timber.i("routeEntities ${routeEntities.size}")
                    } else {
                        Timber.e("routes isNullOrEmpty")
                        routeEntities = mutableListOf()
                        routeEntitiesSorted = mutableListOf()
                        notifyDataChanged = false
                    }
                    dataChangeCompleted = true
                }
            )
        }

        if (dataChangeCompleted) {
            RouteFilesGroupedList(
                routeEntitiesSorted,
                selectRoute = { route, action ->
                    //Timber.i("action:${action.name}")
                    if (route != null) {
                        Timber.i(
                            "action:${action.name} " + "${route.name} ${route.region}"
                        )
                        when (action) {
                            RouteEntityItemAction.Select -> {
                                //routeEntity = route
                                showRouteMoBo = route
                                Timber.i("${route.name}  ${route.kmlString.length}")
                                showRouteChart = null
                            }

                            RouteEntityItemAction.Delete -> {
                                val routeRepository =
                                    RouteRepository.getInstance(
                                        context,
                                        Executors.newSingleThreadExecutor()
                                    )
                                routeRepository.removeRoute(route.id)
                                var result = routeEntitiesSorted.remove(route)
                                Timber.i(
                                    "remove result $result routeEntitiesSorted ${routeEntitiesSorted.size}"
                                )
                                result = routeEntities.remove(route)
                                Timber.i(
                                    "remove result $result routeEntities ${routeEntities.size}"
                                )
                                dataChangeCompleted = false
                                notifyDataChanged = true
                            }

                            RouteEntityItemAction.Map -> {
                                Timber.i(
                                    "$action ${route.name}"
                                )
//                            showRouteEntityInMap(context, mapboxMap, route)
                                val rootFolder =
                                    File(context.filesDir, Const.ROUTEFOLDER)
                                val routeFolder = File(rootFolder, route.region)
                                val routeFile = File(routeFolder, route.name)
                                val lllh =
                                    if (route.name.endsWith(Const.JPG_EXT)) {
                                        Helpers.getCoordinatesFromExif(routeFile)
                                    } else
                                        Helpers.getLllhFromFile(routeFile)
                                val kmlString = lllh?.lllhToKmlString(route.name)
                                val encodedString =
                                    lllh?.let { encodeLllh(it) }
                                Timber.i("encodedString length: ${encodedString?.length}")
                                Timber.i("encodedString: $encodedString")
                                val lllhDecoded = encodedString?.let { decodeLllh(it) }
                                Timber.i("lllhDecoded size: ${lllhDecoded?.size}")

                                if (kmlString != null) {
                                    route.kmlString = kmlString
                                    Timber.i("kmlString length: ${kmlString.length}")
                                } else
                                    Timber.i(
                                        "invalid route ${route.name}"
                                    )

                                selectRoute(route, RouteMenu.Map)
                            }

                            RouteEntityItemAction.Hide -> {
                                route.let {
                                    //removeRouteLine(it.name)
                                    Timber.i(
                                        "removeRouteLine ${it.name}"
                                    )
                                }
                            }

                            RouteEntityItemAction.Database -> {
                                snackRoutesData =
                                    SnackRoutesData(
                                        RouteMenu.Placeholder,
                                        context.getString(
                                            R.string.added_to_database,
                                            route.name
                                        ),
                                        action = Nothing, actionText = null, null
                                    )
                            }
                        }
                    } //else Timber.i("route = null")
                },
                deleteRouteFolder = { region ->
                    snackRoutesData =
                        SnackRoutesData(
                            RouteMenu.Placeholder,
                            context.getString(R.string.remove_route_folder, region),
                            action = RemoveRouteFolder,
                            actionText = context.getString(R.string.ok),
                            actionData = region
                        )
                }, singleRouteMenu = {routeFolder ->
                    showSingleRouteImportExportMenu = routeFolder
                }
            )
        }

        showRouteMoBo?.let {
            RouteFileMoBoSheet(it) { action ->
                Timber.i("${it.name} ${it.region} action $action")
                when (action) {
                    RouteMenu.Home -> showRouteMoBo = null
                    RouteMenu.RefreshPreview -> {
                        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                        val routeFolder = File(rootRouteFolder, it.region)
                        val routeFile = File(routeFolder, it.name)
                        val lllh =
                            if (it.name.endsWith(Const.JPG_EXT)) {
                                Helpers.getCoordinatesFromExif(routeFile)
                            } else
                                Helpers.getLllhFromFile(routeFile)
                        if (lllh != null) {
                            val routeCenter = lllh.getCenter()
                            val mvtTileMatch: GeoJsonUtils.Companion.Tile =
                                pointToTile(routeCenter.longitude, routeCenter.latitude, 9.0)
                            Timber.i("$routeCenter mvtTileMatch: $mvtTileMatch")
                            val mvtMatchingMap = "${Const.MVT_PREFIX}${mvtTileMatch.x}_${mvtTileMatch.y}_${mvtTileMatch.z}"
                            val mvtRootFolder = File(context.filesDir, Const.MVT_FOLDER)
                            val mvtMatchingFile = File(mvtRootFolder, mvtMatchingMap.plus(Const.MBTILES_EXT))
                            val preferences = getDefaultSharedPreferences(context)
                            val mvtCurrentPath = preferences.getString(Const.PREF_MVT_FILEPATH, null)
                            var baseMapChange = false
                            if (mvtMatchingFile.exists() && mvtMatchingFile.path != mvtCurrentPath) {
                                preferences.edit { putString(Const.PREF_MVT_FILEPATH, mvtMatchingFile.path) }
                                Timber.i("pref ${Const.PREF_MVT_FILEPATH} changed: ${mvtMatchingFile.path}")
                                baseMapChange = true
                            }
                            Helpers.takeRouteSnapshot(context, lllh, it.name, Const.styleVectorUri, 512, 0.1, true,
                                routeFolder)
                            { snapShot, _ ->
                                Timber.i("takeLocationsSnapshot ready")
                                snapShot?.let { snapshot ->
                                    addLineToSnapshotWithGradient(snapshot, lllh)
                                    it.bitmap = snapShot.bitmap // it = showRouteMoBo
                                    //dataChangeCompleted = false
                                    //notifyDataChanged = true
                                    showRouteMoBo = null
                                    Timber.i("notifyDataChanged ${it.name}")

                                    // experiment 21mai2026
                                    // Helpers.textRecognition(snapShot.bitmap)

                                    val routeDisplayName = it.name
                                        .removeSuffix(Const.GPX_EXT)
                                        .removeSuffix(Const.KML_EXT)
                                        .removeSuffix(Const.JPG_EXT)

                                    val snackTitle = StringBuilder(context.getString(R.string.refresh_route_preview_ready, routeDisplayName))
                                    if (baseMapChange) {
                                        snackTitle.append("\n")
                                            .append(context.getString(R.string.vector_map_changed_to_, mvtMatchingFile.name))
                                    } else if (mvtMatchingFile.exists()) {
                                        snackTitle.append("\n")
                                            .append(context.getString(R.string.vector_map_used_, mvtMatchingFile.name))
                                    } else {
                                        snackTitle.append("\n")
                                            .append(context.getString(R.string.vector_map_missing_, mvtMatchingFile.name))
                                    }

                                    snackRoutesData =
                                        SnackRoutesData(
                                            RouteMenu.RefreshPreview,
                                            snackTitle.toString(),
                                            action = Nothing,
                                            actionText = null, snapShot.bitmap
                                        )
                                }
                            }
                        } else
                            Timber.e("${it.name} lllh = null")
                    }
                    RouteMenu.Map -> {
                        val rootFolder =
                            File(context.filesDir, Const.ROUTEFOLDER)
                        val routeFolder = File(rootFolder, it.region)
                        val routeFile = File(routeFolder, it.name)
                        val lllh =
                            if (it.name.endsWith(Const.JPG_EXT)) {
                                Helpers.getCoordinatesFromExif(routeFile)
                            } else
                                Helpers.getLllhFromFile(routeFile)
                        val kmlString = lllh?.lllhToKmlString(it.name)
                        if (kmlString != null) {
                            it.kmlString = kmlString
                        } else
                            Timber.i("invalid route ${it.name}")
                        selectRoute(it, RouteMenu.Map)
                        showRouteMoBo = null
                    }
                    RouteMenu.MoveFile -> {
                        if (showRouteMoBo != null && showRouteMoBo?.name == "routes.geojson") {
                            val rootFolder = context.filesDir
                            val routesRootFolder = File(rootFolder, Const.ROUTEFOLDER)
                            val routesFolder =
                                File(routesRootFolder, showRouteMoBo!!.region)
                            val sourceFile = File(routesFolder, showRouteMoBo!!.name)
                            val targetFile = File(routesRootFolder, showRouteMoBo!!.name)
                            sourceFile.copyTo(targetFile, true)
                            if (targetFile.exists())
                                sourceFile.delete()
                            snackRoutesData =
                                SnackRoutesData(
                                    action,
                                    context.getString(
                                        R.string.moved_to,
                                        "routes folder"
                                    ),
                                    action = Nothing, actionText = null, null
                                )
                            moveFile = true
                            showRouteMoBo = null
                        } else {
                            askForRouteName = showRouteMoBo?.copy()
                            moveFile = true
                            showRouteMoBo = null
                        }
                    }
                    RouteMenu.DeleteFile -> {
                        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                        val routeFolder = File(rootRouteFolder, it.region)
                        if (showRouteMoBo?.name == "routes.geojson") {
                            File(rootRouteFolder, showRouteMoBo!!.name).delete()
                            File(routeFolder, showRouteMoBo!!.name).delete()
                            dataChangeCompleted = false
                            notifyDataChanged = true
                            showRouteMoBo = null
                        } else {
                            val result =
                                routeEntitiesSorted.remove(showRouteMoBo)
                            Timber.i(
                                "remove result $result routeEntitiesSorted ${routeEntitiesSorted.size}"
                            )
                            routeEntities.remove(showRouteMoBo)
                            val routeFile = File(routeFolder, it.name)
                            val deleteResult = routeFile.delete()
                            Timber.i(
                                "delete file ${routeFile.path} $deleteResult"
                            )
                            val sharedPref = context.getSharedPreferences(
                                context.getString(R.string.early_annotations),
                                Context.MODE_PRIVATE
                            )
                            sharedPref.edit { remove(routeFile.path) }
                            dataChangeCompleted = false
                            notifyDataChanged = true
                            showRouteMoBo = null
                        }
                    }
                    RouteMenu.ShareFile -> {
                        shareRouteFile(context, it)
                        showRouteMoBo = null
                    }
                    RouteMenu.ShareSnapshot -> {
                        shareRouteSnapshot(context, it)
                        showRouteMoBo = null
                    }
                    RouteMenu.SaveFile -> {
                        askForRouteName = it.copy()
                        moveFile = false
                        showRouteMoBo = null
                    }
                    RouteMenu.InsertIntoDatabase -> {
                        val rootRouteFolder =
                            File(context.filesDir, Const.ROUTEFOLDER)
                        val routeFolder = File(rootRouteFolder, it.region)
                        val routeFile = File(routeFolder, it.name)

                        if (routeFile.exists()) {
                            val lllh =
                                if (it.name.endsWith(Const.JPG_EXT)) {
                                    Helpers.getCoordinatesFromExif(routeFile)
                                } else
                                    Helpers.getLllhFromFile(routeFile)
                            val region = it.region
                            val name = it.name
                            region.let {
                                name.let { name ->
                                    lllh?.let { _ ->
                                        takeSnapshot(context, lllh, region, name, routeFolder) {
                                            Timber.i("takeSnapshot ready $name")
                                            snackRoutesData =
                                                SnackRoutesData(
                                                    action,
                                                    context.getString(R.string.save_to_database_ready, name),
                                                    action = Nothing, actionText = null, null
                                                )
                                        }
                                    }
                                }
                            }
                        }
                        showRouteMoBo = null
                    }
                    RouteMenu.Chart -> {
                        val rootRouteFolder =
                            File(context.filesDir, Const.ROUTEFOLDER)
                        val routeFolder = File(rootRouteFolder, it.region)
                        val routeFile = File(routeFolder, it.name)

                        if (routeFile.exists()) {
                            showRouteChart = routeFile
                        }
                        showRouteMoBo = null
                    }
                    RouteMenu.Gradient -> {
                        val rootRouteFolder =
                            File(context.filesDir, Const.ROUTEFOLDER)
                        val routeFolder = File(rootRouteFolder, it.region)
                        val routeFile = File(routeFolder, it.name)
                        if (routeFile.exists()) {
                            val lllh =
                                if (it.name.endsWith(Const.JPG_EXT)) {
                                    Helpers.getCoordinatesFromExif(routeFile)
                                } else
                                    Helpers.getLllhFromFile(routeFile)
                            if (lllh.isNullOrEmpty().not()) {
                                Timber.i(it.name)
                                val kmlString = lllh.lllhToKmlString(it.name)
                                showRouteGradient = it.copy()
                                showRouteGradient!!.kmlString = kmlString
                            }
                        }
                        showRouteMoBo = null
                    }
                    RouteMenu.ElevationRefreshFromSrtm -> {
                        val rootRouteFolder =
                            File(context.filesDir, Const.ROUTEFOLDER)
                        var routeFile: File?
                        val routeFolder = File(rootRouteFolder, it.region)
                        routeFile = File(routeFolder, it.name)
                        if (routeFile.exists()) {
                            val lllh =
                                if (it.name.endsWith(Const.JPG_EXT)) {
                                    Helpers.getCoordinatesFromExif(routeFile)
                                } else
                                    Helpers.getLllhFromFile(routeFile)
                            val hgtFile = srtmFile
                            if (hgtFile != null) {
                                Timber.i("hgtFile: ${hgtFile.name}")
                                if (hgtFile.exists()) {
                                    val hgtReader = HgtReader(context, hgtFile)
                                    val refreshedLllh =
                                        hgtReader.refreshRouteElevationFromSrtm(lllh).lllh
                                    val path =
                                        routeFile.path.replace(Const.GPX_EXT, Const.KML_EXT)
                                            .replace(Const.JPG_EXT, Const.KML_EXT)
                                    val result =
                                        Helpers.writeLllh2KmlFile(refreshedLllh, path)
                                    if (result) {
                                        snackRoutesData =
                                            SnackRoutesData(
                                                action,
                                                context.getString(R.string.saved_to_, path),
                                                action = Nothing, actionText = null, null
                                            )
                                    }
                                    Timber.i("result:$result")
                                } else {
                                    snackRoutesData =
                                        SnackRoutesData(
                                            action,
                                            context.getString(R.string.not_found_, hgtFile.path),
                                            action = Nothing, actionText = null, null
                                        )
                                    Timber.i(
                                        "not found: ${hgtFile.path}"
                                    )
                                }
                            } else {
                                snackRoutesData =
                                    SnackRoutesData(
                                        action,
                                        context.getString(R.string.select_hgt_file),
                                        action = SnackRoutesAction.ShowSrtmFiles,
                                        actionText = context.getString(R.string.ok),
                                        null
                                    )
                            }
                        } else
                            Timber.i("not found: ${routeFile.path}")
                        showRouteMoBo = null
                    }
                    RouteMenu.ElevationGmsService -> {
                        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                        var routeFile: File?
                        val routeFolder = File(rootRouteFolder, it.region)
                        routeFile = File(routeFolder, it.name)
                        if (routeFile.exists()) {
                            val lllh =
                                if (it.name.endsWith(Const.JPG_EXT)) {
                                    Helpers.getCoordinatesFromExif(routeFile)
                                } else
                                    Helpers.getLllhFromFile(routeFile)
                            if (lllh != null && lllh.size < 512) {
                                val gmsLatLng: List<LatLng> = List(lllh.size) { i ->
                                    LatLng(lllh[i].latitude, lllh[i].longitude)
                                }
                                val encodedPolyline = PolyUtil.encode(gmsLatLng)
                                scope.launch {
                                    val refreshedLllh = MapUtils.gmsElevationService(
                                        context,
                                        "enc:${encodedPolyline}"
                                    )
                                    if (refreshedLllh.isNotEmpty()) {
                                        val path =
                                            routeFile.path.replace(
                                                Const.GPX_EXT,
                                                Const.KML_EXT
                                            )
                                                .replace(Const.JPG_EXT, Const.KML_EXT)
                                        val result =
                                            Helpers.writeLllh2KmlFile(refreshedLllh, path)
                                        if (result)
                                            snackRoutesData =
                                                SnackRoutesData(
                                                    action,
                                                    context.getString(R.string.saved_to_, path),
                                                    action = Nothing, actionText = null, null
                                                )
                                    } else {
                                        snackRoutesData =
                                            SnackRoutesData(
                                                action,
                                                "${context.getString(R.string.gms_elevation_service)} FAILED",
                                                action = Nothing, actionText = null, null
                                            )
                                        Timber.i(
                                            "" +
                                                    "${context.getString(R.string.gms_elevation_service)} FAILED"
                                        )
                                    }
                                }
                            }
else {
                                Timber.i("route coordinates: ${lllh?.size}")
                                snackRoutesData =
                                    SnackRoutesData(
                                        action,
                                        context.getString(R.string.too_many_coordinates_512),
                                        action = Nothing, actionText = null, null
                                    )
                            }
                        }
                        showRouteMoBo = null
                    }

                    RouteMenu.Placeholder -> {}
                }
            }
        }
        showRouteChart?.let {
            Timber.i("LineGraphLllh ${it.name}")
            val lllh =
                if (it.name.endsWith(Const.JPG_EXT)) {
                    Helpers.getCoordinatesFromExif(it)
                } else
                    Helpers.getLllhFromFile(it)
            val lllhReduced = lllh?.reduceWithTolerance(200.0)
            ModalBottomSheet(onDismissRequest = { showRouteChart = null }) {
                LineYGraphLllh(lllhReduced, it.name, 0F, { _ ->
                    Timber.i("")
                    showRouteChart = null
                }, {}, Icons.AutoMirrored.Filled.ArrowBack)
            }
        }
        showRouteGradient?.let {
            val lllh = it.kmlString.kmlString2Lllh()
            val distRoute = lllh.getDistanceFromLllh()
            Timber.i("GradientChartMonitor ${it.name}")
            ModalBottomSheet(onDismissRequest = { showRouteGradient = null }) {
                GradientChartMonitor(
                    it,
                    0.0f, Icons.AutoMirrored.Filled.ArrowBack,
                    result = {
                        showRouteGradient = null
                    }, true
                )
                Text(
                    text = distRoute.formatDistM(true),
                    Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(96.dp))
            }
        }
        askForRouteName?.let { routeEntity ->
            val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
            val routeFolder = File(rootRouteFolder, routeEntity.region)
            var routeFile = File(routeFolder, routeEntity.name)
            if (routeFile.name.endsWith(Const.JPG_EXT)) {
                val lllh = Helpers.getCoordinatesFromExif(routeFile)
                val newFileName = routeFile.name.replace(Const.JPG_EXT, Const.KML_EXT)
                val newRouteFile = File(routeFolder, newFileName)
                Helpers.writeLllh2KmlFile(lllh, newRouteFile.path)
                routeEntities.add(
                    RouteEntity(
                        UUID.randomUUID(),
                        newRouteFile.name,
                        routeEntity.region
                    )
                )
                moveFile = false
                askForRouteName = null
                routeEntitiesSorted =
                    routeEntities.sortedBy { entity ->
                        entity.region.plus(entity.name)
                    }.toMutableList()
                dataChangeCompleted = false
                notifyDataChanged = true
                snackRoutesData =
                    SnackRoutesData(
                        RouteMenu.Placeholder, context.getString(
                            R.string.route_file_created_,
                            newRouteFile.path
                        ), action = Nothing, actionText = null, null
                    )
            } else {
                RouteFileSaveMoBoSheet(routeEntity.name) { targetFileName, targetRouteFolder ->
                    if (targetRouteFolder != null) { // && routeEntity.region != targetRouteFolder.first) {
                        if (routeEntity.region == Const.ROUTEFOLDER)
                            routeFile = File(rootRouteFolder, routeEntity.name)
                        var newFileName = targetFileName
                        if (routeFile.exists()) {
                            if (!targetFileName.contains("."))
                                newFileName = targetFileName.plus((Const.KML_EXT))
                            val newRouteFile = File(targetRouteFolder.second, newFileName)
                            routeFile.copyTo(newRouteFile, overwrite = true)
                            Timber.i(
                                "newRouteFile ${newRouteFile.path}"
                            )
                            targetRouteFolder.let { it1 ->
                                routeEntities.add(
                                    RouteEntity(
                                        UUID.randomUUID(),
                                        targetFileName,
                                        it1.first
                                    )
                                )
                            }
                            if (moveFile) {
                                routeEntities.remove(askForRouteName)
                                routeFile.delete()
                            }
                            snackRoutesData =
                                SnackRoutesData(
                                    RouteMenu.Placeholder, context.getString(
                                        R.string.route_file_created_,
                                        newRouteFile.path
                                    ), action = Nothing, actionText = null, null
                                )
                        } else Timber.i(
                            "$logtag ${Thread.currentThread().stackTrace[2].lineNumber}: ${routeFile.path} not found"
                        )
                    }
                    moveFile = false
                    askForRouteName = null
                    routeEntitiesSorted =
                        routeEntities.sortedBy { entity ->
                            entity.region.plus(entity.name)
                        }.toMutableList()
                    dataChangeCompleted = false
                    notifyDataChanged = true
                }
            }
        }

        if (askForNameFilter) {
            AskForRouteNameFilter(routeEntities, filter = { filter, region ->
                Timber.i("filter $filter")
                askForNameFilter = false
                routeEntitiesSorted = when (region) {
                    null -> if (filter == null) routeEntities.sortedBy { entity ->
                        entity.region.plus(entity.name) }.toMutableList()
                    else filterByName(filter, routeEntities).toMutableList()
                    else -> filterByRegion(region, routeEntities).toMutableList()
                }
                //notifyDataChanged = true
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


fun filterByRegion(region: String?, routeEntities: List<RouteEntity>): List<RouteEntity> {
    val filteredRouteEntities: ArrayList<RouteEntity> = ArrayList()
    routeEntities.forEach { routeEntity ->
        if (routeEntity.region == region)
            filteredRouteEntities.add(routeEntity)
    }
    //Timber.i("filteredRouteEntities ${filteredRouteEntities.size}")
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
    //Timber.i("filteredRouteEntities ${filteredRouteEntities.size}")
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
    Timber.i("routeEntities ${routeEntities.size}")
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
                                    Timber.i("route folder: $initial")
                                    singleRouteMenu(initial)
                                }) {
                                    Icon(
                                        Icons.Outlined.ImportExport,
                                        contentDescription = null
                                    )
                                }
                                IconButton(onClick = {
                                    Timber.i("route folder: $initial")
                                    val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                                    @SuppressLint("SimpleDateFormat") val timeFormat =
                                        SimpleDateFormat(Const.TIME_PATTERN_LONG_YEAR)
                                    val timeTag = java.lang.String.format(
                                        Locale.getDefault(), "%s", timeFormat.format(System.currentTimeMillis())
                                    )
                                    val zipFile = File(context.cacheDir, "${Const.ROUTEFOLDER}_${initial}_${timeTag}${Const.ZIP_EXT}")
                                    Timber.i("zipFile: ${zipFile.path}")
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
                                    Timber.i(initial)
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
                        RouteFilesItem(context, routeItem, onItemClick = { routeItem, action ->
                            Timber.i("routeItem ${routeItem.name} action ${action.name}")
                            selectRoute(routeItem, action)
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
            } else
                Timber.i("NOT found ${routeFile.path}")
        }
    )
}

@Composable
fun ProgressDialog() {
    AlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        confirmButton = {},
        title = { Text(stringResource(R.string.loading)) },
/*
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
 */
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
                // Lays out and draws an image sized to the rectangular subsection of the ImageBitmap
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

@Composable
private fun RouteFilesContent(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    initialize: (List<RouteEntity>?) -> Unit
) {
    var routeEntities : List<RouteEntity>
    LaunchedEffect(Unit) {
        Timber.i("RouteFilesContent LaunchedEffect")
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            routeEntities = getAllRoutesSimple(context)
            initialize(routeEntities.sortedBy {entity ->
                entity.region.plus(entity.name)
            })
        }.invokeOnCompletion {
            Timber.i("invokeOnCompletion")
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
private fun DropdownSingleRouteImportExport(action: (SingleRouteAction) -> Unit) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = { action(SingleRouteAction.Nothing) }
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    Icons.Outlined.FileDownload,
                    null
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.import_file)
                )
            },
            onClick = {
                action(SingleRouteAction.Text)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    Icons.Outlined.Image,
                    null
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.import_photo)
                )
            },
            onClick = {
                action(SingleRouteAction.Image)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    Icons.Outlined.ClearAll,
                    null
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.cleanup)
                )
            },
            onClick = {
                action(SingleRouteAction.Cleanup)
            }
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
private fun DropdownRoutesImportExport(action: (RoutesAction) -> Unit) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = { action(RoutesAction.Nothing) }
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    Icons.Outlined.ImportExport,
                    null
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.import_title)
                )
            },
            onClick = {
                action(RoutesAction.Import)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    Icons.Outlined.Share,
                    null
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.share)
                )
            },
            onClick = {
                action(RoutesAction.Export)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    Icons.Outlined.Delete,
                    null
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_all_routes)
                )
            },
            onClick = {
                action(RoutesAction.Delete)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    Icons.Outlined.ImportExport,
                    null
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.import_thumbnails)
                )
            },
            onClick = {
                action(RoutesAction.ImportThumbnails)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    Icons.Outlined.Share,
                    null
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.export_thumbnails)
                )
            },
            onClick = {
                action(RoutesAction.ExportThumbnails)
            }
        )

    }
}

@Composable
fun DropdownSrtmFiles(context: Context, srtmFile: File?, selected: (File?, Boolean) -> Unit) {
    val hgtFolder = File(context.filesDir, Const.HGT_FOLDER_NAME)
    val hgtFiles = hgtFolder.listFiles()
    hgtFiles?.sort()
    Surface(Modifier
        .fillMaxWidth()
        .padding(top = 100.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.align(Alignment.TopEnd)) {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { selected(null, false) }
                ) {
                    DropdownMenuItem(
                        trailingIcon = {
                            Icon(
                                Icons.Outlined.ImportExport,
                                null
                            )
                        },
                        text = { Text(text = stringResource(R.string.import_title), textDecoration = TextDecoration.Underline) },
                        onClick = {
                            selected(null, true)
                        }
                    )
                    hgtFiles?.forEach { file ->
                        Timber.i(
                            "${file.name}"
                        )
                        DropdownMenuItem(
                            trailingIcon = {
                                if (srtmFile?.name == file.name) Icon(
                                    Icons.Outlined.Check,
                                    null
                                )
                            },
                            text = { Text(text = file.name) },
                            onClick = {
                                selected(file, false)
                            }
                        )
                    }
                }
            }
        }
    }
}

fun deleteMainRouteFolder(context: Context, finished: () -> Unit) {
    val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
    var result = rootRouteFolder.deleteRecursively()
    Timber.i("${rootRouteFolder.name} deleteRecursively $result")
    val thumbnailFolder = File(context.filesDir, Const.THUMBNAILS)
    result = thumbnailFolder.deleteRecursively()
    Timber.i("${thumbnailFolder.name} deleteRecursively $result")

    finished()
}

fun deleteFilesOlderThan(folder: File, days: Int) {
    //val folder = File(context.filesDir, Const.THUMBNAILS)
    val twoDaysAgo = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
    val deletedCount = Helpers.deleteFilesOlderThan(folder, twoDaysAgo, recursive = false)
    Timber.i("Deleted $deletedCount old ${folder.name} files")
}
/**
 * Cleans up a specific region folder by removing redundant route files.
 *
 * This function iterates through all JPEG images in the specified [region] folder.
 * For each image found, it looks for corresponding .kml and .gpx files with the
 * same filename and deletes them to save storage space.
 *
 * @param context The Android context used to access internal files.
 * @param region The sub-folder name within the routes directory to clean.
 * @param finished A callback invoked with the total number of files deleted.
 */
fun cleanUpRouteFolder(context: Context, routeFolder: File?, finished: (Int, Int) -> Unit) {
    //deleteFilesOlderThan(File(context.filesDir, Const.THUMBNAILS), 3)
    var renameCount = 0
    // Define problematic double extensions and their corrected replacements
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
        //if (File(routeFolder, "$baseName${Const.JPG_EXT}").delete()) count++
    }

    finished(deleteCount, renameCount)
}


fun deleteRouteFolder(context: Context, region: String, routeEntities: List<RouteEntity>,
                      finished: (List<RouteEntity>, List<RouteEntity>) -> Unit) {
    val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
    val routeFolder = File(rootRouteFolder, region)
    val result = routeFolder.deleteRecursively()
    Timber.i("${routeFolder.name} deleteRecursively $result")
    val newRouteEntities = ArrayList<RouteEntity>()
    routeEntities.forEach { routeEntity ->
        if (routeEntity.region != region)
            newRouteEntities.add((routeEntity))
    }

    val routeEntitiesSorted = newRouteEntities.sortedBy {entity ->  entity.region.plus(entity.name) }.toMutableList()
    finished(newRouteEntities, routeEntitiesSorted)
}

fun getAllRoutesSimpleWithFilter(context: Context, filter: String): List<RouteEntity> {
    //val filterNoSpaces = filter.replace(" ", "")
    val routeEntities = ArrayList<RouteEntity>()
    val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
    if (!rootRouteFolder.exists()) {
        Timber.i("return emptyList")
        return emptyList()
    }
    Timber.i("filter: $filter")
    rootRouteFolder.walkTopDown().forEach { routeFile ->
        Timber.i("${routeFile.parentFile?.name} - ${routeFile.nameWithoutExtension}")
        if (routeFile.isFile && routeFile.nameWithoutExtension.contains(filter, true)) {
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

private fun shareRouteSnapshot(context: Context, routeEntity: RouteEntity) {
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
            // AI fix for: Immutable bitmap passed to Canvas constructor
            val thumbnail = BitmapFactory.decodeFile(routeSnapshotFile.path, options) ?: return
            val bmp: Bitmap = createBitmap(thumbnail.width, thumbnail.height + 30)
            bmp.let {
                val thumbCanvas = Canvas(it)
                thumbCanvas.drawColor(android.graphics.Color.WHITE)
                thumbCanvas.drawBitmap(thumbnail, 0f, 0f, null)
                val name = routeSnapshotFile.name.replace(Const.JPG_EXT, "")
                drawRouteName(context, thumbCanvas, name)
                val file = File(context.cacheDir, routeSnapshotFile.name)
                val b = file.createNewFile()
                Timber.i("${file.path} create $b")

                val out = FileOutputStream(file)
                it.compress( //isBoundary ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG,
                    Bitmap.CompressFormat.JPEG, 90, out
                )
                out.flush()
                out.close()
                val exifInterface = ExifInterface(file.path)
                exifInterface.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, distString)
                exifInterface.setAttribute(
                    ExifInterface.TAG_ORIENTATION,  // 28jan2022
                    ExifInterface.ORIENTATION_NORMAL.toString()
                )

                kmlString?.let {
                    if (it.length < Const.EXIF_MAX_SIZE) {
                        exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, it)
                    } else {
                        Timber.w("kmlString too large for EXIF: ${it.length}")
                    }
                }
                latLngArray?.let { exifInterface.setLatLong(it[0], it[1]) }
                exifInterface.saveAttributes()
                Timber.i("${file.name} write exif OK")

                val uri = FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".provider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.type = "*/*"
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        } else
            Timber.i(context.getString(R.string.file_not_found, routeSnapshotFile.path))
    } catch (e: Exception) {
        Timber.i("${e.message}")
    }
}

fun drawRouteName(context: Context, thumbCanvas: Canvas, name: String, textSize: Float = 32f) {
    val bgTextPaint = Paint()
    bgTextPaint.color = ContextCompat.getColor(context, R.color.white_transparent_)
    bgTextPaint.isAntiAlias = true
    bgTextPaint.strokeWidth = context.resources.getDimension(R.dimen.thumbLineWidth)
    //bgTextPaint.setStrokeWidth(2);
    bgTextPaint.style = Paint.Style.FILL
    bgTextPaint.strokeJoin = Paint.Join.ROUND
    bgTextPaint.strokeCap = Paint.Cap.ROUND
    val textPaint = TextPaint()
    textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    textPaint.color = ContextCompat.getColor(context, R.color.design_default_color_primary)
    textPaint.isAntiAlias = true
    textPaint.strokeWidth = 2f
    textPaint.textSize = textSize
    val textBounds = Rect()
    textPaint.getTextBounds(name, 0, name.length, textBounds)
    thumbCanvas.drawRect(textBounds, bgTextPaint)
    thumbCanvas.drawText(name, 5F, ((thumbCanvas.height - 0.25*textBounds.height()).toFloat()), textPaint)
}

fun drawLastPageIndicator(context: Context, thumbCanvas: Canvas, name: String, textSize: Float = 64f) {
    val bgTextPaint = Paint()
    bgTextPaint.color = ContextCompat.getColor(context, R.color.white_transparent_)
    bgTextPaint.isAntiAlias = true
    bgTextPaint.strokeWidth = context.resources.getDimension(R.dimen.thumbLineWidth)*2
    //bgTextPaint.setStrokeWidth(2);
    bgTextPaint.style = Paint.Style.FILL
    bgTextPaint.strokeJoin = Paint.Join.ROUND
    bgTextPaint.strokeCap = Paint.Cap.ROUND
    val textPaint = TextPaint()
    textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    textPaint.color = ContextCompat.getColor(context, R.color.design_default_color_primary)
    textPaint.isAntiAlias = true
    textPaint.strokeWidth = 2f
    textPaint.textSize = textSize
    val textBounds = Rect()
    textPaint.getTextBounds(name, 0, name.length, textBounds)
    thumbCanvas.drawRect(textBounds, bgTextPaint)
    thumbCanvas.drawText(name, 0.5f*(thumbCanvas.width - textBounds.width()), ((0.5*thumbCanvas.height - 0.25*textBounds.height()).toFloat()), textPaint)
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
            val intent = Intent(Intent.ACTION_SEND)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } else
            Timber.i(context.getString(R.string.file_not_found, routeFile.path))
        if (routeSnapshotFile != null && routeSnapshotFile.exists()) {
            val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", routeSnapshotFile)
            val intent = Intent(Intent.ACTION_SEND)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        val msg = e.message
        if (msg != null) {
            Timber.e(msg)
        }
    }
}

@Composable
fun RouteFilesRegionList(
    paddingValues: PaddingValues,
    routeEntities: List<RouteEntity>?,
    selectRegion: (String?, Boolean) -> Unit
) {
    Timber.i("routeEntities ${routeEntities?.size}")
    val regions = createRegionArray(routeEntities)
    LazyColumn(
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(regions) { region ->
            Box(
                modifier = Modifier
                    .background(color = Color.White)
                    //.clickable { selectRegion(region) }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                Timber.i("onTap $region")
                                selectRegion(region, false)
                            }, onLongPress = {
                                Timber.i("onLongPress $region")
                                selectRegion(region, true)
                            }
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
            Modifier
                //.padding(paddings)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { filter(null, null) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back home"
                    )
                }
                Text(text = stringResource(R.string.regions_), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(0.8f))
            }
            Spacer(modifier = Modifier.height(4.dp))
            RouteFilesRegionList(PaddingValues(0.dp), routeEntities) { region, remove ->
                Timber.i("$region $remove")
                filter(null, region)
            }
            HorizontalDivider()
            //TextInsideBoxScreen(stringResource(R.string.name_filter))
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


enum class RouteMenu {
    Home,
    Map,
    Chart,
    ElevationRefreshFromSrtm,
    ElevationGmsService,
    Gradient,
    RefreshPreview,
    SaveFile,
    DeleteFile,
    ShareFile,
    ShareSnapshot,
    MoveFile,
    InsertIntoDatabase,
    Placeholder
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
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteMenu.Map) }) {
                    Row {
                        Icon(
                            painterResource(R.drawable.outline_map_24),
                            contentDescription = stringResource(R.string.map)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            modifier = Modifier.weight(0.8f),
                            fontSize = 14.sp,
                            text = stringResource(R.string.map),
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                Button(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteMenu.RefreshPreview) }) {
                    Row {
                        Icon(
                            Icons.Outlined.Preview,
                            contentDescription = stringResource(R.string.refresh_route_preview)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            modifier = Modifier.weight(0.8f),
                            fontSize = 14.sp,
                            text = stringResource(R.string.refresh_route_preview),
                            color = Color.Black
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteMenu.Chart) }) {
                    Row {
                        Icon(
                            painterResource(R.drawable.monitoring_24px),
                            contentDescription = stringResource(R.string.elevation_chart)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(0.8f),
                            fontSize = 14.sp,
                            text = stringResource(R.string.elevation_chart),
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                Button(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteMenu.Gradient) }) {
                    Row {
                        Icon(
                            painterResource(R.drawable.gradient_24px),
                            contentDescription = stringResource(R.string.gradient)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(0.8f),
                            fontSize = 14.sp,
                            text = stringResource(R.string.gradient),
                            color = Color.Black
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteMenu.ElevationGmsService) }) {
                    Row {
                        Icon(
                            Icons.Outlined.Height,
                            contentDescription = stringResource(R.string.gms_elevation_service)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(0.8f),
                            fontSize = 14.sp,
                            text = stringResource(R.string.gms_elevation_service),
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                Button(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteMenu.ElevationRefreshFromSrtm) }) {
                    Row {
                        Icon(
                            Icons.Outlined.Height,
                            contentDescription = stringResource(R.string.elevation_refresh)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(0.8f),
                            fontSize = 14.sp,
                            text = stringResource(R.string.elevation_refresh),
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteMenu.SaveFile) }) {
                    Row {
                        Icon(
                            painterResource(R.drawable.file_save_24px),
                            contentDescription = stringResource(R.string.save_route)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            modifier = Modifier.weight(0.8f),
                            fontSize = 14.sp,
                            text = stringResource(R.string.save_route),
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                Button(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteMenu.DeleteFile) }) {
                    Row {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete_file)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            modifier = Modifier.weight(0.8f),
                            fontSize = 14.sp,
                            text = stringResource(R.string.delete_file),
                            color = Color.Black
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteMenu.ShareFile) }) {
                    Row {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.share_file)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            modifier = Modifier.weight(0.8f),
                            fontSize = 14.sp,
                            text = stringResource(R.string.share_file),
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                Button(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteMenu.ShareSnapshot) }) {
                    Row {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.share_route_snapshot)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            modifier = Modifier.weight(0.8f),
                            fontSize = 14.sp,
                            text = stringResource(R.string.share_route_snapshot),
                            color = Color.Black
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteMenu.MoveFile) }) {
                    Row {
                        Icon(
                            painterResource(R.drawable.file_move_24px),
                            contentDescription = stringResource(R.string.move_file)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            modifier = Modifier.weight(0.8f),
                            fontSize = 14.sp,
                            text = stringResource(R.string.move_file),
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                Button(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),                    onClick = { routeMenu(RouteMenu.InsertIntoDatabase) }) {
                    Row {
                        Icon(
                            painterResource(R.drawable.database_24),
                            contentDescription = stringResource(R.string.save_to_database)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            modifier = Modifier.weight(0.8f),
                            fontSize = 14.sp,
                            text = stringResource(R.string.save_to_database),
                            color = Color.Black
                        )
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
    Timber.i("routeName:$routeName")
    Timber.i("files:${files.size}")
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
                Text(
                    fontSize = 16.sp,
                    text = stringResource(R.string.save_route),
                    color = Color.Black
                )
            }
            OutlinedTextField(
                value = routeName, readOnly = false,
                onValueChange = {
                    routeName = it
                    Timber.i(routeName)
                },
                label = { Text(stringResource(R.string.route_name)) },
                modifier = Modifier
                    .fillMaxWidth()
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
//        routeRepository.removeRoute(name, region)
//        routeRepository.addRoute(routeEntity)
    }
}

internal fun encodeLllh(lllh: ArrayList<LatLngH>): String {
    //PolyUtil.decode("xxx");
    val gmsLllh = List(lllh.size) { i ->
        lllh[i].latLngGms
    }
    val encodedPolyLine = PolyUtil.encode(gmsLllh)
    return encodedPolyLine
}

internal fun decodeLllh(encodedPolyline: String) : List<LatLngH> {
    //PolyUtil.decode("xxx");
    val decodedPolyline = PolyUtil.decode(encodedPolyline)
    val lllh = List(decodedPolyline.size) { i ->
        LatLngH(decodedPolyline[i].latitude, decodedPolyline[i].longitude)
    }
    return lllh
}
enum class SnackRoutesAction {
    Nothing,
    RemoveRouteFolder,
    RemoveAllRoutes,
    ShowSrtmFiles
}
data class SnackRoutesData(
    val routeMenu: RouteMenu, val title: String,
    val action: SnackRoutesAction, val actionText: String?, val actionData: Any?
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoboSnack(snackRoutesData: SnackRoutesData, finished: (action: SnackRoutesAction) -> Unit) {
    ModalBottomSheet(onDismissRequest = { finished(Nothing) }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 3.dp, start = 3.dp, end = 3.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = snackRoutesData.title,
                    Modifier
                        .weight(0.8f)
                        .padding(top = 8.dp, bottom = 8.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Blue
                )
                snackRoutesData.actionText?.let { text ->
                    TextButton(onClick = {
                        Timber.i(snackRoutesData.action.name)
                        finished(snackRoutesData.action)
                    }, modifier = Modifier.weight(0.2f)) {
                        Text(
                            text = text,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Blue
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (snackRoutesData.routeMenu == RouteMenu.RefreshPreview && snackRoutesData.actionData != null) {
                val imageBitmap = (snackRoutesData.actionData as Bitmap).asImageBitmap()
                // Lays out and draws an image sized to the rectangular subsection of the ImageBitmap
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = BitmapPainter(
                            imageBitmap,
                            IntOffset(0, 0),
                            IntSize(imageBitmap.width, imageBitmap.height)
                        ),
                        contentDescription = snackRoutesData.action.name
                    )
                }
            }
        }
    }
}

@ComposePreview(showBackground = true)
@Composable
fun RouteFileMoBoSheetPreview() {
    RamaniTheme {
        RouteFileMoBoSheet(
            routeEntity = RouteEntity(
                name = "Sample Route.kml",
                region = "Sample Region"
            ),
            routeMenu = {}
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
fun RouteFileSaveMoBoSheetPreview() {
    RamaniTheme {
        RouteFileSaveMoBoSheet(
            name = "Sample Route.kml",
            callback = { _, _ -> }
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
fun RouteFilesItemPreview() {
    RamaniTheme {
        RouteFilesItemUI(
            routeItem = RouteEntity(name = "Sample Route.kml", region = "Mountains"),
            routeDbState = 0,
            onItemClick = {},
            onMapClick = {},
            onDatabaseClick = {}
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
fun RouteFilesContentPreview() {
    RamaniTheme {
        RouteFilesContent(
            context = LocalContext.current,
            lifecycleOwner = LocalLifecycleOwner.current,
            initialize = {}
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
fun RouteFilesScreenPreview() {
    RamaniTheme {
        RouteFilesScreen(selectRoute = { _, _ -> })
    }
}

@ComposePreview(showBackground = true)
@Composable
fun RouteFilesMoboSnackPreview() {
    RamaniTheme {
        MoboSnack(
            snackRoutesData = SnackRoutesData(
                routeMenu = RouteMenu.Placeholder,
                title = "Sample Snack Message",
                action = Nothing,
                actionText = "OK",
                actionData = null
            ),
            finished = {}
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
fun RouteFilesAskForRouteNameFilterPreview() {
    val sampleRouteEntities = listOf(
        RouteEntity(name = "Hiking Trail.kml", region = "Mountains"),
        RouteEntity(name = "City Walk.gpx", region = "City")
    )
    RamaniTheme {
        AskForRouteNameFilter(
            routeEntities = sampleRouteEntities,
            filter = { _, _ -> }
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
fun RouteFilesRegionListPreview() {
    val sampleRouteEntities = listOf(
        RouteEntity(name = "Hiking Trail.kml", region = "Mountains"),
        RouteEntity(name = "City Walk.gpx", region = "City")
    )
    RamaniTheme {
        RouteFilesRegionList(
            paddingValues = PaddingValues(0.dp),
            routeEntities = sampleRouteEntities,
            selectRegion = { _, _ -> }
        )
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


