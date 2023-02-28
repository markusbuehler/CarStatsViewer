package com.mbuehler.carStatsViewer.appPreferences

import android.content.SharedPreferences
import com.mbuehler.carStatsViewer.enums.DistanceUnitEnum
import com.mbuehler.carStatsViewer.plot.enums.PlotDimension

class AppPreference<T>(
    private val key: String,
    private val default: T,
    private val sharedPref: SharedPreferences) {

    var value: T
        get() {
            return when (default) {
                is Boolean -> sharedPref.getBoolean(key, default) as T
                is Int -> sharedPref.getInt(key, default) as T
                is String -> sharedPref.getString(key, default) as T
                is PlotDimension -> PlotDimension.valueOf(sharedPref.getString(key, default.name)!!) as T
                is DistanceUnitEnum -> DistanceUnitEnum.valueOf(sharedPref.getString(key, default.name)!!) as T
                else -> default
            }
        }
        set(value) {
            when (default) {
                is Boolean -> sharedPref.edit().putBoolean(key, value as Boolean).apply()
                is Int -> sharedPref.edit().putInt(key, value as Int).apply()
                is String -> sharedPref.edit().putString(key, value as String).apply()
                is PlotDimension -> sharedPref.edit().putString(key, (value as PlotDimension).name).apply()
                is DistanceUnitEnum ->sharedPref.edit().putString(key, (value as DistanceUnitEnum).name).apply()
                //else ->
            }
        }
}

