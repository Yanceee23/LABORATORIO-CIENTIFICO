package com.laboratoriofisico.app.p05formulasfisicas.magnetismo

import kotlin.math.sqrt

object BIntensidad {
    const val FORMULA: String = "B=sqrt(Bx2+By2+Bz2)"

    fun calcular(bx: Double, by: Double, bz: Double): Double =
        sqrt(bx * bx + by * by + bz * bz)
}
