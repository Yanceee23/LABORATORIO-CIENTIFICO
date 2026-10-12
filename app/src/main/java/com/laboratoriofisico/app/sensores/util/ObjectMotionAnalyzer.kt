package com.laboratoriofisico.app.sensores.util

import kotlin.math.sqrt

data class ObjectMotionPoint(
    val timeSeconds: Double,
    val x: Double,
    val y: Double
)

data class ObjectMotionState(
    val elapsedSeconds: Double = 0.0,
    val initialX: Double = 0.0,
    val initialY: Double = 0.0,
    val currentX: Double = 0.0,
    val currentY: Double = 0.0,
    val displacementPx: Double = 0.0,
    val totalDistancePx: Double = 0.0,
    val currentSpeedPxPerSecond: Double = 0.0,
    val averageSpeedPxPerSecond: Double = 0.0,
    val maxSpeedPxPerSecond: Double = 0.0,
    val movementCount: Int = 0,
    val isMoving: Boolean = false,
    val path: List<ObjectMotionPoint> = emptyList()
)

/**
 * Métricas en vivo a partir de posiciones reales de cada frame.
 *
 * Suaviza la posición para reducir jitter, ignora pasos muy pequeños y calcula la velocidad
 * sobre una ventana corta para que el estado "Quieto/En movimiento" no vibre frame a frame.
 */
class ObjectMotionAnalyzer(
    private val smoothingAlpha: Double = 0.35,
    private val minStepPx: Double = 2.0,
    private val speedWindowSeconds: Double = 0.5,
    private val movingSpeedThresholdPxPerSecond: Double = 8.0,
    private val maxPathPoints: Int = 300
) {
    private var startTimestampMs: Long? = null
    private var initialX = 0.0
    private var initialY = 0.0
    private var currentX = 0.0
    private var currentY = 0.0
    private var smoothX = 0.0
    private var smoothY = 0.0
    private var totalDistancePx = 0.0
    private var maxSpeedPxPerSecond = 0.0
    private var movementCount = 0
    private var wasMoving = false
    private val path = ArrayDeque<ObjectMotionPoint>()

    fun reset() {
        startTimestampMs = null
        initialX = 0.0
        initialY = 0.0
        currentX = 0.0
        currentY = 0.0
        smoothX = 0.0
        smoothY = 0.0
        totalDistancePx = 0.0
        maxSpeedPxPerSecond = 0.0
        movementCount = 0
        wasMoving = false
        path.clear()
    }

    fun select(timestampMs: Long, x: Double, y: Double): ObjectMotionState {
        reset()
        startTimestampMs = timestampMs
        initialX = x
        initialY = y
        currentX = x
        currentY = y
        smoothX = x
        smoothY = y
        path.addLast(ObjectMotionPoint(0.0, x, y))
        return currentState(elapsedSeconds = 0.0)
    }

    fun update(timestampMs: Long, rawX: Double, rawY: Double): ObjectMotionState {
        val start = startTimestampMs ?: return select(timestampMs, rawX, rawY)
        val elapsed = ((timestampMs - start).coerceAtLeast(0L)) / 1000.0

        smoothX += smoothingAlpha * (rawX - smoothX)
        smoothY += smoothingAlpha * (rawY - smoothY)

        val step = distance(currentX, currentY, smoothX, smoothY)
        if (step >= minStepPx) {
            totalDistancePx += step
            currentX = smoothX
            currentY = smoothY
        }

        path.addLast(ObjectMotionPoint(elapsed, currentX, currentY))
        while (path.size > maxPathPoints) path.removeFirst()

        val currentSpeed = speedInWindow(elapsed)
        maxSpeedPxPerSecond = maxOf(maxSpeedPxPerSecond, currentSpeed)
        val isMoving = currentSpeed >= movingSpeedThresholdPxPerSecond
        if (isMoving && !wasMoving) movementCount++
        wasMoving = isMoving

        return currentState(
            elapsedSeconds = elapsed,
            currentSpeedPxPerSecond = currentSpeed,
            isMoving = isMoving
        )
    }

    private fun currentState(
        elapsedSeconds: Double,
        currentSpeedPxPerSecond: Double = 0.0,
        isMoving: Boolean = false
    ): ObjectMotionState {
        val displacement = distance(initialX, initialY, currentX, currentY)
        val averageSpeed = if (elapsedSeconds > 0.0) totalDistancePx / elapsedSeconds else 0.0
        return ObjectMotionState(
            elapsedSeconds = elapsedSeconds,
            initialX = initialX,
            initialY = initialY,
            currentX = currentX,
            currentY = currentY,
            displacementPx = displacement,
            totalDistancePx = totalDistancePx,
            currentSpeedPxPerSecond = currentSpeedPxPerSecond,
            averageSpeedPxPerSecond = averageSpeed,
            maxSpeedPxPerSecond = maxSpeedPxPerSecond,
            movementCount = movementCount,
            isMoving = isMoving,
            path = path.toList()
        )
    }

    private fun speedInWindow(elapsedSeconds: Double): Double {
        if (path.size < 2) return 0.0
        val windowStart = elapsedSeconds - speedWindowSeconds
        val window = path.filter { it.timeSeconds >= windowStart }
        if (window.size < 2) return 0.0
        val dt = window.last().timeSeconds - window.first().timeSeconds
        if (dt <= 0.0) return 0.0
        var distance = 0.0
        for (index in 1 until window.size) {
            distance += distance(
                window[index - 1].x,
                window[index - 1].y,
                window[index].x,
                window[index].y
            )
        }
        return distance / dt
    }

    private fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
}
