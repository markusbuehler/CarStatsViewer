package com.mbuehler.carStatsViewer.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.transition.Fade
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.SeekBar
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColor
import androidx.core.view.get
import com.mbuehler.carStatsViewer.CarStatsViewer
import com.mbuehler.carStatsViewer.R
import com.mbuehler.carStatsViewer.appPreferences.AppPreferences
import com.mbuehler.carStatsViewer.dataManager.*
import com.mbuehler.carStatsViewer.plot.enums.*
import com.mbuehler.carStatsViewer.plot.graphics.PlotLinePaint
import com.mbuehler.carStatsViewer.plot.graphics.PlotPaint
import com.mbuehler.carStatsViewer.plot.objects.PlotLine
import com.mbuehler.carStatsViewer.plot.objects.PlotLineConfiguration
import com.mbuehler.carStatsViewer.plot.objects.PlotMarkers
import com.mbuehler.carStatsViewer.plot.objects.PlotRange
import com.mbuehler.carStatsViewer.utils.InAppLogger
import com.mbuehler.carStatsViewer.utils.StringFormatters
import com.mbuehler.carStatsViewer.views.PlotView
import kotlinx.android.synthetic.main.activity_summary.*
import java.util.concurrent.TimeUnit


class SummaryActivity: Activity() {

    private var primaryColor = CarStatsViewer.primaryColor

    private var chargePlotLine = PlotLine(
        PlotLineConfiguration(
            PlotRange(0f, 20f, 0f, 160f, 20f),
            PlotLineLabelFormat.FLOAT,
            PlotHighlightMethod.AVG_BY_TIME,
            "kW"
        )
    )

    private lateinit var consumptionPlotLine: PlotLine

    private lateinit var tripData: TripData

    private lateinit var appPreferences: AppPreferences
    private lateinit var consumptionPlotLinePaint : PlotLinePaint
    private lateinit var chargePlotLinePaint : PlotLinePaint

    private lateinit var disabledTint: PorterDuffColorFilter
    private lateinit var enabledTint: android.graphics.ColorFilter

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            setVisibleChargeCurve(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            InAppLogger.d(intent.action?: "")
            when (intent.action) {
                getString(R.string.distraction_optimization_broadcast) -> {
                    updateDistractionOptimization()
                }
                else -> {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_summary)

        appPreferences = AppPreferences(applicationContext)

        consumptionPlotLinePaint = PlotLinePaint(
            PlotPaint.byColor(getColor(R.color.primary_plot_color), PlotView.textSize),
            PlotPaint.byColor(getColor(R.color.secondary_plot_color), PlotView.textSize),
            PlotPaint.byColor(getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
        ) { appPreferences.consumptionPlotSecondaryColor }

        chargePlotLinePaint = PlotLinePaint(
            PlotPaint.byColor(getColor(R.color.charge_plot_color), PlotView.textSize),
            PlotPaint.byColor(getColor(R.color.secondary_plot_color), PlotView.textSize),
            PlotPaint.byColor(getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
        ) { appPreferences.chargePlotSecondaryColor }


        //val tripDataFileName = intent.getStringExtra("FileName").toString()
        // tripData = if (tripDataFileName != "null") DataManager.getTripData(tripDataFileName)
        // else DataManager.getTripData()

        val dataManager :DataManager = if (intent.hasExtra("dataManager")) {
            DataManagers.values()[intent.getIntExtra("dataManager", 0)].dataManager
        } else DataManagers.values()[appPreferences.mainViewTrip].dataManager

        if (appPreferences.mainViewTrip != 0) {
            summary_button_reset.isEnabled = false
            summary_button_reset.setColorFilter(getColor(R.color.disabled_tint))
        }

        tripData = dataManager.tripData!!
        consumptionPlotLine = dataManager.consumptionPlotLine

        summary_selected_trip_bar[appPreferences.mainViewTrip].background = primaryColor!!.toColor().toDrawable()

        summary_button_trip_prev.setOnClickListener {
            var tripIndex = appPreferences.mainViewTrip
            tripIndex--
            if (tripIndex < 0) tripIndex = 3
            appPreferences.mainViewTrip = tripIndex
            refreshActivity(tripIndex)
        }

        summary_button_trip_next.setOnClickListener {
            var tripIndex = appPreferences.mainViewTrip
            tripIndex++
            if (tripIndex > 3) tripIndex = 0
            appPreferences.mainViewTrip = tripIndex
            refreshActivity(tripIndex)
        }

        summary_title.setOnClickListener {
            var isMainDataManager = false
            for (mainDataManager in DataManagers.values()) {
                isMainDataManager = dataManager == mainDataManager.dataManager
                if (isMainDataManager) break
            }
            if (isMainDataManager) {
                var tripIndex = appPreferences.mainViewTrip
                tripIndex++
                if (tripIndex > 3) tripIndex = 0
                appPreferences.mainViewTrip = tripIndex
                refreshActivity(tripIndex)
            } else
                TODO("Don't use onClick when showing a specific trip, not one of the 4 main trips")
        }

        // enabledTint = summary_charge_plot_button_next.foreground!!
        disabledTint = PorterDuffColorFilter(getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)
        enabledTint = PorterDuffColorFilter(primaryColor!!, PorterDuff.Mode.SRC_IN)

        summary_title.text = getString(resources.getIdentifier(
            dataManager.printableName, "string", packageName
        ))

        summary_button_back.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.stay_still, R.anim.slide_out_down)
        }

        summary_button_reset.setOnClickListener {
            createResetDialog()
        }

        summary_trip_date_text.text = getString(R.string.summary_trip_start_date).format(StringFormatters.getDateString(tripData.tripStartDate))

        summary_button_show_consumption_container.isSelected = true

        updateDistractionOptimization()

        summary_button_show_consumption_container.setOnClickListener {
            switchToConsumptionLayout()
        }

        summary_button_show_charge_container.setOnClickListener {
            switchToChargeLayout()
        }

        setupConsumptionLayout()
        setupChargeLayout()

        registerReceiver(broadcastReceiver, IntentFilter(getString(R.string.distraction_optimization_broadcast)))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    private fun setupConsumptionLayout() {
        val plotMarkers = PlotMarkers()
        plotMarkers.addMarkers(tripData.markers)
        summary_consumption_plot.addPlotLine(consumptionPlotLine, consumptionPlotLinePaint)
        summary_consumption_plot.sessionGapRendering = PlotSessionGapRendering.JOIN
        summary_consumption_plot.dimensionYSecondary = when (appPreferences.secondaryConsumptionDimension) {
            1 -> {
                summary_button_secondary_dimension.text =
                    getString(R.string.main_secondary_axis, getString(R.string.main_speed))
                PlotDimensionY.SPEED
            }
            2 -> {
                summary_button_secondary_dimension.text =
                    getString(R.string.main_secondary_axis, getString(R.string.main_SoC))
                PlotDimensionY.STATE_OF_CHARGE
            }
            else -> {
                summary_button_secondary_dimension.text = getString(R.string.main_secondary_axis, "-")
                null
            }
        }
        summary_consumption_plot.dimension = PlotDimensionX.DISTANCE
        summary_consumption_plot.dimensionRestriction = appPreferences.distanceUnit.asUnit(((appPreferences.distanceUnit.toUnit(tripData.traveledDistance) / MainActivity.DISTANCE_TRIP_DIVIDER).toInt() + 1) * MainActivity.DISTANCE_TRIP_DIVIDER) + 1
        summary_consumption_plot.dimensionRestrictionMin = appPreferences.distanceUnit.asUnit(MainActivity.DISTANCE_TRIP_DIVIDER)
        summary_consumption_plot.dimensionSmoothing = 0.02f
        summary_consumption_plot.dimensionSmoothingType = PlotDimensionSmoothingType.PERCENTAGE
        summary_consumption_plot.setPlotMarkers(plotMarkers)
        summary_consumption_plot.visibleMarkerTypes.add(PlotMarkerType.CHARGE)
        summary_consumption_plot.visibleMarkerTypes.add(PlotMarkerType.PARK)

        summary_consumption_plot.invalidate()

        summary_distance_value_text.text = StringFormatters.getTraveledDistanceString(tripData.traveledDistance)
        summary_used_energy_value_text.text = StringFormatters.getEnergyString(tripData.usedEnergy)
        summary_avg_consumption_value_text.text = StringFormatters.getAvgConsumptionString(tripData.usedEnergy, tripData.traveledDistance)
        summary_travel_time_value_text.text = StringFormatters.getElapsedTimeString(tripData.travelTime)

        summary_button_secondary_dimension.setOnClickListener {
            var currentIndex = appPreferences.secondaryConsumptionDimension
            currentIndex++
            if (currentIndex > 2) currentIndex = 0
            appPreferences.secondaryConsumptionDimension = currentIndex
            summary_consumption_plot.dimensionYSecondary = when (appPreferences.secondaryConsumptionDimension) {
                1 -> {
                    summary_button_secondary_dimension.text =
                        getString(R.string.main_secondary_axis, getString(R.string.main_speed))
                    PlotDimensionY.SPEED
                }
                2 -> {
                    summary_button_secondary_dimension.text =
                        getString(R.string.main_secondary_axis, getString(R.string.main_SoC))
                    PlotDimensionY.STATE_OF_CHARGE
                }
                else -> {
                    summary_button_secondary_dimension.text = getString(R.string.main_secondary_axis, "-")
                    null
                }
            }
        }
    }

    private fun refreshActivity(index: Int?) {
        val refreshIntent = Intent(this, SummaryActivity::class.java).apply {
            if (index != null) {
            putExtra("dataManager", index)
            }
        }
        finish()
        overridePendingTransition(0, 0)
        startActivity(refreshIntent)
        overridePendingTransition(0, 0)
    }

    private fun setupChargeLayout() {
        summary_charge_plot_sub_title_curve.text = "%s (%d/%d, %s)".format(
            getString(R.string.settings_sub_title_last_charge_plot),
            tripData.chargeCurves.size,
            tripData.chargeCurves.size,
            StringFormatters.getDateString(
                if (tripData.chargeCurves.isNotEmpty()) tripData.chargeCurves.last().chargeStartDate
                else null
            )
        )

        chargePlotLine.reset()
        if (tripData.chargeCurves.isNotEmpty()) {
            chargePlotLine.addDataPoints(tripData.chargeCurves.last().chargePlotLine)
            summary_charge_plot_button_next.isEnabled = false
            summary_charge_plot_button_next.colorFilter = disabledTint
            summary_charge_plot_button_prev.isEnabled = true
            summary_charge_plot_button_prev.colorFilter = enabledTint

            if (tripData.chargeCurves.last().chargePlotLine.filter { it.Marker == PlotLineMarkerType.END_SESSION }.size > 1)
            // Charge has been interrupted
                summary_charged_energy_warning_text.visibility = View.VISIBLE
            else summary_charged_energy_warning_text.visibility = View.GONE

            summary_charged_energy_value_text.text = chargedEnergy(tripData.chargeCurves.last())
            summary_charge_time_value_text.text = StringFormatters.getElapsedTimeString(tripData.chargeCurves.last().chargeTime)
            summary_charge_ambient_temp.text = StringFormatters.getTemperatureString(tripData.chargeCurves.last().ambientTemperature)
            summary_charge_plot_view.dimensionRestriction = TimeUnit.MINUTES.toMillis((TimeUnit.MILLISECONDS.toMinutes(tripData.chargeCurves.last().chargeTime) / 5) + 1) * 5 + 1
            summary_charge_plot_view.dimensionRestrictionMin = TimeUnit.MINUTES.toMillis(5L)
        }
        if (tripData.chargeCurves.size < 2){
            summary_charge_plot_button_next.isEnabled = false
            summary_charge_plot_button_next.colorFilter = disabledTint
            summary_charge_plot_button_prev.isEnabled = false
            summary_charge_plot_button_prev.colorFilter = disabledTint
        }
        summary_charge_plot_view.addPlotLine(chargePlotLine, chargePlotLinePaint)

        summary_charge_plot_view.dimension = PlotDimensionX.TIME
        // summary_charge_plot_view.dimensionSmoothingPercentage = 0.01f
        summary_charge_plot_view.sessionGapRendering = PlotSessionGapRendering.GAP
        summary_charge_plot_view.dimensionYPrimary = PlotDimensionY.STATE_OF_CHARGE
        summary_charge_plot_view.invalidate()

        summary_charge_plot_seek_bar.max = (tripData.chargeCurves.size - 1).coerceAtLeast(0)
        summary_charge_plot_seek_bar.progress = (tripData.chargeCurves.size - 1).coerceAtLeast(0)
        summary_charge_plot_seek_bar.setOnSeekBarChangeListener(seekBarChangeListener)

        summary_charge_plot_button_next.setOnClickListener {
            val newProgress = summary_charge_plot_seek_bar.progress + 1
            if (newProgress <= (tripData.chargeCurves.size - 1)) {
                summary_charge_plot_seek_bar.progress = newProgress
            }
            summary_charge_plot_view.dimensionShift = 0L
        }

        summary_charge_plot_button_prev.setOnClickListener {
            val newProgress = summary_charge_plot_seek_bar.progress - 1
            if (newProgress >= 0) {
                summary_charge_plot_seek_bar.progress = newProgress
            }
            summary_charge_plot_view.dimensionShift = 0L
        }
    }

    private fun setVisibleChargeCurve(progress: Int) {
        summary_charge_plot_sub_title_curve.text = "%s (%d/%d, %s)".format(
            getString(R.string.settings_sub_title_last_charge_plot),
            tripData.chargeCurves.size,
            tripData.chargeCurves.size,
            StringFormatters.getDateString(tripData.chargeCurves.last().chargeStartDate))

        if (tripData.chargeCurves.size - 1 == 0) {
            summary_charge_plot_sub_title_curve.text = "%s (0/0)".format(
                getString(R.string.settings_sub_title_last_charge_plot))

            summary_charge_plot_button_next.isEnabled = false
            summary_charge_plot_button_next.colorFilter = disabledTint
            summary_charge_plot_button_prev.isEnabled = false
            summary_charge_plot_button_prev.colorFilter = disabledTint

        } else {
            summary_charge_plot_sub_title_curve.text = "%s (%d/%d, %s)".format(
                getString(R.string.settings_sub_title_last_charge_plot),
                progress + 1,
                tripData.chargeCurves.size,
                StringFormatters.getDateString(tripData.chargeCurves.last().chargeStartDate))

            when (progress) {
                0 -> {
                    summary_charge_plot_button_prev.isEnabled = false
                    summary_charge_plot_button_prev.colorFilter = disabledTint
                    summary_charge_plot_button_next.isEnabled = true
                    summary_charge_plot_button_next.colorFilter = enabledTint
                }
                tripData.chargeCurves.size - 1 -> {
                    summary_charge_plot_button_next.isEnabled = false
                    summary_charge_plot_button_next.colorFilter = disabledTint
                    summary_charge_plot_button_prev.isEnabled = true
                    summary_charge_plot_button_prev.colorFilter = enabledTint
                }
                else -> {
                    summary_charge_plot_button_next.isEnabled = true
                    summary_charge_plot_button_next.colorFilter = enabledTint
                    summary_charge_plot_button_prev.isEnabled = true
                    summary_charge_plot_button_prev.colorFilter = enabledTint
                }
            }
        }
        if (tripData.chargeCurves[progress].chargePlotLine.filter { it.Marker == PlotLineMarkerType.END_SESSION }.size > 1)
            // Charge has been interrupted
            summary_charged_energy_warning_text.visibility = View.VISIBLE
        else summary_charged_energy_warning_text.visibility = View.GONE

        summary_charged_energy_value_text.text = chargedEnergy(tripData.chargeCurves[progress])
        summary_charge_time_value_text.text = StringFormatters.getElapsedTimeString(tripData.chargeCurves[progress].chargeTime)
        summary_charge_ambient_temp.text = StringFormatters.getTemperatureString(tripData.chargeCurves[progress].ambientTemperature)

        chargePlotLine.reset()
        chargePlotLine.addDataPoints(tripData.chargeCurves[progress].chargePlotLine)
        summary_charge_plot_view.dimensionRestriction = TimeUnit.MINUTES.toMillis((TimeUnit.MILLISECONDS.toMinutes(tripData.chargeCurves[progress].chargeTime) / 5) + 1) * 5 + 1
        summary_charge_plot_view.invalidate()
    }

    private fun switchToConsumptionLayout() {
        summary_consumption_container.visibility = View.VISIBLE
        summary_charge_container.visibility = View.GONE
        summary_consumption_container.scrollTo(0,0)
        summary_button_show_charge_container.isSelected = false
        summary_button_show_consumption_container.isSelected = true

    }

    private fun switchToChargeLayout() {
        summary_consumption_container.visibility = View.GONE
        summary_charge_container.visibility = View.VISIBLE
        summary_charge_container.scrollTo(0,0)
        summary_button_show_consumption_container.isSelected = false
        summary_button_show_charge_container.isSelected = true
    }

    private fun createResetDialog() {
        val builder = AlertDialog.Builder(this@SummaryActivity)
        builder.setTitle(getString(R.string.dialog_reset_title))
            .setMessage(getString(R.string.dialog_reset_message))
            .setCancelable(true)
            .setPositiveButton(getString(R.string.dialog_reset_do_save)) { _, _ ->
                DataCollector.CurrentTripDataManager.reset()
                sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
                this@SummaryActivity.finish()
                this@SummaryActivity.overridePendingTransition(R.anim.stay_still, R.anim.slide_out_down)
            }
            .setNegativeButton(R.string.dialog_reset_no_save) { _, _ ->
                // DataCollector.CurrentTripDataManager.reset()
                // enumValues<DataManagers>().forEach { it.dataManager.reset() }
                DataManagers.CURRENT_TRIP.dataManager.reset()
                sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
                this@SummaryActivity.finish()
                this@SummaryActivity.overridePendingTransition(R.anim.stay_still, R.anim.slide_out_down)
            }
            .setNeutralButton(getString(R.string.dialog_reset_cancel)) { dialog, _ ->
                dialog.cancel()
            }
        val alert = builder.create()
        alert.show()
        alert.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = false
        alert.getButton(DialogInterface.BUTTON_NEGATIVE).setBackgroundColor(getColor(R.color.bad_red))

    }

    private fun updateDistractionOptimization() {
        InAppLogger.d("Distraction optimization: ${appPreferences.doDistractionOptimization}")
        summary_parked_warning.visibility =
            if (appPreferences.doDistractionOptimization) View.VISIBLE
            else View.GONE
        summary_content_container.visibility =
            if (appPreferences.doDistractionOptimization) View.GONE
            else View.VISIBLE
    }

    private fun chargedEnergy(chargeCurve: ChargeCurve): String {
        return "%s (%d%%)".format(
            StringFormatters.getEnergyString(chargeCurve.chargedEnergy),
            (chargeCurve.chargePlotLine.last().StateOfCharge - chargeCurve.chargePlotLine.first().StateOfCharge).toInt()
        )
    }
}