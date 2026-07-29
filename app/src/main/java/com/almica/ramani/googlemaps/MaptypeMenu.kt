package com.almica.ramani.googlemaps

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.almica.ramani.Const
import com.almica.ramani.R
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber

private data class MapTypeOption(
    val entry: String,
    val url: String,
    val imageRes: Int?,
)

@Composable
fun MaptypeMenu(context: Context, finished: (String?) -> Unit) {
    val maptypeTemplates = remember {
        arrayOf(
            R.drawable.phonemaps_tile,
            R.drawable.opentopo_tile,
            R.drawable.outdoor_tile,
            R.drawable.thunderforest_tile
        )
    }
    val maptypeEntries = remember { context.resources.getStringArray(R.array.pref_tilemaker_maptypes_entries) }
    val maptypeUrls = remember { context.resources.getStringArray(R.array.pref_tilemaker_maptypes_urls) }

    val options = remember(maptypeEntries, maptypeUrls, maptypeTemplates) {
        maptypeEntries.mapIndexed { index, entry ->
            MapTypeOption(
                entry = entry,
                url = maptypeUrls.getOrNull(index) ?: "",
                imageRes = maptypeTemplates.getOrNull(index)
            )
        }
    }

    val preferences = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val currentMapType = remember {
        preferences.getString(
            context.getString(R.string.pref_tilemaker_maptype),
            Const.OUTDOOR
        )
    }

    LaunchedEffect(currentMapType) {
        Timber.i("currentMapType: $currentMapType")
    }

    MaptypeMenu(
        options = options,
        currentMapType = currentMapType,
        onMapTypeSelected = { option ->
            Timber.i("Selected map type: ${option.entry}, URL: ${option.url}")
            preferences.edit {
                putString(context.getString(R.string.pref_tilemaker_url), option.url)
                putString(context.getString(R.string.pref_tilemaker_maptype), option.entry)
            }
            finished(option.entry)
        }
    ) { finished(null) }
}

@Composable
private fun MaptypeMenu(
    options: List<MapTypeOption>,
    currentMapType: String?,
    onMapTypeSelected: (MapTypeOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.uc_close))
            }
        },
        title = {
            Text(
                text = stringResource(R.string.raster_map_type),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                currentMapType?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                MaptypeMenuContent(
                    options = options,
                    currentMapType = currentMapType,
                    onMapTypeSelected = onMapTypeSelected
                )
            }
        }
    )
}

@Composable
private fun MaptypeMenuContent(
    options: List<MapTypeOption>,
    currentMapType: String?,
    onMapTypeSelected: (MapTypeOption) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(options) { index, option ->
            val isSelected = option.entry == currentMapType
            Surface(
                onClick = { onMapTypeSelected(option) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.surface,
                tonalElevation = if (isSelected) 4.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = option.entry,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textDecoration = if (isSelected) TextDecoration.Underline else TextDecoration.None
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    option.imageRes?.let { resId ->
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = option.entry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
            if (index < (options.size - 1)) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MaptypeMenuPreview() {
    val options = listOf(
        MapTypeOption("Outdoor", "", R.drawable.outdoor_tile),
        MapTypeOption("OpenTopo", "", R.drawable.opentopo_tile),
        MapTypeOption("Thunderforest", "", R.drawable.thunderforest_tile)
    )
    RamaniTheme {
        MaptypeMenu(
            options = options,
            currentMapType = "Outdoor",
            onMapTypeSelected = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MaptypeMenuDarkPreview() {
    val options = listOf(
        MapTypeOption("Outdoor", "", R.drawable.outdoor_tile),
        MapTypeOption("OpenTopo", "", R.drawable.opentopo_tile),
        MapTypeOption("Thunderforest", "", R.drawable.thunderforest_tile)
    )
    RamaniTheme(darkTheme = true) {
        MaptypeMenu(
            options = options,
            currentMapType = "Outdoor",
            onMapTypeSelected = {},
            onDismiss = {}
        )
    }
}
