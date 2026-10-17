package com.peostectica.hereshift

/** Coordinates represent a user-chosen synthetic or manually specified test point.
 * No sensor-based actual-location data is read or transmitted. */
data class TestPoint(val latitude: Double, val longitude: Double)
object ScenarioRules {
    fun parse(latitudeText: String, longitudeText: String): TestPoint? {
        val lat = latitudeText.trim().toDoubleOrNull() ?: return null
        val lon = longitudeText.trim().toDoubleOrNull() ?: return null
        if (!lat.isFinite() || !lon.isFinite()) return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return TestPoint(lat, lon)
    }
}