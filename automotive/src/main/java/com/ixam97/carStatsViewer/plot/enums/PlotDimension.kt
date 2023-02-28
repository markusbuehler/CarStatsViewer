package com.mbuehler.carStatsViewer.plot.enums

enum class PlotDimension {
    INDEX, DISTANCE, TIME, STATE_OF_CHARGE;

    fun toPlotDirection(): PlotDirection {
        return when (this) {
            TIME, STATE_OF_CHARGE -> PlotDirection.LEFT_TO_RIGHT
            else -> PlotDirection.RIGHT_TO_LEFT
        }
    }
}