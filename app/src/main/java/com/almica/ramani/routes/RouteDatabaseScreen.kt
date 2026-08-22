package com.almica.ramani.routes

import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.Composer
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almica.ramani.Const
import com.almica.ramani.Helpers
import com.almica.ramani.R
import com.almica.ramani.routes.Track
import com.almica.ramani.charts.GradientChartMonitor
import com.almica.ramani.charts.LineYGraphLllh
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.utils.isNotNull
import com.almica.ramani.routes.ComposeHelpers.Companion.removeRouteLine
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.formatDistM
import com.almica.ramani.utils.getDistanceFromLllh
import com.almica.ramani.utils.kmlString2Lllh
import com.almica.ramani.utils.lllhToKmlString
import com.almica.ramani.utils.reduceWithTolerance
import timber.log.Timber
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import androidx.compose.ui.tooling.preview.Preview as ComposePreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDatabaseScreen(
    mapPos: LatLng?,
    viewModel: RouteViewModel = viewModel(
        factory = RouteViewModelFactory(
            RouteRepository.getInstance(LocalContext.current.applicationContext, Executors.newSingleThreadExecutor()),
            mapPos
        )
    ),
    selectRoute: (RouteEntity?, RouteMenu) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val composer = currentComposer

    BackHandler {
        selectRoute(null, RouteMenu.Home)
    }

    Scaffold(
        topBar = {
            RouteDatabaseTopBar(
                srtmFile = uiState.srtmFile,
                onBack = { selectRoute(null, RouteMenu.Home) },
                onFilterClick = { viewModel.showAskForFilter(true) },
                onExportClick = { viewModel.exportDatabase(context) },
                onSrtmClick = { viewModel.showSrtmFiles(true) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (uiState.askForNameFilter) {
                AskForRouteNameFilter(
                    routeEntities = routes,
                    onFilter = { name, region ->
                        if (name == null && region == null) viewModel.clearFilter()
                        else if (region != null) viewModel.onFilterRegionChanged(region)
                        else viewModel.onFilterNameChanged(name)
                    },
                    onRestoreRegionsList = { viewModel.clearFilter() }
                )
            } else {
                RouteDatabaseGroupedList(
                    mapPos = mapPos,
                    routeEntities = routes,
                    onItemAction = { route, action ->
                        handleRouteAction(route, action, viewModel, selectRoute, composer)
                    },
                    onDeleteRegion = { viewModel.deleteRegion(it) }
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (uiState.showSrtmFiles) {
                DropdownSrtmFiles(context, uiState.srtmFile) { file, import ->
                    viewModel.showSrtmFiles(false)
                    viewModel.setSrtmFile(file)
                    if (import) {
                        FileImportActivity.launch(context, FileType.Hgt)
                    }
                }
            }
        }
    }

    RouteDatabaseSheets(uiState, viewModel, selectRoute)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteDatabaseTopBar(
    srtmFile: File?,
    onBack: () -> Unit,
    onFilterClick: () -> Unit,
    onExportClick: () -> Unit,
    onSrtmClick: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back home")
            }
        },
        title = { Text(text = stringResource(R.string.route_database), fontSize = 18.sp) },
        actions = {
            IconButton(onClick = onFilterClick) {
                Icon(
                    painterResource(R.drawable.outline_filter_alt_24),
                    "filter",
                    modifier = Modifier.padding(horizontal = 10.dp).width(60.dp).height(60.dp)
                )
            }
            IconButton(onClick = onExportClick) {
                Icon(
                    Icons.Default.ImportExport,
                    "export",
                    modifier = Modifier.padding(horizontal = 10.dp).width(60.dp).height(60.dp)
                )
            }
            BadgedBox(badge = {
                if (srtmFile.isNotNull()) Badge { Text(text = Const.UC_CHECKMARK) }
            }) {
                TextButton(onClick = onSrtmClick) {
                    Text(text = stringResource(R.string.srtm))
                }
            }
        }
    )
}

private fun handleRouteAction(
    route: RouteEntity?,
    action: RouteEntityItemAction,
    viewModel: RouteViewModel,
    selectRoute: (RouteEntity?, RouteMenu) -> Unit,
    composer: Composer
) {
    if (route == null) return
    when (action) {
        RouteEntityItemAction.Select -> viewModel.selectRoute(route)
        RouteEntityItemAction.Delete -> viewModel.deleteRoute(route)
        RouteEntityItemAction.Map -> selectRoute(route, RouteMenu.Map)
        RouteEntityItemAction.Hide -> {
            removeRouteLine(composer, route.name)
            selectRoute(route, RouteMenu.Home)
        }
        RouteEntityItemAction.Database -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteDatabaseSheets(
    uiState: RouteUiState,
    viewModel: RouteViewModel,
    onSelectRoute: (RouteEntity?, RouteMenu) -> Unit
) {
    val context = LocalContext.current

    uiState.snackData?.let { snack ->
        MoboSnack(snack) { action ->
            when (action) {
                SnackDbRoutesAction.Nothing -> viewModel.dismissSnack()
                SnackDbRoutesAction.RemoveRegion -> snack.actionData?.let { viewModel.confirmDeleteRegion(it) }
                SnackDbRoutesAction.ShowSrtmFiles -> {
                    viewModel.showSrtmFiles(true)
                    viewModel.dismissSnack()
                }
            }
        }
    }

    uiState.showRouteChart?.let { route ->
        val lllh = route.kmlString.kmlString2Lllh()
        val lllhReduced = lllh.reduceWithTolerance(200.0)
        ModalBottomSheet(onDismissRequest = { viewModel.showChart(null) }) {
            LineYGraphLllh(lllhReduced, route.name, 0f, { viewModel.showChart(null) }, {}, Icons.AutoMirrored.Filled.ArrowBack)
        }
    }

    uiState.showRouteGradient?.let { route ->
        val lllh = route.kmlString.kmlString2Lllh()
        val distRoute = lllh.getDistanceFromLllh()
        ModalBottomSheet(
            modifier = Modifier.padding(bottom = 96.dp),
            onDismissRequest = { viewModel.showGradient(null) }
        ) {
            GradientChartMonitor(
                route, 0.0f, Icons.AutoMirrored.Filled.ArrowBack,
                result = { viewModel.showGradient(null) }, animated = true
            )
            Text(
                text = distRoute.formatDistM(true),
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }

    uiState.showRouteMoBo?.let { route ->
        RouteDatabaseMoBoSheet(route) { menuAction ->
            when (menuAction) {
                RouteDatabaseMenu.Home -> viewModel.selectRoute(null)
                RouteDatabaseMenu.Map -> {
                    onSelectRoute(route, RouteMenu.Map)
                    viewModel.selectRoute(null)
                }
                RouteDatabaseMenu.DeleteEntry -> {
                    viewModel.deleteRoute(route)
                    context.getSharedPreferences(context.getString(R.string.early_annotations), Context.MODE_PRIVATE)
                        .edit { remove(route.id.toString()) }
                }
                RouteDatabaseMenu.Chart -> viewModel.showChart(route)
                RouteDatabaseMenu.Gradient -> viewModel.showGradient(route)
                RouteDatabaseMenu.RefreshPreview -> {
                    val lllh = route.kmlString.kmlString2Lllh()
                    Helpers.takeSnapshot(context, lllh, route.name, Const.styleVectorUri, 512, 0.1, true) { snapShot ->
                        if (snapShot != null) {
                            route.bitmap = snapShot.bitmap
                            val kmlString = lllh.lllhToKmlString(route.name)
                            viewModel.replaceRoute(route.name, route.region, kmlString, snapShot.bitmap, Track(lllh))
                        } else {
                            viewModel.selectRoute(null)
                        }
                    }
                }
                RouteDatabaseMenu.ElevationRefreshFromSrtm -> viewModel.refreshElevationFromSrtm(context, route)
                RouteDatabaseMenu.ElevationGmsService -> viewModel.refreshElevationFromGms(context, route)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RouteDatabaseGroupedList(
    mapPos: LatLng?,
    routeEntities: List<RouteEntity>,
    onItemAction: (RouteEntity, RouteEntityItemAction) -> Unit,
    onDeleteRegion: (String) -> Unit
) {
    val marginTopDp = TopAppBarDefaults.TopAppBarExpandedHeight.value
    val routesGrouped = routeEntities.groupBy { it.region }
    var groupExpanded by remember { mutableStateOf<String?>(null) }

    Scaffold(modifier = Modifier.padding(top = marginTopDp.dp.times(1.5f), bottom = marginTopDp.dp.times(1.4f))) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            routesGrouped.forEach { (region, entities) ->
                stickyHeader {
                    RegionHeader(
                        region = region,
                        count = entities.size,
                        isExpanded = groupExpanded == region,
                        onToggle = { groupExpanded = if (groupExpanded == region) null else region },
                        onDelete = { onDeleteRegion(region) }
                    )
                }

                if (groupExpanded == region) {
                    items(entities) { route ->
                        RouteDatabaseItem(
                            mapPos = mapPos,
                            routeItem = route,
                            onItemClick = onItemAction
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionHeader(
    region: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(color = Color.LightGray),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BadgedBox(badge = { Badge { Text("$count") } }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onToggle) {
                    Text(
                        text = region,
                        modifier = Modifier.fillMaxWidth(0.5f),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
                TextButton(onClick = onToggle) {
                    Text(
                        text = if (isExpanded) Const.UC_DROPUP_ARROW else Const.UC_DROPDOWN_ARROW,
                        textAlign = TextAlign.Center, fontSize = 20.sp
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun RouteDatabaseItem(
    mapPos: LatLng?,
    routeItem: RouteEntity,
    onItemClick: (RouteEntity, RouteEntityItemAction) -> Unit
) {
    Box(
        modifier = Modifier
            .background(color = Color.White)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onItemClick(routeItem, RouteEntityItemAction.Select) },
                    onLongPress = { onItemClick(routeItem, RouteEntityItemAction.Delete) }
                )
            }
    ) {
        Column(
            Modifier.fillMaxSize().padding(start = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val dist = mapPos?.let { SphericalUtil.computeDistanceBetween(LatLng(routeItem.latitudeStart, routeItem.longitudeStart), it) } ?: 0.0
            val heading = mapPos?.let { SphericalUtil.computeHeading(it, LatLng(routeItem.latitudeStart, routeItem.longitudeStart)) } ?: 0.0
            val direction = if (dist < 50) Const.UC_DISTANCE_ARROW else Helpers.getArrowDirection(heading, LocalContext.current)
            
            val itemText = String.format(Locale.ENGLISH, "%s %s %s", routeItem.getLine(), direction, dist.formatDistM(true))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(0.8f),
                    text = routeItem.name.removeSuffix(".gpx").removeSuffix(".jpg").removeSuffix(".kml"),
                    fontSize = 14.sp
                )
                IconButton(modifier = Modifier.weight(0.2f), onClick = { onItemClick(routeItem, RouteEntityItemAction.Map) }) {
                    Icon(Icons.Outlined.Map, null)
                }
            }
            Text(text = itemText, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            routeItem.bitmap?.let {
                Image(painter = BitmapPainter(it.asImageBitmap(), IntOffset.Zero, IntSize(it.width, it.height)), contentDescription = routeItem.name)
                Spacer(modifier = Modifier.height(4.dp))
            }
            HorizontalDivider(thickness = 2.dp, color = Color.LightGray)
        }
    }
}

@Composable
private fun AskForRouteNameFilter(
    routeEntities: List<RouteEntity>,
    onFilter: (String?, String?) -> Unit,
    onRestoreRegionsList: () -> Unit
) {
    var nameFilter by remember { mutableStateOf("") }
    Surface {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { onFilter(null, null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(text = stringResource(R.string.regions_), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(0.8f))
                IconButton(onClick = onRestoreRegionsList) {
                    Icon(Icons.Outlined.Restore, contentDescription = "Restore")
                }
            }
            RouteDatabaseRegionList(PaddingValues(0.dp), routeEntities) { onFilter(null, it) }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(0.7f),
                    value = nameFilter,
                    onValueChange = { nameFilter = it },
                    label = { Text(stringResource(R.string.name_filter)) }
                )
                TextButton(onClick = { onFilter(nameFilter, null) }) {
                    Text(Const.UC_CHECKMARK)
                }
            }
        }
    }
}

@Composable
fun RouteDatabaseRegionList(
    paddingValues: PaddingValues,
    routeEntities: List<RouteEntity>,
    selectRegion: (String?) -> Unit
) {
    val regions = routeEntities.map { it.region }.distinct().toMutableList()
    if (regions.size == 1) regions.add(0, "")

    LazyColumn(contentPadding = paddingValues) {
        items(regions) { region ->
            Box(modifier = Modifier.fillMaxWidth().background(color = Color.White).clickable { selectRegion(region) }.padding(12.dp)) {
                Text(
                    text = region,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteDatabaseMoBoSheet(routeEntity: RouteEntity, routeMenu: (action: RouteDatabaseMenu) -> Unit) {
    ModalBottomSheet(onDismissRequest = { routeMenu(RouteDatabaseMenu.Home) }) {
        Column(Modifier.padding(bottom = 16.dp)) {
            Text(text = "${routeEntity.region} ${routeEntity.name}", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            
            val buttonModifier = Modifier.weight(0.5f).fillMaxHeight()
            val buttonColors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Blue)
            
            Row(Modifier.height(IntrinsicSize.Min).padding(horizontal = 4.dp, vertical = 2.dp)) {
                Button(modifier = buttonModifier, shape = RoundedCornerShape(5.dp), colors = buttonColors, onClick = { routeMenu(RouteDatabaseMenu.Map) }, contentPadding = PaddingValues(0.dp)) {
                    Icon(painterResource(R.drawable.outline_map_24), null, modifier = Modifier.padding(horizontal = 5.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(modifier = Modifier.weight(0.8f),text = stringResource(R.string.map), color = Color.Black)
                }
                Spacer(Modifier.width(2.dp))
                Button(modifier = buttonModifier, shape = RoundedCornerShape(5.dp), colors = buttonColors, onClick = { routeMenu(RouteDatabaseMenu.RefreshPreview) }, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Outlined.Preview, null, modifier = Modifier.padding(horizontal = 5.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(modifier = Modifier.weight(0.8f),text = stringResource(R.string.refresh_route_preview), color = Color.Black)
                }
            }
            Row(Modifier.height(IntrinsicSize.Min).padding(horizontal = 4.dp, vertical = 2.dp)) {
                Button(modifier = buttonModifier, shape = RoundedCornerShape(5.dp), colors = buttonColors, onClick = { routeMenu(RouteDatabaseMenu.Chart) }, contentPadding = PaddingValues(0.dp)) {
                    Icon(painterResource(R.drawable.monitoring_24px), null, modifier = Modifier.padding(horizontal = 5.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(modifier = Modifier.weight(0.8f),text = stringResource(R.string.elevation_chart), color = Color.Black)
                }
                Spacer(Modifier.width(2.dp))
                Button(modifier = buttonModifier, shape = RoundedCornerShape(5.dp), colors = buttonColors, onClick = { routeMenu(RouteDatabaseMenu.Gradient) }, contentPadding = PaddingValues(0.dp)) {
                    Icon(painterResource(R.drawable.gradient_24px), null, modifier = Modifier.padding(horizontal = 5.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(modifier = Modifier.weight(0.8f),text = stringResource(R.string.gradient), color = Color.Black)
                }
            }
            Row(Modifier.height(IntrinsicSize.Min).padding(horizontal = 4.dp, vertical = 2.dp)) {
                Button(modifier = buttonModifier, shape = RoundedCornerShape(5.dp), colors = buttonColors, onClick = { routeMenu(RouteDatabaseMenu.ElevationGmsService) }, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Outlined.Height, null, modifier = Modifier.padding(horizontal = 5.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(modifier = Modifier.weight(0.8f),text = stringResource(R.string.gms_elevation_service), color = Color.Black)
                }
                Spacer(Modifier.width(2.dp))
                Button(modifier = buttonModifier, shape = RoundedCornerShape(5.dp), colors = buttonColors, onClick = { routeMenu(RouteDatabaseMenu.ElevationRefreshFromSrtm) }, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Outlined.Height, null, modifier = Modifier.padding(horizontal = 5.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(modifier = Modifier.weight(0.8f),text = stringResource(R.string.elevation_refresh), color = Color.Black)
                }
            }
            Row(Modifier.height(IntrinsicSize.Min).padding(horizontal = 4.dp, vertical = 2.dp)) {
                Button(modifier = buttonModifier, shape = RoundedCornerShape(5.dp), colors = buttonColors, onClick = { routeMenu(RouteDatabaseMenu.DeleteEntry) }, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Outlined.Delete, null, modifier = Modifier.padding(horizontal = 5.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(modifier = Modifier.weight(0.8f),text = stringResource(R.string.delete_route), color = Color.Black)
                }
                Spacer(Modifier.weight(0.5f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoboSnack(snackData: SnackDbRoutesData, onAction: (SnackDbRoutesAction) -> Unit) {
    ModalBottomSheet(onDismissRequest = { onAction(SnackDbRoutesAction.Nothing) }) {
        Box(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.border(2.dp, Color.LightGray, RectangleShape), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = snackData.title,
                    modifier = Modifier.weight(0.8f).padding(8.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = Color.Blue
                )
                snackData.actionText?.let { text ->
                    TextButton(onClick = { onAction(snackData.action) }, modifier = Modifier.weight(0.2f)) {
                        Text(text = text, fontWeight = FontWeight.Bold, color = Color.Blue)
                    }
                }
            }
        }
    }
}
