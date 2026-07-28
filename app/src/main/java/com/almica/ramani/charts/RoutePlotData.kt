package com.almica.ramani.charts

import co.yml.charts.common.model.PlotData
import co.yml.charts.common.model.PlotType

/**
 * Clone of co.yml.charts.ui.linechart.model.LinePlotData
 * LinePlotData is a data class that holds line graph related data and styling components
 * @param plotType : Defines the type of plot/graph
 * @param lines : Data related to the list of lines to be drawn.
 */
data class RoutePlotData(
    override val plotType: PlotType = PlotType.Line,
    val lines: List<RouteLine>
) : PlotData {
    companion object {
        fun default() =
            RoutePlotData(lines = listOf())
    }
}
