package com.almica.ramani

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.charts.theme.Black
import com.almica.ramani.utils.isNotNull
import timber.log.Timber

@Composable
fun MainMvtMapSwitchButton(mapSwitchOption: String?,
                           mainSnackbarData: (MainSnackbarData) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val preferences = getDefaultSharedPreferences(context)
    Box {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.fillMaxHeight(0.75f))
            AnimatedVisibility(visible = mapSwitchOption.isNotNull()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(text = resources.getString(R.string.restart),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End)
                }
            }
            Row {
                Spacer(modifier = Modifier.weight(0.9F))
                AnimatedVisibility(visible = mapSwitchOption.isNotNull()) {
                    IconButton(
                        onClick = {
                            Timber.i("mapSwitchOption $mapSwitchOption")
                            preferences.edit { putString(Const.PREF_MVT_FILEPATH, mapSwitchOption)}
                            mainSnackbarData(MainSnackbarData(
                                resources.getString(R.string.askfor_app_restart),
                                resources.getString(android.R.string.ok),
                                MainSnackbarSelection.AppRestart, null)
                            )
                        },
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .clip(CircleShape)
                            .width(32.dp)
                            .height(32.dp)
                            .border(1.dp, Black, CircleShape)
                            .background(colorResource(R.color.teal_200_trans))
                    ) {
                        Icon(
                            Icons.Outlined.Map,
                            null
                        )
                    }
                }
            }
        }
    }
}