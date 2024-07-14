package com.dmgritsan.pockettelemetry
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentCallbacks2
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import timber.log.Timber
import java.util.Locale

class MyDataCollectionService : Service() {
    companion object {
        const val ACTION_SERVICE_STOPPED = "com.dmgritsan.pockettelemetry.SERVICE_STOPPED"
    }

    private val NOTIFICATION_CHANNEL_ID = "foreground_service_channel"
    private val NOTIFICATION_ID = 1

    private val dumpTimeSec = 30
    private var startTimeMillis: Long = 0
    private var tmpFiles: MutableList<String> = mutableListOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var dataCollector: DataCollector
    private lateinit var dataSaver: DataSaver

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateNotification()
            handler.postDelayed(this, 1000) // Update notification every second
        }
    }

    override fun onCreate() {
        Timber.d("onCreate")
        super.onCreate()
        createNotificationChannel()
        handler.post(updateRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand")
        intent?.let {
            return handleCommand(it)
        }
        return START_STICKY
//        return START_REDELIVER_INTENT
    }

    private fun handleCommand(intent: Intent) : Int {
        if (intent.action == "STOP_SERVICE") {
            Timber.d("Handling STOP_SERVICE")
            stopSelf()
            return 0
        }
        else{
            Timber.d("Handling command")
            Timber.d(intent.action)
            startTimeMillis = System.currentTimeMillis()
            startForeground(NOTIFICATION_ID, buildNotification("Service is running..."))
            dataCollector = DataCollector(this)
            dataCollector.startCollection()
            return START_STICKY
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Foreground Service Channel",
                IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopSelfIntent = Intent(this, MyDataCollectionService::class.java).apply {
            action = "STOP_SERVICE"
        }
        val stopSelfPendingIntent = PendingIntent.getService(
            this,
            1, stopSelfIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val elapsedTime = System.currentTimeMillis() - startTimeMillis
        val hrs = elapsedTime / 3600000
        val mins = (elapsedTime / 60000) % 60
        val secs = (elapsedTime / 1000) % 60
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)

        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Sensor Service Running")
            .setContentText("$text - Running for $formattedTime")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopSelfPendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        Timber.d("onBind")
        return null
    }

    override fun onDestroy() {
        Timber.d("service onDestroy")
        super.onDestroy()
        var raceSummary = saveData()
        var intent = Intent(ACTION_SERVICE_STOPPED)
        intent.putExtra("RaceSummaryFile", raceSummary.fileName)
        intent.putExtra("RaceSummaryDataCnt", raceSummary.dataCount)
        sendBroadcast(intent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Timber.d("service onTaskRemoved")
        super.onTaskRemoved(rootIntent)
        // Code to save data or clean up resources
        // Как будто бы тут не надо этого делать, потому что потом вызовется onDestroy
        // Зато тут можно попробовать перезапустить тасочку. Хотя, может, и в onDestroy можно
        Timber.d(rootIntent.toString())
        if (rootIntent != null) {
            Timber.d(rootIntent.action)
        }
//        saveData()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            Timber.d("onTrimMemory saveData")
            saveData()  // Save data when the system is running low on memory
        }
    }

    private fun saveData(): RaceSummary {
        Timber.d("saveData")
        val dataCollected = dataCollector.stopCollection()
        val dataSaver = DataSaver(this)
//        val prefix = if (testMode) "fake_race_" else "race_"
        val prefix = "race_"
        val currentFileName = "$prefix$startTimeMillis.txt"
        return dataSaver.saveNewRace(dataCollected, currentFileName)
    }


    private fun updateNotification() {
        startForeground(1, buildNotification("Race is running for "))
        val elapsedTime = System.currentTimeMillis() - startTimeMillis
//        Timber.d("Elapsed time $elapsedTime")
        if ( (elapsedTime / 1000 > 1) && ((elapsedTime / 1000) % dumpTimeSec).toInt() == 0){
            Timber.d("Elapsed time $elapsedTime")
            dumpData()
        }

    }

    private fun dumpData(){
        val dataSaver = DataSaver(this)
        val prefix = "tmp_race_"
        val postfix = "_${tmpFiles.count()}"
        val dumpTimeMillis = startTimeMillis + dumpTimeSec * tmpFiles.count() * 1000
        val dataCollected = dataCollector.getDataBetween(dumpTimeMillis, dumpTimeMillis + dumpTimeSec * 1000)
        val currentFileName = "$prefix$startTimeMillis$postfix.txt"
        dataSaver.saveTmpRace(dataCollected, currentFileName)
        tmpFiles.add(currentFileName)
    }
}
