package com.laboratoriofisico.app.sensores.util

import kotlin.math.abs

enum class NearbyMagnetismLevel {
    MUY_BAJO,
    MEDIO,
    ALTO
}

/**
 * Magnetismo cercano: diferencia real entre la lectura y el campo de la habitación.
 * Sin imán queda cerca de cero. Al acercar un imán, el valor sube.
 */
object NearbyMagnetism {
    const val LOW_MAX_UT = 8.0
    const val MEDIUM_MAX_UT = 45.0
    private const val HIGH_BAR_SPAN_UT = 100.0

    fun excessUt(deltaUt: Double?): Double? = deltaUt?.let { abs(it) }

    fun level(excessUt: Double): NearbyMagnetismLevel = when {
        excessUt < LOW_MAX_UT -> NearbyMagnetismLevel.MUY_BAJO
        excessUt < MEDIUM_MAX_UT -> NearbyMagnetismLevel.MEDIO
        else -> NearbyMagnetismLevel.ALTO
    }

    fun gaugeProgress(excessUt: Double?): Float {
        if (excessUt == null || excessUt <= 0.0) return 0f
        val portion = when {
            excessUt < LOW_MAX_UT ->
                (excessUt / LOW_MAX_UT) * 0.33
            excessUt < MEDIUM_MAX_UT ->
                0.33 + ((excessUt - LOW_MAX_UT) / (MEDIUM_MAX_UT - LOW_MAX_UT)) * 0.33
            else ->
                0.66 + ((excessUt - MEDIUM_MAX_UT) / HIGH_BAR_SPAN_UT).coerceAtMost(1.0) * 0.34
        }
        return portion.toFloat().coerceIn(0f, 1f)
    }
}
