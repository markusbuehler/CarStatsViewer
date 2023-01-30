package com.mbuehler.carStatsViewer.objects

import com.mbuehler.carStatsViewer.plot.objects.PlotLineItem

data class ChargeCurve(
    var chargePlotLine: List<PlotLineItem>,
    var stateOfChargePlotLine: List<PlotLineItem>?,
    var chargeTime: Long,
    var chargedEnergyWh: Float,
    var maxChargeRatemW: Float,
    var avgChargeRatemW: Float
) {}