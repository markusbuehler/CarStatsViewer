package com.mbuehler.carStatsViewer.ui.plot.objects

import com.mbuehler.carStatsViewer.ui.plot.enums.PlotDimensionSmoothingType
import com.mbuehler.carStatsViewer.ui.plot.enums.PlotHighlightMethod
import com.mbuehler.carStatsViewer.ui.plot.enums.PlotLineLabelFormat
import com.mbuehler.carStatsViewer.ui.plot.enums.PlotSessionGapRendering

class PlotLineConfiguration(
    internal val Range: PlotRange,
    var LabelFormat: PlotLineLabelFormat,
    var HighlightMethod: PlotHighlightMethod,
    var Unit: String,
    var Divider: Float = 1f,
    var UnitFactor: Float = 1f,
    var DimensionSmoothing: Float? = null,
    var DimensionSmoothingType: PlotDimensionSmoothingType? = null,
    var DimensionSmoothingHighlightMethod: PlotHighlightMethod? = null,
    var SessionGapRendering : PlotSessionGapRendering? = null
)