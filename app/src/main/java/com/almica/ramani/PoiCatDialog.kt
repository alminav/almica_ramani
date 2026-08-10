package com.almica.ramani

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.WheelItem
import com.almica.ramani.utils.WheelSelectionContent
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * A Dialog for selecting a POI category and naming a placemark.
 *
 * @param presetName Initial name for the placemark.
 * @param callback Called when a category is selected or the sheet is dismissed.
 */
@Composable
fun PoiCatDialog(
    presetName: String,
    callback: (name: String, category: String?) -> Unit
) {
    Timber.i("PoiCatDialog $presetName")
    val context = LocalContext.current
    var placemarkName by rememberSaveable { mutableStateOf(presetName) }

    var hasFocusedOnce by rememberSaveable { mutableStateOf(false) }
    // Remember heavy computations to avoid re-calculating on every recomposition
    val catMap = remember(context) {
        Helpers.getPoiDrawableMapWithoutSpecials(context)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val enterNameMessage = stringResource(R.string.enter_placemark_name)

    // Reactive validation logic
    val isNameInvalid = remember(placemarkName) {
        placemarkName.isBlank() || placemarkName == Const.UNKNOWN
    }

    Dialog(
        onDismissRequest = { callback(placemarkName, null) }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Scaffold(
                modifier = Modifier.fillMaxWidth(),
                snackbarHost = {
                    // Wrap SnackbarHost in a Box to ensure the snackbarHost slot 
                    // always produces at least one layout node. This avoids a 
                    // NoSuchElementException in some versions of Material 3 Scaffold
                    // that call .first() on the snackbar placeables list.
                    Box(Modifier.fillMaxWidth()) {
                        SnackbarHost(hostState = snackbarHostState)
                    }
                },
                containerColor = Color.Transparent
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.add_placemark),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = placemarkName,
                        onValueChange = { placemarkName = it },
                        label = { Text(stringResource(R.string.placemark_name)) },
                        isError = isNameInvalid,
                        supportingText = {
                            if (isNameInvalid) {
                                Text(enterNameMessage)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && !hasFocusedOnce) {
                                    placemarkName = ""
                                    hasFocusedOnce = true
                                }
                            },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PoicatWheel(
                        poicatMap = catMap,
                        finished = { callback(placemarkName, null) },
                        select = { wheelItem ->
                            val categoryKey = catMap.keys.elementAtOrNull(wheelItem.id)
                            if (!isNameInvalid) {
                                callback(placemarkName, categoryKey)
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(enterNameMessage)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PoicatWheel(
    poicatMap: Map<String, Pair<Int, Int>>,
    finished: () -> Unit,
    select: (WheelItem) -> Unit,
    initialSelection: Int = 2
) {
    val wheelItems = remember(poicatMap) {
        poicatMap.toList().mapIndexed { index, (key, data) ->
            val (iconRes, _) = data
            WheelItem(
                id = index,
                text = key, // The key from map is already translated or appropriate
                imageRes = iconRes
            )
        }
    }
    WheelSelectionContent(
        onDismissRequest = { finished() },
        onItemSelected = { wheelItem -> select(wheelItem) },
        items = wheelItems,
        initialSelection = wheelItems.getOrNull(initialSelection),
        visibleItemsCount = 5,
        itemHeight = 56.dp,
        title = stringResource(R.string.categories),
        iconSize = 30.dp
    )
}

@Preview(showBackground = true)
@Composable
fun PoiCatDialogPreview() {
    RamaniTheme {
        PoiCatDialog(
            presetName = "Sample Location",
            callback = { _, _ -> }
        )
    }
}
