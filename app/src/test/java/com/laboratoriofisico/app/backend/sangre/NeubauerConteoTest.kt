package com.laboratoriofisico.app.backend.sangre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NeubauerConteoTest {

    @Test
    fun rojos_multiplicaPorDiezMil() {
        assertEquals(50_000.0, NeubauerConteo.rojos(5)!!, 1e-9)
    }

    @Test
    fun blancos_multiplicaPorCincuenta() {
        assertEquals(400.0, NeubauerConteo.blancos(8)!!, 1e-9)
    }

    @Test
    fun general_dividePorAreaYProfundidad() {
        assertEquals(100_000.0, NeubauerConteo.general(10, 200.0, 0.2)!!, 1e-9)
    }

    @Test
    fun noCalculaSiFaltanDatos() {
        assertNull(NeubauerConteo.rojos(0))
        assertNull(NeubauerConteo.general(10, 20.0, 0.0))
        assertNull(NeubauerConteo.general(10, 0.0, 0.4))
    }
}
