package com.mbuehler.carStatsViewer.carPropertiesClient

import android.car.VehiclePropertyIds

object CarProperties {

    const val PERF_VEHICLE_SPEED = VehiclePropertyIds.PERF_VEHICLE_SPEED
    const val EV_BATTERY_INSTANTANEOUS_CHARGE_RATE = VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE
    const val GEAR_SELECTION = VehiclePropertyIds.GEAR_SELECTION
    const val EV_CHARGE_PORT_CONNECTED = VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED
    const val EV_BATTERY_LEVEL = VehiclePropertyIds.EV_BATTERY_LEVEL
    const val IGNITION_STATE = VehiclePropertyIds.IGNITION_STATE
    const val ENV_OUTSIDE_TEMPERATURE = VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE

    val usedProperties = listOf(
        VehiclePropertyIds.PERF_VEHICLE_SPEED,
        VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE,
        VehiclePropertyIds.GEAR_SELECTION,
        VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED,
        VehiclePropertyIds.EV_BATTERY_LEVEL,
        VehiclePropertyIds.IGNITION_STATE,
        VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE,
    )
    fun getNameById(propertyId: Int) = when (propertyId) {
        VehiclePropertyIds.PERF_VEHICLE_SPEED -> "Speed"
        VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE -> "Power"
        VehiclePropertyIds.GEAR_SELECTION -> "Gear selection"
        VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED -> "Charge port connected"
        VehiclePropertyIds.EV_BATTERY_LEVEL -> "Battery level"
        VehiclePropertyIds.IGNITION_STATE -> "Ignition state"
        VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE -> "Outside temperature"
        else -> "Unused car property"
    }
}