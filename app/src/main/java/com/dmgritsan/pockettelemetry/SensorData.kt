package com.dmgritsan.pockettelemetry

sealed class SensorData {
    abstract val timestamp: Long

    data class AccelerometerData(override val timestamp: Long, val x: Float, val y: Float, val z: Float) : SensorData(){
        override fun toString() = "$timestamp, $x, $y, $z"
    }
    data class GyroscopeData(override val timestamp: Long, val x: Float, val y: Float, val z: Float) : SensorData(){
        override fun toString() = "$timestamp, $x, $y, $z"
    }
    data class MagneticData(override val timestamp: Long, val x: Float, val y: Float, val z: Float) : SensorData(){
        override fun toString() = "$timestamp, $x, $y, $z"
    }
    data class PressureData(override val timestamp: Long, val pressure: Float) : SensorData(){
        override fun toString() = "$timestamp, $pressure"
    }
    data class GPSData(override val timestamp: Long, val latitude: Double, val longitude: Double, val altitude: Double, val speed: Float) : SensorData(){
        override fun toString() = "$timestamp, $latitude, $longitude, $altitude, $speed"
    }
    // Add other sensor types as needed
}
