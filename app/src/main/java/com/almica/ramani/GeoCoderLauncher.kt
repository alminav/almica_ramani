package com.almica.ramani

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.almica.ramani.googlemaps.MapUtils.gmsElevationService
import com.almica.ramani.utils.BackPressHandler
import com.almica.ramani.utils.addPoiDao
import com.strongtogether.googlemapsjetpackcompose.PlacesActivity
import kotlinx.coroutines.delay
import org.maplibre.android.geometry.LatLng
import timber.log.Timber
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.ui.theme.RamaniTheme
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoCoderLauncher(latLng: com.google.android.gms.maps.model.LatLng?, showInMap: (name: String?, category: String?, LatLng?) -> Unit) {
    val context = LocalContext.current
    val marginTopDp = TopAppBarDefaults.TopAppBarExpandedHeight.value
    var showPoiCatMoBoSheet: ResultData? by remember { mutableStateOf(null) }
    var snackbarData by remember { mutableStateOf<GeoCoderLauncherSnackbarData?>(null) }
    LaunchedEffect(key1 = snackbarData) {
        Timber.i( "LaunchedEffect: ${snackbarData?.actionData?.name} ${snackbarData?.action}")
        delay(5000)
        if (snackbarData != null && snackbarData!!.actionData != null) {
            showInMap(snackbarData!!.actionData!!.name,
                snackbarData!!.actionData!!.category, snackbarData!!.actionData!!.latLng)
        }
        snackbarData = null
    }
    BackPressHandler {
        Timber.i( "Back Press intercepted")
        if (showPoiCatMoBoSheet != null)
            showPoiCatMoBoSheet = null
        else
            showInMap(null, null, null)
    }
    var activityResult by remember { mutableStateOf<ActivityResult?>(null) }
    var resultLatLng by remember { mutableStateOf(arrayOf(latLng?.latitude ?: -1.0,
        latLng?.longitude ?: -1.0)) }
    activityResult?.let { result ->
        // places api result from: com.strongtogether.googlemapsjetpackcompose.PlacesActivity
        val placesName = result.data?.getStringExtra(Const.PLACE_NAME)
        val placesLat = result.data?.getDoubleExtra(Const.PLACE_LATITUDE, -1.0)
        val placesLon = result.data?.getDoubleExtra(Const.PLACE_LONGITUDE, -1.0)
        if (placesName != null) {
            Timber.i("placesName = $placesName $placesLat $placesLon")
//            prefs.edit { placesLat?.let { putLong(Const.PREF_LATITUDE, it.toRawBits()) } }
//            prefs.edit { placesLon?.let { putLong(Const.PREF_LONGITUDE, it.toRawBits()) } }
            placesLat?.let { resultLatLng[0] = it }
            placesLon?.let { resultLatLng[1] = it }
            showPoiCatMoBoSheet =
                ResultData(placesName, LatLng(resultLatLng[0], resultLatLng[1]), null)
        }
        activityResult = null
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                activityResult = result
                // Process the result here
            }
        }
    LaunchedEffect(key1 = Unit) {
        delay(100.milliseconds)
        val intent = Intent(context, PlacesActivity::class.java)
        intent.putExtra(Const.EXTRA_LATITUDE, resultLatLng[0])
        intent.putExtra(Const.EXTRA_LONGITUDE, resultLatLng[1])
        launcher.launch(intent)
    }
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    Scaffold(
        modifier = Modifier.padding(top = marginTopDp.dp),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { showInMap(null, null, null) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back home"
                        )
                    }
                },
                title = {
                    Text(text = "GeoCoder")
                }
            )
        }
    ) { paddingValues ->
        snackbarData?.let {
            MoboSnack(snackbarData!!) { action ->
                when (action) {
                    GeoCoderLauncherSnackbarAction.Nothing -> {
                        Timber.i("action: $action")
                    }
                }
                snackbarData = null
            }
        }
        showPoiCatMoBoSheet?.let { _ ->
            Timber.i("name: ${showPoiCatMoBoSheet!!.name}")
            PoiCatMoBoSheet(showPoiCatMoBoSheet!!.name.toString()) { name, category ->
                Timber.i("$name $category")
                if (showPoiCatMoBoSheet!!.latLng != null)
                    when (category) {
                        null -> {
                            showInMap(null, null, showPoiCatMoBoSheet!!.latLng)
                            showPoiCatMoBoSheet = null
                        } else -> {
                            val locationsParm = "${showPoiCatMoBoSheet!!.latLng!!.latitude},${showPoiCatMoBoSheet!!.latLng!!.longitude}"
                            gmsElevationService(context, locationsParm) { lllh0 ->
                                val h = lllh0[0].altitude
                                addPoiDao(
                                    context, name,
                                    com.google.android.gms.maps.model.LatLng(
                                        showPoiCatMoBoSheet!!.latLng!!.latitude,
                                        showPoiCatMoBoSheet!!.latLng!!.longitude),
                                    h,
                                    category
                                ) { _ ->
                                    snackbarData = GeoCoderLauncherSnackbarData(
                                        context.getString(
                                            R.string.added_to_database,
                                            name
                                        ),
                                        null, null,
                                        GeoCoderLauncherSnackbarAction.Nothing,
                                        showPoiCatMoBoSheet
                                    )
                                    showPoiCatMoBoSheet = null
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
private fun MoboSnack(geoCoderSnackbarData: GeoCoderLauncherSnackbarData, finished: (action: GeoCoderLauncherSnackbarAction) -> Unit) {
    ModalBottomSheet(onDismissRequest = {
        Timber.i("onDismissRequest")
        finished(GeoCoderLauncherSnackbarAction.Nothing)
    }) {
        Box(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) {
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
                        Modifier
                            .weight(0.8f)
                            .padding(top = 8.dp, bottom = 8.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Blue
                    )
                }
                geoCoderSnackbarData.category.let { text ->
                    TextButton(onClick = {
                        Timber.i("category: $text")
                        finished(GeoCoderLauncherSnackbarAction.Nothing)
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
fun GeoCoderLauncherPreview() {
    RamaniTheme {
        GeoCoderLauncher(
            latLng = com.google.android.gms.maps.model.LatLng(-1.286389, 36.817223),
            showInMap = { _, _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MoboSnackPreview() {
    RamaniTheme {
        MoboSnack(
            geoCoderSnackbarData = GeoCoderLauncherSnackbarData(
                name = "Sample Place",
                category = "Sample Category",
                actionText = null,
                action = GeoCoderLauncherSnackbarAction.Nothing,
                actionData = null
            ),
            finished = {}
        )
    }
}
