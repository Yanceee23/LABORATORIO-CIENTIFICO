package com.laboratoriofisico.app.backend.piel

import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.roundToInt

object PielEscamasCalculo {
    fun distanciaPixeles(x1: Float, y1: Float, x2: Float, y2: Float): Double? {
        val pixeles = hypot((x1 - x2).toDouble(), (y1 - y2).toDouble())
        return pixeles.takeIf { it > 0.0 }
    }

    fun umPorPixel(distanciaRealUm: Double, pixeles: Double): Double? {
        if (distanciaRealUm <= 0.0 || pixeles <= 0.0) return null
        return distanciaRealUm / pixeles
    }

    fun diametroCampoMm(anchoImagenPx: Int, umPorPixel: Double): Double? {
        if (anchoImagenPx <= 0 || umPorPixel <= 0.0) return null
        return (anchoImagenPx * umPorPixel) / 1000.0
    }

    fun areaCampoMm2(diametroMm: Double): Double? {
        if (diametroMm <= 0.0) return null
        val radio = diametroMm / 2.0
        return PI * radio * radio
    }

    fun escamasPorMm2(n: Int, areaMm2: Double): Double? {
        if (n <= 0 || areaMm2 <= 0.0) return null
        return n / areaMm2
    }

    fun tamanoUm(pixeles: Double, umPorPixel: Double): Double? {
        if (pixeles <= 0.0 || umPorPixel <= 0.0) return null
        return pixeles * umPorPixel
    }

    fun relacionLargoAncho(largoUm: Double, anchoUm: Double): Double? {
        if (largoUm <= 0.0 || anchoUm <= 0.0) return null
        return largoUm / anchoUm
    }

    fun numero(texto: String): Double? =
        texto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }

    fun entero(texto: String): Int? =
        texto.trim().toIntOrNull()?.takeIf { it > 0 }

    fun texto(valor: Double): String {
        val redondo = valor.roundToInt()
        if (kotlin.math.abs(valor - redondo) < 0.001) return redondo.toString()
        val crudo = ((valor * 100.0).roundToInt() / 100.0).toString()
        val partes = crudo.split('.')
        val decimales = partes.getOrElse(1) { "0" }.trimEnd('0').ifEmpty { "0" }
        return if (decimales == "0") partes[0] else "${partes[0]},$decimales"
    }
}

object PielCalculoNotas {
    const val MARCA = "\n\n[[calculo-local-piel]]\n"

    fun descripcionIa(analisis: String): String = analisis.substringBefore(MARCA)

    fun bloque(analisis: String): String? =
        analisis.substringAfter(MARCA, missingDelimiterValue = "").trim().ifBlank { null }

    fun unir(analisis: String, bloque: String): String =
        descripcionIa(analisis).trimEnd() + MARCA + bloque.trim()

    fun leer(analisis: String): MedidasGuardadas {
        val texto = bloque(analisis) ?: return MedidasGuardadas()
        return MedidasGuardadas(
            distanciaRealUm = valor(texto, "Distancia real:"),
            pixelesCalibracion = valor(texto, "Píxeles de calibración:"),
            anchoImagenPx = valor(texto, "Ancho de la imagen:")?.toInt() ?: 0,
            n = valor(texto, "N escamas:")?.toInt()?.takeIf { it > 0 } ?: 0,
            pixelesEscama = valor(texto, "Píxeles de la escama:"),
            pixelesLargo = valor(texto, "Píxeles del largo:"),
            pixelesAncho = valor(texto, "Píxeles del ancho:")
        )
    }

    fun informe(medidas: MedidasGuardadas, umPorPixel: Double): String = buildString {
        val diametro = PielEscamasCalculo.diametroCampoMm(medidas.anchoImagenPx, umPorPixel)
        val area = diametro?.let(PielEscamasCalculo::areaCampoMm2)
        val densidad = area?.let { PielEscamasCalculo.escamasPorMm2(medidas.n, it) }
        val escama = medidas.pixelesEscama?.let { PielEscamasCalculo.tamanoUm(it, umPorPixel) }
        val largo = medidas.pixelesLargo?.let { PielEscamasCalculo.tamanoUm(it, umPorPixel) }
        val ancho = medidas.pixelesAncho?.let { PielEscamasCalculo.tamanoUm(it, umPorPixel) }
        val relacion = if (largo != null && ancho != null) {
            PielEscamasCalculo.relacionLargoAncho(largo, ancho)
        } else {
            null
        }
        appendLine("Cálculo educativo, no es un dato de salud de la piel")
        appendLine("µm por píxel: ${PielEscamasCalculo.texto(umPorPixel)}")
        medidas.distanciaRealUm?.let { appendLine("Distancia real: ${PielEscamasCalculo.texto(it)} µm") }
        medidas.pixelesCalibracion?.let { appendLine("Píxeles de calibración: ${PielEscamasCalculo.texto(it)}") }
        if (medidas.anchoImagenPx > 0) appendLine("Ancho de la imagen: ${medidas.anchoImagenPx} px")
        diametro?.let { appendLine("Diámetro del campo: ${PielEscamasCalculo.texto(it)} mm") }
        area?.let { appendLine("Área del campo: ${PielEscamasCalculo.texto(it)} mm²") }
        if (medidas.n > 0) appendLine("N escamas: ${medidas.n}")
        densidad?.let { appendLine("Escamas por mm²: ${PielEscamasCalculo.texto(it)}") }
        medidas.pixelesEscama?.let { appendLine("Píxeles de la escama: ${PielEscamasCalculo.texto(it)}") }
        escama?.let { appendLine("Tamaño de una escama: ${PielEscamasCalculo.texto(it)} µm") }
        medidas.pixelesLargo?.let { appendLine("Píxeles del largo: ${PielEscamasCalculo.texto(it)}") }
        largo?.let { appendLine("Largo: ${PielEscamasCalculo.texto(it)} µm") }
        medidas.pixelesAncho?.let { appendLine("Píxeles del ancho: ${PielEscamasCalculo.texto(it)}") }
        ancho?.let { appendLine("Ancho: ${PielEscamasCalculo.texto(it)} µm") }
        relacion?.let { appendLine("Relación largo/ancho: ${PielEscamasCalculo.texto(it)}") }
    }.trimEnd()

    private fun valor(texto: String, prefijo: String): Double? =
        texto.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith(prefijo) }
            ?.removePrefix(prefijo)
            ?.trim()
            ?.substringBefore(' ')
            ?.let(PielEscamasCalculo::numero)
}

data class MedidasGuardadas(
    val distanciaRealUm: Double? = null,
    val pixelesCalibracion: Double? = null,
    val anchoImagenPx: Int = 0,
    val n: Int = 0,
    val pixelesEscama: Double? = null,
    val pixelesLargo: Double? = null,
    val pixelesAncho: Double? = null
)
