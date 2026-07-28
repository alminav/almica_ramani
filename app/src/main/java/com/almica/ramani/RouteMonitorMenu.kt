package com.almica.ramani

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AreaChart
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.almica.ramani.charts.theme.Black

enum class RouteMonitorSelection {
    Elevation,
    Gradient,
    Save,
    Remove,
    Reverse,
    SrtmRefresh,
    Nothing
}
@Composable
fun RouteMonitorMenu(context: Context, finished: (RouteMonitorSelection) -> Unit) {
    Surface(
        Modifier
            .fillMaxWidth()
            .padding(top = 250.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.align(Alignment.CenterEnd)) {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { finished(RouteMonitorSelection.Nothing) }
                ) {
                    DropdownMenuItem(
                        { Text(text = context.getString(R.string.gradient), color = Black) },
                        onClick = { finished(RouteMonitorSelection.Gradient) },
                        leadingIcon = {Icon (Icons.Outlined.BarChart, null)}
                    )
                    DropdownMenuItem(
                        { Text(text = context.getString(R.string.elevation), color = Black) },
                        onClick = { finished(RouteMonitorSelection.Elevation) },
                        leadingIcon = {Icon (Icons.Outlined.AreaChart, null)}
                    )
                    DropdownMenuItem(
                        { Text(text = context.getString(R.string.save_route), color = Black) },
                        onClick = { finished(RouteMonitorSelection.Save) },
                        leadingIcon = {Icon (Icons.Outlined.Save, null)}
                    )
                    DropdownMenuItem(
                        { Text(text = context.getString(R.string.remove), color = Black) },
                        onClick = { finished(RouteMonitorSelection.Remove) },
                        leadingIcon = {Icon(Icons.Outlined.Remove, null)}
                    )
                    DropdownMenuItem(
                        { Text(text = context.getString(R.string.reverse), color = Black) },
                        onClick = { finished(RouteMonitorSelection.Reverse) },
                        leadingIcon = {Icon(Icons.Outlined.Repeat, null)}
                    )
                    DropdownMenuItem(
                        { Text(text = context.getString(R.string.srtm_refresh), color = Black) },
                        onClick = { finished(RouteMonitorSelection.SrtmRefresh) },
                        leadingIcon = {Icon(Icons.Outlined.BarChart, null)}
                    )

                }
            }
        }
    }
}
