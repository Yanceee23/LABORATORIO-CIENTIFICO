package com.laboratoriofisico.app.sensores.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class PendulumAnalyzerTest {

    /** Simula la señal del acelerómetro alimentando al analizador; no forma parte de la app. */
    private fun feedOscillation(
        analyzer: PendulumAnalyzer,
        periodSec: Double,
        amplitude: Double,
        durationSec: Double,
        sampleRateHz: Double = 50.0
    ): PendulumState {
        val stepMs = (1000.0 / sampleRateHz).toLong()
        val samples = (durationSec * sampleRateHz).toInt()
        var state = PendulumState()
        for (i in 0 until samples) {
            val tSec = i / sampleRateHz
            val timestampMs = 1_000L + i * stepMs
            val value = amplitude * sin(2.0 * PI * tSec / periodSec)
            state = analyzer.update(timestampMs, x = 0.1, y = value, z = 9.81)
        }
        return state
    }

    @Test
    fun detectsPeriodOfKnownOscillation() {
        val analyzer = PendulumAnalyzer()
        val state = feedOscillation(analyzer, periodSec = 1.4, amplitude = 2.0, durationSec = 20.0)

        val period = state.periodSeconds
        assertNotNull("Debe detectar un periodo", period)
        assertEquals(1.4, period!!, 0.15)
        assertEquals(1.0 / period, state.frequencyHz!!, 0.01)
        assertTrue("Debe contar varias oscilaciones", state.oscillations >= 5)
        assertEquals(PendulumAxis.Y, state.dominantAxis)
    }

    @Test
    fun reportsNoPeriodWhenSignalIsFlat() {
        val analyzer = PendulumAnalyzer()
        var state = PendulumState()
        for (i in 0 until 500) {
            state = analyzer.update(1_000L + i * 20L, x = 0.0, y = 0.02, z = 9.81)
        }
        assertNull("Sin movimiento no debe reportar periodo", state.periodSeconds)
        assertEquals(0, state.oscillations)
        assertTrue("Debe indicar señal insuficiente", !state.hasEnoughSignal)
    }

    @Test
    fun periodAndOscillationCountAreConsistent() {
        val analyzer = PendulumAnalyzer()
        val durationSec = 18.0
        val state = feedOscillation(analyzer, periodSec = 2.0, amplitude = 1.5, durationSec = durationSec)

        val period = state.periodSeconds!!
        val expectedOscillations = (durationSec / period).toInt()
        assertTrue(
            "Oscilaciones (${state.oscillations}) incoherentes con periodo $period",
            state.oscillations in (expectedOscillations - 2)..(expectedOscillations + 1)
        )
    }

    @Test
    fun stopsReportingPeriodWhenOscillationStops() {
        val analyzer = PendulumAnalyzer()
        val state = feedOscillation(analyzer, periodSec = 1.2, amplitude = 2.0, durationSec = 15.0)
        assertNotNull("Precondición: debe haber periodo", state.periodSeconds)
        val oscillationsWhileMoving = state.oscillations

        var quiet = state
        val lastTimestamp = 1_000L + (15.0 * 50).toInt() * 20L
        for (i in 0 until 400) {
            quiet = analyzer.update(lastTimestamp + i * 20L, x = 0.1, y = 0.0, z = 9.81)
        }
        assertNull("Al detenerse no debe seguir mostrando un periodo", quiet.periodSeconds)
        assertNull(quiet.frequencyHz)
        assertEquals(
            "No debe inventar oscilaciones nuevas en reposo",
            oscillationsWhileMoving,
            quiet.oscillations
        )
    }

    @Test
    fun frequencyIsInverseOfPeriod() {
        val analyzer = PendulumAnalyzer()
        val state = feedOscillation(analyzer, periodSec = 1.6, amplitude = 2.5, durationSec = 20.0)
        val period = state.periodSeconds!!
        assertEquals(1.0 / period, state.frequencyHz!!, 1e-9)
    }

    @Test
    fun ignoresNoiseBurstsShorterThanMinimumPeriod() {
        val analyzer = PendulumAnalyzer(minPeriodSeconds = 0.35)
        var state = PendulumState()
        // Ruido alternante a 25 Hz: mucha amplitud pero ningún ciclo físicamente plausible.
        for (i in 0 until 600) {
            val value = if (i % 2 == 0) 3.0 else -3.0
            state = analyzer.update(1_000L + i * 20L, x = 0.0, y = value, z = 9.81)
        }
        assertNull("El ruido rápido no debe producir un periodo", state.periodSeconds)
    }

    @Test
    fun resetClearsPreviousMeasurement() {
        val analyzer = PendulumAnalyzer()
        feedOscillation(analyzer, periodSec = 1.0, amplitude = 2.0, durationSec = 10.0)
        analyzer.reset()
        val state = analyzer.update(5_000L, x = 0.0, y = 0.0, z = 9.81)
        assertNull(state.periodSeconds)
        assertEquals(0, state.oscillations)
    }
}
