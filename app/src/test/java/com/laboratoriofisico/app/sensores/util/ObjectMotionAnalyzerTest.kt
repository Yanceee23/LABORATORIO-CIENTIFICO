package com.laboratoriofisico.app.sensores.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObjectMotionAnalyzerTest {

    @Test
    fun displacementUsesCurrentPositionFromInitialPoint() {
        val analyzer = ObjectMotionAnalyzer(smoothingAlpha = 1.0, minStepPx = 0.0)

        analyzer.select(timestampMs = 0L, x = 0.0, y = 0.0)
        val state = analyzer.update(timestampMs = 1_000L, rawX = 3.0, rawY = 4.0)

        assertEquals(5.0, state.displacementPx, 0.001)
        assertEquals(3.0, state.currentX, 0.001)
        assertEquals(4.0, state.currentY, 0.001)
    }

    @Test
    fun totalDistanceAccumulatesSegments() {
        val analyzer = ObjectMotionAnalyzer(smoothingAlpha = 1.0, minStepPx = 0.0)

        analyzer.select(timestampMs = 0L, x = 0.0, y = 0.0)
        analyzer.update(timestampMs = 1_000L, rawX = 3.0, rawY = 4.0)
        val state = analyzer.update(timestampMs = 2_000L, rawX = 6.0, rawY = 8.0)

        assertEquals(10.0, state.totalDistancePx, 0.001)
        assertEquals(5.0, state.averageSpeedPxPerSecond, 0.001)
    }

    @Test
    fun speedUsesShortWindowAndFallsWhenStopped() {
        val analyzer = ObjectMotionAnalyzer(
            smoothingAlpha = 1.0,
            minStepPx = 0.0,
            speedWindowSeconds = 0.5
        )

        analyzer.select(timestampMs = 0L, x = 0.0, y = 0.0)
        analyzer.update(timestampMs = 250L, rawX = 10.0, rawY = 0.0)
        val moving = analyzer.update(timestampMs = 500L, rawX = 20.0, rawY = 0.0)
        val stopped = analyzer.update(timestampMs = 1_000L, rawX = 20.0, rawY = 0.0)

        assertEquals(40.0, moving.currentSpeedPxPerSecond, 0.001)
        assertEquals(0.0, stopped.currentSpeedPxPerSecond, 0.001)
    }

    @Test
    fun movementCountIncrementsOnMovingTransitions() {
        val analyzer = ObjectMotionAnalyzer(
            smoothingAlpha = 1.0,
            minStepPx = 0.0,
            speedWindowSeconds = 0.3,
            movingSpeedThresholdPxPerSecond = 5.0
        )

        analyzer.select(timestampMs = 0L, x = 0.0, y = 0.0)
        val firstMove = analyzer.update(timestampMs = 100L, rawX = 2.0, rawY = 0.0)
        val stopped = analyzer.update(timestampMs = 500L, rawX = 2.0, rawY = 0.0)
        val secondMove = analyzer.update(timestampMs = 600L, rawX = 4.0, rawY = 0.0)

        assertTrue(firstMove.isMoving)
        assertEquals(1, firstMove.movementCount)
        assertFalse(stopped.isMoving)
        assertTrue(secondMove.isMoving)
        assertEquals(2, secondMove.movementCount)
    }

    @Test
    fun ignoresSmallVibrationsUnderThreshold() {
        val analyzer = ObjectMotionAnalyzer(
            smoothingAlpha = 1.0,
            minStepPx = 3.0,
            movingSpeedThresholdPxPerSecond = 5.0
        )

        analyzer.select(timestampMs = 0L, x = 10.0, y = 10.0)
        val state = analyzer.update(timestampMs = 100L, rawX = 11.0, rawY = 11.0)

        assertEquals(0.0, state.displacementPx, 0.001)
        assertEquals(0.0, state.totalDistancePx, 0.001)
        assertFalse(state.isMoving)
    }
}
