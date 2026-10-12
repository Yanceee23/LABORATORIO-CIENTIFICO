package com.laboratoriofisico.app.p14pruebas

import com.laboratoriofisico.app.fisica.DefaultFisicaEngine
import com.laboratoriofisico.app.fisica.FormulaId
import com.laboratoriofisico.app.p05formulasfisicas.magnetismo.BIntensidad
import org.junit.Assert.assertEquals
import org.junit.Test

class FormulasTest {
    private val engine = DefaultFisicaEngine()

    @Test
    fun formulasMagnetismo_integradasConEngine() {
        val bx = 1.0
        val by = 2.0
        val bz = 2.0
        val directo = BIntensidad.calcular(bx, by, bz)
        val viaEngine = engine.calcular(
            FormulaId.B_INTENSIDAD.id,
            mapOf("Bx" to bx, "By" to by, "Bz" to bz)
        )
        assertEquals(directo, viaEngine!!, 1e-9)
    }
}
