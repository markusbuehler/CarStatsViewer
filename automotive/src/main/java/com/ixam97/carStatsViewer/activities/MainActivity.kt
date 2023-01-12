package com.mbuehler.carStatsViewer.activities

import com.mbuehler.carStatsViewer.*
import com.mbuehler.carStatsViewer.objects.*
import com.mbuehler.carStatsViewer.services.*
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.car.Car
import android.car.VehicleGear
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.Toast
import com.mbuehler.carStatsViewer.plot.PlotDimension
import com.mbuehler.carStatsViewer.plot.PlotPaint
import com.mbuehler.carStatsViewer.views.PlotView
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.concurrent.TimeUnit

var emulatorMode = false
var emulatorPowerSign = -1

const val SETTINGS_REQUEST_CODE = 1

class MainActivity : Activity() {
    companion object {
        private val permissions = arrayOf(Car.PERMISSION_ENERGY, Car.PERMISSION_SPEED)
        private const val uiUpdateDelayMillis = 200L
    }

    /** values and variables */

    private lateinit var timerHandler: Handler
    private lateinit var starterIntent: Intent
    private lateinit var context: Context
    private lateinit var appPreferences: AppPreferences

    private var updateUi = false
    private var lastPlotUpdate: Long = 0L
    private var lastTimeString = "00:00:00"

    private val updateActivityTask = object : Runnable {
        override fun run() {
            updateActivity()
            if (updateUi) timerHandler.postDelayed(this, uiUpdateDelayMillis)
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


        if (appPreferences.consumptionPlotSecondaryColor) {
            DataHolder.speedPlotLine.plotPaint = PlotPaint.byColor(Color.LTGRAY, main_consumption_plot.textSize)
        } else {
            DataHolder.speedPlotLine.plotPaint = PlotPaint.byColor(Color.parseColor("#00BF00"), main_consumption_plot.textSize)
        }
        
        main_power_gage.maxValue = if (appPreferences.consumptionPlotSingleMotor) 170f else 300f
        main_power_gage.minValue = if (appPreferences.consumptionPlotSingleMotor) -100f else -150f

        main_power_gage.barVisibility = appPreferences.consumptionPlotVisibleGages
        main_consumption_gage.barVisibility = appPreferences.consumptionPlotVisibleGages

        enableUiUpdates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InAppLogger.log("MainActivity.onCreate")

        context = applicationContext

        appPreferences = AppPreferences(context)

        startForegroundService(Intent(this, DataCollector::class.java))

        mainActivityPendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        starterIntent = intent

        checkPermissions()
        setContentView(R.layout.activity_main)
        setupDefaultUi()
        setUiEventListeners()

        timerHandler = Handler(Looper.getMainLooper())
        enableUiUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        disableUiUpdates()
        InAppLogger.log("MainActivity.onDestroy")
    }

    override fun onPause() {
        super.onPause()
        disableUiUpdates()
        InAppLogger.log("MainActivity.onPause")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        InAppLogger.log("onRequestPermissionResult")
        if(grantResults[0]==PackageManager.PERMISSION_GRANTED)
        {
            finish()
            startActivity(starterIntent)
            resetStats()
        }
    }

    /** Private functions */

    private fun updateActivity() {
        InAppLogger.logUIUpdate()
        /** Use data from DataHolder to Update MainActivity text */

        setUiVisibilities()

        updatePlots()
        updateGages()

        setValueColors()

        currentPowerTextView.text = getCurrentPowerString()
        currentSpeedTextView.text = getCurrentSpeedString()
        usedEnergyTextView.text = getUsedEnergyString()
        traveledDistanceTextView.text = getTraveledDistanceString()
        currentInstConsTextView.text = getInstConsumptionString()
        averageConsumptionTextView.text = getAvgConsumptionString()

        main_gage_avg_consumption_text_view.text = "  Ø %s".format(getAvgConsumptionString())
        main_gage_distance_text_view.text = "  %s".format(getTraveledDistanceString())
        main_gage_used_power_text_view.text = "  %s".format(getUsedEnergyString())
        main_gage_time_text_view.text = "  %s".format(getElapsedTimeString())
    }

    private fun getCurrentSpeedString(): String {
        return "${(DataHolder.currentSpeedSmooth * 3.6).toInt()} km/h"
    }

    private fun getAvgSpeedString(): String {
        return " %d km/h".format(
            ((DataHolder.traveledDistance / 1000) / (DataHolder.travelTimeMillis.toFloat() / 3_600_000)).toInt())
    }

    private fun getCurrentPowerString(): String {
        return "%.1f kW".format(
            Locale.ENGLISH,
            DataHolder.currentPowerSmooth / 1_000_000)
    }

    private fun getUsedEnergyString(): String {
        if (!appPreferences.consumptionUnit) {
            return "%.1f kWh".format(
                Locale.ENGLISH,
                DataHolder.usedEnergy / 1000)
        }
        return "${DataHolder.usedEnergy.toInt()} Wh"
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
        if (DataHolder.traveledDistance <= 0) {
            return "-/-"
        }
        if (!appPreferences.consumptionUnit) {
            return "%.1f kWh/100km".format(
                Locale.ENGLISH,
                (DataHolder.usedEnergy /(DataHolder.traveledDistance /1000))/10)
        }
        return "${(DataHolder.usedEnergy /(DataHolder.traveledDistance /1000)).toInt()} Wh/km"
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

        if (main_consumption_plot_container.visibility == View.GONE && !DataHolder.chargePortConnected) {
            main_consumption_plot_container.visibility = View.VISIBLE
        }
        else if (main_consumption_plot_container.visibility == View.VISIBLE && DataHolder.chargePortConnected) {
            main_consumption_plot_container.visibility = View.GONE
        }

        if (main_charge_plot_container.visibility == View.GONE && DataHolder.chargePortConnected) {
            main_charge_plot.reset()
            main_charge_plot_container.visibility = View.VISIBLE
        }
        else if (main_charge_plot_container.visibility == View.VISIBLE && !DataHolder.chargePortConnected) {
            main_charge_plot_container.visibility = View.GONE
        }
    }

    private fun updateGages() {
        main_power_gage.setValue(DataHolder.currentPowerSmooth / 1000000)
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
        if (appPreferences.plotDistance == 3) main_consumption_plot.dimensionRestriction = dimensionRestrictionById(appPreferences.plotDistance)

        if (SystemClock.elapsedRealtime() - lastPlotUpdate > 1_000L) {
            if (main_consumption_plot_container.visibility == View.VISIBLE) {
                main_consumption_plot.invalidate()
            }

            if (main_charge_plot_container.visibility == View.VISIBLE) {
                main_charge_plot.invalidate()
            }

            lastPlotUpdate = SystemClock.elapsedRealtime()
        }
    }

    private fun getElapsedTimeString(): String {
        return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(DataHolder.travelTimeMillis),
            TimeUnit.MILLISECONDS.toMinutes(DataHolder.travelTimeMillis) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(DataHolder.travelTimeMillis) % TimeUnit.MINUTES.toSeconds(1))
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

    private fun dimensionRestrictionById(id : Int) : Long {
        return when (id) {
            1 -> 5_001L
            2 -> 15_001L
            3 -> ((DataHolder.traveledDistance / 5_000).toInt() + 1) * 5_000L + 1
            else -> 10_001L
        }
    }

    private fun dimensionSmoothingById(id : Int) : Long {
        return when (id) {
            1 -> 100L
            2 -> 500L
            3 -> 1_000L
            else -> 100L
        }
    }

    private fun checkPermissions() {
        if(checkSelfPermission(Car.PERMISSION_ENERGY) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions, 0)
        }
        if(checkSelfPermission(Car.PERMISSION_SPEED) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions, 1)
        }
    }

    private fun resetStats() {
        finish()
        startActivity(intent)
        InAppLogger.log("MainActivity.resetStats")
        main_consumption_plot.reset()

        DataHolder.traveledDistance = 0F
        traveledDistanceTextView.text = String.format("%.3f km", DataHolder.traveledDistance / 1000)
        DataHolder.usedEnergy = 0F
        usedEnergyTextView.text = String.format("%d Wh", DataHolder.usedEnergy.toInt())
        DataHolder.averageConsumption = 0F
        averageConsumptionTextView.text = String.format("%d Wh/km", DataHolder.averageConsumption.toInt())
        DataHolder.travelTimeMillis = 0L

    }

    private fun setupDefaultUi() {

        main_checkbox_speed.isChecked = appPreferences.plotSpeed
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

        DataHolder.speedPlotLine.Visible = main_checkbox_speed.isChecked

        main_consumption_plot.dimension = PlotDimension.DISTANCE
        main_consumption_plot.dimensionRestriction = dimensionRestrictionById(appPreferences.plotDistance)
        main_consumption_plot.dimensionSmoothing = dimensionSmoothingById(appPreferences.plotDistance)
        main_consumption_plot.invalidate()

        main_charge_plot.reset()
        main_charge_plot.addPlotLine(DataHolder.chargePlotLine)
        main_charge_plot.addPlotLine(DataHolder.stateOfChargePlotLine)

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
    }

    private fun setUiEventListeners() {

        main_title.setOnClickListener {
            if (emulatorMode) {
                DataHolder.currentGear = when (DataHolder.currentGear) {
                    VehicleGear.GEAR_PARK -> {
                        Toast.makeText(this, "Drive", Toast.LENGTH_SHORT).show()
                        // DataHolder.resetTimestamp += (System.nanoTime() - DataHolder.parkTimestamp)
                        VehicleGear.GEAR_DRIVE
                    }
                    else -> {
                        Toast.makeText(this, "Park", Toast.LENGTH_SHORT).show()
                        // DataHolder.parkTimestamp = System.nanoTime()
                        VehicleGear.GEAR_PARK
                    }
                }
            }
        }
        main_title_icon.setOnClickListener {
            if (emulatorMode) {
                emulatorPowerSign = if (emulatorPowerSign < 0) 1
                else -1
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
            startActivityForResult(Intent(this, SettingsActivity::class.java), SETTINGS_REQUEST_CODE)
        }

        /** cycle through consumption plot distances when tapping the plot */
        main_consumption_plot.setOnClickListener {
            var plotDistanceId = main_radio_10.id

            appPreferences.plotDistance++

            if (appPreferences.plotDistance >= 4) appPreferences.plotDistance = 1

            when (appPreferences.plotDistance) {
                1 -> plotDistanceId = main_radio_10.id
                2 -> plotDistanceId = main_radio_25.id
                3 -> plotDistanceId = main_radio_50.id
            }
            main_radio_group_distance.check(plotDistanceId)
        }

        main_radio_group_distance.setOnCheckedChangeListener { group, checkedId ->
            var id = when (checkedId) {
                main_radio_10.id -> 1
                main_radio_25.id -> 2
                main_radio_50.id -> 3
                else -> 1
            }

            main_consumption_plot.dimensionRestriction = dimensionRestrictionById(id)
            main_consumption_plot.dimensionSmoothing = dimensionSmoothingById(id)

            appPreferences.plotDistance = id

            // sharedPref.edit()
            //     .putInt(getString(R.string.preferences_plot_distance_key), id)
            //     .apply()
            // appPreferences.plotDistance = id
        }

        main_checkbox_speed.setOnClickListener {
            if (main_checkbox_speed.isChecked && !DataHolder.speedPlotLine.Visible) {
                DataHolder.speedPlotLine.Visible = true
            } else if (!main_checkbox_speed.isChecked && DataHolder.speedPlotLine.Visible) {
                DataHolder.speedPlotLine.Visible = false
            }

            appPreferences.plotSpeed = main_checkbox_speed.isChecked

            // sharedPref.edit()
            //     .putBoolean(getString(R.string.preferences_plot_speed_key), main_checkbox_speed.isChecked)
            //     .apply()
            // appPreferences.plotSpeed = main_checkbox_speed.isChecked

            main_consumption_plot.invalidate()
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
