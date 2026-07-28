package com.almica.ramani

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
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.almica.ramani.charts.theme.Orange
import com.almica.ramani.charts.theme.Red500
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber

@Composable
fun MainRecalcButton(
    recalc: () -> Unit
) {
    Box {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.fillMaxHeight(0.3f))
            Row {
                Spacer(modifier = Modifier.weight(0.9F))
                    IconButton(
                        onClick = {
                            Timber.i("restart")
                            recalc()
                        },
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .clip(CircleShape)
                            .width(36.dp)
                            .height(36.dp)
                            .border(1.dp, Red500, CircleShape)
                            .background(Orange)
                    ) {
                        Icon(
                            Icons.Outlined.Navigation,
                            null
                        )
                    }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainRecalcButtonPreview() {
    RamaniTheme {
        MainRecalcButton(
            recalc = {}
        )
    }
}
