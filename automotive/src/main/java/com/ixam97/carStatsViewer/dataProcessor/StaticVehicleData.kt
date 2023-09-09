package com.mbuehler.carStatsViewer.dataProcessor

data class StaticVehicleData(
    val batteryCapacity: Float? = null,
    val vehicleMake: String? = null,
    val modelName: String? = null
) {
    fun isInitialized(): Boolean = batteryCapacity != null
}
