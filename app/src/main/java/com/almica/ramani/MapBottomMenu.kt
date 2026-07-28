package com.almica.ramani

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MapsHomeWork
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.almica.ramani.ui.theme.RamaniTheme

enum class ActionMapBottomMenu {
    Home,
    LayersControlFunctions,
    ManageAdditionalMaps,
    Preferences
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapBottomMenu(
    onAction: (ActionMapBottomMenu) -> Unit,
    onDismissRequest: () -> Unit = { onAction(ActionMapBottomMenu.Home) }
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        MapBottomMenuContent(onAction = onAction)
    }
}

@Composable
fun MapBottomMenuContent(
    onAction: (ActionMapBottomMenu) -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val preferences = remember {
        if (isPreview) null else PreferenceManager.getDefaultSharedPreferences(context)
    }

    var keepScreenOn by remember {
        mutableStateOf(
            preferences?.getBoolean(Const.PREF_KEEP_SCREEN_ON, true) ?: true
        )
    }
    var useStepCounter by remember {
        mutableStateOf(
            preferences?.getBoolean(Const.PREF_USE_STEPCOUNTER, false) ?: false
        )
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        MapMenuItem(
            icon = Icons.Outlined.MapsHomeWork,
            text = stringResource(R.string.additional_maps),
            onClick = { onAction(ActionMapBottomMenu.ManageAdditionalMaps) }
        )
        MapMenuItem(
            icon = Icons.Outlined.Layers,
            text = stringResource(R.string.layers_control),
            onClick = { onAction(ActionMapBottomMenu.LayersControlFunctions) }
        )
        MapMenuItem(
            icon = Icons.Outlined.Settings,
            text = stringResource(R.string.preferences),
            onClick = { onAction(ActionMapBottomMenu.Preferences) }
        )

        Spacer(modifier = Modifier.padding(8.dp))

        SettingsCard(
            title = stringResource(R.string.keep_screen_on),
            checked = keepScreenOn,
            onCheckedChange = { checked ->
                keepScreenOn = checked
                preferences?.edit {
                    putBoolean(Const.PREF_KEEP_SCREEN_ON, checked)
                }
            }
        )

        SettingsCard(
            title = stringResource(R.string.use_stepcounter),
            checked = useStepCounter,
            onCheckedChange = { checked ->
                useStepCounter = checked
                preferences?.edit {
                    putBoolean(Const.PREF_USE_STEPCOUNTER, checked)
                }
            }
        )
    }
}

@Composable
private fun MapMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null, // decorative, text provides context
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = text,
                fontSize = 16.sp,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapBottomMenuPreview() {
    RamaniTheme {
        MapBottomMenuContent(
            onAction = {}
        )
    }
}

