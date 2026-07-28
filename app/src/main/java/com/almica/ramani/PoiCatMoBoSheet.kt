package com.almica.ramani

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.almica.ramani.ui.theme.RamaniTheme
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiCatMoBoSheet(presetName: String, callback: (name: String, category: String?) -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var placemarkName by remember { mutableStateOf(presetName) }
    val catMap = Helpers.getPoiDrawableMapWithoutSpecials(context)
    val keys = ArrayList<String>()
    catMap.forEach { item ->
        keys.add(item.key)
    }
    var popupSnackMsg: String? by remember { mutableStateOf(null) }
    LaunchedEffect(key1 = popupSnackMsg) {
        Timber.i( "LaunchedEffect $popupSnackMsg")
        delay(3000.milliseconds)
        popupSnackMsg = null
    }

    ModalBottomSheet(
        onDismissRequest = { callback(placemarkName, null) },
        modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection())
    ) {
        popupSnackMsg?.let { msg ->
            Popup(properties = PopupProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
                alignment = Alignment.TopCenter,
                onDismissRequest = {
                    popupSnackMsg = null
                }) {
                Surface(
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 4.dp,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
            OutlinedButton(
                border = BorderStroke(0.dp, Color.Transparent),
                onClick = { callback(placemarkName, null) }) {
                Row {
                    //Icon(Icons.Outlined.Close, contentDescription = null)
                    //Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        //modifier = Modifier.align(alignment = Alignment.CenterVertically),
                        fontSize = 16.sp,
                        text = stringResource(R.string.add_placemark), textAlign = TextAlign.Center,
                        color = Color.Black
                    )
                }
            }
            OutlinedTextField(
                value = placemarkName,
                onValueChange = {
                    placemarkName = it
                    Timber.i( "placemarkName $placemarkName") },
                label = { Text(stringResource(R.string.placemark_name)) },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
            )
        }
        LazyColumn() {
            items(keys) { key ->
                OutlinedButton(
                    border = BorderStroke(0.dp, Color.Transparent),
                    onClick = {
                        if (placemarkName.isNotEmpty() && placemarkName != Const.UNKNOWN)
                            callback(placemarkName, key)
                        else
                            popupSnackMsg = resources.getString(R.string.enter_placemark_name)
                    }) {
                    Row {
                        catMap[key]?.let {
                            Image(
                                painter = painterResource(id = it.first),
                                contentDescription = key
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        catMap[key]?.let {
                            Text(
                                modifier = Modifier.align(alignment = Alignment.CenterVertically),
                                fontSize = 16.sp,
                                text = if (it.second > 0) stringResource(it.second) else key,
                                color = Color.Black
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
fun PoiCatMoBoSheetPreview() {
    RamaniTheme {
        PoiCatMoBoSheet(
            presetName = "Sample Location",
            callback = { _, _ -> }
        )
    }
}
