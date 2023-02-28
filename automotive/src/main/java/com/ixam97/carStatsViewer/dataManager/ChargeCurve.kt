package com.mbuehler.carStatsViewer.dataManager

import com.mbuehler.carStatsViewer.plot.objects.PlotLineItem
import java.util.*

data class ChargeCurve(
    val chargePlotLine: List<PlotLineItem>,
    val chargeTime: Long,
    val chargedEnergy: Float,
    val ambientTemperature: Float? = null,
    val chargeStartDate: Date? = null
) {}