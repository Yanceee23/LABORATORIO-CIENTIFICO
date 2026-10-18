package com.laboratoriofisico.app.backend.piel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI

class PielEscamasCalculoTest {

    @Test
    fun calibracionYCampoUsanLaReglaYElAnchoDeLaFoto() {
        assertEquals(5.0, PielEscamasCalculo.distanciaPixeles(0f, 0f, 3f, 4f)!!, 0.0001)
        assertEquals(5.0, PielEscamasCalculo.umPorPixel(1000.0, 200.0)!!, 0.0001)
        assertEquals(3.2, PielEscamasCalculo.diametroCampoMm(640, 5.0)!!, 0.0001)
        assertEquals(PI * 1.6 * 1.6, PielEscamasCalculo.areaCampoMm2(3.2)!!, 0.0001)
    }

    @Test
    fun densidadTamanoYRelacionSalenDeLaCalibracion() {
        val area = PielEscamasCalculo.areaCampoMm2(3.2)!!
        assertEquals(10.0 / area, PielEscamasCalculo.escamasPorMm2(10, area)!!, 0.0001)
        assertEquals(200.0, PielEscamasCalculo.tamanoUm(40.0, 5.0)!!, 0.0001)
        assertEquals(2.5, PielEscamasCalculo.relacionLargoAncho(200.0, 80.0)!!, 0.0001)
    }

    @Test
    fun sinCalibracionNoHayNumeros() {
        assertNull(PielEscamasCalculo.umPorPixel(0.0, 200.0))
        assertNull(PielEscamasCalculo.umPorPixel(1000.0, 0.0))
        assertNull(PielEscamasCalculo.diametroCampoMm(640, 0.0))
        assertNull(PielEscamasCalculo.escamasPorMm2(0, 8.0))
        assertNull(PielEscamasCalculo.tamanoUm(40.0, 0.0))
        assertNull(PielEscamasCalculo.relacionLargoAncho(10.0, 0.0))
        assertNull(PielEscamasCalculo.distanciaPixeles(2f, 2f, 2f, 2f))
    }

    @Test
    fun elTextoDeLaIaQuedaIgualAlAgregarElCalculo() {
        val ia = "## Observaciones visuales\nLáminas irregulares."
        val unido = PielCalculoNotas.unir(ia, "µm por píxel: 5")
        assertEquals(ia, PielCalculoNotas.descripcionIa(unido))
        assertEquals("µm por píxel: 5", PielCalculoNotas.bloque(unido))
        assertEquals(unido, PielCalculoNotas.unir(unido, "µm por píxel: 5"))
    }
}
