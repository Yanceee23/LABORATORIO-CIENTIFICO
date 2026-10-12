package com.laboratoriofisico.app.sensores.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObjectTrackerTest {

    private val width = 80
    private val height = 60

    /** Construye un frame con un cuadrado claro sobre fondo con textura suave. */
    private fun frameWithSquare(cx: Int, cy: Int, timestampMs: Long = 0L): LumaFrame {
        val luma = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                luma[y * width + x] = 40 + ((x * 3 + y * 2) % 11)
            }
        }
        for (y in (cy - 2)..(cy + 2)) {
            for (x in (cx - 2)..(cx + 2)) {
                if (x in 0 until width && y in 0 until height) {
                    luma[y * width + x] = 230
                }
            }
        }
        return LumaFrame(luma, width, height, width * 4, height * 4, timestampMs)
    }

    @Test
    fun tracksObjectAcrossFrames() {
        val tracker = ObjectTracker()
        val selection = tracker.selectTarget(frameWithSquare(20, 30), 20, 30)
        assertEquals(TrackingOutcome.TRACKED, selection.outcome)

        val moved = tracker.track(frameWithSquare(28, 34, timestampMs = 100L))
        assertEquals(TrackingOutcome.TRACKED, moved.outcome)
        val position = moved.position
        assertNotNull(position)
        assertEquals(28.0, position!!.x.toDouble(), 2.0)
        assertEquals(34.0, position.y.toDouble(), 2.0)
        assertTrue("Confianza debe ser alta", position.confidence > 0.5)
    }

    @Test
    fun reportsLostWhenObjectDisappears() {
        val tracker = ObjectTracker()
        tracker.selectTarget(frameWithSquare(30, 30), 30, 30)

        val emptyFrame = LumaFrame(
            luma = IntArray(width * height) { 200 },
            width = width,
            height = height,
            sourceWidth = width * 4,
            sourceHeight = height * 4,
            timestampMs = 200L
        )
        val result = tracker.track(emptyFrame)
        assertEquals(TrackingOutcome.LOST, result.outcome)
        assertEquals(null, result.position)
    }

    @Test
    fun rejectsTargetWithoutContrast() {
        val tracker = ObjectTracker()
        val flatFrame = LumaFrame(
            luma = IntArray(width * height) { 128 },
            width = width,
            height = height,
            sourceWidth = width,
            sourceHeight = height,
            timestampMs = 0L
        )
        val result = tracker.selectTarget(flatFrame, 40, 30)
        assertEquals(TrackingOutcome.INSUFFICIENT_CONTRAST, result.outcome)
        assertTrue("No debe quedar objetivo fijado", !tracker.hasTarget)
    }

    @Test
    fun survivesGlobalIlluminationChange() {
        val tracker = ObjectTracker()
        assertEquals(
            TrackingOutcome.TRACKED,
            tracker.selectTarget(frameWithSquare(30, 30), 30, 30).outcome
        )

        // Mismo contenido, toda la escena 45 niveles más oscura (exposición automática).
        val darker = frameWithSquare(34, 30, timestampMs = 100L)
        for (i in darker.luma.indices) {
            darker.luma[i] = (darker.luma[i] - 45).coerceAtLeast(0)
        }
        val result = tracker.track(darker)
        assertEquals(TrackingOutcome.TRACKED, result.outcome)
        assertEquals(34.0, result.position!!.x.toDouble(), 2.0)
    }

    @Test
    fun followsFastObjectUsingMotionPrediction() {
        val tracker = ObjectTracker()
        tracker.selectTarget(frameWithSquare(15, 30), 15, 30)

        var x = 15
        var lastOutcome = TrackingOutcome.IDLE
        repeat(4) { step ->
            x += 14
            lastOutcome = tracker.track(frameWithSquare(x, 30, timestampMs = 100L * step)).outcome
        }
        assertEquals("Debe seguir un objeto rápido con predicción", TrackingOutcome.TRACKED, lastOutcome)
    }

    @Test
    fun trackWithoutTargetIsIdle() {
        val tracker = ObjectTracker()
        val result = tracker.track(frameWithSquare(30, 30))
        assertEquals(TrackingOutcome.IDLE, result.outcome)
    }
}
