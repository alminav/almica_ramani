package com.almica.composecharts.charts.bar

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.almica.composecharts.charts.bar.render.bar.IBarDrawer
import com.almica.composecharts.charts.bar.render.bar.SimpleBarDrawer
import com.almica.composecharts.charts.bar.render.label.ILabelDrawer
import com.almica.composecharts.charts.bar.render.label.SimpleLabelDrawer
import com.almica.composecharts.charts.bar.render.xaxis.IXAxisDrawer
import com.almica.composecharts.charts.bar.render.xaxis.SimpleXAxisDrawer
import com.almica.composecharts.charts.bar.render.yaxis.IYAxisDrawer
import com.almica.composecharts.charts.bar.render.yaxis.SimpleYAxisDrawer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.almica.composecharts.charts.simpleChartAnimation
import androidx.compose.ui.tooling.preview.Preview
import timber.log.Timber

/**
 * Created by bytebeats on 2021/9/25 : 15:56
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

@Composable
fun BarChartAdjustableAnimation(
    barChartData: BarChartData,
    modifier: Modifier = Modifier,
    barDrawer: IBarDrawer = SimpleBarDrawer(),
    xAxisDrawer: IXAxisDrawer = SimpleXAxisDrawer(),
    yAxisDrawer: IYAxisDrawer = SimpleYAxisDrawer(labelValueFormatter = { value -> if (value == 0F) "m" else "%.0f".format(value) }),
    labelDrawer: ILabelDrawer = SimpleLabelDrawer(),
    routePointer: Int,
    locationTime: Long,
    animated: Boolean
) {
//    Log.i(logtag, "${Thread.currentThread().getStackTrace()[2].lineNumber}: routePointer:$routePointer locationId:$locationId")
    //val transitionAnimation = remember(barChartData.bars) {
    val transitionAnimation = remember(locationTime) {
        //Animatable(initialValue = 0F)
        Animatable(initialValue = if (animated) 0F else 1F) // 1F no animation
    }

    //LaunchedEffect(barChartData.bars) {
    LaunchedEffect(locationTime) {
        //transitionAnimation.animateTo(1F, animationSpec = repeatableChartAnimation())
        transitionAnimation.animateTo(1F, animationSpec = simpleChartAnimation())
    }

    val progress = transitionAnimation.value
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val (xAxisArea, yAxisArea) = axisAreas(
                        drawScope = this,
                        totalSize = size,
                        xAxisDrawer = xAxisDrawer,
                        labelDrawer = labelDrawer
                    )

                    val barDrawableArea = barDrawableArea(xAxisArea)

                    yAxisDrawer.drawAxisLine(
                        drawScope = this,
                        canvas = canvas,
                        drawableArea = yAxisArea
                    )

                    xAxisDrawer.drawXAxisLine(
                        drawScope = this,
                        canvas = canvas,
                        drawableArea = xAxisArea
                    )

                    barChartData.forEachWithArea(
                        this,
                        barDrawableArea,
                        progress,
                        routePointer,
                        labelDrawer
                    ) { barArea, bar, barBorderArea ->
                        // 20aug2026 sliderposition shows routePointer
//                        if (barBorderArea != Rect.Zero)
//                            barDrawer.drawBar(drawScope = this, canvas, barBorderArea, bar.copy(color = Color.Black))
                        barDrawer.drawBar(drawScope = this, canvas, barArea, bar)
                    }
                }
            }
    ) {

        drawIntoCanvas { canvas ->
            val (xAxisArea, yAxisArea) = axisAreas(
                drawScope = this,
                totalSize = size,
                xAxisDrawer = xAxisDrawer,
                labelDrawer = labelDrawer
            )
            val barDrawableArea = barDrawableArea(xAxisArea)

            barChartData.forEachWithArea(
                this,
                barDrawableArea,
                progress,
                routePointer,
                labelDrawer
            ) { barArea, bar, _ ->
                //Timber.i("labelDrawer bar: $bar")
                labelDrawer.drawLabel(
                    drawScope = this,
                    canvas = canvas,
                    label = bar.label,
                    barArea = barArea,
                    xAxisArea = xAxisArea
                )
            }

            yAxisDrawer.drawAxisLabels(
                drawScope = this,
                canvas = canvas,
                minValue = barChartData.minY,
                maxValue = barChartData.maxY,
                drawableArea = yAxisArea
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 300)
@Composable
fun BarChartAdjustableAnimationPreview() {
    val barChartData = remember {
        BarChartData(
            bars = listOf(
                BarChartData.Bar(10f, Color.Red, "B1"),
                BarChartData.Bar(20f, Color.Green, "B2"),
                BarChartData.Bar(15f, Color.Blue, "B3"),
                BarChartData.Bar(30f, Color.Yellow, "B4"),
                BarChartData.Bar(25f, Color.Cyan, "B5"),
            )
        )
    }
    BarChartAdjustableAnimation(
        barChartData = barChartData,
        modifier = Modifier.size(300.dp),
        routePointer = 2,
        locationTime = 0L,
        animated = false
    )
}
