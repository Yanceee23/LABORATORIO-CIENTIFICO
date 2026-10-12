package com.laboratoriofisico.app.tutor.juego

import com.laboratoriofisico.app.backend.model.EjercicioDato
import com.laboratoriofisico.app.backend.model.EjercicioResuelto
import com.laboratoriofisico.app.fisica.FormulaId
import com.laboratoriofisico.app.tutor.ejercicio.EjercicioLocalVerifier
import com.laboratoriofisico.app.tutor.ejercicio.TutorTextCleaner
import kotlin.math.abs

/**
 * Lógica pura del juego "Aprender jugando". Sin Gemini.
 */
object JuegoEjercicioEngine {

    private val formulaLegiblesConocidas = mapOf(
        FormulaId.V_IGUAL_D_T to "v = d ÷ t",
        FormulaId.D_IGUAL_V_T to "d = v × t",
        FormulaId.A_IGUAL_DELTA_V_DELTA_T to "a = Δv ÷ Δt",
        FormulaId.F_IGUAL_1_SOBRE_T to "f = 1 ÷ T",
        FormulaId.T_IGUAL_1_SOBRE_F to "T = 1 ÷ f",
        FormulaId.V_IGUAL_LAMBDA_F to "v = λ × f",
        FormulaId.G_PENDULO to "g = 4π²L ÷ T²",
        FormulaId.B_INTENSIDAD to "B = √(Bx² + By² + Bz²)",
        FormulaId.THETA_INCIDENCIA_REFLEXION to "θi = θr"
    )

    fun puedeRetoGuiado(ejercicio: EjercicioResuelto, verifier: EjercicioLocalVerifier): Boolean {
        if (ejercicio.datos.size < 2) return false
        if (ejercicio.formula.isBlank() && ejercicio.formulaLegible.isBlank()) return false
        return verifier.mapFormula(ejercicio.formula).let { it != null } ||
            verifier.mapFormula(ejercicio.formulaLegible) != null
    }

    fun validarRespuestaNumerica(
        respuestaTexto: String,
        unidadTexto: String,
        valorEsperado: Double,
        unidadEsperada: String,
        toleranciaRelativa: Double = 0.05,
        toleranciaAbsoluta: Double = 1e-3
    ): Boolean {
        val valor = parseNumero(respuestaTexto) ?: return false
        if (!unidadesCompatibles(unidadTexto, unidadEsperada)) return false
        val diff = abs(valor - valorEsperado)
        val scale = maxOf(abs(valorEsperado), abs(valor), 1.0)
        return diff <= toleranciaAbsoluta || diff / scale <= toleranciaRelativa
    }

    /**
     * 3 = acierto al primer intento sin pistas.
     * 2 = usó pistas (sin fallos previos, o solo pistas).
     * 1 = falló antes (o usó "mostrar respuesta").
     */
    fun calcularEstrellas(
        aciertoPrimerIntento: Boolean,
        pistasUsadas: Int,
        fallosPrevios: Int,
        mostroRespuesta: Boolean = false
    ): Int = when {
        mostroRespuesta || fallosPrevios > 0 -> 1
        pistasUsadas > 0 -> 2
        aciertoPrimerIntento -> 3
        else -> 1
    }

    fun generarOpcionesFormula(
        ejercicio: EjercicioResuelto,
        verifier: EjercicioLocalVerifier
    ): List<FormulaOpcion> {
        val correctaRaw = ejercicio.formulaLegible.ifBlank {
            TutorTextCleaner.formulaLegibleDesdeCruda(ejercicio.formula)
        }
        val correcta = TutorTextCleaner.clean(correctaRaw).ifBlank { "—" }
        val idCorrecta = verifier.mapFormula(ejercicio.formula)
            ?: verifier.mapFormula(ejercicio.formulaLegible)
        val distractores = formulaLegiblesConocidas
            .filterKeys { it != idCorrecta }
            .values
            .filter { !mismaFormula(it, correcta) }
            .shuffled()
            .take(2)
            .ifEmpty {
                listOf("v = λ × f", "a = Δv ÷ Δt").filter { !mismaFormula(it, correcta) }.take(2)
            }
        return (listOf(FormulaOpcion(correcta, true)) + distractores.map { FormulaOpcion(it, false) })
            .shuffled()
    }

    fun generarOpcionesDatos(datos: List<EjercicioDato>): List<DatoOpcion> {
        val correctas = datos.map {
            DatoOpcion(
                etiqueta = "${it.nombre} = ${formatCorto(it.valor)} ${it.unidad}".trim(),
                correcto = true,
                dato = it
            )
        }
        val distractores = distractoresDatos(datos).map {
            DatoOpcion(
                etiqueta = "${it.nombre} = ${formatCorto(it.valor)} ${it.unidad}".trim(),
                correcto = false,
                dato = it
            )
        }
        return (correctas + distractores).shuffled()
    }

    fun slotsSustitucion(ejercicio: EjercicioResuelto): List<SustitucionSlot> {
        val formula = ejercicio.formulaLegible.ifBlank {
            TutorTextCleaner.formulaLegibleDesdeCruda(ejercicio.formula)
        }
        return ejercicio.datos.mapIndexed { index, d ->
            SustitucionSlot(
                id = index,
                simbolo = d.nombre,
                valorCorrecto = d.valor,
                unidad = d.unidad,
                pistaEnFormula = formula
            )
        }
    }

    fun parseNumero(raw: String): Double? {
        val s = raw.trim()
            .replace(",", ".")
            .replace(Regex("[^0-9.\\-]"), "")
        return s.toDoubleOrNull()
    }

    private fun unidadesCompatibles(a: String, b: String): Boolean {
        val na = normalizarUnidad(a)
        val nb = normalizarUnidad(b)
        if (na.isEmpty() || nb.isEmpty()) return true
        return na == nb
    }

    private fun normalizarUnidad(u: String): String =
        u.trim().lowercase()
            .replace(" ", "")
            .replace("·", "/")
            .replace("×", "")
            .replace("metros/segundo", "m/s")
            .replace("m/s^2", "m/s²")
            .replace("m/s2", "m/s²")

    private fun mismaFormula(a: String, b: String): Boolean {
        fun n(s: String) = s.lowercase()
            .replace(" ", "")
            .replace("÷", "/")
            .replace("×", "*")
            .replace("·", "*")
        return n(a) == n(b)
    }

    private fun distractoresDatos(datos: List<EjercicioDato>): List<EjercicioDato> {
        val nombresUsados = datos.map { it.nombre.lowercase() }.toSet()
        val pool = listOf(
            EjercicioDato("masa", 2.5, "kg"),
            EjercicioDato("temperatura", 25.0, "°C"),
            EjercicioDato("presión", 101.0, "kPa"),
            EjercicioDato("carga", 1.6, "C"),
            EjercicioDato("ángulo", 30.0, "°")
        ).filter { it.nombre.lowercase() !in nombresUsados }
        val count = if (datos.size >= 3) 1 else 2
        return pool.shuffled().take(count)
    }

    private fun formatCorto(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString()
        else String.format(java.util.Locale.US, "%.4g", v)
}

data class FormulaOpcion(val texto: String, val correcta: Boolean)

data class DatoOpcion(
    val etiqueta: String,
    val correcto: Boolean,
    val dato: EjercicioDato
)

data class SustitucionSlot(
    val id: Int,
    val simbolo: String,
    val valorCorrecto: Double,
    val unidad: String,
    val pistaEnFormula: String
)
