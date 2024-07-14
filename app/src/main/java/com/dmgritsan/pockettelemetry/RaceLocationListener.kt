package com.dmgritsan.pockettelemetry
import android.location.Location
import android.location.LocationListener
import android.os.Bundle

class RaceLocationListener(private val buffer: MutableList<SensorData>) : LocationListener {
    override fun onLocationChanged(location: Location) {
        buffer.add(
            SensorData.GPSData(
                timestamp = System.currentTimeMillis(),
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                speed = location.speed
            )
        )
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
