package com.laboratoriofisico.app.p05formulasfisicas.ondas

import kotlin.math.abs

object FIgual1SobreT {
    const val FORMULA: String = "f=1/T"

    fun calcular(periodo: Double): Double? {
        if (abs(periodo) <= EPSILON) return null
        return 1.0 / periodo
    }

    private const val EPSILON = 1e-12
}
