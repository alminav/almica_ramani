package com.almica.ramani

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.preference.PreferenceManager
import com.almica.ramani.utils.DriveSharedLinks
import timber.log.Timber
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListRasterDriveEntries(
    onDismissRequest: () -> Unit,
    import: (String?) -> Unit,
    onItemClick: (RasterItemModel) -> Unit
) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(LocalContext.current)
    val rasterMapFilePathSet =
        prefs.getStringSet(Const.PREF_MBTILES_FILEPATH_SET, null)
    var rasterItemModels: List<RasterItemModel>
    var currentRasterText = ""
    if (!rasterMapFilePathSet.isNullOrEmpty()) {
        val firstRasterMapFile = File(rasterMapFilePathSet.elementAt(0))
        currentRasterText = if (firstRasterMapFile.exists())
            "${firstRasterMapFile.name.replace(Const.MBTILES_EXT, "")} (+${rasterMapFilePathSet.size - 1})"
        else ""
    }
    val rasterFolder = File(LocalContext.current.filesDir, Const.MBTILES_FOLDER)
    val driveMap = DriveSharedLinks.Companion.RasterMaps().list
    val regionNames = arrayListOf<String>()
    driveMap.keys.forEach { key -> regionNames.add(key) }
    regionNames.sort()
    rasterItemModels = (0..<regionNames.size).map {
        val f = File(rasterFolder, regionNames[it])
        val splits = regionNames[it].replace(Const.MBTILES_EXT, "").split(Const.UNDERLINE, limit = 5)
        RasterItemModel(
            name = regionNames[it].replace(Const.MBTILES_EXT, ""),
            path = "",
            splits[1].toInt(),
            splits[2].toInt(),
            mapType = splits[4],
            f.exists(),
            rasterMapFilePathSet?.contains(f.path) ?: false
        )
    }
    Timber.i( "rasterItemModels:${rasterItemModels.size}")

    //sortOrder = featuresSortOrder
    val itemsGrouped = rasterItemModels.groupBy { it.mapType }
    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.92f),
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true,
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        ),
        shape = RoundedCornerShape(20.dp),
        onDismissRequest = {
            onDismissRequest()
        }, confirmButton = {
            TextButton(onClick = { import(null) }) {
                Text(text = stringResource(R.string.import_title))
            }
        }, dismissButton = {
            TextButton(onClick = { onDismissRequest() }) {
                Text(text = stringResource(R.string.uc_close))
            }
        }, title = {
            Column {
                Text(
                    text = stringResource(R.string.raster_drive_content),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
                    Text(
                        text = stringResource(R.string.is_active),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentRasterText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                val scrollState = rememberScrollState()
            }
        }, text = {
            //Timber.i("sortOrder $sortOrder")
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsGrouped.forEach { (initial, driveEntries) ->
                    stickyHeader {
                        Column(
                            modifier = Modifier
                                .height(24.dp)
                                .fillMaxWidth()
                                .background(Color.LightGray)
                        ) {
                            Text(
                                text = initial,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    items(count = driveEntries.size) { index ->
                        RasterDriveEntryItem(driveEntries[index]) { driveEntry ->
                            driveEntry.let {
                                Timber.i("${it.name} ")
                                onItemClick(driveEntry)
                            }
                        }
                    }
                }
            }
        })
}

@Composable
private fun RasterDriveEntryItem(
    rasterItemModel: RasterItemModel,
    onItemClick: (RasterItemModel) -> Unit
) {
    Box(
        modifier = Modifier
            .background(color = Color.White)
            .fillMaxSize()
            .clickable { onItemClick(rasterItemModel) }
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically) {
                Timber.i(rasterItemModel.name)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = rasterItemModel.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (rasterItemModel.exists) FontWeight.Bold else FontWeight.Normal,
                    textDecoration = if (rasterItemModel.active) TextDecoration.Underline
                    else TextDecoration.None
                )
            }
        }
    }
}
data class RasterItemModel(
    val name: String,
    val path: String,
    val x: Int,
    val y: Int,
    val mapType: String,
    val exists: Boolean,
    val active: Boolean
)