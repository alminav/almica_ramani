package com.almica.ramani

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.almica.ramani.routes.RouteEntity

@Composable
fun PdfRoutesDropdownMenu(
    pdfRoutes: List<RouteEntity>?,
    finish: () -> Unit,
    routeSelection: (String?, String?) -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = { finish() }
    ) {
        pdfRoutes?.forEachIndexed { index, entity ->
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Route,
                        null
                    )
                },
                text = {
                    Text(
                        text = "${entity.region} - ${entity.name}"
                    )
                },
                onClick = {
                    routeSelection(entity.name, entity.region)
                }
            )
            if (index < pdfRoutes.size - 1) {
                HorizontalDivider()
            }
        }
    }
}
