package com.almica.ramani

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.almica.ramani.googlemaps.MapUtils.gmsElevationService
import com.almica.ramani.utils.addPoiDao
import com.strongtogether.googlemapsjetpackcompose.PlacesActivity
import kotlinx.coroutines.delay
import org.maplibre.android.geometry.LatLng
import timber.log.Timber
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.ui.theme.RamaniTheme
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
    var showPoiCatDialog: ResultData? by remember { mutableStateOf(null) }
    var snackbarData by remember { mutableStateOf<GeoCoderLauncherSnackbarData?>(null) }

    LaunchedEffect(key1 = snackbarData) {
        snackbarData?.actionData?.let { actionData ->
            Timber.i("LaunchedEffect: ${actionData.name} ${snackbarData?.action}")
            delay(5000.milliseconds)
            showInMap(actionData.name, actionData.category, actionData.latLng)
            snackbarData = null
        }
    }

    BackHandler {
        Timber.i("Back Press intercepted")
        if (showPoiCatDialog != null) {
            showPoiCatDialog = null
        } else {
            showInMap(null, null, null)
        }
    }

    var activityResult by remember { mutableStateOf<ActivityResult?>(null) }
    var resultLatLng by remember {
        mutableStateOf(
            LatLng(latLng?.latitude ?: -1.0, latLng?.longitude ?: -1.0)
        )
    }

    activityResult?.let { result ->
        val placesName = result.data?.getStringExtra(Const.PLACE_NAME)
        val placesLat = result.data?.getDoubleExtra(Const.PLACE_LATITUDE, -1.0) ?: -1.0
        val placesLon = result.data?.getDoubleExtra(Const.PLACE_LONGITUDE, -1.0) ?: -1.0

        if (placesName != null) {
            Timber.i("placesName = $placesName $placesLat $placesLon")
            resultLatLng = LatLng(placesLat, placesLon)
            showPoiCatDialog = ResultData(placesName, resultLatLng, null)
        }
        activityResult = null
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            activityResult = result
        }
    }

    LaunchedEffect(key1 = Unit) {
        delay(100.milliseconds)
        val intent = Intent(context, PlacesActivity::class.java).apply {
            putExtra(Const.EXTRA_LATITUDE, resultLatLng.latitude)
            putExtra(Const.EXTRA_LONGITUDE, resultLatLng.longitude)
        }
        launcher.launch(intent)
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
            snackbarData?.let { data ->
                PlaceAddedBottomSheet(data) {
                    snackbarData = null
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
                            gmsElevationService(context, locationsParam) { lllh0 ->
                                val h = lllh0.firstOrNull()?.altitude ?: 0.0
                                val addedToDatabaseMsg = context.resources.getString(R.string.added_to_database, name)
                                addPoiDao(
                                    context,
                                    name,
                                    GmsLatLng(dialogLatLng.latitude, dialogLatLng.longitude),
                                    h,
                                    category
                                ) { _ ->
                                    snackbarData = GeoCoderLauncherSnackbarData(
                                        addedToDatabaseMsg,
                                        null,
                                        null,
                                        GeoCoderLauncherSnackbarAction.Nothing,
                                        dialogData
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

data class GeoCoderLauncherSnackbarData(
    val name: String?,
    val category: String?,
    val actionText: String?,
    val action: GeoCoderLauncherSnackbarAction,
    val actionData: ResultData?)

enum class GeoCoderLauncherSnackbarAction {
    Nothing
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceAddedBottomSheet(
    geoCoderSnackbarData: GeoCoderLauncherSnackbarData,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = {
        Timber.i("onDismissRequest")
        onDismiss()
    }) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.border(
                    width = 2.dp,
                    color = Color.LightGray,
                    shape = RectangleShape
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                geoCoderSnackbarData.name?.let {
                    Text(
                        text = it,
                        modifier = Modifier
                            .weight(0.8f)
                            .padding(vertical = 12.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                geoCoderSnackbarData.category?.let { text ->
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.2f)
                    ) {
                        Text(
                            text = text,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
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
            geoCoderSnackbarData = GeoCoderLauncherSnackbarData(
                name = "Sample Place",
                category = "Sample Category",
                actionText = null,
                action = GeoCoderLauncherSnackbarAction.Nothing,
                actionData = null
            ),
            onDismiss = {}
        )
    }
}
