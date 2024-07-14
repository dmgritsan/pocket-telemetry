package com.dmgritsan.pockettelemetry

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager

class DataCollector(context: Context) {
    companion object {
        const val SENSOR_UPDATE_FREQUENCY_MICROS = 1000 // 1 milliseconds in microseconds
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val dataBuffer = mutableMapOf<String, MutableList<SensorData>>()

    @SuppressLint("MissingPermission")
    fun startCollection() {

        dataBuffer["Accelerometer"] = mutableListOf()
        dataBuffer["Pressure"] = mutableListOf()
        dataBuffer["Gyroscope"] = mutableListOf()
        dataBuffer["Magnetic"] = mutableListOf()

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        sensorManager.registerListener(RaceSensorListener(dataBuffer["Accelerometer"]!!), accelerometer, SENSOR_UPDATE_FREQUENCY_MICROS)
        sensorManager.registerListener(RaceSensorListener(dataBuffer["Pressure"]!!), pressureSensor, SENSOR_UPDATE_FREQUENCY_MICROS)
        sensorManager.registerListener(RaceSensorListener(dataBuffer["Gyroscope"]!!), gyroscope, SENSOR_UPDATE_FREQUENCY_MICROS)
        sensorManager.registerListener(RaceSensorListener(dataBuffer["Magnetic"]!!), magnetic, SENSOR_UPDATE_FREQUENCY_MICROS)

        dataBuffer["GPS"] = mutableListOf()
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, RaceLocationListener(dataBuffer["GPS"]!!))
    }

    fun stopCollection(): List<Pair<String, List<SensorData>>> {
        sensorManager.unregisterListener(RaceSensorListener(mutableListOf()))
        locationManager.removeUpdates(RaceLocationListener(mutableListOf()))
        return dataBuffer.map { Pair(it.key, it.value.toList()) }
    }

    fun getDataBetween(startTimestamp: Long, endTimestamp: Long): List<Pair<String, List<SensorData>>> {
        return dataBuffer.map { buf ->
            Pair(buf.key, buf.value.filter {it.timestamp in startTimestamp..endTimestamp })
        }
    }

}
