package com.laboratoriofisico.app.backend.hoja

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HojaCalculoTest {

    @Test
    fun indiceEstomaticoUsaEnterosNoNegativos() {
        assertEquals(25.0, HojaCalculo.indiceEstomatico(10, 30)!!, 0.0001)
        assertEquals(0.0, HojaCalculo.indiceEstomatico(0, 8)!!, 0.0001)
        assertNull(HojaCalculo.indiceEstomatico(0, 0))
        assertNull(HojaCalculo.indiceEstomatico(-1, 4))
        assertEquals(0, HojaCalculo.enteroNoNegativo("0"))
        assertNull(HojaCalculo.enteroNoNegativo(""))
    }

    @Test
    fun laDescripcionDeLaIaQuedaIgual() {
        val ia = "## Observaciones visuales\nSe ven estomas."
        val unido = HojaCalculoNotas.unir(ia, "Índice estomático: 25 %")
        assertEquals(ia, HojaCalculoNotas.descripcionIa(unido))
        assertEquals("Índice estomático: 25 %", HojaCalculoNotas.bloque(unido))
    }
}
