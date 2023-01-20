package com.mbuehler.carStatsViewer.objects

import com.mbuehler.carStatsViewer.plot.PlotLine
import com.mbuehler.carStatsViewer.plot.PlotLineItem
import java.util.*
import kotlin.collections.ArrayList

data class TripData(
    var saveDate: Date,
    var traveledDistance: Float,
    var usedEnergy: Float,
    var averageConsumption: Float,
    var travelTimeMillis: Long,
    var consumptionPlotLine: List<PlotLineItem>,
    var speedPlotLine: List<PlotLineItem>,
    var chargeCurves: List<DataHolder.ChargeCurve>
) {

}
