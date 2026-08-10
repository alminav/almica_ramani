package com.almica.ramani

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.Const.Companion.EXTRA_ACTIVITY
import com.almica.ramani.Const.Companion.EXTRA_LATLNG
import com.almica.ramani.Helpers.Companion.saveLocations
import com.almica.ramani.charts.GraphDataPoints
import com.almica.ramani.charts.PlotResult
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.googlemaps.MapUtils.gmsElevationService
import com.almica.ramani.googlemaps.MoboLocationsMonitor
import com.almica.ramani.routes.RouteDialogMode
import com.almica.ramani.routes.RouteFileSaveMoBoSheet
import com.almica.ramani.ui.theme.MapsComposeSampleTheme
import com.almica.ramani.utils.BackPressHandler
import com.almica.ramani.utils.TimePagerDialog
import com.almica.ramani.utils.addPoiDao
import kotlinx.coroutines.delay
import org.maplibre.android.geometry.LatLng
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds

class LauncherActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    @SuppressLint("LocalContextGetResourceValueCall", "MutableCollectionMutableState")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MapsComposeSampleTheme {
                LauncherScreen(viewModel)
            }
        }
    }

    @Composable
    private fun LauncherScreen(viewModel: LauncherViewModel) {
        val context = LocalContext.current
        val prefs = remember { getDefaultSharedPreferences(context) }
        val uiState by viewModel.uiState.collectAsState()

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                handleActivityResult(result)
            }
        }

        BackPressHandler { finish() }

        LauncherContent(
            uiState = uiState,
            onActivityClick = { activityKClass, stringExtras, doubleExtras, maptypeKey ->
                prefs.edit { putInt(Const.PREF_MAPTYPE_KEY, maptypeKey.ordinal) }

                val intent = Intent(context, activityKClass.java).apply {
                    stringExtras.forEach { putExtra(it.first, it.second) }
                    doubleExtras.forEach { putExtra(it.first, it.second) }
                    putExtra(Const.EXTRA_LATITUDE, uiState.resultLatLng.first)
                    putExtra(Const.EXTRA_LONGITUDE, uiState.resultLatLng.second)
                    putExtra(Const.EXTRA_ROUTE_DIALOG_MODE, RouteDialogMode.Admin.ordinal)
                }
                launcher.launch(intent)
            },
            viewModel = viewModel
        )
    }

    private fun handleActivityResult(result: ActivityResult) {
        val data = result.data ?: return
        data.getStringExtra(EXTRA_ACTIVITY)?.let { Timber.i("$EXTRA_ACTIVITY: $it") }
        Timber.i("data: $data")
        handleCoordinatesResult(data)
        handleImportResults(data)
    }

    private fun handleCoordinatesResult(data: Intent) {
        val latLng = data.getDoubleArrayExtra(EXTRA_LATLNG)
        Timber.i("latLng: ${latLng.contentToString()}")
        val placesLat = data.getDoubleExtra(Const.PLACE_LATITUDE, -1.0)
        val placesLon = data.getDoubleExtra(Const.PLACE_LONGITUDE, -1.0)
        viewModel.refreshAll() // logCount update 25jul2026
        val (lat, lon) = when {
            latLng != null -> latLng[0] to latLng[1]
            placesLat != -1.0 && placesLon != -1.0 -> placesLat to placesLon
            else -> return
        }

        viewModel.updateCoordinates(lat, lon)
    }

    private fun handleImportResults(data: Intent) {
        val hgtImport = data.getBooleanExtra(Const.SETRESULT_IMPORT_HGT, false)
        val geojsonType = data.getStringExtra(Const.SETRESULT_IMPORT_GEOJSON)

        val fileType = when {
            hgtImport -> FileType.Hgt.name
            geojsonType != null -> geojsonType
            else -> return
        }

        val intent = Intent(this, FileImportActivity::class.java)
            .setAction(getString(R.string.import_title))
            .putExtra(Const.EXTRA_FILETYPE, fileType)
        startActivity(intent)
    }

    @SuppressLint("MutableCollectionMutableState")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LauncherContent(
        uiState: LauncherUiState,
        viewModel: LauncherViewModel,
        onActivityClick: (KClass<out ComponentActivity>, ArrayList<Pair<String, String>>, ArrayList<Pair<String, Double>>, MaptypeKey) -> Unit
    ) {
        val context = LocalContext.current

        LauncherContentUI(
            uiState = uiState,
            onActivityClick = onActivityClick,
            onMonitor = { viewModel.onMonitorClicked() },
            onSaveAsRoute = { viewModel.onSaveAsRouteClicked() },
            onSaveLocations = { dateMilliSeconds, onResult ->
                saveLocations(context, dateMilliSeconds, onResult)
            },
            onAddPoi = { name, latLng, category, onResult ->
                val locationsParm = "${latLng.latitude},${latLng.longitude}"
                gmsElevationService(context, locationsParm) { lllh0 ->
                    val h = lllh0[0].altitude
                    addPoiDao(context, name, com.google.android.gms.maps.model.LatLng(latLng.latitude, latLng.longitude), h, category) { _ ->
                        onResult(name)
                    }
                }
            },
            onRefreshLogCount = { viewModel.refreshAll() },
            onDismissLocationStatistic = { viewModel.dismissLocationStatistic() },
            onDismissRouteSavingScreen = { viewModel.dismissRouteSavingScreen() },
            mvtChange = { viewModel.refreshAll() },
            onToggleTracking = {
                GpsRepository.getInstance().updateTrackingEnabled(it)
                viewModel.refreshAll()
                Timber.i("onToggleTracking: $it")
            }
        )
    }
    data class LauncherSnackbarData(
        val name: String?,
        val category: String?,
        val actionText: String?,
        val actionData: String?
    )

    enum class LauncherSnackbarAction {
        Nothing
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherContentUI(
    uiState: LauncherUiState,
    onActivityClick: (KClass<out ComponentActivity>, ArrayList<Pair<String, String>>, ArrayList<Pair<String, Double>>, MaptypeKey) -> Unit,
    onMonitor: () -> Unit,
    onSaveAsRoute: () -> Unit,
    onSaveLocations: (Long, (String) -> Unit) -> Unit,
    onAddPoi: (String, LatLng, String, (String) -> Unit) -> Unit,
    onRefreshLogCount: () -> Unit,
    onDismissLocationStatistic: () -> Unit,
    onDismissRouteSavingScreen: () -> Unit,
    mvtChange: () -> Unit,
    onToggleTracking: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val resources = context.resources
    val snackDelayDefault = 5000L
    var snackDelay by remember { mutableLongStateOf(snackDelayDefault) }
    var snackbarData by remember { mutableStateOf<LauncherActivity.LauncherSnackbarData?>(null) }

    var showLocationsMenu by remember { mutableStateOf(false) }
    var showLocationsSnapshot by remember { mutableStateOf(false) }
    var showLocationsTimeDialog by remember { mutableStateOf(false) }
    var showPoiCatDialog: ResultData? by remember { mutableStateOf(null) }

    LaunchedEffect(key1 = snackbarData) {
        if (snackDelay > 0) {
            delay(snackDelay.milliseconds)
            snackbarData = null
        }
    }
    val appIcon = Helpers.getBitmapFromVectorDrawable(context, R.mipmap.ic_launcher_round)
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        topBar = {
            LauncherTopBar(appIcon)
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val rasterDescription = uiState.rasterTilesPrefSet?.takeIf { it.isNotEmpty() }?.let { set ->
                val firstName = File(set.first() ?: "").name.replace(Const.MBTILES_EXT, "")
                if (set.size > 1) "$firstName (+${set.size - 1})" else firstName
            }

            RamaniNavHost(
                uiState.mvtName,
                rasterDescription,
                uiState.geojsonDescription,
                uiState.ghFileName,
                uiState.geojsonFolderDescription,
                uiState.resultRouteFolderName,
                uiState.firstLocationDate,
                uiState.lastLocationDate,
                uiState.lastLocationCoords,
                uiState.logCount,
                isTrackingEnabled = uiState.isTrackingEnabled,
                onActivityClick = onActivityClick,
                showLocationsMenu = { showLocationsMenu = true },
                onToggleTracking = onToggleTracking
            )
        }

        snackbarData?.let {
            LauncherMoboSnackStatic(it) {
                snackbarData = null
            }
        }

        showPoiCatDialog?.let { sheetData ->
            PoiCatDialog(sheetData.name.toString()) { name, category ->
                if (sheetData.latLng != null) {
                    if (category == null) {
                        showPoiCatDialog = null
                    } else {
                        onAddPoi(name, sheetData.latLng, category) { addedName ->
                            showPoiCatDialog = null
                            snackDelay = snackDelayDefault
                            snackbarData = LauncherActivity.LauncherSnackbarData(
                                resources.getString(R.string.added_to_database, addedName),
                                null, null, null
                            )
                        }
                    }
                }
            }
        }

        //Timber.i("logCount: ${uiState.logCount}")
        if (showLocationsSnapshot) {
            MbsLocationsSnapshot(
                finished = {
                    showLocationsSnapshot = false
                    snackbarData = null
                    mvtChange()
                },
                showSaveResult = { msg ->
                    showLocationsSnapshot = false
                    snackDelay = snackDelayDefault
                    snackbarData = LauncherActivity.LauncherSnackbarData(msg, null, null, null)
                    mvtChange()
                },
                refreshLogCount = { onRefreshLogCount() }
            )
        }

        if (showLocationsMenu) {
            LocationsBottomMenu { msg, action ->
                when (action) {
                    LocationsAction.Close -> showLocationsMenu = false
                    LocationsAction.Save -> {
                        snackDelay = snackDelayDefault
                        snackbarData = msg?.let { LauncherActivity.LauncherSnackbarData(it, null, null, null) }
                        showLocationsMenu = false
                    }
                    LocationsAction.Monitor -> {
                        showLocationsMenu = false
                        onMonitor()
                    }
                    LocationsAction.SnapShot -> {
                        showLocationsMenu = false
                        if (uiState.logCount > 0) {
                            showLocationsSnapshot = true
                            snackDelay = 0L
                            snackbarData = LauncherActivity.LauncherSnackbarData(
                                resources.getString(R.string.take_snapshot_started),
                                null, null, null
                            )
                        }
                    }
                    LocationsAction.Reset -> {
                        onRefreshLogCount()
                        snackDelay = snackDelayDefault
                        snackbarData = msg?.let { LauncherActivity.LauncherSnackbarData(it, null, null, null) }
                        showLocationsMenu = false
                    }
                    LocationsAction.DeleteTracks -> {
                        snackDelay = snackDelayDefault
                        snackbarData = msg?.let { LauncherActivity.LauncherSnackbarData(it, null, null, null) }
                        showLocationsMenu = false
                    }
                    LocationsAction.SaveAsRoute -> {
                        showLocationsMenu = false
                        onSaveAsRoute()
                    }
                    LocationsAction.StartTime -> {
                        showLocationsMenu = false
                        showLocationsTimeDialog = true
                    }
                }
            }
        }

        if (showLocationsTimeDialog) {
            TimePagerDialog(true, onConfirm = { dateMilliSeconds, timeTag ->
                showLocationsTimeDialog = false
                onSaveLocations(dateMilliSeconds) { msg ->
                    snackDelay = snackDelayDefault
                    snackbarData = LauncherActivity.LauncherSnackbarData("$msg startTime:$timeTag", null, null, null)
                }
            }) {
                showLocationsTimeDialog = false
            }
        }

        if (uiState.showLocationStatistic) {
            MoboLocationsMonitor(null, uiState.plotResult) {
                onDismissLocationStatistic()
            }
        }

        if (uiState.showRouteSavingScreen && !uiState.locationsLllh.isNullOrEmpty()) {
            val platformLocale = LocalLocale.current.platformLocale
            val timeFormat = SimpleDateFormat(Const.TIME_PATTERN_LONG_YEAR, platformLocale)
            val routeNameTemplate = String.format(
                platformLocale, "%s_%s", "route",
                timeFormat.format(uiState.locationsLllh.last().time)
            )
            RouteFileSaveMoBoSheet(routeNameTemplate) { targetFileName, targetRouteFolder ->
                onDismissRouteSavingScreen()
                if (targetRouteFolder != null) {
                    val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
                    val routeFolder = File(rootRouteFolder, targetRouteFolder.first)
                    val routeFile = File(routeFolder, "$targetFileName${Const.KML_EXT}")
                    val result = Helpers.writeLllh2KmlFile(ArrayList(uiState.locationsLllh), routeFile.path)
                    snackDelay = snackDelayDefault
                    snackbarData = LauncherActivity.LauncherSnackbarData(
                        "${routeFile.name} ${resources.getString(R.string.route_save_result)}: " +
                                if (result) resources.getString(R.string.ok) else resources.getString(R.string.error),
                        null, null, null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherTopBar(appIcon: android.graphics.Bitmap?) {
    CenterAlignedTopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                appIcon?.let {
                    Icon(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "App Icon",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.launcher_activity_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LauncherMoboSnackStatic(
    launcherSnackbarData: LauncherActivity.LauncherSnackbarData,
    finished: (action: LauncherActivity.LauncherSnackbarAction) -> Unit
) {
    ModalBottomSheet(onDismissRequest = { finished(LauncherActivity.LauncherSnackbarAction.Nothing) }) {
        Box(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) {
            Row(
                modifier = Modifier.border(
                    width = 2.dp,
                    color = Color.LightGray,
                    shape = RectangleShape
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                launcherSnackbarData.name?.let {
                    Text(
                        text = it,
                        Modifier
                            .weight(0.8f)
                            .padding(top = 8.dp, bottom = 8.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Blue
                    )
                }
                launcherSnackbarData.category.let { text ->
                    TextButton(onClick = {
                        finished(LauncherActivity.LauncherSnackbarAction.Nothing)
                    }, modifier = Modifier.weight(0.2f)) {
                        if (text != null) {
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
}

@Preview(showBackground = true)
@Composable
fun LauncherContentUIPreview() {
    MapsComposeSampleTheme {
        LauncherContentUI(
            uiState = LauncherUiState(
                rasterTilesPrefSet = setOf("sample_raster"),
                geojsonFolderDescription = "Sample GeoJSON Folder",
                geojsonDescription = "Sample GeoJSON Description",
                mvtName = "Sample.MVT",
                ghFileName = "sample.gh",
                resultRouteFolderName = "home",
                resultLatLng = Pair(-1.2833, 36.8167),
                logCount = 42,
                lastLocationDate = "2026-04-12 16:20",
                firstLocationDate = "2026-04-12 10:00",
                showLocationStatistic = false,
                showRouteSavingScreen = false,
                plotResult = PlotResult(
                    lines = GraphDataPoints(arrayListOf(), arrayListOf(), arrayListOf(), arrayListOf()),
                    distKM = 0F
                ),
                locationsLllh = listOf(
                    LatLngH(
                        -1.2833,
                        36.8167,
                        1600.0,
                        time = System.currentTimeMillis()
                    )
                )
            ),
            onActivityClick = { _, _, _, _ -> },
            onMonitor = {},
            onSaveAsRoute = {},
            onSaveLocations = { _, _ -> },
            onAddPoi = { _, _, _, _ -> },
            onRefreshLogCount = { },
            onDismissLocationStatistic = {},
            onDismissRouteSavingScreen = {},
            mvtChange = {},
            onToggleTracking = {}
        )
    }
}

data class ResultData(val name: String?, val latLng: LatLng?, val category: String?)