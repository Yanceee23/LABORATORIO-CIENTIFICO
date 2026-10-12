package com.laboratoriofisico.app.tutor.ejercicio

import com.laboratoriofisico.app.backend.model.EjercicioDato
import com.laboratoriofisico.app.backend.model.EjercicioResultadoValor
import com.laboratoriofisico.app.backend.model.MateriaEjercicio
import com.laboratoriofisico.app.backend.model.VerificacionEjercicio
import com.laboratoriofisico.app.fisica.FisicaEngine
import com.laboratoriofisico.app.fisica.FormulaId
import kotlin.math.abs

/**
 * Recalcula en física/matemática simple con [FisicaEngine] y compara con Gemini.
 */
class EjercicioLocalVerifier(
    private val fisicaEngine: FisicaEngine,
    private val toleranciaRelativa: Double = 0.05,
    private val toleranciaAbsoluta: Double = 1e-6
) {
    fun verificar(
        materia: MateriaEjercicio,
        formula: String,
        datos: List<EjercicioDato>,
        resultadoGemini: EjercicioResultadoValor
    ): VerificacionEjercicio {
        if (materia != MateriaEjercicio.FISICA && materia != MateriaEjercicio.MATEMATICA) {
            return VerificacionEjercicio.SIN_VERIFICACION
        }
        val formulaId = mapFormula(formula) ?: return VerificacionEjercicio.SIN_VERIFICACION
        val entradas = mapEntradas(datos, formulaId)
        if (entradas.isEmpty()) return VerificacionEjercicio.SIN_VERIFICACION
        val local = fisicaEngine.calcular(formulaId.id, entradas)
            ?: return VerificacionEjercicio.SIN_VERIFICACION
        return if (coincide(local, resultadoGemini.valor)) {
            VerificacionEjercicio.COINCIDE
        } else {
            VerificacionEjercicio.NO_COINCIDE
        }
    }

    fun mapFormula(formula: String): FormulaId? {
        val n = formula.lowercase()
            .replace(" ", "")
            .replace("·", "*")
            .replace("×", "*")
        return when {
            n.contains("v=d/t") || n.contains("v=d÷t") ||
                n.contains("velocidad") && n.contains("d/t") ||
                n.contains("movimiento.vigualdt") -> FormulaId.V_IGUAL_D_T
            n.contains("d=v*t") || n.contains("d=vt") ||
                n.contains("movimiento.digualvt") -> FormulaId.D_IGUAL_V_T
            n.contains("a=Δv/Δt") || n.contains("a=dv/dt") || n.contains("a=Δv/Δt") ||
                n.contains("a=") && (n.contains("deltav") || n.contains("Δv")) ||
                n.contains("movimiento.aigualdeltavdeltat") -> FormulaId.A_IGUAL_DELTA_V_DELTA_T
            n.contains("f=1/t") || n.contains("ondas.figual1sobret") -> FormulaId.F_IGUAL_1_SOBRE_T
            n.contains("t=1/f") || n.contains("ondas.tigual1sobref") -> FormulaId.T_IGUAL_1_SOBRE_F
            n.contains("v=λ*f") || n.contains("v=lambda*f") || n.contains("v=λf") ||
                n.contains("ondas.viguallambdaf") -> FormulaId.V_IGUAL_LAMBDA_F
            n.contains("pendulo") || n.contains("g=") && n.contains("4π") ||
                n.contains("pendulo.gpendulo") -> FormulaId.G_PENDULO
            n.contains("b=") && (n.contains("bx") || n.contains("√")) ||
                n.contains("magnetismo.bintensidad") -> FormulaId.B_INTENSIDAD
            n.contains("theta") || n.contains("reflexion.thetaincidenciareflexion") ->
                FormulaId.THETA_INCIDENCIA_REFLEXION
            FormulaId.from(formula.trim()) != null -> FormulaId.from(formula.trim())
            else -> null
        }
    }

    fun mapEntradas(datos: List<EjercicioDato>, formulaId: FormulaId): Map<String, Double> {
        val byName = datos.associate { normalizeName(it.nombre) to it.valor }
        fun pick(vararg keys: String): Double? =
            keys.firstNotNullOfOrNull { byName[it] }

        return when (formulaId) {
            FormulaId.V_IGUAL_D_T -> buildMap {
                pick("d", "distancia", "desplazamiento")?.let { put("d", it) }
                pick("t", "tiempo")?.let { put("t", it) }
            }
            FormulaId.D_IGUAL_V_T -> buildMap {
                pick("v", "velocidad")?.let { put("v", it) }
                pick("t", "tiempo")?.let { put("t", it) }
            }
            FormulaId.A_IGUAL_DELTA_V_DELTA_T -> buildMap {
                pick("dv", "deltav", "delta_v", "Δv")?.let { put("dv", it) }
                pick("dt", "deltat", "delta_t", "Δt")?.let { put("dt", it) }
            }
            FormulaId.F_IGUAL_1_SOBRE_T -> buildMap {
                pick("t", "periodo", "T")?.let { put("T", it) }
            }
            FormulaId.T_IGUAL_1_SOBRE_F -> buildMap {
                pick("f", "frecuencia")?.let { put("f", it) }
            }
            FormulaId.V_IGUAL_LAMBDA_F -> buildMap {
                pick("l", "lambda", "λ", "longituddeonda")?.let { put("lambda", it) }
                pick("f", "frecuencia")?.let { put("f", it) }
            }
            FormulaId.G_PENDULO -> buildMap {
                pick("l", "longitud", "L")?.let { put("L", it) }
                pick("t", "periodo", "T")?.let { put("T", it) }
            }
            FormulaId.B_INTENSIDAD -> buildMap {
                pick("bx", "Bx")?.let { put("Bx", it) }
                pick("by", "By")?.let { put("By", it) }
                pick("bz", "Bz")?.let { put("Bz", it) }
            }
            FormulaId.THETA_INCIDENCIA_REFLEXION -> buildMap {
                pick("theta_i", "thi", "incidenci", "θi")?.let { put("theta_i", it) }
                pick("theta_r", "thr", "reflex", "θr")?.let { put("theta_r", it) }
            }
        }
    }

    private fun normalizeName(nombre: String): String =
        nombre.trim().lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u")
            .replace(" ", "")

    private fun coincide(local: Double, gemini: Double): Boolean {
        val diff = abs(local - gemini)
        val scale = maxOf(abs(local), abs(gemini), 1.0)
        return diff <= toleranciaAbsoluta || diff / scale <= toleranciaRelativa
    }
}
