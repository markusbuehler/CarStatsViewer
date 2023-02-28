package com.mbuehler.carStatsViewer.plot.objects

import com.mbuehler.carStatsViewer.plot.enums.*

class PlotLineConfiguration(
    internal val Range: PlotRange,
    var LabelFormat: PlotLineLabelFormat,
    var LabelPosition: PlotLabelPosition,
    var HighlightMethod: PlotHighlightMethod,
    var Unit: String,
    var Divider: Float = 1f,
    var UnitFactor: Float = 1f
)