package com.mbuehler.carStatsViewer.compose

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mbuehler.carStatsViewer.R
import com.mbuehler.carStatsViewer.compose.screens.SettingsScreen
import com.mbuehler.carStatsViewer.compose.theme.CarTheme

class ComposeSettingsActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val themeSetting = settingsViewModel.themeSettingStateFLow.collectAsState()

            settingsViewModel.finishActivityLiveData.observe(this) {
                if (it.consume() == true) finish()
            }

            CarTheme( if (themeSetting.value == 0) Build.BRAND else null ) {
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}