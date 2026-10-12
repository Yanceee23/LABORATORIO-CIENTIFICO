package com.laboratoriofisico.app.sensores.util

/**
 * Calibración píxel→centímetro. Mientras no exista, las distancias se reportan en píxeles.
 *
 * Se obtiene midiendo un objeto de tamaño conocido: si un objeto de [referenceSizeCm]
 * ocupa [observedPixels] píxeles en la imagen, la escala es `observedPixels / referenceSizeCm`.
 */
data class PixelCalibration(
    val referenceSizeCm: Double,
    val observedPixels: Double
) {
    val pixelsPerCm: Double = observedPixels / referenceSizeCm

    val isValid: Boolean = referenceSizeCm > 0.0 && observedPixels > 0.0 && pixelsPerCm.isFinite()
}

object DistanceCalibration {

    /**
     * Convierte píxeles a centímetros solo si hay calibración válida.
     * Devuelve `null` en caso contrario: no se infiere una escala física sin medirla.
     */
    fun toCentimeters(pixels: Double, calibration: PixelCalibration?): Double? {
        val cal = calibration ?: return null
        if (!cal.isValid) return null
        return pixels / cal.pixelsPerCm
    }
}
