package com.dmgritsan.pockettelemetry

import android.Manifest
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File


class MainActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var logTextView: TextView
    private lateinit var testModeSwitch: Switch
    private var isRecording = false

    private lateinit var dataSaver: DataSaver

    private var raceSummaries = mutableListOf<RaceSummary>()
    private lateinit var raceListAdapter: RaceListAdapter

    companion object {
        private const val PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = getSharedPreferences("ServicePreferences", Context.MODE_PRIVATE)
        isRecording = sharedPreferences.getBoolean("serviceRunning", false)
        super.onCreate(savedInstanceState)

        Log.d("com.dmgritsan.pockettelemetry", "initializing")
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.raceListRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)

        dataSaver = DataSaver(this)
        raceSummaries = dataSaver.loadRaceSummaries()
//        loadRaceSummaries()

        raceListAdapter = RaceListAdapter(raceSummaries)
        sortRaceSummaries()
        recyclerView.adapter = raceListAdapter
        registerForContextMenu(recyclerView)

        checkAndRequestPermissions()

        testModeSwitch = findViewById(R.id.switch_test_mode)
        logTextView = findViewById(R.id.logTextView)

        testModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                logTextView.visibility = View.VISIBLE
            } else {
                logTextView.visibility = View.GONE
            }
        }

        collectionServiceInit()
    }


    private fun checkAndRequestPermissions() {
        val requiredPermissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BODY_SENSORS
//            ,
//            Manifest.permission.HIGH_SAMPLING_RATE_SENSORS
        )

        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun sortRaceSummaries() {
        raceSummaries.sortByDescending {
            it.fileName.substringAfter("race_").substringBefore(".txt").toLongOrNull() ?: 0L
        }
        runOnUiThread {
            raceListAdapter.notifyDataSetChanged()
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> openFile(raceListAdapter.currentPosition)
            2 -> sendFile(raceListAdapter.currentPosition)
            3 -> deleteFile(raceListAdapter.currentPosition)
        }
        return super.onContextItemSelected(item)
    }

    fun openFile(position: Int) {
        val fileUri = FileProvider.getUriForFile(this, "com.dmgritsan.pockettelemetry.provider",
            File(applicationContext.getExternalFilesDir(null),raceSummaries[position].fileName))
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(fileUri, "text/plain") // Change MIME type based on your file type
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Open with"))
    }

    fun sendFile(position: Int) {
        val fileUri = FileProvider.getUriForFile(this, "com.dmgritsan.pockettelemetry.provider",
            File(applicationContext.getExternalFilesDir(null), raceSummaries[position].fileName))
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain" // Change MIME type based on your file type
        intent.putExtra(Intent.EXTRA_STREAM, fileUri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Send using"))
    }

    fun deleteFile(position: Int) {
        val filePath = raceSummaries[position].fileName
        val file = File(applicationContext.getExternalFilesDir(null), filePath)
        Log.d("DeleteFile", "Attempting to delete file at path: $filePath")
        if (file.exists()) {
            if (file.delete()) {
                println("File deleted successfully")
            } else {
                println("Failed to delete the file")
            }
        } else {
            println("File does not exist")
        }
        // Remove from list
        raceSummaries.removeAt(position)
        raceListAdapter.notifyItemRemoved(position)
        raceListAdapter.notifyItemRangeChanged(position, raceSummaries.size)

        // Save the updated list to SharedPreferences
        dataSaver.saveRaceSummaries(raceSummaries)
//        saveRaceSummaries()
    }

    fun appendLog(message: String) {
        runOnUiThread {
            logTextView.append("\n$message")
            // Scroll to the bottom whenever a new message is added
            val scrollAmount = logTextView.layout.getLineTop(logTextView.lineCount) - logTextView.height
            if (scrollAmount > 0)
                logTextView.scrollTo(0, scrollAmount)
            else
                logTextView.scrollTo(0, 0)
        }
    }

    private lateinit var toggleServiceButton: Button
    private lateinit var collectionServiceStateReceiver: BroadcastReceiver

    private fun collectionServiceInit() {
        sharedPreferences = getSharedPreferences("ServicePrefs", MODE_PRIVATE)

        toggleServiceButton = findViewById(R.id.toggleServiceButton)
        setupCollectionBroadcastReceiver()
        updateButtonState()

        toggleServiceButton.setOnClickListener {
            if (isServiceRunning()) {
                stopCollectionService()
            } else {
                startCollectionService()
            }
            updateButtonState()
        }
    }

    private fun startCollectionService() {
        val startIntent = Intent(this, MyDataCollectionService::class.java)
        startForegroundService(startIntent)
//        sharedPreferences.edit().putBoolean("ServiceRunning", true).apply()
    }

    private fun stopCollectionService() {
        val stopIntent = Intent(this, MyDataCollectionService::class.java)
        stopService(stopIntent)
//        sharedPreferences.edit().putBoolean("ServiceRunning", false).apply()
    }

    private fun setupCollectionBroadcastReceiver() {
        collectionServiceStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == MyDataCollectionService.ACTION_SERVICE_STOPPED) {
                    updateButtonState()
//                    loadRaceSummaries()
//                    sortRaceSummaries()
                    val fileName = intent.getStringExtra("RaceSummaryFile")
                    val dataCount = intent.getIntExtra("RaceSummaryDataCnt", 0)
                    raceSummaries.add(RaceSummary(fileName ?: "not found", dataCount))
                    sortRaceSummaries()
//                    runOnUiThread {
//                        raceListAdapter.notifyDataSetChanged()
//                    }
                }
            }
        }
        IntentFilter(MyDataCollectionService.ACTION_SERVICE_STOPPED).also {
            registerReceiver(collectionServiceStateReceiver, it, RECEIVER_NOT_EXPORTED)
        }
    }

    private fun isServiceRunning(): Boolean {
        val serviceClass = MyDataCollectionService::class.java
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun updateButtonState() {
        if (isServiceRunning()) {
            toggleServiceButton.text = "Stop Race"
        } else {
            toggleServiceButton.text = "Start Race"
        }
    }

}
