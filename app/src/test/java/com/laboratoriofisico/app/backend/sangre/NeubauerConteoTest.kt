package com.laboratoriofisico.app.backend.sangre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NeubauerConteoTest {

    @Test
    fun rojos_multiplicaPorDiezMil() {
        assertEquals(50_000.0, NeubauerConteo.calcular(TipoConteoNeubauer.ROJOS, 5)!!, 1e-9)
    }

    @Test
    fun blancos_multiplicaPorCincuenta() {
        assertEquals(400.0, NeubauerConteo.calcular(TipoConteoNeubauer.BLANCOS, 8)!!, 1e-9)
    }

    @Test
    fun otraDilucion_usaFormulaGeneral() {
        assertEquals(2_500.0, NeubauerConteo.calcular(TipoConteoNeubauer.OTRA, 10, 50.0, 0.2)!!, 1e-9)
    }

    @Test
    fun noCalculaSiFaltanDatos() {
        assertNull(NeubauerConteo.calcular(TipoConteoNeubauer.ROJOS, 0))
        assertNull(NeubauerConteo.calcular(TipoConteoNeubauer.OTRA, 10, dilucion = 20.0))
        assertNull(NeubauerConteo.calcular(TipoConteoNeubauer.OTRA, 10, volumenMicrolitros = 0.4))
    }
}
