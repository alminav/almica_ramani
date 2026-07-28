package com.almica.ramani.googlemaps

import android.content.ClipData
import android.content.Intent
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.almica.ramani.Const
import com.almica.ramani.R
import com.almica.ramani.charts.theme.Teal200
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.utils.format
import com.almica.ramani.googlemaps.NewMapAction.Create
import com.almica.ramani.googlemaps.NewMapAction.Import
import com.almica.ramani.tilemaker.MbtilesDatabase
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import kotlinx.coroutines.delay
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.ui.theme.RamaniTheme
import com.google.maps.android.compose.rememberCameraPositionState
import timber.log.Timber

@Composable
fun GmsMapRefreshButton(
    cameraPositionState: CameraPositionState,
    updateTileOverlay: (LatLng, Pair<String, String>?) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var showNewMapMenu by remember { mutableStateOf(false) }
    var createMbTileRegion by remember { mutableStateOf<String?>(null) }
    var progressCreateTilename by remember { mutableStateOf<String?>(null) }
    var progressCreateValue by remember { mutableIntStateOf(0) }
    var tileProviderLatLng = cameraPositionState.position.target
    //var tileProviderMbTiles: Pair<String, String>? = null
    var tileProviderMbTiles = getMbTileName(context, cameraPositionState.position.target)
    var currentTileProviderMbTiles: String? by remember { mutableStateOf(null) }
    val clipboardManager = LocalClipboard.current
    var clipText: String? by remember { mutableStateOf(null) }
    LaunchedEffect(clipText) {
        if (!clipText.isNullOrEmpty()) {
            Timber.i("clipText: $clipText")
            val clipData = ClipData.newPlainText( Import.name, clipText)
            val clipEntry = ClipEntry(clipData)
            clipboardManager.setClipEntry(clipEntry)
            clipText = null
        }
    }
    // can replace a snack bar 12dez2025, works like CountDownTimer
    LaunchedEffect(key1 = currentTileProviderMbTiles) {
        Timber.i("tileProviderMbTiles: $currentTileProviderMbTiles")
        delay(3000)
        if (!showNewMapMenu)
            currentTileProviderMbTiles = null
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        52.dp else 4.dp
                )
        ) {
            progressCreateTilename?.let {
                Text(
                    text = "$it ${progressCreateValue}%",
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .fillMaxWidth()
                        .background(Color.White),
                    textAlign = TextAlign.Center
                )
                LinearProgressIndicator(
                    progress = (0.01 * progressCreateValue).toFloat(),
                    modifier = Modifier
                        .background(Color.White)
                        .padding(start = 10.dp, end = 10.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        tileProviderLatLng = cameraPositionState.position.target
                        val mbTileName = getMbTileName(context, tileProviderLatLng)
                        currentTileProviderMbTiles = mbTileName.first
                        tileProviderMbTiles = mbTileName
                        updateTileOverlay(tileProviderLatLng, tileProviderMbTiles)
                        Timber.i("currentTileProviderMbTiles: $currentTileProviderMbTiles")
                        Timber.i(
                            "tileProviderLatLng: " +
                                    "${tileProviderLatLng.latitude.format(4)} ${
                                        tileProviderLatLng.longitude.format(
                                            4
                                        )
                                    }"
                        )
                    }, modifier = Modifier
                        .clip(CircleShape)
                        .width(32.dp)
                        .height(32.dp)
                        .border(1.dp, Teal200, CircleShape)
                        .background(colorResource(R.color.teal_200_trans))
                ) {
                    Icon(Icons.Outlined.Refresh, null, tint = Color.White)
                }
            }
            if (currentTileProviderMbTiles != null) {
                AnimatedVisibility(
                    visible = true
                ) {
                    val dbName = "${currentTileProviderMbTiles}${Const.MBTILES_EXT}"
                    val dbFile = MbtilesDatabase.DatabaseContext(context).getDatabasePath(dbName)
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(onClick = {
                            if (!dbFile.exists()) {
                                showNewMapMenu = true
                            }
                        }) {
                            Text(
                                text = currentTileProviderMbTiles!!,
                                modifier = if (dbFile.exists()) Modifier
                                    .background(Color.White)
                                    .padding(start = 5.dp, end = 5.dp)
                                else Modifier
                                    .background(Color.Red)
                                    .padding(start = 5.dp, end = 5.dp)
                            )
                        }
                    }
                }
            }

            if (showNewMapMenu) {
                Timber.i("tileProviderMbTiles: $tileProviderMbTiles")
                NewMapMenu(context, cameraPositionState.position.target) { action, mapname ->
                    showNewMapMenu = false
                    when (action) {
                        null -> {}
                        NewMapAction.Nothing -> {}
                        Create -> {
                            createMbTileRegion = currentTileProviderMbTiles
                            progressCreateTilename = createMbTileRegion
                            Timber.i("createMbTileRegion: $createMbTileRegion")
                            currentTileProviderMbTiles = null
                        }

                        Import -> {
                            Timber.i("$action")
                            currentTileProviderMbTiles = null
                            clipText = mapname
                            context.startActivity(
                                Intent(context, FileImportActivity::class.java)
                                    .setAction(resources.getString(R.string.import_title))
                                    .putExtra(Const.EXTRA_FILETYPE, FileType.MbTiles.name)
                            )
                        }
                    }
                }
            }
            createMbTileRegion?.let {
                CreateMbTileRegion(
                    it,
                    progress_ = { p ->
                        progressCreateValue = p
                        createMbTileRegion = null
                        Timber.i("progress: $p")
                    },
                    finished = {
                        progressCreateValue = 100
                        createMbTileRegion = null
                        progressCreateTilename = null
                        Timber.i("finished")
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GmsMapRefreshButtonPreview() {
    RamaniTheme {
        val cameraPositionState = rememberCameraPositionState()
        GmsMapRefreshButton(
            cameraPositionState = cameraPositionState,
            updateTileOverlay = { _, _ -> }
        )
    }
}
