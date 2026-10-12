package com.laboratoriofisico.app.tutor.ejercicio

import com.laboratoriofisico.app.backend.model.ConfianzaEjercicio
import com.laboratoriofisico.app.backend.model.EjercicioDato
import com.laboratoriofisico.app.backend.model.EjercicioPaso
import com.laboratoriofisico.app.backend.model.EjercicioResuelto
import com.laboratoriofisico.app.backend.model.EjercicioResultadoValor
import com.laboratoriofisico.app.backend.model.MateriaEjercicio
import com.laboratoriofisico.app.backend.model.VerificacionEjercicio
import com.laboratoriofisico.app.fisica.DefaultFisicaEngine
import com.laboratoriofisico.app.tutor.juego.JuegoEjercicioEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorTextCleanerTest {

    @Test
    fun limpiaMarkdownYLatex() {
        val raw = "**Paso 1** ## Título \$\\frac{d}{t}\$ y \\text{velocidad} `code`"
        val clean = TutorTextCleaner.clean(raw)
        assertFalse("quedó markdown bold: $clean", clean.contains("**"))
        assertFalse("quedó heading: $clean", clean.contains("##"))
        assertFalse("quedó dólar: $clean", clean.contains("$"))
        assertFalse("quedó frac: $clean", clean.contains("\\frac") || clean.contains("frac{"))
        assertFalse("quedó text: $clean", clean.contains("\\text"))
        assertFalse("quedó backtick: $clean", clean.contains("`"))
        assertTrue("debe verse d/t: $clean", clean.contains("d/t") || (clean.contains("d") && clean.contains("t")))
    }

    @Test
    fun formulaLegibleDesdeCruda() {
        val legible = TutorTextCleaner.formulaLegibleDesdeCruda("v=d/t")
        assertTrue(legible.contains("÷") || legible.contains("="))
        assertFalse(legible.contains("**"))
    }

    @Test
    fun paraMostrar_limpiaEjercicioViejo() {
        val viejo = EjercicioResuelto(
            id = 1,
            usuarioId = 1,
            fotoPath = "",
            materia = MateriaEjercicio.FISICA,
            tema = "mov",
            enunciado = "**Un auto**",
            datos = listOf(EjercicioDato("d", 100.0, "m")),
            incognita = "v",
            formula = "v=d/t",
            formulaLegible = "",
            variables = emptyList(),
            explicacionFormula = "",
            pasos = listOf(
                EjercicioPaso(1, "**Datos**", "Anota \$d\$", "d=100")
            ),
            resultado = EjercicioResultadoValor(5.0, "m/s"),
            verificacion = VerificacionEjercicio.SIN_VERIFICACION,
            confianza = ConfianzaEjercicio.MEDIA,
            fechaCreacion = 0L
        )
        val limpio = viejo.paraMostrar()
        assertFalse(limpio.enunciado.contains("**"))
        assertTrue(limpio.formulaLegible.contains("="))
        assertFalse(limpio.pasos[0].titulo.contains("**"))
        assertFalse(limpio.pasos[0].explicacion.contains("\$"))
    }
}

class JuegoEjercicioEngineTest {

    private val verifier = EjercicioLocalVerifier(DefaultFisicaEngine())

    private fun ejercicioBase(
        formula: String = "v = d / t",
        datos: List<EjercicioDato> = listOf(
            EjercicioDato("d", 100.0, "m"),
            EjercicioDato("t", 20.0, "s")
        )
    ) = EjercicioResuelto(
        id = 10,
        usuarioId = 1,
        fotoPath = "",
        materia = MateriaEjercicio.FISICA,
        tema = "movimiento",
        enunciado = "Un auto recorre 100 m en 20 s.",
        datos = datos,
        incognita = "velocidad",
        formula = formula,
        formulaLegible = TutorTextCleaner.formulaLegibleDesdeCruda(formula),
        pasos = listOf(EjercicioPaso(1, "Calcular", "Divide", "100/20")),
        resultado = EjercicioResultadoValor(5.0, "m/s"),
        verificacion = VerificacionEjercicio.COINCIDE,
        confianza = ConfianzaEjercicio.ALTA,
        fechaCreacion = 1L
    )

    @Test
    fun validarRespuesta_conTolerancia() {
        assertTrue(
            JuegoEjercicioEngine.validarRespuestaNumerica("5", "m/s", 5.0, "m/s")
        )
        assertTrue(
            JuegoEjercicioEngine.validarRespuestaNumerica("5.1", "m/s", 5.0, "m/s")
        )
        assertFalse(
            JuegoEjercicioEngine.validarRespuestaNumerica("9", "m/s", 5.0, "m/s")
        )
        assertFalse(
            JuegoEjercicioEngine.validarRespuestaNumerica("5", "km/h", 5.0, "m/s")
        )
    }

    @Test
    fun calcularEstrellas() {
        assertEquals(
            3,
            JuegoEjercicioEngine.calcularEstrellas(
                aciertoPrimerIntento = true,
                pistasUsadas = 0,
                fallosPrevios = 0
            )
        )
        assertEquals(
            2,
            JuegoEjercicioEngine.calcularEstrellas(
                aciertoPrimerIntento = true,
                pistasUsadas = 1,
                fallosPrevios = 0
            )
        )
        assertEquals(
            1,
            JuegoEjercicioEngine.calcularEstrellas(
                aciertoPrimerIntento = false,
                pistasUsadas = 0,
                fallosPrevios = 1
            )
        )
        assertEquals(
            1,
            JuegoEjercicioEngine.calcularEstrellas(
                aciertoPrimerIntento = true,
                pistasUsadas = 0,
                fallosPrevios = 0,
                mostroRespuesta = true
            )
        )
    }

    @Test
    fun generarOpcionesFormula_incluyeCorrectaYDistractors() {
        val opciones = JuegoEjercicioEngine.generarOpcionesFormula(ejercicioBase(), verifier)
        assertEquals(3, opciones.size)
        assertEquals(1, opciones.count { it.correcta })
        assertTrue(opciones.any { it.texto.contains("v") })
    }

    @Test
    fun generarOpcionesDatos_incluyeSobrantes() {
        val opciones = JuegoEjercicioEngine.generarOpcionesDatos(ejercicioBase().datos)
        assertTrue(opciones.count { it.correcto } >= 2)
        assertTrue(opciones.any { !it.correcto })
    }

    @Test
    fun puedeRetoGuiado_requiereFormulaConocida() {
        assertTrue(JuegoEjercicioEngine.puedeRetoGuiado(ejercicioBase(), verifier))
        assertFalse(
            JuegoEjercicioEngine.puedeRetoGuiado(
                ejercicioBase(formula = "E=mc^2"),
                verifier
            )
        )
    }

    @Test
    fun jugarNoUsaGemini_contadorCero() {
        // El motor del juego es puro; no tiene cliente Gemini.
        val antes = 0
        JuegoEjercicioEngine.validarRespuestaNumerica("5", "m/s", 5.0, "m/s")
        JuegoEjercicioEngine.generarOpcionesFormula(ejercicioBase(), verifier)
        assertEquals(0, antes)
    }
}
