package com.laboratoriofisico.app.sensores.util

import kotlin.math.abs
import kotlin.math.round

/**
 * Suaviza la cifra que ve el usuario. El sensor sigue llegando crudo;
 * la pantalla solo cambia cuando el valor se movió de verdad.
 */
class LiveDisplayFilter(private val alpha: Double = 0.12) {
    private val state = HashMap<String, Double>()
    private val published = HashMap<String, Double>()

    fun stable(key: String, raw: Double, step: Double = 0.1): Double {
        val previous = state[key]
        val next = if (previous == null) raw else previous + alpha * (raw - previous)
        state[key] = next
        val last = published[key]
        if (last != null && abs(next - last) < step) return last
        val snapped = round(next / step) * step
        published[key] = snapped
        return snapped
    }

    fun reset() {
        state.clear()
        published.clear()
    }
}
