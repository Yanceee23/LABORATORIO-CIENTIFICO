package com.laboratoriofisico.app.sensores.util

import kotlin.math.max

data class RelativeLightResult(
    val luma: Double,
    val rawEquivalent: Double,
    val baseline: Double?,
    val relativeChange: Double,
    val level: LightLevel,
    val calibrated: Boolean,
    val calibrationJustCompleted: Boolean
)

/**
 * Calibra automáticamente la iluminación durante el primer segundo y clasifica cambios
 * relativos. No presupone lux ni valores absolutos propios de un modelo de teléfono.
 */
class RelativeLightCalibrator(
    private val calibrationDurationMs: Long = 1_000L,
    private val relativeThreshold: Double = 0.15
) {
    private var startedAtMs: Long? = null
    private val baselineSamples = mutableListOf<Double>()
    private var baseline: Double? = null

    fun reset() {
        startedAtMs = null
        baselineSamples.clear()
        baseline = null
    }

    /** Posición de la barra: 0.5 = línea base; se mueve con el cambio relativo. */
    fun progressFor(relativeChange: Double, luma: Double, calibrated: Boolean): Float =
        if (!calibrated) {
            (luma / LUMA_MAX).coerceIn(0.0, 1.0).toFloat()
        } else {
            (0.5 + relativeChange / 2.0).coerceIn(0.0, 1.0).toFloat()
        }

    fun update(luma: Double, timestampMs: Long): RelativeLightResult {
        val safeLuma = luma.coerceIn(0.0, LUMA_MAX)
        val start = startedAtMs ?: timestampMs.also { startedAtMs = it }
        var justCompleted = false

        if (baseline == null && timestampMs - start < calibrationDurationMs) {
            baselineSamples += safeLuma
        } else if (baseline == null) {
            if (baselineSamples.isEmpty()) baselineSamples += safeLuma
            baseline = baselineSamples.average()
            justCompleted = true
        }

        val reference = baseline
        val relativeChange = if (reference == null) {
            0.0
        } else {
            (safeLuma - reference) / max(reference, MIN_REFERENCE_LUMA)
        }
        val level = when {
            reference == null -> LightLevel.MEDIUM
            relativeChange <= -relativeThreshold -> LightLevel.LOW
            relativeChange >= relativeThreshold -> LightLevel.HIGH
            else -> LightLevel.MEDIUM
        }

        return RelativeLightResult(
            luma = safeLuma,
            rawEquivalent = safeLuma / LUMA_MAX * RAW_SCALE_MAX,
            baseline = reference,
            relativeChange = relativeChange,
            level = level,
            calibrated = reference != null,
            calibrationJustCompleted = justCompleted
        )
    }

    private companion object {
        const val LUMA_MAX = 255.0
        const val RAW_SCALE_MAX = 60_000.0
        const val MIN_REFERENCE_LUMA = 16.0
    }
}
