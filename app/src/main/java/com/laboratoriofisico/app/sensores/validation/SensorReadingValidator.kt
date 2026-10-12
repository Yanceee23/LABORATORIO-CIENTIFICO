package com.laboratoriofisico.app.sensores.validation

import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.model.SensorType

object SensorReadingValidator {
    fun isValid(reading: SensorReading): Boolean {
        if (reading.timestamp <= 0L) return false
        if (reading.unit.isBlank()) return false
        if (!isValidSensorType(reading.sensorType)) return false

        return when (reading.sensorType) {
            SensorType.ACCELEROMETER,
            SensorType.GYROSCOPE,
            SensorType.MAGNETOMETER -> {
                isFinite(reading.valueX) && isFinite(reading.valueY) && isFinite(reading.valueZ)
            }
            SensorType.LIGHT -> isFinite(reading.valueScalar) && reading.valueScalar!! >= 0
            SensorType.LOCATION -> {
                isFinite(reading.valueX) && isFinite(reading.valueY) &&
                    reading.valueX!! in -90.0..90.0 &&
                    reading.valueY!! in -180.0..180.0 &&
                    (reading.valueZ == null || isFinite(reading.valueZ)) &&
                    (reading.altitude == null || isFinite(reading.altitude)) &&
                    (reading.accuracy == null || reading.accuracy >= 0f) &&
                    (reading.speed == null || reading.speed >= 0f)
            }
            SensorType.MICROPHONE -> isFinite(reading.valueScalar) && reading.valueScalar!! >= 0
            SensorType.CAMERA -> reading.metadata?.contains("state=") == true
        }
    }

    private fun isValidSensorType(type: SensorType): Boolean =
        SensorType.entries.contains(type)

    private fun isFinite(value: Double?): Boolean =
        value != null && value.isFinite()
}
