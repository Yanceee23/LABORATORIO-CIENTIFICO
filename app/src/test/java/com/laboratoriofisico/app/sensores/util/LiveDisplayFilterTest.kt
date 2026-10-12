package com.laboratoriofisico.app.sensores.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveDisplayFilterTest {
    @Test
    fun smallNoiseDoesNotChangeTheShownNumber() {
        val filter = LiveDisplayFilter(alpha = 0.2)
        val first = filter.stable("mag", 9.81, step = 0.1)
        var last = first
        repeat(30) { index ->
            val noise = if (index % 2 == 0) 0.03 else -0.03
            last = filter.stable("mag", 9.81 + noise, step = 0.1)
        }
        assertEquals(first, last, 0.0)
    }

    @Test
    fun aRealChangeMovesTheShownNumber() {
        val filter = LiveDisplayFilter(alpha = 0.5)
        val rest = filter.stable("mag", 9.8, step = 0.1)
        var moved = rest
        repeat(8) {
            moved = filter.stable("mag", 15.0, step = 0.1)
        }
        assertTrue(moved > rest + 1.0)
    }
}
