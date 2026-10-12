package com.laboratoriofisico.app.sensores.util

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

data class PendulumState(
    val periodSeconds: Double? = null,
    val frequencyHz: Double? = null,
    val oscillations: Int = 0,
    val amplitudeMs2: Double? = null,
    val elapsedSeconds: Double = 0.0,
    val signal: Double = 0.0,
    val dominantAxis: PendulumAxis? = null,
    val hasEnoughSignal: Boolean = false,
    val lastCyclePeriodSec: Double? = null,
    val averagePeriodSec: Double? = null
)

enum class PendulumAxis { X, Y, Z }

/**
 * Detector de oscilaciones periódicas con acelerómetro real.
 *
 * Tras un breve calentamiento fija el eje de mayor varianza para evitar saltos entre ejes
 * cuando el teléfono se mueve en más de una dirección.
 */
class PendulumAnalyzer(
    private val minPeriodSeconds: Double = 0.35,
    private val maxPeriodSeconds: Double = 12.0,
    private val minAmplitudeMs2: Double = 0.25,
    private val periodWindow: Int = 8
) {
    private val lowPassStage1 = DoubleArray(3)
    private val lowPassStage2 = DoubleArray(3)
    private val baseline = DoubleArray(3)
    private val variance = DoubleArray(3)
    private var lastSampleMs: Long? = null
    private var initialized = false
    private var warmupSamples = 0
    private var lockedAxisIndex: Int? = null

    private var startTimestampMs: Long? = null
    private var lastCrossingMs: Long? = null
    private var armedBelow = false
    private val periods = ArrayDeque<Double>()
    private var oscillationCount = 0
    private var lastCyclePeriodSec: Double? = null

    fun reset() {
        lowPassStage1.fill(0.0)
        lowPassStage2.fill(0.0)
        baseline.fill(0.0)
        variance.fill(0.0)
        lastSampleMs = null
        initialized = false
        warmupSamples = 0
        lockedAxisIndex = null
        startTimestampMs = null
        lastCrossingMs = null
        armedBelow = false
        periods.clear()
        oscillationCount = 0
        lastCyclePeriodSec = null
    }

    fun update(timestampMs: Long, x: Double, y: Double, z: Double): PendulumState {
        val raw = doubleArrayOf(x, y, z)
        if (!initialized) {
            initialized = true
            startTimestampMs = timestampMs
            lastSampleMs = timestampMs
            for (i in raw.indices) {
                lowPassStage1[i] = raw[i]
                lowPassStage2[i] = raw[i]
                baseline[i] = raw[i]
                variance[i] = 0.0
            }
            return currentState(timestampMs, signal = 0.0, axisIndex = null)
        }

        expireStalePeriods(timestampMs)
        val axes = lowPass(timestampMs, raw)

        for (i in axes.indices) {
            val deviation = axes[i] - baseline[i]
            baseline[i] += BASELINE_ALPHA * deviation
            variance[i] += VARIANCE_ALPHA * (deviation * deviation - variance[i])
        }

        warmupSamples++
        if (lockedAxisIndex == null && warmupSamples >= WARMUP_SAMPLES) {
            lockedAxisIndex = (0..2).maxByOrNull { variance[it] }
        }

        val axisIndex = lockedAxisIndex ?: (0..2).maxByOrNull { variance[it] } ?: 0
        val signal = axes[axisIndex] - baseline[axisIndex]
        val amplitude = sqrt(max(variance[axisIndex], 0.0)) * SQRT_TWO
        val threshold = max(minAmplitudeMs2 * HYSTERESIS_FLOOR, amplitude * HYSTERESIS_RATIO)

        if (amplitude >= minAmplitudeMs2) {
            detectCrossing(timestampMs, signal, threshold)
        } else {
            armedBelow = false
        }

        return currentState(timestampMs, signal, axisIndex, amplitude)
    }

    private fun lowPass(timestampMs: Long, raw: DoubleArray): DoubleArray {
        val previous = lastSampleMs
        lastSampleMs = timestampMs
        val dtSec = previous
            ?.let { (timestampMs - it) / 1000.0 }
            ?.coerceIn(MIN_DT_SECONDS, MAX_DT_SECONDS)
            ?: MIN_DT_SECONDS
        val alpha = dtSec / (dtSec + LOW_PASS_TAU)
        for (i in raw.indices) {
            lowPassStage1[i] += alpha * (raw[i] - lowPassStage1[i])
            lowPassStage2[i] += alpha * (lowPassStage1[i] - lowPassStage2[i])
        }
        return lowPassStage2
    }

    private fun detectCrossing(timestampMs: Long, signal: Double, threshold: Double) {
        if (signal < -threshold) {
            armedBelow = true
            return
        }
        if (!armedBelow || signal <= threshold) return

        val previous = lastCrossingMs
        armedBelow = false
        lastCrossingMs = timestampMs
        if (previous == null) return

        val periodSec = (timestampMs - previous) / 1000.0
        if (periodSec < minPeriodSeconds || periodSec > maxPeriodSeconds) return

        oscillationCount++
        lastCyclePeriodSec = periodSec
        periods.addLast(periodSec)
        while (periods.size > periodWindow) periods.removeFirst()
    }

    private fun currentState(
        timestampMs: Long,
        signal: Double,
        axisIndex: Int?,
        amplitude: Double? = null
    ): PendulumState {
        val elapsed = startTimestampMs?.let { (timestampMs - it).coerceAtLeast(0) / 1000.0 } ?: 0.0
        val period = averagePeriod()
        val enoughSignal = (amplitude ?: 0.0) >= minAmplitudeMs2
        return PendulumState(
            periodSeconds = period,
            frequencyHz = period?.takeIf { it > 0.0 }?.let { 1.0 / it },
            oscillations = oscillationCount,
            amplitudeMs2 = amplitude,
            elapsedSeconds = elapsed,
            signal = signal,
            dominantAxis = axisIndex?.let { PendulumAxis.entries[it] },
            hasEnoughSignal = enoughSignal,
            lastCyclePeriodSec = lastCyclePeriodSec,
            averagePeriodSec = period
        )
    }

    private fun expireStalePeriods(timestampMs: Long) {
        val last = lastCrossingMs ?: return
        if (periods.isEmpty()) return
        val toleranceMs = (periods.average() * 1000.0 * STALE_CYCLES).coerceAtLeast(MIN_STALE_MS)
        if (timestampMs - last > toleranceMs) {
            periods.clear()
            lastCyclePeriodSec = null
            armedBelow = false
        }
    }

    private fun averagePeriod(): Double? {
        if (periods.size < MIN_PERIODS) return null
        val sorted = periods.sorted()
        val median = sorted[sorted.size / 2]
        val consistent = periods.filter { abs(it - median) <= median * OUTLIER_TOLERANCE }
        if (consistent.size < MIN_PERIODS) return null
        return consistent.average()
    }

    private companion object {
        const val BASELINE_ALPHA = 0.04
        const val VARIANCE_ALPHA = 0.06
        const val HYSTERESIS_RATIO = 0.22
        const val HYSTERESIS_FLOOR = 0.45
        const val OUTLIER_TOLERANCE = 0.35
        const val MIN_PERIODS = 2
        const val STALE_CYCLES = 3.0
        const val MIN_STALE_MS = 2_000.0
        const val LOW_PASS_CUTOFF_HZ = 6.0
        const val LOW_PASS_TAU = 1.0 / (2.0 * Math.PI * LOW_PASS_CUTOFF_HZ)
        const val MIN_DT_SECONDS = 0.001
        const val MAX_DT_SECONDS = 1.0
        const val WARMUP_SAMPLES = 40
        val SQRT_TWO = sqrt(2.0)
    }
}
