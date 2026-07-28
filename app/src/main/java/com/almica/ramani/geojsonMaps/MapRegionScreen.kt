package com.almica.ramani.geojsonMaps

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almica.ramani.R
import com.almica.ramani.charts.theme.Black
import com.almica.ramani.utils.getBounds
import org.maplibre.android.geometry.LatLngBounds
import timber.log.Timber
import java.util.concurrent.Executors

/**
 * 18nov2025
 * After using geojson regions (541_336_10, 541_335_10)
 * this composable is not necessary
 */
private const val logtag = "MapRegionScreen"
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapRegionScreen(
    visibleRegion: LatLngBounds,
    finished: (Int, String?) -> Unit
) {
    val context = LocalContext.current
    //val geojsonMapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
    var regionSelection by remember { mutableStateOf("") }
    var newRegion by remember { mutableStateOf(false) }
    var showRegions by remember { mutableStateOf(false) }
    var mapEntitiesVisible by remember { mutableStateOf<List<GeojsonMapEntity>?>(null) }
    var mapEntitiesAll by remember { mutableStateOf<List<GeojsonMapEntity>?>(null) }
    var confirmRequested by remember { mutableStateOf(true) }
    LaunchedEffect(mapEntitiesAll == null) {
        getAllGeojsonMaps(context) { maps ->
            mapEntitiesAll = maps
        }
        getVisibleGeojsonMaps(visibleRegion, mapEntitiesAll) { maps ->
            mapEntitiesVisible = maps
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            finished(0, null)
                            //ScreenRouter.navigateHome()
                            Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: navigateHome")
                        }
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back home")
                    }
                }, title = {
                    Text(text = stringResource(R.string.geojson_maps), fontSize = 14.sp)
                }, actions = {
                    TextButton(onClick = { showRegions = true })
                    {
                        Text(stringResource(R.string.regions_))
                    }

                    IconButton(onClick = { newRegion = true })
                    {
                        Icon(
                            Icons.Outlined.Add,
                            "newRegion",
                            modifier = Modifier
                                .padding(end = 10.dp, start = 10.dp)
                                .width(60.dp)
                                .height(60.dp)
                        )
                    }
                    AnimatedVisibility(visible = confirmRequested) {
                        TextButton(onClick = {
                            updateGeojsonMaps(mapEntitiesAll, mapEntitiesVisible, context, regionSelection)
                            finished(mapEntitiesVisible?.size ?: 0, regionSelection) })
                        {
                            Text(stringResource(R.string.confirm))
                        }
                    }
                })
        }, content = {
            val marginTopDp = TopAppBarDefaults.MediumAppBarExpandedHeight.value
            if (mapEntitiesVisible != null) {
                //Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: mapEntitiesVisible: ${mapEntitiesVisible!!.size}")
                AnimatedVisibility(visible = showRegions.not().and(newRegion.not())) {
                    MapList(PaddingValues(top = marginTopDp.dp), mapEntitiesVisible)
                }
            }
            if (showRegions)
                //MapRegionList(PaddingValues(top = marginTopDp.dp), mapEntitiesAll) {region ->
                RegionDropdownMenu (mapEntitiesAll) {region ->
                    //selectRegion(region, remove)
                    if (region != null) {
                        regionSelection = region
                        mapEntitiesVisible?.forEach { mapEntity ->
                            mapEntity.path = region
                        }
                        showRegions = false
                        confirmRequested = true
                    } else
                        showRegions = false
                }
            AnimatedVisibility(visible = newRegion) {
                Row(
                    modifier = Modifier
                        .padding(top = marginTopDp.dp)
                        .background(Color.White)
                ) {
                    IconButton(
                        modifier = Modifier.align(alignment = Alignment.CenterVertically),
                        //.border(border = BorderStroke(2.dp, Color.LightGray)),
                        onClick = {
                            newRegion = false
                        }
                    ) {
                        Icon(//modifier = Modifier.align(alignment = Alignment.CenterVertically),
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close"
                        )
                    }
                    var regionChanged by remember { mutableStateOf(regionSelection)}
                    OutlinedTextField(
                        value = regionChanged,
                        onValueChange = { regionChanged = it },
                        label = { Text(stringResource(R.string.region)) },
                        modifier = Modifier
                            .padding(start = 6.dp, end = 6.dp)
                            .fillMaxWidth(0.8f)
                    )
                    IconButton(
                        modifier = Modifier.align(alignment = Alignment.CenterVertically),
                        //.border(border = BorderStroke(2.dp, Color.LightGray)),
                        onClick = {
                            newRegion = false
                            regionSelection = regionChanged
                            mapEntitiesVisible?.forEach { mapEntity ->
                                mapEntity.path = regionSelection
                            }
                            confirmRequested = true
                        }
                    ) {
                        Icon(//modifier = Modifier.align(alignment = Alignment.CenterVertically),
                            imageVector = Icons.Outlined.Done,
                            contentDescription = "Confirm"
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun RegionDropdownMenu(mapEntities: List<GeojsonMapEntity>?, selectRegion: (String?) -> Unit) {
    val regions = createRegionArray(mapEntities)
    Surface(
        Modifier
            .fillMaxWidth()
            .padding(top = 250.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.align(Alignment.CenterStart)) {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { selectRegion(null) }
                ) {
                    regions.forEach { region ->
                        if (region != null) {
                            DropdownMenuItem(
                                { Text(text = region, color = Black) },
                                onClick = { selectRegion(region) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapRegionList(
    paddingValues: PaddingValues,
    mapEntities: List<GeojsonMapEntity>?,
    selectRegion: (String?) -> Unit
) {
    Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: mapEntities ${mapEntities?.size}")
    val regions = createRegionArray(mapEntities)
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
                                Timber.i("${Thread.currentThread().getStackTrace()[2].lineNumber}: onTap $region")
                                selectRegion(region)
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


@Composable
private fun MapList(
    paddingValues: PaddingValues,
    mapEntities: List<GeojsonMapEntity>?
) {
    Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: mapEntities ${mapEntities?.size}")
    val maps = arrayListOf<String>()
    mapEntities?.forEach { mapEntity ->
        maps.add("${mapEntity.name} - ${mapEntity.path}")
    }
    LazyColumn(
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(maps) { mapName ->
            Box(
                modifier = Modifier
                    .background(color = Color.White)
                    //.clickable { selectRegion(region) }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                Timber.i("${Thread.currentThread().getStackTrace()[2].lineNumber}: onTap $mapName")
                            }
                        )
                    }
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Text(
                            text = mapName,
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


fun createRegionArray(mapEntities: List<GeojsonMapEntity>?) : Array<String?> {
    val regionList = ArrayList<String>()
    if (mapEntities != null) {
        for (mapEntity in mapEntities) {
            if (!regionList.contains(mapEntity.path))
                regionList.add(mapEntity.path)
        }
    }
    if (regionList.size == 1)
        regionList.add(0, "")
    var regionArr = arrayOfNulls<String>(regionList.size)
    regionArr = regionList.toArray(regionArr)
    return regionArr
}

private fun updateGeojsonMaps(allMaps: List<GeojsonMapEntity>?, visibleMaps: List<GeojsonMapEntity>?, context: Context, region: String) {
    val namesForUpdate = arrayListOf<String>()
    visibleMaps?.forEach { geojsonMapEntity ->
        namesForUpdate.add(geojsonMapEntity.name)
    }
    val geojsonMapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
    Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: $region visibleMaps: ${visibleMaps?.size}")
    allMaps?.forEach { geojsonMapEntity ->
        if (namesForUpdate.contains(geojsonMapEntity.name))
            geojsonMapRepository.updateGeojsonMapStatus(true, geojsonMapEntity.name, region) {}
        else
            geojsonMapRepository.updateGeojsonMapStatus(false, geojsonMapEntity.name) {}
    }
}

private fun getVisibleGeojsonMaps(
    visibleRegion: LatLngBounds, allMaps: List<GeojsonMapEntity>?,
    finished: (ArrayList<GeojsonMapEntity>) -> Unit
) {
    val visibleMaps = ArrayList<GeojsonMapEntity>()
    allMaps?.forEach { geojsonMapEntity ->
        val intersectBounds = visibleRegion.intersect(geojsonMapEntity.getBounds())
        val state = intersectBounds != null && intersectBounds.isEmptySpan.not()
        if (state) visibleMaps.add(geojsonMapEntity)
    }
    Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: enabled maps: ${visibleMaps.size}")
    finished(visibleMaps)
}

private fun getAllGeojsonMaps(context: Context, finished: (List<GeojsonMapEntity>) -> Unit) {
    val geojsonMapRepository = GeojsonMapRepository.getInstance(context, Executors.newSingleThreadExecutor())
    geojsonMapRepository.getAllSimple(false) {geojsonMapEntities ->
        Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}: geojsonMapEntities: ${geojsonMapEntities.size}")
        finished(geojsonMapEntities)
    }
}