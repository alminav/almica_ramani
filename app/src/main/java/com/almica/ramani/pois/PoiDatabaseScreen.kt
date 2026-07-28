package com.almica.ramani.pois

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.Note
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Height
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almica.ramani.Helpers
import com.almica.ramani.R
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.BackPressHandler
import com.almica.ramani.utils.format
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import timber.log.Timber
import java.util.Locale
import java.util.UUID

enum class PoiNavigationItem {
    ByName,
    ByDistance,
    ByCategory,
    Categories,
    NameFilter
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiDatabaseScreen(
    marginTopDp: Float,
    mapPos: LatLng?,
    viewModel: PoiViewModel = viewModel(),
    selectPoi: (PoiEntity?, PoiItemAction) -> Unit
) {
    val resources = LocalResources.current
    val catMap = Helpers.getPoiDrawableMap(LocalContext.current)

    val poiEntitiesSorted by viewModel.poiEntities.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val snackPoiData by viewModel.snackPoiData.collectAsStateWithLifecycle()

    var showCategoryList by remember { mutableStateOf(false) }
    var askForNameFilter by remember { mutableStateOf(false) }

    LaunchedEffect(mapPos) {
        viewModel.setMapPosition(mapPos)
    }

    BackPressHandler {
        selectPoi(null, PoiItemAction.Map)
    }

    PoiDatabaseScreenLayout(
        marginTopDp = marginTopDp,
        mapPos = mapPos,
        categories = categories,
        poiEntitiesSorted = poiEntitiesSorted,
        sortOrder = sortOrder,
        snackPoiData = snackPoiData,
        showCategoryList = showCategoryList,
        askForNameFilter = askForNameFilter,
        catMap = catMap,
        onPoiAction = { poi, action ->
            if (poi != null) {
                when (action) {
                    PoiItemAction.Map, PoiItemAction.Stop -> {
                        selectPoi(poi, action)
                    }
                    PoiItemAction.Delete -> {
                        viewModel.deletePoi(poi.id)
                        selectPoi(poi, action)
                    }
                    PoiItemAction.ElevationRefresh -> {
                        viewModel.refreshElevation(poi, resources.getString(R.string._srtm_refresh_done, "%s"))
                    }
                }
            }
        },
        onNavigationItemSelected = { select ->
            showCategoryList = false
            askForNameFilter = false
            when (select) {
                PoiNavigationItem.ByName -> viewModel.setSortOrder(PoiSortOrder.ByName)
                PoiNavigationItem.ByDistance -> viewModel.setSortOrder(PoiSortOrder.ByDistance)
                PoiNavigationItem.ByCategory -> viewModel.setSortOrder(PoiSortOrder.ByCategory)
                PoiNavigationItem.Categories -> showCategoryList = true
                PoiNavigationItem.NameFilter -> askForNameFilter = true
            }
        },
        onGoBack = { selectPoi(null, PoiItemAction.Map) },
        onClearAll = {
            viewModel.showDeleteAllSnack(
                resources.getString(R.string.delete_all_pois),
                resources.getString(android.R.string.ok)
            )
        },
        onSnackAction = { action ->
            when (action) {
                SnackPoiAction.Nothing -> viewModel.clearSnack()
                SnackPoiAction.DeleteAll -> {
                    viewModel.deleteAll()
                    viewModel.clearSnack()
                }
            }
        },
        onCategorySelected = { category ->
            showCategoryList = false
            viewModel.setCategory(category)
        },
        onNameFilterApplied = { filter ->
            askForNameFilter = false
            viewModel.setSearchQuery(filter)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiDatabaseScreenLayout(
    marginTopDp: Float,
    mapPos: LatLng?,
    categories: List<String>,
    poiEntitiesSorted: List<PoiEntity>,
    sortOrder: PoiSortOrder,
    snackPoiData: SnackPoiData?,
    showCategoryList: Boolean,
    askForNameFilter: Boolean,
    catMap: Map<String, Pair<Int, Int>>,
    onPoiAction: (PoiEntity?, PoiItemAction) -> Unit,
    onNavigationItemSelected: (PoiNavigationItem) -> Unit,
    onGoBack: () -> Unit,
    onClearAll: () -> Unit,
    onSnackAction: (SnackPoiAction) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onNameFilterApplied: (String?) -> Unit
) {
    Scaffold(
        //modifier = Modifier.padding(top = marginTopDp.dp, bottom = 48.dp),
        bottomBar = {
            NavigationBarPois(onNavigationItemSelected)
        },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onGoBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back home"
                        )
                    }
                },
                title = { Text(text = stringResource(R.string.poi_database)) },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        IconButton(onClick = onClearAll) {
                            Icon(Icons.Outlined.ClearAll, contentDescription = null)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        PoiDatabaseList(paddingValues, mapPos, poiEntitiesSorted, catMap, sortOrder, onPoiAction)

        if (showCategoryList) {
            CategoryList(categories, onCategorySelected)
        }
        if (askForNameFilter) {
            AskForPoiNameFilter(onNameFilterApplied)
        }
        snackPoiData?.let {
            MoboPoiSnack(it, onSnackAction)
        }
    }
}




@Composable
fun PoiDatabaseGroupedList(
    paddingValues: PaddingValues,
    mapPos: LatLng?,
    poiEntitiesSorted: List<PoiEntity>,
    catMap: Map<String, Pair<Int, Int>>,
    selectPoi: (PoiEntity?, PoiItemAction) -> Unit
) {
    val grouped = poiEntitiesSorted.groupBy { it.category }
    LazyColumn(contentPadding = paddingValues) {
        grouped.forEach { (category, pois) ->
            item {
                Text(
                    text = category,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(8.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(pois) { poi ->
                PoiItem(mapPos, poi, catMap, selectPoi)
            }
        }
    }
}

@Composable
fun PoiDatabaseList(
    paddingValues: PaddingValues,
    mapPos: LatLng?,
    poiEntitiesSorted: List<PoiEntity>,
    catMap: Map<String, Pair<Int, Int>>,
    sortOrder: PoiSortOrder,
    selectPoi: (PoiEntity?, PoiItemAction) -> Unit
) {
    if (sortOrder == PoiSortOrder.ByCategory) {
        PoiDatabaseGroupedList(paddingValues, mapPos, poiEntitiesSorted, catMap, selectPoi)
    } else {
        LazyColumn(contentPadding = paddingValues) {
            items(poiEntitiesSorted) { poi ->
                PoiItem(mapPos, poi, catMap, selectPoi)
            }
        }
    }
}

@Composable
fun PoiItem(
    mapPos: LatLng?,
    poi: PoiEntity,
    catMap: Map<String, Pair<Int, Int>>,
    selectPoi: (PoiEntity, PoiItemAction) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { selectPoi(poi, PoiItemAction.Map) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            catMap[poi.category]?.first?.let { iconRes ->
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(text = poi.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${poi.latitude.format(4)}, ${poi.longitude.format(4)} ${Const.UC_ELE_ARROW}${poi.altitude.format(0)}m",
                    style = MaterialTheme.typography.bodySmall
                )
                mapPos?.let {
                    val distance = SphericalUtil.computeDistanceBetween(
                        LatLng(poi.latitude, poi.longitude),
                        it
                    )
                    val heading = mapPos.let {
                        SphericalUtil.computeHeading(
                            it, LatLng(
                                poi.latitude,
                                poi.longitude
                            )
                        )
                    }
                    var textHeading =
                        heading.let { Helpers.getArrowDirection(it, LocalContext.current) }
                    if (distance < 50)
                        textHeading = Const.UC_DISTANCE_ARROW
                    Text(
                        text = "$textHeading ${String.format(Locale.ENGLISH, "%.2f km", distance / 1000)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = { selectPoi(poi, PoiItemAction.ElevationRefresh) }) {
                Icon(Icons.Outlined.Height, contentDescription = "Refresh Elevation")
            }
            IconButton(onClick = { selectPoi(poi, PoiItemAction.Delete) }) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete POI")
            }
            IconButton(onClick = {
                Timber.i("stop ${poi.name}")
                selectPoi(poi, PoiItemAction.Stop)
            }) {
                Icon(
                    painterResource(R.drawable.circle_filled_red_24px),
                    null,
                    tint = Color.Unspecified
                )
            }
        }
    }
}

@Composable
fun NavigationBarPois(onSelect: (PoiNavigationItem) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        NavigationBarItem(
            selected = false,
            onClick = { onSelect(PoiNavigationItem.ByName) },
            icon = { Icon(Icons.AutoMirrored.Outlined.Note, contentDescription = null) },
            label = { Text("Name") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { onSelect(PoiNavigationItem.ByDistance) },
            icon = { Icon(Icons.AutoMirrored.Outlined.CompareArrows, contentDescription = null) },
            label = { Text("Distance") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { onSelect(PoiNavigationItem.ByCategory) },
            icon = { Icon(Icons.Outlined.Description, contentDescription = null) },
            label = { Text("Category") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { onSelect(PoiNavigationItem.Categories) },
            icon = { Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null) },
            label = { Text("Categories") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { onSelect(PoiNavigationItem.NameFilter) },
            icon = { Icon(Icons.Outlined.FilterAlt, contentDescription = null) },
            label = { Text("Filter") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryList(
    categories: List<String>,
    onCategorySelected: (String?) -> Unit
) {
    ModalBottomSheet(onDismissRequest = { onCategorySelected(null) }) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            items(categories) { category ->
                Text(
                    text = category,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategorySelected(category) }
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskForPoiNameFilter(onFilter: (String?) -> Unit) {
    var text by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = { onFilter(null) }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Filter by name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onFilter(text) }) {
                Text("Apply")
            }
        }
    }
}

// Redundant declarations removed.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoboPoiSnack(snackPoiData: SnackPoiData, finished: (action: SnackPoiAction) -> Unit) {
    Timber.i("snackPoiData: $snackPoiData")
    ModalBottomSheet(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 3.dp, start = 3.dp, end = 3.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = snackPoiData.title,
                    modifier = Modifier
                        .weight(0.8f)
                        .padding(top = 8.dp, bottom = 8.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Blue
                )
                snackPoiData.actionText?.let { text ->
                    TextButton(
                        onClick = { finished(snackPoiData.action) },
                        modifier = Modifier.weight(0.2f)
                    ) {
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

@Preview(showBackground = true)
@Composable
fun PoiDatabaseScreenPreview() {
    val samplePois = listOf(
        PoiEntity(UUID.randomUUID(), com.almica.ramani.Const.HOME, 45.0, 9.0, 100.0,
            com.almica.ramani.Const.HOME),
        PoiEntity(UUID.randomUUID(), "Work", 45.1, 9.1, 110.0, "Office"),
        PoiEntity(UUID.randomUUID(), "Park", 45.2, 9.2, 120.0, "Nature")
    )
    val catMap = mapOf(
        com.almica.ramani.Const.HOME to Pair(R.drawable.mx_village, R.string.locality),
        "Office" to Pair(R.drawable.s_street_small, R.string.street),
        "Nature" to Pair(R.drawable.s_food_small, R.string.restaurant)
    )
    RamaniTheme {
        PoiDatabaseScreenLayout(
            marginTopDp = 0f,
            mapPos = LatLng(45.0, 9.0),
            categories = listOf("Locality", "Office", "Nature"),
            poiEntitiesSorted = samplePois,
            sortOrder = PoiSortOrder.ByName,
            snackPoiData = null,
            showCategoryList = false,
            askForNameFilter = false,
            catMap = catMap,
            onPoiAction = { _, _ -> },
            onNavigationItemSelected = {},
            onGoBack = {},
            onClearAll = {},
            onSnackAction = {},
            onCategorySelected = {},
            onNameFilterApplied = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PoiItemPreview() {
    RamaniTheme {
        PoiItem(
            mapPos = LatLng(45.0, 9.0),
            poi = PoiEntity(UUID.randomUUID(), "Sample POI", 45.001, 9.001, 100.0,
                com.almica.ramani.Const.HOME),
            catMap = mapOf(com.almica.ramani.Const.HOME to Pair(R.drawable.mx_village, R.string.locality)),
            selectPoi = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NavigationBarPoisPreview() {
    RamaniTheme {
        NavigationBarPois(onSelect = {})
    }
}

@Preview(showBackground = true)
@Composable
fun CategoryListPreview() {
    RamaniTheme {
        CategoryList(
            categories = listOf("Category A", "Category B"),
            onCategorySelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AskForPoiNameFilterPreview() {
    RamaniTheme {
        AskForPoiNameFilter(onFilter = {})
    }
}

@Preview(showBackground = true)
@Composable
fun MoboPoiSnackPreview() {
    RamaniTheme {
        MoboPoiSnack(
            snackPoiData = SnackPoiData(
                title = "Elevation refresh done",
                action = SnackPoiAction.Nothing,
                actionText = "OK",
                actionData = null
            ),
            finished = {}
        )
    }
}
