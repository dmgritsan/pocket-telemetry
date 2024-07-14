package com.dmgritsan.pockettelemetry

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.io.File

class DataSaver(private val context: Context) {
    private fun saveRaceDataToFile(dataList: List<Pair<String, List<SensorData>>>, fileName: String) {
        val file = File(context.getExternalFilesDir(null), fileName)
        file.printWriter().use { out ->
            dataList.forEach { (sensorType, data) ->
                out.println("Data Source: $sensorType")
                data.forEach { sensorData ->
                    out.println(sensorData)  // This assumes that the SensorData classes have a meaningful toString() implementation
                }
                out.println() // Adds a blank line for better readability between sensor data sections
            }
        }
    }

    fun loadRaceSummaries() : MutableList<RaceSummary> {
        val sharedPreferences = context.getSharedPreferences("RaceSummaries",
            AppCompatActivity.MODE_PRIVATE
        )
        val gson = Gson()
        val json = sharedPreferences.getString("races", null)
        val type = object : TypeToken<MutableList<RaceSummary>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }


    fun saveRaceSummaries(raceSummaries: MutableList<RaceSummary>) {
        val sharedPreferences = context.getSharedPreferences("RaceSummaries",
            AppCompatActivity.MODE_PRIVATE
        )
        val editor = sharedPreferences.edit()
        val gson = Gson()  // Use Google's Gson to convert list to JSON
        val json = gson.toJson(raceSummaries)
        editor.putString("races", json)
        editor.apply()
    }

    fun saveNewRace(dataList: List<Pair<String, List<SensorData>>>, fileName: String): RaceSummary {
        saveRaceDataToFile(dataList, fileName)
        val raceSummaries = loadRaceSummaries()
        var dataEntriesCnt = 0
        dataList.forEach { (sensorType, data) ->
            dataEntriesCnt += data.size
        }
        val raceSummary = RaceSummary(fileName, dataEntriesCnt)
        raceSummaries.add(raceSummary)
        saveRaceSummaries(raceSummaries)
        return raceSummary
    }

    fun saveTmpRace(dataList: List<Pair<String, List<SensorData>>>, fileName: String) {
        saveRaceDataToFile(dataList, fileName)
    }
}