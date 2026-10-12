package com.laboratoriofisico.app.fisica

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.sqrt

class FisicaEngineTest {

    private val engine: FisicaEngine = DefaultFisicaEngine()

    @Test
    fun vIgualDT_calculaVelocidad() {
        val result = engine.calcular(
            FormulaId.V_IGUAL_D_T.id,
            mapOf("d" to 100.0, "t" to 4.0)
        )
        assertEquals(25.0, result!!, 1e-9)
    }

    @Test
    fun vIgualDT_divisionPorCero_retornaNull() {
        assertNull(engine.calcular(FormulaId.V_IGUAL_D_T.id, mapOf("d" to 10.0, "t" to 0.0)))
    }

    @Test
    fun aIgualDeltaVDeltaT_calculaAceleracion() {
        val result = engine.calcular(
            FormulaId.A_IGUAL_DELTA_V_DELTA_T.id,
            mapOf("dv" to 20.0, "dt" to 2.0)
        )
        assertEquals(10.0, result!!, 1e-9)
    }

    @Test
    fun dIgualVT_calculaDistancia() {
        val result = engine.calcular(
            FormulaId.D_IGUAL_V_T.id,
            mapOf("v" to 5.0, "t" to 3.0)
        )
        assertEquals(15.0, result!!, 1e-9)
    }

    @Test
    fun fIgual1SobreT_calculaFrecuencia() {
        val result = engine.calcular(
            FormulaId.F_IGUAL_1_SOBRE_T.id,
            mapOf("T" to 0.5)
        )
        assertEquals(2.0, result!!, 1e-9)
    }

    @Test
    fun tIgual1SobreF_calculaPeriodo() {
        val result = engine.calcular(
            FormulaId.T_IGUAL_1_SOBRE_F.id,
            mapOf("f" to 50.0)
        )
        assertEquals(0.02, result!!, 1e-9)
    }

    @Test
    fun vIgualLambdaF_calculaVelocidadOnda() {
        val result = engine.calcular(
            FormulaId.V_IGUAL_LAMBDA_F.id,
            mapOf("l" to 2.0, "f" to 440.0)
        )
        assertEquals(880.0, result!!, 1e-9)
    }

    @Test
    fun gPendulo_calculaGravedad() {
        val result = engine.calcular(
            FormulaId.G_PENDULO.id,
            mapOf("L" to 1.0, "T" to 2.0)
        )
        assertEquals(Math.PI * Math.PI, result!!, 1e-6)
    }

    @Test
    fun bIntensidad_calculaMagnitud() {
        val result = engine.calcular(
            FormulaId.B_INTENSIDAD.id,
            mapOf("Bx" to 3.0, "By" to 4.0, "Bz" to 0.0)
        )
        assertEquals(5.0, result!!, 1e-9)
    }

    @Test
    fun bIntensidad_coincideConFuncionTipada() {
        val bx = 12.0
        val by = -5.0
        val bz = 8.0
        val expected = sqrt(bx * bx + by * by + bz * bz)
        val result = engine.calcular(
            FormulaId.B_INTENSIDAD.id,
            mapOf("Bx" to bx, "By" to by, "Bz" to bz)
        )
        assertEquals(expected, result!!, 1e-9)
    }

    @Test
    fun thetaIncidenciaReflexion_devuelveDeltaAngular() {
        val result = engine.calcular(
            FormulaId.THETA_INCIDENCIA_REFLEXION.id,
            mapOf("theta_i" to 30.0, "theta_r" to 30.0)
        )
        assertEquals(0.0, result!!, 1e-9)
    }

    @Test
    fun formulaIdDesconocido_retornaNull() {
        assertNull(engine.calcular("desconocida", mapOf("x" to 1.0)))
    }
}
