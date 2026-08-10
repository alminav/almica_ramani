package com.almica.ramani.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.almica.ramani.R
import com.almica.ramani.ui.theme.RamaniTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

// Datenmodell für die Liste
data class WheelItem(
    val id: Int,
    val text: String,
    val imageRes: Int?
)

@Composable
fun WheelSelectionDialog(
    onDismissRequest: () -> Unit,
    onItemSelected: (WheelItem) -> Unit,
    items: List<WheelItem>,
    initialSelection: WheelItem? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        WheelSelectionContent(onDismissRequest, onItemSelected, items, initialSelection)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelSelectionContent(
    onDismissRequest: () -> Unit,
    onItemSelected: (WheelItem) -> Unit,
    items: List<WheelItem>,
    initialSelection: WheelItem? = null
) {
    // 1. Using an odd number of visible items for a clear center selection
    val visibleItemsCount = 3 
    val itemHeight = 140.dp
    val coroutineScope = rememberCoroutineScope()

    // Add padding to ensure the first and last items can reach the center
    val paddedItems = remember(items) {
        val pad = visibleItemsCount / 2
        List(pad) { null } + items + List(pad) { null }
    }

    // Initialen Index berechnen
    val initialIndex = remember(items, initialSelection) {
        val index = items.indexOf(initialSelection)
        if (index != -1) index else 0
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.raster_map_type),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight * visibleItemsCount),
                contentAlignment = Alignment.Center
            ) {
                // Selection Highlighting
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {}

                LazyColumn(
                    state = listState,
                    flingBehavior = flingBehavior,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(paddedItems.size) { index ->
                        val item = paddedItems[index]
                        if (item != null) {
                            // 2. Continuous 3D Effect calculation
                            val scaleAndAlpha = remember {
                                derivedStateOf {
                                    val layoutInfo = listState.layoutInfo
                                    val visibleItemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
                                        ?: return@derivedStateOf 0.8f to 0.5f
                                    
                                    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                                    val itemCenter = visibleItemInfo.offset + visibleItemInfo.size / 2f
                                    val distanceFromCenter = abs(viewportCenter - itemCenter)
                                    
                                    // Normalize distance (0.0 at center, 1.0 at edge)
                                    val fraction = (distanceFromCenter / (itemHeight.value * 2)).coerceIn(0f, 1f)
                                    
                                    val scale = 1f - (fraction * 0.4f) // 1.0 down to 0.6
                                    val alpha = 1f - (fraction * 0.6f) // 1.0 down to 0.4
                                    scale to alpha
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(itemHeight)
                                    .graphicsLayer {
                                        // 3. Performance: Apply transforms in graphicsLayer
                                        scaleX = scaleAndAlpha.value.first
                                        scaleY = scaleAndAlpha.value.first
                                        alpha = scaleAndAlpha.value.second
                                    }
                                    .clickable {
                                        // 4. Tap to select
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(index - (visibleItemsCount / 2))
                                        }
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                item.imageRes?.let {
                                    Image(
                                        painter = painterResource(id = it),
                                        contentDescription = item.text,
                                        modifier = Modifier.size(100.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(itemHeight))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                Button(
                    onClick = {
                        val centerIndex = listState.firstVisibleItemIndex + (visibleItemsCount / 2)
                        val selectedItem = paddedItems.getOrNull(centerIndex)
                        if (selectedItem != null) onItemSelected(selectedItem)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.select))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WheelSelectionDialogPreview() {
    val items = listOf(
        WheelItem(0, "Outdoor", R.drawable.outdoor_tile),
        WheelItem(1, "OpenTopo", R.drawable.opentopo_tile),
        WheelItem(2, "Thunderforest", R.drawable.thunderforest_tile),
        WheelItem(3, "PhoneMaps", R.drawable.phonemaps_tile)
    )
    RamaniTheme {
        WheelSelectionContent(
            onDismissRequest = {},
            onItemSelected = {},
            items = items,
            initialSelection = items[1]
        )
    }
}
