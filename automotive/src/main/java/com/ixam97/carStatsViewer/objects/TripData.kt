package com.mbuehler.carStatsViewer.objects

import com.mbuehler.carStatsViewer.plot.PlotLine
import com.mbuehler.carStatsViewer.plot.PlotLineItem
import com.mbuehler.carStatsViewer.plot.PlotLineMarkerType
import com.mbuehler.carStatsViewer.plot.PlotMarker
import com.mbuehler.carStatsViewer.plot.PlotMarkers
import java.util.*

data class TripData(
    var appVersion: String,
    var saveDate: Date,
    var traveledDistance: Float,
    var usedEnergy: Float,
    var averageConsumption: Float,
    var travelTimeMillis: Long,
    var lastPlotDistance: Float,
    var lastPlotEnergy: Float,
    var lastPlotTime: Long,
    var lastPlotGear: Int,
    var lastPlotMarker: PlotLineMarkerType?,
    var lastChargePower:Float,
    var consumptionPlotLine: List<PlotLineItem>,
    var speedPlotLine: List<PlotLineItem>,
    var chargeCurves: List<ChargeCurve>,
    var markers: List<PlotMarker>
) {

}
