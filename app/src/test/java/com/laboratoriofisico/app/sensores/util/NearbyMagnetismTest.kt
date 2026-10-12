package com.laboratoriofisico.app.sensores.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyMagnetismTest {
    @Test
    fun excessIsAbsoluteChangeFromRoomBaseline() {
        assertNull(NearbyMagnetism.excessUt(null))
        assertEquals(0.0, NearbyMagnetism.excessUt(0.0)!!, 0.001)
        assertEquals(25.0, NearbyMagnetism.excessUt(-25.0)!!, 0.001)
        assertEquals(80.0, NearbyMagnetism.excessUt(80.0)!!, 0.001)
    }

    @Test
    fun levelRisesAsMagnetGetsCloser() {
        assertEquals(NearbyMagnetismLevel.MUY_BAJO, NearbyMagnetism.level(2.0))
        assertEquals(NearbyMagnetismLevel.MEDIO, NearbyMagnetism.level(20.0))
        assertEquals(NearbyMagnetismLevel.ALTO, NearbyMagnetism.level(90.0))
        assertTrue(NearbyMagnetism.gaugeProgress(2.0) < NearbyMagnetism.gaugeProgress(20.0))
        assertTrue(NearbyMagnetism.gaugeProgress(20.0) < NearbyMagnetism.gaugeProgress(90.0))
    }
}
