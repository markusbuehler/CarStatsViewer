package com.mbuehler.carStatsViewer.dataManager

import com.mbuehler.carStatsViewer.plot.objects.PlotLineItem
import com.mbuehler.carStatsViewer.plot.objects.PlotMarker
import java.util.*

data class TripData(
    var appVersion: String,
    var dataVersion: Int?,
    var tripStartDate: Date,
    var chargeStartDate: Date,
    var usedEnergy: Float,
    var traveledDistance: Float,
    var travelTime: Long,
    var chargedEnergy: Float,
    var chargeTime: Long,
    var consumptionPlotLine: List<PlotLineItem>,
    var chargePlotLine: List<PlotLineItem>,
    var chargeCurves: List<ChargeCurve>,
    var markers: List<PlotMarker>
)
