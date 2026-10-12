package com.laboratoriofisico.app.sensores.util

import com.laboratoriofisico.app.sensores.model.SensorReading

enum class LightLevel {
    LOW,
    MEDIUM,
    HIGH
}

/** Unidad real reportada por el hardware; determina qué umbrales aplican. */
enum class LightMeasurementUnit {
    LUX,
    RAW
}

/**
 * Umbrales centralizados de clasificación. Modificar aquí tras calibrar en dispositivo.
 *
 * Los valores `RAW_*` corresponden al Camera Light Sensor de Samsung (tipo 65604), cuya
 * escala no está calibrada en lux; representan intensidad relativa, no unidades fotométricas.
 */
object LightThresholds {
    /** lux: por debajo de este valor → [LightLevel.LOW]. */
    const val LUX_LOW_MAX = 100.0

    /** lux: por debajo de este valor → [LightLevel.MEDIUM]; igual o mayor → [LightLevel.HIGH]. */
    const val LUX_MEDIUM_MAX = 500.0

    /** lux: tope de la escala visual de la barra. */
    const val LUX_SCALE_MAX = 1_000.0

    /** raw: tope visual de la barra; debe coincidir con maximumRange del Camera Light Sensor Samsung (~60000). */
    const val RAW_SCALE_MAX = 60_000.0

    /** raw Samsung SM-G780G: oscuro ~500–3000, habitación ~5000–15000, lámpara cerca >20000. */
    const val RAW_LOW_MAX = 3_000.0

    /** raw: por debajo de este valor → [LightLevel.MEDIUM]; igual o mayor → [LightLevel.HIGH]. */
    const val RAW_MEDIUM_MAX = 15_000.0

    fun lowMax(unit: LightMeasurementUnit): Double = when (unit) {
        LightMeasurementUnit.LUX -> LUX_LOW_MAX
        LightMeasurementUnit.RAW -> RAW_LOW_MAX
    }

    fun mediumMax(unit: LightMeasurementUnit): Double = when (unit) {
        LightMeasurementUnit.LUX -> LUX_MEDIUM_MAX
        LightMeasurementUnit.RAW -> RAW_MEDIUM_MAX
    }

    fun scaleMax(unit: LightMeasurementUnit): Double = when (unit) {
        LightMeasurementUnit.LUX -> LUX_SCALE_MAX
        LightMeasurementUnit.RAW -> RAW_SCALE_MAX
    }
}

object LightClassifier {

    fun unitFromString(unit: String): LightMeasurementUnit =
        if (unit.equals("raw", ignoreCase = true) || unit.equals("crudo", ignoreCase = true)) {
            LightMeasurementUnit.RAW
        } else {
            LightMeasurementUnit.LUX
        }

    fun unitFromReading(reading: SensorReading): LightMeasurementUnit =
        unitFromString(reading.unit)

    fun classifyLight(value: Double, unit: LightMeasurementUnit): LightLevel = when {
        value < LightThresholds.lowMax(unit) -> LightLevel.LOW
        value < LightThresholds.mediumMax(unit) -> LightLevel.MEDIUM
        else -> LightLevel.HIGH
    }

    fun classifyFromReading(reading: SensorReading): LightLevel? {
        val value = SensorValueExtractors.lightPrimaryValue(reading) ?: return null
        return classifyLight(value, unitFromReading(reading))
    }

    /**
     * Normaliza la lectura real a la posición visual de la barra (0..1).
     *
     * Usa escala lineal respecto al máximo del hardware cuando está disponible, de modo que
     * la barra se mueva en todo el rango real del sensor (p. ej. 0..60000 en Samsung raw).
     * La clasificación BAJA/MEDIA/ALTA sigue usando [LightThresholds] por separado.
     */
    fun lightProgress(value: Double, unit: LightMeasurementUnit, sensorMaxRange: Double? = null): Float {
        if (value.isNaN() || value < 0.0) return 0f
        val scaleMax = when {
            sensorMaxRange != null && sensorMaxRange > 0.0 -> sensorMaxRange
            unit == LightMeasurementUnit.RAW -> LightThresholds.RAW_SCALE_MAX
            else -> LightThresholds.LUX_SCALE_MAX
        }
        return (value / scaleMax).coerceIn(0.0, 1.0).toFloat()
    }

    fun lightProgressFromReading(reading: SensorReading): Float? {
        val value = SensorValueExtractors.lightPrimaryValue(reading) ?: return null
        return lightProgress(
            value,
            unitFromReading(reading),
            SensorValueExtractors.lightSensorMaxRange(reading)
        )
    }

    fun levelDisplayName(level: LightLevel): String = when (level) {
        LightLevel.LOW -> "BAJA"
        LightLevel.MEDIUM -> "MEDIA"
        LightLevel.HIGH -> "ALTA"
    }

    fun levelDescription(level: LightLevel): String = when (level) {
        LightLevel.LOW -> "Poca luz — zona verde"
        LightLevel.MEDIUM -> "Luz considerable — zona amarilla"
        LightLevel.HIGH -> "Demasiada luz — zona roja"
    }

    fun safetyDisclaimer(): String =
        "La lectura representa intensidad de iluminación ambiental. " +
            "No determina por sí sola riesgo ocular."
}
