package com.dmgritsan.pockettelemetry
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLoggingTree(private val logFile: File) : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        try {
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            FileWriter(logFile, true).apply {
                append("$timeStamp [$tag] ${priorityToString(priority)}: $message")
                t?.let {
                    append("\n").append(Log.getStackTraceString(it))
                }
                append("\n")
                flush()
                close()
            }
        } catch (e: IOException) {
            Log.e("Timber", "Error writing log to file", e)
        }
    }

    private fun priorityToString(priority: Int): String {
        return when (priority) {
            Log.VERBOSE -> "VERBOSE"
            Log.DEBUG -> "DEBUG"
            Log.INFO -> "INFO"
            Log.WARN -> "WARN"
            Log.ERROR -> "ERROR"
            Log.ASSERT -> "ASSERT"
            else -> "UNKNOWN"
        }
    }
}
