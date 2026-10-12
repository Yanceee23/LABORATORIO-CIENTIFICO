package com.laboratoriofisico.app.tutor.ejercicio

import com.laboratoriofisico.app.backend.model.EjercicioResuelto
import com.laboratoriofisico.app.backend.model.EjercicioVariable

/**
 * Limpia markdown/LaTeX que se cuela en respuestas de Gemini.
 * También sirve para ejercicios viejos guardados (sin nueva llamada).
 */
object TutorTextCleaner {
    private val latexCommands = listOf(
        "frac", "text", "mathrm", "mathbf", "left", "right", "cdot", "times",
        "div", "sqrt", "overline", "underline", "begin", "end"
    )

    fun clean(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var s = raw.trim()
        // Bloques de código / backticks
        s = s.replace(Regex("```[a-zA-Z]*"), "")
        s = s.replace("```", "")
        s = s.replace("`", "")
        // Markdown headings / bold / italic
        s = s.replace(Regex("#{1,6}\\s*"), "")
        s = s.replace("**", "")
        s = s.replace("__", "")
        s = s.replace(Regex("(?<!\\w)\\*(?!\\s)"), "")
        // Delimitadores LaTeX
        s = s.replace("$$", "")
        s = s.replace(Regex("""\$+"""), "")
        s = s.replace("\\[", "").replace("\\]", "")
        s = s.replace("\\(", "").replace("\\)", "")
        // \frac{a}{b} → a/b
        s = s.replace(Regex("""\\frac\s*\{([^{}]+)\}\s*\{([^{}]+)\}""")) { m ->
            "${m.groupValues[1].trim()}/${m.groupValues[2].trim()}"
        }
        // \text{...} y similares
        for (cmd in latexCommands) {
            s = s.replace(Regex("""\\$cmd\s*\{([^{}]*)\}"""), "$1")
            s = s.replace(Regex("""\\$cmd\b"""), "")
        }
        // Símbolos comunes
        s = s.replace("\\times", "×")
        s = s.replace("\\cdot", "·")
        s = s.replace("\\div", "÷")
        s = s.replace("\\pi", "π")
        s = s.replace("\\Delta", "Δ")
        s = s.replace("\\delta", "δ")
        s = s.replace("\\sqrt", "√")
        s = s.replace("\\leq", "≤")
        s = s.replace("\\geq", "≥")
        s = s.replace("\\neq", "≠")
        s = s.replace("\\approx", "≈")
        s = s.replace("^2", "²")
        s = s.replace("^3", "³")
        s = s.replace(Regex("""\^\{2\}"""), "²")
        s = s.replace(Regex("""\^\{3\}"""), "³")
        // Barras sobrantes
        s = s.replace("\\", "")
        s = s.replace("$", "")
        s = s.replace(Regex("\\s{2,}"), " ")
        return s.trim()
    }

    /** Convierte fórmulas pegadas tipo v=d/t a forma más legible. */
    fun formulaLegibleDesdeCruda(raw: String?): String {
        var s = clean(raw)
        if (s.isBlank()) return "—"
        s = s.replace(Regex("""\s*=\s*"""), " = ")
        s = s.replace(Regex("""\s*/\s*"""), " ÷ ")
        s = s.replace(Regex("""\s*\*\s*"""), " × ")
        s = s.replace("·", " × ")
        s = s.replace(Regex("""\s{2,}"""), " ")
        return s.trim()
    }
}

object TutorNumberFormat {
    fun format(value: Double, maxDecimals: Int = 4): String {
        if (value.isNaN() || value.isInfinite()) return "—"
        val abs = kotlin.math.abs(value)
        val decimals = when {
            abs >= 1000 -> 0
            abs >= 100 -> 1
            abs >= 10 -> 2
            abs >= 1 -> 3
            else -> maxDecimals
        }
        val pattern = if (decimals == 0) "#,##0" else "#,##0." + "#".repeat(decimals)
        val nf = java.text.DecimalFormat(pattern, java.text.DecimalFormatSymbols(java.util.Locale.US))
        return nf.format(value)
    }

    fun withUnit(value: Double, unidad: String): String {
        val u = unidad.trim()
        return if (u.isEmpty()) format(value) else "${format(value)} $u"
    }
}

fun EjercicioResuelto.paraMostrar(): EjercicioResuelto {
    val limpiaFormula = if (TutorTextCleaner.clean(formulaLegible).isNotBlank()) {
        TutorTextCleaner.clean(formulaLegible)
    } else {
        TutorTextCleaner.formulaLegibleDesdeCruda(formula)
    }
    val limpiaVars = if (variables.isNotEmpty()) {
        variables.map {
            EjercicioVariable(
                simbolo = TutorTextCleaner.clean(it.simbolo),
                nombre = TutorTextCleaner.clean(it.nombre),
                unidad = TutorTextCleaner.clean(it.unidad)
            )
        }
    } else {
        datos.map { d ->
            EjercicioVariable(
                simbolo = TutorTextCleaner.clean(d.nombre).ifBlank { "?" },
                nombre = nombreAmigableVariable(d.nombre),
                unidad = TutorTextCleaner.clean(d.unidad)
            )
        }
    }
    val expl = TutorTextCleaner.clean(explicacionFormula).ifBlank {
        explicacionFormulaFallback(limpiaFormula)
    }
    return copy(
        enunciado = TutorTextCleaner.clean(enunciado),
        incognita = TutorTextCleaner.clean(incognita),
        formula = TutorTextCleaner.clean(formula),
        formulaLegible = limpiaFormula,
        variables = limpiaVars,
        explicacionFormula = expl,
        pasos = pasos.map {
            it.copy(
                titulo = TutorTextCleaner.clean(it.titulo),
                explicacion = TutorTextCleaner.clean(it.explicacion),
                operacion = TutorTextCleaner.clean(it.operacion)
            )
        },
        tema = TutorTextCleaner.clean(tema)
    )
}

private fun nombreAmigableVariable(nombre: String): String {
    val n = TutorTextCleaner.clean(nombre).lowercase()
    return when {
        n == "v" || n.contains("veloc") -> "velocidad"
        n == "d" || n.contains("dist") || n.contains("desplaz") -> "distancia"
        n == "t" || n.contains("tiempo") || n.contains("period") -> "tiempo"
        n == "a" || n.contains("acel") -> "aceleración"
        n == "f" || n.contains("frec") -> "frecuencia"
        else -> TutorTextCleaner.clean(nombre).ifBlank { "magnitud" }
    }
}

private fun explicacionFormulaFallback(formula: String): String = when {
    formula.contains("÷") || formula.contains("/") ->
        "La cantidad que buscas se obtiene dividiendo una magnitud entre otra."
    formula.contains("×") || formula.contains("*") ->
        "La cantidad que buscas se obtiene multiplicando las magnitudes."
    else -> "Usa la fórmula para relacionar los datos y hallar lo que se pide."
}
