package com.almica.composecharts.charts.bar

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
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

/**
 * Created by bytebeats on 2021/9/25 : 15:56
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

private const val logtag = "BarChartWithoutAnimation"
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
//    Log.i(logtag, "${Thread.currentThread().getStackTrace()[2].lineNumber}: progress:$progress")
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
                        if (barBorderArea != Rect.Zero)
                            barDrawer.drawBar(drawScope = this, canvas, barBorderArea, bar.copy(color = Color.Black))
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
            ) { barArea, bar, _, ->
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
