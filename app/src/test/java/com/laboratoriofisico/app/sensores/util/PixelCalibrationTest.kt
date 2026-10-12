package com.laboratoriofisico.app.sensores.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PixelCalibrationTest {

    @Test
    fun withoutCalibrationNoPhysicalDistanceIsInferred() {
        assertNull(DistanceCalibration.toCentimeters(363.5, null))
    }

    @Test
    fun convertsPixelsUsingReferenceObject() {
        val calibration = PixelCalibration(referenceSizeCm = 10.0, observedPixels = 250.0)
        assertEquals(25.0, calibration.pixelsPerCm, 0.0001)
        assertEquals(4.0, DistanceCalibration.toCentimeters(100.0, calibration)!!, 0.0001)
    }

    @Test
    fun invalidCalibrationIsIgnored() {
        val calibration = PixelCalibration(referenceSizeCm = 0.0, observedPixels = 250.0)
        assertNull(DistanceCalibration.toCentimeters(100.0, calibration))
    }
}
