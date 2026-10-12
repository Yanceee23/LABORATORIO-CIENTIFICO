package com.laboratoriofisico.app.sensores.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraSceneLuxTest {

    @Test
    fun indoorExposure_isRoomLightNotSun() {
        val lux = CameraSceneLux.estimateLux(
            luma = 90.0,
            exposureTimeNs = 16_666_667L,
            iso = 400,
            aperture = 1.8
        )!!
        assertTrue("habitación debería estar entre 50 y 400 lux, fue $lux", lux in 50.0..400.0)
    }

    @Test
    fun sunlight_isMuchBrighterThanARoom() {
        val room = CameraSceneLux.estimateLux(90.0, 16_666_667L, 400, 1.8)!!
        val sun = CameraSceneLux.estimateLux(90.0, 500_000L, 50, 1.8)!!
        assertTrue(sun > 10_000.0)
        assertTrue(sun > room * 20)
    }

    @Test
    fun coveredLens_dropsEvenIfExposureStaysTheSame() {
        val open = CameraSceneLux.estimateLux(90.0, 20_000_000L, 800, 1.8)!!
        val covered = CameraSceneLux.estimateLux(4.0, 20_000_000L, 800, 1.8)!!
        assertTrue(covered < open / 20)
    }

    @Test
    fun missingAperture_usesRearCameraFallback() {
        val withFallback = CameraSceneLux.estimateLux(90.0, 16_666_667L, 400, null)!!
        val explicit = CameraSceneLux.estimateLux(90.0, 16_666_667L, 400, CameraSceneLux.FALLBACK_APERTURE)!!
        assertEquals(explicit, withFallback, 0.001)
    }

    @Test
    fun invalidExposure_returnsNull() {
        assertNull(CameraSceneLux.estimateLux(90.0, 0L, 400, 1.8))
        assertNull(CameraSceneLux.estimateLux(90.0, 16_666_667L, 0, 1.8))
    }
}
