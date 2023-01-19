package com.mbuehler.carStatsViewer.views

import android.R
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.mbuehler.carStatsViewer.plot.*
import java.lang.Long.max
import java.util.concurrent.TimeUnit


class PlotView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    companion object {
        const val textSize = 26f
    }


    var xMargin: Int = 100
        set(value) {
            val diff = value != field
            if (value > 0) {
                field = value
                if (diff) invalidate()
            }
        }

    var xLineCount: Int = 6
        set(value) {
            val diff = value != field
            if (value > 1) {
                field = value
                if (diff) invalidate()
            }
        }

    var yMargin: Int = 60
        set(value) {
            val diff = value != field
            if (value > 0) {
                field = value
                if (diff) invalidate()
            }
        }

    var yLineCount: Int = 5
        set(value) {
            val diff = value != field
            if (value > 1) {
                field = value
                if (diff) invalidate()
            }
        }

    var dimension: PlotDimension = PlotDimension.INDEX
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    var dimensionRestrictionTouchInterval: Long? = null
    var dimensionRestriction: Long? = null
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    var dimensionShiftTouchInterval: Long? = null
    var dimensionShift: Long? = null
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    var dimensionSmoothing: Long? = null
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    var dimensionSmoothingPercentage: Float? = null
        set(value) {
            val diff = value != field
            field = value
            if (diff) invalidate()
        }

    private val plotLines = ArrayList<PlotLine>()
    private val plotPaint = ArrayList<PlotPaint>()

    private lateinit var labelPaint: Paint
    private lateinit var labelLinePaint: Paint
    private lateinit var baseLinePaint: Paint
    private lateinit var backgroundPaint: Paint

    init {
        setupPaint()
    }

    // Setup paint with color and stroke styles
    private fun setupPaint() {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.colorControlActivated, typedValue, true)

        val basePaint = PlotPaint.basePaint(textSize)

        labelLinePaint = Paint(basePaint)
        labelLinePaint.color = Color.GRAY

        labelPaint = Paint(labelLinePaint)
        labelPaint.style = Paint.Style.FILL

        baseLinePaint = Paint(labelLinePaint)
        baseLinePaint.color = Color.LTGRAY

        backgroundPaint = Paint(basePaint)
        backgroundPaint.color = Color.BLACK
        backgroundPaint.style = Paint.Style.FILL

        val plotColors = listOf(
            null,
            //Color.parseColor("#00BF00"), // Green
            Color.GREEN,
            Color.CYAN,
            Color.BLUE,
            Color.RED
        )

        for (color in plotColors) {
            plotPaint.add(PlotPaint.byColor(color ?: typedValue.data, textSize))
        }
    }

    fun reset() {
        for (item in plotLines) {
            item.reset()
        }
        invalidate()
    }

    fun addPlotLine(plotLine: PlotLine) {
        if (plotLine.plotPaint == null) {
            plotLine.plotPaint = plotPaint[plotLines.size]
        }
        plotLines.add(plotLine)
        invalidate()
    }

    fun removePlotLine(plotLine: PlotLine?) {
        plotLines.remove(plotLine)
        invalidate()
    }

    fun removeAllPlotLine() {
        plotLines.clear()
        invalidate()
    }

    private fun x(index: Float?, min: Float, max: Float, maxX: Float): Float? {
        return x(PlotLineItem.cord(index, min, max), maxX)
    }

    private fun x(value: Float?, maxX: Float): Float? {
        return when (value) {
            null -> null
            else -> xMargin + (maxX - (2 * xMargin)) * value
        }
    }

    private fun y(index: Float?, min: Float, max: Float, maxY: Float): Float? {
        return y(PlotLineItem.cord(index, min, max), maxY)
    }

    private fun y(value: Float?, maxY: Float): Float? {
        return when (value) {
            null -> null
            else -> {
                val px = maxY - (2 * yMargin)
                yMargin + px - px * value
            }
        }
    }

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            val shiftInterval = dimensionShiftTouchInterval
            if (shiftInterval != null) {
                touchDimensionShiftDistance += - distanceX
                dimensionShift = (touchDimensionShift + (touchDimensionShiftDistance / max(1L, touchActionDistance)).toLong() * shiftInterval)
                    .coerceAtMost(touchDimensionMax + (shiftInterval - touchDimensionMax % shiftInterval) - (dimensionRestriction!! - 1))
                    .coerceAtLeast(0L)
            }

            val restrictionInterval = dimensionRestrictionTouchInterval
            if (restrictionInterval != null) {
                touchDimensionRestrictionDistance += - distanceY
                dimensionRestriction = (touchDimensionRestriction + (touchDimensionRestrictionDistance / max(1L, touchActionDistance)).toLong() * restrictionInterval)
                    .coerceAtMost(touchDimensionMax + (restrictionInterval - touchDimensionMax % restrictionInterval))
                    .coerceAtLeast(restrictionInterval)
            }

            return true
        }
    }

    private val mScaleDetector = GestureDetector(context, mGestureListener)

    private var touchDimensionShift : Long = 0L
    private var touchDimensionShiftDistance : Float = 0f

    private var touchDimensionRestriction : Long = 0L
    private var touchDimensionRestrictionDistance : Float = 0f

    private var touchActionDistance : Long = 1L

    private var touchDimensionMax : Long = 0L

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (dimensionRestriction == null) return true
        if (dimensionShiftTouchInterval == null && dimensionRestrictionTouchInterval == null) return true

        when (ev.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchDimensionShiftDistance = 0f
                touchDimensionRestrictionDistance = 0f

                touchDimensionShift = dimensionShift ?: 0L
                touchDimensionRestriction = dimensionRestriction ?: 0L

                touchDimensionMax = (plotLines.mapNotNull { it.distanceDimension(dimension) }.max() ?: 0f).toLong()
            }
        }

        mScaleDetector.onTouchEvent(ev)
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        touchActionDistance = (((canvas.width - 2 * xMargin).toFloat() / xLineCount.toFloat()) * 0.75f).toLong()

        alignZero()
        drawBackground(canvas)
        drawXLines(canvas)
        drawYBaseLines(canvas)
        drawPlot(canvas)
        drawYLines(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        val maxX = canvas.width.toFloat()
        val maxY = canvas.height.toFloat()

        canvas.drawRect(xMargin.toFloat(), yMargin.toFloat(), maxX - xMargin, maxY - yMargin, backgroundPaint)
    }

    private fun alignZero() {
        if (plotLines.none { it.alignZero }) return

        var zeroAt : Float? = null
        for (index in plotLines.indices) {
            val line = plotLines[index]

            if (index == 0) {
                if (line.isEmpty() || !line.Visible) return

                val dataPoints = line.getDataPoints(dimension, dimensionRestriction, dimensionShift)
                if (dataPoints.isEmpty()) return

                val minValue = line.minValue(dataPoints)!!
                val maxValue = line.maxValue(dataPoints)!!

                zeroAt = PlotLineItem.cord(0f, minValue, maxValue)
                continue
            }

            if (line.alignZero) {
                line.zeroAt = zeroAt
            }
        }
    }

    private fun drawPlot(canvas: Canvas) {
        val maxX = canvas.width.toFloat()
        val maxY = canvas.height.toFloat()

        for (line in plotLines) {
            if (line.isEmpty() || !line.Visible) continue

            val dataPoints = line.getDataPoints(dimension, dimensionRestriction, dimensionShift)
            if (dataPoints.isEmpty()) continue

            val minValue = line.minValue(dataPoints)!!
            val maxValue = line.maxValue(dataPoints)!!

            val minDimension = line.minDimension(dataPoints, dimension, dimensionRestriction)
            val maxDimension = line.maxDimension(dataPoints, dimension)

            val smoothing = when {
                dimensionSmoothing != null -> dimensionSmoothing
                dimensionSmoothingPercentage != null -> (line.distanceDimension(dataPoints, dimension, dimensionRestriction) * dimensionSmoothingPercentage!!).toLong()
                else -> null
            }

            val backgroundZeroCord = y(line.Range.backgroundZero, minValue, maxValue, maxY)

            val dataPointsUnrestricted = line.getDataPoints(dimension)
            val plotLineItemPointCollection = line.toPlotLineItemPointCollection(dataPointsUnrestricted, dimension, smoothing, minDimension, maxDimension)

            val plotPointCollection = ArrayList<ArrayList<PlotPoint>>()
            for (collection in plotLineItemPointCollection) {
                if (collection.isEmpty()) continue

                val plotPoints = ArrayList<PlotPoint>()

                for (group in collection.groupBy { it.group }) {
                    val plotPoint = when {
                        group.value.size <= 1 -> {
                            val point = group.value.first()
                            PlotPoint(
                                x(point.x, 0f, 1f, maxX)!!,
                                y(point.y.Value, minValue, maxValue, maxY)!!
                            )
                        }
                        else -> {
                            val x = when (plotPoints.size) {
                                0 -> group.value.minBy { it.x }?.x!!
                                else -> group.value.maxBy { it.x }?.x!!
                            }

                            PlotPoint(
                                x(x, 0f, 1f, maxX)!!,
                                y(line.averageValue(group.value.map { it.y }, dimension), minValue, maxValue, maxY)!!
                            )
                        }
                    }

                    plotPoints.add(plotPoint)
                }

                plotPointCollection.add(plotPoints)
            }

            canvas.save()

            // TOP
            canvas.clipOutRect(0f, 0f, maxX, yMargin.toFloat())
            // BOTTOM
            canvas.clipOutRect(0f, maxY - yMargin, maxX, maxY)
            // LEFT
            canvas.clipOutRect(0f, 0f, xMargin.toFloat(), maxY)
            // RIGHT
            canvas.clipOutRect(maxX - xMargin, 0f, maxX, maxY)

            for (plotPoints in plotPointCollection) {
                if (backgroundZeroCord != null) {
                    drawPlot(canvas, line.plotPaint!!.PlotBackground, plotPoints, backgroundZeroCord)
                }

                drawPlot(canvas, line.plotPaint!!.Plot, plotPoints)
            }

            canvas.restore()
        }
    }

    private fun drawPlot(canvas : Canvas, paint : Paint, plotPoints : ArrayList<PlotPoint>, backgroundZeroCord: Float? = null) {
        if (plotPoints.isEmpty()) return

        val path = Path()

        var firstPoint: PlotPoint? = null
        var prevPoint: PlotPoint? = null

        for (i in plotPoints.indices) {
            val point = plotPoints[i]

            if (prevPoint === null) {
                firstPoint = point
                path.moveTo(point.x, point.y)
            } else {
                val midX = (prevPoint.x + point.x) / 2
                val midY = (prevPoint.y + point.y) / 2

                if (i == 1) {
                    path.lineTo(midX, midY)
                } else {
                    path.quadTo(prevPoint.x, prevPoint.y, midX, midY)
                }
            }

            prevPoint = point
        }

        path.lineTo(prevPoint!!.x, prevPoint.y)

        if (backgroundZeroCord != null) {
            path.lineTo(prevPoint.x, backgroundZeroCord)
            path.lineTo(firstPoint!!.x, backgroundZeroCord)
        }

        canvas.drawPath(path, paint)
    }

    private fun drawXLines(canvas: Canvas) {
        val maxX = canvas.width.toFloat()
        val maxY = canvas.height.toFloat()

        val distanceDimension = when {
            dimensionRestriction != null -> dimensionRestriction!!.toFloat()
            else -> plotLines.mapNotNull { it.distanceDimension(dimension, dimensionRestriction) }.max()?:0f
        }

        for (i in 0 until xLineCount) {
            val cordX = x(i.toFloat(), 0f, xLineCount.toFloat() - 1, maxX)!!
            val cordY = maxY - yMargin

            val leftZero = (distanceDimension / (xLineCount - 1) * i) + (dimensionShift ?: 0L)
            val rightZero = distanceDimension - leftZero + (2 * (dimensionShift ?: 0L))

            drawXLine(canvas, cordX, maxY, labelLinePaint)

            val label = when (dimension) {
                PlotDimension.INDEX -> String.format("%d", leftZero.toInt())
                PlotDimension.DISTANCE -> when {
                    rightZero < 1000 -> String.format("%d m", (rightZero - (rightZero % 100)).toInt())
                    else -> String.format("%d km", (rightZero / 1000).toInt())
                }
                PlotDimension.TIME -> String.format("%02d:%02d", TimeUnit.MINUTES.convert(leftZero.toLong(), TimeUnit.NANOSECONDS), TimeUnit.SECONDS.convert(leftZero.toLong(), TimeUnit.NANOSECONDS) % 60)
            }

            val bounds = Rect()
            labelPaint.getTextBounds(label, 0, label.length, bounds)

            canvas.drawText(
                label,
                cordX - bounds.width() / 2,
                cordY + yMargin / 2 + bounds.height() / 2,
                labelPaint
            )
        }
    }

    private fun drawXLine(canvas: Canvas, cord: Float?, maxY: Float, paint: Paint?) {
        if (cord == null) return
        val path = Path()
        path.moveTo(cord, yMargin.toFloat())
        path.lineTo(cord, maxY - yMargin)
        canvas.drawPath(path, paint ?: labelLinePaint)
    }

    private fun drawYBaseLines(canvas: Canvas) {
        val maxX = canvas.width.toFloat()
        val maxY = canvas.height.toFloat()

        for (i in 0 until yLineCount) {
            val cordY = y(i.toFloat(), 0f, yLineCount.toFloat() - 1, maxY)!!
            drawYLine(canvas, cordY, maxX, labelLinePaint)
        }
    }

    private fun drawYLines(canvas: Canvas) {
        val maxX = canvas.width.toFloat()
        val maxY = canvas.height.toFloat()

        val bounds = Rect()
        labelPaint.getTextBounds("Dummy", 0, "Dummy".length, bounds)

        for (drawHighlightLabelOnly in listOf(false, true)) {
            for (line in plotLines.filter { it.Visible }) {
                val dataPoints = line.getDataPoints(dimension, dimensionRestriction, dimensionShift)

                val minValue = line.minValue(dataPoints)!!
                val maxValue = line.maxValue(dataPoints)!!
                val highlight = line.byHighlightMethod(dataPoints)

                val labelShiftY = (bounds.height() / 2).toFloat()
                val valueShiftY = (maxValue - minValue) / (yLineCount - 1)

                val labelUnitXOffset = when (line.LabelPosition) {
                    PlotLabelPosition.LEFT -> 0f
                    PlotLabelPosition.RIGHT -> -labelPaint.measureText(line.Unit) / 2
                    else -> 0f
                }

                val labelCordX = when (line.LabelPosition) {
                    PlotLabelPosition.LEFT -> textSize
                    PlotLabelPosition.RIGHT -> maxX - xMargin + textSize / 2
                    else -> null
                }

                val highlightCordY = y(highlight, minValue, maxValue, maxY)

                if (!drawHighlightLabelOnly) {
                    if (line.LabelPosition !== PlotLabelPosition.NONE && labelCordX != null) {
                        if (line.Unit.isNotEmpty()) {
                            canvas.drawText(line.Unit, labelCordX + labelUnitXOffset,yMargin - (yMargin / 3f), labelPaint)
                        }

                        for (i in 0 until yLineCount) {
                            val valueY = maxValue - i * valueShiftY
                            val cordY = y(valueY, minValue, maxValue, maxY)!!
                            val label = String.format(line.LabelFormat, valueY / line.Divider)

                            canvas.drawText(label, labelCordX, cordY + labelShiftY, labelPaint)
                        }
                    }

                    when (line.HighlightMethod) {
                        PlotHighlightMethod.AVG_BY_INDEX -> drawYLine(canvas, highlightCordY, maxX, line.plotPaint!!.HighlightLabelLine)
                        PlotHighlightMethod.AVG_BY_DISTANCE -> drawYLine(canvas, highlightCordY, maxX, line.plotPaint!!.HighlightLabelLine)
                        PlotHighlightMethod.AVG_BY_TIME -> drawYLine(canvas, highlightCordY, maxX, line.plotPaint!!.HighlightLabelLine)
                        else -> {
                            // Don't draw
                        }
                    }

                    for (baseLineAt in line.baseLineAt) {
                        drawYLine(canvas, y(baseLineAt, minValue, maxValue, maxY), maxX, baseLinePaint)
                    }
                } else {
                    if (labelCordX != null && highlightCordY != null) {
                        val label = String.format(line.HighlightFormat, highlight!! / line.Divider)
                        line.plotPaint!!.HighlightLabel.textSize = 35f
                        val labelWidth = line.plotPaint!!.HighlightLabel.measureText(label)
                        val labelHeight = line.plotPaint!!.HighlightLabel.textSize
                        val textBoxMargin = line.plotPaint!!.HighlightLabel.textSize / 3.5f

                        canvas.drawRect(
                            labelCordX + labelUnitXOffset - textBoxMargin,
                            highlightCordY - labelHeight + labelShiftY,
                            labelCordX + labelUnitXOffset + labelWidth + textBoxMargin,
                            highlightCordY + labelShiftY + textBoxMargin,
                            backgroundPaint
                        )

                        canvas.drawRect(
                            labelCordX + labelUnitXOffset - textBoxMargin,
                            highlightCordY - labelHeight + labelShiftY,
                            labelCordX + labelUnitXOffset + labelWidth + textBoxMargin,
                            highlightCordY + labelShiftY + textBoxMargin,
                            line.plotPaint!!.Plot
                        )

                        canvas.drawText(label, labelCordX + labelUnitXOffset, highlightCordY + labelShiftY, line.plotPaint!!.HighlightLabel)
                    }
                }
            }
        }
    }

    private fun drawYLine(canvas: Canvas, cord: Float?, maxX: Float, paint: Paint?) {
        if (cord == null) return
        val path = Path()
        path.moveTo(xMargin.toFloat(), cord)
        path.lineTo(maxX - xMargin, cord)
        canvas.drawPath(path, paint ?: labelLinePaint)
    }


}