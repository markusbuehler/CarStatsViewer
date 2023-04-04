package com.mbuehler.carStatsViewer.liveData.abrpLiveData

/**
 * This file extends the AppPreferences to contain keys used by the API implementation. The rest of
 * the app does not need to see them.
 */

import com.mbuehler.carStatsViewer.appPreferences.AppPreference
import com.mbuehler.carStatsViewer.appPreferences.AppPreferences

val AppPreferences.AbrpGenericToken: AppPreference<String>
    get() = AppPreference<String>("preference_abrp_generic_token", "", sharedPref)

val AppPreferences.AbrpUseApi: AppPreference<Boolean>
    get() = AppPreference<Boolean>("preference_abrp_use", false, sharedPref)

var AppPreferences.abrpGenericToken: String get() = AbrpGenericToken.value; set(value) {AbrpGenericToken.value = value}
var AppPreferences.abrpUseApi: Boolean get() = AbrpUseApi.value; set(value) {AbrpUseApi.value = value}