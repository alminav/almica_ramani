package com.almica.composecharts.charts.bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.almica.composecharts.charts.bar.render.bar.IBarDrawer
import com.almica.composecharts.charts.bar.render.bar.SimpleBarDrawer
import com.almica.composecharts.charts.bar.render.label.ILabelDrawer
import com.almica.composecharts.charts.bar.render.label.SimpleLabelDrawer
import com.almica.composecharts.charts.bar.render.xaxis.IXAxisDrawer
import com.almica.composecharts.charts.bar.render.xaxis.SimpleXAxisDrawer
import com.almica.composecharts.charts.bar.render.yaxis.IYAxisDrawer
import com.almica.composecharts.charts.bar.render.yaxis.SimpleYAxisDrawer
import com.almica.composecharts.charts.simpleChartAnimation

/**
 * Created by bytebeats on 2021/9/25 : 15:56
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

@Composable
fun BarChart(
    barChartData: BarChartData,
    modifier: Modifier = Modifier,
    animation: AnimationSpec<Float> = simpleChartAnimation(),
    barDrawer: IBarDrawer = SimpleBarDrawer(),
    xAxisDrawer: IXAxisDrawer = SimpleXAxisDrawer(),
    yAxisDrawer: IYAxisDrawer = SimpleYAxisDrawer(),
    labelDrawer: ILabelDrawer = SimpleLabelDrawer()
) {
    val transitionAnimation = remember(barChartData.bars) {
        Animatable(initialValue = 0F)
        //Animatable(initialValue = 1F) // no animation
    }

    LaunchedEffect(barChartData.bars) {
        transitionAnimation.animateTo(1F, animationSpec = animation)
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
                        -1,
                        labelDrawer
                    ) { barArea, bar, _ ->
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
                -1,
                labelDrawer
            ) { barArea, bar, _ ->
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
