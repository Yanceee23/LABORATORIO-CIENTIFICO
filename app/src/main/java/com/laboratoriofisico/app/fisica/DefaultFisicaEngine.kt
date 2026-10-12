package com.laboratoriofisico.app.fisica

import com.laboratoriofisico.app.p05formulasfisicas.magnetismo.BIntensidad
import com.laboratoriofisico.app.p05formulasfisicas.movimiento.AIgualDeltaVDeltaT
import com.laboratoriofisico.app.p05formulasfisicas.movimiento.DIgualVT
import com.laboratoriofisico.app.p05formulasfisicas.movimiento.VIgualDT
import com.laboratoriofisico.app.p05formulasfisicas.ondas.FIgual1SobreT
import com.laboratoriofisico.app.p05formulasfisicas.ondas.TIgual1SobreF
import com.laboratoriofisico.app.p05formulasfisicas.ondas.VIgualLambdaF
import com.laboratoriofisico.app.p05formulasfisicas.pendulo.GPendulo
import com.laboratoriofisico.app.p05formulasfisicas.reflexion.ThetaIncidenciaReflexion

class DefaultFisicaEngine : FisicaEngine {

    override fun calcular(formulaId: String, entradas: Map<String, Double>): Double? {
        val id = FormulaId.from(formulaId) ?: return null
        return when (id) {
            FormulaId.V_IGUAL_D_T -> {
                val d = entradas["d"] ?: return null
                val t = entradas["t"] ?: return null
                VIgualDT.calcular(d, t)
            }
            FormulaId.A_IGUAL_DELTA_V_DELTA_T -> {
                val deltaV = entradas["dv"] ?: entradas["deltaV"] ?: return null
                val deltaT = entradas["dt"] ?: entradas["deltaT"] ?: return null
                AIgualDeltaVDeltaT.calcular(deltaV, deltaT)
            }
            FormulaId.D_IGUAL_V_T -> {
                val v = entradas["v"] ?: return null
                val t = entradas["t"] ?: return null
                DIgualVT.calcular(v, t)
            }
            FormulaId.F_IGUAL_1_SOBRE_T -> {
                val periodo = entradas["T"] ?: entradas["t"] ?: return null
                FIgual1SobreT.calcular(periodo)
            }
            FormulaId.T_IGUAL_1_SOBRE_F -> {
                val f = entradas["f"] ?: return null
                TIgual1SobreF.calcular(f)
            }
            FormulaId.V_IGUAL_LAMBDA_F -> {
                val lambda = entradas["l"] ?: entradas["lambda"] ?: return null
                val f = entradas["f"] ?: return null
                VIgualLambdaF.calcular(lambda, f)
            }
            FormulaId.G_PENDULO -> {
                val longitud = entradas["L"] ?: entradas["l"] ?: return null
                val periodo = entradas["T"] ?: entradas["t"] ?: return null
                GPendulo.calcular(longitud, periodo)
            }
            FormulaId.B_INTENSIDAD -> {
                val bx = entradas["Bx"] ?: entradas["bx"] ?: return null
                val by = entradas["By"] ?: entradas["by"] ?: return null
                val bz = entradas["Bz"] ?: entradas["bz"] ?: return null
                BIntensidad.calcular(bx, by, bz)
            }
            FormulaId.THETA_INCIDENCIA_REFLEXION -> {
                val thetaI = entradas["theta_i"] ?: entradas["ti"] ?: return null
                val thetaR = entradas["theta_r"] ?: entradas["tr"] ?: return null
                ThetaIncidenciaReflexion.calcular(thetaI, thetaR)
            }
        }
    }
}
