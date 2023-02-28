package com.mbuehler.carStatsViewer.dataManager

import android.car.VehicleIgnitionState

class DrivingState(private val ChargePortConnected: VehicleProperty, private val CurrentIgnitionState: VehicleProperty) {
    // States
    companion object {
        val UNKNOWN = -1
        val PARKED = 0
        val DRIVE = 1
        val CHARGE = 2

        val nameMap = mapOf<Int, String>(
            UNKNOWN to "UNKNOWN",
            PARKED to "PARKED",
            DRIVE to "DRIVE",
            CHARGE to "CHARGE"
        )
    }

    var lastDriveState: Int = UNKNOWN

    /** Check weather the DrivingState has changed since the last time this function has been called. */
    fun hasChanged(): Boolean {
        if (getDriveState() != lastDriveState) {
            lastDriveState = getDriveState()
            return true
        }
        return false
    }

    /** Get the current DrivingState independent of hasChanged(). */
    fun getDriveState(): Int {
        val chargePortConnected = (ChargePortConnected.value ?: false) as Boolean
        val currentIgnitionState = (CurrentIgnitionState.value ?: 0) as Int
        return if (chargePortConnected) CHARGE
        else if (currentIgnitionState == VehicleIgnitionState.START) DRIVE
        else if (currentIgnitionState != VehicleIgnitionState.UNDEFINED) PARKED
        else UNKNOWN
    }
}