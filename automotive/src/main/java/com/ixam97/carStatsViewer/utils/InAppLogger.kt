package com.mbuehler.carStatsViewer.utils

import android.util.Log
import com.mbuehler.carStatsViewer.BuildConfig
import com.mbuehler.carStatsViewer.CarStatsViewer
import com.mbuehler.carStatsViewer.appPreferences.AppPreference
import com.mbuehler.carStatsViewer.appPreferences.AppPreferences
import com.mbuehler.carStatsViewer.database.log.LogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

val AppPreferences.LogLevel: AppPreference<Int>
    get() = AppPreference<Int>("preference_log_level_2", 3, sharedPref)
val AppPreferences.LogLength: AppPreference<Int>
    get() = AppPreference<Int>("preference_log_length", 4, sharedPref)

var AppPreferences.logLevel: Int get() = LogLevel.value; set(value) {LogLevel.value = value}
var AppPreferences.logLength: Int get() = LogLength.value; set(value) {LogLength.value = value}

object InAppLogger {

    // number of days to keep logs, delete after
    private const val deleteDays: Int = 28

    private val _realTimeLog = MutableStateFlow<LogEntry?>(null)
    val realTimeLog = _realTimeLog.asStateFlow()

    private var lastTrim = 0L

    fun typeSymbol(type: Int): String = when (type) {
        Log.VERBOSE -> "V"
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        else -> "?"
    }

    // private val logDatabase = Room.databaseBuilder(
    //     CarStatsViewer.appContext,
    //     LogDatabase::class.java,
    //     "LogDatabase"
    // ).build()
    private val logDao = CarStatsViewer.logDao

    fun v(message: String) = log(message, Log.VERBOSE)
    fun d(message: String) = log(message, Log.DEBUG)
    fun i(message: String) = log(message, Log.INFO)
    fun w(message: String) = log(message, Log.WARN)
    fun e(message: String) = log(message, Log.ERROR)

    fun log(message: String, type: Int = Log.INFO) {
        val logTime = System.currentTimeMillis()
        val messageTime = SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(logTime)
        Log.println(type,"InAppLogger:", "$messageTime ${typeSymbol(type)}: $message")

        // respect the log level setting when writing to database to reduce stress.
        if (type < CarStatsViewer.appPreferences.logLevel + 2) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newLogEntry = LogEntry(
                    epochTime = logTime,
                    type = type,
                    message = message
                )
                logDao.insert(newLogEntry)

                // Delete old log entries older than 'deleteDays', only trim once every 24h.
                val currentTime = System.currentTimeMillis()
                if (currentTime > lastTrim + 86_400_000L) {
                    lastTrim = currentTime
                    val trimTime = currentTime - (86_400_000L * deleteDays)
                    logDao.trim(trimTime)
                }

                _realTimeLog.value = newLogEntry// "${SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(newLogEntry.epochTime)} ${typeSymbol(newLogEntry.type)}: ${newLogEntry.message}"
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getLogString(logLevel: Int = Log.VERBOSE, logLength: Int = 0): String {
        val logStringBuilder = StringBuilder()
        try {
            val startTime = System.currentTimeMillis()
            val logEntries: List<LogEntry> = if (logLength == 0) logDao.getLevel(logLevel) else logDao.getLevelAndLength(logLevel, logLength).reversed()
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

    fun getLogArray(logLevel: Int = Log.VERBOSE, logLength: Int = 0): List<String> {
        val logEntries: List<LogEntry> = if (logLength == 0) logDao.getLevel(logLevel) else logDao.getLevelAndLength(logLevel, logLength).reversed()
        return logEntries.map {
            "${SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS").format(it.epochTime)} ${typeSymbol(it.type)}: ${it.message}"
        }
    }

    fun getLogEntries(logLevel: Int = Log.VERBOSE, logLength: Int = 0) = if (logLength == 0) logDao.getLevel(logLevel) else logDao.getLevelAndLength(logLevel, logLength).reversed()

    suspend fun resetLog() {
        try {
            logDao.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}