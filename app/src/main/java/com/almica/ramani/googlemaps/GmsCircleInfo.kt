package com.almica.ramani.googlemaps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almica.ramani.Const
import com.almica.ramani.utils.format
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.formatDistM
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber

@Composable
fun GmsCircleInfo(circle: CircleInfo, showLocationStatistic: (Boolean) -> Unit) {
    Timber.i("circle: ${circle.name} ${circle.travelledTime}")
    Box {
        Box(
            Modifier
                .clickable(true, onClick = { showLocationStatistic(true) })
                .align(alignment = Alignment.TopEnd)
                .padding(top = 32.dp, start = 4.dp, end = 4.dp)
                .clip(RoundedCornerShape(20))
        ) {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .padding(6.dp)
            ) {
                Row {
                    Spacer(modifier = Modifier.weight(0.1f))
                    Text(
                        text = "${Const.UC_DISTANCE_ARROW} ${circle.name}",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.45f)
                    )
                    Text(
                        text = circle.travelledTime,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.45f)
                    )
                }
                Row {
                    Spacer(modifier = Modifier.weight(0.1f))
                    circle.altitude?.let {
                        Text(
                            text = "${Const.UC_ELE_ARROW} ${it.formatDistM(true)}",
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.45f)
                        )
                    }
                    circle.speed?.let {
                        Text(
                            text = "${it.format(1)} KmH",
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.45f)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GmsCircleInfoPreview() {
    RamaniTheme {
        val sampleCircle = CircleInfo(
            name = "Sample Circle",
            center = LatLng(0.0, 0.0),
            travelledTime = "12:34",
            time = 123456789L,
            altitude = 150.0,
            speed = 5.5f,
            avgSpeed = 5.0
        )
        GmsCircleInfo(circle = sampleCircle, showLocationStatistic = {})
    }
}
