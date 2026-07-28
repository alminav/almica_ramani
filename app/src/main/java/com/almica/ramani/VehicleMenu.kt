package com.almica.ramani

import android.content.Context
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.almica.ramani.utils.GhHelper
import timber.log.Timber

private const val logtag = "VehicleMenu"

@Composable
fun VehicleMenu(context: Context, finished: () -> Unit) {
    val vehicleCodes = listOf("0.1", "1.1", "1.0", "2.1", "2.0", "3.1")
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val currentCode = preferences.getString(
        context.getString(R.string.setting_locomotion),
        Const.DEFAULT_LOCOMOTION
    )
    val currentId = currentCode?.let { GhHelper.Companion.getVehicleIcon(context, it) }
    Timber.i("$currentCode $currentId")
    DropdownMenu(
        expanded = true,
        onDismissRequest = { finished() }
    ) {
        for (code in vehicleCodes) {
            DropdownMenuItem(
                trailingIcon = {
                    if (code == currentCode) Icon(
                        Icons.Outlined.Check,
                        null
                    )
                },
                text = {
                    GhHelper.getVehicleDescription(context, code)
                        ?.let { Text(text = it) }
                },
                leadingIcon = {
                    Icon(
                        painterResource(GhHelper.getVehicleIcon(context, code)),
                        null
                    )
                },
                onClick = {
                    //menuSelect = 4
                    preferences.edit {
                        putString(
                            context.getString(R.string.setting_locomotion),
                            code
                        )
                    }
                    finished()
                }
            )
        }
    }
}