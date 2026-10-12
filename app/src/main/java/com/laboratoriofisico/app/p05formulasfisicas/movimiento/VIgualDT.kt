package com.laboratoriofisico.app.p05formulasfisicas.movimiento

import kotlin.math.abs

object VIgualDT {
    const val FORMULA: String = "v=d/t"

    fun calcular(d: Double, t: Double): Double? {
        if (abs(t) <= EPSILON) return null
        return d / t
    }

    private const val EPSILON = 1e-12
}
