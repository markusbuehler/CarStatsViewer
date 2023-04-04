package com.mbuehler.carStatsViewer.plot.objects

import com.mbuehler.carStatsViewer.enums.DistanceUnitEnum
import com.mbuehler.carStatsViewer.plot.enums.*

object PlotGlobalConfiguration {
    val DataVersion : Int? = 20230206

    val DimensionYConfiguration: HashMap<PlotDimensionY, PlotLineConfiguration> =
        hashMapOf(
            PlotDimensionY.SPEED to PlotLineConfiguration(
                PlotRange(0f, 40f, 0f, 240f, 40f),
                PlotLineLabelFormat.NUMBER,
                PlotHighlightMethod.AVG_BY_TIME,
                "km/h"
            ),
            PlotDimensionY.DISTANCE to PlotLineConfiguration(
                PlotRange(),
                PlotLineLabelFormat.DISTANCE,
                PlotHighlightMethod.NONE,
                "km"
            ),
            PlotDimensionY.TIME to PlotLineConfiguration(
                PlotRange(),
                PlotLineLabelFormat.TIME,
                PlotHighlightMethod.MAX,
                "Time"
            ),
            PlotDimensionY.STATE_OF_CHARGE to PlotLineConfiguration(
                PlotRange(0f, 100f, backgroundZero = 0f),
                PlotLineLabelFormat.PERCENTAGE,
                PlotHighlightMethod.LAST,
                "% SoC",
                DimensionSmoothing = 1f,
                DimensionSmoothingType = PlotDimensionSmoothingType.PIXEL,
                DimensionSmoothingHighlightMethod = PlotHighlightMethod.MAX,
                SessionGapRendering = PlotSessionGapRendering.GAP
            ),
            PlotDimensionY.ALTITUDE to PlotLineConfiguration(
                PlotRange(smoothAxis = 20f),
                PlotLineLabelFormat.ALTITUDE,
                PlotHighlightMethod.AVG_BY_TIME,
                "m",
                SessionGapRendering = PlotSessionGapRendering.GAP
            )
        )

    fun updateDistanceUnit(distanceUnit: DistanceUnitEnum) {

        DimensionYConfiguration[PlotDimensionY.SPEED]?.UnitFactor = distanceUnit.asFactor()
        DimensionYConfiguration[PlotDimensionY.SPEED]?.Divider = distanceUnit.asFactor()
        DimensionYConfiguration[PlotDimensionY.SPEED]?.Unit = "%s/h".format(distanceUnit.unit())

        DimensionYConfiguration[PlotDimensionY.DISTANCE]?.UnitFactor = distanceUnit.toFactor()
        DimensionYConfiguration[PlotDimensionY.DISTANCE]?.Divider = distanceUnit.asFactor()
        DimensionYConfiguration[PlotDimensionY.DISTANCE]?.Unit = "%s".format(distanceUnit.unit())

        DimensionYConfiguration[PlotDimensionY.ALTITUDE]?.UnitFactor = distanceUnit.asSubFactor()
        DimensionYConfiguration[PlotDimensionY.ALTITUDE]?.Unit = "%s".format(distanceUnit.subUnit())
    }
}