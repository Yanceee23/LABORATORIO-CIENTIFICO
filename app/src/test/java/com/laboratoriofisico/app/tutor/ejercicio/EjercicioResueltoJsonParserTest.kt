package com.laboratoriofisico.app.tutor.ejercicio

import com.laboratoriofisico.app.backend.model.EjercicioDato
import com.laboratoriofisico.app.backend.model.EjercicioResultadoValor
import com.laboratoriofisico.app.backend.model.MateriaEjercicio
import com.laboratoriofisico.app.backend.model.VerificacionEjercicio
import com.laboratoriofisico.app.fisica.DefaultFisicaEngine
import com.laboratoriofisico.app.fisica.FormulaId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EjercicioResueltoJsonParserTest {

    private val validJson = """
        {
          "materia":"fisica",
          "tema":"movimiento",
          "enunciado":"Un auto recorre 100 m en 20 s. ¿Cuál es su velocidad?",
          "datos":[{"nombre":"d","valor":100,"unidad":"m"},{"nombre":"t","valor":20,"unidad":"s"}],
          "incognita":"velocidad",
          "formula":"v = d / t",
          "formula_legible":"v = d ÷ t",
          "variables":[{"simbolo":"v","nombre":"velocidad","unidad":"m/s"},{"simbolo":"d","nombre":"distancia","unidad":"m"}],
          "explicacion_formula":"La velocidad es la distancia dividida entre el tiempo",
          "pasos":[
            {"numero":1,"titulo":"Datos","explicacion":"Anota distancia y tiempo.","operacion":"d=100, t=20"},
            {"numero":2,"titulo":"Calcular","explicacion":"Divide distancia entre tiempo.","operacion":"100/20"}
          ],
          "resultado":{"valor":5,"unidad":"m/s"},
          "confianza":"alta"
        }
    """.trimIndent()

    @Test
    fun parse_validJson_ok() {
        val result = EjercicioResueltoJsonParser.parse(validJson)
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals(MateriaEjercicio.FISICA, parsed.materia)
        assertEquals(2, parsed.pasos.size)
        assertEquals(5.0, parsed.resultado.valor, 0.001)
        assertEquals("v = d ÷ t", parsed.formulaLegible)
        assertEquals(2, parsed.variables.size)
        assertTrue(parsed.explicacionFormula.contains("velocidad"))
    }

    @Test
    fun parse_markdownWrapped_reparaUnaVez() {
        val wrapped = "```json\n$validJson\n```"
        val result = EjercicioResueltoJsonParser.parse(wrapped)
        assertTrue(result.isSuccess)
    }

    @Test
    fun repararUnaVez_quitaComaFinal() {
        val broken = """{"materia":"fisica","tema":"x","enunciado":"e","datos":[],"incognita":"i","formula":"f","pasos":[{"numero":1,"titulo":"a","explicacion":"b","operacion":"c"},],"resultado":{"valor":1,"unidad":"m"},"confianza":"media",}"""
        val repaired = EjercicioResueltoJsonParser.repararUnaVez(broken)
        val result = EjercicioResueltoJsonParser.parse(repaired)
        assertTrue(result.isSuccess)
    }

    @Test
    fun parse_basura_falla() {
        val result = EjercicioResueltoJsonParser.parse("esto no es json")
        assertTrue(result.isFailure)
    }

    @Test
    fun codec_roundTrip() {
        val datos = listOf(EjercicioDato("d", 10.0, "m"))
        val encoded = EjercicioResueltoJsonCodec.encodeDatos(datos)
        val decoded = EjercicioResueltoJsonCodec.decodeDatos(encoded)
        assertEquals(1, decoded.size)
        assertEquals("d", decoded[0].nombre)
        assertEquals(10.0, decoded[0].valor, 0.0)
    }
}

class EjercicioLocalVerifierTest {

    private val verifier = EjercicioLocalVerifier(DefaultFisicaEngine())

    @Test
    fun coincide_cuandoMotorLocalIgual() {
        val v = verifier.verificar(
            materia = MateriaEjercicio.FISICA,
            formula = "v = d / t",
            datos = listOf(
                EjercicioDato("d", 100.0, "m"),
                EjercicioDato("t", 20.0, "s")
            ),
            resultadoGemini = EjercicioResultadoValor(5.0, "m/s")
        )
        assertEquals(VerificacionEjercicio.COINCIDE, v)
    }

    @Test
    fun noCoincide_cuandoDifiere() {
        val v = verifier.verificar(
            materia = MateriaEjercicio.FISICA,
            formula = "v = d / t",
            datos = listOf(
                EjercicioDato("d", 100.0, "m"),
                EjercicioDato("t", 20.0, "s")
            ),
            resultadoGemini = EjercicioResultadoValor(9.0, "m/s")
        )
        assertEquals(VerificacionEjercicio.NO_COINCIDE, v)
    }

    @Test
    fun sinVerificacion_formulaDesconocida() {
        val v = verifier.verificar(
            materia = MateriaEjercicio.FISICA,
            formula = "E = mc^2",
            datos = listOf(EjercicioDato("m", 1.0, "kg")),
            resultadoGemini = EjercicioResultadoValor(1.0, "J")
        )
        assertEquals(VerificacionEjercicio.SIN_VERIFICACION, v)
    }

    @Test
    fun sinVerificacion_biologia() {
        val v = verifier.verificar(
            materia = MateriaEjercicio.BIOLOGIA,
            formula = "v = d / t",
            datos = listOf(EjercicioDato("d", 1.0, "m"), EjercicioDato("t", 1.0, "s")),
            resultadoGemini = EjercicioResultadoValor(1.0, "m/s")
        )
        assertEquals(VerificacionEjercicio.SIN_VERIFICACION, v)
    }

    @Test
    fun mapFormula_vIgualDt() {
        assertEquals(FormulaId.V_IGUAL_D_T, verifier.mapFormula("v=d/t"))
        assertNotNull(verifier.mapFormula("movimiento.vIgualDT"))
    }
}
