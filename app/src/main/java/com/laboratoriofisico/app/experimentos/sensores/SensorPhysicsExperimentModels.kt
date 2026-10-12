package com.laboratoriofisico.app.experimentos.sensores

import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.util.SensorValueExtractors
import kotlin.math.abs
import kotlin.math.sqrt

private const val GRAVITY_MS2 = 9.80665

enum class SensorPhysicsExperimentType(
    val title: String,
    val description: String
) {
    OBJECT_ACCELERATION(
        title = "Medir la aceleración de un objeto",
        description = "Aceleración medida con el acelerómetro real del teléfono."
    ),
    FREE_FALL(
        title = "Estudiar una caída",
        description = "Movimiento de caída registrado con el acelerómetro real del teléfono."
    ),
    PENDULUM(
        title = "Analizar un péndulo",
        description = "Oscilación registrada con acelerómetro real del teléfono."
    )
}

data class MotionSensorSample(
    val reading: SensorReading,
    val timestampMs: Long,
    val ax: Double,
    val ay: Double,
    val az: Double,
    val magnitude: Double,
    val dynamicMagnitude: Double
) {
    companion object {
        fun fromReading(reading: SensorReading): MotionSensorSample? {
            val ax = reading.valueX ?: return null
            val ay = reading.valueY ?: return null
            val az = reading.valueZ ?: return null
            val magnitude = sqrt(ax * ax + ay * ay + az * az)
            return MotionSensorSample(
                reading = reading,
                timestampMs = SensorValueExtractors.motionEventTimestampMs(reading) ?: reading.timestamp,
                ax = ax,
                ay = ay,
                az = az,
                magnitude = magnitude,
                dynamicMagnitude = abs(magnitude - GRAVITY_MS2)
            )
        }
    }
}

data class ProcessedSensorExperiment(
    val type: SensorPhysicsExperimentType,
    val samples: List<MotionSensorSample>,
    val primaryValue: Double,
    val primaryUnit: String,
    val primaryName: String,
    val summary: String,
    val numericResults: Map<String, Double>,
    val textResults: Map<String, String>
)

object SensorPhysicsExperimentProcessor {
    fun process(
        type: SensorPhysicsExperimentType,
        samples: List<MotionSensorSample>,
        pendulumLengthM: Double? = null
    ): ProcessedSensorExperiment? {
        if (samples.isEmpty()) return null
        val sorted = samples.sortedBy { it.timestampMs }
        return when (type) {
            SensorPhysicsExperimentType.OBJECT_ACCELERATION -> processAcceleration(sorted)
            SensorPhysicsExperimentType.FREE_FALL -> processFreeFall(sorted)
            SensorPhysicsExperimentType.PENDULUM -> processPendulum(sorted, pendulumLengthM)
        }
    }

    private fun processAcceleration(samples: List<MotionSensorSample>): ProcessedSensorExperiment {
        val duration = durationSeconds(samples)
        val maxTotal = samples.maxOf { it.magnitude }
        val avgTotal = samples.map { it.magnitude }.average()
        val maxDynamic = samples.maxOf { it.dynamicMagnitude }
        val avgDynamic = samples.map { it.dynamicMagnitude }.average()
        val summary = "Aceleración máxima %.2f m/s². Promedio %.2f m/s² durante %.2f s.".format(
            maxDynamic,
            avgDynamic,
            duration
        )
        return ProcessedSensorExperiment(
            type = SensorPhysicsExperimentType.OBJECT_ACCELERATION,
            samples = samples,
            primaryValue = maxDynamic,
            primaryUnit = "m/s²",
            primaryName = "Aceleración máxima del objeto",
            summary = summary,
            numericResults = mapOf(
                "duracion_s" to duration,
                "muestras" to samples.size.toDouble(),
                "aceleracion_total_max_ms2" to maxTotal,
                "aceleracion_total_promedio_ms2" to avgTotal,
                "aceleracion_dinamica_max_ms2" to maxDynamic,
                "aceleracion_dinamica_promedio_ms2" to avgDynamic
            ),
            textResults = mapOf(
                "lectura_usuario" to summary,
                "sensor" to "Acelerómetro del teléfono"
            )
        )
    }

    private fun processFreeFall(samples: List<MotionSensorSample>): ProcessedSensorExperiment {
        val duration = durationSeconds(samples)
        val freeFallSamples = samples.filter { it.magnitude <= FREE_FALL_THRESHOLD_MS2 }
        val freeFallDuration = durationSeconds(freeFallSamples)
        val minMagnitude = samples.minOf { it.magnitude }
        val impactMagnitude = samples.maxOf { it.magnitude }
        val detected = freeFallSamples.size >= MIN_FREE_FALL_SAMPLES
        val summary = if (detected) {
            "Se detectó una posible caída libre de %.2f s y un impacto máximo de %.2f m/s².".format(
                freeFallDuration,
                impactMagnitude
            )
        } else {
            "No se detectó una caída libre clara. La aceleración mínima fue %.2f m/s².".format(minMagnitude)
        }
        return ProcessedSensorExperiment(
            type = SensorPhysicsExperimentType.FREE_FALL,
            samples = samples,
            primaryValue = if (detected) freeFallDuration else minMagnitude,
            primaryUnit = if (detected) "s" else "m/s²",
            primaryName = if (detected) "Duración de caída detectada" else "Aceleración mínima registrada",
            summary = summary,
            numericResults = mapOf(
                "duracion_s" to duration,
                "muestras" to samples.size.toDouble(),
                "caida_detectada" to if (detected) 1.0 else 0.0,
                "duracion_caida_s" to freeFallDuration,
                "aceleracion_min_ms2" to minMagnitude,
                "impacto_max_ms2" to impactMagnitude
            ),
            textResults = mapOf(
                "lectura_usuario" to summary,
                "sensor" to "Acelerómetro del teléfono"
            )
        )
    }

    private fun processPendulum(
        samples: List<MotionSensorSample>,
        pendulumLengthM: Double?
    ): ProcessedSensorExperiment {
        val analyzer = com.laboratoriofisico.app.sensores.util.PendulumAnalyzer()
        var state = com.laboratoriofisico.app.sensores.util.PendulumState()
        samples.forEach { sample ->
            state = analyzer.update(sample.timestampMs, sample.ax, sample.ay, sample.az)
        }
        val period = state.periodSeconds ?: 0.0
        val frequency = state.frequencyHz ?: 0.0
        val duration = durationSeconds(samples)
        val gravity = if (pendulumLengthM != null && period > 0.0) {
            4.0 * Math.PI * Math.PI * pendulumLengthM / (period * period)
        } else {
            null
        }
        val summary = if (period > 0.0) {
            "Período %.2f s, frecuencia %.2f Hz y %d oscilaciones detectadas.".format(
                period,
                frequency,
                state.oscillations
            )
        } else {
            "Se registraron %.2f s, pero aún no hubo suficientes oscilaciones claras.".format(duration)
        }
        return ProcessedSensorExperiment(
            type = SensorPhysicsExperimentType.PENDULUM,
            samples = samples,
            primaryValue = period.takeIf { it > 0.0 } ?: duration,
            primaryUnit = if (period > 0.0) "s" else "s",
            primaryName = if (period > 0.0) "Período del péndulo" else "Tiempo registrado",
            summary = summary,
            numericResults = buildMap {
                put("duracion_s", duration)
                put("muestras", samples.size.toDouble())
                put("periodo_s", period)
                put("frecuencia_hz", frequency)
                put("oscilaciones", state.oscillations.toDouble())
                state.amplitudeMs2?.let { put("amplitud_ms2", it) }
                pendulumLengthM?.let { put("longitud_m", it) }
                gravity?.let { put("gravedad_calculada_ms2", it) }
            },
            textResults = mapOf(
                "lectura_usuario" to summary,
                "sensor" to "Acelerómetro del teléfono",
                "eje_dominante" to (state.dominantAxis?.name ?: "no detectado")
            )
        )
    }

    private fun durationSeconds(samples: List<MotionSensorSample>): Double {
        if (samples.size < 2) return 0.0
        return ((samples.last().timestampMs - samples.first().timestampMs).coerceAtLeast(0L)) / 1000.0
    }

    private const val FREE_FALL_THRESHOLD_MS2 = 3.0
    private const val MIN_FREE_FALL_SAMPLES = 3
}
