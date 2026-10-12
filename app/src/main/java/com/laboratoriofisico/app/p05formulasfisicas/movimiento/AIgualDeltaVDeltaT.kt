package com.laboratoriofisico.app.p05formulasfisicas.movimiento

import kotlin.math.abs

object AIgualDeltaVDeltaT {
    const val FORMULA: String = "a=dv/dt"

    fun calcular(deltaV: Double, deltaT: Double): Double? {
        if (abs(deltaT) <= EPSILON) return null
        return deltaV / deltaT
    }

    private const val EPSILON = 1e-12
}
