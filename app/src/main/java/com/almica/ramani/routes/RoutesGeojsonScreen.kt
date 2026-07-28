package com.almica.ramani.routes

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.almica.ramani.BuildConfig
import com.almica.ramani.Const
import com.almica.ramani.R
import com.almica.ramani.charts.GradientChartMonitor
import com.almica.ramani.charts.LineYGraphLllh
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.utils.GeoJsonUtils
import com.almica.ramani.utils.MoboMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Objects
import java.util.concurrent.Executors
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.formatDistM
import com.almica.ramani.utils.getDistanceFromLllh
import com.almica.ramani.utils.kmlString2Lllh

//private const val logtag = "RoutesGeojsonScreen"
@SuppressLint("UnrememberedMutableState", "UnusedMaterial3ScaffoldPaddingParameter",
    "LocalContextGetResourceValueCall", "BinaryOperationInTimber", "MutableCollectionMutableState"
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesGeojsonScreen(
    selectRoute: (RouteEntity?, RouteMenu) -> Unit
) {
    var importedFileUri by remember { mutableStateOf<Uri>(Uri.EMPTY) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
        onResult = { result ->
            Timber.i("result: $result")
            if (result != null) {
                Timber.i("uri: $result")
                importedFileUri = result
            }
        }
    )
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    //var routeEntity by remember { mutableStateOf<RouteEntity?>(null) }
    var showRouteMoBo by remember { mutableStateOf<RouteEntity?>(null) }
    var showRouteChart by remember { mutableStateOf<RouteEntity?>(null) }
    var showRouteGradient by remember { mutableStateOf<RouteEntity?>(null) }
    var askForNameFilter by remember { mutableStateOf(false) }
    var routeEntities by remember { mutableStateOf< MutableList<RouteEntity>>(mutableListOf())}
    var routeEntitiesSorted by remember { mutableStateOf<MutableList<RouteEntity>> (arrayListOf()) }
    var notifyDataChanged by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    //var minimalDialogText by remember { mutableStateOf<String?>(null) }
    var snackGeojsonRoutesData by remember { mutableStateOf<SnackGeojsonRoutesData?>(null) }
    var moboMessage: String? by remember { mutableStateOf(null) }
    val rootRouteFolder = File(LocalContext.current.filesDir, Const.ROUTEFOLDER)
    val fileGeojson = File(rootRouteFolder, "routes${Const.GEOJSON_EXT}")
    var fileGeojsonExits by remember { mutableStateOf(fileGeojson.exists()) }
    LaunchedEffect(key1 = snackGeojsonRoutesData) {
        Timber.i( "LaunchedEffect snackGeojsonRoutesData")
        delay(5000)
        snackGeojsonRoutesData = null
    }
    if (importedFileUri != Uri.EMPTY) {
        Timber.i("selectedFileUri $importedFileUri")
        val takeFlags: Int =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        // Check if the URI can be granted persistable permissions
        val takeFlagsSupported =
            DocumentsContract.isDocumentUri(context, importedFileUri)
        if (takeFlagsSupported) {
                LocalContext.current.contentResolver.takePersistableUriPermission(
                    importedFileUri, takeFlags
                )
                var contentSchemeNameAndSize: Pair<String, Int>? =
                    LocalContext.current.contentResolver.query(importedFileUri, null, null, null, null)
                        ?.use { cursor ->
                            if (!cursor.moveToFirst()) return@use null
                            val name =
                                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val size = cursor.getColumnIndex(OpenableColumns.SIZE)
                            cursor.getString(name) to cursor.getInt(size)
                        }
                if (contentSchemeNameAndSize != null) {
                    val filename = contentSchemeNameAndSize.first
                    Timber.i( "import: $filename")
                    saveFile(context, importedFileUri, filename)
                    //minimalDialogText = context.getString(R.string.import_ready_, filename)
                    snackGeojsonRoutesData = SnackGeojsonRoutesData(context.getString(R.string.import_ready_, filename),
                        SnackGeojsonRoutesAction.Nothing, null, null)
                    notifyDataChanged = true
                    fileGeojsonExits = fileGeojson.exists()
                    importedFileUri = Uri.EMPTY
                    contentSchemeNameAndSize = null
                    //selectRoute(null, RouteMenu.Home)
                }
        }
    }
    BackHandler {
        scope.launch {
            Timber.i("")
            if (snackGeojsonRoutesData != null)
                snackGeojsonRoutesData = null
//            if (minimalDialogText != null)
//                minimalDialogText = null
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
                    Text(text = stringResource(R.string.geojson), fontSize = 18.sp)
                }, actions = {
                    if (fileGeojsonExits) {
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
                        IconButton(onClick = {
                            shareFile(context, fileGeojson)
                        })
                        {
                            Icon(
                                Icons.Outlined.Share,
                                "share",
                                modifier = Modifier
                                    .padding(end = 10.dp, start = 10.dp)
                                    .width(60.dp)
                                    .height(60.dp)
                            )
                        }
                        IconButton(onClick = {
                            fileGeojson.delete()
                            fileGeojsonExits = fileGeojson.exists()
                        })
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
                    }
                })

        }) { _ ->
        snackGeojsonRoutesData?.let {
            //MinimalDialog(it) { minimalDialogText = null }
            MoboSnack(snackGeojsonRoutesData!!) {action ->
                when(action) {
                    SnackGeojsonRoutesAction.Nothing -> {}
                    SnackGeojsonRoutesAction.RemoveRegion -> {}
                }
            }
        }
        if (notifyDataChanged) {
            if (fileGeojsonExits) {
                RouteGeojsonContent(
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
                        } else
                            Timber.e("routes isNullOrEmpty")
                    }
                )
            }
        }
        if (fileGeojsonExits) {
            RouteGeojsonGroupedList(
                routeEntitiesSorted,
                selectRoute = { route, action ->
                    if (route != null) {
                        Timber.i(
                            "action:${action.name} " + "${route.name} ${route.region}"
                        )
                        when (action) {
                            RouteEntityItemAction.Select -> {
                                //routeEntity = route
                                showRouteMoBo = route
                                Timber.i(
                                    "${route.name}  ${route.kmlString.length}"
                                )
                                showRouteChart = null
                            }

                            RouteEntityItemAction.Map -> {
                                Timber.i("$action ${route.name}")
                                selectRoute(route, RouteMenu.Map)
                            }

                            RouteEntityItemAction.Hide -> {}
                            RouteEntityItemAction.Delete -> {}
                            RouteEntityItemAction.Database -> {}
                        }
                    } //else Timber.i("route = null")
                }
            )
        } else {
            notifyDataChanged = false
            RouteGeojsonActionScreen(fileGeojson) {action ->
                when(action) {
                    RouteGeojsonAction.Import -> {
                        launcher.launch(arrayOf("text/plain", "application/octet-stream", "*/*"))
                    }
                    RouteGeojsonAction.CreateFromFiles -> {
                        moboMessage = context.getString(R.string.create_geojson_from_files_)
                        val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                        val fileGeojson = File(rootRouteFolder, "routes${Const.GEOJSON_EXT}")

                        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            GeoJsonUtils.createGeojsonFromRoutes(rootRouteFolder, fileGeojson)
                            //delay(5000)
                        }.invokeOnCompletion {
                            Timber.i("invokeOnCompletion")
                            moboMessage = null
                            notifyDataChanged = true
                            fileGeojsonExits = fileGeojson.exists()
                        }
                    }
                    RouteGeojsonAction.CreateFromDatabase -> {
                        moboMessage = context.getString(R.string.create_geojson_from_database_)
                        GeoJsonUtils.createGeojsonFromRoutesDatabase(context) {
                            moboMessage = null
                            notifyDataChanged = true
                            fileGeojsonExits = fileGeojson.exists()
                        }
                    }
                    RouteGeojsonAction.Nothing -> {}
                }
            }
        }
        moboMessage?.let {
            MoboMessage(moboMessage!!) {
                moboMessage = null
            }
        }
        showRouteMoBo?.let {
            RouteGeojsonMoBoSheet(context,it) { action ->
                Timber.i(
                    "${Thread.currentThread().stackTrace[2].lineNumber}:´${it.name} ${it.region} action $action")
                when (action) {
                    RouteGeojsonMenu.Home -> showRouteMoBo = null
                    RouteGeojsonMenu.Chart -> {
                        showRouteChart = showRouteMoBo
                        showRouteMoBo = null
                    }
                    RouteGeojsonMenu.Gradient -> {
                        showRouteGradient = showRouteMoBo
                        showRouteMoBo = null
                    }

                    RouteGeojsonMenu.Snapshot -> {
                        takeSnapshot(
                            context,
                            it.kmlString.kmlString2Lllh(),
                            it.region,
                            it.name,
                            null
                        ) {
                            Timber.i(
                                "" +
                                        context.getString(
                                            R.string.refresh_route_preview_ready,
                                            it.name
                                        )
                            )
                            showRouteMoBo = null
                            //minimalDialogText = context.getString(R.string.refresh_route_preview_ready, it.name)
                            snackGeojsonRoutesData = SnackGeojsonRoutesData(
                                context.getString(R.string.refresh_route_preview_ready, it.name),
                                SnackGeojsonRoutesAction.Nothing, null, null
                            )
                        }
                    }
                }
            }
        }
        showRouteChart?.let {
            Timber.i("LineGraphLllh ${it.name}")
            val lllh = showRouteChart!!.kmlString.kmlString2Lllh()
            ModalBottomSheet(onDismissRequest = { showRouteChart = null }) {
                LineYGraphLllh(lllh, it.name, 0f, { _ ->
                    Timber.i("")
                    showRouteChart = null
                }, {}, Icons.AutoMirrored.Filled.ArrowBack)
            }
        }
        showRouteGradient?.let {
            val lllh = it.kmlString.kmlString2Lllh()
            val distRoute = lllh.getDistanceFromLllh()
            Timber.i("GradientChartMonitor ${it.name}")
            ModalBottomSheet(modifier = Modifier.padding(bottom = 96.dp), onDismissRequest = { showRouteGradient = null }) {
                GradientChartMonitor(
                    it,
                    Location(null), 0.0f, Icons.AutoMirrored.Filled.ArrowBack,
                    result = {
                        showRouteGradient = null
                    }, true)
                Text(
                    text = distRoute.formatDistM(true),
                    Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                //Spacer(modifier = Modifier.height(96.dp))
            }
        }
        if (askForNameFilter) {
            AskForRouteNameFilter(routeEntities, filter = { filter, region ->
                Timber.i("filter $filter")
                askForNameFilter = false
                routeEntitiesSorted = when (region) {
                    null if filter == null -> routeEntities.sortedBy { entity ->
                        entity.region.plus(entity.name)
                    }.toMutableList()
                    null -> filterByName(filter, routeEntities).toMutableList()
                    else -> filterByRegion(region, routeEntities).toMutableList()
                }
                //notifyDataChanged = true
            })
        }
    }
}

@Composable
private fun RouteGeojsonContent(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    initialize: (List<RouteEntity>?) -> Unit
) {
    var routeEntities : List<RouteEntity> = emptyList()
    LaunchedEffect(Unit) {
        Timber.i("LaunchedEffect")
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            routeEntities = GeoJsonUtils.getRouteEntitiesFromGeojson(context, null)
        }.invokeOnCompletion {
            Timber.i("")
            initialize(routeEntities.sortedBy {entity -> entity.region.plus(entity.name) })
        }
    }
}

enum class RouteGeojsonAction {
    Import,
    CreateFromFiles,
    CreateFromDatabase,
    Nothing
}
//@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun RouteGeojsonActionScreen(fileGeojson: File, action: (RouteGeojsonAction) -> Unit) {
    Timber.i("${fileGeojson.name}")
    val marginTopDp = TopAppBarDefaults.TopAppBarExpandedHeight.value
    Surface (modifier = Modifier.padding(top = (2 * marginTopDp).dp, bottom = (marginTopDp * 1.4).dp))
    { Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
                Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.file_not_found, fileGeojson.name),
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(onClick = {
                        action(RouteGeojsonAction.Import)
                    }, border = BorderStroke(2.dp, Color.DarkGray)) {
                        Text(text = stringResource(R.string.import_title),
                        Modifier.fillMaxWidth(0.8f), textAlign = TextAlign.Center, fontSize = 16.sp,)
                    }

                    OutlinedButton(onClick = {
                        action(RouteGeojsonAction.CreateFromFiles)
                    }, border = BorderStroke(2.dp, Color.DarkGray)) {
                        Text(text = stringResource(R.string.create_geojson_from_files),
                            Modifier.fillMaxWidth(0.8f), textAlign = TextAlign.Center, fontSize = 16.sp,)
                    }
                    OutlinedButton(onClick = {
                        action(RouteGeojsonAction.CreateFromDatabase)
                    }, border = BorderStroke(2.dp, Color.DarkGray)) {
                        Text(text = stringResource(R.string.create_geojson_from_database),
                            Modifier.fillMaxWidth(0.8f), textAlign = TextAlign.Center, fontSize = 16.sp,)
                    }
                }
        }
        Timber.i("not found: ${fileGeojson.path}")
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RouteGeojsonGroupedList(
    routeEntities: List<RouteEntity>,
    selectRoute: (RouteEntity?, RouteEntityItemAction) -> Unit) {
    Timber.i("routeEntities ${routeEntities.size}")
    val marginTopDp = TopAppBarDefaults.TopAppBarExpandedHeight.value
    val routesGrouped = routeEntities.groupBy { it.region }
    var groupExpanded by remember { mutableStateOf<String?>(null) }
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
                            }

                        }
                    }
                }

                items(routeEntities) { routeItem ->
                    if(groupExpanded != null && routeItem.region == groupExpanded) {
                        RouteGeojsonItem(routeItem, onItemClick = { routeItem, action ->
                            Timber.i(
                                "routeItem ${routeItem.name} action ${action.name}"
                            )
                            when (action) {
                                RouteEntityItemAction.Select -> selectRoute(routeItem, action)
                                RouteEntityItemAction.Map -> {
                                    selectRoute(routeItem, action)
                                }

                                RouteEntityItemAction.Hide -> {}
                                RouteEntityItemAction.Delete -> {}
                                RouteEntityItemAction.Database -> selectRoute(routeItem, action)
                            }
                        })
                    }
                }
                selectRoute(null, RouteEntityItemAction.Select)
            }
        }
    }
}

@Composable
fun RouteGeojsonItem(routeItem: RouteEntity, onItemClick: (RouteEntity, RouteEntityItemAction) -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 5.dp)
            .background(color = Color.White)
            .clickable { onItemClick(routeItem, RouteEntityItemAction.Select) }
    ) {
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
                Row {
                    val distText = routeItem.distance.formatDistM(true)
                    Text(
                        modifier = Modifier
                            .align(alignment = Alignment.CenterVertically)
                            .fillMaxWidth(0.8f),
                        text = "${
                            routeItem.name.replace(Const.GPX_EXT, "").replace(Const.KML_EXT, "")
                                .replace(Const.JPG_EXT, "")
                        }\n${Const.UC_DISTANCE_ARROW} $distText",
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = {
                        onItemClick(routeItem, RouteEntityItemAction.Map)
                    }) {
                        Icon(Icons.Outlined.Map, null)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteGeojsonMoBoSheet(context: Context, routeEntity: RouteEntity, routeMenu: (action: RouteGeojsonMenu) -> Unit) {
    Timber.i(routeEntity.name)
    val routeRepository = RouteRepository.getInstance(
        context.applicationContext,
        Executors.newSingleThreadExecutor()
    )
    val folderThumbnails = File(context.filesDir, Const.THUMBNAILS)
    var picFileName = routeEntity.name.replace(Const.GPX_EXT, Const.JPG_EXT)
        .replace(Const.KML_EXT, Const.JPG_EXT)
    if (!picFileName.endsWith(Const.JPG_EXT))
        picFileName = picFileName.plus(Const.JPG_EXT)
    val picFile = File(folderThumbnails, picFileName)
    var thumbnail : Bitmap? = null
    if (picFile.exists()) {
//                    Timber.i("thumbnail found ${picFile.name}")
        thumbnail = BitmapFactory.decodeFile(picFile.path)
    } else {
        Timber.i("thumbnail not found: ${picFile.path}")
        val routeName = routeEntity.name
//            .replace(Const.GPX_EXT, "")
//            .replace(Const.KML_EXT, "")
        Timber.i("routeRepository.getRouteThumbnail $routeName")
        routeRepository.getRouteThumbnail(routeName) {bitmap ->
            Timber.i("bitmap: ${bitmap?.height}")
            thumbnail = bitmap
        }
    }
    ModalBottomSheet(onDismissRequest = { routeMenu(RouteGeojsonMenu.Home) }) {
        Column {
            Text(text = routeEntity.region + " - " +
                    routeEntity.name.replace(Const.GPX_EXT, "")
                        .replace(Const.KML_EXT, "")
                        .replace(Const.JPG_EXT, ""), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            if (thumbnail != null) {
                val imageBitmap = thumbnail!!.asImageBitmap()
                // Lays out and draws an image sized to the rectangular subsection of the ImageBitmap
                Image(
                    modifier = Modifier
                        .align(alignment = Alignment.CenterHorizontally),
                        //.clickable { routeMenu(RouteGeojsonMenu.Snapshot) },
                    painter = BitmapPainter(
                        imageBitmap, IntOffset(0, 0),
                        IntSize(thumbnail!!.width, thumbnail!!.height)
                    ),
                    contentDescription = routeEntity.name
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            OutlinedButton(
                border = BorderStroke(0.dp, Color.Transparent),
                onClick = { routeMenu(RouteGeojsonMenu.Chart) }) {
                Row{
                    Icon(painterResource(R.drawable.monitoring_24px),
                        contentDescription = stringResource(R.string.elevation_chart))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        modifier = Modifier.align(alignment = Alignment.CenterVertically),
                        fontSize = 16.sp,
                        text = stringResource(R.string.elevation_chart),
                        color = Color.Black
                    )
                }
            }
            OutlinedButton(
                border = BorderStroke(0.dp, Color.Transparent),
                onClick = { routeMenu(RouteGeojsonMenu.Gradient) }) {
                Row {
                    Icon(
                        painterResource(R.drawable.gradient_24px),
                        contentDescription = stringResource(R.string.gradient)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        modifier = Modifier.align(alignment = Alignment.CenterVertically),
                        fontSize = 16.sp,
                        text = stringResource(R.string.gradient),
                        color = Color.Black
                    )
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoboSnack(snackGeojsonRoutesData: SnackGeojsonRoutesData, finished: (action: SnackGeojsonRoutesAction) -> Unit) {
    ModalBottomSheet(onDismissRequest = { finished(SnackGeojsonRoutesAction.Nothing) }) {
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
                    text = snackGeojsonRoutesData.title,
                    Modifier
                        .weight(0.8f)
                        .padding(top = 8.dp, bottom = 8.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Blue
                )
                snackGeojsonRoutesData.actionText?.let { text ->
                    TextButton(onClick = {
                        Timber.i(snackGeojsonRoutesData.action.name)
                        finished(snackGeojsonRoutesData.action)
                        when (snackGeojsonRoutesData.action) {
                            SnackGeojsonRoutesAction.Nothing -> {}
                            SnackGeojsonRoutesAction.RemoveRegion -> {}
                        }
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
        }
    }
}

enum class SnackGeojsonRoutesAction {
    Nothing,
    RemoveRegion
}
data class SnackGeojsonRoutesData(val title: String, val action: SnackGeojsonRoutesAction,
                             val actionText: String?, val actionData: String?)

enum class RouteGeojsonMenu {
    Home,
    Chart,
    Snapshot,
    Gradient
}
private fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)
    val intent = Intent(Intent.ACTION_SEND)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.type = "*/*"
    intent.putExtra(Intent.EXTRA_STREAM, uri)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

private fun saveFile(context: Context, selectedFileUri: Uri, filename: String) {
    val routesRootFolder = File(context.filesDir, Const.ROUTEFOLDER)
    routesRootFolder.mkdirs()
    val f = File(routesRootFolder, filename)
    Timber.i( "transfer --> $filename")
    val ins = context.contentResolver.openInputStream(selectedFileUri)
    if (ins != null) {
        val bytes = transferTo(ins, f.outputStream())
        Timber.i( "transfer bytes: $bytes")
    } else
        Timber.i( "$filename openInputStream = null")
}

@Throws(IOException::class)
private fun transferTo(ins: InputStream, out: FileOutputStream): Long {
    Objects.requireNonNull(out, "out")
    var transferred: Long = 0
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var read: Int
    while ((ins.read(buffer, 0, DEFAULT_BUFFER_SIZE)
            .also { read = it }) >= 0
    ) {
        out.write(buffer, 0, read)
        transferred += read.toLong()
    }
    return transferred
}

/**
 * replaced by MoBoSnack 30dez2025
 */
@Composable
fun MinimalDialog(text: String, onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = { onDismissRequest() },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.0f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            TextButton(onClick = {
                onDismissRequest()
            }) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RouteGeojsonItemPreview() {
    RamaniTheme {
        RouteGeojsonItem(
            routeItem = RouteEntity(
                name = "Sample Route",
                region = "Sample Region",
                latitudeStart = 0.0,
                longitudeStart = 0.0,
                latitudeCenter = 0.0,
                longitudeCenter = 0.0,
                distance = 1500.0,
                kmlString = "",
                bitmap = null
            ),
            onItemClick = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RouteGeojsonActionScreenPreview() {
    RamaniTheme {
        RouteGeojsonActionScreen(
            fileGeojson = File("routes.geojson"),
            action = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RouteGeojsonGroupedListPreview() {
    RamaniTheme {
        RouteGeojsonGroupedList(
            routeEntities = listOf(
                RouteEntity(name = "Route 1", region = "Region A", distance = 1000.0),
                RouteEntity(name = "Route 2", region = "Region A", distance = 2000.0),
                RouteEntity(name = "Route 3", region = "Region B", distance = 3000.0)
            ),
            selectRoute = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RouteGeojsonMoboSnackPreview() {
    RamaniTheme {
        MoboSnack(
            snackGeojsonRoutesData = SnackGeojsonRoutesData(
                title = "Sample Snack Message",
                action = SnackGeojsonRoutesAction.Nothing,
                actionText = "Action",
                actionData = null
            ),
            finished = {}
        )
    }
}
