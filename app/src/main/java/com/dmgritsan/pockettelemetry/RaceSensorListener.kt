package com.dmgritsan.pockettelemetry
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener

class RaceSensorListener(private val dataBuffer: MutableList<SensorData>) : SensorEventListener {
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> dataBuffer.add(SensorData.AccelerometerData(System.currentTimeMillis(), event.values[0], event.values[1], event.values[2]))
            Sensor.TYPE_GYROSCOPE -> dataBuffer.add(SensorData.GyroscopeData(System.currentTimeMillis(), event.values[0], event.values[1], event.values[2]))
            Sensor.TYPE_MAGNETIC_FIELD -> dataBuffer.add(SensorData.MagneticData(System.currentTimeMillis(), event.values[0], event.values[1], event.values[2]))
            Sensor.TYPE_PRESSURE -> dataBuffer.add(SensorData.PressureData(System.currentTimeMillis(), event.values[0]))
            // Add cases for other sensors
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}


}


