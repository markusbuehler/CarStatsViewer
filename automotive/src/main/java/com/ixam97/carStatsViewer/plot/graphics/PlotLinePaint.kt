package com.mbuehler.carStatsViewer.plot.graphics

import com.mbuehler.carStatsViewer.plot.enums.PlotSecondaryDimension

class PlotLinePaint(
    private val primary : PlotPaint,
    private val secondaryNormal : PlotPaint,
    private val secondaryAlternative : PlotPaint,
    private var useSecondaryAlternative: () -> Boolean
) {
    fun bySecondaryDimension(secondaryDimension: PlotSecondaryDimension?) : PlotPaint {
        return when {
            secondaryDimension != null -> when {
                useSecondaryAlternative.invoke() -> secondaryAlternative
                else -> secondaryNormal
            }
            else -> primary
        }
    }
}