package com.almica.ramani

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.almica.ramani.charts.theme.Red500
import org.maplibre.android.maps.MapLibreMap
import timber.log.Timber

@Composable
fun MainRestartButton(map: MapLibreMap?, appRestartRequired: Boolean, restart: () -> Unit
) {
    Box {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.fillMaxHeight(0.2f))
            Row {
                Spacer(modifier = Modifier.weight(0.9F))
                AnimatedVisibility(visible = (map == null).or(appRestartRequired)) {
                    IconButton(
                        onClick = {
                            Timber.i("restart")
                            restart()
                        },
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .clip(CircleShape)
                            .width(32.dp)
                            .height(32.dp)
                            .border(2.dp, Red500, CircleShape)
                            .background(Red500)
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            null
                        )
                    }
                }
            }
        }
    }
}