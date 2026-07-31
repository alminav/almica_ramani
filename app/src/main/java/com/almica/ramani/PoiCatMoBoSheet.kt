package com.almica.ramani

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.almica.ramani.ui.theme.RamaniTheme
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * A Bottom Sheet for selecting a POI category and naming a placemark.
 *
 * @param presetName Initial name for the placemark.
 * @param callback Called when a category is selected or the sheet is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiCatMoBoSheet(
    presetName: String,
    callback: (name: String, category: String?) -> Unit
) {
    Timber.i("PoiCatMoBoSheet $presetName")
    val context = LocalContext.current
    var placemarkName by remember { mutableStateOf(presetName) }
    
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

    ModalBottomSheet(
        onDismissRequest = { callback(placemarkName, null) },
        modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header action
                TextButton(
                    onClick = { callback(placemarkName, null) }
                ) {
                    Text(
                        text = stringResource(R.string.add_placemark),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = placemarkName,
                    onValueChange = { placemarkName = it },
                    label = { Text(stringResource(R.string.placemark_name)) },
                    isError = isNameInvalid,
                    supportingText = {
                        if (isNameInvalid) {
                            Text(stringResource(R.string.enter_placemark_name))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Iterate directly over map entries to avoid redundant lookups
                    items(catMap.toList()) { (key, data) ->
                        val (iconRes, labelRes) = data
                        
                        Surface(
                            onClick = {
                                if (!isNameInvalid) {
                                    callback(placemarkName, key)
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(enterNameMessage)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = key
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = if (labelRes > 0) stringResource(labelRes) else key,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Snackbar for transient messages
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PoiCatMoBoSheetPreview() {
    RamaniTheme {
        PoiCatMoBoSheet(
            presetName = "Sample Location",
            callback = { _, _ -> }
        )
    }
}
