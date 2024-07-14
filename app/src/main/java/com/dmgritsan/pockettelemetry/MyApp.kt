package com.dmgritsan.pockettelemetry

import android.app.Application
import timber.log.Timber
import java.io.File

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val logFile = File(getExternalFilesDir(null), "app_log.txt")
        Timber.plant(FileLoggingTree(logFile))
    }
}