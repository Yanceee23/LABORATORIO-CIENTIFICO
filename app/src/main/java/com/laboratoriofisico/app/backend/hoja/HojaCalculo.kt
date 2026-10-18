package com.laboratoriofisico.app.backend.hoja

import com.laboratoriofisico.app.backend.piel.PielEscamasCalculo

object HojaCalculo {
    fun indiceEstomatico(estomas: Int, celulasEpidermicas: Int): Double? {
        if (estomas < 0 || celulasEpidermicas < 0) return null
        val suma = estomas + celulasEpidermicas
        if (suma <= 0) return null
        return estomas * 100.0 / suma
    }

    fun enteroNoNegativo(texto: String): Int? {
        if (texto.isBlank()) return null
        return texto.trim().toIntOrNull()?.takeIf { it >= 0 }
    }
}

object HojaCalculoNotas {
    const val MARCA = "\n\n[[calculo-local-hoja]]\n"

    fun descripcionIa(analisis: String): String = analisis.substringBefore(MARCA)

    fun bloque(analisis: String): String? =
        analisis.substringAfter(MARCA, missingDelimiterValue = "").trim().ifBlank { null }

    fun unir(analisis: String, bloque: String): String =
        descripcionIa(analisis).trimEnd() + MARCA + bloque.trim()

    fun leer(analisis: String): MedidasHoja {
        val texto = bloque(analisis) ?: return MedidasHoja()
        return MedidasHoja(
            distanciaRealUm = valor(texto, "Distancia real:"),
            pixelesCalibracion = valor(texto, "Píxeles de calibración:"),
            estomas = valor(texto, "Estomas:")?.toInt(),
            celulasEpidermicas = valor(texto, "Células epidérmicas:")?.toInt(),
            pixelesEstoma = valor(texto, "Píxeles del estoma:"),
            pixelesCelula = valor(texto, "Píxeles de la célula:"),
            pixelesLargo = valor(texto, "Píxeles del largo:"),
            pixelesAncho = valor(texto, "Píxeles del ancho:")
        )
    }

    fun informe(medidas: MedidasHoja, umPorPixel: Double?): String? {
        val indice = if (medidas.estomas != null && medidas.celulasEpidermicas != null) {
            HojaCalculo.indiceEstomatico(medidas.estomas, medidas.celulasEpidermicas)
        } else {
            null
        }
        val estoma = if (umPorPixel != null && medidas.pixelesEstoma != null) {
            PielEscamasCalculo.tamanoUm(medidas.pixelesEstoma, umPorPixel)
        } else {
            null
        }
        val celula = if (umPorPixel != null && medidas.pixelesCelula != null) {
            PielEscamasCalculo.tamanoUm(medidas.pixelesCelula, umPorPixel)
        } else {
            null
        }
        val relacion = if (medidas.pixelesLargo != null && medidas.pixelesAncho != null) {
            PielEscamasCalculo.relacionLargoAncho(medidas.pixelesLargo, medidas.pixelesAncho)
        } else {
            null
        }
        if (umPorPixel == null && indice == null && relacion == null && estoma == null && celula == null) {
            return null
        }
        return buildString {
            appendLine("Cálculo educativo, no es un dato de la salud de la planta")
            if (umPorPixel != null) {
                appendLine("µm por píxel: ${PielEscamasCalculo.texto(umPorPixel)}")
                medidas.distanciaRealUm?.let { appendLine("Distancia real: ${PielEscamasCalculo.texto(it)} µm") }
                medidas.pixelesCalibracion?.let {
                    appendLine("Píxeles de calibración: ${PielEscamasCalculo.texto(it)}")
                }
            }
            if (indice != null && medidas.estomas != null && medidas.celulasEpidermicas != null) {
                appendLine("Estomas: ${medidas.estomas}")
                appendLine("Células epidérmicas: ${medidas.celulasEpidermicas}")
                appendLine("Índice estomático: ${PielEscamasCalculo.texto(indice)} %")
            }
            medidas.pixelesEstoma?.let { appendLine("Píxeles del estoma: ${PielEscamasCalculo.texto(it)}") }
            estoma?.let { appendLine("Tamaño del estoma: ${PielEscamasCalculo.texto(it)} µm") }
            medidas.pixelesCelula?.let { appendLine("Píxeles de la célula: ${PielEscamasCalculo.texto(it)}") }
            celula?.let { appendLine("Tamaño de la célula: ${PielEscamasCalculo.texto(it)} µm") }
            medidas.pixelesLargo?.let { appendLine("Píxeles del largo: ${PielEscamasCalculo.texto(it)}") }
            medidas.pixelesAncho?.let { appendLine("Píxeles del ancho: ${PielEscamasCalculo.texto(it)}") }
            relacion?.let { appendLine("Forma de la célula: ${PielEscamasCalculo.texto(it)}") }
        }.trimEnd()
    }

    private fun valor(texto: String, prefijo: String): Double? =
        texto.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith(prefijo) }
            ?.removePrefix(prefijo)
            ?.trim()
            ?.substringBefore(' ')
            ?.replace(',', '.')
            ?.toDoubleOrNull()
            ?.takeIf { it >= 0.0 }
}

data class MedidasHoja(
    val distanciaRealUm: Double? = null,
    val pixelesCalibracion: Double? = null,
    val estomas: Int? = null,
    val celulasEpidermicas: Int? = null,
    val pixelesEstoma: Double? = null,
    val pixelesCelula: Double? = null,
    val pixelesLargo: Double? = null,
    val pixelesAncho: Double? = null
)
