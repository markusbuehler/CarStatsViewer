package com.mbuehler.carStatsViewer.utils

import android.app.Activity
import com.mbuehler.carStatsViewer.CarStatsViewer
import com.mbuehler.carStatsViewer.R

fun setContentViewAndTheme(context: Activity, resId: Int) {
    if (CarStatsViewer.appPreferences.colorTheme > 0) context.setTheme(R.style.ColorTestTheme)
    context.setContentView(resId)
}