package com.laboratoriofisico.app.p05formulasfisicas.pendulo

import kotlin.math.PI
import kotlin.math.abs

object GPendulo {
    const val FORMULA: String = "g=4pi2L/T2"

    fun calcular(longitud: Double, periodo: Double): Double? {
        if (abs(periodo) <= EPSILON) return null
        return 4.0 * PI * PI * longitud / (periodo * periodo)
    }

    private const val EPSILON = 1e-12
}
