package com.laboratoriofisico.app.sensores.util

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Frame en escala de grises ya reducido y rotado a orientación vertical.
 *
 * [width]/[height] son las dimensiones del buffer reducido con el que trabaja el rastreador;
 * [sourceWidth]/[sourceHeight] son las del frame original de la cámara, para poder reportar
 * posiciones en píxeles reales del sensor.
 */
class LumaFrame(
    val luma: IntArray,
    val width: Int,
    val height: Int,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val timestampMs: Long
) {
    fun at(x: Int, y: Int): Int = luma[y * width + x]

    val scaleToSourceX: Double get() = sourceWidth.toDouble() / width
    val scaleToSourceY: Double get() = sourceHeight.toDouble() / height
}

/** Posición del objeto en el espacio reducido del frame, con la confianza del emparejamiento. */
data class TrackedPosition(
    val x: Int,
    val y: Int,
    val confidence: Double
)

enum class TrackingOutcome {
    /** Aún no se ha seleccionado un objeto. */
    IDLE,

    /** Emparejamiento válido en este frame. */
    TRACKED,

    /** El objeto ya no se encuentra en la zona de búsqueda. */
    LOST,

    /** El objeto seleccionado no tiene suficiente contraste para seguirse. */
    INSUFFICIENT_CONTRAST
}

data class TrackingResult(
    val outcome: TrackingOutcome,
    val position: TrackedPosition? = null,
    /** Diferencia absoluta media del mejor emparejamiento; menor es mejor. */
    val matchScore: Double? = null
)

/**
 * Seguimiento de un objeto por correspondencia de plantilla sobre la luminancia real del frame.
 *
 * El usuario elige el objeto tocando la pantalla: se guarda el parche de luminancia alrededor
 * de ese punto y en cada frame se busca, dentro de una ventana alrededor de la última posición,
 * el desplazamiento que minimiza la diferencia absoluta media. No hay posiciones sintéticas:
 * si ningún candidato baja del umbral, se reporta [TrackingOutcome.LOST].
 */
class ObjectTracker(
    private val templateRadius: Int = 4,
    private val searchRadius: Int = 16,
    private val lostMeanDiff: Double = 34.0,
    private val minTemplateContrast: Double = 3.0,
    private val templateBlend: Double = 0.08
) {
    private var template: IntArray? = null
    private var templateMean = 0.0
    private var templateWidth = 0
    private var frameWidth = 0
    private var frameHeight = 0
    private var lastX = 0
    private var lastY = 0
    private var velocityX = 0.0
    private var velocityY = 0.0

    val hasTarget: Boolean get() = template != null

    fun reset() {
        template = null
        templateMean = 0.0
        templateWidth = 0
        frameWidth = 0
        frameHeight = 0
        velocityX = 0.0
        velocityY = 0.0
    }

    /**
     * Fija el objeto a seguir en torno a ([x], [y]) del frame reducido.
     * Devuelve `false` si el parche es demasiado uniforme para distinguirlo del fondo.
     */
    fun selectTarget(frame: LumaFrame, x: Int, y: Int): TrackingResult {
        val size = templateRadius * 2 + 1
        if (frame.width < size || frame.height < size) {
            return TrackingResult(TrackingOutcome.INSUFFICIENT_CONTRAST)
        }
        val cx = x.coerceIn(templateRadius, frame.width - templateRadius - 1)
        val cy = y.coerceIn(templateRadius, frame.height - templateRadius - 1)

        val patch = IntArray(size * size)
        var index = 0
        for (dy in -templateRadius..templateRadius) {
            for (dx in -templateRadius..templateRadius) {
                patch[index++] = frame.at(cx + dx, cy + dy)
            }
        }

        if (standardDeviation(patch) < minTemplateContrast) {
            reset()
            return TrackingResult(TrackingOutcome.INSUFFICIENT_CONTRAST)
        }

        template = patch
        templateMean = patch.average()
        templateWidth = size
        frameWidth = frame.width
        frameHeight = frame.height
        lastX = cx
        lastY = cy
        velocityX = 0.0
        velocityY = 0.0
        return TrackingResult(TrackingOutcome.TRACKED, TrackedPosition(cx, cy, 1.0))
    }

    fun track(frame: LumaFrame): TrackingResult {
        val patch = template ?: return TrackingResult(TrackingOutcome.IDLE)
        if (frame.width != frameWidth || frame.height != frameHeight) {
            reset()
            return TrackingResult(TrackingOutcome.LOST)
        }

        val minCenter = templateRadius
        val maxCenterX = frame.width - templateRadius - 1
        val maxCenterY = frame.height - templateRadius - 1

        // La ventana se centra en la posición predicha por la velocidad reciente: así se
        // sigue un objeto rápido sin ampliar el radio de búsqueda (y su coste cuadrático).
        val predictedX = (lastX + velocityX).roundToInt().coerceIn(minCenter, maxCenterX)
        val predictedY = (lastY + velocityY).roundToInt().coerceIn(minCenter, maxCenterY)

        var bestScore = Double.MAX_VALUE
        var bestX = lastX
        var bestY = lastY

        for (cy in (predictedY - searchRadius)..(predictedY + searchRadius)) {
            if (cy < minCenter || cy > maxCenterY) continue
            for (cx in (predictedX - searchRadius)..(predictedX + searchRadius)) {
                if (cx < minCenter || cx > maxCenterX) continue
                val score = meanAbsoluteDifference(frame, patch, cx, cy, bestScore)
                if (score < bestScore) {
                    bestScore = score
                    bestX = cx
                    bestY = cy
                }
            }
        }

        if (bestScore > lostMeanDiff) {
            velocityX = 0.0
            velocityY = 0.0
            return TrackingResult(TrackingOutcome.LOST, matchScore = bestScore)
        }

        val stepX = (bestX - lastX).toDouble()
        val stepY = (bestY - lastY).toDouble()
        velocityX += VELOCITY_ALPHA * (stepX - velocityX)
        velocityY += VELOCITY_ALPHA * (stepY - velocityY)
        lastX = bestX
        lastY = bestY
        blendTemplate(frame, patch, bestX, bestY)

        val confidence = (1.0 - bestScore / lostMeanDiff).coerceIn(0.0, 1.0)
        return TrackingResult(
            TrackingOutcome.TRACKED,
            TrackedPosition(bestX, bestY, confidence),
            matchScore = bestScore
        )
    }

    /**
     * Diferencia absoluta media entre plantilla y candidato, restando a cada uno su media.
     *
     * Al comparar sólo la textura y no el brillo absoluto, el emparejamiento sobrevive a los
     * cambios de exposición automática de la cámara, que son la causa habitual de perder un
     * objeto que sigue perfectamente visible.
     */
    private fun meanAbsoluteDifference(
        frame: LumaFrame,
        patch: IntArray,
        cx: Int,
        cy: Int,
        ceiling: Double
    ): Double {
        val size = templateWidth
        var candidateSum = 0L
        for (dy in -templateRadius..templateRadius) {
            val row = (cy + dy) * frame.width
            for (dx in -templateRadius..templateRadius) {
                candidateSum += frame.luma[row + cx + dx]
            }
        }
        val candidateMean = candidateSum.toDouble() / (size * size)
        val meanOffset = candidateMean - templateMean

        val limit = if (ceiling == Double.MAX_VALUE) Double.MAX_VALUE else ceiling * patch.size
        var sum = 0.0
        var index = 0
        for (dy in -templateRadius..templateRadius) {
            val row = (cy + dy) * frame.width
            for (dx in -templateRadius..templateRadius) {
                sum += abs(frame.luma[row + cx + dx] - patch[index++] - meanOffset)
            }
            if (sum > limit) return Double.MAX_VALUE
        }
        return sum / (size * size)
    }

    /** Adaptación lenta de la plantilla para tolerar cambios de aspecto sin derivar. */
    private fun blendTemplate(frame: LumaFrame, patch: IntArray, cx: Int, cy: Int) {
        var index = 0
        for (dy in -templateRadius..templateRadius) {
            for (dx in -templateRadius..templateRadius) {
                val observed = frame.at(cx + dx, cy + dy)
                patch[index] = (patch[index] * (1 - templateBlend) + observed * templateBlend)
                    .roundToInt()
                index++
            }
        }
        templateMean = patch.average()
    }

    private companion object {
        /** Suavizado de la velocidad estimada, en celdas por frame. */
        const val VELOCITY_ALPHA = 0.5
    }

    private fun standardDeviation(values: IntArray): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.sum().toDouble() / values.size
        var acc = 0.0
        values.forEach { acc += (it - mean) * (it - mean) }
        return sqrt(acc / values.size)
    }
}
