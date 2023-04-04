package com.mbuehler.carStatsViewer.liveData

import android.content.Context
import android.content.Intent
import android.os.Handler
import com.mbuehler.carStatsViewer.CarStatsViewer
import com.mbuehler.carStatsViewer.dataManager.DataManager
import com.mbuehler.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

abstract class LiveDataApi(
    val broadcastAction: String,
    var detailedLog: Boolean
    ){

    /**
     * Indicates the current connection status of the API
     *      0: Unused
     *      1: Connected
     *      2: Error
     */
    var connectionStatus: ConnectionStatus = ConnectionStatus.UNUSED
    var timeout: Int = 5_000

    enum class ConnectionStatus(val status: Int) {
        UNUSED(0),
        CONNECTED(1),
        ERROR(2);

        companion object {
            fun fromInt(status: Int) = values().first { it.status == status }
        }
    }

    /**
     * Dialog to setup API.
     */
    abstract fun showSettingsDialog(context: Context)

    /**
     * creates a runnable to be executed in intervals. Returns null if API does not send data in
     * timed intervals.
     */
    open fun createLiveDataTask(
        dataManager: DataManager,
        handler: Handler,
        interval: Long
    ): Runnable? {
        timeout = interval.toInt()
        return object : Runnable {
            override fun run() {
                coroutineSendNow(dataManager)
                handler.postDelayed(this, interval)
            }
        }
    }

    /**
     * sendNow, but wrapped in a coroutine to not block main thread.
     */
    fun coroutineSendNow(dataManager: DataManager) {
        CoroutineScope(Dispatchers.Default).launch {
            sendNow(dataManager)
            sendStatusBroadcast(CarStatsViewer.appContext)
        }
    }

    fun requestFlow(serviceScope: CoroutineScope, dataManager: DataManager, interval: Long): Flow<Unit> {
        timeout = interval.toInt()
        return flow {
            while (true) {
                coroutineSendNow(dataManager)
                delay(interval)
            }
        }
    }

    /**
     * Code to be executed in coroutineSendNow. This function should not be called outside a
     * coroutine to not block main thread.
     */
    protected abstract fun sendNow(dataManager: DataManager)

    private fun sendStatusBroadcast(context: Context) {
        val broadcastIntent = Intent(broadcastAction)
        broadcastIntent.putExtra("status", connectionStatus.status)
        context.sendBroadcast(broadcastIntent)
    }
}