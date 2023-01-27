package com.mbuehler.carStatsViewer.objects

import com.mbuehler.carStatsViewer.plot.PlotLineItem

data class ChargeCurve(
    var chargePlotLine: List<PlotLineItem>,
    var stateOfChargePlotLine: List<PlotLineItem>,
    var startTimeNanos: Long,
    var endTimeNanos: Long,
    var chargedEnergyWh: Float,
    var maxChargeRatemW: Float,
    var avgChargeRatemW: Float
) {}