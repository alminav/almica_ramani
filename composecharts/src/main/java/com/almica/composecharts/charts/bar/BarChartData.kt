package com.almica.composecharts.charts.bar

import androidx.compose.ui.graphics.Color
import com.almica.composecharts.charts.util.FLOAT_100
import timber.log.Timber

/**
 * Created by bytebeats on 2021/9/25 : 13:52 E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
data class BarChartData(
    val bars: List<Bar>,
    val padBy: Float = 10F,
    val startAtZero: Boolean = true,
    val maxBarValue: Float = bars.maxOf { it.value }
) {

    init {
        require(padBy in 0F..FLOAT_100) {
            "padBy must be between 0F and 100F, included"
        }
        Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: maxBarValue: $maxBarValue")
        val barsMaxOf = bars.maxOf { it.value }
        Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: barsMaxOf: $barsMaxOf")
        require(maxBarValue >= bars.maxOf { it.value }) {
            "maxBarValue must be at least the value of the highest bar"
        }
    }

    private val yMinMaxValues: Pair<Float, Float>
        get() {
            val minValue = bars.minOf { it.value }
            val maxValue = maxBarValue
            return minValue to maxValue
        }

    val maxY: Float
        get() = yMinMaxValues.second + (yMinMaxValues.second - yMinMaxValues.first) * padBy / FLOAT_100
    val minY: Float
        get() = if (startAtZero) 0F
        else yMinMaxValues.first - (yMinMaxValues.second - yMinMaxValues.first) * padBy / FLOAT_100

    data class Bar(
        val value: Float,
        val color: Color,
        val label: String
    )
}
