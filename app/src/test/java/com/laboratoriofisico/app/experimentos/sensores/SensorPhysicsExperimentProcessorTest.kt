package com.laboratoriofisico.app.experimentos.sensores

import com.laboratoriofisico.app.sensores.model.AccelerometerReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class SensorPhysicsExperimentProcessorTest {
    @Test
    fun accelerationUsesRealAxisValuesFromReadings() {
        val samples = listOf(
            sample(timestampMs = 1_000L, x = 0.0, y = 0.0, z = 9.81),
            sample(timestampMs = 1_100L, x = 3.0, y = 0.0, z = 9.81),
            sample(timestampMs = 1_200L, x = 5.0, y = 0.0, z = 9.81)
        )

        val result = SensorPhysicsExperimentProcessor.process(
            type = SensorPhysicsExperimentType.OBJECT_ACCELERATION,
            samples = samples
        )

        assertNotNull(result)
        assertEquals(3, result!!.numericResults["muestras"]!!.toInt())
        assertTrue(result.numericResults["aceleracion_dinamica_max_ms2"]!! > 1.0)
        assertEquals(result.numericResults["aceleracion_dinamica_max_ms2"]!!, result.primaryValue, 1e-9)
    }

    @Test
    fun freeFallDetectsLowAccelerationWindowAndImpact() {
        val samples = listOf(
            sample(1_000L, 0.0, 0.0, 9.81),
            sample(1_100L, 0.0, 0.0, 0.6),
            sample(1_200L, 0.0, 0.0, 0.5),
            sample(1_300L, 0.0, 0.0, 0.8),
            sample(1_400L, 0.0, 0.0, 22.0)
        )

        val result = SensorPhysicsExperimentProcessor.process(
            type = SensorPhysicsExperimentType.FREE_FALL,
            samples = samples
        )

        assertNotNull(result)
        assertEquals(1.0, result!!.numericResults["caida_detectada"]!!, 0.0)
        assertEquals(22.0, result.numericResults["impacto_max_ms2"]!!, 0.001)
        assertEquals("s", result.primaryUnit)
    }

    @Test
    fun pendulumReportsPeriodAndFrequencyFromSamples() {
        val periodSec = 1.4
        val samples = buildList {
            val sampleRateHz = 50.0
            val totalSamples = (20.0 * sampleRateHz).toInt()
            for (i in 0 until totalSamples) {
                val tSec = i / sampleRateHz
                val timestampMs = 1_000L + (i * 20L)
                val y = 2.0 * sin(2.0 * PI * tSec / periodSec)
                add(sample(timestampMs, x = 0.1, y = y, z = 9.81))
            }
        }

        val result = SensorPhysicsExperimentProcessor.process(
            type = SensorPhysicsExperimentType.PENDULUM,
            samples = samples
        )

        assertNotNull(result)
        assertEquals(periodSec, result!!.numericResults["periodo_s"]!!, 0.2)
        assertEquals(1.0 / result.numericResults["periodo_s"]!!, result.numericResults["frecuencia_hz"]!!, 0.01)
        assertTrue(result.numericResults["oscilaciones"]!! >= 5.0)
    }

    private fun sample(timestampMs: Long, x: Double, y: Double, z: Double): MotionSensorSample {
        val eventTsNs = timestampMs * 1_000_000L
        val reading = AccelerometerReading(
            x = x,
            y = y,
            z = z,
            timestamp = timestampMs + 7L,
            eventTimestampNanos = eventTsNs,
            samplePeriodUs = 20_000
        ).toSensorReading()
        return MotionSensorSample.fromReading(reading)!!
    }
}
