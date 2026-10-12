package com.laboratoriofisico.app.sensores.util

import kotlin.math.ln
import kotlin.math.pow

/**
 * Estima lux a partir de un frame real de la cámara.
 *
 * La definición fotográfica dice que EV 0 equivale a 2,5 lux. El valor de exposición
 * sale del diafragma, el tiempo y la ISO que eligió la cámara en ese frame. El brillo
 * del pixel corrige el resultado cuando la imagen no quedó en el gris medio.
 */
object CameraSceneLux {
    const val EV0_LUX = 2.5

    /** Y aproximado al que apunta la exposición automática en un gris medio. */
    const val TARGET_LUMA = 90.0

    /** Diafragma de la cámara trasera del S20 FE si el frame no trae el valor. */
    const val FALLBACK_APERTURE = 1.8

    fun estimateLux(
        luma: Double,
        exposureTimeNs: Long,
        iso: Int,
        aperture: Double?
    ): Double? {
        if (exposureTimeNs <= 0L || iso <= 0) return null
        val n = aperture?.takeIf { it.isFinite() && it > 0.0 } ?: FALLBACK_APERTURE
        val seconds = exposureTimeNs / 1_000_000_000.0
        if (seconds <= 0.0) return null
        val ev100 = ln(n * n / seconds) / LN2 - ln(iso / 100.0) / LN2
        val midGrayLux = EV0_LUX * 2.0.pow(ev100)
        if (!midGrayLux.isFinite() || midGrayLux < 0.0) return null
        val linear = (luma.coerceIn(0.0, 255.0) / 255.0).pow(GAMMA)
        val targetLinear = (TARGET_LUMA / 255.0).pow(GAMMA)
        val factor = (linear / targetLinear).coerceIn(0.0, 12.0)
        val lux = midGrayLux * factor
        return lux.takeIf { it.isFinite() && it >= 0.0 }
    }

    private const val GAMMA = 2.2
    private val LN2 = ln(2.0)
}
