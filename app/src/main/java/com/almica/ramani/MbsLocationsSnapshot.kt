package com.almica.ramani

//noinspection UsingMaterialAndMaterial3Libraries
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Switch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almica.ramani.Helpers.Companion.saveLocations
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.formatDistM
import kotlinx.coroutines.flow.update
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.snapshotter.MapSnapshot
import timber.log.Timber
import java.text.SimpleDateFormat
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MbsLocationsSnapshot(
    finished: () -> Unit,
    refreshLogCount: () -> Unit, showSaveResult: (String) -> Unit,
    viewModel: LocationsSnapshotViewModel = viewModel()) {
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        Timber.i("MbsLocationsSnapshot LaunchedEffect")
        viewModel.loadLocations()
    }
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.snapshotBitmap != null) {
        ModalBottomSheet(onDismissRequest = {
            finished()
        }) {
            MbsLocationsSnapshotContent(
                snapshotBitmap = uiState.snapshotBitmap!!,
                title = uiState.title,
                locationsLllh = uiState.lllhLocations,
                showGradient = uiState.showGradient,
                startTime = uiState.startTime,
                onStartTimeChange = { time ->
                    viewModel.setStartTime(time)
                },
                saveLocations = { time ->
                    saveLocations(context, time) { msg ->
                        showSaveResult(msg)
                    }
                },
                deleteLocationsAfter = { time ->
                    viewModel.deleteLocationsAfter(time, refreshLogCount)
                },
                deleteLocationsBefore = { time ->
                    viewModel.deleteLocationsBefore(time, refreshLogCount)
                },
                changeGradientState = { state ->
                    viewModel.setShowGradient(state)
                },
                pixelForLatLng = uiState.snapshot?.let { snap -> { latLng -> snap.pixelForLatLng(latLng) } }
            )
        }
    } else if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun MbsLocationsSnapshotContent(
    snapshotBitmap: ImageBitmap,
    title: String?,
    locationsLllh: List<LatLngH>?,
    showGradient: Boolean,
    startTime: Long,
    onStartTimeChange: (Long) -> Unit,
    saveLocations: (Long) -> Unit,
    deleteLocationsAfter: (Long) -> Unit,
    deleteLocationsBefore: (Long) -> Unit,
    changeGradientState: (Boolean) -> Unit,
    pixelForLatLng: ((LatLng) -> PointF)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    var selectedTime by remember { mutableLongStateOf(startTime) }

    Column(modifier = Modifier.padding(start = 3.dp, end = 3.dp)) {
        title?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(modifier = Modifier.weight(0.25f), onClick = {
                    deleteLocationsBefore(selectedTime)
                }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = stringResource(R.string.delete_before),
                            fontSize = 12.sp
                        )
                    }
                }
                TextButton(modifier = Modifier.weight(0.2f), onClick = {
                    saveLocations(selectedTime)
                }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.SaveAlt,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = stringResource(R.string.save),
                            fontSize = 12.sp
                        )
                    }
                }
                TextButton(modifier = Modifier.weight(0.2f), onClick = {
                    deleteLocationsAfter(selectedTime)
                }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = stringResource(R.string.delete_after),
                            fontSize = 12.sp
                        )
                    }
                }
                Row(modifier = Modifier.weight(0.3f), verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = showGradient,
                        onCheckedChange = {
                            changeGradientState(it)
                        }
                    )
                    Text(
                        text = stringResource(R.string.gradient),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
        val widthDp = with(density) { (snapshotBitmap.width).toDp() }
        Box( // necessary to center the snapshot
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(widthDp), contentAlignment = Alignment.Center) {
            Timber.i("snapshotBitmap: ${snapshotBitmap.width} x ${snapshotBitmap.height}}")
            //val heightDp = with(density) { (snapshotBitmap.height).toDp() }
            //Timber.i("snapshotBitmap.dp: $density | $widthDp x $heightDp")
            Image(modifier = Modifier.size(widthDp, widthDp),
                painter = BitmapPainter(snapshotBitmap),
                contentDescription = null
            )

            // Marker Overlay
            pixelForLatLng?.let { pfl ->
                locationsLllh?.let { lllh ->
                    val index = lllh.indexOfLast { it.time <= selectedTime }.coerceAtLeast(0)
                    if (index < lllh.size) {
                        val h = lllh[index].altitude
                        val textH = "${Const.UC_ELE_ARROW}${h.formatDistM(true)}"
                        val pixel = pfl(lllh[index].latLngMapLibre)
                        val markerRed = remember { Helpers.getBitmapFromVectorDrawable(context, R.drawable.stop_marker_24) }
                        markerRed?.let { m ->
                            val startPadding =
                                    (pixel.x / snapshotBitmap.width.toFloat() * widthDp.value - 14).coerceAtLeast(0F).dp
                            val topPadding = (pixel.y / snapshotBitmap.height.toFloat() * widthDp.value - 28).coerceAtLeast(0F).dp
                            //Timber.i("startPadding: $startPadding topPadding: $topPadding")
                            Image(
                                painter = BitmapPainter(m.asImageBitmap()),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(
                                        start = startPadding,
                                        top = topPadding
                                    )
                            )
                            Text(
                                textH,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(
                                        start = startPadding,
                                        top = topPadding + (2 * 14.sp.value).dp
                                    ))
                        }
                    }
                }
            }
        }
        }

        Spacer(modifier = Modifier.height(4.dp))

        locationsLllh?.takeIf { it.isNotEmpty() }?.let { entities ->
            var sliderPosition by remember { mutableFloatStateOf(0f) }
            Column(modifier = Modifier.padding(start = 32.dp, end = 4.dp)) {
                if (entities.size > 1) {
                    Slider(
                        value = sliderPosition,
                        onValueChange = {
                            sliderPosition = it
                            val index = sliderPosition.roundToInt().coerceIn(0, entities.size - 1)
                            selectedTime = entities[index].time
                            onStartTimeChange(selectedTime)
                        },
                        steps = entities.size - 2,
                        valueRange = 0f..(entities.size - 1).toFloat()
                    )
                }
                val index = sliderPosition.roundToInt().coerceIn(0, entities.size - 1)
                @SuppressLint("SimpleDateFormat") val timeFormat =
                    SimpleDateFormat(Const.TIME_PATTERN_LONG_YEAR_DE)
                val timeTag = java.lang.String.format(
                    LocalLocale.current.platformLocale, "%s", timeFormat.format(entities[index].time)
                )
                Text(
                    text = "$index: $timeTag  $title",
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MbsLocationsSnapshotContentPreview() {
    RamaniTheme {
        MbsLocationsSnapshotContent(
            snapshotBitmap = ImageBitmap(100, 100),
            title = "10.5 km 01:23:45 [h:m:s]",
            locationsLllh = listOf(),
            showGradient = false,
            startTime = 0L,
            onStartTimeChange = {},
            saveLocations = {},
            deleteLocationsAfter = {},
            deleteLocationsBefore = {},
            changeGradientState = {}
        )
    }
}

private fun addMarker(
    snapshot: Any,
    bitmap: Bitmap?,
    context: Context,
    latLng: LatLng
): Bitmap? {
    val snap = snapshot as MapSnapshot
    val canvas = bitmap?.let { Canvas(it) }
    val marker = Helpers.getBitmapFromVectorDrawable(context, R.drawable.stop_marker_24)
    val markerLocation = snap.pixelForLatLng(latLng)
    marker?.let {
        canvas?.drawBitmap(
            it, /* Subtract half of the width so we center the bitmap correctly */
            markerLocation.x - marker.width / 2, /* Subtract half of the height so we align the bitmap bottom correctly */
            markerLocation.y - marker.height,
            null
        )
    }
    return bitmap
}