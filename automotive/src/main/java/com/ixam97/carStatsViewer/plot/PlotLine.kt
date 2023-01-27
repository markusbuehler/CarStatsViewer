package com.mbuehler.carStatsViewer.plot

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class PlotLine(
    internal val Range: PlotRange,

    var Divider: Float,

    var LabelFormat: String,
    var HighlightFormat: String,

    var Unit: String,

    var LabelPosition: PlotLabelPosition,
    var HighlightMethod: PlotHighlightMethod,

    var Visible: Boolean = true
) {
    private val dataPoints: ConcurrentHashMap<Int, PlotLineItem> = ConcurrentHashMap()

    var baseLineAt: ArrayList<Float> = ArrayList()

    var plotPaint: PlotPaint? = null

    var alignZero: Boolean = false

    var zeroAt: Float? = null

    fun addDataPoint(item: Float, time: Long, distance: Float, timeDelta: Long? = null, distanceDelta: Float? = null, plotLineMarkerType: PlotLineMarkerType? = null) {
        val prev = dataPoints[dataPoints.size - 1]

        addDataPoint(PlotLineItem(
            item,
            time,
            distance,
            timeDelta?:(time - (prev?.Time ?: time)),
            distanceDelta?:(distance - (prev?.Distance ?: distance)),
            plotLineMarkerType
        ))
    }

    fun addDataPoint(dataPoint: PlotLineItem) {
        when {
            dataPoint.Value.isFinite() -> {
                dataPoints[dataPoints.size] = dataPoint
            }
            dataPoint.Marker == PlotLineMarkerType.END_SESSION -> {
                val lastPoint = dataPoints[dataPoints.size - 1]
                when {
                    lastPoint != null && lastPoint.Marker == null -> {
                        lastPoint.Marker = dataPoint.Marker
                    }
                }
            }
        }
    }

    fun addDataPoints(dataPoints: List<PlotLineItem>) {
        for (dataPoint in dataPoints) {
            addDataPoint(dataPoint)
        }
    }

    fun reset() {
        dataPoints.clear()
    }

    fun getDataPoints(dimension: PlotDimension, dimensionRestriction: Long? = null, dimensionShift: Long? = null): List<PlotLineItem> {
        return when {
            dataPoints.isEmpty() || dimensionRestriction == null -> dataPoints.map { it.value }
            else -> when (dimension) {
                PlotDimension.INDEX -> {
                    var max = dataPoints.size - 1 - (dimensionShift ?: 0L)
                    var min = max - dimensionRestriction

                    dataPoints.filter { it.key in min..max }.map { it.value }
                }
                PlotDimension.DISTANCE -> {
                    var max = dataPoints[dataPoints.size - 1]!!.Distance - (dimensionShift ?: 0L)
                    var min = max - dimensionRestriction

                    dataPoints.filter { it.value.Distance in min..max }.map { it.value }
                }
                PlotDimension.TIME -> {
                    var max = dataPoints[dataPoints.size - 1]!!.Time - (dimensionShift ?: 0L)
                    var min = max - dimensionRestriction

                    dataPoints.filter { it.value.Time in min..max }.map { it.value }
                }
            }
        }
    }

    internal fun minDimension(dataPoints: List<PlotLineItem>, dimension: PlotDimension, dimensionRestriction: Long?): Any {
        return when (dimension) {
            PlotDimension.INDEX -> 0f
            PlotDimension.DISTANCE -> when {
                dataPoints.isEmpty() -> return 0f
                else -> (maxDimension(dataPoints, dimension) as Float - (dimensionRestriction ?: 0L))
                    .coerceAtMost(dataPoints.first().Distance)
            }
            PlotDimension.TIME -> when {
                dataPoints.isEmpty() -> return 0L
                else -> (maxDimension(dataPoints, dimension) as Long - (dimensionRestriction ?: 0L))
                    .coerceAtMost(dataPoints.first().Time)
            }
        }
    }

    internal fun maxDimension(dataPoints: List<PlotLineItem>, dimension: PlotDimension): Any {
        return when (dimension) {
            PlotDimension.INDEX -> (dataPoints.size - 1).toFloat()
            PlotDimension.DISTANCE -> when {
                dataPoints.isEmpty() -> return 0f
                else -> dataPoints.last().Distance
            }
            PlotDimension.TIME -> when {
                dataPoints.isEmpty() -> return 0L
                else -> dataPoints.last().Time
            }
        }
    }

    fun distanceDimension(dimension: PlotDimension, dimensionRestriction: Long? = null): Float {
        return distanceDimension(getDataPoints(dimension), dimension, dimensionRestriction)
    }

    fun distanceDimension(dataPoints: List<PlotLineItem>, dimension: PlotDimension, dimensionRestriction: Long?): Float {
        return when (dimension) {
            PlotDimension.TIME -> (maxDimension(dataPoints, dimension) as Long - minDimension(dataPoints, dimension, dimensionRestriction) as Long).toFloat()
            else -> maxDimension(dataPoints, dimension) as Float - minDimension(dataPoints, dimension, dimensionRestriction) as Float
        }
    }

    fun maxValue(dataPoints: List<PlotLineItem>): Float? {
        val max : Float? = when {
            dataPoints.isEmpty() -> Range.minPositive
            else -> {
                val min = (dataPoints.maxBy { it.Value }?.Value ?: Range.minPositive).coerceAtLeast(Range.minPositive)
                when {
                    Range.maxPositive != null -> min.coerceAtMost(Range.maxPositive)
                    else -> min
                }
            }
        }

        return when {
            max == null -> null
            Range.smoothAxis != null -> when (max % Range.smoothAxis) {
                0f -> max
                else -> max + (Range.smoothAxis - max % Range.smoothAxis)
            }
            else -> max
        }
    }

    fun minValue(dataPoints: List<PlotLineItem>): Float? {
        val min : Float? = when {
            dataPoints.isEmpty() -> Range.minNegative
            else -> {
                val max = (dataPoints.minBy { it.Value }?.Value ?: Range.minNegative).coerceAtMost(Range.minNegative)
                when {
                    Range.maxNegative != null -> max.coerceAtLeast(Range.maxNegative)
                    else -> max
                }
            }
        }

        val minSmooth = when {
            min == null -> null
            Range.smoothAxis != null -> when (min % Range.smoothAxis) {
                0f -> min
                else -> min - (min % Range.smoothAxis) - Range.smoothAxis
            }
            else -> min
        }

        val zeroAtCopy = zeroAt
        when {
            zeroAtCopy == null || zeroAtCopy <= 0f || zeroAtCopy >= 1f -> return minSmooth
            else -> {
                val max = maxValue(dataPoints) ?: return minSmooth
                return -(max /  (1f - zeroAtCopy) * (zeroAtCopy))
            }
        }
    }

    fun averageValue(dataPoints: List<PlotLineItem>, dimension: PlotDimension): Float? {
        return when(dimension) {
            PlotDimension.INDEX -> averageValue(dataPoints, PlotHighlightMethod.AVG_BY_INDEX)
            PlotDimension.DISTANCE -> averageValue(dataPoints, PlotHighlightMethod.AVG_BY_DISTANCE)
            PlotDimension.TIME -> averageValue(dataPoints, PlotHighlightMethod.AVG_BY_TIME)
        }
    }

    private fun averageValue(dataPoints: List<PlotLineItem>, averageMethod: PlotHighlightMethod): Float? {
        if (dataPoints.isEmpty()) return null
        if (dataPoints.size == 1) return dataPoints.first().Value

        return when (averageMethod) {
            PlotHighlightMethod.AVG_BY_INDEX -> dataPoints.map { it.Value }.average().toFloat()
            PlotHighlightMethod.AVG_BY_DISTANCE -> {
                val value = dataPoints.filter { it.DistanceDelta != null }.map { (it.DistanceDelta?:0f) * it.Value }.sum()
                val distance = dataPoints.filter { it.DistanceDelta != null }.map { (it.DistanceDelta?:0f) }.sum()

                return when {
                    distance != 0f -> value / distance
                    else -> 0f
                }
            }
            PlotHighlightMethod.AVG_BY_TIME -> {
                val value = dataPoints.filter { it.TimeDelta != null }.map { (it.TimeDelta?:0L) * it.Value }.sum()
                val distance = dataPoints.filter { it.TimeDelta != null }.map { (it.TimeDelta?:0L) }.sum()

                return when {
                    distance != 0L -> value / distance
                    else -> 0f
                }
            }
            else -> null
        }
    }

    fun isEmpty(): Boolean {
        return dataPoints.isEmpty()
    }

    fun byHighlightMethod(dataPoints: List<PlotLineItem>): Float? {
        if (dataPoints.isEmpty()) return null

        return when (HighlightMethod) {
            PlotHighlightMethod.MIN -> minValue(dataPoints)
            PlotHighlightMethod.MAX -> maxValue(dataPoints)
            PlotHighlightMethod.FIRST -> dataPoints.first().Value
            PlotHighlightMethod.LAST -> dataPoints.last().Value
            PlotHighlightMethod.AVG_BY_INDEX -> averageValue(dataPoints, HighlightMethod)
            PlotHighlightMethod.AVG_BY_DISTANCE -> averageValue(dataPoints, HighlightMethod)
            PlotHighlightMethod.AVG_BY_TIME -> averageValue(dataPoints, HighlightMethod)
            else -> null
        }
    }

    fun x(dataPoints: List<PlotLineItem>, value: Long?, valueDimension: PlotDimension, targetDimension: PlotDimension, min: Any, max: Any) : Float? {
        if (dataPoints.isEmpty() || value == null) return null
        return when (targetDimension) {
            PlotDimension.DISTANCE -> when (valueDimension) {
                PlotDimension.TIME -> {
                    if (value !in dataPoints.first().Time .. dataPoints.last().Time) return null

                    val closePoint = dataPoints.minBy { abs(it.Time - value) }
                    when (closePoint.Marker) {
                        PlotLineMarkerType.BEGIN_SESSION -> x(closePoint.Distance - (closePoint.DistanceDelta ?: 0f), min, max)
                        else -> x(closePoint.Distance, min, max)
                    }
                }
                PlotDimension.DISTANCE -> x(value.toFloat(), min, max)
                else -> null
            }
            PlotDimension.TIME -> when (valueDimension) {
                PlotDimension.TIME -> x(value.toFloat(), min, max)
                PlotDimension.DISTANCE -> {
                    if (value.toFloat() !in dataPoints.first().Distance .. dataPoints.last().Distance) return null

                    val closePoint = dataPoints.minBy { abs(it.Distance - value) }
                    when (closePoint.Marker) {
                        PlotLineMarkerType.BEGIN_SESSION -> x(closePoint.Time - (closePoint.TimeDelta ?: 0L), min, max)
                        else -> x(closePoint.Distance, min, max)
                    }
                }
                else -> null
            }
            else -> null
        }
    }

    fun x(index: Float, min: Any, max: Any) : Float {
        return PlotLineItem.cord(
            index,
            min as Float,
            max as Float
        )
    }

    fun x(index: Long, min: Any, max: Any) : Float {
        return PlotLineItem.cord(
            index,
            min as Long,
            max as Long
        )
    }

    fun toPlotLineItemPointCollection(dataPoints: List<PlotLineItem>, dimension: PlotDimension, dimensionSmoothing: Long?, min: Any, max: Any): ArrayList<ArrayList<PlotLineItemPoint>> {
        val result = ArrayList<ArrayList<PlotLineItemPoint>>()
        var group = ArrayList<PlotLineItemPoint>()

        for (index in dataPoints.indices) {
            val item = dataPoints[index]

            if (item.Marker == PlotLineMarkerType.BEGIN_SESSION) {
                group.add(
                    PlotLineItemPoint(
                        when (dimension) {
                            PlotDimension.INDEX -> x(index.toFloat(), min, max)
                            PlotDimension.DISTANCE -> x(item.Distance - (item.DistanceDelta ?: 0f), min, max)
                            PlotDimension.TIME -> x(item.Time - (item.TimeDelta ?: 0L), min, max)
                        },
                        item,
                        item.group(index, dimension, dimensionSmoothing)
                    )
                )
            }

            group.add(
                PlotLineItemPoint(
                    when (dimension) {
                        PlotDimension.INDEX -> x(index.toFloat(), min, max)
                        PlotDimension.DISTANCE -> x(item.Distance, min, max)
                        PlotDimension.TIME -> x(item.Time, min, max)
                    },
                    item,
                    item.group(index, dimension, dimensionSmoothing)
                )
            )

            if ((item.Marker ?: PlotLineMarkerType.BEGIN_SESSION) != PlotLineMarkerType.BEGIN_SESSION) {
                result.add(group)
                group = ArrayList()
            }
        }

        if (!group.isEmpty()) {
            result.add(group)
        }

        return result
    }
}

