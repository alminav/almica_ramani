package com.almica.ramani.charts

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.almica.ramani.charts.theme.Green900
import com.almica.ramani.charts.theme.LightGreen600
import com.almica.ramani.utils.format
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber


private const val logtag = "LineGraphSpeed"
@Composable
internal fun LineGraphSpeed(
    lines: Pair<List<DataPointWithDist>, List<DataPointWithDist>>,
    inverseXAxis: Boolean,
    onSelect: (String, LatLng?, Long) -> Unit
) {
    val graphSteps = 10
    val xAxisInvers = LinePlot.XAxis(steps = graphSteps) { min, offset, max -> // inverted x-values
        for (it in 0 until graphSteps) {
            val value = it * offset + min
            Text(
                text = (1 + max - value).toInt().toString(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface
            )
            if (value > max) {
                break
            }
        }
    }
    var speed by remember { mutableStateOf("") }
    var latLng by remember { mutableStateOf<LatLng?>(null) }
    LineGraph(
        onSelection = { offset, lines ->
            Timber.i("offset:$offset")
            speed = lines[0].y.format(1) + " km/h"
            latLng = lines[0].geoLocation
            onSelect(speed, latLng, lines[0].time)
        },
        plot = LinePlot(
            listOf(
                LinePlot.Line(
                    lines.first,
                    LinePlot.Connection(LightGreen600, 1.dp),
                    null, //LinePlot.Intersection(LightGreen600, 5.dp),
                    LinePlot.Highlight(Green900, 3.dp),
                    LinePlot.AreaUnderLine(LightGreen600, 0.3f)
                ), LinePlot.Line(
                    lines.second,
                    LinePlot.Connection(Color.Gray, 2.dp),
                    null,
                ),
            ), xAxis = if (inverseXAxis) xAxisInvers else LinePlot.XAxis(steps = graphSteps),
            selection = LinePlot.Selection(
                highlight = LinePlot.Connection(
                    Green900,
                    strokeWidth = 2.dp,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f, 20f))
                )
            ),
        ),
        modifier = Modifier
            //.offset(200.dp)
            //.padding(paddingValues)
            .fillMaxWidth()
            //.fillMaxHeight(0.25f)
            .height(240.dp)
    )
}
