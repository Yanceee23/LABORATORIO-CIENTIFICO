package com.laboratoriofisico.app.p05formulasfisicas.ondas

import kotlin.math.abs

object TIgual1SobreF {
    const val FORMULA: String = "T=1/f"

    fun calcular(frecuencia: Double): Double? {
        if (abs(frecuencia) <= EPSILON) return null
        return 1.0 / frecuencia
    }

    private const val EPSILON = 1e-12
}
