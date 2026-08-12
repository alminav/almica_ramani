package com.almica.ramani

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import com.almica.ramani.routes.RouteDialogMode
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.KiThumbnailer.PixelPoint
import com.almica.ramani.utils.formatAlti
import com.almica.ramani.utils.formatDistM
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import timber.log.Timber
import java.io.File
import java.io.IOException
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan

/**
 * Data class used to store transformation parameters for rendering a GPS route onto a static bitmap with RouteDialogUI
 *
 * @property minX The minimum horizontal coordinate of the route's bounding box in Mercator projection.
 * @property minY The minimum vertical coordinate of the route's bounding box in Mercator projection.
 * @property scale The scaling factor applied to fit the projected route within the thumbnail dimensions.
 * @property offsetX The horizontal translation required to center the route within the available canvas space.
 * @property offsetY The vertical translation required to center the route within the available canvas space.
 */
private data class RouteDisplayParams(
    val minX: Float,
    val minY: Float,
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float
)
private data class RouteDataState(
    val rawName: String,
    val lllh: List<LatLngH>,
    val distanceValues: List<Double>,
    val thumbnail: ImageBitmap?,
    val projection: ProjectionParams?
)
private data class ProjectionParams(
    val minX: Float, val minY: Float, val scale: Float, val offsetX: Float, val offsetY: Float
)
@Composable
fun RouteDialog(filesDir: File?, routeFile: File, finish: () -> Unit, alert: (String) -> Unit,
                share: () -> Unit, refresh: () -> Unit, select: () -> Unit, dialogModeOrdinal: Int) {
    routeFile.let {
        Timber.i("routeFile ${routeFile.path}")
        val routeFolder = routeFile.parentFile
        val routeEntityRawName = routeFile.nameWithoutExtension
        var routeThumbFile =
            File(routeFolder, routeEntityRawName.plus(Const.JPG_EXT))
        if (!routeThumbFile.exists())
            routeThumbFile =
                File(File(filesDir, Const.THUMBNAILS), routeEntityRawName.plus(Const.JPG_EXT))
        val imgDesc = Helpers.getImageDescriptionFromExif(routeThumbFile)
        // ImageDescription -   26.1km 51.893019 10.255557 51.821871 10.348713
        //                      dist NW-lat NW-lng SE-lat SE-lng
        if (imgDesc != null && imgDesc.startsWith(Const.GEOJSON_ROOT_FOLDER)) {
            Dialog(onDismissRequest = { finish() },
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        //ExpandableCard(routeEntityRawName, routeEntityRawName, routeThumbFile)

                        val bitmap = remember(routeThumbFile) {
                            if (routeThumbFile.exists()) {
                                BitmapFactory.decodeFile(routeThumbFile.path)?.asImageBitmap()
                            } else null
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it,
                                contentDescription = routeEntityRawName,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                if (dialogModeOrdinal == RouteDialogMode.Admin.ordinal)
                                    refresh() else select()
                            }) {
                                Text(stringResource(if (dialogModeOrdinal == RouteDialogMode.Admin.ordinal)
                                    R.string.refresh else R.string.select))
                            }
                            TextButton(onClick = {
                                var geojsonString: String? // only for test 29jun2026
                                val exifInterface: ExifInterface
                                try {
                                    exifInterface = ExifInterface(routeThumbFile.path)
                                    geojsonString =
                                        exifInterface.getAttribute(ExifInterface.TAG_USER_COMMENT)
                                            ?.let { compressedData ->
                                                Helpers.decompressString(compressedData)
                                            }
                                    Timber.i("geojsonString: $geojsonString")
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                                finish()
                            }) {
                                Text(stringResource(R.string.exit_))
                            }
                        }
                    }
                }
            }
        } else {
            val parts = imgDesc?.split(" ")?.filter { it.isNotBlank() }
            if (parts == null || parts.size < 5) {
                Timber.i("invalid exif image description: $imgDesc $routeEntityRawName")
                alert("invalid exif image description: $imgDesc $routeEntityRawName")
            }

            val thumbnail = remember(routeThumbFile.path) {
                if (routeThumbFile.exists()) {
                    val options = BitmapFactory.Options().apply { inMutable = true }
                    BitmapFactory.decodeFile(routeThumbFile.path, options)?.asImageBitmap()
                } else {
                    Timber.i("could not read thumbnail file: ${routeThumbFile.path}")
                    null
                }
            }
            val thumbnailCopy =
                thumbnail?.let {
                    createBitmap(
                        (it.asAndroidBitmap().width),
                        (it.asAndroidBitmap().height)
                    ).asImageBitmap()
                }
            thumbnailCopy?.let { image ->
                Canvas(image.asAndroidBitmap()).drawBitmap(
                    thumbnail.asAndroidBitmap(),
                    0f,
                    0f,
                    null
                )
            }
            val lllh =
                (Helpers.getCoordinatesFromExif(routeThumbFile) as List<LatLngH>) //.simplifyToTargetCount(200)
            val distanceValues = arrayListOf<Double>()
            var dist = 0.0
            lllh.forEachIndexed { index, _ ->
                if (index > 0)
                    dist += SphericalUtil.computeDistanceBetween(
                        lllh[index].latLngGms,
                        lllh[index - 1].latLngGms
                    )
                distanceValues.add(dist)
            }
            Timber.i("imgDesc: $imgDesc")

            val displayParams = remember(parts, routeThumbFile.path) {
                if (parts == null || parts.size < 5) return@remember null
                try {
                    val nwLat = parts[1].toDouble()
                    val nwLng = parts[2].toDouble()
                    val seLat = parts[3].toDouble()
                    val seLng = parts[4].toDouble()
                    val gpsRoute = listOf(
                        LatLng(nwLat, nwLng),
                        LatLng(nwLat, seLng),
                        LatLng(seLat, seLng),
                        LatLng(seLat, nwLng)
                    )

                    // 1. Project all points
                    val projectedPoints = gpsRoute.map { projectMercator(it) }

                    // 2. Determine extreme values (Bounding Box)
                    val minX = projectedPoints.minOf { it.x }
                    val maxX = projectedPoints.maxOf { it.x }
                    val minY = projectedPoints.minOf { it.y }
                    val maxY = projectedPoints.maxOf { it.y }

                    val routeWidth = maxX - minX
                    val routeHeight = maxY - minY

                    val dimensions = Helpers.getImageDimensionsFromExif(routeThumbFile)
                    val canvasWidth = dimensions.first?.toInt()
                    if (canvasWidth != null) {
                        val usableWidth = canvasWidth.toFloat()
                        val usableHeight = usableWidth

                        val scaleX = if (routeWidth > 0) usableWidth / routeWidth else 1f
                        val scaleY = if (routeHeight > 0) usableHeight / routeHeight else 1f
                        val scale = minOf(scaleX, scaleY)

                        val offsetX = (usableWidth - (routeWidth * scale)) / 2f
                        val offsetY = (usableHeight - (routeHeight * scale)) / 2f

                        RouteDisplayParams(minX, minY, scale, offsetX, offsetY)
                    } else {
                        Timber.e("exif interface: canvas invalid")
                        null
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing exif data")
                    null
                }
            }

            if (displayParams != null) {
                RouteDialogStateless(
                    routeEntityRawName = routeEntityRawName,
                    thumbnail = thumbnail,
                    thumbnailCopy = thumbnailCopy,
                    lllh = lllh,
                    distanceValues = distanceValues,
                    minX = displayParams.minX,
                    minY = displayParams.minY,
                    scale = displayParams.scale,
                    offsetX = displayParams.offsetX,
                    offsetY = displayParams.offsetY,
                    finish = {
                        Timber.i("RouteDialog: finish")
                        finish()
                    }, share = {
                        share()
                    }, refresh = {
                        refresh()
                    }, selected = {
                        select()
                    }, dialogModeOrdinal = dialogModeOrdinal
                )
            } else {
                finish()
            }
        }
    }
}

@Composable
private fun RouteDialogStateless(
    routeEntityRawName: String,
    thumbnail: ImageBitmap?,
    thumbnailCopy: ImageBitmap?,
    lllh: List<LatLngH>,
    distanceValues: List<Double>,
    minX: Float,
    minY: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    finish: () -> Unit,
    share: () -> Unit,
    refresh: () -> Unit,
    selected: () -> Unit,
    dialogModeOrdinal: Int
) {
    RouteDialogUI(
        routeEntityRawName = routeEntityRawName,
        thumbnail = thumbnail,
        thumbnailCopy = thumbnailCopy,
        lllh = lllh,
        distanceValues = distanceValues,
        minX = minX,
        minY = minY,
        scale = scale,
        offsetX = offsetX,
        offsetY = offsetY,
        finish = finish,
        share = share,
        refresh = refresh,
        selected = selected,
        dialogModeOrdinal = dialogModeOrdinal
    )
}
@Preview(showBackground = true)
@Composable
private fun RouteDialogPreview() {
    val sampleLllh = listOf(
        LatLngH(51.8283, 10.2825, 100.0),
        LatLngH(51.8290, 10.2830, 110.0),
        LatLngH(51.8300, 10.2840, 120.0)
    )
    val distanceValues = listOf(0.0, 100.0, 250.0)
    val dummyBitmap = createBitmap(512, 512, Bitmap.Config.ARGB_8888).asImageBitmap()

    RamaniTheme {
        RouteDialogStateless(
            routeEntityRawName = "Sample Route",
            thumbnail = dummyBitmap,
            thumbnailCopy = dummyBitmap,
            lllh = sampleLllh,
            distanceValues = distanceValues,
            minX = 0f,
            minY = 0f,
            scale = 1f,
            offsetX = 0f,
            offsetY = 0f,
            finish = {},
            share = {},
            refresh = {},
            selected = {},
            dialogModeOrdinal = RouteDialogMode.Admin.ordinal
        )
    }
}
@Composable
fun RouteDialogUI(
    routeEntityRawName: String,
    thumbnail: ImageBitmap?,
    thumbnailCopy: ImageBitmap?,
    lllh: List<LatLngH>,
    distanceValues: List<Double>,
    minX: Float,
    minY: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    finish: () -> Unit,
    share: () -> Unit,
    refresh: () -> Unit,
    selected: () -> Unit,
    dialogModeOrdinal: Int
) {
    Timber.i("RouteDialogUI: $routeEntityRawName")
    val state = remember(routeEntityRawName, thumbnail, lllh) {
        RouteDataState(
            rawName = routeEntityRawName,
            lllh = lllh,
            distanceValues = distanceValues,
            thumbnail = thumbnail,
            projection = ProjectionParams(minX, minY, scale, offsetX, offsetY)
        )
    }
    val dialogMode = RouteDialogMode.entries.getOrElse(dialogModeOrdinal) { RouteDialogMode.Admin }

    Dialog(
        onDismissRequest = finish,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        RouteDialogContent(
            state = state,
            dialogMode = dialogMode,
            onShare = share,
            onRefresh = refresh,
            onSelected = selected,
            onDismiss = finish
        )
    }
}
@Composable
private fun RouteDialogContent(
    state: RouteDataState,
    dialogMode: RouteDialogMode,
    onShare: () -> Unit,
    onRefresh: () -> Unit,
    onSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    var routePointer by remember { mutableIntStateOf(0) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RouteHeader(state.rawName)

        state.thumbnail?.let { img ->
            RouteMapViewer(
                thumbnail = img,
                state = state,
                routePointer = routePointer,
                dialogMode = dialogMode,
                onSelected = onSelected
            )
        }

        if (state.lllh.isNotEmpty()) {
            RouteInfoBar(
                distance = state.distanceValues[routePointer],
                altitude = state.lllh[routePointer].altitude,
                onClick = {
                    if (routePointer < state.lllh.size - 1) {
                        routePointer++
                        sliderPosition = routePointer.toFloat()
                    }
                }
            )

            RouteControls(
                lllhSize = state.lllh.size,
                sliderPosition = sliderPosition,
                onSliderChange = { pos ->
                    sliderPosition = pos
                    routePointer = pos.roundToInt().coerceIn(0, state.lllh.size - 1)
                    Timber.i("sliderPosition: $sliderPosition, routePointer: $routePointer")
                },
                dialogMode = dialogMode,
                onShare = onShare,
                onRefresh = onRefresh,
                onDismiss = onDismiss
            )
        }
    }
}
@Composable
private fun RouteControls(
    lllhSize: Int,
    sliderPosition: Float,
    onSliderChange: (Float) -> Unit,
    dialogMode: RouteDialogMode,
    onShare: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    if (lllhSize > 1) {
        Slider(
            value = sliderPosition,
            onValueChange = onSliderChange,
            steps = if (lllhSize > 2) lllhSize - 2 else 0,
            valueRange = 0f..(lllhSize - 1).toFloat()
        )
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        if (dialogMode == RouteDialogMode.Admin) {
            TextButton(modifier = Modifier.weight(1f), onClick = onShare) {
                Text(text = stringResource(R.string.share))
            }
            TextButton(modifier = Modifier.weight(1f), onClick = onRefresh) {
                Text(text = stringResource(R.string.refresh))
            }
        }
        TextButton(modifier = Modifier.weight(1f), onClick = onDismiss) {
            Text(text = stringResource(R.string.exit_))
        }
    }
}
@Composable
private fun RouteHeader(name: String) {
    Text(
        modifier = Modifier.padding(bottom = 8.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        text = name
    )
}

@Composable
private fun RouteMapViewer(
    thumbnail: ImageBitmap,
    state: RouteDataState,
    routePointer: Int,
    dialogMode: RouteDialogMode,
    onSelected: () -> Unit
) {
    val projection = state.projection ?: return
    val point = state.lllh.getOrNull(routePointer) ?: return

    val markerPainter = painterResource(id = R.drawable.stop_marker_24)
    val aspectRatio = remember(thumbnail) {
        thumbnail.width.toFloat() / thumbnail.height.toFloat()
    }

    // Pre-calculate marker position relative to original thumbnail pixels
    val markerPosition = remember(point, projection) {
        val projectedPoint = projectMercator(point.latLngGms)
        Offset(
            x = (projectedPoint.x - projection.minX) * projection.scale + projection.offsetX,
            y = (projectedPoint.y - projection.minY) * projection.scale + projection.offsetY
        )
    }

    // Optimize Text Paint for theme changes and accessibility
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val density = LocalDensity.current
    val textSizePx = with(density) { 14.sp.toPx() } // Using SP for accessibility

    val textPaint = remember(textColor, textSizePx) {
        Paint().apply {
            color = textColor
            textSize = textSizePx
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            // Optional: add a small shadow for better legibility on busy maps
            setShadowLayer(2f, 0f, 0f, android.graphics.Color.WHITE)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onSelected,
                onClickLabel = stringResource(R.string.view_on_map)
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
        ) {
            // 1. Draw the background thumbnail
            drawImage(
                image = thumbnail,
                dstSize = IntSize(size.width.toInt(), size.height.toInt())
            )

            // 2. Calculate scaling for overlays
            val scaleFactor = size.width / thumbnail.width
            val markerX = markerPosition.x * scaleFactor
            val markerY = markerPosition.y * scaleFactor
            val markerSize = 24.dp.toPx()

            // 3. Draw the marker
            translate(left = markerX - markerSize / 2, top = markerY - markerSize) {
                with(markerPainter) {
                    draw(size = androidx.compose.ui.geometry.Size(markerSize, markerSize))
                }
            }

            // 4. Draw the altitude text
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    point.altitude.formatAlti(true),
                    markerX,
                    markerY - markerSize - 2,
                    textPaint
                )
            }
        }

        // Overlay Map Icon for specific modes
        if (dialogMode == RouteDialogMode.MapProvider) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null, // Visual indicator only
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun RouteInfoBar(distance: Double, altitude: Double, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text = "${distance.formatDistM(true)} ${altitude.formatAlti(true)}")
    }
}
fun projectMercator(point: LatLng): PixelPoint {
    val x = (point.longitude + 180.0) / 360.0
    val radLat = Math.toRadians(point.latitude)
    val mercatorY = ln(tan(PI / 4.0 + radLat / 2.0))
    val y = (1.0 - (mercatorY / PI)) / 2.0
    return PixelPoint(x.toFloat(), y.toFloat())
}