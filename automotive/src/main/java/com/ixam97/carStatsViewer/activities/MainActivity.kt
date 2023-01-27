package com.mbuehler.carStatsViewer.activities

import com.mbuehler.carStatsViewer.*
import com.mbuehler.carStatsViewer.objects.*
import com.mbuehler.carStatsViewer.services.*
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.car.VehicleGear
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.Toast
import com.mbuehler.carStatsViewer.plot.PlotDimension
import com.mbuehler.carStatsViewer.plot.PlotMarkerType
import com.mbuehler.carStatsViewer.plot.PlotPaint
import com.mbuehler.carStatsViewer.views.PlotView
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Runnable
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

var emulatorMode = false
var emulatorPowerSign = -1

class MainActivity : Activity() {
    companion object {
        private const val UI_UPDATE_INTERVAL = 500L
        private const val DISTANCE_1 =  5_001L
        private const val DISTANCE_2 = 15_001L
        const val DISTANCE_TRIP_DIVIDER = 5_000L
    }

    /** values and variables */

    private lateinit var timerHandler: Handler
    private lateinit var starterIntent: Intent
    private lateinit var context: Context
    private lateinit var appPreferences: AppPreferences

    private var updateUi = false
    private var lastPlotUpdate: Long = 0L

    private val updateActivityTask = object : Runnable {
        override fun run() {
            updateActivity()
            if (updateUi) timerHandler.postDelayed(this, UI_UPDATE_INTERVAL)
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                getString(R.string.ui_update_plot_broadcast) -> updatePlots()
                getString(R.string.ui_update_gages_broadcast) -> updateGages()
            }
        }
    }

    /** Overrides */

    override fun onResume() {
        super.onResume()

        InAppLogger.log("MainActivity.onResume")

        if (appPreferences.consumptionUnit) {
            DataHolder.consumptionPlotLine.Unit = "Wh/km"
            DataHolder.consumptionPlotLine.HighlightFormat = "Ø %.0f"
            DataHolder.consumptionPlotLine.LabelFormat = "%.0f"
            DataHolder.consumptionPlotLine.Divider = 1f
        } else {
            DataHolder.consumptionPlotLine.Unit = "kWh/100km"
            DataHolder.consumptionPlotLine.HighlightFormat = "Ø %.1f"
            DataHolder.consumptionPlotLine.LabelFormat = "%.1f"
            DataHolder.consumptionPlotLine.Divider = 10f
        }

        main_power_gage.maxValue = if (appPreferences.consumptionPlotSingleMotor) 170f else 300f
        main_power_gage.minValue = if (appPreferences.consumptionPlotSingleMotor) -100f else -150f

        main_power_gage.barVisibility = appPreferences.consumptionPlotVisibleGages
        main_consumption_gage.barVisibility = appPreferences.consumptionPlotVisibleGages

        main_checkbox_speed.isChecked = appPreferences.plotSpeed
        DataHolder.speedPlotLine.Visible = appPreferences.plotSpeed
        DataHolder.chargePlotLine.plotPaint = PlotPaint.byColor(getColor(R.color.charge_plot_color), PlotView.textSize)
        main_consumption_plot.invalidate()

        main_button_speed.text = if (DataHolder.speedPlotLine.Visible) {
            getString(R.string.main_button_hide_speed)
        } else {
            getString(R.string.main_button_show_speed)
        }

        enableUiUpdates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InAppLogger.log("MainActivity.onCreate")

        context = applicationContext

        appPreferences = AppPreferences(context)

        startForegroundService(Intent(this, DataCollector::class.java))

        mainActivityPendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        setContentView(R.layout.activity_main)
        setupDefaultUi()
        setUiEventListeners()

        timerHandler = Handler(Looper.getMainLooper())

        registerReceiver(
            broadcastReceiver,
            IntentFilter(getString(R.string.ui_update_plot_broadcast))
        )
        registerReceiver(
            broadcastReceiver,
            IntentFilter(getString(R.string.ui_update_gages_broadcast))
        )

        main_button_performance.isEnabled = false
        main_button_performance.colorFilter = PorterDuffColorFilter(getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)

        enableUiUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        disableUiUpdates()
        unregisterReceiver(broadcastReceiver)
        InAppLogger.log("MainActivity.onDestroy")
    }

    override fun onPause() {
        super.onPause()
        disableUiUpdates()
        InAppLogger.log("MainActivity.onPause")
    }

    /** Private functions */

    private fun updateActivity() {
        InAppLogger.logUIUpdate()
        /** Use data from DataHolder to Update MainActivity text */

        setUiVisibilities()
        updateGages()
        setValueColors()

        currentPowerTextView.text = getCurrentPowerString(false)
        currentSpeedTextView.text = getCurrentSpeedString()
        usedEnergyTextView.text = getUsedEnergyString()
        traveledDistanceTextView.text = getTraveledDistanceString()
        currentInstConsTextView.text = getInstConsumptionString()
        averageConsumptionTextView.text = getAvgConsumptionString()

        main_gage_avg_consumption_text_view.text = "  Ø %s".format(getAvgConsumptionString())
        main_gage_distance_text_view.text = "  %s".format(getTraveledDistanceString())
        main_gage_used_power_text_view.text = "  %s".format(getUsedEnergyString())
        main_gage_time_text_view.text = "  %s".format(getElapsedTimeString(DataHolder.travelTimeMillis))

        main_gage_charged_energy_text_view.text = "  %s".format(getChargedEnergyString())
        main_gage_charge_time_text_view.text = "  %s".format(getElapsedTimeString(DataHolder.chargeTimeMillis))
    }

    private fun getCurrentSpeedString(): String {
        return "${(DataHolder.currentSpeedSmooth * 3.6).toInt()} km/h"
    }

    private fun getAvgSpeedString(): String {
        return " %d km/h".format(
            ((DataHolder.traveledDistance / 1000) / (DataHolder.travelTimeMillis.toFloat() / 3_600_000)).toInt())
    }

    private fun getCurrentPowerString(detailed : Boolean = true): String {
        val rawPower = DataHolder.currentPowerSmooth / 1_000_000

        if (!detailed && rawPower.absoluteValue >= 10) {return "%d kW".format(
            Locale.ENGLISH,
            rawPower.roundToInt())}
        
        return "%.1f kW".format(
            Locale.ENGLISH,
            rawPower)
    }

    private fun getUsedEnergyString(): String {
        if (!appPreferences.consumptionUnit) {
            return "%.1f kWh".format(
                Locale.ENGLISH,
                DataHolder.usedEnergy / 1000)
        }
        return "${DataHolder.usedEnergy.toInt()} Wh"
    }

    private fun getChargedEnergyString(): String {
        if (!appPreferences.consumptionUnit) {
            return "%.1f kWh".format(
                Locale.ENGLISH,
                DataHolder.chargedEnergy / 1000)
        }
        return "${DataHolder.chargedEnergy.toInt()} Wh"
    }

    private fun getInstConsumptionString(): String {
        if (DataHolder.currentSpeed <= 0) {
            return "-/-"
        }
        if (!appPreferences.consumptionUnit) {
            return "%.1f kWh/100km".format(
                Locale.ENGLISH,
                ((DataHolder.currentPowerSmooth / 1000) / (DataHolder.currentSpeedSmooth * 3.6))/10)
        }
        return "${((DataHolder.currentPowerSmooth / 1000) / (DataHolder.currentSpeedSmooth * 3.6)).toInt()} Wh/km"
    }

    private fun getAvgConsumptionString(): String {
        val unitString = if (appPreferences.consumptionUnit) "Wh/km" else "kWh/100km"
        if (DataHolder.traveledDistance <= 0) {
            return "-/- $unitString"
        }
        if (!appPreferences.consumptionUnit) {
            return "%.1f %s".format(
                Locale.ENGLISH,
                (DataHolder.usedEnergy /(DataHolder.traveledDistance /1000))/10,
                unitString)
        }
        return "${(DataHolder.usedEnergy /(DataHolder.traveledDistance /1000)).toInt()} $unitString"
    }

    private fun getTraveledDistanceString(): String {
        return "%.1f km".format(Locale.ENGLISH, DataHolder.traveledDistance / 1000)
    }

    private fun setUiVisibilities() {
        if (appPreferences.experimentalLayout && legacy_layout.visibility == View.VISIBLE) {
            gage_layout.visibility = View.VISIBLE
            legacy_layout.visibility = View.GONE
        } else if (!appPreferences.experimentalLayout && legacy_layout.visibility == View.GONE) {
            gage_layout.visibility = View.GONE
            legacy_layout.visibility = View.VISIBLE
        }

        if (main_button_dismiss_charge_plot.isEnabled == DataHolder.chargePortConnected)
            main_button_dismiss_charge_plot.isEnabled = !DataHolder.chargePortConnected

        if (main_charge_layout.visibility == View.GONE && DataHolder.chargePortConnected) {
            main_consumption_layout.visibility = View.GONE
            main_charge_layout.visibility = View.VISIBLE
        }
    }

    private fun updateGages() {
        if ((DataHolder.currentPowerSmooth / 1_000_000).absoluteValue > 10 && true) { // Add Setting!
            val newValue = (DataHolder.currentPowerSmooth / 1_000_000).toInt()
            main_power_gage.setValue(newValue)
        } else {
            main_power_gage.setValue(DataHolder.currentPowerSmooth / 1_000_000)
        }
        main_charge_gage.setValue(-DataHolder.currentPowerSmooth / 1_000_000)
        main_SoC_gage.setValue((100f / DataHolder.maxBatteryCapacity * DataHolder.currentBatteryCapacity).roundToInt())

        var consumptionValue: Float? = null

        if (appPreferences.consumptionUnit) {
            main_consumption_gage.gageUnit = "Wh/km"
            main_consumption_gage.minValue = -300f
            main_consumption_gage.maxValue = 600f
            if (DataHolder.currentSpeed * 3.6 > 3) {
                main_consumption_gage.setValue(((DataHolder.currentPowerSmooth / 1000) / (DataHolder.currentSpeedSmooth * 3.6)).toInt())
            } else {
                main_consumption_gage.setValue(consumptionValue)
            }

        } else {
            main_consumption_gage.gageUnit = "kWh/100km"
            main_consumption_gage.minValue = -30f
            main_consumption_gage.maxValue = 60f
            if (DataHolder.currentSpeed * 3.6 > 3) {
                main_consumption_gage.setValue(((DataHolder.currentPowerSmooth / 1000) / (DataHolder.currentSpeedSmooth * 3.6f))/10)
            } else {
                main_consumption_gage.setValue(consumptionValue)
            }
        }
    }

    private fun updatePlots(){
        // if (appPreferences.plotDistance == 3) main_consumption_plot.dimensionRestriction = dimensionRestrictionById(appPreferences.plotDistance)

        if (SystemClock.elapsedRealtime() - lastPlotUpdate > 1_000L) {
            if (main_consumption_layout.visibility == View.VISIBLE) {
                main_consumption_plot.invalidate()
            }

            if (main_charge_layout.visibility == View.VISIBLE) {
                main_charge_plot.invalidate()
            }

            lastPlotUpdate = SystemClock.elapsedRealtime()
        }
    }

    private fun getElapsedTimeString(elapsedTime: Long): String {
        return String.format("%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(elapsedTime),
            TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % TimeUnit.MINUTES.toSeconds(1))
    }

    private fun setValueColors() {
        if (DataHolder.currentPowerSmooth > 0) {
            currentPowerTextView.setTextColor(Color.RED)
            currentInstConsTextView.setTextColor(Color.RED)
        }
        else {
            currentPowerTextView.setTextColor(Color.GREEN)
            currentInstConsTextView.setTextColor(Color.GREEN)
        }
    }

    // private fun dimensionRestrictionById(id : Int) : Long {
    //     return when (id) {
    //         1 -> DISTANCE_1
    //         2 -> DISTANCE_2
    //         3 -> ((DataHolder.traveledDistance / DISTANCE_TRIP_DIVIDER).toInt() + 1) * DISTANCE_TRIP_DIVIDER + 1
    //         else -> DISTANCE_2
    //     }
    // }

    private fun resetStats() {
        finish()
        startActivity(intent)
        InAppLogger.log("MainActivity.resetStats")

        traveledDistanceTextView.text = String.format("%.3f km", DataHolder.traveledDistance / 1000)
        usedEnergyTextView.text = String.format("%d Wh", DataHolder.usedEnergy.toInt())
        averageConsumptionTextView.text = String.format("%d Wh/km", DataHolder.averageConsumption.toInt())
        DataHolder.resetDataHolder()
        sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
    }

    private fun setupDefaultUi() {

        var plotDistanceId = when (appPreferences.plotDistance) {
            1 -> main_radio_10.id
            2 -> main_radio_25.id
            3 -> main_radio_50.id
            else -> main_radio_10.id
        }

        main_radio_group_distance.check(plotDistanceId)

        if (appPreferences.experimentalLayout) {
            legacy_layout.visibility = View.GONE
            gage_layout.visibility = View.VISIBLE
        }

        main_consumption_plot.reset()
        main_consumption_plot.addPlotLine(DataHolder.consumptionPlotLine)
        main_consumption_plot.addPlotLine(DataHolder.speedPlotLine)
        // main_consumption_plot.setPlotMarkers(DataHolder.plotMarkers)
        // main_consumption_plot.visibleMarkerTypes.add(PlotMarkerType.CHARGE)
        // main_consumption_plot.visibleMarkerTypes.add(PlotMarkerType.PARK)

        DataHolder.speedPlotLine.Visible = appPreferences.plotSpeed
        main_button_speed.text = if (DataHolder.speedPlotLine.Visible) {
            getString(R.string.main_button_hide_speed)
        } else {
            getString(R.string.main_button_show_speed)
        }

        if (appPreferences.consumptionPlotSecondaryColor) {
            DataHolder.speedPlotLine.plotPaint = PlotPaint.byColor(getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
        }

        main_consumption_plot.dimension = PlotDimension.DISTANCE
        main_consumption_plot.dimensionRestriction = 10_001L
        main_consumption_plot.dimensionSmoothingPercentage = 0.02f
        main_consumption_plot.invalidate()

        main_charge_plot.reset()
        main_charge_plot.addPlotLine(DataHolder.chargePlotLine)
        main_charge_plot.addPlotLine(DataHolder.stateOfChargePlotLine)

        if (appPreferences.chargePlotSecondaryColor) {
            DataHolder.stateOfChargePlotLine.plotPaint = PlotPaint.byColor(getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
        }

        main_charge_plot.dimension = PlotDimension.TIME
        main_charge_plot.dimensionRestriction = null
        main_charge_plot.dimensionSmoothingPercentage = 0.01f
        main_charge_plot.invalidate()

        main_power_gage.gageName = getString(R.string.main_gage_power)
        main_power_gage.gageUnit = "kW"
        main_power_gage.maxValue = if (appPreferences.consumptionPlotSingleMotor) 170f else 300f
        main_power_gage.minValue = if (appPreferences.consumptionPlotSingleMotor) -100f else -150f
        main_power_gage.setValue(0f)

        main_consumption_gage.gageName = getString(R.string.main_gage_consumption)
        main_consumption_gage.gageUnit = "kWh/100km"
        main_consumption_gage.minValue = -30f
        main_consumption_gage.maxValue = 60f
        main_consumption_gage.setValue(0f)

        main_charge_gage.gageName = getString(R.string.main_gage_charging_power)
        main_charge_gage.gageUnit = "kW"
        main_charge_gage.primaryColor = getColor(R.color.charge_plot_color)
        main_charge_gage.minValue = 0f
        main_charge_gage.maxValue = 160f
        main_charge_gage.setValue(0f)

        main_SoC_gage.gageName = getString(R.string.main_gage_SoC)
        main_SoC_gage.gageUnit = "%"
        main_SoC_gage.primaryColor = getColor(R.color.charge_plot_color)
        main_SoC_gage.minValue = 0f
        main_SoC_gage.maxValue = 100f
        main_SoC_gage.setValue(0f)
    }

    private fun setUiEventListeners() {

        main_title.setOnClickListener {
            if (emulatorMode) {
                DataHolder.currentGear = when (DataHolder.currentGear) {
                    VehicleGear.GEAR_PARK -> {
                        Toast.makeText(this, "Drive", Toast.LENGTH_SHORT).show()
                        // DataHolder.resetTimestamp += (System.nanoTime() - DataHolder.parkTimestamp)
                        DataHolder.plotMarkers.endMarker(SystemClock.elapsedRealtimeNanos())
                        VehicleGear.GEAR_DRIVE
                    }
                    else -> {
                        Toast.makeText(this, "Park", Toast.LENGTH_SHORT).show()
                        // DataHolder.parkTimestamp = System.nanoTime()
                        DataHolder.plotMarkers.addMarker(PlotMarkerType.PARK, SystemClock.elapsedRealtimeNanos())
                        VehicleGear.GEAR_PARK
                    }
                }
                sendBroadcast(Intent(getString(R.string.gear_update_broadcast)))
                if (DataHolder.currentGear == VehicleGear.GEAR_PARK) sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
            }
        }
        main_title_icon.setOnClickListener {
            if (emulatorMode) {
                emulatorPowerSign = if (emulatorPowerSign < 0) 1
                else -1
                Toast.makeText(this, "Power sign: ${if(emulatorPowerSign<0) "-" else "+"}", Toast.LENGTH_SHORT).show()
            }
        }

        main_button_reset.setOnClickListener {

            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle(getString(R.string.main_dialog_reset_title))
                .setMessage(getString(R.string.main_dialog_reset_message))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.dialog_confirm)) { dialog, id ->
                    resetStats()
                }
                .setNegativeButton(getString(R.string.dialog_dismiss)) { dialog, id ->
                    // Dismiss the dialog
                    InAppLogger.log("Dismiss reset but refresh MainActivity")
                    finish()
                    startActivity(intent)
                }
            val alert = builder.create()
            alert.show()
        }

        main_button_settings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        /** cycle through consumption plot distances when tapping the plot */
        // main_consumption_plot.setOnClickListener {
        //     main_consumption_plot.dimensionRestriction = 5_000L
        // }

        /* main_radio_group_distance.setOnCheckedChangeListener { group, checkedId ->
            var id = when (checkedId) {
                main_radio_10.id -> 1
                main_radio_25.id -> 2
                main_radio_50.id -> 3
                else -> 1
            }

            main_consumption_plot.dimensionRestriction = dimensionRestrictionById(id)

            appPreferences.plotDistance = id
        }

        main_checkbox_speed.setOnClickListener {
            if (main_checkbox_speed.isChecked && !DataHolder.speedPlotLine.Visible) {
                DataHolder.speedPlotLine.Visible = true
            } else if (!main_checkbox_speed.isChecked && DataHolder.speedPlotLine.Visible) {
                DataHolder.speedPlotLine.Visible = false
            }

            appPreferences.plotSpeed = main_checkbox_speed.isChecked
            main_consumption_plot.invalidate()
        } */

        main_button_speed.setOnClickListener {
            DataHolder.speedPlotLine.Visible = !DataHolder.speedPlotLine.Visible
            appPreferences.plotSpeed = DataHolder.speedPlotLine.Visible
            main_consumption_plot.invalidate()
            main_button_speed.text = if (DataHolder.speedPlotLine.Visible)
                getString(R.string.main_button_hide_speed)
            else getString(R.string.main_button_show_speed)
        }

        main_button_summary.setOnClickListener {
            // sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
            startActivity(Intent(this, SummaryActivity::class.java))
        }

        main_button_dismiss_charge_plot.setOnClickListener {
            main_charge_layout.visibility = View.GONE
            main_consumption_layout.visibility = View.VISIBLE
            main_consumption_plot.invalidate()
            DataHolder.chargedEnergy = 0f
            DataHolder.chargeTimeMillis = 0L
        }

        main_button_reset_charge_plot.setOnClickListener {
            main_charge_plot.reset()
        }
    }

    private fun enableUiUpdates() {
        InAppLogger.log("Enabling UI updates")
        updateUi = true
        if (this::timerHandler.isInitialized) {
            timerHandler.removeCallbacks(updateActivityTask)
            timerHandler.post(updateActivityTask)
        }

    }

    private fun disableUiUpdates() {
        InAppLogger.log("Disabling UI Updates")
        updateUi = false
        if (this::timerHandler.isInitialized) {
            timerHandler.removeCallbacks(updateActivityTask)
        }
    }

}
