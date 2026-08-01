package com.almica.ramani.routes

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Height
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.currentComposer
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.almica.ramani.Const
import com.almica.ramani.Helpers
import com.almica.ramani.LatLngH
import com.almica.ramani.R
import com.almica.ramani.charts.GradientChartMonitor
import com.almica.ramani.charts.LineYGraphLllh
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.googlemaps.MapUtils
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.routes.ComposeHelpers.Companion.removeRouteLine
import com.almica.ramani.utils.HgtReader
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.formatDistM
import com.almica.ramani.utils.getDistanceFromLllh
import com.almica.ramani.utils.kmlString2Lllh
import com.almica.ramani.utils.lllhToKmlString
import com.almica.ramani.utils.reduceWithTolerance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

private const val logtag = "RouteDatabaseScreen"
internal enum class RouteSortOrder{
    ByName,
    ByDistance
}

@SuppressLint("UnrememberedMutableState", "UnusedMaterial3ScaffoldPaddingParameter",
    "LocalContextGetResourceValueCall", "MutableCollectionMutableState"
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDatabaseScreen(mapPos: LatLng?, selectRoute: (RouteEntity?, RouteMenu) -> Unit) {
    val context = LocalContext.current
    val currentComposer = currentComposer
    val lifecycleOwner = LocalLifecycleOwner.current
    var routeEntity by remember { mutableStateOf<RouteEntity?>(null) }
    var showRouteMoBo by remember { mutableStateOf<RouteEntity?>(null) }
    var showRouteChart by remember { mutableStateOf<RouteEntity?>(null) }
    var showRouteGradient by remember { mutableStateOf<RouteEntity?>(null) }
    var showSrtmFiles by remember { mutableStateOf(false) }
    var srtmFile by remember { mutableStateOf<File?>(null) }
    var askForNameFilter by remember { mutableStateOf(false) }
    var routeEntities: MutableList<RouteEntity> = mutableListOf()
    var routeEntitiesSorted by remember { mutableStateOf<MutableList<RouteEntity>>(mutableListOf()) }
    var sortOrder by remember { mutableStateOf(RouteSortOrder.ByName) }
    var notifyDataChanged by remember { mutableStateOf(true) }
    var filterString by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var snackDbRoutesData by remember { mutableStateOf<SnackDbRoutesData?>(null) }
    LaunchedEffect(key1 = snackDbRoutesData) {
        Timber.i( "${snackDbRoutesData?.title}")
        delay(5000)
        snackDbRoutesData = null
    }
    BackHandler {
        scope.launch {
            Timber.i( "")
            //(context as Activity).finish()
            selectRoute(null, RouteMenu.Home)
        }
    }
    Scaffold(
        //modifier = Modifier.padding(bottom = Const.DP42.times(LocalDensity.current.density)),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            selectRoute(null, RouteMenu.Home)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back home"
                        )
                    }
                }, title = {
                    Text(text = stringResource(R.string.route_database), fontSize = 18.sp)
                }, actions = {
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
                        snackDbRoutesData = SnackDbRoutesData(context.getString(R.string.export_started),
                                SnackDbRoutesAction.Nothing, actionText = null, null)
                        exportRouteDatabase(context, lifecycleOwner) {
                            snackDbRoutesData = null
                        }
                    })
                    {
                        Icon(Icons.Default.ImportExport,
                            context.getString(R.string.export_started),
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
        }) { _ ->
        snackDbRoutesData?.let {
            MoboSnack(snackDbRoutesData!!) { action ->
                Timber.i( "action: $action")
                when (action) {
                    SnackDbRoutesAction.Nothing -> snackDbRoutesData = null
                    SnackDbRoutesAction.RemoveRegion -> {
                        val region = it.actionData
                        Timber.i("${it.action}")
                        val routeRepository = RouteRepository.getInstance(context, Executors.newSingleThreadExecutor())
                        region?.let { it1 ->
                            routeRepository.removeRoutes(it1) {
                                notifyDataChanged = true
                            }
                        }
                    }
                    SnackDbRoutesAction.ShowSrtmFiles -> {
                        showSrtmFiles = true
                        snackDbRoutesData = null
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
        if (notifyDataChanged) {
            RouteDatabaseContent(
                context, mapPos, lifecycleOwner, sortOrder,
                initialize = { routes ->
                    if (!routes.isNullOrEmpty()) {
                        Timber.i("routes ${routes.size}")
                        routeEntities = routes.toMutableList()
                        //routeEntitiesSorted = routes.toMutableList()
                        if (filterString.isNotNull()) {
                            val routeEntitiesFiltered =
                                filterByName(filterString, routeEntities).toMutableList()
                            Timber.i("routeEntitiesSorted: ${routeEntitiesSorted.size}")
                            routeEntitiesSorted =
                                routeEntitiesFiltered.sortedBy { it.region.plus(it.name) }
                                    .toMutableList()
                            filterString = null
                        } else
                            routeEntitiesSorted =
                                routeEntities.sortedBy { it.region.plus(it.name) }
                                    .toMutableList()
                        sortOrder = RouteSortOrder.ByName
                    } else
                        Timber.e(
                            "routes isNullOrEmpty")
                    //ScreenRouter.navigateHome()
                    notifyDataChanged = false
                }
            )
        }
        //Timber.i( "routeEntitiesSorted ${routeEntitiesSorted.size}")
        RouteDatabaseGroupedList(mapPos, routeEntitiesSorted, selectRoute = { route, action ->
            //Timber.i( "routeEntities ${routeEntities.size} delete $delete" )
            //Timber.i( "${routeEntity?.name} $action")
            if (route != null) {
                Timber.i(
                    "${route.name} ${route.region}")
                when (action) {
                    RouteEntityItemAction.Select -> {
                        routeEntity = route
                        showRouteMoBo = route
                        showRouteChart = null
                        //selectRoute(routeEntity)
                    }

                    RouteEntityItemAction.Delete -> {
                        val routeRepository =
                            RouteRepository.getInstance(context, Executors.newSingleThreadExecutor())
                        routeRepository.removeRoute(route.id)
                        var result = routeEntitiesSorted.remove(route)
                        Timber.i(
                            "remove result $result routeEntitiesSorted ${routeEntitiesSorted.size}"
                        )
                        result = routeEntities.remove(route)
                        Timber.i(
                            "remove result $result routeEntities ${routeEntities.size}"
                        )
                        sortOrder = RouteSortOrder.ByName
                        notifyDataChanged = true
                    }

                    RouteEntityItemAction.Map -> {
                        //showDbRouteEntityInMap(currentComposer, map, route)
                        selectRoute(route, RouteMenu.Map) // exit
                    }

                    RouteEntityItemAction.Hide -> {
                        route.let {
                            removeRouteLine(currentComposer, it.name)
                            selectRoute(route, RouteMenu.Home) // exit
                            Timber.i("removeRouteLine ${it.name}")
                        }
                    }

                    RouteEntityItemAction.Database -> {}
                }
            }
        }, deleteRegion = {region ->
            Timber.i( "region: $region")
            snackDbRoutesData =
                SnackDbRoutesData(
                    context.getString(R.string.remove_region_, region),
                    action = SnackDbRoutesAction.RemoveRegion,
                    actionText = context.getString(R.string.ok), region
                )
        })

        showRouteChart?.let {
            Timber.i(
                "LineGraphLllh ${it.name}")
            val lllh = it.kmlString.kmlString2Lllh()
            val lllhReduced = lllh.reduceWithTolerance(200.0)
            ModalBottomSheet(onDismissRequest = { showRouteChart = null }) {
                LineYGraphLllh(lllhReduced, it.name, 0f, { latLng ->
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
        showRouteMoBo?.let {
            RouteDatabaseMoBoSheet(it) { action ->
                Timber.i(
                    "${it.name} ${it.region} action $action")
                when (action) {
                    RouteDatabaseMenu.Home -> showRouteMoBo = null
                    RouteDatabaseMenu.Map -> {
                        selectRoute(it, RouteMenu.Map)
                        showRouteMoBo = null
                    }

                    RouteDatabaseMenu.DeleteEntry -> {
                        val routeRepository =
                            RouteRepository.getInstance(
                                context,
                                Executors.newSingleThreadExecutor()
                            )
                        routeRepository.removeRoute(it.id)
                        var result = routeEntitiesSorted.remove(it)
                        Timber.i("remove result $result routeEntitiesSorted ${routeEntitiesSorted.size}")
                        result = routeEntities.remove(it)
                        Timber.i("remove result $result routeEntities ${routeEntities.size}")
                        val sharedPref = context.getSharedPreferences(
                            context.getString(R.string.early_annotations),
                            Context.MODE_PRIVATE
                        )
                        sharedPref.edit { remove(it.id.toString()) } // false for database route
                        sortOrder = RouteSortOrder.ByName
                        notifyDataChanged = true
                        showRouteMoBo = null
                    }

                    RouteDatabaseMenu.Chart -> {
                        showRouteChart = it.copy()
                        showRouteMoBo = null
                    }

                    RouteDatabaseMenu.Gradient -> {
                        showRouteGradient = it.copy()
                        showRouteMoBo = null
                    }

                    RouteDatabaseMenu.RefreshPreview -> {
                        val lllh = it.kmlString.kmlString2Lllh()
                        Helpers.takeSnapshot(context, lllh, it.name, Const.styleVectorUri, 512, 0.1, true) { snapShot ->
                            if (snapShot != null) {
                                it.bitmap =
                                    snapShot.bitmap // required for listview refresh
                                val track = Track(lllh)
                                val kmlString = lllh.lllhToKmlString(it.name)
                                replaceRouteDao(
                                    context,
                                    it.name,
                                    it.region,
                                    kmlString,
                                    snapShot.bitmap,
                                    track
                                ) {
                                    //notifyDataChanged = true
                                    showRouteMoBo = null
                                    Timber.i("notifyDataChanged ${it.name}")
                                }
                            } else {
                                showRouteMoBo = null
                                Timber.i("snapshot = null")
                            }
                        }
                    }

                    RouteDatabaseMenu.ElevationRefreshFromSrtm -> {
                        val lllh = it.kmlString.kmlString2Lllh()
                        val hgtFile = srtmFile
                        if (hgtFile != null) {
                            Timber.i("hgtFile: ${hgtFile.name}")
                            if (hgtFile.exists()) {
                                val hgtReader = HgtReader(context, hgtFile)
                                val refreshedLllh =
                                    hgtReader.refreshRouteElevationFromSrtm(lllh).lllh as java.util.ArrayList<LatLngH>
                                routeEntity?.let { it1 ->
                                    val kmlStringUpdated =
                                        refreshedLllh.lllhToKmlString(it1.name)
                                    val routeRepository = RouteRepository.getInstance(
                                        context,
                                        Executors.newSingleThreadExecutor()
                                    )
                                    it1.kmlString = kmlStringUpdated
                                    routeRepository.updateRoute(
                                        kmlStringUpdated,
                                        it1.id
                                    )
                                    snackDbRoutesData =
                                        SnackDbRoutesData(
                                            "Route Database Update: ${it1.name.replace(Const.GPX_EXT, "")
                                                .replace(Const.JPG_EXT, "")
                                                .replace(Const.KML_EXT, "")}",
                                            action = SnackDbRoutesAction.Nothing,
                                            actionText = null, null
                                        )
                                }
                            } else {
                                snackDbRoutesData =
                                    SnackDbRoutesData(
                                        context.getString(R.string.not_found_, hgtFile.path),
                                        action = SnackDbRoutesAction.Nothing,
                                        actionText = null, null
                                    )

                                Timber.i("not found: ${hgtFile.path}")
                            }
                        } else {
                            snackDbRoutesData =
                                SnackDbRoutesData(
                                    context.getString(R.string.select_hgt_file),
                                    action = SnackDbRoutesAction.ShowSrtmFiles,
                                    actionText = context.getString(R.string.ok), null
                                )
                        }
                        showRouteMoBo = null
                    }

                    RouteDatabaseMenu.ElevationGmsService -> {
                        routeEntity?.let {re ->
                            val lllh = re.kmlString.kmlString2Lllh()
                            if (lllh.size < 512) {
                                val gmsLatLng: List<LatLng> = List(lllh.size) { i ->
                                    LatLng(lllh[i].latitude, lllh[i].longitude)
                                }
                                val encodedPolyline = PolyUtil.encode(gmsLatLng)
                                MapUtils.gmsElevationService(
                                    context,
                                    "enc:${encodedPolyline}"
                                ) { refreshedLllh ->
                                    if (refreshedLllh.isNotNull() && refreshedLllh.isNotEmpty()) {
                                        val kmlStringUpdated =
                                            refreshedLllh.lllhToKmlString(re.name)
                                        val routeRepository = RouteRepository.getInstance(
                                            context,
                                            Executors.newSingleThreadExecutor()
                                        )
                                        re.kmlString = kmlStringUpdated
                                        routeRepository.updateRoute(kmlStringUpdated, re.id)
                                        snackDbRoutesData =
                                            SnackDbRoutesData(
                                                "Route Database Update: ${re.name.replace(Const.GPX_EXT, "")
                                                    .replace(Const.JPG_EXT, "")
                                                    .replace(Const.KML_EXT, "")}",
                                                action = SnackDbRoutesAction.Nothing,
                                                actionText = null, null
                                            )
                                    } else {
                                        snackDbRoutesData =
                                            SnackDbRoutesData(
                                                "${context.getString(R.string.gms_elevation_service)} FAILED",
                                                action = SnackDbRoutesAction.Nothing,
                                                actionText = null, null
                                            )
                                        Timber.i(
                                            "" +
                                                    "${context.getString(R.string.gms_elevation_service)} FAILED"
                                        )
                                    }
                                }
                            } else {
                                Timber.i( "route coordinates: ${lllh.size}")
                                snackDbRoutesData =
                                    SnackDbRoutesData(
                                        context.getString(R.string.too_many_coordinates_512),
                                        action = SnackDbRoutesAction.Nothing,
                                        actionText = null, null
                                    )
                            }
                        }
                        showRouteMoBo = null
                    }
                }
            }
        }

        if (askForNameFilter) {
            AskForRouteNameFilter(routeEntitiesSorted, filter = { filter, region ->
                Timber.i( "filter: $filter region: $region")
                askForNameFilter = false
                if (region == null && filter == null)
                    notifyDataChanged = true // show all
//                        routeEntitiesSorted.sortedBy {entity ->
//                            entity.region.plus(entity.name) }.toMutableList()
                else {
                    routeEntitiesSorted = if (region == null)
                        filterByName(filter, routeEntitiesSorted).toMutableList()
                    else
                        filterByRegion(region, routeEntitiesSorted).toMutableList()
                }
                //notifyDataChanged = true
            }, restoreRegionsList = {
                notifyDataChanged = true
            })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RouteDatabaseGroupedList(
    mapPos: LatLng?,
    routeEntities: List<RouteEntity>,
    selectRoute: (RouteEntity?, RouteEntityItemAction) -> Unit,
    deleteRegion: (String?) -> Unit
) {
    //Timber.i( "routeEntities " + routeEntities.size)
    val marginTopDp = TopAppBarDefaults.TopAppBarExpandedHeight.value
    //val regions = createRegionArray(routeEntities)
    var groupExpanded by remember { mutableStateOf<String?>(null) }
    val routesGrouped = routeEntities.groupBy { it.region }
    var performNotify by remember { mutableStateOf(true) }
    if (performNotify) {
        Scaffold(modifier = Modifier.padding(top = marginTopDp.dp.times(1.5f), bottom = marginTopDp.dp.times(1.4f)))
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
                                    IconButton(onClick = {
                                        Timber.i("$initial")
                                        deleteRegion(initial)
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
                            RouteDatabaseItem(
                                mapPos,
                                routeItem,
                                onItemClick = { routeItem, action ->
                                    selectRoute(routeItem, action)
                                    if (action == RouteEntityItemAction.Delete) {
                                        performNotify = false
                                    }
                                })
                        }
                    }
                    selectRoute(null, RouteEntityItemAction.Select)
                }
            }
        }
    }
}

@Composable
fun RouteDatabaseItem(mapPos: LatLng?, routeItem: RouteEntity, onItemClick: (RouteEntity, RouteEntityItemAction) -> Unit) {
    Box(
        modifier = Modifier
            .background(color = Color.White)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        Timber.i("onTap ${routeItem.name}")
                        onItemClick(routeItem, RouteEntityItemAction.Select)
                    }, onLongPress = {
                        Timber.i("onLongPress ${routeItem.name}")
                        onItemClick(routeItem, RouteEntityItemAction.Delete)
                    }
                )
            }
    ) {
        Column(
            Modifier.fillMaxSize().padding(start = 5.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val dist = if (mapPos.isNotNull()) mapPos?.let {
                SphericalUtil.computeDistanceBetween(
                    LatLng(
                        routeItem.latitudeStart,
                        routeItem.longitudeStart
                    ), it
                )
            } else
                0.0
            val textDist = dist?.formatDistM(true)
            val heading = if (mapPos.isNotNull()) mapPos?.let {
                SphericalUtil.computeHeading(
                    it, LatLng(
                        routeItem.latitudeStart,
                        routeItem.longitudeStart
                    )
                )
            } else
                0.0
            var textHeading = heading?.let { Helpers.getArrowDirection(it, LocalContext.current) }
            if (dist != null) {
                if (dist < 50)
                    textHeading = Const.UC_DISTANCE_ARROW
            }
            //Helpers.formatDistM(routeEntities[position].distance, true)
            val itemText = String.format(
                Locale.ENGLISH,
                "%s %s %s",
                routeItem.getLine(),
                textHeading, //Const.UC_DISTANCE_ARROW,
                textDist
            )

            //Text(text = itemText, fontSize = 10.sp) //, fontWeight = FontWeight.Bold)
            Row {
                Text(modifier = Modifier.align(alignment = Alignment.CenterVertically).weight(0.8f),
                    text = routeItem.name.replace(Const.GPX_EXT, "")
                        .replace(Const.JPG_EXT, "")
                        .replace(Const.KML_EXT, ""), fontSize = 14.sp
                )
                IconButton(modifier = Modifier.weight(0.2f),
                    onClick = {
                        onItemClick(routeItem, RouteEntityItemAction.Map)
                    }
                ) {
                    Icon(
                        Icons.Outlined.Map,
                        null
                    )
                }
            }
            Text(
                //modifier = Modifier.align(alignment = Alignment.CenterVertically),
                text = itemText,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (routeItem.bitmap != null) {
                val imageBitmap = routeItem.bitmap!!.asImageBitmap()
                // Lays out and draws an image sized to the rectangular subsection of the ImageBitmap
                Image(
                    painter = BitmapPainter(imageBitmap, IntOffset(0, 0), IntSize(routeItem.bitmap!!.width, routeItem.bitmap!!.height)),
                    contentDescription = routeItem.name
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Spacer(modifier = Modifier
                .height(2.dp)
                .background(Color.LightGray)
                .fillMaxWidth())
        }
    }
}

@Composable
internal fun RouteDatabaseContent(
    context: Context,
    mapPos: LatLng?,
    lifecycleOwner: LifecycleOwner,
    sortOrder: RouteSortOrder,
    initialize: (List<RouteEntity>?) -> Unit
) {
    val routeRepository = RouteRepository.getInstance(
        context.applicationContext,
        Executors.newSingleThreadExecutor()
    )
    var routeEntities : List<RouteEntity> = emptyList()
    LaunchedEffect(Unit) {
        Timber.i( "LaunchedEffect")
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            routeEntities = routeRepository.getAllSimple()
        }.invokeOnCompletion {
//            routeEntities.forEach { routeEntity ->
//                Timber.i( "${routeEntity.name} ${routeEntity.region}")
//            }
            Timber.i( "sortOrder $sortOrder")
            when(sortOrder) {
                RouteSortOrder.ByName -> initialize(routeEntities.sortedBy { it.region.plus(it.name) })
                RouteSortOrder.ByDistance -> initialize(routeEntities.sortedBy {
                    mapPos?.let { it1 ->
                        SphericalUtil.computeDistanceBetween(
                            LatLng(it.latitudeStart, it.longitudeStart), it1)
                    }

                })
            }
        }
    }
}

@Composable
fun RouteDatabaseRegionList(
    paddingValues: PaddingValues,
    routeEntities: List<RouteEntity>?,
    selectRegion: (String?) -> Unit
) {
    //Timber.i( "routeEntities " + routeEntities?.size)
    val regions = createDatabaseRegionArray(routeEntities)
    Timber.i( "regions: ${regions.size}")
    LazyColumn(
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(regions) { region ->
            Box(
                modifier = Modifier
                    .background(color = Color.White)
                    .clickable { selectRegion(region) }
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

fun createDatabaseRegionArray(routeEntities: List<RouteEntity>?) : Array<String?> {
    Timber.i( "routeEntities: ${routeEntities?.size}")
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

fun exportRouteDatabase(context: Context, lifecycleOwner: LifecycleOwner, finished: (Int) -> Unit) {
    val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
    val routeRepository = RouteRepository.getInstance(context, Executors.newSingleThreadExecutor())
    var filesCount = 0
    routeRepository.getAllSimple { routes ->
        Timber.i("routes: ${routes.size}")
        routes.forEachIndexed { _, route ->
            val routeDir = File(rootRouteFolder, route.region)
            routeDir.mkdir()
            val routeFile = File(routeDir, route.name + Const.KML_EXT)
            val lllhRaw = route.kmlString.kmlString2Lllh()
            if (lllhRaw.isNotEmpty()) {
                Helpers.writeLllh2KmlFile(lllhRaw, routeFile.path)
                //Timber.i( "export ready: ${routeFile.path} ")
                filesCount++
            }
        }
        Timber.i("route db export ready: $filesCount")
        finished(filesCount)
    }
}

@Composable
private fun AskForRouteNameFilter(routeEntities: List<RouteEntity>?, filter: (String?, String?) -> Unit, restoreRegionsList: () -> Unit) {
    var nameFilter by remember { mutableStateOf("") }
    Surface {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { filter(null, null) }
                ) {
                    Icon(
                        // imageVector = Icons.Outlined.Restore,
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back home"
                    )
                }
                Text(
                    text = stringResource(R.string.regions_),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                IconButton(
                    onClick = { restoreRegionsList() }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = "restore regions list"
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            RouteDatabaseRegionList(PaddingValues(0.dp), routeEntities) { region ->
                Timber.i( "$region ")
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
enum class RouteDatabaseMenu {
    Home,
    Map,
    Chart,
    ElevationRefreshFromSrtm,
    Gradient,
    RefreshPreview,
    DeleteEntry,
    ElevationGmsService
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteDatabaseMoBoSheet(routeEntity: RouteEntity, routeMenu: (action: RouteDatabaseMenu) -> Unit) {
    ModalBottomSheet(onDismissRequest = { routeMenu(RouteDatabaseMenu.Home) }) {
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteDatabaseMenu.Map) }) {
                    Row {
                        Icon(
                            painterResource(R.drawable.outline_map_24),
                            contentDescription = stringResource(R.string.map)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp,
                            text = stringResource(R.string.map),
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                Button(
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteDatabaseMenu.RefreshPreview) }) {
                    Row {
                        Icon(Icons.Outlined.Preview,
                            contentDescription = stringResource(R.string.refresh_route_preview)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            modifier = Modifier.weight(1f),
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
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteDatabaseMenu.Chart) }) {
                    Row {
                        Icon(
                            painterResource(R.drawable.monitoring_24px),
                            contentDescription = stringResource(R.string.elevation_chart)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(1f),
                            fontSize = 14.sp,
                            text = stringResource(R.string.elevation_chart),
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                Button(
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteDatabaseMenu.Gradient) }) {
                    Row {
                        Icon(
                            painterResource(R.drawable.gradient_24px),
                            contentDescription = stringResource(R.string.gradient)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(1f),
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
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteDatabaseMenu.ElevationGmsService) }) {
                    Row {
                        Icon(
                            Icons.Outlined.Height,
                            contentDescription = stringResource(R.string.gms_elevation_service)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(1f),
                            fontSize = 14.sp,
                            text = stringResource(R.string.gms_elevation_service),
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                Button(
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteDatabaseMenu.ElevationRefreshFromSrtm) }) {
                    Row {
                        Icon(
                            Icons.Outlined.Height,
                            contentDescription = stringResource(R.string.elevation_refresh)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(modifier = Modifier.weight(1f),
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
                    modifier = Modifier.weight(0.5f).fillMaxHeight(),
                    shape = RoundedCornerShape(corner = CornerSize(3.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.Blue
                    ),
                    onClick = { routeMenu(RouteDatabaseMenu.DeleteEntry) }) {
                    Row {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete_route)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp,
                            text = stringResource(R.string.delete_route),
                            color = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(0.5f))
            }
        }
    }
}
enum class SnackDbRoutesAction {
    Nothing,
    RemoveRegion,
    ShowSrtmFiles
}
data class SnackDbRoutesData(val title: String, val action: SnackDbRoutesAction,
                             val actionText: String?, val actionData: String?)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoboSnack(snackDbRoutesData: SnackDbRoutesData, finished: (action: SnackDbRoutesAction) -> Unit) {
    ModalBottomSheet(onDismissRequest = { finished(SnackDbRoutesAction.Nothing) }) {
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
                    text = snackDbRoutesData.title,
                    Modifier
                        .weight(0.8f)
                        .padding(top = 8.dp, bottom = 8.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Blue
                )
                snackDbRoutesData.actionText?.let { text ->
                    TextButton(onClick = {
                        Timber.i("${snackDbRoutesData.action.name}")
                        finished(snackDbRoutesData.action)
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

@ComposePreview(showBackground = true)
@Composable
fun RouteDatabaseItemPreview() {
    val sampleRoute = RouteEntity(
        name = "Sample Route.kml",
        region = "Sample Region",
        latitudeStart = -1.2833,
        longitudeStart = 36.8167,
        distance = 5000.0
    )
    RamaniTheme {
        RouteDatabaseItem(
            mapPos = LatLng(-1.2833, 36.8167),
            routeItem = sampleRoute,
            onItemClick = { _, _ -> }
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
fun RouteDatabaseGroupedListPreview() {
    val sampleRouteEntities = listOf(
        RouteEntity(name = "Hiking Trail.kml", region = "Mountains"),
        RouteEntity(name = "City Walk.gpx", region = "City"),
        RouteEntity(name = "Forest Path.jpg", region = "Mountains")
    )
    RamaniTheme {
        RouteDatabaseGroupedList(
            mapPos = LatLng(-1.2833, 36.8167),
            routeEntities = sampleRouteEntities,
            selectRoute = { _, _ -> },
            deleteRegion = {}
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
fun RouteDatabaseMoBoSheetPreview() {
    RamaniTheme {
        RouteDatabaseMoBoSheet(
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
fun AskForRouteNameFilterPreview() {
    val sampleRouteEntities = listOf(
        RouteEntity(name = "Hiking Trail.kml", region = "Mountains"),
        RouteEntity(name = "City Walk.gpx", region = "City")
    )
    RamaniTheme {
        AskForRouteNameFilter(
            routeEntities = sampleRouteEntities,
            filter = { _, _ -> },
            restoreRegionsList = {}
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
fun MoboSnackPreview() {
    RamaniTheme {
        MoboSnack(
            snackDbRoutesData = SnackDbRoutesData(
                title = "Sample Message",
                action = SnackDbRoutesAction.Nothing,
                actionText = "OK",
                actionData = null
            ),
            finished = {}
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
fun RouteDatabaseRegionListPreview() {
    val sampleRouteEntities = listOf(
        RouteEntity(name = "Hiking Trail.kml", region = "Mountains"),
        RouteEntity(name = "City Walk.gpx", region = "City"),
        RouteEntity(name = "Forest Path.jpg", region = "Mountains")
    )
    RamaniTheme {
        RouteDatabaseRegionList(
            paddingValues = PaddingValues(16.dp),
            routeEntities = sampleRouteEntities,
            selectRegion = {}
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
fun RouteDatabaseContentPreview() {
    RamaniTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            Text("RouteDatabaseContent is a headless data-loading component.")
        }
    }
}

@ComposePreview(showBackground = true)
@Composable
fun RouteDatabaseScreenPreview() {
    RamaniTheme {
        RouteDatabaseScreen(
            mapPos = LatLng(-1.2833, 36.8167),
            selectRoute = { _, _ -> }
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
fun DropdownSrtmFilesPreview() {
    RamaniTheme {
        DropdownSrtmFiles(
            context = LocalContext.current,
            srtmFile = java.io.File("sample.hgt"),
            selected = { _, _ -> }
        )
    }
}
