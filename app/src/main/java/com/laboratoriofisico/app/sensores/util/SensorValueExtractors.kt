package com.laboratoriofisico.app.sensores.util

import com.laboratoriofisico.app.sensores.model.SensorReading
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

object SensorValueExtractors {
    fun lux(reading: SensorReading): Double? =
        if (reading.unit == "crudo" || reading.unit == "raw") null else reading.valueScalar

    fun rawLight(reading: SensorReading): Double? = reading.valueScalar

    /** Valor de luz desde hardware: lux calibrado o raw según [SensorReading.unit]. */
    fun lightPrimaryValue(reading: SensorReading): Double? =
        lux(reading) ?: rawLight(reading)

    /**
     * Instante del evento en milisegundos según el reloj monótono del hardware.
     * Devuelve `null` si el proveedor no adjuntó el timestamp del sensor.
     */
    fun motionEventTimestampMs(reading: SensorReading): Long? =
        reading.metadata
            ?.split(';')
            ?.firstOrNull { it.startsWith("event_ts_ns=") }
            ?.substringAfter('=')
            ?.toLongOrNull()
            ?.div(1_000_000L)

    fun lightSensorMaxRange(reading: SensorReading): Double? =
        reading.metadata
            ?.split(';')
            ?.firstOrNull { it.startsWith("sensor_max_range=") }
            ?.substringAfter('=')
            ?.toDoubleOrNull()

    fun isExperimentalLight(reading: SensorReading): Boolean =
        reading.unit == "crudo" || reading.unit == "raw" ||
            reading.metadata?.contains("experimental_als=true") == true

    fun amplitudeToRelativeDb(amplitude: Double): Double {
        val safe = amplitude.coerceAtLeast(1e-8)
        return 20.0 * kotlin.math.log10(safe)
    }

    /**
     * Nivel 0–100 a partir del RMS real del micrófono.
     * Silencio queda cerca de 0 y el máximo del hardware queda en 100.
     */
    fun microphoneLevelPercent(amplitude: Double): Double {
        val db = amplitudeToRelativeDb(amplitude.coerceAtMost(1.0))
        return ((db + 60.0) / 60.0 * 100.0).coerceIn(0.0, 100.0)
    }

    fun dbRelativeFromReading(reading: SensorReading): Double? =
        reading.valueScalar?.let(::amplitudeToRelativeDb)

    /** @deprecated Usar [dbRelativeFromReading]; no es dB SPL calibrado. */
    @Deprecated("Usar dbRelativeFromReading — no representa dB SPL absolutos")
    fun dbFromReading(reading: SensorReading): Double? = dbRelativeFromReading(reading)

    fun magnetMagnitude(reading: SensorReading): Double? {
        val x = reading.valueX ?: return null
        val y = reading.valueY ?: return null
        val z = reading.valueZ ?: return null
        return sqrt(x * x + y * y + z * z)
    }

    fun magnetMagnitude(x: Double, y: Double, z: Double): Double =
        sqrt(x * x + y * y + z * z)

    fun accelMagnitude(reading: SensorReading): Double? {
        val x = reading.valueX ?: return null
        val y = reading.valueY ?: return null
        val z = reading.valueZ ?: return null
        return sqrt(x * x + y * y + z * z)
    }

    fun accelDynamicMagnitude(reading: SensorReading): Double? {
        val magnitude = accelMagnitude(reading) ?: return null
        return (magnitude - GRAVITY_MS2).coerceAtLeast(0.0)
    }

    /** Pitch y roll en grados a partir del vector de aceleración (incl. gravedad). */
    fun tiltDegrees(reading: SensorReading): Pair<Double, Double>? {
        val x = reading.valueX ?: return null
        val y = reading.valueY ?: return null
        val z = reading.valueZ ?: return null
        return tiltDegrees(x, y, z)
    }

    fun tiltDegrees(x: Double, y: Double, z: Double): Pair<Double, Double> {
        val pitch = Math.toDegrees(atan2(-x, sqrt(y * y + z * z)))
        val roll = Math.toDegrees(atan2(y, z))
        return pitch to roll
    }

    fun gyroMagnitude(reading: SensorReading): Double? {
        val x = reading.valueX ?: return null
        val y = reading.valueY ?: return null
        val z = reading.valueZ ?: return null
        return sqrt(x * x + y * y + z * z)
    }

    fun radiansToDegreesPerSecond(radPerSec: Double): Double = radPerSec * 180.0 / PI

    fun isMoving(accelDynamic: Double, threshold: Double = 0.5): Boolean =
        accelDynamic > threshold

    fun haversineMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun clasificarIluminacion(lux: Double?): String? = when {
        lux == null -> null
        lux < 100 -> "Baja"
        lux <= 500 -> "Moderada"
        else -> "Alta"
    }

    fun clasificarSonidoRelativeDb(db: Double?): String? = when {
        db == null -> null
        db < -50 -> "Bajo"
        db <= -30 -> "Moderado"
        else -> "Alto"
    }

    fun formatAxis(value: Double?, unit: String): String =
        value?.let { "%.2f %s".format(it, unit) } ?: "N/A"

    private const val GRAVITY_MS2 = 9.81
}
