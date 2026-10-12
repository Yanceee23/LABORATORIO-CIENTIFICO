package com.laboratoriofisico.app.reportes

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ResultadosPeriodoTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun agrupaElDiaMasRecientePrimeroYLasHorasEnOrden() {
        val manana = utc(2026, Calendar.OCTOBER, 11, 9, 5)
        val noche = utc(2026, Calendar.OCTOBER, 11, 21, 30)
        val ayer = utc(2026, Calendar.OCTOBER, 10, 23, 10)

        val grupos = ResultadosPeriodo.agrupar(listOf(noche, manana, ayer), utc) { it }

        assertEquals(listOf("2026-10-11", "2026-10-10"), grupos.map { it.clave })
        assertEquals("Domingo 11 de octubre de 2026", grupos[0].etiqueta)
        assertEquals(listOf(9, 21), grupos[0].horas.map { it.hora })
        assertEquals("De las 09:00 a las 10:00", grupos[0].horas[0].etiqueta)
        assertEquals("De las 21:00 a las 22:00", grupos[0].horas[1].etiqueta)
        assertEquals(listOf(manana, noche), grupos[0].horas.flatMap { it.items })
        assertEquals("Sábado 10 de octubre de 2026", grupos[1].etiqueta)
    }

    @Test
    fun elTituloDelCatalogoSeLeeComoEnLaPantalla() {
        assertEquals("Cómo se mueve el teléfono", ResultadosPeriodo.tituloAmigable("Movimiento"))
        assertEquals("Observa una caída", ResultadosPeriodo.tituloAmigable("Estudiar una caída"))
        assertEquals("10,5", ResultadosPeriodo.formatValor(10.5))
        assertEquals("0,28", ResultadosPeriodo.formatValor(0.28))
    }

    private fun utc(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance(utc)
        calendar.set(year, month, day, hour, minute, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
