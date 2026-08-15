package com.almica.ramani

import android.app.Activity.RESULT_OK
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almica.ramani.googlemaps.MapUtils.gmsElevationService
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.addPoiDao
import com.strongtogether.googlemapsjetpackcompose.screens.GoogleMapSearchScreen
import com.strongtogether.googlemapsjetpackcompose.viewmodel.MapViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds
import com.google.android.gms.maps.model.LatLng as GmsLatLng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoCoderLauncher(
    latLng: GmsLatLng?,
    showInMap: (name: String?, category: String?, LatLng?) -> Unit
) {
    Timber.i("GeoCoderLauncher")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: MapViewModel = viewModel()
    var showPoiCatDialog: ResultData? by remember { mutableStateOf(null) }
    var placeAddedSheetData by remember { mutableStateOf<PlaceAddedSheetData?>(null) }

    // Auto-navigate back to the map after a POI is successfully added to the database.
    // This gives the user 5 seconds to see the confirmation sheet before returning.
    LaunchedEffect(key1 = placeAddedSheetData) {
        placeAddedSheetData?.actionData?.let { actionData ->
            Timber.i("LaunchedEffect: ${actionData.name}")
            delay(5000.milliseconds)
            showInMap(actionData.name, actionData.category, actionData.latLng)
            placeAddedSheetData = null
        }
    }

    BackHandler {
        Timber.i("Back Press intercepted")
        when {
            showPoiCatDialog != null -> showPoiCatDialog = null
            placeAddedSheetData != null -> placeAddedSheetData = null
            else -> showInMap(null, null, null)
        }
    }

    var resultLatLng by remember {
        mutableStateOf(
            LatLng(latLng?.latitude ?: -1.0, latLng?.longitude ?: -1.0)
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val placesName = data?.getStringExtra(Const.PLACE_NAME)
            val placesLat = data?.getDoubleExtra(Const.PLACE_LATITUDE, -1.0) ?: -1.0
            val placesLon = data?.getDoubleExtra(Const.PLACE_LONGITUDE, -1.0) ?: -1.0

            if (placesName != null) {
                Timber.i("placesName = $placesName $placesLat $placesLon")
                resultLatLng = LatLng(placesLat, placesLon)
                showPoiCatDialog = ResultData(placesName, resultLatLng, null)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { showInMap(null, null, null) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back_home)
                        )
                    }
                },
                title = { Text(text = stringResource(R.string.geocoder)) }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            GoogleMapSearchScreen(viewModel, resultLatLng.latitude, resultLatLng.longitude) { name, latLng ->
                Timber.i("name: $name latLng: $latLng")
                latLng?.let {
                    resultLatLng = LatLng(it.latitude, it.longitude)
                    showPoiCatDialog = ResultData(name, resultLatLng, null)
                }
            }
            placeAddedSheetData?.let { data ->
                PlaceAddedBottomSheet(data) {
                    placeAddedSheetData = null
                }
            }

            showPoiCatDialog?.let { dialogData ->
                Timber.i("name: ${dialogData.name}")
                PoiCatDialog(dialogData.name.toString()) { name, category ->
                    Timber.i("$name $category")
                    val dialogLatLng = dialogData.latLng
                    if (dialogLatLng != null) {
                        if (category == null) {
                            showInMap(null, null, dialogLatLng)
                            showPoiCatDialog = null
                        } else {
                            val locationsParam = "${dialogLatLng.latitude},${dialogLatLng.longitude}"
                            scope.launch {
                                val lllh0 = gmsElevationService(context, locationsParam)
                                val h = lllh0.firstOrNull()?.altitude ?: 0.0
                                val addedToDatabaseMsg =
                                    context.resources.getString(R.string.added_to_database, name)
                                addPoiDao(
                                    context,
                                    name,
                                    GmsLatLng(dialogLatLng.latitude, dialogLatLng.longitude),
                                    h,
                                    category
                                ) { _ ->
                                    placeAddedSheetData = PlaceAddedSheetData(
                                        name = addedToDatabaseMsg,
                                        category = category,
                                        actionData = dialogData
                                    )
                                    showPoiCatDialog = null
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class PlaceAddedSheetData(
    val name: String?,
    val category: String?,
    val actionData: ResultData?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceAddedBottomSheet(
    data: PlaceAddedSheetData,
    onDismiss: () -> Unit
) {
    Timber.i("PlaceAddedBottomSheet ${data.name} ${data.category}")
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            data.name?.let {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            data.category?.let { category ->
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GeoCoderLauncherPreview() {
    RamaniTheme {
        GeoCoderLauncher(
            latLng = GmsLatLng(-1.286389, 36.817223),
            showInMap = { _, _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceAddedBottomSheetPreview() {
    RamaniTheme {
        PlaceAddedBottomSheet(
            data = PlaceAddedSheetData(
                name = "Sample Place Added",
                category = "Parks",
                actionData = null
            ),
            onDismiss = {}
        )
    }
}
