package com.laboratoriofisico.app.p05formulasfisicas.reflexion

import kotlin.math.abs

object ThetaIncidenciaReflexion {
    const val FORMULA: String = "ti=tr"

    /** Devuelve |θi − θr|; 0 indica que se cumple la ley de reflexión. */
    fun calcular(thetaIncidencia: Double, thetaReflexion: Double): Double =
        abs(thetaIncidencia - thetaReflexion)
}
