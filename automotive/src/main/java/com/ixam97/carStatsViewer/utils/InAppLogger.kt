package com.mbuehler.carStatsViewer.utils

import android.util.Log
import androidx.room.Room
import com.mbuehler.carStatsViewer.BuildConfig
import com.mbuehler.carStatsViewer.CarStatsViewer
import com.mbuehler.carStatsViewer.R
import com.mbuehler.carStatsViewer.appPreferences.AppPreference
import com.mbuehler.carStatsViewer.appPreferences.AppPreferences
import com.mbuehler.carStatsViewer.database.log.LogDatabase
import com.mbuehler.carStatsViewer.database.log.LogEntry
import kotlinx.coroutines.*
import java.text.SimpleDateFormat

val AppPreferences.LogLevel: AppPreference<Int>
    get() = AppPreference<Int>("preference_log_level", Log.VERBOSE, sharedPref)

var AppPreferences.logLevel: Int get() = LogLevel.value; set(value) {LogLevel.value = value}

object InAppLogger {

    private fun typeSymbol(type: Int): String = when (type) {
        Log.VERBOSE -> "V"
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        else -> "?"
    }

    private val logDatabase = Room.databaseBuilder(
        CarStatsViewer.appContext,
        LogDatabase::class.java,
        "LogDatabase"
    ).build()
    private val logDao = logDatabase.logDao()

    fun v(message: String) = log(message, Log.VERBOSE)
    fun d(message: String) = log(message, Log.DEBUG)
    fun i(message: String) = log(message, Log.INFO)
    fun w(message: String) = log(message, Log.WARN)
    fun e(message: String) = log(message, Log.ERROR)

    fun log(message: String, type: Int = Log.INFO) {
        val logTime = System.currentTimeMillis()
        val messageTime = SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(logTime)
        Log.println(type,"InAppLogger:", "$messageTime ${typeSymbol(type)}: $message")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                logDao.insert(LogEntry(
                    epochTime = logTime,
                    type = type,
                    message = message))
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getLogString(logLevel: Int = Log.VERBOSE): String {
        val logStringBuilder = StringBuilder()
        try {
            val startTime = System.currentTimeMillis()
            val logEntries: List<LogEntry> = logDao.getLevel(logLevel)
            val loadedTime = System.currentTimeMillis()
            logEntries.forEach {
                if (it.message.contains("Car Stats Viewer")) logStringBuilder.append("------------------------------------------------------------\n")
                logStringBuilder.append("${SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(it.epochTime)} ${typeSymbol(it.type)}: ${it.message}\n")
            }
            logStringBuilder
                .append("------------------------------------------------------------\n")
                .append("Loaded ${logEntries.size} log entries in ${loadedTime - startTime}ms, string built in ${System.currentTimeMillis() - startTime}ms\n")
                .append("V${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})")
        } catch (e: java.lang.Exception) {
            resetLog()
            e("Loading Log failed. It has been reset.\n${e.stackTraceToString()}")
        }
        return logStringBuilder.toString()
    }

    fun getLogArray(logLevel: Int = Log.VERBOSE): List<String> {
        return logDao.getLevel(logLevel).map {
            "${SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(it.epochTime)} ${typeSymbol(it.type)}: ${it.message}\n"
        }
    }

    fun resetLog() {
        try {
            logDao.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}